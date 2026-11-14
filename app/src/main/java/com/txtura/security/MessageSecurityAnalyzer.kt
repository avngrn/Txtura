@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma")

package com.txtura.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BadParcelableException
import android.util.Log
import androidx.core.content.IntentSanitizer
import com.txtura.ui.i18n.TxturaStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.IDN
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLHandshakeException

enum class SecurityRiskLevel {
    SAFE,
    CAUTION,
    SUSPICIOUS
}

data class SecurityAnalysisResult(
    val riskLevel: SecurityRiskLevel,
    val reasons: List<String>,
    val extractedOtpCode: String?,
)

data class DetectedSafeLink(
    val startIndex: Int,
    val endIndex: Int,
    val rawText: String,
    val browserUrl: String,
)

data class DetectedBankAccount(
    val rawText: String,
    val formattedAccountNumber: String,
    val standardDigits: String,
    val bankName: String?,
    val isIban: Boolean = false,
) {
    val accountNumber: String get() = formattedAccountNumber
    val formattedNumber: String get() = formattedAccountNumber
}

data class DetectedPaymentReference(
    val rawText: String,
    val referenceNumber: String,
    val model: String? = null,
)

data class DetectedPaymentAmount(
    val rawText: String,
    val displayAmount: String,
    val normalizedValue: String,
    val currency: String,
)

data class DetectedDateEvent(
    val rawText: String,
    val displayDate: String,
    val startEpochMillis: Long,
    val isPaymentDeadline: Boolean = false,
)

data class DetectedPackageTracking(
    val rawText: String,
    val trackingCode: String,
    val courierName: String? = null,
)

/**
 * Multi-layer on-device SMS/MMS Threat & Phishing Intelligence Engine:
 * Inspired by OWASP Phishing/URL Heuristics, Google Safe Browsing URL canonicalization,
 * SpamAssassin/rspamd URI scoring, and CISA/NIST Smishing & AI Scam indicators.
 *
 * Key design principles:
 * 1. Benign HTTPS links to normal websites (e.g. YouTube.com, google.com, news, official domains)
 *    are classified as [SecurityRiskLevel.SAFE] (zero false-positive fatigue).
 * 2. Detects IDN Homograph / Punycode (`xn--`), mixed Cyrillic/Latin spoofing, and brand typosquatting.
 * 3. Detects Malware / APK / EXE dropper links sent via SMS (FluBot, SpyNote, Medusa banking trojans).
 * 4. Detects URL `@` credential redirection tricks, raw IP URLs, unencrypted HTTP login lures,
 *    and abused high-risk TLDs (`.xyz`, `.top`, `.click`, `.zip`, `.icu`, `.tk`, etc.).
 * 5. Detects global Phishing, Package/Customs Smishing, AI Investment/Crypto scams, Fake Job/Task
 *    scams, and AI Voice-Clone / "Hi Mom/Dad new number" emergency scams worldwide.
 */
object MessageSecurityAnalyzer {

    private const val TAG = "TxturaSecurity"

    private data class OnlineUrlVerdict(
        val isMalicious: Boolean,
        val reason: String?,
        val checkedAtMillis: Long = System.currentTimeMillis(),
    )

    private const val MAX_VERDICT_CACHE_SIZE = 500

    // Thread-safe bounded cache of online URL threat intelligence verdicts (host/url -> verdict)
    private val onlineVerdictCache = ConcurrentHashMap<String, OnlineUrlVerdict>()

    private fun cacheOnlineVerdict(key: String, verdict: OnlineUrlVerdict) {
        if (onlineVerdictCache.size >= MAX_VERDICT_CACHE_SIZE) {
            val now = System.currentTimeMillis()
            val expiredKeys = onlineVerdictCache.entries
                .filter { (now - it.value.checkedAtMillis) >= 1_800_000L }
                .map { it.key }
            if (expiredKeys.isNotEmpty()) {
                expiredKeys.forEach { onlineVerdictCache.remove(it) }
            } else {
                val oldestKey = onlineVerdictCache.entries.minByOrNull { it.value.checkedAtMillis }?.key
                if (oldestKey != null) {
                    onlineVerdictCache.remove(oldestKey)
                }
            }
        }
        onlineVerdictCache[key] = verdict
    }

    @Volatile
    var isOnlineMaliciousWebsiteCheckEnabled: Boolean = true

    private data class ParsedUrl(
        val rawUrl: String,
        val scheme: String,
        val host: String,
        val registeredDomain: String,
        val tld: String,
        val pathAndQuery: String,
        val hasEmbeddedAtSymbol: Boolean,
        val nonStandardPort: Int?,
    )

    private val urlExtractRegex = Regex(
        pattern = """(?i)\b((?:https?://|www\.)[^\s<>"]+|[a-z0-9\u0400-\u04FF.-]+\.[a-z\u0400-\u04FF]{2,10}(?:/[^\s<>"]*)?)""",
    )

    // Well-known legitimate global & regional domains (unless spoofed via subdomain or '@' trick)
    private val trustedGlobalDomains = setOf(
        "google.com", "youtube.com", "youtu.be", "gmail.com", "maps.app.goo.gl",
        "apple.com", "icloud.com", "microsoft.com", "live.com", "office.com",
        "github.com", "gitlab.com", "stackoverflow.com", "wikipedia.org",
        "amazon.com", "amazon.de", "amazon.co.uk", "ebay.com",
        "netflix.com", "spotify.com", "twitch.tv", "reddit.com",
        "instagram.com", "facebook.com", "fb.com", "messenger.com", "whatsapp.com", "wa.me",
        "viber.com", "telegram.org", "signal.org", "linkedin.com", "x.com", "twitter.com",
        "paypal.com", "stripe.com", "revolut.com", "wise.com",
        "dhl.com", "fedex.com", "ups.com", "usps.com",
        "posta.rs", "mts.rs", "yettel.rs", "a1.rs", "euprava.gov.rs", "rts.rs", "n1info.rs",
        "bbc.com", "bbc.co.uk", "cnn.com", "nytimes.com", "reuters.com",
    )

