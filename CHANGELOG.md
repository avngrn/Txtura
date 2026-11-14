# Txtura Changelog & Improvements (Dnevnik izmena i poboljšanja)

## [v3.8] - 2026-11-14 (Trenutno izdanje / Current Release)

### Dodate opcije (Added Options)
- **Direct Security Overview Access from Conversation Header Shield & Settings Footer (Otvaranje pregleda zaštite klikom na štit u zaglavlju razgovora i u podnožju podešavanja):** Clicking on the green security shield in the conversation header or on the "Zaštićena SMS/MMS aplikacija" footer badge in Settings directly opens the Security Configuration Summary dialog (`SecurityMeasuresDialog`).
- **Dedicated "Swipe Gestures" ("Pokreti prevlačenja") Settings Screen (Zaseban ekran podešavanja pokreta prevlačenja):** Grouped conversation list swipe gestures (*Swipe Right Action (→)* and *Swipe Left Action (←)*) and message bubble swipe gestures (*Message Bubble Swipe Right (→)* and *Message Bubble Swipe Left (←)*) into a dedicated glass sub-menu screen *"Swipe gestures"* (*"Pokreti prevlačenja"*) inside *Chat & Gestures* settings, with smooth back navigation.
- **Dynamic Category Widths & Proportional Unread Expansion (Ujednačene širine kategorija sa srazmernim širenjem nepročitanih):** Equalized category chip widths on the home screen when there are no unread messages. When unread messages arrive (e.g. `(2)` or `(15)`), the "Nepročitano" category expands dynamically while the other three categories shrink proportionally so the total width always strictly matches the search bar directly above it without horizontal scrolling, and smoothly reverts to equal widths when messages are marked as read.
- **"All" ("Sve") Category First by Default (Kategorija „Sve“ po defaultu na prvom mestu):** Displayed the "All" (`ConversationFilter.ALL`) category first by default on the home screen (`ALL, PEOPLE, SERVICES, UNREAD`), with drag-and-drop custom reordering preserved.
- **Message Bubble Swipe "Trash" Option & Move-to-Trash Confirmation Dialog (Opcija „Obriši“ za svajp na prozorčić poruke uz prozor za potvrdu):** Added `MessageSwipeAction.DELETE` (*"Trash" / "Obriši"*) to both *Message Bubble Swipe Right (→)* and *Message Bubble Swipe Left (←)* settings cards, and wired it to open a confirmation dialog (*"Premesti u obrisane poruke?" / "Move to Trash?"*) before moving the swiped message to Deleted messages.
- **Confirmation Dialogs for Trash Thread Actions & Direct Inspection on Card Tap (Dijalozi za potvrdu u korpi i direktan ulazak u pregled klikom na karticu):** Added user confirmation dialogs before restoring a deleted thread from the Trash list, as well as before restoring selected messages or permanently deleting selected messages in the inspection dialog (`InspectTrashConversationDialog`). Tapping anywhere on the deleted conversation card (or avatar) or tapping "Choose" / "Odaberi" enters the inspection view directly without extra confirmation prompts.
- **Smart Payment Reference ("Poziv na broj" & Model), Payment Amount ("Iznos"), Payment Deadline / Calendar Event ("Rok / Termin"), Package Tracking ("Pošiljka") & Send SMS Quick Actions (Pametno prepoznavanje poziva na broj, iznosa za uplatu, rokova za kalendar, brojeva pošiljki i brzo slanje SMS-a):** Added automatic recognition and one-tap chips inside messages for payment reference numbers (`Poziv na br: ... • Model 97`), bill/transaction amounts (`Iznos: 3628.99 RSD` — copies normalized numeric value for m-banking while ignoring account balances), payment deadlines and scheduled appointments (`Rok: 01.12.2026.` — opens Android Calendar to add a reminder), courier/postal tracking codes (`Pošiljka: PE...RS`, DExpress, BEX, AKS, City Express, DHL), and a *"Pošalji SMS poruku" / "Send SMS message"* option when tapping a detected phone number.
- **Harmful Message Override Confirmation Dialog (Potvrda pre označavanja štetne poruke kao bezbedne):** Added a confirmation dialog (`GlassAlertDialog`) when the user triggers the *"Označi da nije štetna" / "Mark as not harmful"* option (either from the message bubble warning box or the multi-select bar), asking the user to confirm before marking the message(s) as safe.
- **Instant `poslato.wav` Sound on Send Button Tap (Trenutni zvuk `poslato.wav` na pritisak tastera Pošalji):** Configured `R.raw.poslato` (`poslato.wav`) to play immediately as soon as the user presses the Send button in a conversation.
- **Persistent Scheduled Messages & Boot Restoration (`BootCompletedReceiver`) (Trajno čuvanje zakazanih poruka nakon gašenja i restarta uređaja):** Scheduled SMS and MMS messages are now persisted to storage and automatically restored on app launch, device reboot (`BOOT_COMPLETED`), and app update (`MY_PACKAGE_REPLACED`).

