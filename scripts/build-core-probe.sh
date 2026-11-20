#!/usr/bin/env bash
set -euo pipefail
BRIDGE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRIDGE_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
BRIDGE_TOOLS="${BRIDGE_BUILD_TOOLS:-${BRIDGE_SDK}/build-tools/36.0.0}"
BRIDGE_ANDROID="${BRIDGE_ANDROID_JAR:-${BRIDGE_SDK}/platforms/android-34/android.jar}"
BRIDGE_PROBE="$BRIDGE_ROOT/build/core-probe"
[[ "$#" -gt 0 && -f "$BRIDGE_ANDROID" && -f "$BRIDGE_ROOT/.private/signing.p12" ]] || {
  echo 'Build the bridge first, then provide one or more local TCL core APK paths.' >&2; exit 1;
}
BRIDGE_DEPENDENCIES="$(python3 "$BRIDGE_ROOT/scripts/ensure-dependencies.py")"
IFS=: read -r -a BRIDGE_DEPENDENCY_JARS <<< "$BRIDGE_DEPENDENCIES"
mkdir -p "$BRIDGE_PROBE/classes" "$BRIDGE_PROBE/dex" "$BRIDGE_PROBE/assets/cores"
# Only build-generated probe assets/classes are replaced; installed apps are untouched.
python3 - "$BRIDGE_PROBE" "$@" <<'PY'
import pathlib,shutil,sys
root=pathlib.Path(sys.argv[1])
for p in (root/'assets/cores').glob('*.apk'):p.unlink()
for p in (root/'classes').rglob('*.class'):p.unlink()
for p in (root/'dex').glob('classes*.dex'):p.unlink()
for i,source in enumerate(sys.argv[2:],1):shutil.copyfile(source,root/'assets/cores'/('core-%d.apk'%i))
PY
cat > "$BRIDGE_PROBE/AndroidManifest.xml" <<'XML'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="local.tclbridge.compatprobe" android:versionCode="1" android:versionName="1">
  <uses-sdk android:minSdkVersion="34" android:targetSdkVersion="34"/>
  <queries><package android:name="com.tcl.ttvs"/></queries>
  <application android:label="TCL core compatibility probe" android:allowBackup="false" android:debuggable="false"/>
  <instrumentation android:name="local.tclbridge.CoreCompatibilityProbe" android:targetPackage="local.tclbridge.compatprobe"/>
</manifest>
XML
javac --release 8 -cp "$BRIDGE_ANDROID:$BRIDGE_DEPENDENCIES" -d "$BRIDGE_PROBE/classes" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/CoreReflection.java" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/DexSdkCompat.java" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/DexOrderRepair.java" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/CoreArchive.java" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/IsolatedCoreLoader.java" \
  "$BRIDGE_ROOT/app/src/local/tclbridge/CoreApplication.java" \
  "$BRIDGE_ROOT/app/src/tclx/os/SystemProperties.java" \
  "${BRIDGE_PROBE_SOURCE:-$BRIDGE_ROOT/tests/android/CoreCompatibilityProbe.java}"
jar cf "$BRIDGE_PROBE/probe.jar" -C "$BRIDGE_PROBE/classes" .
"$BRIDGE_TOOLS/d8" --min-api 34 --lib "$BRIDGE_ANDROID" --output "$BRIDGE_PROBE/dex" "$BRIDGE_PROBE/probe.jar" "${BRIDGE_DEPENDENCY_JARS[@]}"
"$BRIDGE_TOOLS/aapt" package -f -M "$BRIDGE_PROBE/AndroidManifest.xml" -I "$BRIDGE_ANDROID" -A "$BRIDGE_PROBE/assets" -F "$BRIDGE_PROBE/unsigned.apk"
python3 - "$BRIDGE_PROBE" <<'PY'
import pathlib,sys,zipfile
root=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(root/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for p in (root/'dex').glob('*.dex'):z.write(p,p.name)
PY
"$BRIDGE_TOOLS/zipalign" -f 4 "$BRIDGE_PROBE/unsigned.apk" "$BRIDGE_PROBE/aligned.apk"
"$BRIDGE_TOOLS/apksigner" sign --ks "$BRIDGE_ROOT/.private/signing.p12" --ks-pass "file:$BRIDGE_ROOT/.private/signing-pass.txt" --ks-key-alias bridge --out "$BRIDGE_PROBE/core-probe.apk" "$BRIDGE_PROBE/aligned.apk"
"$BRIDGE_TOOLS/apksigner" verify "$BRIDGE_PROBE/core-probe.apk"
echo "Probe APK (not installed): $BRIDGE_PROBE/core-probe.apk"
