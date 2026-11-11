@file:Suppress("SpellCheckingInspection", "unused")

package com.txtura.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

class AppStrings(val isSerbian: Boolean) {
    private fun s(en: String, sr: String): String = if (isSerbian) sr else en

    val settingsTooltip: String = s("Settings", "Podešavanja")
    val searchPlaceholder: String = s(
        "Search messages, contacts or codes",
        "Pretraži poruke, kontakte ili kodove",
    )
    val clearSearch: String = s("Clear search", "Obriši pretragu")
    val filterPeople: String = s("People", "Osobe")
    val filterServices: String = s("Services", "Usluge")
    val filterUnread: String = s("Unread", "Nepročitano")
    val filterAll: String = s("All", "Sve")
    val newMessage: String = s("New Message", "Nova poruka")
    val markAllReadTooltip: String = s("Mark all as read", "Označi sve kao pročitano")
    val confirmMarkAllReadTitle: String = s("Mark all messages as read?", "Označi sve kao pročitano?")
    val confirmMarkAllReadMessage: String = s(
        "Are you sure you want to mark all conversations and messages as read?",
        "Da li želite da označite sve poruke kao pročitane?",
    )
    val confirmMarkAllReadButton: String = s("Mark as read", "Označi kao pročitano")
    val pinContactThread: String = s("Pin conversation", "Prikači kontakt")
    val unpinContactThread: String = s("Unpin conversation", "Otkači kontakt")
    val pinMessage: String = s("Pin message", "Prikači poruku")
    val unpinMessage: String = s("Unpin message", "Otkači poruku")
    val pinnedMessagesTitle: String = s("Pinned messages", "Prikačene poruke")
    val unpinAllMessages: String = s("Unpin all", "Otkači sve")
    val unpinSelectedMessages: (Int) -> String = { count ->
        if (isSerbian) "Otkači ($count)" else "Unpin ($count)"
    }
    val managePinnedMessagesSub: String = s(
        "Select messages you want to unpin",
        "Odaberite poruke koje želite da otkačite",
    )
    val highlightedMessagesTitle: String = s("Highlighted", "Hajlajtovano")
    val allHighlightedMessagesTitle: String = s("Highlighted Messages", "Hajlajtovane poruke")
    val highlightedMessagesTooltip: String = s("Highlighted messages from all contacts", "Hajlajtovane poruke od svih kontakata")
    val noHighlightedMessages: String = s("No highlighted messages yet", "Još uvek nema hajlajtovanih poruka")
    val noHighlightedMessagesSub: String = s(
        "Long-press any message in a chat and tap Highlight to save and view it here.",
        "Zadržite dodir na poruci u razgovoru i odaberite Hajlajtuj da biste je videli ovde.",
    )
    val openInChatAction: String = s("Open in chat", "Otvori u razgovoru")
    val unhighlightMessageAction: String = s("Unhighlight", "Otkaži hajlajt")
    val callSender: String = s("Call sender", "Pozovi pošiljaoca")
    val viewContactDetails: String = s("Contact details", "Podaci o kontaktu")
    val pickFromContacts: String = s("Pick contact", "Izaberi iz kontakata")
    val copyCodePrefix: String = s("Copy code: ", "Kopiraj kod: ")
    val copyVerificationCodePrefix: String = s("Copy verification code: ", "Kopiraj verifikacioni kod: ")
    val codeCopiedToast: (String) -> String = { code ->
        if (isSerbian) "Kod $code bezbedno kopiran" else "Code $code copied securely"
    }
    val shieldSuspiciousBlocked: String = s(
        "Shield: Suspicious phishing link flagged",
        "Štit: Sumnjiv phishing link blokiran",
    )
    val back: String = s("Back", "Nazad")
    val protectedChannelSuffix: String = s("Protected conversation", "Zaštićena konverzacija")
    val mmsAttachedTitle: String = s("MMS Photo attached", "MMS Fotografija priložena")
    val mmsZeroPermissionSub: String = s(
        "Zero-Permission Photo Picker (scoped access only)",
        "Zero-Permission Photo Picker (bez pristupa celoj galeriji)",
    )
    val mmsAutoCompressedSub: String = s(
        "Auto-compressed to 299.9 kB",
        "Automatski kompresovano na 299.9 kB",
    )
    val removeMmsAttachment: String = s("Remove MMS attachment", "Ukloni MMS prilog")
    val attachMmsImage: String = s("Attach MMS image", "Dodaj MMS sliku")
    val attachCalendarEvent: String = s("Attach calendar event", "Dodaj kalendarski događaj")
    val attachDocumentFile: String = s("Attach document file", "Dodaj dokument fajl")
    val calendarEventTitle: String = s("Calendar event", "Kalendarski događaj")
    val calendarEventSub: String = s("vCalendar (.ics) • max 299.9 kB", "vCalendar (.ics) • max 299.9 kB")
    val documentFileTitle: String = s("Document file", "Dokument fajl")
    val documentFileSub: String = s("Document • max 299.9 kB", "Dokument • max 299.9 kB")
    val calendarPermissionRequired: String = s(
        "Calendar permission is required to select events.",
        "Dozvola za kalendar je neophodna za izbor događaja."
    )
    val calendarPickerTitle: String = s("Choose Calendar Event", "Izaberi kalendarski događaj")
    val noCalendarEventsFound: String = s("No upcoming events found in calendar.", "Nema pronađenih događaja u kalendaru.")
    val quickInviteOption: String = s("Or create a quick invitation", "Ili kreiraj brzu pozivnicu")
    val addMmsCaptionPlaceholder: String = s("Add MMS caption", "Dodaj opis uz MMS")
    val writeSmsPlaceholder: String = s("Write an SMS message", "Napiši SMS poruku")
    val mmsModeIndicator: String = s("MMS Mode • Multimedia message", "MMS režim • Multimedijalna poruka")
    val voiceMessageTitle: String = s("Voice message", "Govorna poruka")
    val voiceMessageTooltip: String = s("Voice message (hold to record)", "Govorna poruka (zadržite za snimanje)")
    val voiceMessageSlideToCancel: String = s("Slide right to cancel >", "Prevucite udesno za poništavanje >")
    val voiceMessageRecording: String = s("Recording...", "Snimanje...")
    val voiceMessageTimeRemaining: (String) -> String = { rem -> if (isSerbian) "Preostalo: $rem" else "Remaining: $rem" }
    val voiceMessagePermissionRequired: String = s(
        "Microphone permission is required to record voice messages.",
        "Dozvola za mikrofon je neophodna za snimanje glasovnih poruka."
    )
    val send: String = s("Send", "Pošalji")
    val cancel: String = s("Cancel", "Poništi")
    val next: String = s("Next", "Dalje")
    val enterNameOrPhoneNumber: String = s("Enter name or phone number", "Unesi ime ili broj telefona")
    val confirm: String = s("Confirm", "Potvrdi")
    val save: String = s("Save", "Sačuvaj")
    val closeDialog: String = s("Close", "Zatvori")
    val toRecipientPrefix: String = s("To:", "Za:")
    val newSmsMmsDialogTitle: String = s("Choose Recipient", "Izaberi primaoca")
    val searchOrEnterNumberPlaceholder: String = s(
        "Search contact or type phone number",
        "Pretraži kontakt ili unesi broj telefona",
    )
    val openChatWithNumberButton: (String) -> String = { num ->
        if (isSerbian) "Otvori čet sa: $num" else "Open conversation with $num"
    }
    val existingChatBadge: String = s("Continue chat", "Nastavi čet")
    val newChatBadge: String = s("New chat", "Nova konverzacija")
    val recipientLabel: String = s("Recipient or phone number", "Primalac ili broj telefona")
    val hdrMmsPhoto: String = s("HDR MMS Photo", "HDR MMS Fotografija")

    // Chat Message Long-Press Options
    val messageOptionsTitle: String = s("Message Options", "Opcije poruke")
    val forwardMessageOption: String = s("Forward message", "Prosledi poruku")
    val deleteMessageOption: String = s("Delete message", "Izbriši poruku")
    val copyMessageOption: String = s("Copy message", "Kopiraj poruku")
    val highlightMessageOption: String = s("Highlight message", "Hajlajtuj poruku")
    val unhighlightMessageOption: String = s("Remove highlight", "Ukloni hajlajt")
    val confirmDeleteSingleMessageTitle: String = s("Delete message?", "Izbriši poruku?")
    val confirmDeleteSingleMessageBody: String = s(
        "Do you want to move this message to Deleted messages?",
        "Da li želite da premestite ovu poruku u obrisane poruke?",
    )
    val forwardMessageDialogTitle: String = s("Forward message to", "Prosledi poruku osobi")
    val messageCopiedToast: String = s("Message copied to clipboard", "Poruka je kopirana")
    val messageDeletedToast: String = s("Message moved to Deleted messages", "Poruka je premeštena u obrisane poruke")
    val messageForwardedToast: (String) -> String = { name ->
        if (isSerbian) "Poruka prosleđena za: $name" else "Message forwarded to $name"
    }

