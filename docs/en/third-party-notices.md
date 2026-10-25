# Third-party components and provenance

[Português (Brasil)](../../THIRD_PARTY_NOTICES.md) | **English** · [Home](../../README.en.md)

The repository's MIT license covers only original TCL Alexa Bridge code. TCL, SmartAlexa, TTVSCore, Alexa and related trademarks belong to their respective owners. This is a community project and does not claim affiliation, commercial authorization or Works with Alexa certification.

## SmartAlexa: separate proprietary APK

Release `v0.4.0` provides `SmartAlexa-6.13.25.apk` as a separate original asset with its TCL signature preserved. This APK is not MIT software; making it available does not transfer rights to the component. The source repository does not include its code, decompiled libraries or signing key. The manufacturer's terms and rights continue to apply.

Provenance recorded during local extraction on October 24, 2026:

| Item | Value |
| --- | --- |
| Package | `com.tcl.smartalexa` |
| Version | `6.13.25_d9fcb51_26012709` |
| versionCode | `613250000` |
| Donor firmware | `V8-R75PT01-LF1V645.zip` (R75/G10; different from the tested TV's G15) |
| Firmware size | 2070866176 bytes |
| Firmware SHA256 | `6d64508b8d9adea2896b2440f4b05a0044dae2164c09ce27ca69ad607ec947d6` |
| APK SHA256 | `f1203599875f2e6d082b893eb3e64a44c071d230d55336d05cf74061531d4441` |
| TCL certificate SHA256 | `25606cd9fadf3da6a16e741db6d70c3c5c5ebf1ec58bfc327517dec430ceba98` |

Recorded sources: [firmware mirror](https://disk.yandex.ru/d/neNks1g0nv50fw) and [post with links](https://forum.benchmark.rs/posts/7811729/). These document provenance, not guaranteed future availability or a license attestation for the mirror. The firmware hash matched the mirror's value; the full firmware's cryptographic signature was not validated. `apksigner verify` confirmed v1/v2/v3 signatures of the extracted APK. No donor firmware flashing is needed.

SmartAlexa later extracted from V648 had the same APK SHA256. This does not establish that 6.13.25 is TCL's latest global version. SHA256 identifies the file; it does not replace the manufacturer's terms.

## TTVSCore and runtime

TTVSCore is a proprietary system dependency already installed on the TV. It is not distributed as a project asset. The private runtime is created only on the device from its local installation and must not be published. The installed signer is called at runtime without exporting or embedding the TCL key in the bridge.

## Modified adblib — BSD

`app/src/com/cgutman/adblib/AdbCrypto.java` and `AdbBase64.java` derive from [cgutman/AdbLib](https://github.com/cgutman/AdbLib), revision `d6937951eb98557c76ee2081e383d50886ce109a`, Copyright (c) 2013 Cameron Gutman, under the three-clause BSD license. This portion is not relicensed as MIT. The full license is in [app/assets/AdbLib-LICENSE.txt](../../app/assets/AdbLib-LICENSE.txt), also preserved as an APK asset for binary distribution.

The copy was modified to generate temporary ADB keys only during local preparation; key import/export and persistence paths were removed. The public key is sent to Android for authorization and the private key is used only in the session. This does not involve extracting or distributing TCL keys. Preserve copyright notices, conditions and disclaimers in source and binary distributions. The author's name does not imply endorsement.

## Build tools

Android SDK/Build Tools, JDK and Python are external dependencies under their own terms. Install them from appropriate channels and accept their terms directly. Source contains no firmware, TCL APKs, decompiled libraries or TCL signing material; the explicitly separate binary distribution exception is the SmartAlexa asset described above.