### Prepravljene opcije (Modified & Fixed Options)
- **Clean Contact Name in Trash Inspection Header (Čisto ime kontakta u zaglavlju pregleda obrisanih poruka):** Removed the redundant "Obrisane poruke • " window title prefix from the inspection dialog header, displaying exclusively the contact's name (or phone number if nameless) and retention days.
- **Streamlined Message Cards in Trash Inspection Dialog (Uklonjene suvišne prečice u listi obrisanih poruka):** Removed the inline green restore and red delete icon buttons from individual message rows inside the inspection dialog to prevent accidental taps and maximize reading space, keeping action control unified under the bottom batch action buttons.
- **Preserved Conversation List Scroll Position on Back Navigation (Očuvanje pozicije liste razgovora pri povratku nazad):** Hoisted conversation list state (`conversationsListState`) to `MainActivity` so navigating back from an open conversation returns to the exact scroll position and category filter where the user was instead of resetting the list to the top.
- **Dedicated In-Chat Sound `dolazna.wav` vs. Notification Tone (Namenski zvuk `dolazna.wav` u razgovoru u odnosu na ton obaveštenja):** Configured `dolazna.wav` to play strictly and exclusively when an incoming message is received while the user is inside that conversation window, while incoming messages received outside the conversation play the user's configured/system notification tone.
- **Precision Phone Number & Link Detector Filtering (Ispravljeno prepoznavanje brojeva telefona, datuma, poziva na broj, delova linkova i skraćenica):** Eliminated false-positive phone number detections on dates/timestamps (`05/10/2026 17:42:27`, `10/11/2026 22:46:31`, `01.12.2026.`), payment reference numbers (`poziv na broj 25600281895057`), and numeric substrings inside URLs (`https://a1.rs/bill?invoice=...7558855268...`), and prevented abbreviations like `br.kartice*5431` from being misidentified as website links (`https://br.kartice`).
- **Scoped `FileProvider` Security & Direct GitHub APK Updates (Bezbedan `FileProvider` uz podršku za direktno GitHub APK ažuriranje):** Restricted `file_paths.xml` strictly to dedicated subdirectories (`backups/`, `mms_media/`, `mms_attachments/`, `mms_open/`, `mms_pdu/`, `voice_messages/`, `voice_playback_cache/`, `updates/`) instead of root directory exposure while retaining direct in-app GitHub APK updates.
- **OMA-Compliant MMS PDU Encoding, Voice Messages & `.txt` / Office Attachments (Standardizovano MMS slanje i primanje govornih poruka i dokumenata):** Rebuilt `MmsPduHelper` with OMA-compliant WSP `Value-length` + `Type`/`Start` headers, a root `<smil>` presentation part, multi-codec audio detection (`.amr`, `.m4a`, `.3gp`, `.mp3`, `.ogg`, `.aac`, `.wav`), `.txt` attachment differentiation, persistent voice message storage in `filesDir/mms_media`, and `MmsCreationMode.FREE` (300 KB) by default.
- **Non-Blocking Asynchronous SMS/MMS Dispatch & Batch MMS Loading (Trenutno slanje bez kočenja ekrana i serijsko učitavanje MMS-a):** Refactored `SmsMmsRepository.sendMessage` to insert optimistic messages immediately on the UI thread while executing contact lookup, image compression, PDU creation, and `SmsManager` dispatch on `Dispatchers.IO`, and replaced N+1 MMS part queries with a single batched `ContentResolver` query (`queryAllMmsPartsBatch`).
- **Active Enforcement of Blocked Senders, MMS Read Reports & Creation Modes (Aktivna primena blokiranih brojeva, MMS potvrda čitanja i režima kreiranja):** Manually blocked senders are now blocked even when automated spam heuristics are off, `isMmsSendReadReports` updates system MMS read status when marking threads read, `MmsCreationMode` (`RESTRICTED` 100 KB / `WARNING` / `FREE` 300 KB) is enforced across attachments and voice recordings, and manual MMS download preserves the exact SIM `subId`.
- **Exact-Width Conversation List Swipe Clamping (Ograničen svajp na listi konverzacija prema širini teksta opcije):** Dynamically measured and clamped the left and right horizontal swipe distance on the main conversation list (`SwipeableConversationCard`) using `TextMeasurer` so the conversation card only slides as far as needed to display the full icon and label of the option.
- **Comprehensive Message Content Screenshot Blocking (Zabrana skrinšotovanja na svim ekranima sa sadržajem poruka):** Expanded screenshot and screen recording protection (`WindowManager.LayoutParams.FLAG_SECURE`) across the conversation list, active conversations, highlighted messages, deleted messages / Trash (including inspection dialog), and SIM card messages.
- **Bounded Security Cache & Startup Temp File Cleanup (Ograničena keš memorija i automatsko čišćenje privremenih fajlova):** Capped `MessageSecurityAnalyzer` URL cache to 500 entries and added automatic startup cleanup for temporary cache files older than 24 hours.