    // Main Screen Selection Mode & Top Actions
    val selectedThreadsCountLabel: (Int) -> String = { count ->
        if (isSerbian) "Označeno: $count" else "$count selected"
    }
    val bottomBarDeleteTooltip: String = s("Delete", "Izbriši")
    val bottomBarMarkReadTooltip: String = s("Mark as read", "Označi kao pročitano")
    val bottomBarSelectAllTooltip: String = s("Select all", "Označi sve")
    val bottomBarDeselectAllTooltip: String = s("Deselect all", "Odznači sve")
    val confirmBatchDeleteTitle: String = s("Delete selected messages?", "Izbriši označene poruke?")
    val confirmBatchDeleteMessage: (Int, Int) -> String = { count, days ->
        if (isSerbian) {
            "Da li želite da izbrišete označene poruke ($count)? Biće premeštene u Obrisane poruke i trajno uklonjene nakon $days dana."
        } else {
            "Do you want to move $count selected conversation(s) to Trash? They will be permanently deleted after $days days."
        }
    }
    val confirmBatchMarkReadTitle: String = s("Mark selected as read?", "Označi kao pročitano?")
    val confirmBatchMarkReadMessage: (Int) -> String = { count ->
        if (isSerbian) {
            "Da li želite da označite označene poruke ($count) kao pročitane?"
        } else {
            "Do you want to mark all messages in the $count selected conversation(s) as read?"
        }
    }
    val threadsDeletedToast: (Int) -> String = { count ->
        if (isSerbian) "Premešteno u obrisane poruke: $count" else "Moved $count conversation(s) to Deleted Messages"
    }
    val threadsMarkedReadToast: (Int) -> String = { count ->
        if (isSerbian) "Označeno kao pročitano: $count" else "Marked $count conversation(s) as read"
    }
    val messagesPinnedToast: (Int) -> String = { count ->
        if (isSerbian) "Prikačeno poruka: $count" else "$count message(s) pinned"
    }
    val messagesUnpinnedToast: (Int) -> String = { count ->
        if (isSerbian) "Otkačeno poruka: $count" else "$count message(s) unpinned"
    }
    val messagesHighlightedToast: (Int) -> String = { count ->
        if (isSerbian) "Hajlajtovano poruka: $count" else "$count message(s) highlighted"
    }
    val messagesUnhighlightedToast: (Int) -> String = { count ->
        if (isSerbian) "Uklonjen hajlajt za: $count" else "Highlight removed for $count"
    }
    val allMessagesMarkedReadToast: String = s(
        "All messages marked as read",
        "Sve poruke su označene kao pročitane",
    )

    // Settings Screen Categories
    val settingsCategoryAppearance: String = s("Appearance & Display", "Izgled i prilagođavanje")
    val settingsCategoryNotifications: String = s("Notifications, Sounds & Haptics", "Obaveštenja, zvuk i odziv")
    val settingsCategoryChat: String = s("Chat & Gestures", "Ponašanje četa i interakcije")
    val settingsCategoryMessaging: String = s("Messaging & Network", "SMS i MMS podešavanja")
    val settingsCategoryData: String = s("Data & Storage", "Podaci i skladište")
    val settingsCategorySystem: String = s("System & About", "Sistem i aplikacija")
    val settingsCategoryPrivacy: String = s("Privacy & Security", "Privatnost i bezbednost")

    // Privacy & Security Details
    val dbEncryptionTitle: String = s("Encrypted Local Database", "Šifrovana lokalna baza")
    val dbEncryptionSub: String = s(
        "Room data is always encrypted with SQLCipher. This option adds AES-256-GCM encryption to the stored conversation snapshot; its key is protected by Android Keystore.",
        "Room baza je uvek šifrovana SQLCipher-om. Ova opcija dodaje AES-256-GCM šifrovanje snapshot-a razgovora, a ključ je zaštićen Android Keystore-om.",
    )
    val dbEncryptionBadge: (Boolean) -> String = { aesLayerEnabled ->
        when {
            isSerbian && aesLayerEnabled -> "SQLCipher + AES-256-GCM"
            isSerbian -> "SQLCipher aktivan"
            aesLayerEnabled -> "SQLCipher + AES-256-GCM"
            else -> "SQLCipher active"
        }
    }
    val dbEncryptionSwitchLabel: String = s(
        "Add AES-256-GCM payload layer",
        "Dodaj AES-256-GCM sloj za podatke",
    )
    val biometricLockTitle: String = s(
        "Biometric Protection & PIN",
        "Biometrijska zaštita i PIN",
    )
    val biometricLockSub: String = s(
        "Enabling app locking with fingerprint, PIN, or device password",
        "Omogućavanje zaključavanja same aplikacije otiskom prsta, PIN-om ili lozinkom uređaja",
    )
    val biometricLockEnabledLabel: String = s("Lock App When Closed", "Zaključavanje aplikacije pri izlasku")
    val sslPinningTitle: String = s(
        "HTTPS & TLS/SSL with Pinning",
        "HTTPS i TLS/SSL sa Pinning-om",
    )
    val sslPinningSub: String = s(
        "Strict encrypted connections with certificate pinning to prevent Man-in-the-Middle (MitM) attacks.",
        "Striktna upotreba šifrovanih veza uz implementaciju sertifikat pinning-a kako bi se sprečili Man-in-the-Middle (MitM) napadi.",
    )
    val sslPinningBadge: String = s("TLS 1.3 / Pinning Active", "TLS 1.3 / Pinning Aktivan")
    val sslPinningSwitchLabel: String = s("Enable SSL Pinning", "Aktiviraj SSL Pinning")
    val metadataMinimizationTitle: String = s("Metadata Minimization", "Minimizacija metapodataka")
    val metadataMinimizationSub: String = s(
        "Retain minimal information about sender, recipient, timestamp and location.",
        "Čuvanje što manje informacija o tome ko šalje poruku kome, kada i odakle.",
    )
    val metadataMinimizationBadge: String = s("Zero Metadata Active", "Zero Metadata Aktivno")
    val metadataMinimizationSwitchLabel: String = s("Zero Metadata Policy", "Politika nultih metapodataka")
    val rootDetectionTitle: String = s("Root State Detection", "Detekcija \"Root\" stanja")
    val rootDetectionSub: String = s(
        "Check for common signs of a modified operating system. This is a best-effort check, not a guarantee of device integrity.",
        "Provera uobičajenih znakova izmenjenog operativnog sistema. Provera je informativna i ne garantuje integritet uređaja.",
    )
    val rootDetectionSwitchLabel: String = s("Enable root checks", "Uključi proveru root stanja")
    val rootDetectionSwitchSub: String = s(
        "Scan when this settings page opens or when you tap re-scan.",
        "Skeniranje pri otvaranju ove stranice ili kada dodirnete ponovnu proveru.",
    )
    val spamFirewallTitle: String = s("Blocked Sender Firewall", "Zaštitni zid blokiranih pošiljalaca")
    val spamFirewallSub: String = s(
        "New SMS from blocked senders can be discarded before Txtura saves them; blocked MMS notifications are ignored and not downloaded. Turning this off does not recover discarded messages. The list is retained.",
        "Nove SMS poruke blokiranih pošiljalaca mogu biti odbačene pre nego što ih Txtura sačuva; MMS obaveštenja se ignorišu i sadržaj se ne preuzima. Isključivanje opcije ne vraća odbačene poruke. Lista ostaje sačuvana.",
    )
    val rootStateSafe: String = s(
        "No common root indicators found (best-effort check; not proof of device integrity)",
        "Nisu pronađeni uobičajeni root indikatori (orijentaciona provera; nije dokaz integriteta uređaja)",
    )
    val rootStateCompromised: String = s("Warning: Common root indicators found", "Upozorenje: Pronađeni uobičajeni root indikatori")
    val rootCheckBinarySafe: String = s("SU binary: Not found in checked paths", "SU fajl: Nije pronađen na proverenim putanjama")
    val rootCheckBinaryRisk: String = s("SU binary: Found", "SU fajl: Pronađen")
    val rootCheckTagsSafe: String = s("Build tags: No test-keys found", "Build oznake: Test-keys nisu pronađeni")
    val rootCheckTagsRisk: String = s("Build tags: Test-keys detected", "Build oznake: Pronađeni test-keys")
    val rootCheckAppsSafe: String = s("Known root managers: None found", "Poznati root menadžeri: Nisu pronađeni")
    val rootCheckAppsRisk: String = s("Known root manager found", "Pronađen poznati root menadžer")
    val rootRescanButton: String = s("Re-check common root indicators", "Ponovo proveri uobičajene root indikatore")
    val rootScanInProgress: String = s("Checking common root indicators...", "Provera uobičajenih root indikatora...")
    val addBlockedSenderTitle: String = s("Block New Number", "Blokiraj novi broj")
    val blockSenderDialogHint: String = s("Enter phone number to block...", "Unesite broj telefona za blokiranje...")
    val blockSenderAction: String = s("Block", "Blokiraj")
    val lockTimeoutTitle: String = s("Lock Timeout", "Vreme do zaključavanja")
    val lockTimeoutImmediate: String = s("Immediately on exit", "Odmah pri izlasku")
    val lockTimeout1Min: String = s("After 1 minute", "Nakon 1 minuta")
    val lockTimeout5Min: String = s("After 5 minutes", "Nakon 5 minuta")
    val lockAppNowButton: String = s("Lock App Now (Test)", "Zaključaj aplikaciju odmah (Test)")
    val dbEncryptionEnabledToast: String = s(
        "AES-256-GCM payload encryption enabled. SQLCipher remains active.",
        "Uključeno je AES-256-GCM šifrovanje podataka. SQLCipher ostaje aktivan.",
    )
    val dbEncryptionDisabledToast: String = s(
        "AES-256-GCM payload layer disabled. SQLCipher remains active.",
        "AES-256-GCM sloj je isključen. SQLCipher ostaje aktivan.",
    )
    val appearanceSectionTitle: String = s("Appearance adjustments", "Prilagođavanje izgleda")
    val uiTransparencyTitle: String = s("UI Transparency", "Providnost interfejsa")
    val permissionsSectionTitle: String = s("App Permissions", "Dozvole aplikacija")
    val permGranted: String = s("Granted", "Odobreno")
    val permDemoNotGranted: String = s("Not granted (Demo mode)", "Nije odobreno (Demo režim)")
    val permSmsTitle: String = s("SMS & MMS permissions", "SMS i MMS dozvole")
    val permMicrophoneTitle: String = s("Microphone permission", "Dozvola za mikrofon")
    val permMicrophoneSubGranted: String = s(
        "Granted for recording voice messages",
        "Odobreno za govorne poruke"
    )
    val permMicrophoneSubNotGranted: String = s(
        "Not granted (tap to allow voice message recording)",
        "Nije odobreno (dodirnite da omogućite snimanje)"
    )
    val permContactsTitle: String = s("Contacts permission", "Dozvola za kontakte")
    val permNotificationsTitle: String = s("Notifications permission", "Dozvola za obaveštenja")
    val voiceMessageHoldToRecordHint: String = s(
        "Press and hold microphone to record, slide right to cancel.",
        "Zadržite mikrofon pritisnutim za snimanje, prevucite udesno za poništavanje."
    )
    val defaultSmsRoleLabel: String = s("Default SMS app", "Podrazumevana SMS aplikacija")
    val roleActive: String = s("Active", "Postavljena")
    val roleNotSet: String = s("Not set", "Nije postavljena")
    val openSystemAppSettingsButton: String = s(
        "Open system app settings",
        "Otvori sistemska podešavanja aplikacije",
    )
    val rationalePermanentDenial: String = s(
        "Permissions were permanently denied. Open App Settings to enable SMS & MMS.",
        "Dozvole su odbijene. Otvorite Podešavanja aplikacije da biste omogućili SMS i MMS.",
    )
    val rationaleRequired: String = s(
        "SMS permissions are required to read and send messages on your device.",
        "SMS dozvole su neophodne za čitanje i slanje poruka sa vašeg uređaja.",
    )
    val rationaleLocalOnly: String = s(
        "Txtura uses SMS, MMS and Contacts permissions strictly on-device.",
        "Txtura koristi dozvole za SMS, MMS i Kontakte isključivo lokalno na telefonu.",
    )
    val setDefaultSmsInSystemToast: String = s(
        "Set Txtura as your default SMS app in System Settings.",
        "Postavite Txtura kao podrazumevanu SMS aplikaciju u Podešavanjima sistema.",
    )
    val dialerErrorToast: String = s("Unable to open phone dialer app.", "Nije moguće otvoriti aplikaciju za pozive.")
    val contactsErrorToast: String = s("Unable to open contacts app.", "Nije moguće otvoriti aplikaciju za kontakte.")
    val securitySectionTitle: String = s("Security Configuration Summary", "Pregled podešavanja zaštite")
    val securitySummaryDisclaimer: String = s(
        "This summarizes app settings and built-in controls; it does not continuously verify that every protection is working.",
        "Ovo prikazuje podešavanja i ugrađene zaštite; ne proverava neprekidno da li svaka zaštita ispravno radi.",
    )
    val secItem1Title: String = s(
        "Global Anti-Phishing, Malware & AI Scam Engine",
        "Globalni Anti-Phishing, Malver i AI-Scam Štit",
    )
    val secItem1Sub: String = s(
        "Multi-layer URL & threat analyzer: detects IDN homograph/brand spoofing, APK/EXE malware links, high-risk TLDs, and AI/crypto scams without flagging safe HTTPS links.",
        "Višeslojni filter pretnji: detektuje lažne brendove (homograf napade), malver fajlove (.apk/.exe), rizične domene i AI/kripto prevare, dok bezbedne sajtove propušta bez lažnih alarma.",
    )
    val secItem2Title: String = s("IntentSanitizer & Redirection Guard", "IntentSanitizer i zaštita od redirekcije")
    val secItem2Sub: String = s(
        "All incoming SEND/SENDTO intents undergo strict scheme validation and forbidden URI flag stripping.",
        "Svi dolazni SEND/SENDTO intenti prolaze strogu validaciju šeme i uklanjanje neovlašćenih URI zastavica.",
    )
    val secItem3Title: String = s("Zero-Permission Photo Picker for MMS", "Zero-Permission Photo Picker za MMS")
    val secItem3Sub: String = s(
        "Attaching MMS images never requests full gallery access, only the selected photo.",
        "Prilaganje MMS slika ne traži pristup celoj galeriji, već samo izabranoj fotografiji.",
    )
    val secItem4Title: String = s("Confidential 2FA/OTP Clipboard Copy", "Zaštićeno kopiranje 2FA/OTP kodova")
    val secItem4Sub: String = s(
        "Uses EXTRA_IS_SENSITIVE flag on Android 13+ to hide verification codes from clipboard previews.",
        "Koristi EXTRA_IS_SENSITIVE zastavicu na Android 13+ kako bi sakrio verifikacione kodove sa ekrana.",
    )
    val secItem5Title: String = s(
        "Protected System Receivers & Parameterized Queries",
        "Zaštićeni sistemski prijemnici i SQL upiti",
    )
    val secItem5Sub: String = s(
        "Receivers are locked with permissions and unencrypted cloud backup is disabled.",
        "Prijemnici su zaključani dozvolama uz parametrizovane upite i isključen nešifrovani bekap.",
    )
    val secItem6Title: String = s(
        "Local Message Cache Encryption (AES-256-GCM)",
        "Šifrovanje lokalnog keša poruka (AES-256-GCM)",
    )
    val secItem6Sub: String = s(
        "When enabled, the local conversation snapshot uses AES-256-GCM with a random data key wrapped by Android Keystore.",
        "Kada je uključeno, lokalni keš razgovora koristi AES-256-GCM i nasumični ključ zaštićen Android Keystore-om.",
    )
    val secItem7Title: String = s(
        "Biometric & Device PIN App Lock",
        "Zaključavanje aplikacije biometrijom i PIN-om uređaja",
    )
    val secItem7Sub: String = s(
        "Locks the application with fingerprint or device PIN, with customizable lock timeout and an impenetrable lock screen overlay.",
        "Zaključava aplikaciju otiskom prsta  ili PIN-om uređaja uz podesivo vreme zaključavanja i neprobojni ekran zaključavanja.",
    )
    val secItem8Title: String = s(
        "HTTPS & TLS/SSL Certificate Pinning (SPKI)",
        "HTTPS i TLS/SSL sertifikat pinning (SPKI)",
    )
    val secItem8Sub: String = s(
        "These app-managed requests always require HTTPS. When enabled, SPKI pins are checked for configured GitHub and Cloudflare hosts; other hosts use Android's standard TLS certificate validation.",
        "Ove mrežne veze uvek zahtevaju HTTPS. Kada je opcija uključena, SPKI pinovi se proveravaju za podešene GitHub i Cloudflare hostove; ostali hostovi koriste standardnu Android TLS proveru sertifikata.",
    )
    val secItem9Title: String = s(
        "Metadata Minimization & Zero-Telemetry",
        "Minimizacija metapodataka i nulta telemetrija",
    )
    val secItem9Sub: String = s(
        "Automated stripping of EXIF GPS coordinates and camera tags from MMS photos and redacted lock-screen notifications.",
        "Automatsko uklanjanje EXIF GPS koordinata i detalja kamere sa slika pre slanja i cenzurisana obaveštenja na zaključanom ekranu.",
    )
    val secItem10Title: String = s(
        "Best-Effort Root Indicator Check",
        "Orijentaciona provera root indikatora",
    )
    val secItem10Sub: String = s(
        "On-demand checks for common root binaries, test build tags, and known root manager packages. A clean result does not prove device integrity.",
        "Provera na zahtev traži uobičajene root fajlove, test potpise sistema i poznate root menadžere. Čist rezultat ne potvrđuje integritet uređaja.",
    )
    val secItem11Title: String = s(
        "Hardware Encrypted Backup & PBKDF2 Password Protection",
        "Hardverski šifrovani bekap i PBKDF2 zaštita lozinkom",
    )
    val secItem11Sub: String = s(
        "Encrypted backup exports using AES-256-GCM protected by the unique device keystore key or custom user password with PBKDF2 key derivation.",
        "Šifrovanje izvoza rezervnih kopija preko AES-256-GCM sa zaštitom jedinstvenim ključem uređaja ili korisničkom lozinkom uz PBKDF2 derivaciju.",
    )
    val secItem12Title: String = s(
        "Inbound Spam & Blocked Senders Firewall",
        "Zaštitni zid za spem i blokirane pošiljaoce",
    )
    val secItem12Sub: String = s(
        "New SMS from blocked senders can be discarded before Txtura saves them; blocked MMS notifications are ignored and not downloaded. Turning this off does not recover discarded messages.",
        "Nove SMS poruke blokiranih pošiljalaca mogu biti odbačene pre nego što ih Txtura sačuva; MMS obaveštenja se ignorišu i sadržaj se ne preuzima. Isključivanje opcije ne vraća odbačene poruke.",
    )

