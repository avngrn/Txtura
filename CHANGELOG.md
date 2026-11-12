# Txtura Changelog & Improvements

## [v3.6] - 2026-11-12

### SIM / eSIM & MMS Engine (SIM / eSIM i MMS motor)
- **Multi-SIM & eSIM Subscription Routing (Preusmeravanje za više SIM kartica i eSIM):** Full `SubscriptionManager` integration utilizing `SmsManager.createForSubscriptionId(subId)` to dynamically send and receive SMS/MMS from the active physical SIM or eSIM slot.
- **Pre-Flight MMS Payload Validation (Centralizovani MmsPayloadValidator do 300 KB):** Integrated central `MmsPayloadValidator` enforcing strict 300 KB (307,200 bytes) PDU payload limits with an 8 KB overhead allowance for OMA-SMIL headers and text.
- **Lossless MIME Attachment Mapping (Potpuno IANA MIME mapiranje bez gubitka podataka):** Comprehensive IANA MIME type mapping for documents (`.docx`, `.doc`, `.pdf`, `.odt`), spreadsheets (`.xlsx`, `.xls`, `.csv`, `.ods`), presentations (`.pptx`, `.ppt`, `.ppsx`, `.odp`), notes (`.txt`, `.rtf`, `.md`), audio (`.amr`, `.aac`, `.ogg`, `.mp4`), and contacts/calendar (`.vcf`, `.ics`, `.vcs`). Oversized documents > 300 KB are gracefully rejected without lossy compression.
- **Pre-Flight Connectivity Validation (Provera mobilne konekcije):** Network capabilities check (`NET_CAPABILITY_MMS` / `NET_CAPABILITY_INTERNET` & `TRANSPORT_CELLULAR`) prevents failed transaction loops when mobile data is disabled, gracefully setting message status to `DeliveryStatus.FAILED_NO_DATA`.

### Architecture, Performance & Stability (Arhitektura, performanse i stabilnost)
- **AOSP Telephony Cleanup (Čišćenje pozadinskih servisa):** Removed redundant 3-minute MMS polling services in favor of real-time `WAP_PUSH_DELIVER` broadcasts, and deprecated offline audio transcoding in favor of native direct AMR-NB microphone recording.
- **EXIF Image Orientation Fix (Ispravka rotacije slika):** Eliminated duplicate EXIF stripping in `MainActivity` prior to `MmsImageCompressor`, ensuring portrait photos retain correct rotation.
- **Jetpack Compose Stability & StrictMode (Stabilnost prikaza i StrictMode):** Annotated core UI models (`ChatMessage`, `ConversationThread`, `MmsAttachment`) with `@Immutable` to eliminate unnecessary recompositions, and configured `TxturaApplication` with `StrictMode` for debug builds.
- **OTA Localization Engine & WorkManager Sync (Over-The-Air lokalizacija):** Integrated zero-blocking OTA localization architecture with strict English fallback and background `PendingLocaleWorker` (WorkManager) for automatic locale updates without APK re-releases.

### UI, Themes & Animations (Interfejs, teme i animacije)
- **Espresso & Lavender Themes with Custom Textures (Teme Espreso i Lavanda sa novim teksturama):** Added rich `ESPRESSO_CREAM` polychrome theme, `MONO_ESPRESSO` dark roasted brown monochrome theme, and `LAVENDER_DREAM` / `MONO_LAVENDER` purple themes with bespoke `COFFEE_BEANS` and `LAVENDER_PETAL` tactile procedural textures.
- **Synchronized Send Countdown Cancel Animation (Glatko poništavanje slanja poruka):** Refactored composer send/cancel countdown action slot using `AnimatedContent` with scale and fade transitions. Delayed draft text restoration by 250ms until exit animations completely finish, eliminating icon glitches, text jumping, and card layout resizing.
- **Settings Streamlining & SMSC Readonly Display (Pojednostavljanje podešavanja mrežnih servisa):** Removed redundant carrier APN configuration cards and obsolete mobile data toggles, replacing the SMSC card with an automatic system/carrier detected number display.

## [v3.5] - 2026-11-12