### Uklonjene opcije (Removed Options)
- **OTA Localization Service (`OtaLocalizationManager` & `PendingLocaleWorker`) (Usluga Over-The-Air lokalizacije):** Completely removed `OtaLocalizationManager.kt`, `PendingLocaleWorker.kt`, and all references to remote OTA localization.
- **`odlazna.wav` Outgoing Sound File (Zvučni efekat `odlazna.wav`):** Deleted unused `res/raw/odlazna.wav` in favor of `res/raw/poslato.wav` when pressing the Send button.

---

## [v3.7] - 2026-11-13

### Dodate opcije (Added Options)
- **Message Bubble Swipe Left & Right (Svajp ulevo i udesno na prozorčić poruke):** Added configurable left and right swipe gestures directly on individual message bubbles inside conversations for instant `(Un)pin` and `(Un)highlight`, featuring a vertical badge layout (icon above label) and a tight `6.dp` gap next to the bubble.
- **Light Feedback Activation Modes (Režimi aktivacije svetlosnog odziva):** Added ringer-mode activation conditions for camera flash alerts: *Always (Uvek)*, *In vibrate mode (U režimu samo vibracija)*, *In silent mode (U režimu nečujno)*, and *In sound mode (U režimu zvuk)*.
- **Universal Direct Update & Any-Network Download Confirmation (Direktno ažuriranje sa bilo koje verzije i preko svake mreže):** Added direct update support from any previously installed version to the newest GitHub release (preserving local database history) and downloading over either Wi-Fi or Mobile Data (Cellular) with a user confirmation dialog showing the active network type and automatic installer continuation after granting install permission.
- **Immediate Background Update Notifications (Trenutna pozadinska obaveštenja o ažuriranju):** Added `AppUpdateCheckWorker` (`WorkManager`) and connectivity listener monitoring so the app sends a system notification as soon as a new release is detected in the background without waiting for the user to open the app.
- **Mark Harmful Message as Safe (Označavanje poruke da nije štetna):** Added the ability to mark a flagged message as not harmful (*"Označi da nije štetna" / "Mark as not harmful"*), available strictly on messages detected as harmful (`SecurityRiskLevel.SUSPICIOUS`) both inside the message warning box and in the selection bar.
- **Conversation Screenshot Protection (Sprečavanje skrinšotovanja konverzacija):** Added an On/Off toggle in *Privacy & Security (Privatnost i bezbednost)* settings directly below *Blocked Senders (Blokirani pošiljaoci)* that prevents taking screenshots and screen recordings (`FLAG_SECURE`) on the conversation list and inside conversations.