    val securityStatusEnabled: String = s("Enabled in settings", "Uključeno u podešavanjima")
    val securityStatusDisabled: String = s("Disabled", "Isključeno")
    val securityStatusTapToConfigure: String = s("Tap to configure", "Dodirnite za podešavanje")
    val securityStatusConfigured: String = s("Built in · not live-tested", "Ugrađeno · nije provereno uživo")
    val securityStatusCompromised: String = s("Root indicators found", "Pronađeni root indikatori")
    val systemSecuritySectionTitle: String = s("System & IPC Security Guard", "Sistemska i IPC zaštita")
    val rootDetectionActiveTitle: String = s(
        "On-demand Root Indicator Check",
        "Provera root indikatora na zahtev",
    )
    val rootDetectionActiveSub: String = s(
        "Checks common root binaries, test-keys and known root managers; a clean result is not proof of device integrity.",
        "Traži uobičajene root fajlove, test-keys i poznate root menadžere; čist rezultat nije dokaz integriteta uređaja.",
    )

    // Security Analyzer
    val riskSuspicious: String = s(
        "Warning: Malicious link, Phishing or Scam detected",
        "Upozorenje: Detektovan fišing, malver ili prevara",
    )
    val reasonHttp: String = s(
        "Unencrypted link",
        "Nešifrovan link",
    )
    val reasonIpUrl: String = s(
        "Link points directly to an IP address or non-standard port",
        "Link vodi direktno na IP adresu ili nestandardni port",
    )
    val reasonShortener: (String) -> String = { host ->
        if (isSerbian) "Skraćeni link skriva pravu destinaciju ($host)"
        else "Shortened link hides true destination ($host)"
    }
    val reasonHighRiskTld: (String) -> String = { tld ->
        if (isSerbian) "Domen visokog rizika ($tld) često korišćen za fišing i malver"
        else "High-risk domain extension ($tld) frequently abused in phishing & malware"
    }
    val reasonSocialEngineering: String = s(
        "Urgent social engineering lure combined with an untrusted link",
        "Socijalni inženjering / hitan zahtev u kombinaciji sa nepouzdanim linkom",
    )
    val reasonMalwareFile: (String) -> String = { ext ->
        if (isSerbian) "Link vodi ka potencijalno opasnom izvršnom/malver fajlu ($ext)"
        else "Dangerous executable/malware file link detected ($ext)"
    }
    val reasonHomographOrBrandSpoof: (String) -> String = { target ->
        if (isSerbian) "Detektovano lažno predstavljanje brenda ili homograf domen ($target)"
        else "Brand impersonation or IDN homograph spoofing detected ($target)"
    }
    val reasonAiOrCryptoScam: String = s(
        "AI investment, crypto fraud, fake task or family impersonation scam pattern",
        "Obrazac AI investicione prevare, lažnog posla ili lažnog hitnog broja",
    )
    val reasonEmbeddedCredentialsUrl: String = s(
        "URL uses '@' redirection trick to disguise real destination",
        "URL koristi '@' trik za maskiranje prave adrese sajta",
    )
    val reasonMaliciousWebsiteOnline: (String) -> String = { host ->
        if (isSerbian) "Sajt ($host) je identifikovan na mreži kao zlonameran (malver ili fišing)"
        else "Website ($host) identified online as malicious (malware or phishing)"
    }
    val reasonMaliciousRedirectOnline: (String, String) -> String = { fromHost, toHost ->
        if (isSerbian) "Link ($fromHost) preusmerava na zlonameran ili fišing sajt ($toHost)"
        else "Link ($fromHost) redirects to a malicious or phishing website ($toHost)"
    }
    val reasonInvalidTlsCertificate: (String) -> String = { host ->
        if (isSerbian) "Nevažeći ili lažni bezbednosni (SSL/TLS) sertifikat sajta ($host)"
        else "Invalid or untrusted SSL/TLS security certificate for ($host)"
    }
    val recallMessagesTitle: String = s("Recall messages", "Opozovi poruke")
    val recallMessagesSub: String = s(
        "Enable or disable the countdown timer when sending messages",
        "Uključuje ili isključuje tajmer za slanje poruka",
    )
    val maliciousWebsitesTitle: String = s(
        "Identify malicious websites in messages",
        "Identifikacija zlonamernih veb-sajtova u porukama",
    )
    val maliciousWebsitesSub: String = s(
        "Internet access required to verify links against online malware & phishing threat intelligence",
        "Potreban je pristup internetu za prepoznavanje zlonamernih i fišing veb-sajtova u porukama",
    )
    val defaultSmsPromptTitle: String = s("Default SMS & MMS App", "Podrazumevana SMS i MMS aplikacija")
    val defaultSmsPromptMessage: String = s(
        "Would you like to set Txtura as your default application for receiving SMS and MMS messages?",
        "Da li želite da podesite Txtura kao podrazumevanu aplikaciju za prijem SMS i MMS poruka?",
    )
    val defaultSmsPromptConfirm: String = s("Set as Default", "Podesi kao podrazumevanu")
    val defaultSmsPromptLater: String = s("Not now", "Ne sada")
    val showContactPhotosTitle: String = s("Show contact photos", "Prikaži slike kontakata")
    val layoutModeCompact: String = s("Compact messages view", "Kompaktni prikaz poruka")

