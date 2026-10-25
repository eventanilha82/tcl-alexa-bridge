# Build from source

[Português (Brasil)](../build.md) | **English** · [Home](../../README.en.md)

To use the bridge, install the ready-made APK through [TV setup](setup.md). This guide is for building your own APK and reviewing the code.

The project uses Java/Android directly, without requiring Gradle or Android Studio. You need Git, Bash, Python 3, a full JDK with `java`, `javac`, `jar` and `keytool`, and the Android SDK. The development environment was validated with JDK 27, Platform 34 and Build Tools 36.0.0. The APK requires Android API 34; hardware compatibility remains limited to the environment in the README.

## 1. Install computer tools

Install Git, Python 3 and JDK 27 through your preferred vendor/package manager. Use a full JDK, not only a JRE. Check in a terminal:

```sh
git --version
python3 --version
java -version
javac -version
jar --version
keytool -help
```

If multiple Java installations exist, set `JAVA_HOME` to the chosen JDK and add its `bin` directory to `PATH`. `java` and `javac` should resolve to the same installation. Scripts use Bash: on Windows, use Linux/WSL with matching Linux tools and SDK. The originally validated environment was macOS.

Download **Command line tools only** for your OS from the [official Android SDK page](https://developer.android.com/studio#command-tools). Extract the ZIP's `cmdline-tools` folder and arrange it as follows:

```text
Android/Sdk/
  cmdline-tools/
    latest/
      bin/sdkmanager
      lib/
      source.properties
```

`latest` must directly contain `bin`, `lib` and the other files, without another nested `cmdline-tools`. Example for a new SDK in Bash; replace the ZIP path with your download:

```sh
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
unzip /path/to/commandlinetools-for-your-os.zip -d "$ANDROID_SDK_ROOT/command-tools-unpacked"
mv "$ANDROID_SDK_ROOT/command-tools-unpacked/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;36.0.0"
```

Read and accept the SDK terms yourself. If you already have an SDK, use that directory and avoid overwriting installed tools. Save the environment settings in your shell configuration if desired.

## 2. Get the source

```sh
git clone https://github.com/eventanilha82/tcl-alexa-bridge.git
cd tcl-alexa-bridge
```

Use the revision matching the current release if the main branch has advanced. Do not copy profiles, tokens, lab APKs or keys from other installations into the repository.

## 3. Build and verify

```sh
python3 scripts/check-shareable.py
./scripts/build.sh
```

Output: `build/alexa-bridge.apk`. Firmware, SmartAlexa and TTVSCore downloads are not needed to compile; they are runtime dependencies on the TV.

The script compiles `app/src`, generates DEX, packages resources/assets, aligns and signs the APK. Override `BRIDGE_BUILD_TOOLS` and `BRIDGE_ANDROID_JAR` to select explicit local dependencies. No vendor APK, decompiled source or TCL key is needed for the build. The distributed profile is empty; the wizard collects and validates native identity on the device.

The first run creates `.private/signing.p12` and `.private/signing-pass.txt`. Keep both out of Git and back them up securely. Updates require the same signing key. A personal build has a different signature from the release and cannot directly update it. The installer does not automatically uninstall an existing app to bypass a signature mismatch.

The build can be repeated with fixed dependencies; byte-for-byte reproducibility is not promised because the initial key is random and packaging may vary. Releases must retain the signing key and publish the SHA256 of the exact APK. Verify locally:

```sh
python3 -c 'import hashlib,pathlib; p=pathlib.Path("build/alexa-bridge.apk"); print(hashlib.sha256(p.read_bytes()).hexdigest())'
"$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner" verify --verbose build/alexa-bridge.apk
```

Before sharing source, rerun `scripts/check-shareable.py` and inspect the contents manually. The scanner detects known patterns; it does not replace a secrets review. Do not commit `build/`, `.private/`, real profiles, raw logs, firmware, private runtime or signing material. The original SmartAlexa is a separate release asset under its own terms, not part of the bridge build.

## Production and isolated v0.4.0 tests

Production must retain `android:debuggable="false"`. Temporary debuggable lab builds are for local validation only, not release assets. Do not publish lab APKs or their private data. An isolated package also needs consistent changes to authorities and private paths; simply renaming the applicationId is insufficient.

## Licenses and tests

The public build includes modified `com.cgutman.adblib`, used only for temporary ADB key cryptography during local preparation. Its BSD license is in `app/assets/AdbLib-LICENSE.txt` and packaged in the APK assets. Preserve that file and source notices; see [third-party notices](third-party-notices.md).

`tests/LocalAdbTest.java` and `tests/DexSdkCompatTest.java` check the local protocol and DEX adaptation. They complement TV testing, but do not by themselves prove visual authorization, Android installation, actual linking, reboot or standby. The v0.4.0 review fixed handling of stale CLSE packets; its suite passed 54 checks in the development environment.

Before a release, verify version/APK name, retained signature, `debuggable=false`, absence of identity/tokens, packaged BSD license and the exact APK hash. The final APK matched the public build's DEX/manifest, had a valid signature and included the BSD notice. Isolated first setup passed; the production APK passed renewal, pause, reconnection, volume and boot without opening the UI. Automated 60-second standby and UDP wake also passed; voice on the current version was not reconfirmed in this round. See [QA](qa-v0.4.0.md) for hash, results and coverage limits.

### Run local tests without Android

From the repository root, with a JDK installed, compile the test classes. `LocalAdbTest` needs no Android SDK, TV, root or real ADB session; it creates a mock server on the computer's loopback only.

```sh
mkdir -p .private/test-classes
javac --release 8 -d .private/test-classes \
  app/src/com/cgutman/adblib/AdbBase64.java \
  app/src/com/cgutman/adblib/AdbCrypto.java \
  app/src/local/tclbridge/LocalAdb.java \
  app/src/local/tclbridge/DexSdkCompat.java \
  tests/LocalAdbTest.java \
  tests/DexSdkCompatTest.java
java -cp .private/test-classes local.tclbridge.LocalAdbTest
```

The optional `DexSdkCompatTest` needs a compatible TTVSCore APK legitimately extracted from your own TV. It is not included or required to build the bridge. If you already have it, run:

```sh
java -cp .private/test-classes local.tclbridge.DexSdkCompatTest \
  /private/path/TTVSCore.apk .private/sdk-compat-test.dex
```

The second argument is a private DEX generated for test inspection. Do not publish that file or the TCL APK in the repository. The test depends on the compatible version's layout; failure on another version needs investigation, not a generic runtime patch.

## 4. Install your build

Transfer `build/alexa-bridge.apk` to the TV using your preferred installation method and follow [TV setup](setup.md). If using a computer, enable and authorize network debugging on the TV, then run:

```sh
./scripts/install.sh YOUR_TV_IP:5555
```

This only installs your APK from `build/` through ADB and opens the wizard. It does not send a profile or tokens. Substitute your TV's IP and port. A signature mismatch means this APK cannot directly update the installed app; do not automatically uninstall to bypass it. Check signature, source and data preservation before switching installations.

If `sdkmanager` is missing, check `cmdline-tools/latest/bin` and `PATH`. If the build requests Platform/Build Tools, check `ANDROID_SDK_ROOT` and the exact versions above. If Java is missing or rejects options, check the full JDK and `JAVA_HOME`.