### MMS Telephony & Background Sync (MMS telefonija i pozadinska sinhronizacija)
- **Periodic background MMS retry engine (Automatska pozadinska MMS sinhronizacija):** Background process running every 3 minutes via `AlarmManager`, `JobScheduler`, and in-process timer to automatically retry stuck outgoing MMS messages and retrieve pending incoming MMS notifications from carrier MMSC.
- **SMSC-based MMS APN auto-detection (Automatska detekcija MMS APN podešavanja):** Background detection and automatic setup of carrier MMS APN parameters (MMSC URL, Proxy, Port) based on SMSC center number and SIM operator (mts, Yettel, A1, Globaltel, HT Eronet, BH Telecom, m:tel, Hrvatski Telekom, Telekom Slovenije, T-Mobile, Vodafone, AT&T, Verizon, etc.).

### Audio Encoders & Media Processing (Zvučni enkokderi i obrada medija)
- **AMR-NB audio format converter (Konverzija i provera AMR-NB formata glasovnih poruka):** Integrated `AmrAudioConverter` to guarantee standard `#!AMR\n` header compliance and transcode non-AMR audio formats to valid AMR-NB before MMS transmission.

### UI Layouts & Settings Refinements (Fino podešavanje interfejsa i podešavanja)
- **Symmetrical APN card & single-line titles (Simetrična APN kartica i naslovi u jednom redu):** Redesigned MMS APN card with 50/50 symmetrical action buttons, clean proxy formatting, single-line text wrapping for settings cards, and static card layout for External Links.
- **Composer countdown layout fix (Sprečavanje treperenja pri poništavanju slanja):** Isolated Send/Cancel countdown timer slot into a fixed-width container, eliminating horizontal layout shifts and text squeezing when touching 'X'.
- **Initial install security defaults (Podrazumevana podešavanja zaštite pri instalaciji):** Security features set to disabled by default on first install (firewall, db encryption, SSL pinning, metadata minimization, root check), with malicious website detection enabled by default.

## [v3.4] - 2026-11-12

### App Updates & Installation (Ažuriranje i instalacija)
- **Direct GitHub Release updater (Direktno preuzimanje i instalacija ažuriranja):** Download APK updates directly from GitHub Releases with live percentage progress, launching the native Android package installer without opening an external browser.

### Messaging, Gestures & Animations (Poruke, pokreti i animacije)
- **Pin and unpin spring motion (Animacija opruge pri pinovanju i otpinovanju):** Smooth spring placement animation when pinning/unpinning conversations, anchored to current scroll index without list jumps.

### Settings & Defaults (Podešavanja i podrazumevane vrednosti)
- **Updated default options (Osvežene podrazumevane opcije):** System theme as default, Starlight Galaxy wallpaper preset, and refined default notification sounds.

## [v3.3.1] - 2026-11-12

### Selective Trash Management & Storage (Upravljanje obrisanim porukama i skladište)
- **Granular Trash inspection and recovery (Selektivno vraćanje i brisanje iz smeća):** Inspect deleted conversations in Trash and select individual messages to restore to the inbox or permanently purge from device memory, with bulk select/deselect controls.

### UI, Customization & Localization (Interfejs, prilagođavanje i lokalizacija)
- **Serbian Latin date headers (Datumski zaglavci na srpskoj latinici):** Conversation date dividers use Serbian Latin script (e.g., `utorak, 19. maj`) instead of Cyrillic for consistent readability.
- **Unread category count indicator (Brojač nepročitanih poruka):** The "Nepročitano" / "Unread" category chip displays the total number of currently unread messages in real-time (e.g., `Nepročitano (3)`).
- **Burgundy theme branding (Brendiranje za Burgundi teme):** Custom bear icon rendered in the main top header for Monochrome Burgundy Accent and Polychrome Burgundy Anita themes, with automatic light/dark mode asset switching and theme color filtering.
- **Header & Footer refinements (Fino podešavanje zaglavlja i podnožja):** Single-line formatting for changelog dialog title and centered two-line layout for settings footer privacy badge.

### Gestures, Touch & Notification Fixes (Pokreti, dodir i obaveštenja)
- **Smooth scrolling without accidental selection (Glatko skrolovanje bez lažnog obeležavanja):** Optimized touch gesture pipeline so scrolling over message bubbles never triggers press scaling, selection, or message highlighting. Selection is strictly restricted to a 500ms stationary long-press.
- **Fix false incoming alert sounds on thread open (Ispravka lažnog zvuka i blica pri ulasku u razgovor):** Opening a thread or loading historical messages no longer triggers incoming message sounds or flash alerts. Notifications are strictly reserved for real-time incoming messages.

