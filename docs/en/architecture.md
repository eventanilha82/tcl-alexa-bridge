# Architecture and dependencies — 0.4.0

[Português (Brasil)](../architecture.md) | **English** · [Home](../../README.en.md)

```text
Setup using the TV remote
    → Android authorization → ADB at 127.0.0.1:5555
    → TCL identity and permissions → session closed

Echo → TCL skill → TCL cloud
                     ↓
         TTVSCore in the bridge process
                     ↓ Binder
          Installed SmartAlexa → TV
```

The bridge handles preparation, linking, private persistence, renewal, connection and recovery. TTVSCore handles registration, code generation and confirmation through its native protocol; the installed SmartAlexa executes controls through Binder. The bridge uses neither accessibility nor a notification listener and is not granted `INJECT_EVENTS`.

## Preparation and identity

The wizard creates a temporary RSA ADB key using modified adblib. It connects only to the TV's own loopback after user action and Android confirmation. It executes fixed preparation commands, without an exposed command console. It closes the connection and discards the private key afterward; normal operation does not depend on a permanent ADB session.

The profile comes from the TV: client type, firmware, country, Wi-Fi MAC and TCL identifier. The native identification interface is queried first. If unavailable, the final fallback reads only the UID 0 HAL diagnostic from the expected native tag, requiring an unambiguous 40-hex-digit identifier. Without valid identity, setup fails. It does not generate a UUID, Android ID hash or another TV's identifier.

The profile and tokens stay in the sandbox. An existing profile is not silently overwritten by an external inbox. The public APK contains empty example fields. An existing link is recognized and resumed by the wizard.

## Private TCL runtime

The app loads system package `com.tcl.ttvs` and prepares a private DEX copy with integrity caching. `CoreArchive` adapts the fixed storage path to the bridge sandbox. The applicationId and path lengths are part of that adaptation; renaming them requires review.

v0.4.0 also handles a specific firmware difference: the new TROM interface exists, but lacks the legacy `TBuild.VERSION.SDK_INT` field expected by the engine. Only in that case, `DexSdkCompat` adapts `PropertyUtil.isTclWidgetUseNewSdk()` in the private copy. In the actual examined DEX, 21 bytes changed; DEX integrity/checksums were recalculated and verified. The test requires the expected method signature and fails on incompatible formats. This is not a general rewrite of TCL APIs.

`IsolatedCoreLoader` isolates the engine and uses a local adapter for `sys.showalexa` and `sys.enablealexa`. Those property values exist only in the bridge runtime; the system TTVSCore's global flags are not reconfigured. The original installation's files and partitions remain intact.

Registration uses the installed TTVSCore code/signer. The bridge also has a verified `RSASignature.sign2bytes(byte[])` call for compatible operations and offline diagnostics. No TCL key is exported or embedded in the APK. The signer diagnostic signs a fixed message, without HTTP or new registration.

## Controls and power

The executor receives command families without a bridge whitelist. A success ACK demonstrates an executor response, not an effect in the player/channel/peripheral. The bridge adapts network/screen wake.

Standby support is optional. During preparation, ADB adjusts TCL network standby, which requires shell access on this firmware, and preserves its old value. `PowerCompatibility` handles global Low Power Standby through `WRITE_SECURE_SETTINGS`, also preserving the old value. Full restoration uses local preparation again for the network setting.

Integration depends on internal TCL interfaces; firmware, DEX, signer or Binder updates may require adaptation. v0.4.0 evidence covers native identity collection and initial linking, renewal/pause/resume, rebind, reconnection and volume in the final production APK on the Brazilian C6K. Reboot recovered the service without opening the UI, preserving the link, cloud, executor, persistence and power settings. Automated 60-second standby kept cloud/executor active and UDP wake worked without opening the UI. Voice confirmation on v0.4.0 was not repeated in this round. See [validation](validation.md) and [QA](qa-v0.4.0.md) for results and untested flows. This is neither Works with Alexa certification nor universal support.
