# Shared OnChat meetings — 28 October 2026

Android now opens the actual Cashpay Web meeting admission and organizer interface in an authenticated WebView. Paid admission requires the server quote, explicit consent and PIN; the previous false-success fallback on failed meeting admission is removed. Group audio/video buttons create a membership-protected meeting and publish its invitation. Group history and sends use the group API, and invitation cards show actual WebSocket state with HTTP fallback.

Meeting links accept only the trusted HTTPS Cashpay origin or a plain meeting code. Authentication uses an HttpOnly cookie, never a token in a URL or JavaScript bridge. File/content access, mixed content and third-party cookies are disabled. Camera/microphone access checks the web origin and Android runtime permissions. Logout clears web cookies. Existing native direct calls remain in place.

Validation: assembleDebug succeeded; all three offline OnChatContractsTest cases passed (zero failures/errors), covering serialization and trusted meeting links. The debug APK is supplied with delivery. No device/emulator or multi-device WebRTC tests were available. The shared surface requires deployment of the matching web branch and its reviewed database migration; it is not a new standalone native conference engine.

## Production checkout cleanup

At the owner’s request, all checked-in unit/instrumentation test sources and screenshot fixtures were removed after validation, together with their Gradle dependencies and runner. Prior test results describe the validation before removal. Legacy automatic cleanup routines that deleted calls by contact names or test-like names, and unused cleanup queries for hard-coded group/user IDs, were removed. Session reload no longer performs these deletions. No local or remote account data was modified by this cleanup.
