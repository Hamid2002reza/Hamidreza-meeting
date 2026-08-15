# Hamidreza Meeting — Final Release Gate

## Automated checks performed in this environment
1. ZIP integrity and extraction
2. Source tree scan for obvious placeholder/TODO/unfinished markers
3. Manifest/package consistency scan
4. No private signing keystore included
5. No obvious API key literals included
6. Kotlin/XML source readability scan
7. Required backend files presence scan

## Device tests required
These cannot honestly be claimed without a physical/device emulator Android environment:
1. Install APK on Xiaomi POCO X4 5G
2. Create meeting
3. Record 10+ minutes
4. Stop/resume/error recovery
5. Persian speech-to-text
6. Edit/save encrypted transcript
7. AI request over HTTPS
8. Backend timeout/offline handling
9. AI structured extraction
10. Word export and opening in Microsoft Word
11. Reminder/notification
12. PIN and Biometric
13. Android process kill/restart recovery
14. Rotation/background/Doze behavior
15. R8/minified release runtime smoke test
16. `apksigner verify -Werr` on the produced APK

## Release signing
Do NOT commit or distribute a private release keystore. Android requires release APKs to be digitally signed; use your own keystore and verify with apksigner.
