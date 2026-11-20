# Compatibilidade da 0.4.1

**Português (Brasil)** | [English](en/compatibility-v0.4.1.md) · [Início](../README.md)

A versão corrige acessos a campos/construtores privados do TCSCore e a ordenação DEX da cópia de execução usada pela ponte. A versão exata **10.60.03 ainda não foi obtida** e não está certificada por este estudo.

A versão também passou por atualização preservando vínculo, renovação nativa persistida, recuperação de socket/executor e volume real na TV de referência. Consulte o [QA da 0.4.1](qa-v0.4.1.md) para separar esses testes dos contratos Java e das limites de cobertura.

## O que foi validado

Em 20/11/2026, um probe separado foi executado numa TCL C6K/G15 com Android 14/API 34. Foram fornecidos 64 APKs de Core distintos por SHA-256: **48 passaram** no carregamento e nos contratos Java exercitados; **16 foram bloqueados na preparação** por layout de armazenamento/SDK não reconhecido neste ambiente. A assinatura do APK do probe e as classes carregadas foram conferidas. As classes selecionadas precisam vir do APK fornecido, evitando que a instalação atual encubra uma falha.

O probe tem UID separado, sem INTERNET ou acesso à conta da ponte. Instancia objetos com valores sintéticos e verifica reflexão, tipos e estado. Não substitui TCSCore/SmartAlexa do sistema e não testa registro, renovação, controle de TV ou todas as bibliotecas nativas de cada firmware. O teste completo da ponte é separado e documentado na QA da release. **Esses resultados não representam suporte universal a todas as TVs TCL.**

Na análise estática, 52 dos 64 APKs passaram nos 51 contratos selecionados; 12 divergiram. O Google Lite TV 10.45.10 desta coleção não contém as classes Alexa selecionadas. As versões antigas e essa variante precisam de estudo próprio; o fix atual não fornece APIs que estejam ausentes.

## Bug de ordenação encontrado na TV

O redirecionamento de `/data/data/com.tcl.ttvs/files/alexa/` para a pasta privada da ponte alterava a ordem lexical das strings em certos DEX. O ART rejeitou a cópia de execução de 10.60.15, 10.60.16, 10.60.17 e 10.62.31. Corrigimos a cópia com dexlib2, reconstruindo índices/referências somente quando a ordem fica inválida. O cache passou para `storage-v4` para não reutilizar cópias anteriores.

As quatro versões passaram depois da correção. A 10.58.16 instalada na TV e a 10.65.12 com membros privados também passaram. O armazenamento nativo continua na mesma pasta privada `files/aa`; os tokens e o firmware original não são migrados por essa correção. A verificação local dos 64 APKs conferiu ordenação e preservação das contagens de classes/membros.

## Matriz por APK

“Preparação bloqueada” descreve o teste nesta TV e não prova incompatibilidade em todo framework TCL. “Java OK” não equivale a controle Alexa completo nesse firmware.

| Core | SHA-256 curto | Contrato estático | Probe Android |
|---|---|---|---|
| 10.10.45 | `5504528df461` | 51/51 | Preparação bloqueada |
| 10.10.49 | `088f3ba39603` | 51/51 | Preparação bloqueada |
| 10.10.50 | `7e46016daf49` | 51/51 | Preparação bloqueada |
| 10.10.51 | `6717941a6497` | 51/51 | Preparação bloqueada |
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
| 10.45.10_3988228_260126_googleLiteTV | `8d3408067a92` | Divergência | Preparação bloqueada |
| 10.45.10_b740223_241227_overseaTV | `c52f6e489e1f` | 51/51 | Java OK |
| 10.45.17_43563bc_250116_overseaTV | `ea78df54f81b` | 51/51 | Java OK |
| 10.45.18_3a29689_250225_overseaTV | `ccb1b78b42b8` | 51/51 | Java OK |
| 10.45.24_f667d7b_250312_overseaTV | `3bc686031833` | 51/51 | Java OK |
| 10.45.25_52c4f2f_250320_overseaTV | `40436d447ffc` | 51/51 | Java OK |
| 10.45.27_3b38019_250402_overseaTV | `dc7c03ac2bde` | 51/51 | Java OK |
| 10.45.54_7efb8f6_250226_overseaTV | `324e4765a56e` | Divergência | Preparação bloqueada |
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
| 2.9.36 | `7326cb9819a8` | Divergência | Preparação bloqueada |
| 2.9.67 | `6dc0d85c87e9` | Divergência | Preparação bloqueada |
| 2.9.78 | `c55896b33c88` | Divergência | Preparação bloqueada |
| 2.9.95 | `7a44e99aded3` | Divergência | Preparação bloqueada |
| 2.9.95 | `abb202356c35` | Divergência | Preparação bloqueada |
| 3.0.11 | `343f140da9bb` | Divergência | Preparação bloqueada |
| 3.0.17 | `e18a58c911f7` | Divergência | Preparação bloqueada |
| 3.0.25 | `19278388cf0c` | Divergência | Preparação bloqueada |
| 3.0.35 | `ff8e49a55258` | Divergência | Preparação bloqueada |
| 3.0.40 | `83db7a41ebde` | Divergência | Preparação bloqueada |

## Reproduzir

Configure o ambiente do [guia de build](build.md). O build baixa as dependências fixadas em `dependencies.lock.json`. Para um Core obtido da própria TV:

```sh
bash scripts/test-core-compatibility.sh /caminho/TCSCore.apk
bash scripts/build-core-probe.sh /caminho/TCSCore.apk
adb install -r build/core-probe/core-probe.apk
adb shell am instrument -w -e core core-1.apk local.tclbridge.compatprobe/local.tclbridge.CoreCompatibilityProbe
adb shell am force-stop local.tclbridge.compatprobe
adb uninstall local.tclbridge.compatprobe
```

A lista de campos estáticos e o adaptador SDK são verificações distintas; algumas amostras falham de maneira esperada. O probe contém os APKs fornecidos e não deve ser publicado como asset do projeto. Os resultados anteriores com falhas foram preservados na pesquisa; não foram apagados para contar somente as tentativas positivas.
