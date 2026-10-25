# Arquitetura e dependências — 0.4.0

```text
Preparação pelo controle remoto
    → autorização Android → ADB em 127.0.0.1:5555
    → identidade TCL e permissões → sessão encerrada

Echo → skill TCL → nuvem TCL
                    ↓
        TTVSCore no processo da ponte
                    ↓ Binder
          SmartAlexa instalado → TV
```

A ponte conduz preparação, vínculo, persistência privada, renovação, conexão e recuperação. O motor TTVSCore realiza registro, código e confirmação pelo protocolo nativo; o SmartAlexa instalado executa controles pelo Binder. Não há acessibilidade, listener de notificações ou concessão de INJECT_EVENTS à ponte.

## Preparação e identidade

O assistente cria uma chave RSA ADB temporária usando adblib modificado. Conecta somente ao loopback da própria TV, após a ação do usuário e confirmação no Android. Executa comandos de preparação definidos pelo app, sem console de comandos exposto. Ao terminar, fecha a conexão e descarta a chave privada; o funcionamento normal não depende de uma sessão ADB permanente.

O perfil vem da própria TV: tipo de cliente, firmware, país, MAC Wi-Fi e identificador TCL. Primeiro é consultada a interface nativa de identificação. Se indisponível, o último recurso lê somente o diagnóstico HAL de UID 0 da tag nativa esperada, exigindo um identificador de 40 dígitos hexadecimais sem ambiguidade. Se não obtiver identidade válida, o fluxo falha. Não gera UUID, hash de Android ID ou identificador de outra TV.

O perfil e tokens ficam no sandbox. Um perfil existente não é sobrescrito silenciosamente por um inbox externo. O APK público contém campos de exemplo vazios. Um vínculo já existente é reconhecido e retomado pelo assistente.

## Runtime TCL privado

O app carrega o pacote de sistema `com.tcl.ttvs` e prepara uma cópia privada do DEX com cache de integridade. `CoreArchive` adapta o caminho fixo de armazenamento para o sandbox da ponte. O applicationId e comprimentos dos caminhos são parte dessa adaptação; renomeá-los exige revisão.

A 0.4.0 também trata uma diferença específica do firmware: a interface TROM nova está presente, mas falta o campo legado `TBuild.VERSION.SDK_INT` esperado pelo motor. Somente nesse caso, `DexSdkCompat` adapta o método `PropertyUtil.isTclWidgetUseNewSdk()` na cópia privada. No DEX real examinado, a alteração afetou 21 bytes; a integridade/checksums DEX foram recalculados e conferidos. O teste exige a assinatura do método esperada e falha em formatos incompatíveis. Não é uma reescrita geral das APIs TCL.

`IsolatedCoreLoader` isola o motor e usa um adaptador local para `sys.showalexa` e `sys.enablealexa`. Os valores dessas duas propriedades existem apenas no runtime da ponte; os flags globais do TTVSCore de sistema não são reconfigurados. Os arquivos e partições da instalação original permanecem intactos.

O registro usa o código/assinador do TTVSCore instalado. A ponte também dispõe de uma chamada verificada a `RSASignature.sign2bytes(byte[])` para operações compatíveis e diagnóstico offline. Nenhuma chave TCL é exportada ou incorporada no APK. O diagnóstico de assinador assina uma mensagem fixa, sem HTTP ou novo registro.

## Controles e energia

O executor recebe as famílias de comandos sem whitelist da ponte. ACK de sucesso demonstra resposta do executor, não efeito no player/canal/periférico. A ponte adapta o despertar por rede/tela.

A opção de standby é voluntária. Na preparação, a sessão ADB ajusta a configuração TCL de espera por rede, que esse firmware exige alterar pelo shell, preservando o valor anterior. `PowerCompatibility` trata a opção global de Low Power Standby com `WRITE_SECURE_SETTINGS`, também preservando o estado anterior. A restauração completa usa novamente a preparação local para a parte de rede.

A integração depende de interfaces internas TCL; atualizações de firmware, DEX, assinador ou Binder podem exigir adaptação. A prova da 0.4.0 cobre coleta nativa e vínculo inicial completo, além de renovação/pausa/retomada, rebind, reconexão e volume no APK final de produção da C6K BR. O reinício recuperou o serviço sem abrir a interface e preservou vínculo, nuvem, executor, persistência e ajustes de energia. O standby automático de 60 segundos manteve nuvem/executor ativos e o despertar UDP funcionou sem abrir a interface. A confirmação de voz da 0.4.0 ainda não foi feita nesta rodada; outros fluxos ainda não exercitados estão identificados no relatório de QA. Não é certificação Works with Alexa ou suporte universal. Consulte [validação](validation.md) e o [relatório atual](qa-v0.4.0.md) para os resultados e limitações.