    // Global brands frequently impersonated in smishing (mapped to their official domains)
    private val protectedBrandDomains = mapOf(
        "paypal" to setOf("paypal.com", "paypal.me"),
        "apple" to setOf("apple.com", "icloud.com"),
        "icloud" to setOf("icloud.com", "apple.com"),
        "google" to setOf("google.com", "youtube.com", "gmail.com"),
        "microsoft" to setOf("microsoft.com", "live.com", "office.com"),
        "netflix" to setOf("netflix.com"),
        "amazon" to setOf("amazon.com", "amazon.de", "amazon.co.uk", "amzn.to", "amzn.eu"),
        "binance" to setOf("binance.com", "binance.us"),
        "coinbase" to setOf("coinbase.com"),
        "metamask" to setOf("metamask.io"),
        "revolut" to setOf("revolut.com"),
        "dhl" to setOf("dhl.com", "dhl.de", "dhl.rs"),
        "fedex" to setOf("fedex.com"),
        "usps" to setOf("usps.com"),
        "ups" to setOf("ups.com"),
        "intesa" to setOf("bancaintesa.rs", "intesasanpaolo.com"),
        "raiffeisen" to setOf("raiffeisenbank.rs", "raiffeisen.at"),
        "unicredit" to setOf("unicreditbank.rs", "unicredit.eu"),
        "postars" to setOf("posta.rs"),
        "euprava" to setOf("euprava.gov.rs")
    )

    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "rb.gy", "is.gd", "cutt.ly", "cut.ly",
        "shorturl.at", "t.ly", "tiny.cc", "ow.ly", "rebrand.ly", "bl.ink", "v.gd", "clck.ru"
    )

    // High-abuse TLDs according to Spamhaus, APWG & Threat Intelligence feeds
    private val highRiskTlds = setOf(
        ".xyz", ".top", ".click", ".link", ".work", ".zip", ".mov", ".gq", ".tk",
        ".ml", ".cf", ".ga", ".icu", ".buzz", ".rest", ".fit", ".cam", ".monster",
        ".sbs", ".cfd", ".loan", ".kim", ".men", ".win", ".vip", ".shop"
    )

    // Dangerous file extensions used in SMS malware droppers (Android trojans, PC malware)
    private val malwareFileExtensions = setOf(
        ".apk", ".xapk", ".dex", ".exe", ".scr", ".msi", ".bat", ".cmd",
        ".ps1", ".vbs", ".jar", ".dmg", ".iso", ".elf", ".sh"
    )

    private val ipv4HostRegex = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")

    // Worldwide Phishing & Smishing social-engineering patterns (EN, SR/HR/BS, DE, ES, FR, Global)
    private val phishingSocialEngineeringPatterns = listOf(
        // Serbian / Ex-Yu
        "blokiran račun", "verifikujte karticu", "hitna uplata", "carina paket",
        "vaš nalog je suspendovan", "unesite pin", "potvrdite identitet",
        "neisporučen paket", "neplaćena carina", "ažurirajte adresu za dostavu",
        "vaša kartica je privremeno blokirana", "izbegli blokadu",
        // English / Global
        "account suspended", "verify your card", "urgent payment", "customs fee",
        "confirm your identity", "unusual login activity", "unauthorized transaction",
        "package could not be delivered", "incomplete address", "reschedule delivery",
        "unpaid toll", "tax refund pending", "bank account locked", "update payment method",
        // German / Spanish / French common global smishing lures
        "konto gesperrt", "paket konnte nicht zugestellt", "zollgebühr",
        "cuenta suspendida", "paquete retenido", "aduanas",
        "compte suspendu", "colis en attente", "frais de douane"
    )

    // AI Scams, Crypto/Pig-Butchering, Fake Task Scams & Family Emergency ("Hi Mom") Impersonation
    private val aiAndModernScamPatterns = listOf(
        // AI & Crypto Investment Scams
        "ai trading bot", "quantum ai", "chatgpt trading", "guaranteed daily profit",
        "crypto arbitrage", "usdt reward", "claim your airdrop", "seed phrase",
        "connect your wallet", "validate your wallet", "earn 500$ a day", "earn $",
        "zarada pomoću ai", "sigurna kripto zarada", "dnevna zarada na klik",
        // Fake Job / YouTube/TikTok Like Task Scams
        "like youtube videos and earn", "part-time remote job", "telegram receptionist",
        "lajkovanje videa za novac", "posao od kuće bez iskustva isplata odmah",
        // "Hi Mom / Hi Dad" & AI Voice/Emergency Impersonation
        "hi mum this is my new number", "hi mom this is my new number",
        "broke my phone", "dropped my phone in water", "pay an urgent bill for me",
        "mama ovo je moj novi broj", "tata ovo je moj novi broj",
        "razbio sam telefon", "pokvario mi se telefon hitno mi uplati"
    )

    // Login / credential harvesting path tokens inside URLs
    private val credentialPathKeywords = listOf(
        "login", "signin", "verify", "secure", "account", "update", "banking",
        "wallet", "password", "passcode", "confirm", "verifikacija", "prijava", "kartica"
    )

    // Matches standard prefixed 2FA codes (e.g., G-123456, FB-12345)
    private val prefixedOtpRegex = Regex(
        pattern = """\b(?:G|FB|IG|WA|MS|A|U|V)-(\d{4,8})\b"""
    )

    // Matches 4-8 digit OTP codes preceded or followed by verification context
    private val otpContextRegex = Regex(
        pattern = """(?i)(?:kod|code|otp|2fa|verifikac\w*|verification|verify|pin|lozink\w*|passcode|token|sigurnosn\w*|security|potvrd\w*|confirm\w*|aktivac\w*|jednokratn\w*)\b[^\d\n]{0,40}?\b(\d{4,8})\b|\b(\d{4,8})\b[^\d\n]{0,35}?\b(?:je vaš|is your|kod|code|otp|2fa|verification|za prijavu|to verify|to sign in)"""
    )

    // Matches hyphenated 6-digit codes (e.g., 123-456) when verification keywords are present
    private val hyphenatedOtpRegex = Regex(
        pattern = """\b(\d{3})-(\d{3})\b"""
    )

    // Keywords indicating the SMS is a 2FA / authentication / verification code message
    private val otpHintKeywords = listOf(
        "kod", "code", "otp", "2fa", "verifikac", "verification", "verify",
        "pin", "lozink", "passcode", "token", "sigurnosn", "security",
        "potvrd", "confirm", "aktivac", "jednokratn", "one-time", "prijav", "login", "<#>"
    )

    // Any 4-8 digit standalone token (excluding years like 2024-2029 unless no other code is present)
    private val standaloneDigitsRegex = Regex("""\b(\d{4,8})\b""")

    // Pre-compiled control character stripper (avoids regex compilation per message)
    private val controlCharsRegex = Regex("[\u202A-\u202E\u2066-\u2069\u0000]")

    fun analyzeMessage(body: String): SecurityAnalysisResult {
        if (body.isBlank()) {
            return SecurityAnalysisResult(SecurityRiskLevel.SAFE, emptyList(), null)
        }

        val strings = TxturaStrings.forLocale()
        val reasons = mutableListOf<String>()
        var threatScore = 0 // 0 = SAFE, 1..2 = CAUTION, >= 3 = SUSPICIOUS
        val lowerBody = body.lowercase()

        val parsedUrls = extractAndParseUrls(body)

        for (url in parsedUrls) {
            val isTrustedDomain = (url.registeredDomain in trustedGlobalDomains) && !url.hasEmbeddedAtSymbol

            // 1. Check for '@' credential redirection trick
            if (url.hasEmbeddedAtSymbol) {
                threatScore += 4
                reasons.add(strings.reasonEmbeddedCredentialsUrl)
            }

            // 2. Check for raw IP address URLs
            if (ipv4HostRegex.matches(url.host)) {
                threatScore += 4
                reasons.add(strings.reasonIpUrl)
            }

            // 3. Check for Malware / APK / EXE file droppers in the URL path
            val matchedMalwareExt = malwareFileExtensions.firstOrNull { ext ->
                url.pathAndQuery.substringBefore('?').endsWith(ext) ||
                    url.pathAndQuery.contains("$ext?") ||
                    url.pathAndQuery.contains("$ext&")
            }
            if ((matchedMalwareExt != null) && !isTrustedDomain) {
                threatScore += 5
                reasons.add(strings.reasonMalwareFile(matchedMalwareExt))
            }

            // 4. Check for IDN Homograph / Punycode (xn--) or mixed Latin + Cyrillic characters in host
            if (hasHomographOrMixedScript(url.host)) {
                threatScore += 4
                reasons.add(strings.reasonHomographOrBrandSpoof(url.host))
            }

            // 5. Check for Brand Impersonation / Typosquatting / Subdomain Spoofing
            val spoofedBrand = detectBrandImpersonation(url)
            if (spoofedBrand != null) {
                threatScore += 4
                reasons.add(strings.reasonHomographOrBrandSpoof(spoofedBrand))
            }

            // 6. Check for URL Shorteners (hides true destination)
            if ((url.registeredDomain in urlShorteners) || (url.host in urlShorteners)) {
                // Shortener alone is CAUTION (+2); combined with any other indicator becomes SUSPICIOUS (>=3)
                threatScore += 2
                reasons.add(strings.reasonShortener(url.host))
            }

            // 7. Check for High-Risk Abused TLDs strictly on the host TLD (never substring in path!)
            if (!isTrustedDomain && (url.tld in highRiskTlds)) {
                threatScore += 3
                reasons.add(strings.reasonHighRiskTld(url.tld))
            }

            // 8. Check for unencrypted HTTP protocol
            if (url.scheme == "http") {
                val hasLoginPath = credentialPathKeywords.any { url.pathAndQuery.contains(it) || url.host.contains(it) }
                if (hasLoginPath || !isTrustedDomain) {
                    threatScore += if (hasLoginPath) 3 else 2
                    reasons.add(strings.reasonHttp)
                }
            }

            // 9. Suspicious non-standard ports (e.g. :8080, :4444) or excessive hyphen/subdomain obfuscation
            if ((url.nonStandardPort != null) && (url.nonStandardPort !in listOf(80, 443))) {
                threatScore += 3
                reasons.add(strings.reasonIpUrl)
            }

            // 9b. Apply cached online threat intelligence verdict if available & enabled
            if (isOnlineMaliciousWebsiteCheckEnabled) {
                val cachedVerdict = onlineVerdictCache[url.rawUrl.lowercase()]
                    ?: onlineVerdictCache[url.host.lowercase()]
                if (cachedVerdict?.isMalicious == true) {
                    threatScore += 5
                    reasons.add(cachedVerdict.reason ?: strings.reasonMaliciousWebsiteOnline(url.host))
                }
            }
        }

        // 10. Check for Phishing / Smishing Social Engineering Lures
        val hasPhishingKeywords = phishingSocialEngineeringPatterns.any { lowerBody.contains(it) }
        if (hasPhishingKeywords) {
            if (parsedUrls.isNotEmpty()) {
                val allUrlsTrusted = parsedUrls.all { (_, scheme, _, registeredDomain, _, _, hasEmbeddedAtSymbol, _) ->
                    (registeredDomain in trustedGlobalDomains) && !hasEmbeddedAtSymbol && (scheme != "http")
                }
                if (!allUrlsTrusted) {
                    threatScore += 3
                    reasons.add(strings.reasonSocialEngineering)
                }
            } else {
                threatScore += 1
                reasons.add(strings.reasonSocialEngineering)
            }
        }

        // 11. Check for AI Scams, Crypto Pig-Butchering, Fake Task Scams & Family Impersonation
        val hasAiOrCryptoScam = aiAndModernScamPatterns.any { lowerBody.contains(it) }
        if (hasAiOrCryptoScam) {
            threatScore += if (parsedUrls.isNotEmpty()) 4 else 3
            reasons.add(strings.reasonAiOrCryptoScam)
        }

        val finalRisk = when {
            threatScore >= 3 -> SecurityRiskLevel.SUSPICIOUS
            threatScore in 1..2 -> SecurityRiskLevel.CAUTION
            else -> SecurityRiskLevel.SAFE
        }

        val otpCode = extractOtpOrVerificationCode(body, lowerBody)

        return SecurityAnalysisResult(
            riskLevel = finalRisk,
            reasons = reasons.distinct(),
            extractedOtpCode = otpCode,
        )
    }

    /**
     * Extracts 2FA, OTP, or standard verification codes from an SMS message body.
     */
    private fun extractOtpOrVerificationCode(body: String, lowerBody: String): String? {
        // 1. Check standard prefixed codes like G-123456
        prefixedOtpRegex.find(body)?.groupValues?.getOrNull(1)?.let { return it }

        // 2. Check direct contextual patterns
        otpContextRegex.find(body)?.let { match ->
            (match.groups[1]?.value ?: match.groups[2]?.value)?.let { return it }
        }

        val hasOtpKeyword = otpHintKeywords.any { lowerBody.contains(it) }
        if (hasOtpKeyword) {
            // 3. Check hyphenated 6-digit codes like 123-456 in a verification message
            hyphenatedOtpRegex.find(body)?.let { match ->
                val p1 = match.groupValues.getOrNull(1).orEmpty()
                val p2 = match.groupValues.getOrNull(2).orEmpty()
                if (p1.isNotEmpty() && p2.isNotEmpty()) {
                    return p1 + p2
                }
            }
            // 4. Fallback to first 4-8 digit number in a message that contains 2FA/code keywords
            standaloneDigitsRegex.findAll(body)
                .map { it.value }
                .firstOrNull { candidate ->
                    val isNotFourDigits = candidate.length != 4
                    val isNotRecentYear = candidate.toIntOrNull() !in 1990..2035
                    isNotFourDigits || isNotRecentYear
                }
                ?.let { return it }
        }

        // 5. If the entire trimmed SMS is just a 4-8 digit code
        val trimmed = body.trim()
        if (standaloneDigitsRegex.matches(trimmed)) {
            return trimmed
        }

        return null
    }

    private val nonUrlAbbreviationPrefixes = setOf(
        "br", "broj", "ul", "dr", "mr", "prof", "sv", "st", "god", "str",
        "tel", "mob", "din", "rsd", "eur", "racun", "račun", "ugovor",
        "paket", "posiljka", "pošiljka", "iznos", "model", "dana", "kod"
    )

    private val validCcTlds = setOf(
        "ac", "ad", "ae", "af", "ag", "ai", "al", "am", "ao", "aq", "ar", "as", "at", "au", "aw", "ax", "az",
        "ba", "bb", "bd", "be", "bf", "bg", "bh", "bi", "bj", "bm", "bn", "bo", "br", "bs", "bt", "bw", "by", "bz",
        "ca", "cc", "cd", "cf", "cg", "ch", "ci", "ck", "cl", "cm", "cn", "co", "cr", "cu", "cv", "cw", "cx", "cy", "cz",
        "de", "dj", "dk", "dm", "do", "dz", "ec", "ee", "eg", "er", "es", "et", "eu", "fi", "fj", "fk", "fm", "fo", "fr",
        "ga", "gb", "gd", "ge", "gf", "gg", "gh", "gi", "gl", "gm", "gn", "gp", "gq", "gr", "gs", "gt", "gu", "gw", "gy",
        "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il", "im", "in", "io", "iq", "ir", "is", "it", "je", "jm", "jo", "jp",
        "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk", "lr", "ls", "lt", "lu", "lv", "ly",
        "ma", "mc", "md", "me", "mg", "mh", "mk", "ml", "mm", "mn", "mo", "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz",
        "na", "nc", "ne", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om", "pa", "pe", "pf", "pg", "ph", "pk", "pl", "pm", "pn", "pr", "ps", "pt", "pw", "py",
        "qa", "re", "ro", "rs", "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sk", "sl", "sm", "sn", "so", "sr", "ss", "st", "su", "sv", "sx", "sy", "sz",
        "tc", "td", "tf", "tg", "th", "tj", "tk", "tl", "tm", "tn", "to", "tr", "tt", "tv", "tw", "tz", "ua", "ug", "uk", "us", "uy", "uz",
        "va", "vc", "ve", "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "za", "zm", "zw"
    )

    private val validGenericTlds = setOf(
        ".com", ".org", ".net", ".edu", ".gov", ".mil", ".int", ".info", ".biz",
        ".app", ".dev", ".online", ".site", ".store", ".shop", ".tech", ".cloud",
        ".bank", ".pay", ".page", ".link", ".live", ".news", ".media", ".world",
        ".digital", ".finance", ".money", ".plus", ".pro", ".mobi", ".name",
        ".aero", ".coop", ".museum", ".travel", ".jobs", ".cat", ".tel", ".post",
        ".asia", ".xxx", ".arpa", ".one", ".network", ".systems", ".solutions",
        ".services", ".support", ".center", ".company", ".group", ".team",
        ".agency", ".studio", ".design", ".art", ".games", ".game", ".play",
        ".video", ".music", ".audio", ".photo", ".photos", ".pics", ".today",
        ".life", ".lifestyle", ".health", ".care", ".clinic", ".dental",
        ".legal", ".law", ".tax", ".cash", ".fund", ".investments", ".credit",
        ".exchange", ".market", ".markets", ".trading", ".crypto", ".wallet",
        ".express", ".delivery", ".auto", ".car", ".cars", ".taxi",
        ".hotel", ".hotels", ".flights", ".tours", ".vacation", ".holiday",
        ".house", ".homes", ".estate", ".property", ".rent", ".apartments",
        ".school", ".academy", ".university", ".college", ".courses", ".training",
        ".guide", ".wiki", ".blog", ".forum", ".chat", ".social", ".community",
        ".club", ".fans", ".events", ".party", ".tickets", ".deals", ".sale",
        ".discount", ".coupons", ".Promo", ".gift", ".gifts", ".free", ".best",
        ".win", ".vip", ".top", ".xyz", ".click", ".work", ".icu", ".buzz"
    )

    /**
     * Extracts safe URLs with their exact character offsets in [body] so the UI can render
     * clickable links that open in the default browser while refusing to linkify suspicious URLs.
     */
    fun extractSafeLinks(
        body: String,
        precomputedAnalysis: SecurityAnalysisResult? = null,
    ): List<DetectedSafeLink> {
        if (body.isBlank()) return emptyList()
        val (riskLevel) = precomputedAnalysis ?: analyzeMessage(body)
        if (riskLevel == SecurityRiskLevel.SUSPICIOUS) return emptyList()

        return urlExtractRegex.findAll(body).mapNotNull { match ->
            val matchStart = match.range.first
            val matchEndExclusive = (match.range.last + 1).coerceAtMost(body.length)
            // Ignore abbreviations attached to masked card numbers like "br.kartice*5431"
            if (matchStart > 0 && body[matchStart - 1] == '*') return@mapNotNull null
            if (matchEndExclusive < body.length && body[matchEndExclusive] == '*') return@mapNotNull null

            val raw = match.value.trimEnd('.', ',', ';', ')', ']', '!', '?')
            if (raw.isBlank()) return@mapNotNull null
            if (raw.contains('@') && !raw.contains("://") && !raw.contains('/')) {
                return@mapNotNull null
            }
            val parsed = parseSingleUrl(raw) ?: return@mapNotNull null
            if (!isParsedUrlIndividuallySafe(parsed)) return@mapNotNull null

            val start = match.range.first
            val end = (start + raw.length).coerceAtMost(body.length)
            if (start >= end) return@mapNotNull null

            val browserUrl = if (
                raw.startsWith("https://", ignoreCase = true) ||
                raw.startsWith("http://", ignoreCase = true)
            ) {
                raw
            } else {
                "https://$raw"
            }

            DetectedSafeLink(
                startIndex = start,
                endIndex = end,
                rawText = raw,
                browserUrl = browserUrl,
            )
        }.toList()
    }

    private fun isParsedUrlIndividuallySafe(url: ParsedUrl): Boolean {
        if (isOnlineMaliciousWebsiteCheckEnabled) {
            val cachedVerdict = onlineVerdictCache[url.rawUrl.lowercase()]
                ?: onlineVerdictCache[url.host.lowercase()]
            if (cachedVerdict?.isMalicious == true) return false
        }
        val isTrustedDomain = (url.registeredDomain in trustedGlobalDomains) && !url.hasEmbeddedAtSymbol
        if (url.hasEmbeddedAtSymbol) return false
        if (ipv4HostRegex.matches(url.host)) return false
        val hasMalwareExt = malwareFileExtensions.any { ext ->
            url.pathAndQuery.substringBefore('?').endsWith(ext) ||
                url.pathAndQuery.contains("$ext?") ||
                url.pathAndQuery.contains("$ext&")
        }
        if (hasMalwareExt && !isTrustedDomain) return false
        if (hasHomographOrMixedScript(url.host)) return false
        if (detectBrandImpersonation(url) != null) return false
        if (!isTrustedDomain && (url.tld in highRiskTlds)) return false
        if (url.scheme == "http") {
            val hasLoginPath = credentialPathKeywords.any {
                url.pathAndQuery.contains(it) || url.host.contains(it)
            }
            if (hasLoginPath || !isTrustedDomain) return false
        }
        return (url.nonStandardPort == null) || (url.nonStandardPort in listOf(80, 443))
    }

    private fun extractAndParseUrls(text: String): List<ParsedUrl> {
        return urlExtractRegex.findAll(text).mapNotNull { match ->
            val matchStart = match.range.first
            val matchEndExclusive = (match.range.last + 1).coerceAtMost(text.length)
            if (matchStart > 0 && text[matchStart - 1] == '*') return@mapNotNull null
            if (matchEndExclusive < text.length && text[matchEndExclusive] == '*') return@mapNotNull null

            val raw = match.value.trimEnd('.', ',', ';', ')', ']', '!', '?')
            // Avoid matching plain email addresses or version numbers like "v1.2.3"
            if (raw.contains('@') && !raw.contains("://") && !raw.contains('/')) {
                return@mapNotNull null
            }
            parseSingleUrl(raw)
        }.toList()
    }

    private fun parseSingleUrl(rawUrl: String): ParsedUrl? {
        val lower = rawUrl.lowercase()
        val hasExplicitSchemeOrWww = lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("www.")
        val scheme = when {
            lower.startsWith("http://") -> "http"
            lower.startsWith("https://") -> "https"
            else -> "https"
        }
        val withoutScheme = lower
            .removePrefix("https://")
            .removePrefix("http://")

        val authorityAndPath = withoutScheme.split('/', '?', '#', limit = 2)
        val rawAuthority = authorityAndPath.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val pathAndQuery = if (authorityAndPath.size > 1) "/" + authorityAndPath[1] else ""

        val hasEmbeddedAt = rawAuthority.contains('@')
        val hostPort = rawAuthority.substringAfterLast('@')
        val host = hostPort.substringBefore(':').removePrefix("www.")
        if (host.isBlank() || !host.contains('.')) return null

        val port = hostPort.substringAfter(':', "").toIntOrNull()
        val hostParts = host.split('.').filter { it.isNotBlank() }
        if (hostParts.size < 2) return null

        val lastPart = hostParts.last()
        val tld = ".$lastPart"

        // When no http(s):// or www. is present, strictly validate TLD and reject prose abbreviations like "br.kartice"
        if (!hasExplicitSchemeOrWww) {
            if (pathAndQuery.isEmpty() && hostParts.first() in nonUrlAbbreviationPrefixes) {
                return null
            }
            if (tld in setOf(".jpg", ".jpeg", ".png", ".pdf", ".txt", ".doc", ".docx", ".xls", ".xlsx", ".mp4", ".mp3")) {
                return null
            }
            val isValidTld = (lastPart.length == 2 && lastPart in validCcTlds) ||
                (tld in validGenericTlds) ||
                (tld in highRiskTlds) ||
                host.endsWith(".gov.rs") ||
                host.endsWith(".ac.rs") ||
                host.endsWith(".edu.rs") ||
                host.endsWith(".co.rs") ||
                host.endsWith(".org.rs")
            if (!isValidTld) {
                return null
            }
        }

        val hasCountryCodeCctld = (hostParts.size >= 3) &&
            (hostParts[hostParts.size - 2] in setOf("co", "com", "org", "gov", "ac", "net")) &&
            (hostParts.last().length == 2)
        val registeredDomain = if (hasCountryCodeCctld) {
            hostParts.takeLast(3).joinToString(".")
        } else {
            hostParts.takeLast(2).joinToString(".")
        }

        return ParsedUrl(
            rawUrl = rawUrl,
            scheme = scheme,
            host = host,
            registeredDomain = registeredDomain,
            tld = tld,
            pathAndQuery = pathAndQuery,
            hasEmbeddedAtSymbol = hasEmbeddedAt,
            nonStandardPort = port
        )
    }

    /**
     * Detects Punycode (`xn--`) and mixed Latin + Cyrillic/Greek script homograph attacks
     * (e.g., replacing Latin 'a' or 'o' with Cyrillic 'а' or 'о').
     */
    private fun hasHomographOrMixedScript(host: String): Boolean {
        if (host.contains("xn--")) return true
        val hasLatin = host.any { ch -> ch in 'a'..'z' }
        val hasCyrillic = host.any { ch -> ch in '\u0400'..'\u04FF' }
        return hasLatin && hasCyrillic
    }

    /**
     * Detects when a URL hostname impersonates a major global/local brand (e.g. `paypal-secure.com`,
     * `paypa1.com`, `dhl.com.track-package.xyz`) without belonging to the brand's official domains.
     */
    private fun detectBrandImpersonation(url: ParsedUrl): String? {
        // Normalize common leetspeak substitutions (1 -> l, 0 -> o, rn -> m)
        val normalizedHost = url.host
            .replace('0', 'o')
            .replace('1', 'l')
            .replace("rn", "m")

        for ((brand, officialDomains) in protectedBrandDomains) {
            if (url.host.contains(brand) || normalizedHost.contains(brand)) {
                val isOfficial = officialDomains.any { official ->
                    url.registeredDomain == official || url.host.endsWith(".$official")
                }
                if (!isOfficial) {
                    return brand.replaceFirstChar { it.uppercase() }
                }
            }
        }
        return null
    }

    /**
     * Checks whether [body] contains any candidate URLs that can be verified online.
     */
    fun hasUrlsToInspect(body: String): Boolean {
        return body.isNotBlank() && extractAndParseUrls(body).isNotEmpty()
    }

    /**
     * Performs an asynchronous online malicious website & phishing inspection for all URLs in [body]
     * when [isOnlineMaliciousWebsiteCheckEnabled] is true and internet connectivity is available:
     * 1. Queries Cloudflare Security DNS-over-HTTPS (`security.cloudflare-dns.com`, which blocks
     *    known malware & phishing domains by returning `0.0.0.0` or censored response).
     * 2. Resolves HTTP/HTTPS redirect chains for shortened or untrusted URLs to uncover hidden
     *    phishing/malware landing domains, invalid TLS certificates, or credential-harvesting forms.
     *
     * Returns an updated [SecurityAnalysisResult] if a new online threat is discovered, or `null`
     * if no change in security status occurred.
     */
    suspend fun inspectMessageUrlsOnline(
        context: Context,
        body: String
    ): SecurityAnalysisResult? = withContext(Dispatchers.IO) {
        if (!isOnlineMaliciousWebsiteCheckEnabled || body.isBlank()) return@withContext null
        if (!isInternetAvailable(context)) return@withContext null

        val parsedUrls = extractAndParseUrls(body)
        if (parsedUrls.isEmpty()) return@withContext null

        val strings = TxturaStrings.forLocale()
        var newlyFlagged = false

        for (url in parsedUrls) {
            val rawKey = url.rawUrl.lowercase()
            val hostKey = url.host.lowercase()

            // Skip already cached URLs (valid for 30 minutes)
            val existing = onlineVerdictCache[rawKey] ?: onlineVerdictCache[hostKey]
            if (existing != null && (System.currentTimeMillis() - existing.checkedAtMillis) < 1_800_000L) {
                if (existing.isMalicious) newlyFlagged = true
                continue
            }

            val isTrusted = (url.registeredDomain in trustedGlobalDomains) &&
                !url.hasEmbeddedAtSymbol &&
                (url.registeredDomain !in urlShorteners)

            // 1. Check domain reputation via Cloudflare Malware/Phishing Filtering DNS-over-HTTPS
            if (!isTrusted) {
                val blockedBySecurityDns = checkCloudflareSecurityDnsBlocked(url.host, context)
                if (blockedBySecurityDns) {
                    val reason = strings.reasonMaliciousWebsiteOnline(url.host)
                    val verdict = OnlineUrlVerdict(isMalicious = true, reason = reason)
                    cacheOnlineVerdict(hostKey, verdict)
                    cacheOnlineVerdict(rawKey, verdict)
                    newlyFlagged = true
                    continue
                }
            }

            // 2. Inspect redirect chain, TLS validity, and landing page for shorteners or untrusted links
            if (!isTrusted || (url.registeredDomain in urlShorteners) || (url.host in urlShorteners)) {
                val redirectVerdict = inspectUrlRedirectAndLandingOnline(url, strings, context)
                cacheOnlineVerdict(rawKey, redirectVerdict)
                if (redirectVerdict.isMalicious) {
                    if (url.registeredDomain !in urlShorteners) {
                        cacheOnlineVerdict(hostKey, redirectVerdict)
                    }
                    newlyFlagged = true
                    continue
                }
            }

            cacheOnlineVerdict(rawKey, OnlineUrlVerdict(isMalicious = false, reason = null))
        }

        if (newlyFlagged) {
            analyzeMessage(body)
        } else {
            null
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Queries Cloudflare's 1.1.1.2 Malware & Phishing Blocking DoH endpoint:
     * `https://security.cloudflare-dns.com/dns-query?name=<host>&type=A`
     * If the domain is sinkholed to `0.0.0.0` or carries the "Censored"/"Filtered" EDE comment,
     * the domain is a confirmed malicious/phishing website.
     */
    private fun checkCloudflareSecurityDnsBlocked(rawHost: String, context: Context): Boolean {
        return try {
            val asciiHost = IDN.toASCII(rawHost.trim().trimEnd('.'))
            if (asciiHost.isBlank() || ipv4HostRegex.matches(asciiHost)) return false

            val encodedName = URLEncoder.encode(asciiHost, "UTF-8")
            val endpoint = URL("https://security.cloudflare-dns.com/dns-query?name=$encodedName&type=A")
            val conn = AppSecurityManager.openSecureConnection(endpoint, context)
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/dns-json")
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            conn.instanceFollowRedirects = false

            try {
                if (conn.responseCode != 200) return false
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val answers = json.optJSONArray("Answer")
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val ans = answers.optJSONObject(i) ?: continue
                        val data = ans.optString("data", "").trim()
                        if (data == "0.0.0.0" || data == "::" || data == "127.0.0.1") {
                            return true
                        }
                    }
                }
                val comment = json.optString("Comment", "").lowercase()
                if (comment.contains("filtered") || comment.contains("malware") || comment.contains("phishing")) {
                    return true
                }
                false
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Follows up to 4 HTTP redirects without executing JavaScript to verify:
     * - Whether a shortened link (`bit.ly`, `tinyurl.com`, etc.) redirects to a phishing/malware domain
     * - Whether the destination domain is blocked by Cloudflare Security DNS or static brand-spoof rules
     * - Whether the HTTPS server presents an invalid/self-signed/expired TLS certificate
     * - Whether an untrusted landing page contains password/card-number harvesting inputs impersonating a brand
     */
    private fun inspectUrlRedirectAndLandingOnline(
        initialUrl: ParsedUrl,
        strings: com.txtura.ui.i18n.AppStrings,
        context: Context
    ): OnlineUrlVerdict {
        var currentUrlStr = if (
            initialUrl.rawUrl.startsWith("http://", ignoreCase = true) ||
            initialUrl.rawUrl.startsWith("https://", ignoreCase = true)
        ) {
            initialUrl.rawUrl
        } else {
            "https://${initialUrl.rawUrl}"
        }

        var hops = 0
        while (hops < 4) {
            hops++
            val conn = try {
                AppSecurityManager.openSecureConnection(URL(currentUrlStr), context)
            } catch (_: Exception) {
                null
            } ?: break

            try {
                conn.instanceFollowRedirects = false
                conn.requestMethod = "GET"
                conn.connectTimeout = 3500
                conn.readTimeout = 3500
                conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"
                )

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")?.trim()
                    if (location.isNullOrBlank()) break
                    val nextUrl = try {
                        URL(URL(currentUrlStr), location).toString()
                    } catch (_: Exception) {
                        location
                    }
                    val parsedDest = parseSingleUrl(nextUrl)
                    if ((parsedDest != null) &&
                        (!isParsedUrlIndividuallySafe(parsedDest) || checkCloudflareSecurityDnsBlocked(parsedDest.host, context))
                    ) {
                        return OnlineUrlVerdict(
                            isMalicious = true,
                            reason = strings.reasonMaliciousRedirectOnline(initialUrl.host, parsedDest.host)
                        )
                    }
                    currentUrlStr = nextUrl
                    continue
                }

                // Final landing page reached — check content-type for direct APK/EXE dropper
                val contentType = conn.contentType?.lowercase().orEmpty()
                if (contentType.contains("application/vnd.android.package-archive") ||
                    contentType.contains("application/x-msdownload") ||
                    contentType.contains("application/x-dosexec")
                ) {
                    return OnlineUrlVerdict(
                        isMalicious = true,
                        reason = strings.reasonMalwareFile(".apk/.exe")
                    )
                }

                // Inspect first 16 KB of HTML on untrusted domains for credential/card harvesting forms
                val finalParsed = parseSingleUrl(currentUrlStr) ?: initialUrl
                val isFinalTrusted = finalParsed.registeredDomain in trustedGlobalDomains
                if (!isFinalTrusted && contentType.contains("text/html")) {
                    val snippet = try {
                        val reader = conn.inputStream.bufferedReader()
                        val buf = CharArray(16_384)
                        val readCount = reader.read(buf)
                        if (readCount > 0) String(buf, 0, readCount).lowercase() else ""
                    } catch (_: Exception) {
                        ""
                    }

                    if (snippet.isNotBlank()) {
                        val hasPasswordOrCardField = snippet.contains("type=\"password\"") ||
                            snippet.contains("type='password'") ||
                            snippet.contains("autocomplete=\"cc-number\"") ||
                            snippet.contains("name=\"cvv\"") ||
                            snippet.contains("name=\"cardnumber\"")

                        val mentionsProtectedBrand = protectedBrandDomains.keys.any { brand ->
                            snippet.contains("<title") && snippet.contains(brand)
                        }

                        if (hasPasswordOrCardField && mentionsProtectedBrand) {
                            return OnlineUrlVerdict(
                                isMalicious = true,
                                reason = strings.reasonMaliciousWebsiteOnline(finalParsed.host)
                            )
                        }
                    }
                }
                break
            } catch (_: SSLHandshakeException) {
                val host = parseSingleUrl(currentUrlStr)?.host ?: initialUrl.host
                return OnlineUrlVerdict(
                    isMalicious = true,
                    reason = strings.reasonInvalidTlsCertificate(host)
                )
            } catch (_: Exception) {
                break
            } finally {
                conn.disconnect()
            }
        }

        return OnlineUrlVerdict(isMalicious = false, reason = null)
    }

    /**
     * Strictly sanitizes a recipient phone number or short code to prevent injection or malformed URIs.
     */
    fun sanitizePhoneNumber(rawInput: String): String {
        val cleaned = rawInput.trim()
            .filterIndexed { index, ch ->
                ch.isDigit() || ((index == 0) && (ch == '+'))
            }
        return cleaned.take(20)
    }

    /**
     * Strips dangerous control characters (such as RTL override characters used in spoofing) from message text.
     */
    fun sanitizeMessageText(rawText: String): String {
        return rawText
            .replace(controlCharsRegex, "")
            .take(2000)
    }

    /**
     * Validates and sanitizes incoming SMS/MMS intents (ACTION_SENDTO, ACTION_VIEW, ACTION_SEND)
     * using AndroidX IntentSanitizer and explicit flag checks to prevent Intent Redirection
     * and unauthorized URI permission leakage.
     */
    data class ValidatedSmsIntent(
        val recipientAddress: String,
        val prefilledBody: String?
    )

    fun validateIncomingSmsIntent(context: Context, incomingIntent: Intent?): ValidatedSmsIntent? {
        if (incomingIntent == null) return null
        val action = incomingIntent.action
        if (action != Intent.ACTION_SENDTO &&
            action != Intent.ACTION_VIEW &&
            action != Intent.ACTION_SEND
        ) {
            return null
        }

        val forbiddenUriFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

        if ((incomingIntent.flags and forbiddenUriFlags) != 0) {
            Log.w(TAG, "Blocked incoming intent with forbidden URI grant flags")
            return null
        }

        incomingIntent.selector = null

        val allowedSchemes = setOf("sms", "smsto", "mms", "mmsto")
        val sanitizer = IntentSanitizer.Builder()
            .allowComponent(ComponentName(context, "com.txtura.MainActivity"))
            .allowAction(Intent.ACTION_SENDTO)
            .allowAction(Intent.ACTION_VIEW)
            .allowAction(Intent.ACTION_SEND)
            .allowData { uri -> uri.scheme?.lowercase() in allowedSchemes }
            .allowType("text/plain")
            .allowExtra("sms_body", String::class.java) { it.length <= 2000 }
            .allowExtra(Intent.EXTRA_TEXT, String::class.java) { it.length <= 2000 }
            .allowExtra("address", String::class.java) { it.length <= 64 }
            .build()

        return try {
            val safeIntent = sanitizer.sanitizeByFiltering(incomingIntent)
            val dataUri = safeIntent.data
            val schemeSpecific = dataUri?.schemeSpecificPart?.substringBefore('?') ?: ""
            val rawAddress = schemeSpecific.ifBlank {
                safeIntent.getStringExtra("address").orEmpty()
            }
            val cleanAddress = sanitizePhoneNumber(rawAddress)
            val rawBody = safeIntent.getStringExtra("sms_body")
                ?: safeIntent.getStringExtra(Intent.EXTRA_TEXT)
            val cleanBody = rawBody?.let { sanitizeMessageText(it) }

            if (cleanAddress.isEmpty() && cleanBody.isNullOrEmpty()) {
                null
            } else {
                ValidatedSmsIntent(
                    recipientAddress = cleanAddress,
                    prefilledBody = cleanBody
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Blocked unsafe incoming SMS intent", e)
            null
        } catch (e: BadParcelableException) {
            Log.w(TAG, "Blocked malformed parcelable in incoming intent", e)
            null
        }
    }

    private val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")

    // Matches "model 97, poziv na broj 25600281895057" or "model: 97 poziv na br. 97-123456"
    private val modelAndPozivRegex = Regex(
        pattern = """(?i)\bmodel\s*:?\s*(\d{2})\s*[,;/\-]?\s*poziv\s+na\s+(?:broj|br\.?)\s*:?\s*([0-9][0-9\-/]{3,24}[0-9])"""
    )

    // Matches "poziv na broj (model 97): 25600281895057" or "poziv na broj: 25600281895057"
    private val pozivNaBrojRegex = Regex(
        pattern = """(?i)\bpoziv\s+na\s+(?:broj|br\.?)(?:\s*\(\s*model\s*(\d{2})\s*\))?\s*:?\s*(?:model\s*(\d{2})\s*[,;\-]?\s*)?([0-9][0-9\-/]{3,24}[0-9])"""
    )

    // Matches dates and date+time timestamps (e.g. 05/10/2026 17:42:27, 01.12.2026., 2026-11-14 12:00)
    private val dateTimeSpanRegex = Regex(
        pattern = """\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\.?(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?|\b\d{4}[./-]\d{1,2}[./-]\d{1,2}(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?|\b\d{1,2}/\d{4}\b|\b\d{1,2}:\d{2}(?::\d{2})?\b"""
    )

    // Matches masked card numbers like "br.kartice*5431 **** **** 6652" or "**6652"
    private val maskedCardSpanRegex = Regex(
        pattern = """(?i)(?:\bbr\.?\s*kartice\s*)?\*+\s*\d{4}(?:[\s*]+\d{4})*|\b\d{4}[\s*]+\*+[\s*]+\d{4}\b"""
    )

    // Matches currency amounts like "3628.99 RSD", "937,99 RSD", "42475,30RSD", "1.450,00 din"
    private val currencyAmountSpanRegex = Regex(
        pattern = """(?i)\b\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?\s*(?:RSD|din(?:ara|\.)?|EUR|€|USD|\$|CHF|GBP|KM|BAM)\b"""
    )

    // Matches contextual payment/bill/transaction amounts (excluding "Raspoloživo" / available balance)
    private val paymentAmountContextRegex = Regex(
        pattern = """(?i)\b(?:iznosi|iznos(?:\s+računa|\s+racuna|\s+transakcije|\s+za\s+uplatu)?|u\s+iznosu\s+od|za\s+uplatu|za\s+naplatu|otkupnina|ukupno(?:\s+za\s+uplatu)?|dug(?:ovanje)?(?:\s+iznosi)?|uplati(?:te|ti)?)\s*:?\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)\s*(RSD|din(?:ara|\.)?|EUR|€|USD|\$|CHF|GBP|KM|BAM)\b"""
    )

    // Matches UPU / Post Express / Pošta Srbije tracking numbers (e.g. PE123456789RS, CC123456789RS)
    private val upuTrackingRegex = Regex(
        pattern = """\b([A-Z]{2}\s*\d{9}\s*[A-Z]{2})\b"""
    )

    // Matches courier tracking numbers preceded by pošiljka/paket/tracking context
    private val contextualTrackingRegex = Regex(
        pattern = """(?i)\b(?:pošiljk[ae]\s+(?:broj|br\.?)|posiljk[ae]\s+(?:broj|br\.?)|broj\s+pošiljke|broj\s+posiljke|paket\s+(?:broj|br\.?)|tracking(?:\s+number|\s+no\.?|\s+code)?|awb)\s*:?\s*([A-Z0-9][A-Z0-9\-]{5,22}[A-Z0-9])\b"""
    )

    // Matches actionable payment deadlines or scheduled appointments (e.g. "do 01.12.2026.", "zakazano za 18.11.2026. u 14:30")
    private val actionableDateRegex = Regex(
        pattern = """(?i)\b(do|rok(?:\s+za\s+uplatu|\s+plaćanja|\s+placanja)?|najkasnije\s+do|dospeva|važi\s+do|vazi\s+do|ističe|istice|zakazano\s+za|termin(?:\s+je)?|pregled(?:\s+je)?|isporuka(?:\s+je)?)\s*:?\s*(\d{1,2})[./-](\d{1,2})[./-](\d{4})\.?(?:\s+u\s+(\d{1,2}):(\d{2}))?"""
    )

    private val phoneCandidateRegex = Regex(
        pattern = """(?<![A-Za-z0-9/:._=*?-])(?:\+|00)?\d[\d\s\-()/]{5,17}\d(?![A-Za-z0-9/:._=*])"""
    )

    private val phoneContextPrecedingRegex = Regex(
        pattern = """(?i)(?:tel(?:efon)?|kontakt(?:\s+centar)?|pozovite|poziv|call|mob(?:ilni)?|phone|centar|kurir|info|podršk[ae]|podrsk[ae]|servis|broj)\s*:?\s*$"""
    )

    private val nonPhonePrecedingRegex = Regex(
        pattern = """(?i)(?:poziv\s+na\s+(?:broj|br\.?)|model\s*\d{0,2}|račun\w*|racun\w*|partij\w*|ugovor\w*|faktur\w*|invoice|transakcij\w*|pošiljk\w*|posiljk\w*|paket\w*|iznos\w*|raspoloživ\w*|raspoloziv\w*|kod|code|otp|pin|id|ref)\s*:?\s*$"""
    )

    /**
     * Extracts email addresses from an incoming SMS/MMS message body.
     */
    fun extractEmailAddresses(body: String): List<String> {
        if (body.isBlank() || !body.contains('@')) return emptyList()
        return emailRegex.findAll(body).map { it.value.trim().trimEnd('.', ',', ';', ':') }.distinct().toList()
    }

    /**
     * Extracts Serbian and international payment reference numbers ("Poziv na broj" & "Model")
     * from bill and banking SMS messages so the user can copy them with a single tap.
     */
    fun extractPaymentReferences(body: String): List<DetectedPaymentReference> {
        if (body.isBlank()) return emptyList()
        val results = mutableListOf<DetectedPaymentReference>()
        val occupiedRanges = mutableListOf<IntRange>()

        for (match in modelAndPozivRegex.findAll(body)) {
            val model = match.groupValues[1].trim().takeIf { it.isNotEmpty() }
            val refRaw = match.groupValues[2].trim().trimEnd('.', ',', ';', ':')
            if (refRaw.count(Char::isDigit) >= 4) {
                results.add(
                    DetectedPaymentReference(
                        rawText = match.value.trim(),
                        referenceNumber = refRaw,
                        model = model
                    )
                )
                occupiedRanges.add(match.range)
            }
        }

        for (match in pozivNaBrojRegex.findAll(body)) {
            if (occupiedRanges.any { rangesOverlap(it, match.range) }) continue
            val model = match.groupValues[1].ifBlank { match.groupValues[2] }.trim().takeIf { it.isNotEmpty() }
            val refRaw = match.groupValues[3].trim().trimEnd('.', ',', ';', ':')
            if (refRaw.count(Char::isDigit) >= 4 && results.none { it.referenceNumber == refRaw }) {
                results.add(
                    DetectedPaymentReference(
                        rawText = match.value.trim(),
                        referenceNumber = refRaw,
                        model = model
                    )
                )
            }
        }

        return results
    }

    /**
     * Extracts bill or transaction payment amounts (e.g. "3628.99 RSD", "937,99 RSD") so the user
     * can copy the exact numeric value for m-banking without copying "Raspoloživo" balance.
     */
    fun extractPaymentAmounts(body: String): List<DetectedPaymentAmount> {
        if (body.isBlank()) return emptyList()
        val results = mutableListOf<DetectedPaymentAmount>()

        for (match in paymentAmountContextRegex.findAll(body)) {
            val rawNum = match.groupValues[1].trim()
            val rawCur = match.groupValues[2].trim()
            val normalized = normalizeMonetaryNumber(rawNum) ?: continue
            val currency = when {
                rawCur.equals("rsd", ignoreCase = true) || rawCur.startsWith("din", ignoreCase = true) -> "RSD"
                rawCur.equals("eur", ignoreCase = true) || rawCur == "€" -> "EUR"
                rawCur.equals("usd", ignoreCase = true) || rawCur == "$" -> "USD"
                else -> rawCur.uppercase()
            }
            if (results.none { it.normalizedValue == normalized && it.currency == currency }) {
                results.add(
                    DetectedPaymentAmount(
                        rawText = match.value.trim(),
                        displayAmount = "$rawNum $currency",
                        normalizedValue = normalized,
                        currency = currency
                    )
                )
            }
        }

        return results
    }

    private fun normalizeMonetaryNumber(rawNum: String): String? {
        val cleaned = rawNum.trim()
        if (cleaned.isEmpty()) return null
        val normalized = when {
            cleaned.contains('.') && cleaned.contains(',') -> {
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    // European format: 1.833,80 -> 1833.80
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    // US format: 1,833.80 -> 1833.80
                    cleaned.replace(",", "")
                }
            }
            cleaned.contains(',') -> {
                // e.g. 937,99 -> 937.99
                cleaned.replace(',', '.')
            }
            else -> cleaned
        }
        val asDouble = normalized.toDoubleOrNull() ?: return null
        if (asDouble <= 0.0) return null
        return normalized
    }

    /**
     * Extracts actionable dates (payment due dates like "do 01.12.2026." or scheduled appointments
     * like "zakazano za 18.11.2026. u 14:30") so the user can add a Calendar reminder in one tap.
     * Does NOT trigger on automated past card-swipe timestamps (e.g. "10/11/2026 22:46:31 PLACANJE MC").
     */
    fun extractActionableDates(body: String): List<DetectedDateEvent> {
        if (body.isBlank()) return emptyList()
        val results = mutableListOf<DetectedDateEvent>()

        for (match in actionableDateRegex.findAll(body)) {
            val triggerWord = match.groupValues[1].lowercase()
            val day = match.groupValues[2].toIntOrNull() ?: continue
            val month = match.groupValues[3].toIntOrNull() ?: continue
            val year = match.groupValues[4].toIntOrNull() ?: continue
            val hour = match.groupValues.getOrNull(5)?.toIntOrNull() ?: 9
            val minute = match.groupValues.getOrNull(6)?.toIntOrNull() ?: 0

            if (day !in 1..31 || month !in 1..12 || year !in 2024..2100) continue
            if (hour !in 0..23 || minute !in 0..59) continue

            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, year)
                set(java.util.Calendar.MONTH, month - 1)
                set(java.util.Calendar.DAY_OF_MONTH, day)
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            val dayStr = day.toString().padStart(2, '0')
            val monthStr = month.toString().padStart(2, '0')
            val timeSuffix = if (match.groupValues.getOrNull(5).isNullOrBlank()) {
                ""
            } else {
                " ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
            }
            val displayDate = "$dayStr.$monthStr.$year.$timeSuffix"
            val isDeadline = triggerWord.contains("do") ||
                triggerWord.contains("rok") ||
                triggerWord.contains("dospeva") ||
                triggerWord.contains("ističe") ||
                triggerWord.contains("istice") ||
                triggerWord.contains("važi") ||
                triggerWord.contains("vazi")

            if (results.none { it.displayDate == displayDate }) {
                results.add(
                    DetectedDateEvent(
                        rawText = match.value.trim(),
                        displayDate = displayDate,
                        startEpochMillis = cal.timeInMillis,
                        isPaymentDeadline = isDeadline
                    )
                )
            }
        }

        return results
    }

    /**
     * Extracts postal and courier package tracking numbers (Post Express, Pošta Srbije, DExpress,
     * Bex, Aks, City Express, DHL, international UPU codes) from delivery SMS messages.
     */
    fun extractPackageTrackingNumbers(body: String): List<DetectedPackageTracking> {
        if (body.isBlank()) return emptyList()
        val results = mutableListOf<DetectedPackageTracking>()
        val lowerBody = body.lowercase()

        val detectedCourier = when {
            lowerBody.contains("post express") || lowerBody.contains("postexpress") || lowerBody.contains("pošta") || lowerBody.contains("posta.rs") -> "Post Express"
            lowerBody.contains("d express") || lowerBody.contains("dexpress") -> "D Express"
            lowerBody.contains("bex") -> "BEX"
            lowerBody.contains("aks ") || lowerBody.contains("aks-") || lowerBody.contains("aks express") -> "AKS"
            lowerBody.contains("city express") || lowerBody.contains("cityexpress") -> "City Express"
            lowerBody.contains("dhl") -> "DHL"
            else -> null
        }

        for (match in upuTrackingRegex.findAll(body)) {
            val code = match.groupValues[1].replace(" ", "")
            val prefix = code.take(2)
            if (prefix == "RS" && code.length != 13) continue
            val courier = detectedCourier ?: if (code.endsWith("RS")) "Pošta / Post Express" else null
            results.add(
                DetectedPackageTracking(
                    rawText = match.value.trim(),
                    trackingCode = code,
                    courierName = courier
                )
            )
        }

        for (match in contextualTrackingRegex.findAll(body)) {
            val code = match.groupValues[1].trim().trimEnd('.', ',', ';')
            if (code.count(Char::isDigit) >= 5 && results.none { it.trackingCode.equals(code, ignoreCase = true) }) {
                results.add(
                    DetectedPackageTracking(
                        rawText = match.value.trim(),
                        trackingCode = code,
                        courierName = detectedCourier
                    )
                )
            }
        }

        return results
    }

    private fun rangesOverlap(a: IntRange, b: IntRange): Boolean {
        return a.first <= b.last && b.first <= a.last
    }

    /**
     * Extracts phone numbers from an incoming SMS/MMS message body, strictly filtering out:
     * - Substrings inside URLs/links (e.g. hex invoice hashes)
     * - Dates and date+time timestamps (e.g. `05/10/2026 17:42:27`, `01.12.2026.`)
     * - Payment reference numbers ("Poziv na broj" & "Model", e.g. `25600281895057`)
     * - Bank account numbers and IBANs
     * - Masked payment card numbers, monetary amounts, OTP codes, and package tracking codes.
     */
    fun extractPhoneNumbers(body: String): List<String> {
        if (body.isBlank()) return emptyList()

        val excludedRanges = mutableListOf<IntRange>()

        // 1. Exclude all URL matches (including full query strings and hashes)
        urlExtractRegex.findAll(body).forEach { excludedRanges.add(it.range) }

        // 2. Exclude all email matches
        emailRegex.findAll(body).forEach { excludedRanges.add(it.range) }

        // 3. Exclude all date, date+time, month/year, and time-of-day spans
        dateTimeSpanRegex.findAll(body).forEach { excludedRanges.add(it.range) }

        // 4. Exclude all payment reference ("Poziv na broj") spans
        modelAndPozivRegex.findAll(body).forEach { excludedRanges.add(it.range) }
        pozivNaBrojRegex.findAll(body).forEach { excludedRanges.add(it.range) }

        // 5. Exclude all bank account spans
        ibanPattern.findAll(body).forEach { excludedRanges.add(it.range) }
        domesticHyphenPattern.findAll(body).forEach {
            val bankCode = it.groupValues[1]
            val accPart = it.groupValues[2].padStart(13, '0')
            val ctrlPart = it.groupValues[3]
            if (isValidModulo97("$bankCode$accPart$ctrlPart")) {
                excludedRanges.add(it.range)
            }
        }
        continuousBankPattern.findAll(body).forEach {
            val candidate = it.value
            if (SERBIAN_BANKS.containsKey(candidate.substring(0, 3)) && isValidModulo97(candidate)) {
                excludedRanges.add(it.range)
            }
        }

        // 6. Exclude masked card spans, currency amounts, and tracking numbers
        maskedCardSpanRegex.findAll(body).forEach { excludedRanges.add(it.range) }
        currencyAmountSpanRegex.findAll(body).forEach { excludedRanges.add(it.range) }
        upuTrackingRegex.findAll(body).forEach { excludedRanges.add(it.range) }
        contextualTrackingRegex.findAll(body).forEach { excludedRanges.add(it.range) }

        val otpCode = extractOtpOrVerificationCode(body, body.lowercase())

        val matches = mutableListOf<String>()
        for (match in phoneCandidateRegex.findAll(body)) {
            if (excludedRanges.any { rangesOverlap(it, match.range) }) continue

            val raw = match.value.trim().trimEnd('.', ',', ';', ':', '!', '?', ')', '-', '/')
            if (raw.isBlank()) continue
            if (raw.contains('.')) continue

            val slashCount = raw.count { it == '/' }
            if (slashCount > 1) continue
            if (slashCount == 1) {
                val beforeSlash = raw.substringBefore('/').filter(Char::isDigit)
                // In valid phone numbers (e.g. 011/3777-888 or +38164/123456), the prefix before '/' starts with 0 or +
                if (beforeSlash.length !in 3..6 || (!raw.startsWith('+') && !beforeSlash.startsWith('0'))) {
                    continue
                }
            }

            val digits = raw.filter(Char::isDigit)
            if (digits.length !in 7..15) continue
            if (otpCode != null && digits == otpCode) continue

            // Inspect preceding text (up to 32 characters before the match)
            val prefixStart = (match.range.first - 32).coerceAtLeast(0)
            val precedingText = body.substring(prefixStart, match.range.first)
            if (nonPhonePrecedingRegex.containsMatchIn(precedingText)) {
                continue
            }

            val startsWithPlusOrDoubleZero = raw.startsWith('+') || digits.startsWith("00")
            val startsWithValidNationalZero = digits.startsWith("0") &&
                digits.length in 8..11 &&
                digits.getOrNull(1) in setOf('1', '2', '3', '6', '7', '8', '9')
            val hasExplicitPhoneContext = phoneContextPrecedingRegex.containsMatchIn(precedingText)

            if (startsWithPlusOrDoubleZero || startsWithValidNationalZero || hasExplicitPhoneContext) {
                matches.add(raw)
            }
        }
        return matches.distinct()
    }

    val SERBIAN_BANKS = mapOf(
        "105" to "AIK Banka",
        "115" to "Yettel Bank",
        "145" to "Expobank",
        "150" to "Eurobank Direktna",
        "155" to "Halkbank",
        "160" to "Banca Intesa",
        "165" to "Addiko Bank",
        "170" to "UniCredit Bank",
        "190" to "ALTA Banka",
        "200" to "Banka Poštanska štedionica",
        "205" to "NLB Komercijalna Banka",
        "220" to "ProCredit Bank",
        "250" to "Eurobank",
        "265" to "Raiffeisen Banka",
        "295" to "Srpska Banka",
        "325" to "OTP Banka",
        "340" to "Erste Bank",
        "370" to "3 Banka",
        "375" to "API Bank",
        "380" to "ProCredit Bank",
        "385" to "Mirabank",
        "840" to "Uprava za trezor",
        "908" to "Narodna banka Srbije"
    )

    private val ibanPattern = Regex("\\b(RS\\s*[0-9]{2}\\s*[0-9\\s]{16,20})\\b", RegexOption.IGNORE_CASE)
    private val domesticHyphenPattern = Regex("\\b([0-9]{3})\\s*-\\s*([0-9]{1,13})\\s*-\\s*([0-9]{2})\\b")
    private val continuousBankPattern = Regex("\\b([0-9]{18})\\b")

    /**
     * Extracts and validates domestic Serbian bank accounts (18 digits, Modulo 97 verified)
     * as well as IBAN formatted accounts from message text.
     */
    fun extractBankAccounts(body: String): List<DetectedBankAccount> {
        if (body.isBlank()) return emptyList()
        val results = mutableListOf<DetectedBankAccount>()

        // 1. Serbian IBAN format: RS35... (22 chars)
        for (match in ibanPattern.findAll(body)) {
            val raw = match.value.trim()
            val digitsOnly = raw.filter(Char::isDigit)
            if (digitsOnly.length == 20) {
                val fullIban = "RS$digitsOnly"
                val bankCode = fullIban.substring(4, 7)
                val bankName = SERBIAN_BANKS[bankCode]
                results.add(
                    DetectedBankAccount(
                        rawText = raw,
                        formattedAccountNumber = fullIban,
                        standardDigits = fullIban,
                        bankName = bankName,
                        isIban = true
                    )
                )
            }
        }

        // 2. Domestic Serbian bank accounts: 3 digits - 1 to 13 digits - 2 digits
        // e.g. 160-555555-55, 160-0000000555555-55, 265-1100300-22
        for (match in domesticHyphenPattern.findAll(body)) {
            val bankCode = match.groupValues[1]
            val accPart = match.groupValues[2]
            val ctrlPart = match.groupValues[3]

            val paddedAcc = accPart.padStart(13, '0')
            val full18 = "$bankCode$paddedAcc$ctrlPart"
            if (isValidModulo97(full18)) {
                val bankName = SERBIAN_BANKS[bankCode]
                results.add(
                    DetectedBankAccount(
                        rawText = match.value.trim(),
                        formattedAccountNumber = "$bankCode-$paddedAcc-$ctrlPart",
                        standardDigits = full18,
                        bankName = bankName,
                        isIban = false
                    )
                )
            }
        }

        // 3. Continuous 18-digit accounts: e.g. 16000000055555555
        for (match in continuousBankPattern.findAll(body)) {
            val candidate = match.value
            val bankCode = candidate.substring(0, 3)
            if (SERBIAN_BANKS.containsKey(bankCode) && isValidModulo97(candidate)) {
                val bankName = SERBIAN_BANKS[bankCode]
                val formatted = "${candidate.substring(0, 3)}-${candidate.substring(3, 16)}-${candidate.substring(16, 18)}"
                if (results.none { it.standardDigits == candidate }) {
                    results.add(
                        DetectedBankAccount(
                            rawText = candidate,
                            formattedAccountNumber = formatted,
                            standardDigits = candidate,
                            bankName = bankName,
                            isIban = false
                        )
                    )
                }
            }
        }

        return results.distinctBy { it.standardDigits }
    }

    private fun isValidModulo97(eighteenDigits: String): Boolean {
        if (eighteenDigits.length != 18 || !eighteenDigits.all(Char::isDigit)) return false
        var remainder = 0
        for (i in 0 until 18) {
            val digit = eighteenDigits[i] - '0'
            remainder = (remainder * 10 + digit) % 97
        }
        return remainder == 1
    }
}