## [v3.3] - 2026-11-11

### Security & Database Architecture (Bezbednost i arhitektura baze)
- **SQLCipher Room database storage (SQLCipher Room baza):** Encrypted database storage with Room KSP, SQLCipher database encryption, and granular lazy loading for per-conversation histories (`ConversationHistoryEntity`).
- **Security & Package Visibility (Bezbednost i vidljivost):** Added explicit package visibility `<queries>` in `AndroidManifest.xml` for root management tools and refined HTTPS/TLS certificate pinning in `AppSecurityManager`.

### Performance & Lifecycle (Performanse i životni ciklus)
- **Streamlined application lifecycle (Optimizovani životni ciklus):** Migrated asynchronous repository and emergency alert initialization directly into `MainActivity.onCreate()`, eliminating `TxturaApp.kt` overhead for faster cold starts.
- **Message deduplication & sync throttling (Deduplikacija poruka i kontrola sinhronizacije):** Automatic content and ID deduplication across database snapshots and real-time streams prevents duplicated messages in conversation threads.

## [v3.2] - 2026-11-11

### Performance & Security (Performanse i bezbednost)
- **Non-blocking startup (Start bez blokiranja):** Moved preference, cache, and emergency-alert initialization off the main thread. Startup waits asynchronously for the local message snapshot before syncing device messages.
- **AES-256-GCM cache key management (Upravljanje ključem keša):** Local JSON snapshots now use a random 256-bit data key wrapped by Android Keystore and cached in memory for the process session. Existing v1 cache files migrate to v2 after successful decryption.
- **Backup crypto off the UI thread (Kriptografija bekapa van UI niti):** Password-based PBKDF2 remains enabled for portable backups; backup reading, parsing, encryption, and decryption run on background dispatchers.
- **Accurate encryption labels (Tačan opis enkripcije):** Security settings now clearly describe AES-256-GCM protection of the local JSON message cache.

### Messaging, Gestures & Navigation (Poruke, pokreti i navigacija)
- **Pin and unpin list motion (Animacija pinovanja i otpinovanja):** Conversation rows animate between their original position and the top of the list. Unpinning a message keeps the message list at the top instead of jumping to the message's chronological position. Pin controls are hidden when either swipe direction is configured for pin/unpin.
- **System back gestures (Sistemski pokret za nazad):** Android back gestures use the same screen navigation behavior as the in-app back controls.
- **SMS/MMS delivery handling (Obrada slanja i prijema SMS/MMS poruka):** Sent and downloaded message callbacks are registered; incoming WAP Push payloads are validated and processed asynchronously by the MMS receiver.

## [v3.1] - 2026-11-10

### Storage & Permissions (Memorija i dozvole)
- **Permanent deletion (Trajno brisanje):** Emptying Trash now removes SMS/MMS records and cached media attachments from device storage, then saves the updated local cache.
- **Calendar access (Pristup kalendaru):** Calendar permission is requested with the initial permissions so calendar events and ICS sharing can work.

### UI, Navigation & Notifications (Interfejs, navigacija i obaveštenja)
- **Customizable dock (Prilagodljivi dok):** Dock items can be reordered, and the dock remains available on Deleted and Highlighted screens.
- **Smoother transitions (Glatkiji prelazi):** Spring transitions between the main screen, Settings, Deleted, and Highlighted screens reduce lag and flicker.
- **Keyboard GIFs (GIF animacije sa tastature):** Send keyboard GIFs up to 299.9 kB and play them in message bubbles.
- **Flash and audio feedback (Blic i zvučni odziv):** Flash alerts synchronize with incoming SMS sounds, with expanded sound and action feedback options.

## [v3.0] - 2026-11-10

### Messaging & Media (Poruke i multimedija)
- **Incoming MMS downloads (Preuzimanje dolaznih MMS poruka):** Added asynchronous MMS download handling and validated WAP Push processing.
- **Voice messages (Glasovne poruke):** Added in-chat voice recording, playback, and cancellation controls.
- **Attachments and compression (Prilozi i kompresija):** Added document attachments and image compression for MMS.