### Prepravljene opcije (Modified & Fixed Options)
- **Main Menu & In-Chat Search Scroll and Visual Signaling (Pretraga u glavnom meniju i razgovoru sa skrolovanjem i signalizacijom):** Tapping a conversation from main menu search results, as well as searching inside an open conversation (strictly scoped only to that conversation), now automatically scrolls to the matched message and visually signals it with a *"Pronađena poruka" / "Matched message"* badge, glowing accent border, scale pulse, and highlighted query text inside the bubble.
- **Harmful Message Warning Deep Navigation (Navigacija sa upozorenja o štetnoj poruci na glavnom meniju):** Clicking the red warning in the main menu conversation preview directly opens the conversation, loads thread history if needed, scrolls to the harmful message, and visually highlights it with the option to mark it as not harmful.
- **Consolidated Light Feedback Card & Haptics Reordering (Objedinjeno podešavanje svetlosnog odziva i raspored haptike):** Merged the light feedback toggle and activation mode selector into a single card, and moved *Haptic feedback (Haptički odzivi)* directly above *Light feedback (Svetlosni odziv)*.
- **Conversation Date Pill Color Alignment (Boja vremenske odrednice u razgovoru):** Updated the floating conversation date header color and border to match the scroll-to-bottom down-arrow button.
- **Composer Multi-Line & Attachment Close (`+` / `x`) Stability (Stabilnost polja za unos i zatvaranja priloga):** Fixed horizontal icon alignment in the chat input bar during multi-line typing (preventing overlap with the microphone button) and eliminated layout jumping when tapping the `x` icon to close extra attachment categories.
- **Single-Line Dialog Titles & Concise Action Labels (Jednolinijski naslovi dijaloga i nazivi dugmadi):** Updated *"Podrazumevana SMS i MMS aplikacija"* and confirmation dialogs to display full titles in a single row and full horizontal button labels (*"Ne sada"* and *"Podesi kao podrazumevano"*), while shortening Trash action buttons to *"Odaberi" (Choose)* and *"Vrati" (Restore)*.
- **Preloaded SoundPool Audio Engine (Unapred učitani zvučni efekti):** Upgraded `SmsMmsRepository` sound engine with `forceReload` to preload all 19 `.wav` sound effects from `res/raw` into `SoundPool` for zero-latency playback and refresh flash rhythm caches.

### Uklonjene opcije (Removed Options)
- **Separate "When to activate light feedback" Card (Zasebna kartica „Kada aktivirati svetlosni odziv“):** Removed as a standalone settings card after integrating its selector directly into the *Light feedback* card.
- **"• Protected conversation" Header Suffix (Tekst „• Protected conversation“ u zaglavlju razgovora):** Removed the text label next to the phone number in the conversation header, keeping only the green shield icon before the number as the security indicator.

---

## [v3.6] - 2026-11-12

### Dodate opcije (Added Options)
- **Multi-SIM & eSIM Subscription Routing (Preusmeravanje za više SIM kartica i eSIM):** Added `SubscriptionManager` integration using `SmsManager.createForSubscriptionId(subId)` to dynamically send and receive SMS/MMS from the active physical SIM or eSIM slot.
- **Centralized `MmsPayloadValidator` (300 KB) & Lossless IANA MIME Mapping (Provera veličine MMS-a i IANA MIME mapiranje):** Added strict 300 KB PDU payload validation with 8 KB OMA-SMIL overhead reserve, lossless MIME mapping for office documents, spreadsheets, presentations, notes, audio, contacts, and calendar files, and pre-flight cellular connectivity checks (`DeliveryStatus.FAILED_NO_DATA`).
- **OTA Localization Engine & Espresso / Lavender Themes (Over-The-Air lokalizacija i teme Espreso i Lavanda):** Added background `PendingLocaleWorker` OTA translation updates and `ESPRESSO_CREAM`, `MONO_ESPRESSO`, `LAVENDER_DREAM`, and `MONO_LAVENDER` themes with procedural `COFFEE_BEANS` and `LAVENDER_PETAL` textures.

### Prepravljene opcije (Modified & Fixed Options)
- **EXIF Image Orientation & Compose `@Immutable` Stability (Ispravka rotacije slika i stabilnost prikaza):** Fixed portrait photo rotation prior to MMS compression, annotated UI models with `@Immutable`, and enabled `StrictMode` in debug builds.
- **Synchronized Send Countdown Cancel Animation (Glatka animacija poništavanja odbrojavanja slanja):** Refactored the send/cancel countdown slot with `AnimatedContent` and a 250ms draft restoration delay so cancelling a message never causes card resizing or text jumping.
- **Automatic SMSC Display (Automatski prikaz SMSC broja):** Updated the SMSC settings card to automatically display the carrier/SIM-detected service center number.

