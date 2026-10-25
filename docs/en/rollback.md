# Pause, recovery and restoration

[Português (Brasil)](../rollback.md) | **English** · [Home](../../README.en.md)

## Linking and updates

- Pausing in the UI preserves the link. An in-flight renewal must finish before starting another session, so rotated tokens are not lost.
- Closing the screen is not pausing. The service and boot receiver provide continuity; reboot on the current version recovered the service without opening the UI. Automated 60-second standby and UDP wake passed. Voice confirmation on this version was not repeated in this round.
- Force stop, clear data and uninstall have explicit Android effects. Clearing data removes the profile, link and saved restoration values; it is not a normal screen-close test.
- Keep SmartAlexa and TTVSCore installed. No firmware flashing/root is required.
- Updates require the same signing key. The installer does not automatically uninstall to bypass different signatures. Preserve signature and data when updating to v0.4.0; the wizard recognizes the link already held by this installation.
- A personal build or lab APK may have a different signature/package. Its data is not automatically shared with production. Never publish a temporary APK with `debuggable=true`.

## Restore power settings

Open **Configurar esta TV** (Configure this TV) on the dashboard, authorize the local connection again if asked, uncheck **Permitir ligar a TV por Alexa em espera** (Allow Alexa to turn on the TV in standby), and complete **Autorizar e preparar** (Authorize and prepare). Preparation restores both the original TCL network standby value and Low Power Standby. The network setting requires local ADB on the tested firmware.

**Manter Alexa em espera** (Keep Alexa in standby) controls Low Power Standby; disabling it alone does not replace restoring TCL network settings through preparation. Do not clear data before restoration: previous values are held in private storage. Disable debugging in TV settings afterward if no longer needed.

Closing the session and discarding the private key does not automatically revoke the bridge's `WRITE_SECURE_SETTINGS` permission. Advanced users can revoke it through ADB after restoring power settings:

```sh
adb -s YOUR_TV_IP:5555 shell pm revoke local.tclbridge android.permission.WRITE_SECURE_SETTINGS
```

## Revert an update

Keep the previous APK from the same source/signing key and check the result of `adb install --no-incremental -r`. Android may block a versionCode downgrade; in that case, reverting while preserving data requires a compatible build with a higher versionCode and the previous behavior. Do not automatically uninstall. Data compatibility must be assessed when reverting from v0.4.0.

Check the [current version report](qa-v0.4.0.md) before attributing earlier test results to it.
