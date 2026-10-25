# Pausa, recuperação e restauração

- Pausar na interface mantém o vínculo. Uma renovação em voo precisa terminar antes de iniciar outra sessão, para não perder tokens rotacionados.
- Fechar a tela não equivale a pausar. O serviço e o receptor de boot mantêm o controle.
- Desativar Manter Alexa em espera restaura o valor de energia capturado no opt-in. Também é possível usar o broadcast protegido `local.tclbridge.CONFIGURE_STANDBY` com `enabled=false` a partir do ADB.
- Forçar parada, limpar dados ou desinstalar têm efeitos explícitos do Android. Limpar dados remove o vínculo local; não é um teste de fechamento normal.
- Não desinstale SmartAlexa nem TTVSCore. Não há instrução de flash/root.
- Atualizações precisam da mesma chave de assinatura. Não automatizamos desinstalação se a assinatura divergir, para preservar seus dados.
- Para atualizar, use APK da mesma origem/chave e `adb install --no-incremental -r`. Guarde um APK anterior compatível; Android pode impedir downgrade de versionCode, exigindo um build com código maior e comportamento anterior.