### Uklonjene opcije (Removed Options)
- **Manual Carrier APN Configuration Cards & Obsolete Mobile Data Toggles (Ručne APN kartice i zastareli prekidači mobilnih podataka):** Removed redundant manual APN cards and legacy mobile data toggles from Messaging & Network settings.
- **3-Minute MMS Polling Service & Offline Audio Transcoding (Periodični 3-minutni MMS servis i naknadno transkodovanje zvuka):** Removed periodic 3-minute background MMS polling in favor of real-time `WAP_PUSH_DELIVER` broadcasts, and removed offline audio transcoding in favor of direct native AMR-NB recording.

---

## [v3.5] - 2026-11-12

### Dodate opcije (Added Options)
- **SMSC & SIM Operator MMS Parameter Auto-Detection (Automatska detekcija MMS parametara prema SMSC broju i operateru):** Added automatic resolution of carrier MMSC URL, proxy, and port based on SMSC number and SIM operator (mts, Yettel, A1, Globaltel, HT Eronet, BH Telecom, m:tel, Hrvatski Telekom, Telekom Slovenije, T-Mobile, Vodafone, AT&T, Verizon).
- **AMR-NB Voice Header Validator (Provera `#!AMR\n` zaglavlja glasovnih poruka):** Added `AmrAudioConverter` validation to ensure standard `#!AMR\n` header compliance for voice attachments prior to MMS transmission.

### Prepravljene opcije (Modified & Fixed Options)
- **First-Install Security Defaults (Podrazumevana podešavanja zaštite pri prvoj instalaciji):** Updated initial install defaults so optional advanced security toggles (firewall, database encryption, SSL pinning, metadata minimization, root check) start disabled while malicious website detection starts enabled.
- **Static External Links Card & Single-Line Settings Titles (Statična kartica eksternih linkova i jednolinijski naslovi):** Updated settings card titles to single-line layout and stabilized the External Links card layout.

---

## [v3.4] - 2026-11-12

### Dodate opcije (Added Options)
- **In-App GitHub APK Downloader & Package Installer (Direktno preuzimanje i pokretanje instalacije iz aplikacije):** Added in-app APK downloading from GitHub Releases with live percentage progress and automatic native Android package installer launch without opening an external browser.

### Prepravljene opcije (Modified & Fixed Options)
- **Pin & Unpin Spring Placement Motion (Animacija opruge pri kačenju i otkačinjanju razgovora):** Updated conversation list pinning/unpinning with smooth spring placement animations anchored to the current scroll position.
- **Default Theme & Wallpaper Presets (Podrazumevana tema i pozadina):** Updated clean-install defaults to System theme mode and the *Starlight Galaxy* wallpaper preset.

---

## [v3.3.1] - 2026-11-12

### Dodate opcije (Added Options)
- **Selective Trash Message Inspection & Recovery (Selektivni pregled, vraćanje i brisanje poruka u korpi):** Added per-conversation message inspection inside Trash to selectively restore individual messages to the inbox or permanently delete them, with *Select All / Deselect All* controls.
- **Unread Category Counter Badge (Brojač nepročitanih poruka na kategoriji):** Added a live unread message count next to the *Unread / Nepročitano* category chip (e.g., `Nepročitano (3)`).
- **Burgundy Theme Header Branding (Ikonica medveda za Burgundi teme):** Added a custom bear icon in the main top header for *Polychrome Burgundy Anita* and *Monochrome Burgundy Accent* themes with automatic light/dark asset switching.

### Prepravljene opcije (Modified & Fixed Options)
- **Serbian Latin Conversation Date Headers (Datumski zaglavci na srpskoj latinici):** Updated conversation date dividers to always format in Serbian Latin script (e.g., `utorak, 19. maj`) instead of Cyrillic.
- **Scroll Gesture Isolation from Long-Press Selection (Razdvajanje skrolovanja od dugog pritiska):** Updated touch handling so scrolling over message bubbles never triggers press scaling or accidental selection (selection requires a stationary 500ms long-press).
- **Suppressed False Alert Sounds on Thread Open (Sprečeno lažno okidanje zvuka i blica pri ulasku u razgovor):** Fixed thread opening and history loading so they never trigger incoming message sounds or camera flash alerts.

---

## [v3.3] - 2026-11-11

