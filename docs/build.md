# Build local

O fonte usa Java/Android diretamente, sem Gradle. Requisitos: Bash, Python 3, JDK com `javac --release 8`, `jar` e `keytool`, Android SDK Platform 34 e Build Tools 36.0.0. A execução local foi validada com JDK 27. O mínimo do APK é API 34; o suporte de hardware permanece limitado à combinação descrita no README.

```sh
sdkmanager "platforms;android-34" "build-tools;36.0.0"
export ANDROID_SDK_ROOT=/caminho/do/android-sdk
python3 scripts/check-shareable.py
./scripts/build.sh
```

O script compila apenas `app/src`, gera DEX, empacota recursos/assets, alinha e assina `build/alexa-bridge.apk`. Pode-se sobrescrever `BRIDGE_BUILD_TOOLS` e `BRIDGE_ANDROID_JAR` para apontar explicitamente às dependências locais. Não há dependência de APK vendor, fonte decompilado ou chave TCL no build. O perfil distribuído fica vazio; a configuração real é importada no dispositivo.

Na primeira execução, são gerados `.private/signing.p12` e `.private/signing-pass.txt`. Preserve os dois fora do Git e mantenha backup seguro. Atualizações de uma instalação exigem a mesma chave. Um build pessoal terá assinatura diferente da release; não será atualização direta dela. O instalador não remove automaticamente uma instalação para contornar assinatura incompatível.

O fluxo de build é repetível com as dependências fixadas; não promete APK idêntico byte a byte, pois a chave inicial é aleatória e o empacotamento pode variar. Releases precisam conservar a chave de assinatura e publicar o SHA256 do APK exato. Exemplo de conferência local:

```sh
python3 -c 'import hashlib,pathlib; p=pathlib.Path("build/alexa-bridge.apk"); print(hashlib.sha256(p.read_bytes()).hexdigest())'
"$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner" verify --verbose build/alexa-bridge.apk
```

Antes de compartilhar o fonte, rode novamente `scripts/check-shareable.py` e revise manualmente o conteúdo. O scanner detecta padrões conhecidos; não substitui revisão de segredos. Não versione `build/`, `.private/`, perfis reais, logs brutos, firmware, runtime privado ou material de assinatura. O SmartAlexa original é um asset separado da release, com licença própria; não entra no build da ponte.
