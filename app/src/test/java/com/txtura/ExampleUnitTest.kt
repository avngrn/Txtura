package com.txtura

import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import com.txtura.data.WallpaperTexture
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.ui.i18n.TxturaStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun testSerbianStringsHaveNoAmpersand() {
        val serbian = TxturaStrings.forLocale(Locale.forLanguageTag("sr-RS"))
        // Check key descriptions and categories
        assertFalse(serbian.secItem2Title.contains("&"))
        assertTrue(serbian.secItem2Title.contains(" i "))
    }

    @Test
    fun testWallpaperConfigSerializationWithCustomAccent() {
        val customConfig = WallpaperConfig(
            preset = WallpaperPreset.DEFAULT,
            customImageUri = "content://media/external/images/media/42",
            texture = WallpaperTexture.AUTO_THEME,
            customAccentColorHex = 0xFF3B82F6L
        )

        assertEquals(WallpaperTexture.NONE, customConfig.resolvedTexture)
        assertTrue(customConfig.isCustomGalleryImage)

        val serialized = customConfig.serialize()
        assertTrue(serialized.contains("custom:content://media/external/images/media/42"))
        assertTrue(serialized.contains("|acc:ff3b82f6"))

        val deserialized = WallpaperConfig.deserialize(serialized)
        assertEquals("content://media/external/images/media/42", deserialized.customImageUri)
        assertEquals(0xFF3B82F6L, deserialized.customAccentColorHex)
        assertEquals(WallpaperTexture.NONE, deserialized.resolvedTexture)
    }

    @Test
    fun testBankAccountExtractionAndValidation() {
        val acc13 = "12345".padStart(13, '0')
        val base16 = "160$acc13"
        var rem = 0
        for (ch in base16) {
            rem = (rem * 10 + (ch - '0')) % 97
        }
        val rem100 = (rem * 100) % 97
        var ctrl = 98 - rem100
        if (ctrl == 98) ctrl = 1
        val ctrlStr = ctrl.toString().padStart(2, '0')
        val full18 = base16 + ctrlStr
        var fullRem = 0
        for (ch in full18) {
            fullRem = (fullRem * 10 + (ch - '0')) % 97
        }
        val validAccountHyphen = "160-$acc13-$ctrlStr"

        val messageText = "Uplati novac na račun $validAccountHyphen ili pozovi 0641234567."
        val detected = MessageSecurityAnalyzer.extractBankAccounts(messageText)

        assertEquals(1, detected.size)
        val acc = detected[0]
        assertEquals("160-0000000012345-$ctrlStr", acc.formattedAccountNumber)
        assertEquals("Banca Intesa", acc.bankName)
        assertFalse(acc.isIban)

        // Invalid account should not be detected
        val invalidText = "Pogrešan račun 160-0000000012345-99."
        val invalidDetected = MessageSecurityAnalyzer.extractBankAccounts(invalidText)
        assertTrue(invalidDetected.isEmpty())
    }

    @Test
    fun testSerbianIbanExtraction() {
        val ibanText = "Evo IBAN za uplatu: RS35 160 0000000012345 32 hvala"
        val detected = MessageSecurityAnalyzer.extractBankAccounts(ibanText)

        assertEquals(1, detected.size)
        val acc = detected[0]
        assertTrue(acc.isIban)
        assertEquals("Banca Intesa", acc.bankName)
        assertTrue(acc.formattedAccountNumber.startsWith("RS"))
    }

    @Test
    fun testGlobalCatastropheDetection() {
        assertTrue(com.txtura.telephony.EmergencyAlertManager.isGlobalCatastropheText("Uzbuna: Globalna katastrofa aktivirana!"))
        assertTrue(com.txtura.telephony.EmergencyAlertManager.isGlobalCatastropheText("Opasnost od tsunamija u priobalnom pojasu"))
        assertTrue(com.txtura.telephony.EmergencyAlertManager.isGlobalCatastropheText("Planetary crisis declared by international council"))
        assertFalse(com.txtura.telephony.EmergencyAlertManager.isGlobalCatastropheText("Lokalni radovi na putu i zastoj"))
    }

    @Test
    fun testMmsNotificationIndParsing() {
        // Construct binary m-notification-ind (0x82) PDU
        val out = java.io.ByteArrayOutputStream()
        out.write(0x8C); out.write(0x82) // Message-Type: m-notification-ind
        out.write(0x98); out.write("TX123".toByteArray(Charsets.US_ASCII)); out.write(0x00) // Transaction-ID
        out.write(0x8D); out.write(0x92) // Version: 1.2
        // From: length 22, token 0x80, address "+38164123456/TYPE=PLMN\0"
        val fromAddr = "+38164123456/TYPE=PLMN".toByteArray(Charsets.US_ASCII)
        out.write(0x89)
        out.write(fromAddr.size + 2) // length
        out.write(0x80) // Address-present-token
        out.write(fromAddr)
        out.write(0x00)
        // Content-Location
        val url = "http://mms.carrier.rs/servlets/mms?id=456"
        out.write(0x8A)
        out.write(url.toByteArray(Charsets.US_ASCII))
        out.write(0x00)
        // Message-Size: 2 bytes, 1024
        out.write(0x8E)
        out.write(0x02)
        out.write(0x04); out.write(0x00)

        val bytes = out.toByteArray()
        val notif = com.txtura.telephony.MmsPduHelper.parseNotificationInd(bytes)

        assertNotNull(notif)
        assertEquals("TX123", notif!!.transactionId)
        assertEquals(url, notif.contentLocation)
        assertEquals("+38164123456", notif.senderAddress)
        assertEquals(1024L, notif.messageSize)
    }

    @Test
    fun testMmsRetrieveConfParsing() {
        val out = java.io.ByteArrayOutputStream()
        out.write(0x8C); out.write(0x84) // Message-Type: m-retrieve-conf
        out.write(0x98); out.write("RET99".toByteArray(Charsets.US_ASCII)); out.write(0x00)
        out.write(0x8D); out.write(0x92)
        out.write(0x85); out.write(0x04); out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x00) // Date
        // From
        val fromAddr = "064998877/TYPE=PLMN".toByteArray(Charsets.US_ASCII)
        out.write(0x89)
        out.write(fromAddr.size + 2)
        out.write(0x80)
        out.write(fromAddr)
        out.write(0x00)
        // Content-Type: application/vnd.wap.multipart.related
        out.write(0x84)
        out.write("application/vnd.wap.multipart.related".toByteArray(Charsets.US_ASCII))
        out.write(0x00)

        // Multipart: 2 parts (text + fake image)
        out.write(0x02) // 2 parts

        // Part 1: text/plain
        val textBytes = "Zdravo ovo je probni MMS!".toByteArray(Charsets.UTF_8)
        val h1 = java.io.ByteArrayOutputStream()
        h1.write("text/plain".toByteArray(Charsets.US_ASCII)); h1.write(0x00)
        val h1Bytes = h1.toByteArray()
        out.write(h1Bytes.size) // header len
        out.write(textBytes.size) // data len
        out.write(h1Bytes)
        out.write(textBytes)

        // Part 2: image/jpeg with JPEG magic bytes (0xFF, 0xD8, 0xFF, ...)
        val imgBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x12, 0x34, 0xFF.toByte(), 0xD9.toByte())
        val h2 = java.io.ByteArrayOutputStream()
        h2.write("image/jpeg".toByteArray(Charsets.US_ASCII)); h2.write(0x00)
        val h2Bytes = h2.toByteArray()
        out.write(h2Bytes.size)
        out.write(imgBytes.size)
        out.write(h2Bytes)
        out.write(imgBytes)

        val pdu = out.toByteArray()
        val retrieved = com.txtura.telephony.MmsPduHelper.parseRetrieveConf(pdu)

        assertNotNull(retrieved)
        assertEquals("064998877", retrieved!!.senderAddress)
        assertEquals("Zdravo ovo je probni MMS!", retrieved.bodyText)
        assertNotNull(retrieved.imageBytes)
        assertEquals("image/jpeg", retrieved.imageMimeType)
    }

    @Test
    fun testVoiceMmsRetrieveConfParsing() {
        val out = java.io.ByteArrayOutputStream()
        out.write(0x8C); out.write(0x84) // Message-Type: m-retrieve-conf
        out.write(0x98); out.write("VOICE1".toByteArray(Charsets.US_ASCII)); out.write(0x00)
        out.write(0x8D); out.write(0x92)
        out.write(0x85); out.write(0x04); out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x00)
        // From
        val fromAddr = "060112233/TYPE=PLMN".toByteArray(Charsets.US_ASCII)
        out.write(0x89)
        out.write(fromAddr.size + 2)
        out.write(0x80)
        out.write(fromAddr)
        out.write(0x00)
        // Content-Type: application/vnd.wap.multipart.related
        out.write(0x84)
        out.write("application/vnd.wap.multipart.related".toByteArray(Charsets.US_ASCII))
        out.write(0x00)

        // Multipart: 1 part (audio/amr)
        out.write(0x01) // 1 part

        // Part 1: audio/amr with AMR header "#!AMR\n"
        val audioData = byteArrayOf(0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A, 0x3C, 0x4D, 0x5E)
        val h1 = java.io.ByteArrayOutputStream()
        h1.write("audio/amr".toByteArray(Charsets.US_ASCII)); h1.write(0x00)
        val h1Bytes = h1.toByteArray()
        out.write(h1Bytes.size)
        out.write(audioData.size)
        out.write(h1Bytes)
        out.write(audioData)

        val pdu = out.toByteArray()
        val retrieved = com.txtura.telephony.MmsPduHelper.parseRetrieveConf(pdu)

        assertNotNull(retrieved)
        assertEquals("060112233", retrieved!!.senderAddress)
        assertNotNull(retrieved.audioBytes)
        assertEquals("audio/amr", retrieved.audioMimeType)
    }

    @Test
    fun testMmsImageCompressorConstantsAndLimit() {
        assertEquals(299_900L, com.txtura.telephony.MmsImageCompressor.MAX_MMS_IMAGE_BYTES)
        assertEquals(299_900L, com.txtura.telephony.VoiceMessageManager.MAX_VOICE_MMS_BYTES)
    }

    @Test
    fun testVoiceMessageDurationFormatting() {
        assertEquals("0:00", com.txtura.telephony.VoiceMessageManager.formatDuration(0L))
        assertEquals("0:05", com.txtura.telephony.VoiceMessageManager.formatDuration(5_000L))
        assertEquals("1:00", com.txtura.telephony.VoiceMessageManager.formatDuration(60_000L))
        assertEquals("3:16", com.txtura.telephony.VoiceMessageManager.formatDuration(196_000L))
    }

    @Test
    fun testFlashNotificationManager() {
        assertEquals(35L, com.txtura.telephony.FlashNotificationManager.LED_HARDWARE_DELAY_COMPENSATION_MS)
        val serbian = TxturaStrings.forLocale(Locale.forLanguageTag("sr-RS"))
        assertEquals("Svetlosni odziv", serbian.flashAlertTitle)
        assertTrue(serbian.flashAlertSub.contains("sinhronizovano"))
        assertEquals("Zvučni odzivi", serbian.inAppSoundsTitle)
        assertEquals("Uvek", serbian.flashAlertModeAlways)
        assertEquals("U režimu samo vibracija", serbian.flashAlertModeVibrate)
        assertEquals("U režimu nečujno", serbian.flashAlertModeSilent)
        assertEquals("U režimu zvuk", serbian.flashAlertModeSound)

        assertTrue(com.txtura.data.FlashAlertMode.ALWAYS.matchesRingerMode(android.media.AudioManager.RINGER_MODE_NORMAL))
        assertTrue(com.txtura.data.FlashAlertMode.ALWAYS.matchesRingerMode(android.media.AudioManager.RINGER_MODE_VIBRATE))
        assertTrue(com.txtura.data.FlashAlertMode.ALWAYS.matchesRingerMode(android.media.AudioManager.RINGER_MODE_SILENT))

        assertTrue(com.txtura.data.FlashAlertMode.VIBRATE_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_VIBRATE))
        assertFalse(com.txtura.data.FlashAlertMode.VIBRATE_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_NORMAL))
        assertFalse(com.txtura.data.FlashAlertMode.VIBRATE_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_SILENT))

        assertTrue(com.txtura.data.FlashAlertMode.SILENT_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_SILENT))
        assertFalse(com.txtura.data.FlashAlertMode.SILENT_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_NORMAL))
        assertFalse(com.txtura.data.FlashAlertMode.SILENT_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_VIBRATE))

        assertTrue(com.txtura.data.FlashAlertMode.SOUND_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_NORMAL))
        assertFalse(com.txtura.data.FlashAlertMode.SOUND_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_VIBRATE))
        assertFalse(com.txtura.data.FlashAlertMode.SOUND_ONLY.matchesRingerMode(android.media.AudioManager.RINGER_MODE_SILENT))
    }

    @Test
    fun testDockBarItemSerializationAndReorder() {
        val defaultList = com.txtura.data.DockBarItem.defaultOrder
        assertEquals(5, defaultList.size)
        val serialized = com.txtura.data.DockBarItem.serializeOrder(defaultList)
        val deserialized = com.txtura.data.DockBarItem.deserializeOrder(serialized)
        assertEquals(defaultList, deserialized)

        // Custom reorder: move SETTINGS to the first position
        val customOrder = listOf(
            com.txtura.data.DockBarItem.SETTINGS,
            com.txtura.data.DockBarItem.MARK_ALL_READ,
            com.txtura.data.DockBarItem.HIGHLIGHTED,
            com.txtura.data.DockBarItem.NEW_MESSAGE,
            com.txtura.data.DockBarItem.DELETED,
        )
        val customSerialized = com.txtura.data.DockBarItem.serializeOrder(customOrder)
        val customDeserialized = com.txtura.data.DockBarItem.deserializeOrder(customSerialized)
        assertEquals(customOrder, customDeserialized)
    }

    @Test
    fun testSmscMmsApnDataModel() {
        val apn = com.txtura.data.MmsApnSettings(
            carrierName = "mts Srbija",
            mmscUrl = "http://mms.mts.rs/servlets/mms",
            mmsProxy = "172.17.88.198",
            mmsPort = 8080,
            source = "SMSC Auto-detect (+38164)"
        )
        assertEquals("mts Srbija", apn.carrierName)
        assertEquals("http://mms.mts.rs/servlets/mms", apn.mmscUrl)
        assertEquals("172.17.88.198", apn.mmsProxy)
        assertEquals(8080, apn.mmsPort)
    }

    @Test
    fun testA1BillMessageSmartEntityDetectionWithoutFalsePhones() {
        val a1Sms = "Racun kreiran u mesecu 11/2026 iznosi 3628.99 RSD. Uplatu treba da izvrsite do " +
            "01.12.2026. na racun 265-1110312345678-24, model 97, poziv na broj 25600281895057. " +
            "Racun mozete preuzeti sa https://a1.rs/bill?invoice=b0a6694d553608aca80402dc35dda3611cb695f19067a7558855268b6371f2cb, " +
            "a kreirane racune i potrosnju mozete da pratite na Moj A1 aplikaciji. Ako vec niste, " +
            "preuzmite aplikaciju sa onelink.to/moja1 . Vas A1"

        // 1. No false phone numbers from URL hex digits, payment reference, or dates
        val phones = MessageSecurityAnalyzer.extractPhoneNumbers(a1Sms)
        assertTrue("Expected no phone numbers in A1 bill SMS, but got: $phones", phones.isEmpty())

        // 2. Payment reference ("poziv na broj") & model 97 properly extracted
        val paymentRefs = MessageSecurityAnalyzer.extractPaymentReferences(a1Sms)
        assertEquals(1, paymentRefs.size)
        assertEquals("25600281895057", paymentRefs[0].referenceNumber)
        assertEquals("97", paymentRefs[0].model)

        // 3. Payment amount properly extracted
        val amounts = MessageSecurityAnalyzer.extractPaymentAmounts(a1Sms)
        assertEquals(1, amounts.size)
        assertEquals("3628.99", amounts[0].normalizedValue)
        assertEquals("RSD", amounts[0].currency)

        // 4. Payment deadline date properly extracted
        val dates = MessageSecurityAnalyzer.extractActionableDates(a1Sms)
        assertEquals(1, dates.size)
        assertEquals("01.12.2026.", dates[0].displayDate)
        assertTrue(dates[0].isPaymentDeadline)

        // 5. Safe links properly extracted (a1.rs and onelink.to)
        val links = MessageSecurityAnalyzer.extractSafeLinks(a1Sms)
        assertEquals(2, links.size)
    }

    @Test
    fun testUniCreditOtpMessageNoFalseLinkOnBrKarticeAndNoFalseDatePhone() {
        val uniCreditOtpSms = "Unesite jednokratni kod:208042, br.kartice*5431 **** **** 6652, " +
            "za potvrdu transakcije u iznosu od 6562.47 RSD kod trgovca A1 Srbija doo Beograd, " +
            "dana 05/10/2026 17:42:27"

        // 1. "br.kartice" must NOT be recognized as a URL
        val links = MessageSecurityAnalyzer.extractSafeLinks(uniCreditOtpSms)
        assertTrue("Expected no links for br.kartice, got: $links", links.isEmpty())

        // 2. "05/10/2026 17" must NOT be recognized as a phone number
        val phones = MessageSecurityAnalyzer.extractPhoneNumbers(uniCreditOtpSms)
        assertTrue("Expected no phones for date/time, got: $phones", phones.isEmpty())

        // 3. OTP code 208042 must still be extracted
        val analysis = MessageSecurityAnalyzer.analyzeMessage(uniCreditOtpSms)
        assertEquals("208042", analysis.extractedOtpCode)

        // 4. Transaction amount 6562.47 RSD must be extracted
        val amounts = MessageSecurityAnalyzer.extractPaymentAmounts(uniCreditOtpSms)
        assertEquals(1, amounts.size)
        assertEquals("6562.47", amounts[0].normalizedValue)
    }

    @Test
    fun testUniCreditCardPaymentRealPhoneAndAmountWithoutDateFalsePositive() {
        val paymentSms = "10/11/2026 22:46:31 PLACANJE MC **6652 Iznos 937,99 RSD Mesto " +
            "MIKROMARKET NS, , RS Raspolozivo 42475,30RSD Kontakt centar 0113777888"

        // 1. Only the real call-center phone 0113777888 should be detected, NOT "10/11/2026 22"
        val phones = MessageSecurityAnalyzer.extractPhoneNumbers(paymentSms)
        assertEquals(listOf("0113777888"), phones)

        // 2. Only transaction "Iznos 937,99 RSD" should be extracted, NOT "Raspolozivo 42475,30RSD"
        val amounts = MessageSecurityAnalyzer.extractPaymentAmounts(paymentSms)
        assertEquals(1, amounts.size)
        assertEquals("937.99", amounts[0].normalizedValue)
        assertEquals("RSD", amounts[0].currency)
    }

    @Test
    fun testPackageTrackingExtraction() {
        val postExpressSms = "Vasa posiljka PE123456789RS je pristigla i bice urucena danas. Kontakt kurira: 0641122334"
        val tracking = MessageSecurityAnalyzer.extractPackageTrackingNumbers(postExpressSms)
        assertEquals(1, tracking.size)
        assertEquals("PE123456789RS", tracking[0].trackingCode)
        assertEquals("Pošta / Post Express", tracking[0].courierName)

        val phones = MessageSecurityAnalyzer.extractPhoneNumbers(postExpressSms)
        assertEquals(listOf("0641122334"), phones)
    }
}