### Permissions & Notifications (Dozvole i obaveštenja)
- **Granular permissions (Pojedinačne dozvole):** Permissions are requested as needed for app features.
- **Calendar events (Događaji u kalendaru):** Added calendar event selection and ICS generation and sharing.
- **Flash notifications (Obaveštenja blicem):** Added configurable camera-flash alerts for incoming messages.

## [v2.4] - 2026-11-09

### Audio & Notifications (Audio i obaveštenja)
- **Custom In-App Notification Sounds (Prilagođeni tonovi obaveštenja za poruke):**
  - Integrated 6 curated, studio-mastered offline SMS notification tones: *Dupli puls* (Double Pulse), *Kristalni zvončić* (Crystal Chime), *Meki pop* (Soft Pop), *Ritam kod* (Rhythm Code), *Topla kalimba* (Warm Kalimba), and *Diskretni oblak* (Discreet Cloud).
  - Dynamic preview audio playback instantly when selecting any notification sound in Settings.
- **Dedicated System Sound Configuration (Podešavanje zvuka sistema):**
  - Added dynamic "Podesi zvuk sistema" (Set system sound) action row that surfaces specifically when "Sistemski ton" (System tone) is selected.
  - Tapping directly opens the Android system notification channel settings where users can customize default system melodies, ringtones, and vibration patterns.
  - Rephrased sound toggles to concise "Outgoing and incoming message sounds" / "Zvuk za odlazne i dolazne poruke".
- **Tactile In-App Action Audio Effects (Zvučni efekti akcija u aplikaciji):**
  - Crisp audio feedback for key message interactions: highlighting (`highlight_poruke.wav`), unhighlighting (`unhighlight_poruke.wav`), moving to trash (`kanta_brisanje.wav`), permanent deletion (`kanta_poklopac_pop.wav`), pinning (`pin_poruke.wav`), unpinning (`unpin_poruke.wav`), and marking read/unread (`procitano.wav` / `neprocitano.wav`).

### Privacy & Security (Privatnost i bezbednost)
- **Selective Chat & Message List Screenshot Protection (Zabrana skrinšotovanja četova i liste poruka):**
  - Replaced former unconditional whole-app screenshot blocking with a dedicated setting in *Privatnost i bezbednost* placed directly below Biometric Protection.
  - Dynamically enforces `FLAG_SECURE` only when actively viewing conversation lists, active chats, or deleted messages, allowing users to safely screenshot settings and auxiliary views when needed.

### UX, Navigation & Messaging (Korisničko iskustvo i navigacija)
- **New Message Recipient Experience (Prošireno polje za unos primalaca):**
  - Removed contact picker icon inside the recipient input field to grant full screen width and clear visibility of all recipient chips and typed numbers.
- **Conversation Screen Keep-Awake (Zadržavanje uključenog ekrana u razgovoru):**
  - Automatically holds the device screen awake (`FLAG_KEEP_SCREEN_ON`) while inside an active chat window, preventing unexpected display timeouts during reading or typing.
- **Single Active Dropdown Menu (Isključivost padajućih menija):**
  - Enforced single-menu exclusivity across all action bars to eliminate overlapping popup menus.
- **Smooth Main Screen Transition (Optimizacija brzine povratka na glavni ekran):**
  - Eliminated the 2–3s delay/stutter when navigating back to the main conversations screen.

## [v2.2] - 2026-11-09

### Deduplication & Optimization (Deduplikacija i optimizacija)
- **Advanced Thread Deduplication (Napredna deduplikacija konverzacija):**
  - Robust merging of fragmented threads from disparate telephony database IDs into unified conversation entries.
  - Multifactor contact matching based on normalized phone digits, shared message senders, and device phonebook names.
  - Consistent and consolidated view across inbox and main conversation filters.


### Contacts & Categories (Kontakti i pametne kategorije)
- **Smart Service Identification (Ispravka kategorije Usluge):**
  - Named contacts in phonebook and threads with user replies are protected from false service categorization.
  - Removed accidental service flags triggered by OTP / verification code patterns in personal messages.

