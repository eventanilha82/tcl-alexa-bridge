# TCL Alexa Bridge

[Português (Brasil)](README.md) | **English**

Use Alexa on a Brazilian TCL C6K with setup using the TV remote. The bridge manages linking, connection and recovery; the native **TTVSCore and SmartAlexa components remain required** for integration and command execution.

**Version 0.4.0 · No root or firmware flashing · Community project, without Works with Alexa certification.** The app interface is currently in Portuguese.

## Start here

1. On the TV, use Downloader to download and install [TCL Alexa Bridge 0.4.0](https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/TCL-Alexa-Bridge-0.4.0.apk).
2. Open **Alexa na TV**. If prompted, select **Instalar SmartAlexa** (Install SmartAlexa) and confirm the Android installer.
3. Follow **Começar configuração** (Start setup), enable the debugging option indicated by the wizard, then select **Autorizar e preparar** (Authorize and prepare). Confirm Android's authorization prompt on the TV.
4. Select **Gerar meu código** (Generate my code), then finish linking on your phone through the **TCL Assistant** skill in the Alexa app.
5. Wait until the account, connection and controls are ready. Test through your Echo using the exact TV name registered in Alexa.

**[Step-by-step guide with screenshots →](docs/en/setup.md)** No computer, JSON profile or manual token import is needed in the validated flow. Preparation uses a temporary ADB connection only to the TV itself; debugging can be disabled afterward.

To turn on the TV through Alexa while in standby, enable the optional setting during preparation. This grants `WRITE_SECURE_SETTINGS` and changes power/network settings, which may increase standby power consumption. See [how to restore the previous values](docs/en/rollback.md).

## Guides in English and Portuguese

| I want to… | English | Português |
| --- | --- | --- |
| Install and link Alexa | [TV setup](docs/en/setup.md) | [Configuração na TV](docs/setup.md) |
| Build my own APK | [Build from source](docs/en/build.md) | [Compilação](docs/build.md) |
| Understand the bridge and dependencies | [Architecture](docs/en/architecture.md) | [Arquitetura](docs/architecture.md) |
| Troubleshoot or restore power settings | [Recovery](docs/en/rollback.md) | [Recuperação](docs/rollback.md) |
| Check tests and limitations | [v0.4.0 QA](docs/en/qa-v0.4.0.md) · [Validation](docs/en/validation.md) | [QA da 0.4.0](docs/qa-v0.4.0.md) · [Validação](docs/validation.md) |
| Review technical safeguards | [Technical review](docs/en/review.md) | [Revisão](docs/review.md) |
| Check provenance and licenses | [Third-party notices](docs/en/third-party-notices.md) | [Componentes externos](THIRD_PARTY_NOTICES.md) |

## How it works

```text
Echo → TCL skill → TCL cloud → TTVSCore in the bridge → Binder → SmartAlexa → TV
```

The native TTVSCore registers the TV, generates the code, confirms linking and maintains authentication and connection. The installed SmartAlexa executes controls. The bridge prepares a private copy of the TCL runtime, adapts storage and compatibility, and keeps the profile/tokens in the app sandbox. It does not export the TCL key or modify firmware or system APKs. It uses neither accessibility nor `INJECT_EVENTS`.

The service resumes after reboot and recovers the connection/executor. Network wake is a bridge adaptation. See the [architecture guide](docs/en/architecture.md) for details and limitations.

## Compatibility and tests

Validated on **TCL C6K/G15 BR, Android 14, firmware V8-0016T01-LF1V048, TTVSCore 10.58.16 and SmartAlexa 6.13.25**. Other TVs, regions and firmware versions need testing; internal TCL interfaces may change.

The final APK passed an update preserving the link, renewal, pause/resume, connection/executor recovery, actual volume changes, boot without opening the UI and 60-second standby with UDP wake. Volume, power off and power on through an Echo were confirmed in an earlier test; the final v0.4.0 regression was automated.

There is no evidence of 100% parity, overnight standby, phone confirmation with an entirely new Alexa account, the installer with SmartAlexa absent, or power restoration through the UI in this test round. An executor ACK does not guarantee an effect in every player, channel or peripheral. See the [full test report](docs/en/qa-v0.4.0.md).

## Downloads and contributions

The [v0.4.0 release](https://github.com/eventanilha82/tcl-alexa-bridge/releases/tag/v0.4.0) contains the bridge APK and [original SmartAlexa 6.13.25](https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/SmartAlexa-6.13.25.apk) as separate files. The wizard can download SmartAlexa; TTVSCore must already exist on the TV.

To compile, follow the [build guide](docs/en/build.md). The project uses Java/Android without requiring Gradle. Original code is MIT; modified adblib is BSD; TCL components are proprietary. See [provenance and licenses](docs/en/third-party-notices.md).

When opening an issue, include the model, region, firmware, component versions and observed effect. **Do not publish tokens, IDs, MAC addresses, activation codes, profiles, keystores or raw TCL logs.** The [validation guide](docs/en/validation.md) explains testing and how to generate a summarized diagnostic report.