    // SMS & MMS Settings (Carrier, Delivery, Receipts, MMS, Alerts, SMSC, SIM)
    val smsMmsSettingsSectionTitle: String = s("SMS & MMS Settings", "SMS i MMS podešavanja")
    val deliveryAndReceiptsSectionTitle: String = s("Delivery & receipts", "Potvrde i izveštaji")
    val mmsSectionTitle: String = s("Download and creation of MMS", "Preuzimanje i kreiranje MMS-a")
    val soundAndAlertsSectionTitle: String = s("Sound & emergency alerts", "Tonovi i hitna upozorenja")
    val simAndSmscSectionTitle: String = s("SIM Card & SMS Center", "SIM i mrežni servisi")

    // 1. Delivery Reports
    val deliveryReportsTitle: String = s("Delivery reports", "Izveštaji o isporuci")
    val deliveryReportsSub: String = s(
        "Request a delivery report for each message you send",
        "Zahtevajte izveštaj o isporuci za svaku poslatu poruku",
    )
    val deliveryReportsDisabled: String = s("Disabled", "Onemogućeno")
    val deliveryReportsEnabled: String = s("Enabled", "Omogućeno")

    // 2. Request read receipts
    val requestReadReportsTitle: String = s("Request read receipts", "Zahtevaj potvrde o pročitanim porukama")
    val requestReadReportsSub: String = s(
        "Request a read report for each sent MMS message",
        "Zahtevaj izveštaj o čitanju za svaku poslatu MMS poruku",
    )

    // 3. Send read receipts
    val sendReadReportsTitle: String = s("Send read receipts", "Šalji potvrde pročitanoj poruci")
    val sendReadReportsSub: String = s(
        "Send a report that message was read if requested by MMS sender",
        "Pošalji izveštaj da je poruka pročitana ako to zahteva pošiljalac MMS-a",
    )

    // 4. Auto-retrieve MMS
    val mmsAutoRetrieveTitle: String = s("Auto-retrieve MMS", "Automatski preuzmi MMS")
    val mmsAutoRetrieveNotRoaming: String = s("Not roaming", "Kad nije u romingu")
    val mmsAutoRetrieveAlways: String = s("Always", "Uvek")
    val mmsAutoRetrieveDisabled: String = s("Disabled", "Onemogućeno")

    // 5. Download and send MMS
    val mmsDataDisabledTitle: String = s("Download and send MMS", "Preuzmi i pošalji MMS")
    val mmsDataDisabledSub: String = s(
        "Download and send MMS messages even when mobile data is off",
        "Preuzmi i pošalji MMS poruke čak i kada su mobilni podaci isključeni",
    )
    val mmsDataDisabledNotRoaming: String = s("Not roaming", "Kad nije u romingu")
    val mmsDataDisabledAlways: String = s("Always", "Uvek")
    val mmsDataDisabledNever: String = s("Never", "Nikada")

    // 6. Creation mode
    val mmsCreationModeTitle: String = s("Creation mode", "Režim kreiranja")
    val mmsCreationModeFree: String = s("Free", "Slobodno")
    val mmsCreationModeWarning: String = s("Warning", "Upozorenje")
    val mmsCreationModeRestricted: String = s("Restricted", "Ograničeno")

    // MMS Download actions
    val mmsDownloadAction: String = s("Download MMS", "Preuzmi MMS")
    val mmsDownloadingAction: String = s("Downloading MMS...", "Preuzimanje MMS-a...")
    val mmsDownloadFailed: String = s("Download failed • Tap to retry", "Preuzimanje neuspešno • Dodirnite za ponovni pokušaj")
    val mmsPendingTitle: String = s("Incoming MMS", "Dolazna MMS poruka")

    // MMS APN Settings
    val mmsApnSettingsTitle: String = s("Carrier MMS APN Settings", "MMS APN podešavanja operatera")
    val mmsApnSettingsSub: String = s(
        "Configure MMSC URL, proxy, and port for sending and receiving MMS messages over cellular networks.",
        "Podešavanje MMSC URL adrese, MMS proksija i porta za slanje i prijem MMS poruka preko mobilne mreže."
    )
    val mmsApnCarrierLabel: String = s("Carrier profile", "Profil operatera")
    val mmsApnMmscLabel: String = s("MMSC URL", "MMSC URL adresa")
    val mmsApnProxyLabel: String = s("MMS Proxy", "MMS Proksi")
    val mmsApnPortLabel: String = s("MMS Port", "MMS Port")
    val mmsApnPullFromCarrierButton: String = s("Pull from carrier", "Povuci od operatera")
    val mmsApnPullFromSimOrPhoneButton: String = s("Pull from SIM / Phone", "Preuzmi sa SIM-a / telefona")
    val mmsApnOpenPhoneSettingsButton: String = s("Open Phone APN Settings", "Podešavanja telefona (APN)")
    val mmsApnSourceLabel: String = s("Source", "Izvor")
    val mmsApnManualSetupButton: String = s("Manual MMS setup", "Ručno podesi MMS")
    val mmsApnPullSuccess: String = s("MMS parameters pulled from carrier successfully", "MMS parametri su uspešno preuzeti od operatera")
    val mmsApnPullFailure: String = s("Unable to pull APN automatically. Please configure manually.", "Nije moguće automatski preuzeti APN. Podesite parametre ručno.")
    val mmsApnManualDialogTitle: String = s("Configure MMS APN", "Podešavanje MMS APN parametara")
    val mmsApnResetToDefault: String = s("Reset to carrier default", "Vrati na fabrička podešavanja operatera")
    val mmsApnSaveSuccess: String = s("MMS APN parameters saved", "MMS APN parametri su sačuvani")
    val mmsApnNotConfigured: String = s("Not configured", "Nije podešeno")
    val mmsApnDirectConnection: String = s("Direct connection (no proxy)", "Direktna veza (bez proksija)")
    val mmsApnEnterMmscPlaceholder: String = s("http://mms.../servlets/mms", "http://mms.../servlets/mms")
    val mmsApnEnterProxyPlaceholder: String = s("e.g. 172.17.88.198 or 10.28.0.60", "npr. 172.17.88.198 ili 10.28.0.60")
    val mmsApnEnterPortPlaceholder: String = s("8080", "8080")

    // 7. Wireless Emergency Alerts
    val emergencyAlertsTitle: String = s("Wireless emergency alerts", "Upozorenja o hitnom slučaju")
    val emergencyAlertsSub: String = s(
        "Public safety alerts and emergency broadcast settings",
        "Sistemska bezbednosna i hitna upozorenja preko mreže",
    )
    val emergencyLocationLabel: String = s("Current country / location", "Trenutna lokacija i zemlja")
    val emergencyFilterDesc: String = s(
        "Active protection: Only alerts for your current country and global catastrophes are received.",
        "Aktivna zaštita: Prihvataju se isključivo upozorenja za zemlju u kojoj se telefon nalazi i globalne katastrofe."
    )
    val emergencyExtremeTitle: String = s("Extreme threats", "Ekstremne opasnosti")
    val emergencyExtremeSub: String = s(
        "Extreme threats to life and property",
        "Ekstremne opasnosti po život i imovinu"
    )
    val emergencySevereTitle: String = s("Severe threats", "Ozbiljne opasnosti")
    val emergencySevereSub: String = s(
        "Severe weather and disaster alerts",
        "Ozbiljne vremenske nepogode i vanredne situacije"
    )
    val emergencyAmberTitle: String = s("AMBER Alerts / Child abduction", "Pronađi me i Amber Alert")
    val emergencyAmberSub: String = s(
        "Emergency bulletins for abducted children",
        "Hitna obaveštenja o nestalim licima i deci"
    )
    val emergencyGlobalTitle: String = s("Global catastrophes", "Globalne katastrofe")
    val emergencyGlobalSub: String = s(
        "Planetary crises, extreme disasters and presidential alerts (always on)",
        "Planetarne nepogode, globalne katastrofe i vrhovna upozorenja (uvek aktivno)"
    )
    val emergencySoundTitle: String = s("Emergency sound alert", "Zvučni signal uzbune")
    val emergencySoundSub: String = s(
        "Play high-priority alert sound for incoming warnings",
        "Reprodukcija zvučnog signala najvišeg prioriteta za uzbune"
    )
    val emergencyTestButton: String = s("Trigger test alert", "Testiraj uzbunu")
    val emergencyTestToast: String = s("Test emergency alert dispatched", "Test upozorenje je poslato")
    val emergencySystemSettingsButton: String = s("Device system settings", "Sistemska podešavanja telefona")
    val emergencyNoAlertsTitle: String = s("No active emergency alerts", "Nema aktivnih hitnih upozorenja")
    val emergencyNoAlertsSub: String = s(
        "Your device is continuously monitoring for local and global emergency broadcasts.",
        "Vaš uređaj neprekidno prati mrežu za lokalna i globalna hitna upozorenja."
    )

