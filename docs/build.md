# Compilar do zero

Para usar a ponte, você pode instalar o APK pronto pelo [guia da TV](setup.md). Este guia é para quem quer gerar seu próprio APK e revisar o código.

O projeto usa Java/Android diretamente, sem Gradle ou Android Studio obrigatório. Você precisa de Git, Bash, Python 3, um JDK com `java`, `javac`, `jar` e `keytool`, e Android SDK. O ambiente de desenvolvimento foi validado com JDK 27, Platform 34 e Build Tools 36.0.0. O APK exige Android API 34; a compatibilidade de hardware continua limitada ao ambiente documentado no README.

## 1. Instalar ferramentas no computador

Instale Git, Python 3 e JDK 27 usando o fornecedor/gerenciador de pacotes da sua preferência. Use um JDK completo, não apenas um JRE. Confirme no terminal:

```sh
git --version
python3 --version
java -version
javac -version
jar --version
keytool -help
```

Se houver mais de um Java instalado, configure `JAVA_HOME` para a pasta do JDK escolhido e inclua seu diretório `bin` no `PATH`. `java` e `javac` devem apontar para a mesma instalação. Os scripts são Bash: no Windows, use um ambiente Linux/WSL com ferramentas e SDK Linux correspondentes. O ambiente originalmente validado foi macOS.

