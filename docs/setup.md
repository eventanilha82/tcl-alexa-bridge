# Instalação e vínculo

A combinação validada é TCL C6K/G15 BR, Android 14, firmware V8-0016T01-LF1V048, TTVSCore 10.58.16 e SmartAlexa 6.13.25. O projeto depende dos dois pacotes TCL no aparelho. Não instala firmware e não transforma uma TV sem TTVSCore em um modelo compatível.

## Baixar com Downloader na TV

No aplicativo Downloader de sua preferência, digite cada URL completa abaixo. Autorize a instalação de apps dessa fonte nas configurações do Android TV quando solicitado. Instale primeiro o SmartAlexa e depois a ponte:

- SmartAlexa original: <https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.3.4/SmartAlexa-6.13.25.apk>
- TCL Alexa Bridge: <https://github.com/eventanilha82/tcl-alexa-bridge/releases/download/v0.3.4/TCL-Alexa-Bridge-0.3.4.apk>

Esses endereços correspondem aos assets da release `v0.3.4`; só respondem depois que a release tiver sido publicada. Se houver erro de assinatura ao atualizar, pare e confira a versão/origem instalada. Não desinstale automaticamente: a desinstalação pode apagar dados e vínculo. O SmartAlexa continua necessário mesmo que sua tela diga que o país não é suportado; a ponte mantém a sessão e usa o executor dele.

O download dos APKs não conclui a configuração. Esta versão ainda exige computador com ADB e um perfil manual real da sua TV. Um perfil vazio no APK público evita cadastrar todos os usuários como a mesma TV.

## Preparar o computador e a TV

Instale Android SDK Platform Tools pelos canais oficiais do Android. Na TV, habilite Opções do desenvolvedor e a depuração de rede/ADB disponível no firmware. Descubra o IP da TV nas configurações de rede e mantenha computador e TV na mesma rede. Execute:

```sh
adb connect IP_DA_TV:5555
adb -s IP_DA_TV:5555 get-state
```

Aceite na TV a chave do seu computador. `get-state` deve retornar `device`. Se o firmware usar emparelhamento/porta diferentes, use os valores mostrados pela própria TV. Os comandos deste guia usam o transporte TCP 5555 validado na C6K.

Confira os pacotes, sem publicar dumps completos:

```sh
adb -s IP_DA_TV:5555 shell pm path com.tcl.ttvs
adb -s IP_DA_TV:5555 shell pm path com.tcl.smartalexa
```

TTVSCore deve vir da instalação de sistema compatível. Não há APK de TTVSCore ou firmware no projeto. Para instalar os APKs pelo computador em vez do Downloader:

```sh
adb -s IP_DA_TV:5555 install --no-incremental -r SmartAlexa-6.13.25.apk
adb -s IP_DA_TV:5555 install --no-incremental -r TCL-Alexa-Bridge-0.3.4.apk
```

## Montar o perfil privado

Na cópia local deste repositório:

```sh
mkdir -p .private
chmod 700 .private
cp config/device-profile.example.json .private/device-profile.local.json
```

Edite o arquivo usando dados obtidos da própria TV. O coletor automático de identidade ainda não foi validado nesta versão. As informações visíveis de modelo, firmware e MAC ajudam, mas não garantem o identificador interno TCL. Se você não dispõe desse identificador/perfil nativo, não invente um valor: essa etapa permanece um requisito de configuração avançada.

| Campo | Conteúdo esperado |
| --- | --- |
| `device_id` | Identificador nativo TCL da própria TV; não substitua por Android ID, serial ou UUID aleatório. |
| `mac` | MAC real e utilizável da interface da TV, por exemplo no formato nativo `wireless=AA:BB:CC:DD:EE:FF`; o exemplo é ilustrativo. |
| `client_type` | Tipo de cliente TCL exato do aparelho, começando com `TCL-`; não copie o de outra plataforma. |
| `country` | Código numérico usado pela TCL. `52` foi validado na instalação brasileira testada; não é código ISO e não deve ser trocado para US como tentativa genérica. |
| `software_version` | Versão real do firmware da TV de destino, não a versão do firmware doador do SmartAlexa. |
| `tv_name` | Nome amigável desejado; o app pode usar o nome atual das configurações da TV. |
| `netWork_standby` | `on` ou `off`; o exemplo usa `on`. Não concede sozinho permissões de energia. |
| `city`, `dev_desp` | Campos opcionais; podem ficar vazios. |

Não coloque access tokens, refresh tokens, senhas ou chaves de assinatura nesse arquivo. Não use o MAC mascarado `02:00:00:00:00:00`. Valide localmente sem exibir os valores:

```sh
python3 scripts/validate-profile.py .private/device-profile.local.json
```

## Autorizar funcionamento em segundo plano e importar

Os appops TCL abaixo são específicos do firmware testado. Se algum não existir, registre modelo/firmware e a mensagem do comando antes de prosseguir; não presuma que a proteção esteja configurada.

```sh
adb -s IP_DA_TV:5555 shell cmd appops set local.tclbridge AUTO_START allow
adb -s IP_DA_TV:5555 shell cmd appops set --uid local.tclbridge AUTO_START allow
adb -s IP_DA_TV:5555 shell cmd appops set local.tclbridge ASSOC_START allow
adb -s IP_DA_TV:5555 shell cmd appops set --uid local.tclbridge ASSOC_START allow
adb -s IP_DA_TV:5555 shell cmd appops set local.tclbridge RUN_ANY_IN_BACKGROUND allow
adb -s IP_DA_TV:5555 shell cmd appops set local.tclbridge SYSTEM_ALERT_WINDOW allow
adb -s IP_DA_TV:5555 shell cmd appops set local.tclbridge TURN_SCREEN_ON allow
adb -s IP_DA_TV:5555 shell pm grant local.tclbridge android.permission.POST_NOTIFICATIONS
adb -s IP_DA_TV:5555 shell cmd deviceidle whitelist +local.tclbridge
adb -s IP_DA_TV:5555 shell cmd appops write-settings
adb -s IP_DA_TV:5555 shell am start -n local.tclbridge/.MainActivity
adb -s IP_DA_TV:5555 shell mkdir -p /sdcard/Android/data/local.tclbridge/files
adb -s IP_DA_TV:5555 push .private/device-profile.local.json /sdcard/Android/data/local.tclbridge/files/device-profile.json
```

Na primeira configuração, selecione Iniciar/Vincular conta na ponte. O arquivo é validado, copiado para o armazenamento privado e removido do inbox externo. O perfil privado existente tem preferência; reenviar um arquivo não altera silenciosamente a identidade de uma instalação já vinculada. Não limpe os dados para tentar atualizar o perfil: isso remove o vínculo local.

Para builds próprios, `scripts/install.sh IP_DA_TV:5555 .private/device-profile.local.json` automatiza a instalação do APK de `build/` e as permissões. O script preserva um perfil privado existente. Ele não baixa nem instala o SmartAlexa.

O assinador instalado pode ser verificado sem novo registro (com o serviço em execução):

```sh
adb -s IP_DA_TV:5555 shell dumpsys activity service local.tclbridge/.BridgeService test-native-signer
```

O resultado esperado informa disponibilidade e verificação da assinatura; não mostra chaves ou identidade. Uma falha de interface/pacote deve ser resolvida antes do vínculo.

## Vincular à Alexa

Com a ponte iniciada, use o código de ativação mostrado na interface no fluxo da skill TCL/TCL Assistant da sua conta Alexa. O nome/disponibilidade da skill depende da conta e região; use o fluxo disponível na sua conta. Aguarde a conclusão do vínculo e teste com o nome exato da TV cadastrada: volume, mudo, entrada e desligar. Fechar a interface deve manter o serviço; forçar parada pelo Android é diferente de fechar a tela.

## Despertar durante standby

A C6K testada exigiu compatibilidade de energia para manter rede e CPU em espera. A opção é voluntária e pode aumentar o consumo. Conceda a permissão ampla `WRITE_SECURE_SETTINGS` apenas se aceitar esse ajuste:

```sh
adb -s IP_DA_TV:5555 shell pm grant --user 0 local.tclbridge android.permission.WRITE_SECURE_SETTINGS
adb -s IP_DA_TV:5555 shell settings put system tcl_sys_network_standby_enabled 1
adb -s IP_DA_TV:5555 shell am broadcast -n local.tclbridge/.BootReceiver -a local.tclbridge.CONFIGURE_STANDBY --ez enabled true
```

O app usa essa permissão para o ajuste de Low Power Standby, preservando o valor anterior dessa opção. A configuração TCL `tcl_sys_network_standby_enabled` acima é separada: anote seu valor anterior com `adb ... shell settings get system tcl_sys_network_standby_enabled` antes de alterá-la caso queira restaurá-la depois. Em builds próprios, a opção `--standby-compat` do instalador executa essa preparação.

Teste desligar, esperar um minuto e ligar por voz sem abrir a ponte. Depois valide uma noite em espera e reinicialização. Consulte [validação](validation.md), [diagnóstico](../README.md#diagnóstico-e-contribuição) e [restauração](rollback.md). Proteja os computadores autorizados no ADB e desative a depuração quando não precisar dela.
