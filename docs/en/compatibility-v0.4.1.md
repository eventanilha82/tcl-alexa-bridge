# Version 0.4.1 compatibility

[Português (Brasil)](../compatibility-v0.4.1.md) | **English** · [Home](../../README.en.md)

The candidate fixes private TCSCore fields/constructors and DEX ordering in the bridge's runtime copy. The exact **10.60.03 sample has not been obtained** and is not certified by this study.

The candidate also passed link-preserving update, persisted native renewal, socket/executor recovery and actual volume tests on the reference TV. See [0.4.1 QA](qa-v0.4.1.md) to distinguish these checks from the Java contracts and pending standby/voice regressions.

## Validated scope

On 20 November 2026, a separate probe ran on a TCL C6K/G15 with Android 14/API34. Of 64 distinct Core APK SHA-256s, **48 passed** the exercised Java loading/contracts; **16 were blocked during preparation** by an unrecognized storage/SDK preparation layout on this device. The probe checks the loaded class origin so an installed Core cannot conceal a failed supplied copy.

The probe has a separate UID, no INTERNET permission and no access to the bridge account. It creates synthetic objects and checks reflection, types and state. It does not replace system TCSCore/SmartAlexa or validate registration, refresh, TV control or every firmware's native dependencies. Full bridge checks are separate and documented in release QA. **These results are not universal TCL TV support.**

Static checks passed for 52 of 64 APKs across 51 selected contracts; 12 differed. The Google Lite TV 10.45.10 sample lacks the selected Alexa classes. Older versions and this variant need their own investigation; this fix does not supply missing APIs.

## DEX ordering bug found on the TV

Redirecting `/data/data/com.tcl.ttvs/files/alexa/` into bridge private storage changed string ordering in some DEX files. ART rejected the runtime copies of 10.60.15, 10.60.16, 10.60.17 and 10.62.31. dexlib2 now rebuilds indices/references only when the substitution breaks ordering. The cache was advanced to `storage-v4` to avoid reusing old copies.

All four passed after the correction. The TV's installed 10.58.16 and private-member 10.65.12 also passed. Native state stays in the same private `files/aa` directory; this correction does not migrate tokens or firmware. Local checks covered all64 APKs for ordering and preserved class/member counts.

## Per-APK matrix

“Preparation blocked” describes this TV's test and does not prove failure on every TCL framework. “Java OK” does not mean complete Alexa control on that firmware.

