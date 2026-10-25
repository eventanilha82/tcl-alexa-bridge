# TCL Alexa Bridge

**Português (Brasil)** | [English](README.en.md)

Use Alexa na TCL C6K brasileira com configuração pelo controle remoto da TV. A ponte cuida do vínculo, da conexão e da recuperação; os componentes nativos **TTVSCore e SmartAlexa continuam necessários** para integrar e executar os comandos.

**Versão 0.4.0 · Sem root ou troca de firmware · Projeto comunitário, sem certificação Works with Alexa.** A interface do aplicativo está em português.

## Comece aqui

1. Na TV, use o Downloader para baixar e instalar o [TCL Alexa Bridge 0.4.0](https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/TCL-Alexa-Bridge-0.4.0.apk).
2. Abra **Alexa na TV**. Se solicitado, escolha **Instalar SmartAlexa** e confirme o instalador do Android.
3. Siga **Começar configuração**, habilite a depuração indicada pelo assistente e selecione **Autorizar e preparar**. Confirme a autorização do Android na própria TV.
4. Selecione **Gerar meu código** e conclua o vínculo no celular pela skill **TCL Assistant**, no app Alexa.
5. Aguarde conta, conexão e controles prontos. Teste pelo Echo usando o nome exato da TV cadastrado na Alexa.

**[Passo a passo com imagens →](docs/setup.md)** Não é necessário computador, perfil JSON ou importação manual de tokens no fluxo validado. A preparação usa uma conexão ADB temporária apenas com a própria TV; a depuração pode ser desativada depois.

Para ligar por Alexa em espera, marque a opção voluntária durante a preparação. Ela concede `WRITE_SECURE_SETTINGS` e altera configurações de energia/rede, podendo aumentar o consumo em standby. Veja [como restaurar os valores anteriores](docs/rollback.md).

## Guias em português e inglês

| Quero… | Português | English |
| --- | --- | --- |
| Instalar e vincular à Alexa | [Configuração na TV](docs/setup.md) | [TV setup](docs/en/setup.md) |
| Compilar meu próprio APK | [Compilação](docs/build.md) | [Build from source](docs/en/build.md) |
| Entender a ponte e as dependências | [Arquitetura](docs/architecture.md) | [Architecture](docs/en/architecture.md) |
| Resolver problemas ou restaurar energia | [Recuperação](docs/rollback.md) | [Recovery](docs/en/rollback.md) |
| Conferir testes e limitações | [QA da 0.4.0](docs/qa-v0.4.0.md) · [Validação](docs/validation.md) | [v0.4.0 QA](docs/en/qa-v0.4.0.md) · [Validation](docs/en/validation.md) |
| Revisar as proteções técnicas | [Revisão](docs/review.md) | [Technical review](docs/en/review.md) |
| Conferir origem e licenças | [Componentes externos](THIRD_PARTY_NOTICES.md) | [Third-party notices](docs/en/third-party-notices.md) |

## Como funciona

```text
Echo → skill TCL → nuvem TCL → TTVSCore na ponte → Binder → SmartAlexa → TV
```

O TTVSCore nativo registra a TV, gera o código, confirma o vínculo e mantém autenticação e conexão. O SmartAlexa instalado executa os controles. A ponte prepara uma cópia privada do runtime TCL, adapta armazenamento e compatibilidade e mantém perfil/tokens no sandbox do app. Não exporta a chave TCL nem modifica o firmware ou os APKs de sistema. Não usa acessibilidade ou `INJECT_EVENTS`.

O serviço retoma após reinício e recupera conexão/executor. O despertar por rede é uma adaptação da ponte. Detalhes e limites estão no [guia de arquitetura](docs/architecture.md).

## Compatibilidade e testes

Validado em **TCL C6K/G15 BR, Android 14, firmware V8-0016T01-LF1V048, TTVSCore 10.58.16 e SmartAlexa 6.13.25**. Outras TVs, regiões e firmwares ainda precisam de teste; interfaces internas TCL podem mudar.

O APK final passou por atualização preservando vínculo, renovação, pausa/retomada, recuperação de conexão/executor, volume real, boot sem abrir a interface e standby de 60 segundos com despertar UDP. Volume, desligar e ligar pelo Echo foram confirmados em teste anterior; a rodada final da 0.4.0 foi automatizada.

Não há comprovação de paridade de 100%, uma noite inteira em standby, confirmação pelo celular numa conta Alexa inteiramente nova, instalador com SmartAlexa ausente ou restauração de energia pela interface nesta rodada. ACK do executor não garante efeito em todo player, canal ou periférico. Consulte o [relatório completo](docs/qa-v0.4.0.md).

## Downloads e contribuição

A [release v0.4.0](https://github.com/eventanilha82/tcl-alexa-bridge/releases/tag/v0.4.0) contém o APK da ponte e o [SmartAlexa 6.13.25 original](https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/SmartAlexa-6.13.25.apk) como arquivos separados. O assistente pode baixar o SmartAlexa; TTVSCore deve existir na TV.

Para compilar, siga o [guia do zero](docs/build.md). O projeto usa Java/Android, sem Gradle obrigatório. Código próprio sob MIT; adblib modificado sob BSD; componentes TCL são proprietários. Veja [origem e licenças](THIRD_PARTY_NOTICES.md).

Ao abrir uma issue, informe modelo, região, firmware, versões dos componentes e o efeito observado. **Não publique tokens, IDs, MACs, códigos de ativação, perfis, keystores ou logs TCL brutos.** O [guia de validação](docs/validation.md) explica como testar e gerar um diagnóstico resumido.
