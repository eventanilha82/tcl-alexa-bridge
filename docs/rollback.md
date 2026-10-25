# Pausa, recuperação e restauração

**Português (Brasil)** | [English](en/rollback.md) · [Início](../README.md)

## Vínculo e atualização

- Pausar na interface mantém o vínculo. Uma renovação em voo precisa terminar antes de iniciar outra sessão, para não perder tokens rotacionados.
- Fechar a tela não equivale a pausar. O serviço e o receptor de boot implementam a continuidade; o reinício da versão atual recuperou o serviço sem abrir a interface. O ciclo automático de standby de 60 segundos e o despertar UDP passaram. A confirmação de voz nesta versão ainda não foi feita nesta rodada.
- Forçar parada, limpar dados ou desinstalar têm efeitos explícitos do Android. Limpar dados remove perfil, vínculo e os valores salvos para restauração; não é um teste de fechamento normal.
- Não desinstale SmartAlexa nem TTVSCore. O projeto não exige flash/root.
- Atualizações exigem a mesma chave de assinatura. Não desinstalamos automaticamente para contornar assinaturas diferentes. Preserve a assinatura e os dados ao atualizar para a 0.4.0; o assistente reconhece o vínculo existente nesta instalação.
- Um build pessoal ou APK de laboratório pode ter outra assinatura/pacote. Seus dados não são automaticamente os da instalação de produção. Nunca publique o APK temporário com `debuggable=true`.

## Restaurar energia

Abra **Configurar esta TV** no painel, refaça a autorização local caso solicitada, desmarque **Permitir ligar a TV por Alexa em espera** e conclua **Autorizar e preparar**. A preparação restaura tanto o valor original da espera por rede TCL quanto o de Low Power Standby. A parte de rede exige a sessão ADB local no firmware testado.

A configuração **Manter Alexa em espera** trata Low Power Standby; desativá-la isoladamente não substitui a restauração da configuração TCL de rede pela preparação. Não limpe os dados antes de restaurar: os valores anteriores são guardados no armazenamento privado. Após concluir, desative a depuração nas configurações da TV se não precisar dela.

Encerrar a sessão e descartar a chave privada não revoga automaticamente a permissão `WRITE_SECURE_SETTINGS` concedida à ponte. Usuários avançados podem revogá-la pelo ADB depois de restaurar a energia:

```sh
adb -s IP_DA_TV:5555 shell pm revoke local.tclbridge android.permission.WRITE_SECURE_SETTINGS
```

## Reverter uma atualização

Guarde o APK anterior da mesma origem/chave e consulte o resultado de `adb install --no-incremental -r`. Android pode impedir downgrade de versionCode; nesse caso, uma reversão preservando dados requer build compatível com versionCode maior e comportamento anterior. Não use desinstalação como passo automático. A compatibilidade dos dados precisa ser avaliada ao reverter da 0.4.0.

Confira o [relatório da versão atual](qa-v0.4.0.md) antes de atribuir a ela resultados de testes anteriores.
