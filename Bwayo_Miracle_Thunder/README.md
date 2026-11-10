# BWAYO MIRACLE THUNDER

A standalone Flutter starter for phone diagnostics and supported servicing, stored separately from Bwayo Super App.

**Owner & Founder: BWAYO EMMANUEL**

## Included
- Dashboard and connection-status placeholder
- Device information guidance
- Manual diagnostics checklist
- Backup and restore guidance
- Safe, read-only ADB examples
- GitHub Actions builds for Android APK and Windows ZIP

## Build locally
Install Flutter stable, then run `flutter pub get`. Use `flutter build apk --release` for Android or `flutter build windows --release` on Windows with Visual Studio C++ desktop tools installed.

## Current limitations
This is source code, not a precompiled installer. USB/ADB device discovery and vendor-specific servicing are not implemented. A successful build verifies compilation only, not hardware servicing. Only use supported procedures on devices you own or are authorized to repair. This project does not provide FRP/security-lock bypass, IMEI alteration, account bypass, or unauthorized firmware flashing.
