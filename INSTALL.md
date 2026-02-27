# PGK Food Android APK Installation

## Artifact
- File: `pgk-food-v1.0(1)-signed.apk`
- Package: `com.example.pgk_food`
- Version: `1.0 (versionCode 1)`
- minSdk: `24` (Android 7.0+)
- targetSdk: `36`

## Integrity check
Run in the same directory:

```bash
sha256sum -c SHA256SUMS.txt
```

Expected result: `pgk-food-v1.0(1)-signed.apk: OK`

## Install via ADB
1. Enable Developer options and USB debugging on the Android device.
2. Connect device and verify it is visible:

```bash
/home/wsr/Android/Sdk/platform-tools/adb devices
```

3. Install or update app:

```bash
/home/wsr/Android/Sdk/platform-tools/adb install -r "pgk-food-v1.0(1)-signed.apk"
```

Optional launch command:

```bash
/home/wsr/Android/Sdk/platform-tools/adb shell am start -n com.example.pgk_food/.MainActivity
```

## Install without ADB
Copy APK to device and open it from file manager. If prompted, allow installs from unknown sources for that installer app.
