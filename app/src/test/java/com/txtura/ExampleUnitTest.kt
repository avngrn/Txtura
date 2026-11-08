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
}
