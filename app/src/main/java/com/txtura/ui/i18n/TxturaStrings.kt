package com.txtura.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

class AppStrings(val isSerbian: Boolean) {
    private fun s(en: String, sr: String): String = if (isSerbian) sr else en

    val settingsTooltip: String = s("Settings", "Podešavanja")
    val settingsSubtitle: String = s(
        "Appearance, messages, blocked senders, backup & permissions",
        "Izgled, poruke, blokirani pošiljaoci, bekap i dozvole",
    )
    val searchPlaceholder: String = s(
        "Search messages, contacts or 2FA codes...",
        "Pretraži poruke, kontakte ili 2FA kodove...",
    )
    val clearSearch: String = s("Clear search", "Obriši pretragu")
    val filterPeople: String = s("People", "Osobe")
    val filterServices: String = s("Service Messages", "Servisne poruke")
    val filterUnread: String = s("Unread", "Nepročitano")
    val filterPinned: String = s("Pinned", "Zakačeno")
    val filterAll: String = s("All Messages", "Sve poruke")
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
    val highlightedMessagesTitle: String = s("Highlighted", "Hajlajtovano")
    val deleteThread: String = s("Delete conversation", "Obriši razgovor")
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
    val protectedChannelSuffix: String = s("Protected SMS/MMS channel", "Zaštićeni SMS/MMS kanal")
    val mmsAttachedTitle: String = s("MMS Photo attached", "MMS Fotografija priložena")
    val mmsZeroPermissionSub: String = s(
        "Zero-Permission Photo Picker (scoped access only)",
        "Zero-Permission Photo Picker (bez pristupa celoj galeriji)",
    )
    val removeMmsAttachment: String = s("Remove MMS attachment", "Ukloni MMS prilog")
    val attachMmsImage: String = s("Attach MMS image", "Dodaj MMS sliku")
    val addMmsCaptionPlaceholder: String = s("Add MMS caption...", "Dodaj opis uz MMS...")
    val writeSmsPlaceholder: String = s("Write an SMS message...", "Napiši SMS poruku...")
    val mmsModeIndicator: String = s("MMS Mode • Multimedia message", "MMS režim • Multimedijalna poruka")
    val send: String = s("Send", "Pošalji")
    val cancel: String = s("Cancel", "Otkaži")
    val confirm: String = s("Confirm", "Potvrdi")
    val newSmsMmsDialogTitle: String = s("Choose Recipient", "Izaberi primaoca / pošiljaoca")
    val selectSenderSubtitle: String = s(
        "Select a contact or enter a number to open or start a conversation",
        "Izaberite osobu da biste nastavili prethodni čet ili započeli novu konverzaciju",
    )
    val searchOrEnterNumberPlaceholder: String = s(
        "Search contact or type phone number...",
        "Pretraži kontakt ili unesi broj telefona...",
    )
    val openChatWithNumberButton: (String) -> String = { num ->
        if (isSerbian) "Otvori čet sa: $num" else "Open conversation with $num"
    }
    val existingChatBadge: String = s("Continue chat", "Nastavi čet")
    val newChatBadge: String = s("New chat", "Nova konverzacija")
    val recipientLabel: String = s("Recipient (phone number)", "Primalac (broj telefona)")
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
        "Do you want to delete this message from the conversation?",
        "Da li želite da izbrišete ovu poruku iz konverzacije?",
    )
    val forwardMessageDialogTitle: String = s("Forward message to...", "Prosledi poruku osobi...")
    val messageCopiedToast: String = s("Message copied to clipboard", "Poruka je kopirana")
    val messageDeletedToast: String = s("Message deleted", "Poruka je izbrisana")
    val messageForwardedToast: (String) -> String = { name ->
        if (isSerbian) "Poruka prosleđena za: $name" else "Message forwarded to $name"
    }

    // Main Screen Selection Mode & Top Actions
    val selectedThreadsCountLabel: (Int) -> String = { count ->
        if (isSerbian) "Označeno: $count" else "$count selected"
    }
    val bottomBarDeleteTooltip: String = s("Delete", "Izbriši")
    val bottomBarMarkReadTooltip: String = s("Mark as read", "Označi kao pročitano")
    val bottomBarSelectAllTooltip: String = s("Select all", "Odaberi sve")
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
    val confirmSelectAllTitle: String = s("Select all messages?", "Odaberi sve poruke?")
    val confirmSelectAllMessage: (Int) -> String = { count ->
        if (isSerbian) {
            "Da li želite da odaberete sve prikazane poruke ($count)?"
        } else {
            "Do you want to select all $count displayed conversations?"
        }
    }
    val threadsDeletedToast: (Int) -> String = { count ->
        if (isSerbian) "Premešteno u obrisane poruke: $count" else "Moved $count conversation(s) to Deleted Messages"
    }
    val threadsMarkedReadToast: (Int) -> String = { count ->
        if (isSerbian) "Označeno kao pročitano: $count" else "Marked $count conversation(s) as read"
    }
    val allMessagesMarkedReadToast: String = s(
        "All messages marked as read",
        "Sve poruke su označene kao pročitane",
    )

    // Settings Screen Categories
    val appearanceSectionTitle: String = s("Appearance, Themes & Transparency", "Izgled, Teme i Providnost")
    val appearanceSectionDesc: String = s(
        "Light/Dark/AMOLED mode, UI transparency slider, 10 monochrome & 10 colorful themes, wallpapers, icon shape & layout.",
        "Tema, slajder za providnost UI, monohromatske teme sa 10 akcenatskih boja, pozadine, ikonica i prikaz.",
    )
    val uiTransparencyTitle: String = s("UI Glass Opacity & Transparency", "Providnost interfejsa (UI)")
    val uiTransparencySub: String = s(
        "Adjust how translucent or solid the glass cards, bars, and bubbles appear",
        "Podesite slajderom prozirnost staklenih kartica, menija i oblačića sa porukama",
    )
    val permissionsSectionTitle: String = s("Permissions & Default App", "Dozvole i Podrazumevana Aplikacija")
    val smsMmsContactsPermLabel: String = s("SMS, MMS & Contacts permissions", "SMS, MMS i Kontakti dozvole")
    val permGranted: String = s("Granted", "Odobreno")
    val permDemoNotGranted: String = s("Demo mode (Not granted)", "Demo režim (Nije odobreno)")
    val defaultSmsRoleLabel: String = s("Default SMS app (ROLE_SMS)", "Podrazumevana SMS aplikacija (ROLE_SMS)")
    val roleActive: String = s("Active", "Aktivna")
    val roleNotSet: String = s("Not set", "Nije postavljena")
    val openSystemAppSettingsButton: String = s(
        "Open system app settings",
        "Otvori sistemska podešavanja aplikacije",
    )
    val rationalePermanentDenial: String = s(
        "Permissions were permanently denied. Open App Settings to enable SMS & MMS.",
        "Dozvole su trajno odbijene. Otvorite Podešavanja aplikacije da biste omogućili SMS i MMS.",
    )
    val rationaleRequired: String = s(
        "SMS permissions are required to read and send messages on your device.",
        "SMS dozvole su neophodne za čitanje i slanje poruka sa vašeg uređaja.",
    )
    val rationaleLocalOnly: String = s(
        "Txtura uses SMS, MMS and Contacts permissions strictly on-device.",
        "Txtura koristi SMS, MMS i Kontakti dozvole isključivo lokalno na telefonu.",
    )
    val setDefaultSmsInSystemToast: String = s(
        "Set Txtura as your default SMS app in System Settings.",
        "Postavite Txtura kao podrazumevanu SMS aplikaciju u Podešavanjima sistema.",
    )
    val dialerErrorToast: String = s("Unable to open phone dialer app.", "Nije moguće otvoriti aplikaciju za pozive.")
    val contactsErrorToast: String = s("Unable to open contacts app.", "Nije moguće otvoriti aplikaciju za kontakte.")
    val securitySectionTitle: String = s("Active Security Measures", "Aktivne Bezbednosne Mere")
    val secItem1Title: String = s(
        "Global Anti-Phishing, Malware & AI Scam Engine",
        "Globalni Anti-Phishing, Malver i AI-Scam Štit",
    )
    val secItem1Sub: String = s(
        "Multi-layer URL & threat analyzer: detects IDN homograph/brand spoofing, APK/EXE malware links, high-risk TLDs, and AI/crypto scams without flagging safe HTTPS links.",
        "Višeslojni filter pretnji: detektuje lažne brendove (homograf napade), malver fajlove (.apk/.exe), rizične domene i AI/kripto prevare, dok bezbedne sajtove propušta bez lažnih alarma.",
    )
    val secItem2Title: String = s("IntentSanitizer & Redirection Guard", "IntentSanitizer & Zaštita od redirekcije")
    val secItem2Sub: String = s(
        "All incoming SEND/SENDTO intents undergo strict scheme validation and forbidden URI flag stripping.",
        "Svi dolazni SEND/SENDTO intenti prolaze strogu validaciju šeme i uklanjanje neovlašćenih URI zastavica.",
    )
    val secItem3Title: String = s("Zero-Permission Photo Picker for MMS", "Zero-Permission Photo Picker za MMS")
    val secItem3Sub: String = s(
        "Attaching MMS images never requests full gallery access (READ_MEDIA_IMAGES), only the selected photo.",
        "Prilaganje MMS slika ne traži pristup celoj galeriji (READ_MEDIA_IMAGES), već samo izabranoj fotografiji.",
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
        "Receivers are locked with BROADCAST_SMS / BROADCAST_WAP_PUSH permissions and unencrypted cloud backup is disabled.",
        "Prijemnici su zaključani BROADCAST_SMS / BROADCAST_WAP_PUSH dozvolama uz parametrizovane upite i isključen nešifrovani Cloud/ADB bekap.",
    )

    // Security Analyzer
    val riskSuspicious: String = s(
        "Warning: Malicious link, Phishing or Scam detected",
        "Upozorenje: Detektovan fišing, malver ili prevara",
    )
    val reasonHttp: String = s(
        "Unencrypted HTTP link (not protected by TLS)",
        "Nešifrovan HTTP link (podaci nisu zaštićeni TLS-om)",
    )
    val reasonIpUrl: String = s(
        "Link points directly to a raw IP address or non-standard port",
        "Link vodi direktno na sirovu IP adresu ili nestandardni port",
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
    val defaultSmsPromptTitle: String = s("Default SMS & MMS App", "Podrazumevana SMS i MMS aplikacija")
    val defaultSmsPromptMessage: String = s(
        "Would you like to set Txtura as your default application for receiving SMS and MMS messages?",
        "Da li želite da podesite Txtura kao podrazumevanu aplikaciju za prijem SMS i MMS poruka?",
    )
    val defaultSmsPromptConfirm: String = s("Set as Default", "Podesi kao podrazumevanu")
    val defaultSmsPromptLater: String = s("Not now", "Ne sada")
    val showContactPhotosTitle: String = s("Show contact photos", "Prikazuj slike kontakata")
    val showContactPhotosSub: String = s(
        "Display contact photos and avatars in the message list and conversation header",
        "Prikazuje fotografije i avatare kontakata u listi poruka i u zaglavlju razgovora",
    )
    val mainLayoutModeSub: String = s(
        "Choose between Default or Compact view for the main messages window",
        "Izaberite između podrazumevanog (Default) i kompaktnog prikaza glavnog prozora",
    )
    val layoutModeDefault: String = s("Default view", "Default prikaz")
    val layoutModeCompact: String = s("Compact view", "Kompaktni prikaz")

    // Swipe Gestures & Trash / Deleted Messages
    val swipeSectionTitle: String = s("Messages, Categories & Swipe Actions", "Poruke, Kategorije i Svajp radnje")
    val swipeSectionDesc: String = s(
        "Configure swipe gestures, category order, and deleted messages retention.",
        "Podesite brze svajp radnje, redosled kategorija i obrisane poruke.",
    )
    val swipeRightLabel: String = s("Swipe Right Action (→)", "Radnja za svajp udesno (→)")
    val swipeLeftLabel: String = s("Swipe Left Action (←)", "Radnja za svajp ulevo (←)")
    val swipeActionPin: String = s("Pin / Unpin", "Prikači / Otkači")
    val swipeActionDelete: String = s("Delete (Trash)", "Obriši (u korpu)")
    val swipeActionCall: String = s("Call sender", "Pozovi kontakt")
    val swipeActionMarkRead: String = s("Mark read", "Označi pročitano")
    val swipeActionContact: String = s("Contact card", "Detalji kontakta")
    val swipeActionNone: String = s("Disabled", "Isključeno")
    val deletedMessagesTitle: String = s("Deleted Messages", "Obrisane poruke")
    val deletedMessagesSubtitle: (Int, Int) -> String = { count, days ->
        if (isSerbian) "$count u obrisanim • Automatsko brisanje nakon $days dana"
        else "$count in Trash • Auto-delete after $days days"
    }
    val openDeletedMessagesButton: (Int) -> String = { count ->
        if (isSerbian) "Otvori obrisane poruke ($count)" else "Open Deleted Messages ($count)"
    }
    val autoDeleteDaysTitle: String = s("Auto-delete deleted messages after", "Automatski izbriši obrisane poruke nakon")
    val daysCountLabel: (Int) -> String = { days ->
        if (isSerbian) "$days dana" else "$days days"
    }
    val groupingModeTitle: String = s("Sort & group deleted messages by", "Grupisanje i sortiranje obrisanih poruka")
    val groupByDateLabel: String = s("By Deletion Date", "Po datumu brisanja")
    val groupByContactLabel: String = s("By Contact", "Po kontaktu")
    val confirmDeleteTitle: String = s("Move to Deleted Messages?", "Premesti u obrisane poruke?")
    val confirmDeleteMessage: (String, Int) -> String = { name, days ->
        if (isSerbian) {
            "Da li ste sigurni da želite da obrišete razgovor sa „$name“? Poruka neće biti odmah trajno obrisana, već se premešta u Obrisane poruke odakle će biti automatski izbrisana nakon $days dana."
        } else {
            "Are you sure you want to delete the conversation with \"$name\"? It will be moved to Deleted Messages and automatically permanently deleted after $days days."
        }
    }
    val confirmMoveToTrashButton: String = s("Move to Trash", "Premesti u obrisane")
    val confirmPermanentDeleteTitle: String = s("Delete Permanently?", "Trajno brisanje poruke?")
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
    val emptyTrashButton: String = s("Empty Trash", "Isprazni obrisane")
    val emptyTrashMessage: String = s("No deleted messages in Trash.", "Nema poruka u folderu Obrisane poruke.")
    val daysRemainingBadge: (Int) -> String = { days ->
        if (isSerbian) "Još $days d." else "${days}d left"
    }
    val deletedOnPrefix: String = s("Deleted: ", "Obrisano: ")
    val messagesCountLabel: (Int) -> String = { count ->
        if (isSerbian) {
            when {
                ((count % 10) == 1) && ((count % 100) != 11) -> "$count poruka"
                ((count % 10) in 2..4) && ((count % 100) !in 12..14) -> "$count poruke"
                else -> "$count poruka"
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
    val blockedSendersSectionTitle: String = s("Blocked Senders", "Blokiranje pošiljalaca poruka")
    val blockedSendersSectionDesc: String = s(
        "Manage blocked phone numbers and senders whose messages will be blocked.",
        "Dodajte ili uklonite blokirane pošiljaoce i brojeve telefona od kojih ne želite da primate poruke.",
    )
    val addBlockedSenderPlaceholder: String = s(
        "Enter phone number or sender ID...",
        "Unesite broj telefona ili naziv pošiljaoca...",
    )
    val blockSenderButton: String = s("Block", "Blokiraj")
    val unblockSenderButton: String = s("Unblock", "Odblokiraj")
    val blockedSendersEmpty: String = s("No blocked senders.", "Trenutno nema blokiranih pošiljalaca.")
    val senderBlockedToast: (String) -> String = { addr ->
        if (isSerbian) "Blokiran pošiljalac: $addr" else "Blocked sender: $addr"
    }
    val senderUnblockedToast: (String) -> String = { addr ->
        if (isSerbian) "Odblokiran pošiljalac: $addr" else "Unblocked sender: $addr"
    }

    // Wallpapers & Backgrounds
    val wallpaperSectionTitle: String = s("Backgrounds & Themes", "Pozadine i Teme")
    val chatWallpaperTooltip: String = s("Chat wallpaper for this contact", "Pozadina četa za ovaj kontakt")
    val chatWallpaperDialogTitle: (String) -> String = { name ->
        if (isSerbian) "Pozadina četa: $name" else "Wallpaper: $name"
    }
    val pickWallpaperFromGallery: String = s("Add Photo from Gallery", "Dodaj svoju sliku iz galerije")
    val customGalleryWallpaperActive: String = s(
        "Custom Gallery Photo Active",
        "Aktivna sopstvena slika iz galerije",
    )
    val useMainWallpaperDefault: String = s("Use Main Window Wallpaper", "Koristi pozadinu glavnog prozora")
    val wpDefault: String = s("Default Obsidian Glass", "Podrazumevana Obsidijan Glass podloga")
    val wpAurora: String = s("Aurora Borealis", "Polarna svetlost (Aurora)")
    val wpVelvetPlum: String = s("Velvet Plum", "Kraljevska šljiva (Velvet Plum)")
    val wpMidnightOcean: String = s("Midnight Ocean", "Ponoćni okean")
    val wpSunsetHorizon: String = s("Sunset Horizon", "Zalazak sunca (Horizon)")
    val wpEmeraldForest: String = s("Emerald Mist", "Smaragdna magla")
    val wpRoyalAmethyst: String = s("Royal Amethyst", "Kraljevski ametist")
    val wpStarlightGalaxy: String = s("Starlight Galaxy", "Zvezdana galaksija")
    val wpCrimsonVelvet: String = s("Crimson Velvet", "Grimizni somot (Velvet)")
    val wpArcticFrost: String = s("Arctic Frost", "Arktički led (Frost)")
    val wpObsidianGold: String = s("Obsidian & Gold", "Obsidijan i zlato")
    val wpMonoSlate: String = s("Monochrome + Slate Accent", "Monohrom + Titanijum akcenat")
    val wpMonoSapphire: String = s("Monochrome + Sapphire Accent", "Monohrom + Safirno plavi akcenat")
    val wpMonoSage: String = s("Monochrome + Emerald Accent", "Monohrom + Smaragdno zeleni akcenat")
    val wpMonoChampagne: String = s("Monochrome + Gold Accent", "Monohrom + Zlatni akcenat")
    val wpMonoPlum: String = s("Monochrome + Amethyst Accent", "Monohrom + Ametist ljubičasti akcenat")
    val wpMonoGraphite: String = s("Monochrome + Silver/Onyx Accent", "Monohrom + Srebrni/Oniks akcenat")
    val wpMonoRose: String = s("Monochrome + Rose Accent", "Monohrom + Ružičasti akcenat")
    val wpMonoNavy: String = s("Monochrome + Indigo Accent", "Monohrom + Indigo teget akcenat")
    val wpMonoSmoke: String = s("Monochrome + Teal Accent", "Monohrom + Tirkizni akcenat")
    val wpMonoEspresso: String = s("Monochrome + Copper Accent", "Monohrom + Bakarni akcenat")

    // Txtura Tactile Background Textures
    val textureSectionTitle: String = s("Txtura Background Texture", "Tekstura pozadine (Txtura)")
    val textureSectionSub: String = s(
        "Each theme has a matching signature texture, or choose any texture pattern below",
        "Svaka tema ima prilagođenu teksturu, a možete izabrati i bilo koju teksturu po želji",
    )
    val texAutoTheme: String = s("Auto (Theme Texture)", "Automatska (prema temi)")
    val texSilkWeave: String = s("Silk Weave", "Svileno tkanje")
    val texAuroraRibbons: String = s("Aurora Silk Waves", "Svileni talasi")
    val texVelvetNap: String = s("Crushed Velvet", "Kraljevski somot")
    val texOceanCaustics: String = s("Ocean Ripples", "Vodeni talasi")
    val texSandDunes: String = s("Sandstone Dunes", "Peščane dine")
    val texOrganicBotanical: String = s("Botanical & Woodgrain", "Organski godovi")
    val texCrystalFacets: String = s("Crystal Facets", "Kristalne fasete")
    val texConstellationMesh: String = s("Starlight Mesh", "Zvezdana mreža")
    val texDamaskJacquard: String = s("Damask Jacquard", "Žakar brokat")
    val texFrostCrystals: String = s("Frost Crystals", "Ledeni kristali")
    val texKintsugiMarble: String = s("Kintsugi Marble", "Mermer i zlato")
    val texSlateStrata: String = s("Slate Stone", "Škriljac kamen")
    val texBrushedMetal: String = s("Brushed Titanium", "Brušeni titanijum")
    val texLinenCanvas: String = s("Linen Canvas", "Laneno platno")
    val texFlutedGlass: String = s("Fluted Glass", "Rebrasto staklo")
    val texSuedeMicrofiber: String = s("Suede Microfiber", "Antilop koža")
    val texCarbonTwill: String = s("Carbon Fiber", "Karbonska vlakna")
    val texCottonPaper: String = s("Cotton Rag Paper", "Pamučni papir")
    val texHerringboneTwill: String = s("Herringbone Weave", "Riblja kost")
    val texTopographicContour: String = s("Topographic Relief", "Topografski reljef")
    val texPebbledLeather: String = s("Pebbled Leather", "Zrnasta koža")
    val texNone: String = s("No Texture (Smooth)", "Bez teksture (Glatko)")

    // UI Color Mode
    val uiColorModeColorfulTitle: String = s("Colorful (10)", "Višebojne (10)")
    val uiColorModeMonochromeTitle: String = s("Monochrome (10)", "Monohrom (10)")
    val uiColorModeMonochromeDesc: String = s(
        "Follows Light or Dark mode with your choice of 10 single accent colors",
        "Prilagođene svetlom ili tamnom režimu sa 10 akcenatskih boja",
    )
    val wallpaperTabColorful: String = s("Colorful (10)", "Višebojne (10)")
    val wallpaperTabMonochrome: String = s("Monochrome (10)", "Monohrom (10)")

    // Launcher Icon Shape Style
    val launcherIconShapeSectionTitle: String = s("Launcher Icon Style", "Stil ikonice aplikacije")
    val launcherIconShapeCircle: String = s("Circle", "Krug")
    val launcherIconShapeSquircle: String = s("Squircle", "Zaobljeni kvadrat")
    val launcherIconShapeRoundedSquare: String = s("Rounded Box", "Zaobljena kutija")
    val launcherIconShapeTeardrop: String = s("Teardrop", "Kapljica")
    val launcherIconShapePebble: String = s("Pebble", "Kamenčić")
    val launcherIconShapeSavedToast: (String) -> String = { shape ->
        if (isSerbian) "Stil ikonice postavljen na: $shape" else "Launcher icon style set to $shape"
    }

    // Backup & Restore
    val backupSectionTitle: String = s("Backup & Restore", "Bekap i vraćanje podataka")
    val backupSectionDesc: String = s(
        "Create local backups or export your conversations and settings.",
        "Kreirajte rezervne kopije poruka i podešavanja ili vratite podatke iz fajla.",
    )
    val createLocalBackupButton: String = s("Create Local Backup", "Kreiraj lokalnu kopiju")
    val restoreLocalBackupButton: String = s("Restore from Local File", "Vrati iz lokalnog fajla")
    val shareBackupButton: String = s("Share / Export Backup File", "Podeli / Pošalji fajl kopije")

    // Sub-item titles for Settings
    val subSettingThemeModeTitle: String = s("Theme Mode (AMOLED / Light / Dark / Auto)", "Režim teme (AMOLED / Svetla / Tamna / Auto)")

    // Chat Top-Right 3-Dots Menu & Block Contact Confirmation
    val moreOptionsTooltip: String = s("More options", "Više opcija")
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
