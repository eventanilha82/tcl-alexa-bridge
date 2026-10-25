# Arquitetura e dependências

```text
Echo → skill TCL → nuvem TCL
                    ↓
        TTVSCore no processo da ponte
                    ↓ Binder
          SmartAlexa instalado → TV
```

A ponte mantém vínculo, tokens privados, renovação, conexão, inicialização após boot e recuperação do executor. O SmartAlexa instalado executa os comandos pelo protocolo Binder nativo. TTVSCore fornece o motor e assinador. Não há controle por acessibilidade, listener de notificações ou concessão de INJECT_EVENTS à ponte.

O app carrega código do pacote de sistema `com.tcl.ttvs`. Para o motor, cria uma cópia privada do DEX e altera somente o caminho fixo de armazenamento, atualizando sua integridade. O caminho aponta para o sandbox de `local.tclbridge`; a instalação TCL e o firmware permanecem intactos. O applicationId e os comprimentos dos caminhos são parte dessa adaptação; renomeá-los exige revisar o mapeamento.

O registro serializa o perfil e chama `RSASignature.sign2bytes(byte[])` do TTVSCore de sistema instalado. O resultado é verificado pelo método nativo correspondente. A ponte não exporta a chave e falha claramente quando o pacote/interface não é compatível. O diagnóstico de assinador usa uma mensagem fixa, sem HTTP nem novo registro.

O perfil identifica uma TV, não uma conta genérica. É validado e persistido no armazenamento privado; um perfil existente não é sobrescrito silenciosamente pelo inbox. O APK público contém apenas campos vazios. Tokens são obtidos no vínculo e permanecem privados. Não existe um perfil US universal nem identidade reutilizável para todas as TVs.

O executor oficial recebe as famílias de comandos sem whitelist da ponte. ACK de sucesso indica resposta do executor, não prova de efeito no player/canal/periférico. A ponte implementa também despertar por rede/tela; a compatibilidade de Low Power Standby exige opt-in porque muda a política global de energia. Consulte os limites em [validação](validation.md).

A integração usa interfaces internas e comportamentos específicos da TCL. Não é uma API pública estável, certificação Works with Alexa ou suporte universal. Mudanças de firmware, assinador, layout DEX ou Binder podem exigir adaptação. A cobertura comprovada é a C6K brasileira e as versões documentadas.