### Dodate opcije (Added Options)
- **SQLCipher Room Encrypted Database (SQLCipher Room šifrovana baza podataka):** Added Room KSP database storage encrypted with SQLCipher and granular per-thread lazy history loading (`ConversationHistoryEntity`).
- **Explicit Root Package Visibility Queries (Vidljivost paketa za root proveru):** Added `<queries>` declarations in `AndroidManifest.xml` for accurate root manager detection on modern Android versions.

### Prepravljene opcije (Modified & Fixed Options)
- **Message Deduplication & Sync Throttling (Deduplikacija poruka i kontrola sinhronizacije):** Improved automatic message ID and content deduplication between local database snapshots and telephony provider syncs.

### Uklonjene opcije (Removed Options)
- **Legacy Blocking `TxturaApp` Initialization (Zastarela blokirajuća inicijalizacija pri startu):** Removed blocking cold-start initialization overhead in favor of asynchronous startup loading.

---

## [v3.2] - 2026-11-11

### Dodate opcije (Added Options)
- **System Back Gesture Navigation (Sistemski pokret za nazad):** Added `BackHandler` integration across screens so Android system back gestures match in-app back navigation.

### Prepravljene opcije (Modified & Fixed Options)
- **Non-Blocking Startup, v2 Cache Key Migration & Background Backup Crypto (Asinhrono pokretanje, v2 migracija ključa keša i pozadinski bekap):** Moved preference loading, v1-to-v2 Android Keystore cache key migration, and PBKDF2 backup encryption/decryption off the main thread.
- **Asynchronous SMS/MMS Delivery & WAP Push Handling (Asinhrona obrada isporuke i WAP Push paketa):** Updated sent/downloaded callbacks and incoming WAP Push validation to process asynchronously.

### Uklonjene opcije (Removed Options)
- **Redundant Pin Icon on Conversation Cards When Assigned to Swipe (Dupla ikonica čiode kada je kačenje dodeljeno svajpu):** Automatically hides the pin icon on conversation cards when pin/unpin is already configured as the left or right swipe action.

---

## [v3.1] - 2026-11-10

### Dodate opcije (Added Options)
- **Customizable Drag-and-Drop Dock Bar (Prilagodljivi donji dok bar):** Added drag-and-drop reordering for bottom dock icons and kept the dock visible on *Deleted Messages* and *Highlighted Messages* screens.
- **Keyboard GIF Support up to 299.9 KB (GIF animacije sa tastature):** Added direct sending and animated in-bubble playback for GIFs inserted from the keyboard.

### Prepravljene opcije (Modified & Fixed Options)
- **Physical Storage Purge on Empty Trash (Fizičko brisanje poruka i fajlova pri pražnjenju korpe):** Updated permanent deletion to remove both telephony SMS/MMS records and cached media attachment files from internal storage.
- **Startup Calendar Permission & Audio-Synchronized Flash Timing (Dozvola za kalendar pri pokretanju i PCM sinhronizacija blica):** Included calendar permission in the initial permission request and synchronized camera flash pulses with the PCM audio rhythm of the notification sound.

---

## [v3.0] - 2026-11-10

### Dodate opcije (Added Options)
- **In-Chat Voice Messages (Glasovne poruke u razgovoru):** Added voice message recording, timer display, playback, and cancellation inside conversations.
- **Calendar Event (`.ics`) Sharing, Document Attachments & Image Compression (Deljenje kalendarskih događaja, dokumenata i kompresija slika):** Added calendar event picker with `.ics` generation, document attachments, and automatic image compression for MMS.
- **Camera Flash Notifications (Obaveštenja blicem kamere):** Added camera flashlight alerts for incoming messages.

### Prepravljene opcije (Modified & Fixed Options)
- **Asynchronous Incoming MMS Downloads & Feature-Based Permissions (Asinhrono preuzimanje MMS-a i pojedinačne dozvole):** Added background MMS downloading and on-demand runtime permission prompts.

---

## [v2.4] - 2026-11-09