    // 8. Message Tone & Notification Sound
    val smsNotificationSoundTitle: String = s("Notification sound", "Ton obaveštenja")
    val smsNotificationSoundSub: String = s(
        "Choose notification tone for incoming messages",
        "Izaberite melodiju obaveštenja za nove poruke",
    )
    val messageToneTitle: String = s("Set system sound", "Podesi zvuk sistema")
    val messageToneSub: String = s(
        "Notification sound for incoming messages in system settings",
        "Melodija i zvuk obaveštenja za poruke u sistemskim podešavanjima",
    )

    // 9. SMS Center Number (SMSC)
    val smscNumberTitle: String = s("SMS center number", "Broj SMS centra")
    val smscNumberSub: String = s(
        "Service center address for SMS delivery",
        "Broj servisnog centra mobilnog operatera",
    )
    val smscDialogTitle: String = s("SMS Center (SMSC)", "Broj SMS centra")
    val smscDialogDesc: String = s(
        "Service center phone number used by your mobile carrier to route text messages.",
        "Broj SMS centra koji vaš mobilni operater koristi za prenos tekstualnih poruka.",
    )
    val smscNotSet: String = s("Default (System)", "Podrazumevano (Mreža)")
    val smscSavedToast: String = s("SMS center updated", "Broj SMS centra je sačuvan")

    // 10. Manage SIM Card Messages
    val simMessagesTitle: String = s("Manage SIM card messages", "Upravljanje porukama SIM kartice")
    val simMessagesSub: String = s(
        "View and manage messages stored on SIM card",
        "Pregledajte i upravljajte porukama sačuvanim na SIM kartici",
    )
    val simMessagesEmpty: String = s("No messages found on SIM card.", "Nema sačuvanih poruka na SIM kartici.")
    val simMessagesLoading: String = s("Reading SIM card messages...", "Učitavanje poruka sa SIM kartice...")
    val simMessageCopyInbox: String = s("Copy to Inbox", "Prebaci u sanduče")
    val simMessageDelete: String = s("Delete from SIM", "Obriši sa SIM-a")
    val simMessageCopiedToast: String = s("Message copied to inbox", "Poruka je kopirana u sanduče")
    val simMessageDeletedToast: String = s("Message deleted from SIM", "Poruka je obrisana sa SIM kartice")

    // In-app interaction & sound settings (Screenshot 1)
    val inAppSoundsTitle: String = s("Sound feedback", "Zvučni odzivi")
    val inAppSoundsSub: String = s(
        "Play sound effects while chatting, sending, receiving, and interacting",
        "Zvučni signali pri interakcijama, slanju i prijemu poruka unutar aplikacije",
    )
    val pinchToZoomTitle: String = s("Pinch to zoom conversation text", "Uštinite da biste zumirali tekst konverzacije")
    val pinchToZoomSub: String = s(
        "Resize text in chat bubbles with a two-finger pinch gesture",
        "Povećajte ili smanjite tekst u mehurićima širenjem ili skupljanjem prstiju",
    )
    val expressiveAnimationsTitle: String = s("Show expressive animations", "Prikaži izražajne animacije")
    val expressiveAnimationsSub: String = s(
        "Smooth bubble physics, transitions, and lively visual effects",
        "Glatke animacije mehurića, prelaza i vizuelnih detalja",
    )
    val flashAlertTitle: String = s("Light feedback", "Svetlosni odziv")
    val flashAlertSub: String = s(
        "Flash blinking synchronized with the notification sound rhythm",
        "Treperenje blica sinhronizovano sa ritmom zvuka obaveštenja",
    )
    val hapticsTitle: String = s("Haptic feedback", "Haptički odzivi")
    val hapticsSub: String = s(
        "Tactile vibration feedback on touches, selection, and message actions",
        "Taktilne vibracije na dodir, selekciju i radnje sa porukama",
    )

    // Bubbles (Screenshot 2)
    val bubblesSectionTitle: String = s("Bubbles", "Mehurići")
    val bubblesAllowTitle: String = s(
        "Allow Txtura to display notifications as bubbles.",
        "Dozvoli aplikaciji Txtura da prikazuje obaveštenja kao mehuriće.",
    )
    val bubblesInfoDesc: String = s(
        "Some notifications and other content can appear as bubbles on the screen. To open a bubble, simply tap it. To dismiss a bubble, drag it down across the screen.",
        "Neka obaveštenja i drugi sadržaj mogu da se prikažu kao mehurići na ekranu. Da biste otvorili mehurić, jednostavno ga dodirnite. Da biste odbacili mehurić, prevucite ga nadole preko ekrana.",
    )
    val bubblesOpenSystemSettings: String = s("Open system bubble settings", "Otvori sistemska podešavanja mehurića")

    // External links & Automatic previews
    val externalLinksTitle: String = s("External links", "Eksterni linkovi")
    val externalLinksSub: String = s(
        "Previews, rich media, and security threat protection for URLs",
        "Pregledi sadržaja i bezbednosna provera internet linkova u porukama",
    )
    val autoPreviewsSectionTitle: String = s("Automatic previews", "Automatski pregledi")
    val autoPreviewsShowAllTitle: String = s("Show all previews", "Prikazuj sve preglede")
    val autoPreviewsShowAllSub: String = s(
        "Automatically load link and attachment previews in conversations",
        "Automatski učitavaj preglede linkova i priloga u razgovorima",
    )
    val autoPreviewsWebLinksOnlyTitle: String = s("Show only web link previews", "Prikazuj samo preglede veb-linkova")
    val autoPreviewsWebLinksOnlySub: String = s(
        "Load previews only for web URLs, excluding other rich media",
        "Učitavaj preglede isključivo za veb-linkove (URL-ove)",
    )
    val autoPreviewsWifiOnlyTitle: String = s("Download data only over Wi-Fi", "Preuzimaj podatke samo preko WiFi-a")
    val autoPreviewsWifiOnlySub: String = s(
        "Download preview metadata and images only when connected to Wi-Fi",
        "Preuzimaj podatke za pregled samo kada ste povezani na WiFi mrežu",
    )

    // Swipe Gestures & Trash / Deleted Messages
    val swipeSectionTitle: String = s("Messages Actions", "Interakcije sa porukama")
    val swipeRightLabel: String = s("Swipe Right Action (→)", "Radnja za svajp udesno (→)")
    val swipeLeftLabel: String = s("Swipe Left Action (←)", "Radnja za svajp ulevo (←)")
    val swipeActionPin: String = s("(Un)pin", "Prikači / Otkači")
    val swipeGesturePin: String = s("Pin", "Zakači")
    val swipeGestureUnpin: String = s("Unpin", "Otkači")
    val swipeActionDelete: String = s("Trash", "Obriši")
    val swipeActionCall: String = s("Call", "Pozovi")
    val swipeActionMarkRead: String = s("Mark read / unread", "Pročitano / Nepročitano")
    val swipeGestureMarkRead: String = s("Read", "Pročitano")
    val swipeGestureMarkUnread: String = s("Unread", "Nepročitano")
    val swipeActionNone: String = s("Disabled", "Isključeno")
    val deletedMessagesTitle: String = s("Deleted Messages", "Obrisane poruke")
    val deletedMessagesSubtitle: (Int, Int) -> String = { count, days ->
        if (isSerbian) "$count u obrisanim • Automatsko brisanje nakon $days dana"
        else "$count in Trash • Auto-delete after $days days"
    }
    val openDeletedMessagesButton: (Int) -> String = { count ->
        if (isSerbian) "Otvori obrisane poruke ($count)" else "Open Deleted Messages ($count)"
    }
    val autoDeleteDaysTitle: String = s("Auto-delete deleted messages after", "Automatski ukloni obrisane poruke nakon")
    val daysCountLabel: (Int) -> String = { days ->
        if (isSerbian) "$days dana" else "$days days"
    }
    val groupingModeTitle: String = s("Sort & group deleted messages by", "Grupisanje i sortiranje obrisanih poruka")
    val groupByDateLabel: String = s("By Date", "Po datumu")
    val groupByContactLabel: String = s("By Contact", "Po kontaktu")
    val confirmDeleteTitle: String = s("Move to Trash?", "Premesti u obrisane poruke?")
    val confirmDeleteMessage: (String, Int) -> String = { name, days ->
        if (isSerbian) {
            "Da li ste sigurni da želite da obrišete razgovor sa $name?\nPoruka neće biti odmah trajno obrisana, već se premešta u Obrisane poruke odakle će biti automatski izbrisana nakon $days dana."
        } else {
            "Are you sure you want to delete the conversation with \"$name\"?\n It will be moved to Deleted Messages and automatically permanently deleted after $days days."
        }
    }
    val confirmMoveToTrashButton: String = s("Trash", "Obriši")
    val confirmPermanentDeleteTitle: String = s("Delete Permanently?", "Trajno obriši poruke?")
    val confirmPermanentDeleteMessage: (String) -> String = { name ->
        if (isSerbian) {
            "Da li želite da trajno izbrišete razgovor sa „$name“ iz foldera obrisanih poruka? Ova radnja se ne može poništiti."
        } else {
            "Permanently delete the conversation with \"$name\"? This action cannot be undone."
        }
    }
    val confirmPermanentDeleteButton: String = s("Delete Forever", "Trajno obriši")
    val restoreButton: String = s("Restore", "Vrati poruku")
    val deletePermanentlyButton: String = s("Delete forever", "Trajno obriši")
    val emptyTrashButton: String = s("Empty Trash", "Isprazni sve")
    val emptyTrashMessage: String = s("No deleted messages in Trash.", "Nema poruka u folderu Obrisane poruke.")
    val daysRemainingBadge: (Int) -> String = { days ->
        if (isSerbian) "Još $days d." else "${days}d left"
    }
    val deletedOnPrefix: String = s("Deleted: ", "Obrisano: ")
    val messagesCountLabel: (Int) -> String = { count ->
        if (isSerbian) {
            val rem10 = count % 10
            val rem100 = count % 100
            val isSingularForm = (rem10 == 1) && (rem100 != 11)
            val isPaucalForm = (rem10 >= 2) && (rem10 <= 4) && ((rem100 < 12) || (rem100 > 14))
            if (isSingularForm) {
                "$count poruka"
            } else if (isPaucalForm) {
                "$count poruke"
            } else {
                "$count poruka"
            }
        } else {
            if (count == 1) "1 message" else "$count messages"
        }
    }
    val restoredToast: (String) -> String = { name ->
        if (isSerbian) "Vraćene poruke za kontakt: $name" else "Restored messages from $name"
    }
    val movedToTrashToast: (String) -> String = { name ->
        if (isSerbian) "Premestili ste razgovor ($name) u Obrisane poruke" else "Moved $name to Deleted Messages"
    }