| Core | Short SHA-256 | Static contracts | Android probe |
|---|---|---|---|
| 10.10.45 | `5504528df461` | 51/51 | Preparation blocked |
| 10.10.49 | `088f3ba39603` | 51/51 | Preparation blocked |
| 10.10.50 | `7e46016daf49` | 51/51 | Preparation blocked |
| 10.10.51 | `6717941a6497` | 51/51 | Preparation blocked |
| 10.22.07 | `e90a3e635bc1` | 51/51 | Java OK |
| 10.23.05 | `802dbb431f96` | 51/51 | Java OK |
| 10.23.08 | `c129c1a52704` | 51/51 | Java OK |
| 10.30.10 | `5f8b1dd97909` | 51/51 | Java OK |
| 10.30.12 | `c51e96b17a4c` | 51/51 | Java OK |
| 10.30.19 | `0f6282721c62` | 51/51 | Java OK |
| 10.31.01 | `547ad36fb1fd` | 51/51 | Java OK |
| 10.31.08 | `e97397f615d7` | 51/51 | Java OK |
| 10.31.09 | `8824e37658eb` | 51/51 | Java OK |
| 10.31.10 | `79274abe5f2b` | 51/51 | Java OK |
| 10.31.15 | `7fc92a864cff` | 51/51 | Java OK |
| 10.32.22 | `7f1655349b41` | 51/51 | Java OK |
| 10.37.51 | `0ff349588f2a` | 51/51 | Java OK |
| 10.37.52 | `96db8922987e` | 51/51 | Java OK |
| 10.38.18 | `660ced7db371` | 51/51 | Java OK |
| 10.38.19 | `716f2255948f` | 51/51 | Java OK |
| 10.38.22 | `34a62e632b91` | 51/51 | Java OK |
| 10.42.01 | `45874c5b6c20` | 51/51 | Java OK |
| 10.42.07 | `8f84eda6b122` | 51/51 | Java OK |
| 10.45.05 | `b64734820013` | 51/51 | Java OK |
| 10.45.10_3988228_260126_googleLiteTV | `8d3408067a92` | Different | Preparation blocked |
| 10.45.10_b740223_241227_overseaTV | `c52f6e489e1f` | 51/51 | Java OK |
| 10.45.17_43563bc_250116_overseaTV | `ea78df54f81b` | 51/51 | Java OK |
| 10.45.18_3a29689_250225_overseaTV | `ccb1b78b42b8` | 51/51 | Java OK |
| 10.45.24_f667d7b_250312_overseaTV | `3bc686031833` | 51/51 | Java OK |
| 10.45.25_52c4f2f_250320_overseaTV | `40436d447ffc` | 51/51 | Java OK |
| 10.45.27_3b38019_250402_overseaTV | `dc7c03ac2bde` | 51/51 | Java OK |
| 10.45.54_7efb8f6_250226_overseaTV | `324e4765a56e` | Different | Preparation blocked |
| 10.48.00_518ff59_250416_overseaTV | `4177b8bf1e47` | 51/51 | Java OK |
| 10.49.00_28b9764_250508_overseaTV | `090fa05c8b27` | 51/51 | Java OK |
| 10.49.04_d348150_250527_overseaTV | `ac055648e2f8` | 51/51 | Java OK |
| 10.49.06_3ca9e57_250630_overseaTV | `53de88a9ef8a` | 51/51 | Java OK |
| 10.49.08_e006e79_250716_overseaTV | `eb6395196909` | 51/51 | Java OK |
| 10.50.00_bad525a_250805_overseaTV | `76cba620d837` | 51/51 | Java OK |
| 10.50.02_bdb0815_250922_overseaTV | `5f412215d8b8` | 51/51 | Java OK |
| 10.50.06_61f413c_251029_overseaTV | `489f938ddf5c` | 51/51 | Java OK |
| 10.50.09_3fef739_260327_overseaTV | `ef0e7b862f19` | 51/51 | Java OK |
| 10.51.01_1f551e4_260312_overseaTV | `9967f9a10551` | 51/51 | Java OK |
| 10.51.02_23f0a79_260313_overseaTV | `0ca3a0ad5638` | 51/51 | Java OK |
| 10.56.31_f4db8b4_251010_overseaTV | `6cf281c1c3bc` | 51/51 | Java OK |
| 10.56.38_165fdfc_251120_overseaTV | `d4db3867e3fe` | 51/51 | Java OK |
| 10.58.00_edda857_251209_overseaTV | `e6bcc63c0d3f` | 51/51 | Java OK |
| 10.58.16_06adf5c_260311_overseaTV | `9ab96a63748c` | 51/51 | Java OK |
| 10.58.22_a1c6f66_260325_overseaTV | `d186f35fbd70` | 51/51 | Java OK |
| 10.60.15_422bf21_260617_overseaTV | `a905fd3c99c8` | 51/51 | Java OK |
| 10.60.16_b625b27_260710_overseaTV | `db33aba008d5` | 51/51 | Java OK |
| 10.60.17_560b6ed_260716_overseaTV | `67251c81e370` | 51/51 | Java OK |
| 10.62.31_c46501d_260805_overseaTV | `44adc64da7c3` | 51/51 | Java OK |
| 10.64.02_d432a8b_260907_overseaTV | `25116f21899d` | 51/51 | Java OK |
| 10.65.12_0c7383a_261112_overseaTV | `15fa0608488f` | 51/51 | Java OK |
| 2.9.36 | `7326cb9819a8` | Different | Preparation blocked |
| 2.9.67 | `6dc0d85c87e9` | Different | Preparation blocked |
| 2.9.78 | `c55896b33c88` | Different | Preparation blocked |
| 2.9.95 | `7a44e99aded3` | Different | Preparation blocked |
| 2.9.95 | `abb202356c35` | Different | Preparation blocked |
| 3.0.11 | `343f140da9bb` | Different | Preparation blocked |
| 3.0.17 | `e18a58c911f7` | Different | Preparation blocked |
| 3.0.25 | `19278388cf0c` | Different | Preparation blocked |
| 3.0.35 | `ff8e49a55258` | Different | Preparation blocked |
| 3.0.40 | `83db7a41ebde` | Different | Preparation blocked |

## Reproduce

Follow the [build guide](build.md). Locked dependencies are downloaded during the build. For a Core obtained from your own TV:

```sh
bash scripts/test-core-compatibility.sh /path/to/TCSCore.apk
bash scripts/build-core-probe.sh /path/to/TCSCore.apk
adb install -r build/core-probe/core-probe.apk
adb shell am instrument -w -e core core-1.apk local.tclbridge.compatprobe/local.tclbridge.CoreCompatibilityProbe
adb shell am force-stop local.tclbridge.compatprobe
adb uninstall local.tclbridge.compatprobe
```

Static fields and the SDK adapter are separate checks; some samples fail as expected. The probe embeds the supplied APKs and must not be published as a project asset. Earlier failed attempts were retained in the research history rather than discarded from the record.