Baixe **Command line tools only** para seu sistema na [página oficial do Android SDK](https://developer.android.com/studio#command-tools). O ZIP contém uma pasta `cmdline-tools`. Extraia-a e organize o SDK assim:

```text
Android/Sdk/
  cmdline-tools/
    latest/
      bin/sdkmanager
      lib/
      source.properties
```

O diretório `latest` deve conter diretamente `bin`, `lib` e os demais arquivos, sem outro nível `cmdline-tools` dentro dele. Exemplo em Bash para um SDK novo, substituindo o caminho do ZIP pelo arquivo baixado:

```sh
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
unzip /caminho/commandlinetools-do-seu-sistema.zip -d "$ANDROID_SDK_ROOT/command-tools-unpacked"
mv "$ANDROID_SDK_ROOT/command-tools-unpacked/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;36.0.0"
```

Leia e aceite os termos apresentados pelo SDK. Se já tiver um SDK, use seu diretório existente e evite sobrescrever ferramentas instaladas. Você pode guardar `ANDROID_SDK_ROOT` e o ajuste de `PATH` na configuração do seu terminal para futuras sessões.

## 2. Obter o fonte

```sh
git clone https://github.com/eventanilha82/tcl-alexa-bridge.git
cd tcl-alexa-bridge
```

Use a revisão correspondente à release atual caso o branch principal já tenha avançado. Não copie perfis, tokens, APKs de laboratório ou chaves de outras instalações para o repositório.

## 3. Compilar e conferir

```sh
python3 scripts/check-shareable.py
./scripts/build.sh
```

O resultado é `build/alexa-bridge.apk`. Não é necessário baixar firmware, SmartAlexa ou TTVSCore para compilar. Eles são dependências de execução na TV.

O script compila apenas `app/src`, gera DEX, empacota recursos/assets, alinha e assina `build/alexa-bridge.apk`. Pode-se sobrescrever `BRIDGE_BUILD_TOOLS` e `BRIDGE_ANDROID_JAR` para apontar explicitamente às dependências locais. Não há dependência de APK vendor, fonte decompilado ou chave TCL no build. O perfil distribuído fica vazio; o assistente coleta e valida a identidade nativa no próprio dispositivo.

Na primeira execução, são gerados `.private/signing.p12` e `.private/signing-pass.txt`. Preserve os dois fora do Git e mantenha backup seguro. Atualizações de uma instalação exigem a mesma chave. Um build pessoal terá assinatura diferente da release; não será atualização direta dela. O instalador não remove automaticamente uma instalação para contornar assinatura incompatível.

O fluxo de build é repetível com as dependências fixadas; não promete APK idêntico byte a byte, pois a chave inicial é aleatória e o empacotamento pode variar. Releases precisam conservar a chave de assinatura e publicar o SHA256 do APK exato. Exemplo de conferência local:

```sh
python3 -c 'import hashlib,pathlib; p=pathlib.Path("build/alexa-bridge.apk"); print(hashlib.sha256(p.read_bytes()).hexdigest())'
"$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner" verify --verbose build/alexa-bridge.apk
```

Antes de compartilhar o fonte, rode novamente `scripts/check-shareable.py` e revise manualmente o conteúdo. O scanner detecta padrões conhecidos; não substitui revisão de segredos. Não versione `build/`, `.private/`, perfis reais, logs brutos, firmware, runtime privado ou material de assinatura. O SmartAlexa original é um asset separado da release, com licença própria; não entra no build da ponte.

## Produção e testes isolados da 0.4.0

A versão de produção deve manter `android:debuggable="false"`. Builds temporários de laboratório com depuração habilitada servem exclusivamente para validação local e não são assets de release. Não publique APKs de laboratório nem seus dados privados. Um pacote isolado também exige adaptação coerente de authorities e caminhos privados; apenas renomear o applicationId não é suficiente.

## Licenças e testes

O build público inclui `com.cgutman.adblib` modificado, usado somente para a criptografia de uma chave ADB temporária durante preparação local. A licença BSD está em `app/assets/AdbLib-LICENSE.txt` e é incluída no APK pelo empacotamento de assets. Preserve esse arquivo e os avisos do fonte; consulte [notices](../THIRD_PARTY_NOTICES.md).

As suítes Java em `tests/LocalAdbTest.java` e `tests/DexSdkCompatTest.java` verificam o protocolo local e a adaptação DEX. Elas complementam a execução na TV: não comprovam por si só autorização visual, instalação pelo Android, vínculo real, reinício ou standby. A revisão da 0.4.0 corrigiu a entrega de pacotes CLSE antigos no ADB; a suíte correspondente passou por 54 verificações no ambiente de desenvolvimento.

Antes de uma release, confirme versão/nome do APK, assinatura conservada, `debuggable=false`, ausência de identidade/tokens, licença BSD empacotada e o hash do APK exato. O APK final foi conferido com DEX/manifest iguais aos do build público, assinatura válida, `debuggable=false` e licença BSD presente. A primeira configuração passou em instalação isolada; o APK de produção passou pela regressão de renovação, pausa, reconexão, volume e reinício sem abrir a interface. O ciclo automático de 60 segundos em standby e o despertar UDP também passaram; voz da versão atual ainda não foi confirmada nesta rodada. Consulte o [relatório atual](qa-v0.4.0.md) para hash, resultados e limites de QA.

### Executar testes locais sem Android

A partir da raiz do repositório, com o JDK disponível, compile as classes usadas pelos testes. Não é necessário SDK Android, TV, root ou sessão ADB real para a suíte `LocalAdbTest`; ela cria um servidor simulado apenas no loopback do computador.

```sh
mkdir -p .private/test-classes
javac --release 8 -d .private/test-classes \
  app/src/com/cgutman/adblib/AdbBase64.java \
  app/src/com/cgutman/adblib/AdbCrypto.java \
  app/src/local/tclbridge/LocalAdb.java \
  app/src/local/tclbridge/DexSdkCompat.java \
  tests/LocalAdbTest.java \
  tests/DexSdkCompatTest.java
java -cp .private/test-classes local.tclbridge.LocalAdbTest
```

O teste opcional `DexSdkCompatTest` precisa de um APK TTVSCore compatível legitimamente extraído da sua própria TV. Ele não está incluído e não é requisito para compilar a ponte. Se você já dispõe desse APK, execute:

```sh
java -cp .private/test-classes local.tclbridge.DexSdkCompatTest \
  /caminho/privado/TTVSCore.apk .private/sdk-compat-test.dex
```

O segundo argumento é um DEX privado gerado para inspeção do teste. Não publique esse arquivo ou o APK TCL no repositório. O teste depende do layout da versão compatível; uma falha com outra versão requer investigação e não justifica aplicar uma modificação genérica ao runtime.

## 4. Instalar seu build

Transfira `build/alexa-bridge.apk` para a TV por um método de instalação de sua preferência e siga o [assistente na TV](setup.md). Se optar pelo computador, habilite e autorize a depuração de rede na TV e execute:

```sh
./scripts/install.sh IP_DA_TV:5555
```

O script apenas instala seu APK de `build/` pelo ADB e abre o assistente. Não envia perfil ou tokens. Substitua IP e porta pelos da sua TV. Uma mensagem de assinatura incompatível significa que o APK não é atualização direta da instalação atual; não desinstale automaticamente para contornar o erro. Confira assinatura, origem e preservação dos dados antes de trocar de instalação.

Se `sdkmanager` não for encontrado, confira a estrutura `cmdline-tools/latest/bin` e o `PATH`. Se o build pedir Platform/Build Tools, confirme `ANDROID_SDK_ROOT` e a instalação das versões exatas acima. Se Java não for encontrado ou não aceitar as opções usadas, confira o JDK completo e o `JAVA_HOME`.