    // Blocked Senders Section
    val blockedSendersSectionTitle: String = s("Blocked Senders", "Blokirani pošiljaoci")
    val unblockSenderButton: String = s("Unblock", "Odblokiraj")
    val blockedSendersEmpty: String = s("No blocked senders.", "Trenutno nema blokiranih pošiljalaca.")
    val senderBlockedToast: (String) -> String = { addr ->
        if (isSerbian) "Blokiran pošiljalac: $addr" else "Blocked sender: $addr"
    }
    val senderUnblockedToast: (String) -> String = { addr ->
        if (isSerbian) "Odblokiran pošiljalac: $addr" else "Unblocked sender: $addr"
    }

    // Wallpapers & Backgrounds
    val wallpaperSectionTitle: String = s("Backgrounds & Themes", "Pozadine i teme")
    val chatWallpaperTooltip: String = s("Chat wallpaper for this contact", "Pozadina četa za ovaj kontakt")
    val chatWallpaperDialogTitle: (String) -> String = { name ->
        if (isSerbian) "Pozadina četa: $name" else "Wallpaper: $name"
    }
    val pickWallpaperFromGallery: String = s("Add Photo from Gallery", "Dodaj sliku iz galerije")
    val customGalleryWallpaperActive: String = s(
        "Custom Gallery Photo Active",
        "Aktivna slika iz galerije",
    )
    val useMainWallpaperDefault: String = s("Use Main Window Wallpaper", "Koristi pozadinu glavnog prozora")
    val wpDefault: String = s("Obsidian Glass", "Obsidijan staklo")
    val wpAurora: String = s("Aurora Borealis", "Polarna svetlost")
    val wpVelvetPlum: String = s("Velvet Plum", "Kraljevska šljiva")
    val wpMidnightOcean: String = s("Midnight Ocean", "Ponoćni okean")
    val wpSunsetHorizon: String = s("Sunset Horizon", "Zalazak sunca")
    val wpEmeraldForest: String = s("Emerald Mist", "Smaragdna magla")
    val wpRoyalAmethyst: String = s("Royal Amethyst", "Kraljevski ametist")
    val wpStarlightGalaxy: String = s("Starlight Galaxy", "Zvezdana galaksija")
    val wpCrimsonVelvet: String = s("Crimson Velvet", "Grimizni somot")
    val wpBurgundyAnita: String = s("Burgundy Anita", "Burgundi Anita")
    val wpArcticFrost: String = s("Arctic Frost", "Arktički led")
    val wpObsidianGold: String = s("Obsidian & Gold", "Obsidijan i zlato")
    val wpMonoSlate: String = s("With Titanium Accent", "Sa titanijumskim akcentom")
    val wpMonoSapphire: String = s("With Sapphire Accent", "Sa safirnim akcentom")
    val wpMonoSage: String = s("With Emerald Accent", "Sa smaragdnim akcentom")
    val wpMonoChampagne: String = s("With Gold Accent", "Sa zlatnim akcentom")
    val wpMonoPlum: String = s("With Amethyst Accent", "Sa ametistnim akcentom")
    val wpMonoGraphite: String = s("With Silver Accent", "Sa srebrnim akcentom")
    val wpMonoRose: String = s("With Rose Accent", "Sa ružičastim akcentom")
    val wpMonoBurgundy: String = s("With Burgundy Accent", "Sa burgundi akcentom")
    val wpMonoNavy: String = s("With Indigo Accent", "Sa indigo akcentom")
    val wpMonoSmoke: String = s("With Teal Accent", "Sa tirkiznim akcentom")
    val wpMonoEspresso: String = s("With Copper Accent", "Sa bakarnim akcentom")
    val wpAllBlack: String = s("All Black", "All Black")
    val wpAllWhite: String = s("All White", "All White")

    val wpDefaultDesc: String = s(
        "Obsidian & royal sapphire",
        "Obsidijan i kraljevski safir",
    )
    val wpAuroraDesc: String = s(
        "Emerald, violet & rose glow",
        "Smaragdni i ljubičasti sjaj",
    )
    val wpVelvetPlumDesc: String = s(
        "Deep plum with sky-blue",
        "Tamna šljiva i nebesko plava",
    )
    val wpMidnightOceanDesc: String = s(
        "Ocean cobalt with amber",
        "Okeanski kobalt i ćilibar",
    )
    val wpSunsetHorizonDesc: String = s(
        "Warm dusk with turquoise",
        "Grimizni sumrak i tirkiz",
    )
    val wpEmeraldForestDesc: String = s(
        "Forest emerald with coral",
        "Šumski smaragd i koral",
    )
    val wpRoyalAmethystDesc: String = s(
        "Violet prism with gold",
        "Ljubičasta prizma i zlato",
    )
    val wpStarlightGalaxyDesc: String = s(
        "Cosmic indigo & starlight blue",
        "Kosmički indigo i svetloplava",
    )
    val wpCrimsonVelvetDesc: String = s(
        "Deep ruby with emerald",
        "Duboki rubin i smaragd",
    )
    val wpBurgundyAnitaDesc: String = s(
        "Opulent burgundy wine & velvety rose",
        "Raskošni burgundac boje vina i baršunasta ruža",
    )
    val wpArcticFrostDesc: String = s(
        "Polar ice with soft rose",
        "Polarni led i nežna ružičasta",
    )
    val wpObsidianGoldDesc: String = s(
        "Volcanic black & gold veins",
        "Vulkanska crna i zlatne niti",
    )
    val wpMonoSlateDesc: String = s(
        "Cool brushed titanium tone",
        "Hladni ton brušenog titanijuma",
    )
    val wpMonoSapphireDesc: String = s(
        "Clear royal sapphire blue",
        "Čista kraljevska safirno plava",
    )
    val wpMonoSageDesc: String = s(
        "Calm sage & emerald green",
        "Smirena žalfija i smaragdno zelena",
    )
    val wpMonoChampagneDesc: String = s(
        "Warm champagne gold sheen",
        "Topli odsjaj šampanj-zlata",
    )
    val wpMonoPlumDesc: String = s(
        "Soft royal amethyst purple",
        "Nežna ametist ljubičasta",
    )
    val wpMonoGraphiteDesc: String = s(
        "Pure graphite & metallic silver",
        "Čisti grafit i metalik srebrna",
    )
    val wpMonoRoseDesc: String = s(
        "Velvet rose & crimson tone",
        "Baršunasta ružičasta i grimiz",
    )
    val wpMonoBurgundyDesc: String = s(
        "Deep burgundy wine & velvety crimson",
        "Duboki burgundac boje vina i baršunasti rubin",
    )
    val wpMonoNavyDesc: String = s(
        "Deep royal indigo & navy",
        "Duboka kraljevska indigo-teget",
    )
    val wpMonoSmokeDesc: String = s(
        "Modern ocean turquoise",
        "Moderna okeanska tirkizna",
    )
    val wpMonoEspressoDesc: String = s(
        "Warm copper & bronze",
        "Topli ton bakra i bronze",
    )
    val wpAllBlackDesc: String = s(
        "True AMOLED pitch-black with white text",
        "Prava AMOLED crna sa belim tekstom",
    )
    val wpAllWhiteDesc: String = s(
        "Pure clean white with black text",
        "Čista bela podloga sa crnim tekstom",
    )

    // Txtura Tactile Background Textures
    val textureSectionTitle: String = s("Background Txtura", "Txtura pozadine")
    val textureSectionSub: String = s(
        "Each theme has a matching signature txture, or choose any txture pattern below",
        "Svaka tema ima prilagođenu txturu, a možete izabrati i bilo koju txturu po želji",
    )
    val texAutoTheme: String = s("Auto", "Automatska")
    val texSilkWeave: String = s("Silk Weave", "Svilena tkanina")
    val texAuroraRibbons: String = s("Aurora Silk Waves", "Svileni talasi")
    val texVelvetNap: String = s("Crushed Velvet", "Kraljevski somot")
    val texOceanCaustics: String = s("Ocean Ripples", "Vodeni talasi")
    val texSandDunes: String = s("Sandstone Dunes", "Peščane dine")
    val texOrganicBotanical: String = s("Botanical Woodgrain", "Organski godovi")
    val texCrystalFacets: String = s("Crystal Facets", "Kristalne fasete")
    val texConstellationMesh: String = s("Starlight Mesh", "Zvezdana mreža")
    val texDamaskJacquard: String = s("Damask Jacquard", "Žakar brokat")
    val texFrostCrystals: String = s("Frost Crystals", "Ledeni kristali")
    val texKintsugiMarble: String = s("Kintsugi Marble", "Mermer i zlato")
    val texSlateStrata: String = s("Slate Stone", "Kameni škriljac")
    val texBrushedMetal: String = s("Brushed Titanium", "Brušeni titanijum")
    val texLinenCanvas: String = s("Linen Canvas", "Laneno platno")
    val texFlutedGlass: String = s("Fluted Glass", "Rebrasto staklo")
    val texSuedeMicrofiber: String = s("Suede Microfiber", "Antilop mikrovlakna")
    val texCarbonTwill: String = s("Carbon Fiber", "Karbonska vlakna")
    val texCottonPaper: String = s("Cotton Rag Paper", "Pamučni papir")
    val texHerringboneTwill: String = s("Herringbone Weave", "Riblja kost")
    val texTopographicContour: String = s("Topographic Relief", "Topografski reljef")
    val texPebbledLeather: String = s("Pebbled Leather", "Zrnasta koža")
    val texNone: String = s("No Txture", "Bez txture")