### UX, Gestures & Localization (Korisničko iskustvo i lokalizacija)
- **Haptic Feedback on Swipe Gestures (Haptika na prevlačenju):**
  - Tactile haptic feedback on conversation card swipes, drag threshold snap crossing, pinned message carousel switching, and category drag-reordering.
- **Serbian UI & Layout Polish (Lokalizacija na srpskom jeziku):**
  - Fixed category tab weights and auto-sizing so "Nepročitano" is never truncated.
  - Shortened Serbian labels in 2-column cards, trash buttons ("Isprazni sve"), and lock screen buttons with ellipsis protection.

## [v2.1] - 2026-11-09

### Multi-Recipient Messaging (Slanje na više primalaca)
- **Broadcast SMS (Grupno slanje SMS-a):** Send one message to selected contacts and entered numbers as separate 1-to-1 SMS messages, with recipient selection, Select All/Deselect All, live character and segment counts, and a shortcut in conversation details.
- **Biometric reliability (Pouzdanija biometrija):** Added native BiometricPrompt and device credential support, safe fallback when no screen lock is configured, and fixes for relocking after returning from system authentication.

## [v2.0] - 2026-11-08

### Privacy & Security (Privatnost i bezbednost)
- **Local Message Cache Encryption (Šifrovanje lokalnog keša poruka):**
  - Authenticated AES-256-GCM encryption of the local conversation snapshot with a random data key wrapped by Android Keystore.
  - Cache format migration and asynchronous re-encryption when the setting changes.
- **Biometric Protection & PIN/Passphrase (Biometrijska zaštita i PIN/Passphrase):**
  - Native biometric authentication (fingerprint and face unlock) and device PIN/passphrase protection via `KeyguardManager`.
  - Configurable lock timeout options: Immediate (Odmah), 1 minute, and 5 minutes.
  - Dedicated "Zaključaj aplikaciju odmah (Test)" button for immediate testing.
  - Elegant full-screen `AppLockScreen` overlay completely gating conversations and dialogs until authenticated.
- **HTTPS & TLS/SSL Pinning (SSL Pinning):**
  - Strict Public Key (SPKI) SHA-256 fingerprint certificate pinning for GitHub releases and Cloudflare DNS threat feeds.
  - Mandatory enforcement that automatically blocks plaintext HTTP connections (`SecurityException`).
- **Metadata Minimization / Zero Metadata (Minimizacija metapodataka):**
  - Automated EXIF metadata stripping: removes GPS geolocation coordinates, camera model, and device identifiers from MMS photos before transmission.
  - Screen privacy: dynamically sets `WindowManager.LayoutParams.FLAG_SECURE` to block screenshots and screen recording.
  - Notification privacy: redacts lock-screen notification content via `setPublicVersion` placeholder ("Txtura • Nova poruka").
- **Root State Detection (Detekcija "Root" stanja):**
  - Deep system integrity scan checking 12 `su` binary locations, `which su` execution, Magisk/SuperSU/KernelSU/APatch managers, `/system` mount status, and `test-keys` build tags.
  - Real-time status badge with on-demand "Ponovo skeniraj" re-scan capability.
- **Blocked Senders (Blokirani kontakti):**
  - Fully integrated into Category 7 with fast blocking, unblocking, and inbound filtering.

### Hardware, Backups & Adaptive UI (Hardver, bekapi i prilagodljiv interfejs)
- **Hardware-backed security (Hardverska bezbednost):** Added security helpers for protected local data and secure network access.
- **Encrypted portable backups (Šifrovani prenosivi bekapi):** Added password-protected backup and restore using cryptographic key derivation.
- **Emergency alerts (Hitna upozorenja):** Added wireless emergency alert management and an app shortcut to system safety alerts.
- **Adaptive layouts (Prilagodljivi rasporedi):** Added window-aware sizing and layouts for different screen sizes.

### Settings Redesign & Floating Menus
- **Complete Reorganization into 7 Categories:**
  1. *Appearance & Display (Izgled i prilagođavanje)*
  2. *Notifications, Sounds & Haptics (Obaveštenja, zvuk i odziv)*
  3. *Chat & Gestures (Ponašanje četa i interakcije)*
  4. *Privacy & Security (Privatnost i bezbednost)*
  5. *Data & Storage (Podaci i skladište)*
  6. *Messaging & Network (SMS i MMS podešavanja)*
  7. *System & About (Sistem i aplikacija)*