### Dodate opcije (Added Options)
- **6 Studio Notification Tones, Action Audio Effects & System Sound Shortcut (6 prilagođenih tonova, zvučni efekti akcija i prečica za sistemski zvuk):** Added 6 built-in notification tones (*Dupli puls*, *Kristalni zvončić*, *Meki pop*, *Ritam kod*, *Topla kalimba*, *Diskretni oblak*) with instant preview, tactile `.wav` audio effects for message actions (pin, highlight, trash, permanent delete, read/unread), and a *"Podesi zvuk sistema"* shortcut to Android notification channel settings.
- **Selective Chat Screenshot Protection & Chat Keep-Awake (Selektivna zabrana skrinšotovanja četova i budan ekran u razgovoru):** Added a dedicated Privacy & Security toggle enforcing `FLAG_SECURE` only on chats and message lists, and `FLAG_KEEP_SCREEN_ON` while an active conversation is open.

### Prepravljene opcije (Modified & Fixed Options)
- **Single Active Dropdown Menu & Fast Return to Main Screen (Isključivost padajućih menija i brz povratak na glavni ekran):** Enforced single-menu exclusivity across action bars and eliminated the 2–3s delay when navigating back to the conversation list.

### Uklonjene opcije (Removed Options)
- **Unconditional Whole-App Screenshot Blocking & Recipient Field Contact Icon (Bezuslovna zabrana skrinšotovanja cele aplikacije i ikonica u polju primaoca):** Removed global screenshot blocking on settings screens and removed the contact icon inside the recipient text field to maximize input width.

---

## [v2.2] - 2026-11-09

### Dodate opcije (Added Options)
- **Swipe Threshold & Category Reordering Haptics (Haptika pri prevlačenju i promeni rasporeda kategorija):** Added tactile vibration feedback on conversation swipe thresholds, pinned message carousel switching, and category tab drag-reordering.

### Prepravljene opcije (Modified & Fixed Options)
- **Multi-Factor Conversation Deduplication (Višefaktorsko spajanje duplih konverzacija):** Consolidated fragmented telephony thread IDs for the same contact using normalized phone digits and phonebook names.
- **Smart Service Categorization & Serbian Tab Sizing (Tačna kategorizacija „Usluge“ i širina kartica na srpskom):** Protected named contacts and replied-to threads from false *Services* classification caused by OTP codes, and adjusted category tab widths so *"Nepročitano"* is never truncated.

---

## [v2.1] - 2026-11-09

### Dodate opcije (Added Options)
- **Multi-Recipient Broadcast SMS (Višestruko pojedinačno slanje SMS poruka):** Added simultaneous sending of a single message to multiple selected contacts and entered phone numbers as separate 1-on-1 SMS chats (without group MMS), with *Select All / Deselect All*, live character/segment counter, and a shortcut in the conversation menu.

### Prepravljene opcije (Modified & Fixed Options)
- **Biometric Unlock Reliability (Pouzdanost biometrijskog otključavanja):** Added `USE_BIOMETRIC` permission and native `BiometricPrompt` (fingerprint, face, device PIN/passphrase), safe fallback when no device lock is set, and fixed re-lock loops when returning from system authentication dialogs.

---

## [v2.0] - 2026-11-08

### Dodate opcije (Added Options)
- **Security Suite: Biometric Lock, AES-256-GCM Cache, SSL Pinning, Root Check & Encrypted Backups (Bezbednosni paket i aktivni štit):** Added app lock with configurable timeouts (*Immediate*, *1 min*, *5 min*), Android Keystore AES-256-GCM local snapshot encryption, SPKI SHA-256 certificate pinning, EXIF GPS metadata stripping, root integrity scanner, password-protected portable backups, and an interactive *Active Security Measures* hub from the settings header shield icon.
- **Smart Contact Picker & GitHub Release Checker (Pametni birač kontakata i provera ažuriranja sa GitHub-a):** Added real-time fuzzy contact search when composing a new message and GitHub release checking in *System & About*.

### Prepravljene opcije (Modified & Fixed Options)
- **7-Category Settings Redesign, Floating Dropdown Menus & Red "X" Send Recall (Redizajn podešavanja u 7 kategorija, plutajući meniji i crveni „X“ za opoziv slanja):** Reorganized settings into 7 numbered categories with compact right-aligned floating dropdown selectors, and updated the 6-second send countdown button into a red *"X"* cancel action with `cancel.wav` audio feedback.

### Uklonjene opcije (Removed Options)
- **Top-Level Settings Category Subtitles & Modal Option Picker Dialogs (Podnaslovi glavnih kategorija podešavanja i modalni dijalozi izbora):** Removed verbose subtitles from main settings category cards and replaced full-screen popup selection dialogs with compact floating menus.

