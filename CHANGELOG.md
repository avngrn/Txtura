# Txtura Changelog & Improvements

## [v2.1] - 2026-11-09

### Messaging & Broadcast (Poruke i višestruko slanje)
- **Simultaneous Multi-Recipient Broadcast (Slanje jedne poruke na više brojeva istovremeno):**
  - Reimplemented new message flow allowing selection of multiple contacts and typed numbers at once.
  - Direct individual SMS transmission without group MMS conversion.
  - Live character count and SMS segment calculator (`N karaktera • M SMS`), Select All / Deselect All suggestions toggle, and contextual "Pošalji svima (N)" button.
  - Dedicated "Pošalji na više brojeva" action directly in conversation details menu.

### Security & Biometrics (Bezbednost i biometrija)
- **Biometric & Lock Screen Reliability (Otključavanje i pouzdanost biometrije):**
  - Added native `android.permission.USE_BIOMETRIC` permission.
  - Native Android BiometricPrompt (API 29–37) with strong/weak biometrics and device credential support, with seamless KeyguardManager fallback.
  - Complete protection against lockout loops on devices without lock screen PIN or biometrics: auto-unlock, safe disable, clear guidance, and shortcut to system lock screen settings.
  - Fixed activity lifecycle re-lock issues on resume.

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

## [v2.0] - 2026-11-08

### Privacy & Security (Privatnost i bezbednost)
- **Local Database Encryption (Lokalna enkripcija baze podataka):**
  - Authenticated AES-256-GCM / SQLCipher hardware-backed message cache encryption using Android Keystore.
  - Seamless real-time data migration (`reencryptCache`) that instantly encrypts or decrypts local cache when toggled in settings.
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

---

## [v1.0] - 2026-11-08
- Initial release with complete SMS/MMS telephony stack, Material 3 Glassmorphism theme, delivery reports, read receipts, custom chat wallpapers, and zero-permission media attachments.
