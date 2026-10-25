# Componentes externos e proveniência

**Português (Brasil)** | [English](docs/en/third-party-notices.md) · [Início](README.md)

A licença MIT deste repositório cobre somente o código próprio da TCL Alexa Bridge. TCL, SmartAlexa, TTVSCore, Alexa e marcas relacionadas pertencem aos respectivos titulares. O projeto é comunitário e não declara afiliação, autorização comercial ou certificação Works with Alexa.

## SmartAlexa: APK proprietário separado

A release `v0.4.0` disponibiliza `SmartAlexa-6.13.25.apk` como asset separado, original e com assinatura TCL preservada. Esse APK não é software MIT e sua disponibilização não transfere direitos sobre o componente. O repositório fonte não incorpora seu código, bibliotecas decompiladas ou chave de assinatura. Os termos e direitos do fabricante continuam aplicáveis.

Proveniência registrada na extração local em 24/10/2026:

| Item | Valor |
| --- | --- |
| Pacote | `com.tcl.smartalexa` |
| Versão | `6.13.25_d9fcb51_26012709` |
| versionCode | `613250000` |
| Firmware doador | `V8-R75PT01-LF1V645.zip` (R75/G10; diferente do G15 da TV testada) |
| Tamanho do firmware | 2070866176 bytes |
| SHA256 do firmware | `6d64508b8d9adea2896b2440f4b05a0044dae2164c09ce27ca69ad607ec947d6` |
| SHA256 do APK | `f1203599875f2e6d082b893eb3e64a44c071d230d55336d05cf74061531d4441` |
| SHA256 do certificado TCL | `25606cd9fadf3da6a16e741db6d70c3c5c5ebf1ec58bfc327517dec430ceba98` |

Fontes registradas: [mirror do firmware](https://disk.yandex.ru/d/neNks1g0nv50fw) e [post com os links](https://forum.benchmark.rs/posts/7811729/). São registros de proveniência, não garantia de disponibilidade futura nem atestado de licença do mirror. O hash do firmware conferiu com o informado no mirror; a assinatura criptográfica do firmware completo não foi validada. `apksigner verify` confirmou as assinaturas v1/v2/v3 do APK extraído. Nenhum flash do firmware doador é necessário.

O SmartAlexa extraído posteriormente do V648 apresentou o mesmo SHA256 do APK. Isso não estabelece que 6.13.25 seja a última versão global da TCL. O SHA256 documenta a identidade do arquivo; não substitui os termos do fabricante.

## TTVSCore e runtime

TTVSCore é uma dependência proprietária de sistema já instalada na TV. Não é distribuído como asset deste projeto. O runtime privado é criado somente no aparelho a partir da instalação local e não deve ser publicado. O assinador instalado é chamado em runtime sem exportar ou incorporar a chave TCL na ponte.

## adblib modificado — BSD

Os arquivos `app/src/com/cgutman/adblib/AdbCrypto.java` e `AdbBase64.java` derivam de [cgutman/AdbLib](https://github.com/cgutman/AdbLib), revisão `d6937951eb98557c76ee2081e383d50886ce109a`, Copyright (c) 2013 Cameron Gutman, sob licença BSD de três cláusulas. Essa parte não é relicenciada como MIT. O texto integral está em [app/assets/AdbLib-LICENSE.txt](app/assets/AdbLib-LICENSE.txt), preservado também como asset do APK para distribuição binária.

A cópia foi modificada para gerar somente chaves ADB temporárias durante a preparação local; os caminhos de importação/exportação e persistência de chaves foram removidos. A chave pública é enviada ao Android para autorização e a chave privada é usada somente na sessão. Isso não envolve extrair ou distribuir chaves TCL. Preserve os avisos de copyright, condições e isenção de responsabilidade nas distribuições do fonte e binário. O nome do autor não implica endosso ao projeto.

## Ferramentas de build

Android SDK/Build Tools, JDK e Python são dependências externas, sob seus próprios termos. Instale-os pelos canais apropriados e aceite seus termos diretamente. O fonte não contém firmware, APKs TCL, bibliotecas decompiladas ou material de assinatura TCL; a exceção de distribuição binária explicitamente separada é o asset SmartAlexa acima.
