# Version 0.4.1 verification — prerelease

[Português (Brasil)](../qa-v0.4.1.md) | **English** · [Home](../../README.en.md)

Published for compatibility testing; 0.4.0 remains stable. APK `TCL-Alexa-Bridge-0.4.1.apk`, versionCode 11, **1221573 bytes**, SHA-256:

```text
06131e0619d8474413b87ceb33310a33d6861efe3719c74c36725c95a74bd235
```

The asset is the actual candidate installed in the recorded November 20, 2026 tests. A fresh build of the same source has identical ZIP members outside `META-INF/`, including DEX, manifest, resources and licenses. The signer matches 0.4.0, allowing an update without clearing data. Container/signature differences can change a new build's hash. [Machine-readable summary](../qa-v0.4.1.json).

## Results

Functional environment: TCL C6K/G15 BR, Android 14/API 34, TTVSCore 10.58.16 and SmartAlexa 6.13.25. System apps and account linking were preserved.

| Check | Result |
| --- | --- |
| Bridge update without opening its UI | Linking, cloud, executor and persistence retained |
| Native renewal | Expiry advanced and updated state persisted |
| Recovery after executor/socket disconnect | Both reconnected |
| Native volume and callback | Actual effect, two completed callbacks, zero failures; original volume restored |
| 60 seconds in real standby | Asleep, cloud/executor/link/persistence retained |
| Complete standby/wake cycle | Passed: native power-off,60 seconds Asleep, UDP-only wake to Awake and healthy state afterward; no UI opened |
| Host checks | 11 reflection checks and DEX ordering for all 64 APKs passed |
| Isolated Android probe | 48/64 passed; 16 blocked during preparation |

The first test sender failed on the Mac with “no route to host” during unicast and stopped before broadcasting. A subsequent broadcast woke the TV. After the TV became available again, the complete cycle was repeated with the corrected sender and passed in one run. The installed APK was pulled and its SHA-256 exactly matched this release asset.

## Scope and pending checks

The fix detects members and structures rather than selecting repairs by a version list. Collection Java tests do not qualify registration or every Alexa command for each APK, firmware, region or TV. See the [full matrix](compatibility-v0.4.1.md).

The exact 10.60.03 artifact remains unavailable. Fresh Echo voice verification on this candidate, a new account and overnight standby remain pending. First setup and boot have historical 0.4.0 evidence and were not repeated on 0.4.1. These limits justify the prerelease; there is no universal compatibility or 100% parity claim.

## Update and report

Install over the previous official bridge APK without uninstalling or clearing data. SmartAlexa and TTVSCore remain required. Follow the [TV setup guide](setup.md). A self-build uses a different key and cannot directly update the official APK; retain your own key for subsequent self-build updates.

Report model, Android, firmware, Core/SmartAlexa versions and the failing step or observed effect. Do not attach tokens, activation codes, profiles, private identifiers, keys or raw TCL logs. Returning to a lower versionCode may require a specific procedure; do not clear linking to force a downgrade. See [recovery](rollback.md).
