# Validation and current version limits

[Português (Brasil)](../validation.md) | **English** · [Home](../../README.en.md)

Tested environment: TCL C6K BR/G15, Android 14, V8-0016T01-LF1V048, TTVSCore 10.58.16 and SmartAlexa 6.13.25. The [v0.4.0 report](qa-v0.4.0.md) identifies the final APK, wizard evidence and coverage limits.

The final APK preserved the link on update, renewed tokens natively and passed three renewal/pause/resume cycles, HTTP/Rx cleanup without reported errors, executor rebind, socket reconnection and two volume commands with actual effect/ACK, restoring the original level. DEX/manifest of the public build, signature, production debugging disabled and BSD license were verified.

First setup was exercised in an isolated installation: local authorization, real TCL identity collection without an imported profile, starting without credentials, native registration, code, link confirmation, renewal, connection and persistence. Reboot passed without opening the UI: service, account, cloud, executor and persistence recovered, preserving power settings. Automated 60-second standby passed: native power off with ACK, screen in standby, active cloud/executor and UDP wake without opening the UI. Echo voice on v0.4.0 was not reconfirmed in this round; earlier confirmation is historical. The installer with SmartAlexa absent, phone skill confirmation with an entirely new account, power restoration through the UI and overnight standby were not exercised in this round; these are coverage limits, not observed failures.

Earlier development demonstrated volume/mute, HDMI1–4/TV, YouTube/Netflix, navigation, pause/resume, renewal, reconnection, rebind, boot without opening UI and wake. The user confirmed volume, power off and power on through an Echo in an earlier test. This remains implementation history and is not automatically attributed to the current version.

## What a response proves

The 11 `msg_type` families analyzed in the executor are 3,4,5,6,7,8,9,11,15,16,17. The bridge forwards the native protocol; that does not guarantee every feature exists on every model or app. Metrics count commands forwarded to the executor and their responses. A success ACK alone does not demonstrate an effect in content, a player or a peripheral.

Earlier TCL player tests changed position with seek/rewind/fast-forward/restart, while Stop removed the session. Pause produced inconsistent telemetry despite a native success response. Next/previous in a single-item playlist do not prove switching. No tuned channel or real recording was available for a positive test. PVR/channels require validation on a device with those resources.

During the native `BINDING` window, messages can be discarded before reaching the bridge. There is no indiscriminate retry because it could duplicate actions. Abrupt interruption during renewal also depends on TCL backend state.

## Validate your installation

Record model, firmware and versions of all three packages. Then check:

1. Remote-based preparation, code and linking without importing credentials.
2. Voice control using the exact TV name, with the UI closed.
3. Volume/mute, available inputs and actually installed apps.
4. Playback, pause, seek and navigation in each player used.
5. Network loss/recovery and executor recovery.
6. Pause/resume and renewal without losing the link.
7. Reboot without opening the app, short standby and overnight standby.
8. Disabling power options and restoring the previous state.

Distinguish visible effects, voice-triggered actions and telemetry alone. Without a positive hardware test and the corresponding phrase, classify a result as conditional or inconclusive. Do not publish tokens, IDs, MAC addresses, profiles or raw TCL logs in issues.

## Summarized diagnostics

From a computer with ADB access authorized on the TV, run at the repository root:

```sh
mkdir -p .private
./scripts/diagnostics.sh YOUR_TV_IP:5555 > .private/diagnostics.txt
```

This summary avoids raw TCL logcat, which can contain authentication data. Review it before sharing. Include model, region, firmware, package versions, the command/phrase used and the observed effect; leave private identifiers and credentials out.