---

## [v1.5.1] - 2026-11-08

### Dodate opcije (Added Options)
- **30-Day Trash Folder & Message Restore (Premeštanje obrisanih poruka u Korpu sa rokom od 30 dana):** Deleting individual or multi-selected messages moves them to *Deleted Messages (Trash)* with support for restoring to the conversation and automatic 30-day purging.

### Prepravljene opcije (Modified & Fixed Options)
- **Full-Width Bottom Dock & Collapsible Bubble / Link Cards (Donji bar u punoj širini i sklopljive kartice za Mehuriće i Linkove):** Expanded the bottom navigation dock and multi-selection bar to full width and grouped *Bubbles* and *External Links* settings into collapsible cards.

### Uklonjene opcije (Removed Options)
- **Artificial Message Query `LIMIT` (Veštačko ograničenje broja učitanih poruka):** Removed database query `LIMIT` constraints so the app loads the complete SMS/MMS message history stored on the device.

---

## [v1.5] - 2026-11-08

### Dodate opcije (Added Options)
- **In-Chat Search, Highlighted Hub, Message Pinning & Per-Thread Drafts (Pretraga u razgovoru, Hajlajtovane poruke, Pinovanje i Nacrti):** Added in-conversation search with match counter, centralized *Highlighted Messages* screen with jump-to-message, pinning messages to the top of a thread, and automatic per-thread `[Nacrt] / [Draft]` persistence.
- **Smart Phone, Email & Bank Account Recognition + Contact Sharing & Birthday Preset (Prepoznavanje brojeva, emailova i tekućih računa + deljenje kontakata i rođendanski šablon):** Added quick actions for detected phone numbers, emails, and 18-digit bank accounts, contact sharing into the compose bar, and a midnight (`00:00`) birthday scheduling preset.
- **Cloudflare DoH Link Guard, SIM Card Manager, 6s Delayed Send & AMOLED Themes (Cloudflare DoH zaštita, SIM poruke, odloženo slanje i AMOLED teme):** Added Cloudflare DoH malicious link scanning, SIM card message management, a 6-second delayed send undo window, *Pinch-to-zoom* bubble scaling, notification bubbles, and pure AMOLED black and white themes.

### Prepravljene opcije (Modified & Fixed Options)
- **Manual People ↔ Services Sender Override & Link Preview Filters (Ručna promena kategorije pošiljaoca i filteri pregleda linkova):** Added manual sender category switching between *People* and *Services* from the chat menu and configurable automatic link previews (*Wi-Fi only*, *Web links only*).

---

## [v1.0] - 2026-11-08

### Dodate opcije (Added Options)
- **Advanced SMS/MMS Controls & Emergency Alerts Shortcut (Napredne SMS/MMS opcije i prečica za hitna upozorenja):** Added SMS/MMS delivery reports, MMS read receipts, configurable MMS auto-retrieve (*always*, *never*, *when not roaming*), MMS creation modes (*Free*, *Warning*, *Restricted*), scheduled messages, and a shortcut to system wireless emergency alerts.

### Prepravljene opcije (Modified & Fixed Options)
- **Full-Width Category Tabs & Frosted Glass Contrast (Kartice kategorija u punoj širini i stakleni kontrast):** Refined the *People*, *Services*, *Unread*, and *All* category bar, theme-adaptive send button, and text contrast over custom wallpapers.

### Uklonjene opcije (Removed Options)
- **Mock Sample Conversations (Testni primeri razgovora):** Removed all mock/sample messages from production builds.

---

## [v0.9 Beta] - 2026-11-05

### Dodate opcije (Added Options)
- **Smart Categories, Conversation Swipe Actions & Blocked Senders (Pametne kategorije, svajp radnje nad razgovorima i blokiranje pošiljalaca):** Added automatic conversation sorting into *People*, *Services*, and *Unread*, configurable conversation card swipe actions, and sender blocking.

---

## [v0.8 Alpha] - 2026-10-28

### Dodate opcije (Added Options)
- **Initial Release & Core Architecture (Početno izdanje i osnovna arhitektura):** Added the dual-SIM SMS/MMS telephony engine, offline JSON backup and restore, and the original Material 3 Glassmorphism UI with 7 themes and custom wallpapers.