    // UI Color Mode
    val wallpaperTabColorful: String = s("Polychrome", "Polihromne")
    val wallpaperTabMonochrome: String = s("Monochrome", "Monohromne")
    val wallpaperTabMinimalist: String = s("Minimalist", "Minimalističke")

    // Backup & Restore
    val backupSectionTitle: String = s("Backup & Restore", "Rezervna kopija podataka")
    val createLocalBackupButton: String = s("Create Local Backup", "Kreiraj lokalnu kopiju")
    val restoreLocalBackupButton: String = s("Restore from Local File", "Vrati iz lokalnog fajla")
    val shareBackupButton: String = s("Export Backup File", "Izvezi fajl kopije")

    // Sub-item titles for Settings
    val subSettingThemeModeTitle: String = s("Theme Appearance Mode", "Režim prikaza teme")

    // Chat Top-Right 3-Dots Menu & Block Contact Confirmation
    val moreOptionsTooltip: String = s("More options", "Više opcija")
    val toggleSenderCategoryOption: String = s("Change Sender Category", "Promeni kategoriju pošiljaoca")
    val currentCategoryPeopleSub: String = s("Currently: People → Move to Services", "Trenutno: Osobe → Prebaci u Usluge")
    val currentCategoryServiceSub: String = s("Currently: Services → Move to People", "Trenutno: Usluge → Prebaci u Osobe")
    val senderMovedToServicesToast: String = s("Conversation moved to Services", "Razgovor prebačen u Usluge")
    val senderMovedToPeopleToast: String = s("Conversation moved to People", "Razgovor prebačen u Osobe")
    val blockContactOption: String = s("Block messages from this contact", "Blokiraj poruke od ovog kontakta")
    val confirmBlockContactTitle: String = s("Block this contact?", "Blokiraj kontakt?")
    val confirmBlockContactMessage: (String, String) -> String = { name, addr ->
        if (isSerbian) {
            "Da li želite da blokirate sve poruke od kontakta „$name“ ($addr)?"
        } else {
            "Are you sure you want to block all messages from \"$name\" ($addr)?"
        }
    }
    val backupCreatedSuccess: (Int, Int) -> String = { count, msgs ->
        if (isSerbian) "Uspešno kreirana rezervna kopija: $count razgovora, $msgs poruka"
        else "Backup created: $count conversations, $msgs messages"
    }
    val backupRestoreSuccess: (Int, Int) -> String = { count, msgs ->
        if (isSerbian) "Uspešno vraćeno $count razgovora i $msgs poruka"
        else "Restored $count conversations and $msgs messages successfully"
    }
    val backupErrorInvalid: String = s(
        "Invalid or corrupted backup file.",
        "Nevažeća rezervna kopija ili oštećen format fajla.",
    )
    val lastBackupLabel: (String) -> String = { date ->
        if (isSerbian) "Poslednja rezervna kopija: $date" else "Last backup: $date"
    }
    val noPreviousBackup: String = s("No backup created yet", "Još uvek nema kreiranih kopija")
    val confirmRestoreTitle: String = s("Restore from Backup?", "Vraćanje iz rezervne kopije?")
    val confirmRestoreMessage: (String, Int, Int) -> String = { date, count, msgs ->
        if (isSerbian) {
            "Da li želite da vratite podatke iz rezervne kopije ($date) koja sadrži $count razgovora i $msgs poruka? Postojeća podešavanja i poruke biće ažurirani."
        } else {
            "Do you want to restore data from backup ($date) containing $count conversations and $msgs messages? Existing settings and messages will be updated."
        }
    }
    val confirmRestoreButton: String = s("Restore Data", "Vrati podatke")

    // Encrypted Backup & Password strings
    val createEncryptedBackupDialogTitle: String = s("Create Encrypted Backup", "Kreiraj šifrovanu kopiju")
    val createEncryptedBackupDialogDesc: String = s(
        "Your conversations and settings are encrypted with AES-256-GCM. Optionally set a password to allow restoring on a computer or another phone. If left blank, it will be encrypted with this device's hardware key.",
        "Vaše poruke i podešavanja se šifruju pomoću AES-256-GCM standarda. Opciono unesite lozinku ako želite da kopiju možete otvoriti na računaru ili drugom telefonu. Ako ostavite prazno, biće šifrovana hardverskim ključem ovog telefona."
    )
    val backupPasswordPlaceholder: String = s("Optional password", "Opciona lozinka")
    val backupPasswordConfirmButton: String = s("Encrypt & Save", "Šifruj i sačuvaj")
    val restorePasswordDialogTitle: String = s("Enter Backup Password", "Unesite lozinku kopije")
    val restorePasswordDialogDesc: String = s(
        "This backup file is encrypted with a password. Enter the password used when creating the backup to decrypt and restore it.",
        "Ova rezervna kopija je zaštićena lozinkom. Unesite lozinku koja je korišćena prilikom kreiranja kopije kako biste je dešifrovali."
    )
    val restorePasswordPlaceholder: String = s("Backup password", "Lozinka kopije")
    val restorePasswordConfirmButton: String = s("Decrypt & Restore", "Dešifruj i učitaj")
    val backupDeviceKeyMismatch: String = s(
        "This backup was encrypted with another device's hardware key. A password-protected backup is required to restore on a different device.",
        "Ova kopija je šifrovana hardverskim ključem drugog uređaja. Za prenos na novi telefon potrebno je kreirati kopiju sa lozinkom."
    )

    // Selective Trash Management
    val inspectTrashMessagesButton: String = s("Inspect messages", "Pregledaj poruke")
    val inspectTrashDialogTitle: (String) -> String = { contact ->
        if (isSerbian) "Obrisane poruke • $contact" else "Deleted messages • $contact"
    }
    val restoreSelectedMessagesButton: String = s("Restore selected", "Vrati izabrane")
    val deleteSelectedMessagesButton: String = s("Delete selected", "Obriši izabrane")
    val selectedCountLabel: (Int) -> String = { count ->
        if (isSerbian) "$count izabrano" else "$count selected"
    }
    val backupPasswordIncorrect: String = s(
        "Incorrect backup password or corrupted file.",
        "Pogrešna lozinka rezervne kopije ili oštećen fajl."
    )

    // Delayed 6s send & scheduled message strings
    val messageSendCancelledToast: String = s(
        "Message sending cancelled.",
        "Slanje poruke otkazano.",
    )
    val cancelDelayedSendTooltip: String = s(
        "Cancel sending",
        "Otkaži slanje",
    )
    val scheduleMessageTitle: String = s(
        "Schedule Message",
        "Isplaniraj slanje poruke",
    )
    val scheduleMessageSubtitle: String = s(
        "Choose when this message should be sent automatically:",
        "Odaberite kada želite da se poruka automatski pošalje:",
    )
    val schedulePresetIn15Min: String = s("In 15 minutes", "Za 15 minuta")
    val schedulePresetIn1Hour: String = s("In 1 hour", "Za 1 sat")
    val schedulePresetTonight20: String = s("Tonight at 20:00", "Večeras u 20:00")
    val schedulePresetTomorrow09: String = s("Tomorrow at 09:00", "Sutra u 09:00")
    val scheduleCustomTimeTitle: String = s("Exact date & time", "Tačan datum i vreme")
    val scheduleDayLabel: String = s("Day", "Dan")
    val scheduleMonthLabel: String = s("Month", "Mesec")
    val scheduleYearLabel: String = s("Year", "Godina")
    val scheduleHourLabel: String = s("Hour (0-23)", "Sat (0-23)")
    val scheduleMinuteLabel: String = s("Minute (0-59)", "Minut (0-59)")
    val scheduleConfirmButton: String = s("Schedule send", "Zakaži slanje")
    val scheduledMessagesBannerTitle: String = s("Scheduled messages", "Planirane poruke")
    val cancelScheduledMessageTooltip: String = s(
        "Cancel scheduled message",
        "Otkaži planiranu poruku",
    )
    val scheduleInvalidDateTime: String = s(
        "Please enter a valid future date and time.",
        "Unesite važeći datum i vreme u budućnosti.",
    )
    val messageScheduledToast: (String) -> String = { timeStr ->
        if (isSerbian) "Poruka je zakazana za $timeStr." else "Message scheduled for $timeStr."
    }

    // Notification Direct Reply, Mark as Read, Highlight & Copy 2FA Code actions
    val notificationReplyAction: String = s("Reply", "Odgovori")
    val notificationReplyHint: String = s("Write a reply...", "Napiši odgovor...")
    val notificationReplySentToast: String = s("Reply sent.", "Odgovor je poslat.")
    val notificationMarkReadAction: String = s("Mark as read", "Označi kao pročitano")
    val notificationMarkedReadToast: String = s("Message marked as read.", "Poruka je označena kao pročitana.")
    val notificationHighlightAction: String = s("Highlight", "Hajlajtuj")
    val notificationHighlightedToast: String = s("Message highlighted.", "Poruka je hajlajtovana.")
    val notificationCopyCodeAction: (String) -> String = { code ->
        if (isSerbian) "Kopiraj kod ($code)" else "Copy code ($code)"
    }
    val browserOpenErrorToast: String = s(
        "Unable to open link in browser.",
        "Nije moguće otvoriti link u pretraživaču.",
    )

