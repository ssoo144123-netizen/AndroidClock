#!/bin/sh
# ساخت APK بدون Android Studio (لینوکس/WSL): نیازمند default-jdk, aapt, apksigner, zipalign, dalvik-exchange, libandroid-23-java, android-framework-res
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
B=/tmp/jalaliclock_build; rm -rf "$B"; mkdir -p "$B/gen" "$B/classes" "$B/out"
ANDROID_JAR=/usr/share/java/com.android.android-23.jar
FRAMEWORK_RES=/usr/share/android-framework-res/framework-res.apk
sed 's#<manifest #<manifest package="com.jalaliclock" #' "$HERE/app/src/main/AndroidManifest.xml" > "$B/AndroidManifest.xml"
aapt package -f -M "$B/AndroidManifest.xml" -S "$HERE/app/src/main/res" -I "$FRAMEWORK_RES" \
     --min-sdk-version 23 --target-sdk-version 23 --version-code 1 --version-name 1.0 \
     -J "$B/gen" -F "$B/out/unsigned.apk"
javac --release 8 -nowarn -cp "$ANDROID_JAR" -d "$B/classes" $(find "$HERE/app/src/main/java" "$B/gen" -name '*.java')
dalvik-exchange --dex --min-sdk-version=23 --output="$B/out/classes.dex" "$B/classes"
(cd "$B/out" && aapt add unsigned.apk classes.dex >/dev/null)
zipalign -f 4 "$B/out/unsigned.apk" "$B/out/aligned.apk"
KS="$HERE/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias debug \
     -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=JalaliClock Debug" >/dev/null 2>&1
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out "$HERE/JalaliClock.apk" "$B/out/aligned.apk"
apksigner verify --min-sdk-version 23 "$HERE/JalaliClock.apk" && echo "OK: $HERE/JalaliClock.apk"