- **Privacy & Security Reordering:**
  - Card 1: *Blocked Senders (Blokirani pošiljaoci)* with instant block/unblock controls.
  - Card 2: *Biometric Protection & PIN/Passphrase (Biometrijska zaštita i PIN/Passphrase)* with customizable timeout and instant lock action.
  - Cards 3-6: *Local Database Encryption*, *HTTPS & TLS/SSL Pinning*, *Metadata Minimization*, and *Root State Detection*.
- **Live Active Security Measures Hub:**
  - Real-time status list accessible directly from the settings header shield icon.
  - Green checkmark (`✓`) for active protections, red cross (`✕`) for disabled ones, and compromised alerts for rooted states.
  - Interactive item tapping directly navigates to and highlights the corresponding setting toggle.
- **Floating Dropdown Selectors:**
  - Option selectors now open as mini floating dropdown menus anchored directly to the right of each option with clean chevron indicators and checkmarks.
- **Clutter Removal:**
  - Removed redundant subtitles from top-level category cards for an ultra-clean glassmorphic UI.

### Messaging, Interaction & Audio
- **Send Recall with Red "X" & Audio:**
  - During the 6-second delayed send countdown, the send icon transforms into a prominent Red "X" cancel button.
  - Cancelling plays the dedicated `cancel.wav` audio effect and preserves the message draft.
- **Smart Contact Picker (Nova poruka):**
  - Real-time case-insensitive search with fuzzy scoring over contact names and phone numbers.
  - Direct support for unmemorized custom phone numbers.
  - Placeholder "Unesi ime ili broj telefona".
  - Dedicated "Cancel" and "Next (Sledeće)" action buttons that open the chat thread directly.
- **Automated GitHub Update Engine:**
  - Checks for GitHub releases every time the user enters the application.
  - Issues a system notification if an update is available.
  - Tapping the notification navigates straight to Category 6 (System & About) and focuses the update section.

---

## [v1.5.1] - 2026-11-08
- **Message Handling & Trash:**
  - Individual and multi-selected messages deleted inside a chat move to Trash (Deleted Messages) instead of being permanently removed.
  - Full restore and auto-delete lifecycle purging (30 days default).
  - Full message history loading: removed query LIMIT constraints to always load all stored SMS/MMS messages.
- **UI Enhancements:**
  - Full-width bottom dock navigation and multi-selection bars.
  - Larger touch targets and refined haptic responses.
  - Consolidated Bubbles and External Links settings into collapsible cards.

---

## [v1.5] - 2026-11-08
- **Search, Pin, Highlights & Drafts:**
  - In-chat message search with hit counter.
  - Centralized highlighted messages hub with jump-to-message.
  - Message pinning to the top of threads.
  - Per-thread local draft persistence.
- **Broadcast & Smart Actions:**
  - Multi-recipient broadcast messaging in separate private 1-on-1 chats.
  - Smart recognition of phone numbers and emails in received messages with quick dial/email actions.
  - Contact card sharing directly into compose bar.
  - Birthday midnight scheduling preset (00:00).
- **Security & Telephony:**
  - Cloudflare DoH malicious website inspection.
  - SIM card messages management and carrier SMSC configuration.
  - AMOLED pure black and clean white theme presets.
  - Delayed sending with an undo window before messages are transmitted.
  - Manual People/Services category selection for a sender.
- **Interaction & Media:**
  - In-chat sound effects, pinch-to-zoom bubble sizing, expressive bubble animations, and Android notification bubbles.
  - Configurable automatic link and media previews, including Wi-Fi-only and web-link-only options.

---

## [v1.0] - 2026-11-08
- Initial release with the SMS/MMS telephony stack, dual-SIM support, delivery reports and read receipts, scheduled messages, system ringtone selection, custom chat wallpapers, Material 3 Glassmorphism themes, and local-only message handling.

---

## [v0.9 Beta] - 2026-11-05
- Added automatic People, Services, and Unread categories.
- Added configurable swipe actions and spam sender blocking.

---

## [v0.8 Alpha] - 2026-10-28
- Established dual-SIM SMS/MMS sending and receiving, secure offline JSON backup and restore, and the original Glassmorphism interface with seven themes and custom wallpapers.
