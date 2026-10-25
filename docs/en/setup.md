# TV setup — 0.4.0

[Português (Brasil)](../setup.md) | **English** · [Home](../../README.en.md)

Use the TV remote for setup and your phone for linking in the Alexa app. No computer commands or JSON profile are needed in the validated flow. **The app UI is in Portuguese:** this guide keeps the actual button labels and explains them in English. Screenshots also show the Portuguese UI.

Tested combination: TCL C6K/G15 BR, Android 14, firmware V8-0016T01-LF1V048, TTVSCore 10.58.16 and SmartAlexa 6.13.25. The TV should be configured for Brazil and connected to Wi-Fi. Preparation depends on local ADB port 5555, available on the tested firmware. Other combinations may need adaptation; this guide does not establish support for other regions or skill availability in another Alexa account.

## 1. Download the bridge

In Downloader on your TV, open:

<https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/TCL-Alexa-Bridge-0.4.0.apk>

Install the APK and open **Alexa na TV**. Allow installation from the source used by Downloader when Android asks.

The bridge checks the installed TTVSCore and original SmartAlexa. If SmartAlexa is missing or old, choose **Instalar SmartAlexa** (Install SmartAlexa). Allow **Alexa na TV** to install apps in Android settings and return to the wizard. It downloads the original APK, verifies its hash, package, version and certificate, then opens Android's installer for confirmation.

You can also download SmartAlexa directly in Downloader:

<https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/SmartAlexa-6.13.25.apk>

SmartAlexa is proprietary TCL software; see [provenance, hashes and terms](third-party-notices.md). The bridge does not install TTVSCore or firmware. Resolve incompatible TTVSCore or unrecognized signatures before continuing. Do not automatically uninstall apps and erase your link to work around an update signature error.

![Wizard start screen, in Portuguese](../images/comecar.png)

## 2. Prepare the TV once

1. Select **Começar configuração** (Start setup), then **Abrir configurações da TV** (Open TV settings).
2. If developer options are not available yet, go to System → About and select the Android TV build/version seven times, following your TCL menu.
3. Enable the USB/network debugging option provided by the firmware and return to the wizard. Menu names vary; the connection is only to the TV itself at `127.0.0.1:5555`.
4. Choose whether to enable **Permitir ligar a TV por Alexa em espera** (Allow Alexa to turn on the TV in standby). This optional feature may increase power consumption and grants a broad protected-settings permission to adjust Low Power Standby.
5. Select **Autorizar e preparar** (Authorize and prepare). Confirm **Permitir** (Allow) in Android's debugging prompt.

![Preparation and authorization using the remote; standby wake is optional](../images/preparar.png)

Preparation creates a temporary ADB key and uses the local session to configure automatic startup, notifications, display and background execution. With wake enabled, it also configures network standby and Low Power Standby. Previous power values are saved for restoration. No terminal or manual tokens are needed.

The wizard collects the TV's real identity and saves its profile in private storage. It uses the native TCL identifier; if the direct interface cannot provide it, it tries the TV's own native diagnostic identification. It does not derive an Android ID, UUID or random identifier. If identification fails, setup reports the failure instead of inventing a substitute. Follow the displayed instructions, including opening SmartAlexa and retrying if requested.

After preparation, the local socket is closed and the temporary private key discarded. This does not revoke operational permissions or remove a public key remembered by Android. You can disable debugging in TV settings after setup. Normal operation does not keep an ADB connection open.

## 3. Link Alexa

1. At **Vincular Alexa** (Link Alexa), select **Gerar meu código** (Generate my code).
2. On your phone, open Alexa and the **TCL Assistant** skill's linking flow available to your account.
3. Enter the code shown on the TV before it expires. Generate another in the wizard if needed.
4. Wait for confirmation on the TV. A linked account and a connection ready for controls are shown as separate states.
5. When **Concluir configuração** (Finish setup) appears, continue and select **Ir para o painel** (Go to dashboard).

The code comes from the native TTVSCore flow. Registration, link confirmation, renewal and connection run in the app; no credential import is needed. A valid link already held by this installation is recognized and resumed. Data from another installation is not transferred automatically.

SmartAlexa remains the command executor. Its own screen may show a country restriction; this project's setup and linking are handled by the bridge. **Keep SmartAlexa and TTVSCore installed after linking.**

## 4. Check operation

Use the exact TV name registered in Alexa. Start with volume up and power off commands through your Echo, spoken in the language configured for your Alexa account. If wake is enabled, try power on after a minute in standby, then overnight standby and a reboot without opening the bridge.

First setup in an isolated installation demonstrated native identity collection without an imported file, registration without previous credentials, code generation, confirmation, renewal, connection and persistence. The final production APK preserved the link on update and passed renewal/pause/resume, executor/socket recovery and volume with actual effect.

Reboot recovered the service, account, cloud, executor and persistence without opening the UI, preserving power settings. An automated 60-second standby test passed: native power off, active cloud/executor and UDP wake without opening the UI. Echo voice confirmation on v0.4.0 was not repeated in this round. The visible installer with SmartAlexa absent, phone confirmation with an entirely new account, power restoration through the UI and overnight standby were not exercised in this round. These are coverage limits, not observed failures. See [QA](qa-v0.4.0.md) and [validation limits](validation.md).

![Dashboard showing account, cloud, controls and wake status](../images/painel.png)

## Recovery and advanced options

If local ADB port 5555 is unavailable, the wizard reports that preparation is unavailable on this firmware. Do not change country or invent identifiers to work around the failure. Report model, firmware and failed step, without publishing IDs, MAC addresses, codes or tokens.

To reapply permissions or change standby options, open **Configurar esta TV** (Configure this TV) on the dashboard. See [recovery](rollback.md) for power restoration and updating/reverting builds.
