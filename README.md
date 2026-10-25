# TCL Alexa Bridge

Ponte comunitária para manter o vínculo Alexa em uma TCL C6K brasileira, usando o TTVSCore e o SmartAlexa **já instalados na TV**. O aplicativo cuida de vínculo, sessão, persistência, inicialização, recuperação e despertar. O SmartAlexa executa os controles. Não é um substituto independente nem uma certificação Works with Alexa.

**Instalação da versão 0.3.4:** o Downloader instala os APKs; a configuração inicial ainda exige um computador com ADB e o perfil real da TV. A geração do código e o armazenamento/renovação de tokens ficam no app. O assistente completo de instalação na TV ainda não está implementado.

## Compatibilidade comprovada

TCL C6K/G15 brasileira, Android 14 (API 34), firmware V8-0016T01-LF1V048, TTVSCore 10.58.16 e SmartAlexa 6.13.25. Outras combinações precisam de validação. O código carrega interfaces internas da TCL: uma atualização pode exigir adaptação. O `applicationId` é fixo em `local.tclbridge` porque o mapeamento do armazenamento nativo depende do comprimento do caminho.

## Download e instalação

Na release [v0.3.4](https://github.com/eventanilha82/tcl-alexa-bridge/releases/tag/v0.3.4), os APKs são assets separados:

- [SmartAlexa 6.13.25 original](https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.3.4/SmartAlexa-6.13.25.apk) — proprietário TCL; veja [proveniência e licença](THIRD_PARTY_NOTICES.md).
- [TCL Alexa Bridge 0.3.4](https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.3.4/TCL-Alexa-Bridge-0.3.4.apk) — código próprio MIT.

As URLs ficam disponíveis após a publicação. O [guia completo](docs/setup.md) explica Downloader na TV, ADB, permissões, perfil privado e vínculo. Instalar os APKs sozinho não configura a identidade da TV.

## Arquitetura

Echo → skill TCL → nuvem TCL → TTVSCore no processo da ponte → Binder → SmartAlexa → TV.

O app cria uma cópia privada do DEX do TTVSCore e substitui somente o caminho fixo de armazenamento; verifica integridade e recalcula checksums DEX. Não altera a instalação original nem partições de firmware. Registro usa o assinador do TTVSCore de sistema em runtime; nenhuma chave TCL é exportada ou incorporada no app. A implementação não usa acessibilidade ou listener de notificações.

Veja o [desenho da integração](docs/architecture.md).

## Build local

Requer JDK com `javac --release 8` (validado com JDK 27), Python 3 e Android SDK Platform 34 / Build Tools 36.0.0.

```sh
sdkmanager "platforms;android-34" "build-tools;36.0.0"
export ANDROID_SDK_ROOT=/caminho/do/android-sdk
./scripts/build.sh
```

Detalhes de dependências, assinatura e verificação em [build local](docs/build.md).

O script cria uma chave de assinatura pessoal em `.private/` e o APK em `build/`; ambos ficam fora do Git. Preserve essa chave para atualizar suas instalações. O APK público não contém um perfil real de TV.

## Configuração de outra TV

1. Copie `config/device-profile.example.json` para `.private/device-profile.local.json`.
2. Preencha **os dados reais da sua própria TV**: identificador TCL, MAC utilizável, client_type, código numérico TCL do país e firmware. Não gere IDs aleatórios e não reutilize o perfil de outra TV. `country=52` é o valor validado para esta instalação brasileira, não um código ISO genérico.
3. Habilite depuração ADB e autorize seu computador na TV. TV e Echo precisam alcançar a rede correspondente.
4. Instale e configure:

```sh
./scripts/install.sh IP_DA_TV:5555 .private/device-profile.local.json
```

5. Abra a interface, selecione Iniciar/Vincular conta e conclua o código na skill TCL. Após a configuração, não é necessário abrir a interface em cada boot.

O coletor automático de identidade ainda não foi validado; o perfil manual é requisito desta versão. Credenciais são criadas pelo próprio fluxo de vínculo e guardadas no armazenamento privado. O inbox do perfil é importado apenas na primeira configuração e removido após sucesso; um perfil privado existente não é sobrescrito silenciosamente.

## Ligar a TV em standby

Na combinação testada, o Low Power Standby bloqueava rede/CPU do app com a tela apagada. A compatibilidade global é **opt-in** e pode aumentar o consumo em espera:

```sh
./scripts/install.sh IP_DA_TV:5555 .private/device-profile.local.json --standby-compat
```

Essa opção concede `WRITE_SECURE_SETTINGS` pelo ADB e permite ao app manter a configuração específica de standby. É uma permissão ampla; o código deste projeto a utiliza para esse ajuste. O estado anterior é preservado e pode ser restaurado em Configurações → Manter Alexa em espera. AUTO_START e ASSOC_START são permissões operacionais específicas do firmware TCL configuradas para o pacote e UID; não são INJECT_EVENTS.

## Cobertura e limites

As mensagens são encaminhadas sem uma whitelist de famílias: pesquisa, canais, entradas, energia, volume, apps, reprodução, mudo, navegação, seek e PVR chegam ao executor oficial quando disponíveis. Retornos e bytes permanecem nativos. A ponte adapta o ligar por rede/tela, pois o executor SmartAlexa analisado trata o desligamento.

Métricas contam **encaminhados ao executor**, não todos os recebidos da nuvem. A biblioteca nativa pode descartar uma mensagem durante `BINDING`; não fazemos reenvio automático que possa duplicar ações. Retorno `code=0` confirma resposta do executor, não necessariamente efeito em conteúdo/periférico. Canais exigem sintonia; PVR e playlists dependem de recursos reais; na variante brasileira analisada, o caminho de gravação não demonstrou implementação efetiva. Aplicativos podem anunciar controles diferentes.

Veja [validação](docs/validation.md), [revisão](docs/review.md) e [recuperação](docs/rollback.md).

## Diagnóstico e contribuição

```sh
./scripts/diagnostics.sh IP_DA_TV:5555 > .private/diagnostics.txt
```

Esse resumo evita o logcat TCL bruto, que pode conter dados de autenticação. Não envie tokens, IDs, MACs, perfis privados, keystores ou dumps nativos completos em issues. Descreva modelo, firmware, versões dos pacotes e comandos/frases usados. Um relatório de reprodução deve distinguir chamada de voz, injeção local na ponte, retorno do executor e efeito observado.

O fonte não inclui firmware ou APKs TCL. A release disponibiliza o SmartAlexa proprietário como asset separado, sem cobertura MIT; consulte [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) para origem e hashes. Código próprio sob MIT; componentes TCL mantêm seus próprios direitos e termos.
