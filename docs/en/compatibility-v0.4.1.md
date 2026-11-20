# TCL core compatibility: 0.4.1-rc1 candidate

Date: 2026-11-20. The published stable release remains 0.4.0. This candidate has not been installed or qualified on a TV.

## Change

Bridge 0.4.0 looks up public TCL fields and a public `AlexaManager` constructor. In the inspected 10.65.12 APK, several required fields and that constructor are private, preventing initialization.

The candidate centralizes access through `CoreReflection`, resolving declared fields (including superclass fields) and the exact declared constructor. It covers initialization, token/state access, heartbeat, executor recovery and cleanup. It does not alter the installed vendor APK, its access flags or the network protocol. Missing or unexpected `DeviceInfo` fields now fail instead of being silently skipped.

## Local validation

| Core | Artifact | 51 selected DEX contract checks | SDK adapter |
| --- | --- | --- | --- |
| 10.58.16 | Existing C6K/G15 baseline | Passed | Exactly one match |
| 10.58.22 | Extracted from R75PT01 V618 firmware | Passed | Exactly one match |
| 10.60.15 | Extracted from T653T01 V655 firmware | Passed | Exactly one match |
| 10.60.17 | Extracted from V645 and V648; identical APKs | Passed | Exactly one match |
| 10.65.12 | TCL application update catalog | Passed; 37 selected members require more than public lookup | Exactly one match |
| 10.60.03 | Exact APK still unavailable | Pending | Pending |

The firmware 10.60.17 artifact retains public access for the inspected members. It does not establish the cause of the reported 10.60.03 failure. Artifact identity matters beyond an abbreviated version number.

The JVM reflection test passed 11 checks covering private/public/inherited access, static and instance state, 64-bit values, missing members, constructor signatures and target exceptions. SDK tests also check the modified DEX and malformed-header rejection. These are local contract tests, not full Alexa runtime qualification.

## Reproduce

Use a JDK supporting `javac --release 8`, Python 3, and your separately obtained vendor APKs:

```bash
bash scripts/test-core-compatibility.sh /path/to/baseline-core.apk /path/to/new-core.apk
```

Without APK arguments, this runs only the JVM reflection test. Vendor APKs are not included in the source repository.

## Headless Android probe

A separate instrumentation package, `local.tclbridge.compatprobe`, has no Activity or `INTERNET` permission. It loads private copies of supplied cores using the bridge adapters and checks synthetic objects without registration, token refresh, credential persistence or SmartAlexa commands. The probe builds locally; TV execution is pending because the Mac and TV were on different networks and the user deferred it.

After configuring the Android SDK and building the bridge once:

```bash
bash scripts/build-core-probe.sh /path/to/baseline-core.apk /path/to/new-core.apk
adb install -r build/core-probe/core-probe.apk
adb shell am instrument -w local.tclbridge.compatprobe/local.tclbridge.CoreCompatibilityProbe
```

Do not attach the probe to a public release: it bundles the local vendor APKs supplied for testing. It is not included in the bridge application.

## Stable-release gate

TV validation remains required: probe execution, an upgrade retaining data and signing identity, connection/executor/persistence and real renewal, isolated first-time linking, and power/recovery tests when the TV is free. Obtain 10.60.03 before claiming its reported activation failure is fixed. Historical 0.4.0 results do not qualify this candidate. No firmware or core update was installed during this work.
