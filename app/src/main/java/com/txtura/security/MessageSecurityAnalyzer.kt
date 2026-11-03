package com.txtura.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.BadParcelableException
import android.util.Log
import androidx.core.content.IntentSanitizer
import com.txtura.ui.i18n.TxturaStrings

enum class SecurityRiskLevel {
    SAFE,
    CAUTION,
    SUSPICIOUS
}

data class SecurityAnalysisResult(
    val riskLevel: SecurityRiskLevel,
    val reasons: List<String>,
    val extractedOtpCode: String?
)

/**
 * Multi-layer on-device SMS/MMS Threat & Phishing Intelligence Engine:
 * Inspired by OWASP Phishing/URL Heuristics, Google Safe Browsing URL canonicalization,
 * SpamAssassin/rspamd URI scoring, and CISA/NIST Smishing & AI Scam indicators.
 *
 * Key design principles:
 * 1. Benign HTTPS links to normal websites (e.g. youtube.com, google.com, news, official domains)
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

    private data class ParsedUrl(
        val rawUrl: String,
        val scheme: String,
        val host: String,
        val registeredDomain: String,
        val tld: String,
        val pathAndQuery: String,
        val hasEmbeddedAtSymbol: Boolean,
        val nonStandardPort: Int?
    )

    private val urlExtractRegex = Regex(
        pattern = """(?i)\b((?:https?://|www\.)[^\s<>"]+|[a-z0-9\u0400-\u04FF.-]+\.[a-z\u0400-\u04FF]{2,10}(?:/[^\s<>"]*)?)"""
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
        "bbc.com", "bbc.co.uk", "cnn.com", "nytimes.com", "reuters.com"
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

    // Matches 4-8 digit OTP codes preceded or followed by verification context
    private val otpContextRegex = Regex(
        pattern = """(?i)(?:kod|code|otp|verifikacioni|verification|pin|lozinka|token)\s*(?:je|is|:|-)?\s*\b(\d{4,8})\b|\b(\d{4,8})\b\s*(?:je vaš|is your|kod za|verification code)"""
    )

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

            // 1. Check for '@' credential redirection trick (e.g., https://google.com@evil.com)
            if (url.hasEmbeddedAtSymbol) {
                threatScore += 4
                reasons.add(strings.reasonEmbeddedCredentialsUrl)
            }

            // 2. Check for raw IP address URLs (e.g., http://185.220.101.4/login)
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
            if (matchedMalwareExt != null && !isTrustedDomain) {
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
            if (url.registeredDomain in urlShorteners || url.host in urlShorteners) {
                // Shortener alone is CAUTION (+2); combined with any other indicator becomes SUSPICIOUS (>=3)
                threatScore += 2
                reasons.add(strings.reasonShortener(url.host))
            }

            // 7. Check for High-Risk Abused TLDs strictly on the host TLD (never substring in path!)
            if (!isTrustedDomain && url.tld in highRiskTlds) {
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
            if (url.nonStandardPort != null && url.nonStandardPort !in listOf(80, 443)) {
                threatScore += 3
                reasons.add(strings.reasonIpUrl)
            }
        }

        // 10. Check for Phishing / Smishing Social Engineering Lures
        val hasPhishingKeywords = phishingSocialEngineeringPatterns.any { lowerBody.contains(it) }
        if (hasPhishingKeywords) {
            if (parsedUrls.isNotEmpty()) {
                val allUrlsTrusted = parsedUrls.all {
                    it.registeredDomain in trustedGlobalDomains && !it.hasEmbeddedAtSymbol && it.scheme != "http"
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

        val otpMatch = otpContextRegex.find(body)
        val otpCode = otpMatch?.let { match ->
            match.groups[1]?.value ?: match.groups[2]?.value
        }

        return SecurityAnalysisResult(
            riskLevel = finalRisk,
            reasons = reasons.distinct(),
            extractedOtpCode = otpCode
        )
    }

    private fun extractAndParseUrls(text: String): List<ParsedUrl> {
        return urlExtractRegex.findAll(text).mapNotNull { match ->
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

        val tld = "." + hostParts.last()
        // Ignore common file names in prose like "photo.jpg" when no scheme or slash is present
        if (!lower.startsWith("http") && !lower.startsWith("www.") && pathAndQuery.isEmpty()) {
            if (tld in setOf(".jpg", ".jpeg", ".png", ".pdf", ".txt", ".doc", ".mp4", ".mp3")) {
                return null
            }
        }

        val registeredDomain = if (hostParts.size >= 3 &&
            hostParts[hostParts.size - 2] in setOf("co", "com", "org", "gov", "ac", "net") &&
            hostParts.last().length == 2
        ) {
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
        val hasLatin = host.any { it in 'a'..'z' }
        val hasCyrillic = host.any { it in '\u0400'..'\u04FF' }
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
     * Strictly sanitizes a recipient phone number or short code to prevent injection or malformed URIs.
     */
    fun sanitizePhoneNumber(rawInput: String): String {
        val cleaned = rawInput.trim()
            .filterIndexed { index, ch ->
                ch.isDigit() || (index == 0 && ch == '+')
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
}
