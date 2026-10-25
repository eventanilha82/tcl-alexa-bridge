#!/usr/bin/env bash
set -euo pipefail
BRIDGE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRIDGE_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
BRIDGE_TOOLS="${BRIDGE_BUILD_TOOLS:-${BRIDGE_SDK}/build-tools/36.0.0}"
BRIDGE_ANDROID="${BRIDGE_ANDROID_JAR:-${BRIDGE_SDK}/platforms/android-34/android.jar}"
BRIDGE_PRIVATE="$BRIDGE_ROOT/.private"
BRIDGE_OUT="$BRIDGE_ROOT/build"
[[ -f "$BRIDGE_ANDROID" && -x "$BRIDGE_TOOLS/d8" ]] || { echo 'Configure Android SDK Platform 34 e Build Tools 36.0.0; veja README.' >&2; exit 1; }
mkdir -p "$BRIDGE_PRIVATE" "$BRIDGE_OUT/classes" "$BRIDGE_OUT/dex"
chmod 700 "$BRIDGE_PRIVATE"
if [[ ! -f "$BRIDGE_PRIVATE/signing.p12" ]]; then
  python3 - "$BRIDGE_PRIVATE/signing-pass.txt" <<'PY'
import pathlib,secrets,sys
p=pathlib.Path(sys.argv[1]);p.write_text(secrets.token_urlsafe(32));p.chmod(0o600)
PY
  keytool -genkeypair -keystore "$BRIDGE_PRIVATE/signing.p12" -storetype PKCS12 -storepass:file "$BRIDGE_PRIVATE/signing-pass.txt" -alias bridge -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Community TV Bridge'
  chmod 600 "$BRIDGE_PRIVATE/signing.p12"
fi
python3 - "$BRIDGE_ROOT" <<'PY'
import pathlib,sys,subprocess
root=pathlib.Path(sys.argv[1]);classes=root/'build/classes'
for p in classes.rglob('*.class'):p.unlink()
import os
sdk=os.environ.get('ANDROID_SDK_ROOT',os.environ.get('ANDROID_HOME',''))
android=os.environ.get('BRIDGE_ANDROID_JAR',sdk+'/platforms/android-34/android.jar')
sources=sorted(str(p) for p in (root/'app/src').rglob('*.java'))
subprocess.run(['javac','--release','8','-cp',android,'-d',str(classes),*sources],check=True)
PY
jar cf "$BRIDGE_OUT/app.jar" -C "$BRIDGE_OUT/classes" .
"$BRIDGE_TOOLS/d8" --min-api 34 --lib "$BRIDGE_ANDROID" --output "$BRIDGE_OUT/dex" "$BRIDGE_OUT/app.jar"
"$BRIDGE_TOOLS/aapt" package -f -M "$BRIDGE_ROOT/app/AndroidManifest.xml" -I "$BRIDGE_ANDROID" -S "$BRIDGE_ROOT/app/res" -A "$BRIDGE_ROOT/app/assets" -F "$BRIDGE_OUT/unsigned.apk"
python3 - "$BRIDGE_OUT" <<'PY'
import pathlib,sys,zipfile
root=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(root/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for p in (root/'dex').glob('*.dex'):z.write(p,p.name)
PY
"$BRIDGE_TOOLS/zipalign" -f 4 "$BRIDGE_OUT/unsigned.apk" "$BRIDGE_OUT/aligned.apk"
"$BRIDGE_TOOLS/apksigner" sign --ks "$BRIDGE_PRIVATE/signing.p12" --ks-pass "file:$BRIDGE_PRIVATE/signing-pass.txt" --ks-key-alias bridge --out "$BRIDGE_OUT/alexa-bridge.apk" "$BRIDGE_OUT/aligned.apk"
"$BRIDGE_TOOLS/apksigner" verify "$BRIDGE_OUT/alexa-bridge.apk"
echo "APK: $BRIDGE_OUT/alexa-bridge.apk"
