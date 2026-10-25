# Technical review of the current version

[Português (Brasil)](../review.md) | **English** · [Home](../../README.en.md)

The review covered runtime/Binder/lifecycle and onboarding/portability/distribution. This document describes the included fixes and limits; executed results are in the [v0.4.0 report](qa-v0.4.0.md).

| Area | Fix or safeguard |
| --- | --- |
| Binder and executor death | Track/unregister the ServiceConnection and recover the executor |
| Callbacks and pause | State guards, serialized shutdown and renewal drain before a new session |
| Connection | Dedicated worker and overlap protection |
| TCL signing | Installed signer; no embedded/exported TCL private material |
| Identity | Native collection from the TV; private profile and empty public asset |
| Administrative actions | Removed from exported Activity; system-protected diagnostics |
| Observers and timers | Installed during Binder connection; cancellation and guards after stop |
| Power | Opt-in, consumption notice and preservation of original values; TCL network configured through shell session |
| Private runtime | Source hash, ZIP/DEX validation and checksums; narrow SDK adaptation and isolated Alexa properties |
| Local preparation | Loopback only, fixed commands and temporary private ADB key |
| ADB client | Stale CLSE handling covered by local suite |
| Installer | HTTPS, size limits, hash/package/version/certificate checks and read-only provider with a single path |
| UI | Diagnostics separate from component setup; recognition of existing link and recent state |
| Build/distribution | Explicit dependencies, identity-free source profile, production debugging disabled and included BSD license |

No root, accessibility service or `INJECT_EVENTS` grant is used. The private runtime copy does not replace system packages or write firmware. Setup authorization does not imply a permanent ADB session.

Minimum API is 34 and the verified combination is documented in the README. Internal TCL interfaces may change; another TV/firmware needs its own test. ACKs do not prove every media/PVR/peripheral effect, and the backend participates in authentication recovery. The final APK passed update preserving the link, renewal/pause/resume, HTTP/Rx cleanup, rebind, reconnection and actual volume effects. Boot recovered the service without opening UI; automated 60-second standby and UDP wake passed. Voice on the current version was not reconfirmed in this round. Other QA limits are identified in the current report without assuming failures in untested flows.
