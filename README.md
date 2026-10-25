# TCL Alexa Bridge

Ponte comunitária para manter o vínculo Alexa em uma TCL C6K brasileira, usando o TTVSCore e o SmartAlexa **já instalados na TV**. O aplicativo cuida de vínculo, sessão, persistência, inicialização, recuperação e despertar. O SmartAlexa executa os controles. Não é um substituto independente nem uma certificação Works with Alexa.

**Versão 0.4.0:** configure pelo controle remoto. O assistente instala o SmartAlexa original quando necessário, pede uma autorização local de depuração do Android, identifica esta TV e gera o código de vínculo. No celular, conclua pela skill TCL Assistant no app Alexa. Não é necessário preparar um perfil JSON num computador no fluxo validado.

## Compatibilidade comprovada

TCL C6K/G15 brasileira, Android 14 (API 34), firmware V8-0016T01-LF1V048, TTVSCore 10.58.16 e SmartAlexa 6.13.25. Outras combinações precisam de validação. O código carrega interfaces internas da TCL: uma atualização pode exigir adaptação. O `applicationId` é fixo em `local.tclbridge` porque o mapeamento do armazenamento nativo depende do comprimento do caminho.

## Download e instalação

Na release [v0.4.0](https://github.com/eventanilha82/tcl-alexa-bridge/releases/tag/v0.4.0), os APKs são assets separados:

- [SmartAlexa 6.13.25 original](https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/SmartAlexa-6.13.25.apk) — proprietário TCL; veja [proveniência e licença](THIRD_PARTY_NOTICES.md).
- [TCL Alexa Bridge 0.4.0](https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.4.0/TCL-Alexa-Bridge-0.4.0.apk) — código próprio MIT.

O [guia completo](docs/setup.md) explica Downloader, preparação local pelo controle remoto, permissões e vínculo. O assistente pode baixar o SmartAlexa diretamente; não é preciso instalar os dois APKs manualmente.

## Arquitetura

Echo → skill TCL → nuvem TCL → TTVSCore no processo da ponte → Binder → SmartAlexa → TV.

O app usa uma cópia privada do DEX do TTVSCore, adapta o armazenamento e, quando necessário, uma verificação específica da API TCL. As duas propriedades de disponibilidade Alexa são isoladas no processo da ponte. Integridade e checksums DEX são verificados; a instalação original e o firmware permanecem intactos. O fluxo nativo registra e vincula a TV sem incorporar/exportar a chave TCL. A implementação não usa acessibilidade ou listener de notificações.

Veja o [desenho da integração](docs/architecture.md).

## Compilar seu próprio APK

O [guia de build do zero](docs/build.md) explica instalar Git/JDK/Python e Android SDK, clonar o repositório, compilar, verificar e instalar. Depois de preparar essas ferramentas:

```sh
git clone https://github.com/eventanilha82/tcl-alexa-bridge.git
cd tcl-alexa-bridge
python3 scripts/check-shareable.py
./scripts/build.sh
```

O APK é gerado em `build/alexa-bridge.apk`. A chave pessoal fica em `.private/`; preserve-a para atualizar suas instalações e nunca a publique. Um build pessoal não tem automaticamente a mesma assinatura da release. O APK público não contém identidade ou tokens de uma TV.

## Configuração na TV

1. Abra **Alexa na TV** e siga a verificação dos componentes.
2. Habilite as opções de desenvolvedor/depuração indicadas e volte ao assistente.
3. Selecione **Autorizar e preparar** e confirme a mensagem do Android. A sessão ADB conecta apenas à própria TV; sua chave privada é temporária.
4. Selecione **Gerar meu código** e conclua no celular pela skill TCL Assistant. Um vínculo existente nesta instalação é reconhecido.
5. Aguarde conta, conexão e executor prontos antes de concluir.

A coleta usa a identidade TCL real da própria TV. O APK público não contém perfis individuais ou tokens. Após preparar, a conexão ADB é encerrada; a depuração pode ser desativada. Consulte o [guia da 0.4.0](docs/setup.md).

## Ligar a TV em standby

A opção **Permitir ligar a TV por Alexa em espera** é voluntária e pode aumentar o consumo. A preparação concede `WRITE_SECURE_SETTINGS` e ajusta Low Power Standby e espera por rede. O estado anterior é salvo; restaure pela configuração da TV no assistente conforme [recuperação](docs/rollback.md). A permissão é ampla, e o código da ponte a utiliza para esse ajuste específico. AUTO_START e ASSOC_START são operações TCL de início automático, não INJECT_EVENTS.

## Evidência da 0.4.0

O APK final foi atualizado preservando vínculo e passou por renovação nativa, três ciclos de renovação/pausa/retomada, recuperação do executor e socket, e dois comandos de volume com efeito real e ACK. O build público gerou DEX/manifest iguais aos conferidos; assinatura, produção sem depuração e licença BSD incluída foram verificadas.

A primeira configuração também passou em instalação isolada: identidade TCL real coletada sem arquivo importado, registro sem credenciais prévias, código, confirmação, renovação e conexão. O reinício recuperou serviço, vínculo, nuvem, executor e persistência sem abrir a interface, mantendo os ajustes de energia. O teste automático de 60 segundos em standby manteve nuvem/executor ativos e despertou por UDP sem abrir a interface. Voz pelo Echo na 0.4.0 ainda não foi confirmada nesta rodada; instalador com SmartAlexa ausente, confirmação de uma conta Alexa nova, restauração de energia pela interface e uma noite em espera são limites de QA ainda não exercitados. Veja o [relatório atual](docs/qa-v0.4.0.md).

## Cobertura e limites

As mensagens são encaminhadas sem uma whitelist de famílias: pesquisa, canais, entradas, energia, volume, apps, reprodução, mudo, navegação, seek e PVR chegam ao executor oficial quando disponíveis. Retornos e bytes permanecem nativos. A ponte adapta o ligar por rede/tela, pois o executor SmartAlexa analisado trata o desligamento.

Métricas contam **encaminhados ao executor**, não todos os recebidos da nuvem. A biblioteca nativa pode descartar uma mensagem durante `BINDING`; não fazemos reenvio automático que possa duplicar ações. Retorno `code=0` confirma resposta do executor, não necessariamente efeito em conteúdo/periférico. Canais exigem sintonia; PVR e playlists dependem de recursos reais; na variante brasileira analisada, o caminho de gravação não demonstrou implementação efetiva. Aplicativos podem anunciar controles diferentes.

Veja o [relatório atual](docs/qa-v0.4.0.md), os [limites de validação](docs/validation.md), a [revisão técnica](docs/review.md) e a [recuperação](docs/rollback.md).

## Diagnóstico e contribuição

```sh
mkdir -p .private
./scripts/diagnostics.sh IP_DA_TV:5555 > .private/diagnostics.txt
```

Esse resumo evita o logcat TCL bruto, que pode conter dados de autenticação. Não envie tokens, IDs, MACs, perfis privados, keystores ou dumps nativos completos em issues. Descreva modelo, firmware, versões dos pacotes e comandos/frases usados. Um relatório de reprodução deve distinguir chamada de voz, injeção local na ponte, retorno do executor e efeito observado.

O fonte não inclui firmware ou APKs TCL. A release disponibiliza o SmartAlexa proprietário como asset separado, sem cobertura MIT; consulte [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) para origem e hashes. Código próprio sob MIT; adblib modificado sob BSD; componentes TCL mantêm seus próprios direitos e termos.