    // Settings Footer (Official App Version, Release Year, Privacy & Copyright)
    val settingsFooterVersion: (String, Long) -> String = { versionName, versionCode ->
        if (isSerbian) {
            "Verzija $versionName (Build $versionCode)"
        } else {
            "Version $versionName (Build $versionCode)"
        }
    }
    val settingsFooterPrivacyBadge: String = s(
        "Protected SMS/MMS • 100% On-Device Storage\nZero Ads & Tracking",
        "Zaštićena SMS/MMS aplikacija • 100% lokalni podaci\nBez oglasa i praćenja",
    )
    val settingsFooterCopyright: String = s(
        "© 2026 Txtura • All rights reserved",
        "© 2026 Txtura • Sva prava zadržana",
    )

    // App Update Checker (GitHub Releases)
    val updateSectionTitle: String = s("App Updates", "Ažuriranje aplikacije")
    val checkForUpdatesButton: String = s("Check for updates", "Proveri ažuriranja")
    val checkingForUpdatesLabel: String = s("Checking for updates...", "Proveravam ažuriranja...")
    val appUpToDateLabel: (String) -> String = { ver ->
        if (isSerbian) "Aplikacija je ažurna (v$ver)" else "App is up to date (v$ver)"
    }
    val updateAvailableTitle: (String) -> String = { ver ->
        if (isSerbian) "Dostupna je nova verzija: $ver" else "New version available: $ver"
    }
    val downloadAndInstallUpdateButton: String = s("Download and Install", "Preuzmi i instaliraj")
    val downloadingUpdateLabel: (Int) -> String = { pct ->
        if (isSerbian) "Preuzimanje ažuriranja sa GitHub-a... $pct%" else "Downloading update from GitHub... $pct%"
    }
    val updateLastCheckedLabel: (String) -> String = { timeStr ->
        if (isSerbian) "Poslednja provera: $timeStr" else "Last checked: $timeStr"
    }
    val updateNoInternetToast: String = s(
        "No internet connection.",
        "Nema internet konekcije.",
    )
    val updateCheckFailedToast: String = s(
        "Could not reach GitHub Releases right now.",
        "Trenutno nije moguće pristupiti GitHub izdanjima.",
    )
    val updateUpToDateToast: String = s(
        "You already have the latest version of Txtura.",
        "Već koristiš najnoviju verziju aplikacije Txtura.",
    )
    val changelogTooltip: String = s("Changelog & improvements", "Dnevnik izmena i poboljšanja")
    val changelogDialogTitle: String = s("Changelog & Improvements", "Dnevnik izmena i poboljšanja")
    val changelogCurrentVersionBadge: (String, Long) -> String = { vName, vCode ->
        if (isSerbian) "Verzija $vName (Build $vCode) • Trenutna" else "Version $vName (Build $vCode) • Current"
    }
    val changelogAvailableUpdateBadge: (String) -> String = { tag ->
        if (isSerbian) "Najnovije izdanje: $tag" else "Latest release: $tag"
    }
    val updateNotificationTitle: String = s(
        "New update available",
        "Dostupno je novo ažuriranje",
    )
    val updateNotificationText: (String) -> String = { tag ->
        if (isSerbian) "Nova verzija $tag je dostupna. Dodirnite za preuzimanje i instalaciju."
        else "A new version $tag is available. Tap to download and install."
    }
    val updateNotificationChannelName: String = s(
        "App Updates",
        "Ažuriranja aplikacije",
    )
    val updateNotificationChannelDesc: String = s(
        "Notifications about new versions of Txtura",
        "Obaveštenja o novim verzijama aplikacije Txtura",
    )
    val changelogPreviousVersionsHeader: String = s("Previous Versions History", "Istorija prethodnih verzija")

    // Draft messages
    val draftPrefix: String = s("[Draft]", "[Nacrt]")

    // Scheduled message birthday midnight preset
    val scheduleBirthdayMidnightChip: String = s("Midnight for Birthday (00:00)", "U ponoć za rođendan (00:00)")
    val scheduleBirthdayMidnightApplied: String = s("Scheduled for midnight (00:00)", "Postavljeno za ponoć (00:00)")
    val scheduleBirthdayDefaultGreeting: String = s("Happy Birthday! Wishing you all the best!", "Srećan rođendan! Sve najbolje od srca!")

    // Multi-recipient broadcast messaging
    val singleRecipientTab: String = s("Single contact", "Jedan kontakt")
    val multipleRecipientsTab: String = s("Multiple contacts", "Više kontakata")
    val multiRecipientBadge: (Int) -> String = { count ->
        if (isSerbian) "$count primalaca • Zasebni razgovori" else "$count recipients • Separate chats"
    }
    val multiRecipientCountBadge: (Int) -> String = { count ->
        if (isSerbian) "Izabrano: $count primalaca" else "Selected: $count recipients"
    }
    val sendToAllButton: (Int) -> String = { count ->
        if (isSerbian) "Pošalji svima ($count)" else "Send to all ($count)"
    }
    val sendBroadcastAction: (Int) -> String = { count ->
        if (isSerbian) "Pošalji za $count primalaca" else "Send to $count recipients"
    }
    val broadcastMessagesSentToast: (Int) -> String = { count ->
        if (isSerbian) "Poruka poslata za $count primalaca u zasebnim četovima" else "Message sent to $count recipients in separate chats"
    }
    val addMoreRecipients: String = s("Add recipient", "Dodaj primaoca")
    val selectSenderSubtitle: String = s("Select a contact or enter a phone number", "Izaberite kontakt ili unesite broj telefona")
    val broadcastSeparateNotice: String = s(
        "Each recipient will receive this message in their own private 1-on-1 conversation.",
        "Svaki primalac će primiti ovu poruku u sopstvenom privatnom 1-na-1 razgovoru.",
    )
    val sendToOtherContactsOption: String = s(
        "Send to multiple recipients in separate chats",
        "Pošalji i drugim kontaktima u zasebnim četovima",
    )
    val addTypedNumberToRecipients: (String) -> String = { num ->
        if (isSerbian) "+ Dodaj broj $num" else "+ Add number $num"
    }
    val noRecipientsSelectedHint: String = s(
        "Select contacts below or type a phone number to add recipients",
        "Izaberite kontakte ispod ili unesite broj telefona za dodavanje",
    )
    val clearAllRecipientsTooltip: String = s("Clear all", "Poništi sve")
    val selectAllContactsLabel: String = s("Select all", "Označi sve")
    val deselectAllContactsLabel: String = s("Deselect all", "Odznači sve")
    val broadcastMessageTextLabel: String = s(
        "Message text (sent individually to each contact)",
        "Tekst poruke (šalje se svakome pojedinačno)",
    )
    val broadcastMessageTextPlaceholder: String = s("Type your message here...", "Unesite tekst poruke ovde...")

    // In-chat search
    val searchInChatPlaceholder: String = s("Search messages in this chat...", "Pretraži poruke u ovom razgovoru...")
    val searchMatchesCounter: (Int, Int) -> String = { curr, total -> "$curr/$total" }
    val searchNoMatches: String = s("No matches", "Nema rezultata")
    val searchChatTooltip: String = s("Search in chat", "Pretraži u razgovoru")
    val closeSearchTooltip: String = s("Close search", "Zatvori pretragu")
    val searchNextTooltip: String = s("Next match", "Sledeći rezultat")
    val searchPrevTooltip: String = s("Previous match", "Prethodni rezultat")

    // Contact sharing
    val shareContactTooltip: String = s("Share contact (Name Phone)", "Pošalji kontakt (Ime prezime broj telefona)")
    val shareContactDialogTitle: String = s("Select Contact to Share", "Izaberi kontakt za slanje")
    val shareContactAddedToast: (String) -> String = { name ->
        if (isSerbian) "Kontakt $name dodat u poruku" else "Contact $name added to message"
    }
    val pickFromPhonebookButton: String = s("Pick from device contacts", "Izaberi iz imenika telefona")

    // Auto-detected email & phone numbers in received messages
    val saveToContactsAction: String = s("Save to contacts", "Sačuvaj u kontakte")
    val callNumberAction: String = s("Call number", "Pozovi broj")
    val sendEmailAction: String = s("Send email", "Pošalji email")
    val copyPhoneNumberAction: String = s("Copy phone number", "Kopiraj broj telefona")
    val copyEmailAction: String = s("Copy email", "Kopiraj email adresu")
    val phoneSavedToClipboardToast: String = s("Phone number copied to clipboard", "Broj telefona kopiran u privremenu memoriju")
    val emailSavedToClipboardToast: String = s("Email copied to clipboard", "Email adresa kopirana u privremenu memoriju")
    val detectedPhoneNumberLabel: String = s("Detected phone", "Prepoznat broj")
    val detectedEmailLabel: String = s("Detected email", "Prepoznat email")

    // Smart Bank Account (Tekući račun) recognition strings
    val bankAccountCardTitle: String = s("Bank Account", "Tekući račun")
    val copyBankAccountAction: String = s("Copy Account Number", "Kopiraj broj računa")
    val copyFull18DigitsAction: String = s("Copy full format (18 digits)", "Kopiraj puni format (18 cifara)")
    val bankAccountCopiedToast: (String, String?) -> String = { num, bank ->
        val bankSuffix = if (!bank.isNullOrBlank()) " ($bank)" else ""
        if (isSerbian) "Broj računa kopiran: $num$bankSuffix" else "Bank account copied: $num$bankSuffix"
    }

    // Offered accent colors for custom gallery wallpaper
    val customWallpaperAccentTitle: String = s("Accent Color", "Boja akcenta")
    val customWallpaperAccentSub: String = s(
        "Choose an accent color for bubbles and buttons when using this photo",
        "Izaberite boju akcenta za oblačiće i dugmad uz ovu fotografiju"
    )
}

private val EnglishStrings = AppStrings(isSerbian = false)
private val SerbianStrings = AppStrings(isSerbian = true)

object TxturaStrings {

    fun isSerbianLocale(locale: Locale = Locale.getDefault()): Boolean {
        val lang = locale.language.lowercase(Locale.ROOT)
        return lang == "sr"
    }

    fun forLocale(locale: Locale = Locale.getDefault()): AppStrings {
        return if (isSerbianLocale(locale)) SerbianStrings else EnglishStrings
    }

    @Composable
    @ReadOnlyComposable
    fun current(): AppStrings {
        val locale = LocalConfiguration.current.locales[0] ?: Locale.ENGLISH
        return forLocale(locale)
    }
}
