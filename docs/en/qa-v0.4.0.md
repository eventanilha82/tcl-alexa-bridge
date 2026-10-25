# Version 0.4.0 verification

[Português (Brasil)](../qa-v0.4.0.md) | **English** · [Home](../../README.en.md)

Production APK: `TCL-Alexa-Bridge-0.4.0.apk`, **82462 bytes**, SHA256:

```text
ea4df262ad9a0c93f07fb64cb12e7365d6d566c68c784ee4520cae5d9d6ba2f2
```

Environment: TCL C6K BR/G15, Android 14, firmware V8-0016T01-LF1V048, TTVSCore 10.58.16 and SmartAlexa 6.13.25. Tests distinguish first setup in an isolated installation from checks of the final APK in production. Temporary lab APKs are not distributed.

## Final production APK

| Check | Result |
| --- | --- |
| Update preserving profile and link | Passed |
| Renewal through native TCL engine | Passed |
| Renewal followed by pause/resume | Three cycles passed, preserving updated state |
| Cleanup of private HTTP/Rx resources | No errors reported on TV after cycles; waiting/cleanup outside the UI thread |
| Executor disconnection and rebind | Passed |
| Socket closure and reconnection | Passed |
| Actual volume command and native ACK | Two observed effects, two completed responses, zero failures; original volume restored |
| State after recovery | Account linked, cloud/executor connected, native state persisted, no pending authentication |
| Build using public script | DEX and manifest matched the verified final APK |
| Signature and debugging | Signature verified; `debuggable=false` |
| adblib license | BSD notice present in APK |
| Reboot without opening UI | Passed; `BOOT_COMPLETED` started the service |
| Recovery after reboot | Account, cloud, executor and persistence confirmed |
| Power settings after reboot | Global Low Power Standby remained 0; TCL network standby remained 1 |
| Native SmartAlexa power off | ACK code 0 in 39 ms; screen observed Asleep/Dozing |
| Automated 60-second standby | Cloud and executor stayed active |
| Wake after standby | UDP 1025 magic packet changed screen to Awake without opening UI |

Reboot and the automated 60-second standby cycle passed without opening the Activity. Echo voice confirmation on v0.4.0 was not repeated in this round; earlier voice confirmation remains historical evidence. These results describe what was actually exercised on the final APK and must not be extended to every feature or other TVs.

## First setup in an isolated installation

| Check | Evidence |
| --- | --- |
| User-authorized local ADB | Preparation session authorized on the TV itself |
| Collection without imported file | Real native TCL identity saved before any token |
| No invented identity | TCL interface or native diagnostic; no Android ID fallback |
| Start without credentials or native database | Registration started with empty authentication storage |
| Registration, code and confirmation | Complete native SDK flow |
| Renewal, cloud, executor and persistence | Confirmed after initial linking |
| Private runtime and SDK compatibility | No `NoSuchFieldError` during final tested bootstrap |

The last isolated bootstrap with the revised runtime completed between 01:48:38 and 01:48:48. These times describe an observed run, not a guaranteed duration.

Local ADB client tests passed 54 checks, including stale close packets. SDK adaptation changes only the expected method in the private copy; the examined real DEX changed by 21 bytes with valid checksums.

## QA coverage limits

The visible Android download/installer with SmartAlexa absent, new phone skill confirmation for an entirely new account, power restoration through the UI and overnight standby were not exercised in this round. These are test coverage limits, not observed failures.

There is also no positive test of a tuned channel, actual recording/PVR or every player/peripheral. Earlier versions demonstrated volume/mute, inputs, app launching, navigation, boot and voice wake; each current combination should be assessed by its corresponding test.

See [validation](validation.md) for ACK interpretation and limits. Command dispatch or connected states alone do not establish 100% parity or long-term stability.
