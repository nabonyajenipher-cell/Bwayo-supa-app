# BWAYO MIRACLE THUNDER

A standalone Flutter phone diagnostics and authorized servicing utility, stored separately from Bwayo Super App.

**Owner & Founder: BWAYO EMMANUEL**

## Included
- Dashboard and connection-status placeholder
- Device information guidance
- Manual diagnostics checklist
- Firmware servicing preparation screen with manufacturer selection and safety checklist
- Backup and restore guidance
- Safe, read-only ADB examples
- GitHub Actions builds for Android APK and Windows ZIP

## Firmware servicing status
The firmware screen helps confirm device ownership/authorization, exact model and region, official firmware source, backup, battery level and data-loss awareness. It directs users to the manufacturer's official update utility. **Direct USB firmware flashing is not yet implemented.** It requires a tested, model-specific integration and official firmware packages. Do not use firmware intended for a different model or region.

## Build locally
Install Flutter stable, then run `flutter pub get`. Use `flutter build apk --release` for Android or `flutter build windows --release` on Windows with Visual Studio C++ desktop tools installed.

## Safety and current limitations
This is source code, not a precompiled installer. USB/ADB device discovery and vendor-specific servicing are not implemented. A successful CI build verifies compilation only, not hardware servicing. Only use supported procedures on devices you own or are authorized to repair. This project does not provide FRP/security-lock bypass, IMEI alteration, account bypass, or unauthorized firmware flashing.
