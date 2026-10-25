#!/usr/bin/env bash
set -euo pipefail
BRIDGE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRIDGE_SERIAL="${1:?Uso: install.sh IP:5555 caminho/do/perfil-privado.json [--standby-compat]}"
BRIDGE_PROFILE="${2:?Informe seu perfil privado}"
BRIDGE_STANDBY="${3:-}"
if [[ $# -gt 3 || ( -n "$BRIDGE_STANDBY" && "$BRIDGE_STANDBY" != '--standby-compat' ) ]]; then
 echo 'Uso: install.sh IP:5555 caminho/do/perfil-privado.json [--standby-compat]' >&2
 exit 2
fi
python3 "$BRIDGE_ROOT/scripts/validate-profile.py" "$BRIDGE_PROFILE"
adb connect "$BRIDGE_SERIAL"
adb -s "$BRIDGE_SERIAL" install --no-incremental -r "$BRIDGE_ROOT/build/alexa-bridge.apk"
for BRIDGE_OP in AUTO_START ASSOC_START; do
 adb -s "$BRIDGE_SERIAL" shell cmd appops set local.tclbridge "$BRIDGE_OP" allow
 adb -s "$BRIDGE_SERIAL" shell cmd appops set --uid local.tclbridge "$BRIDGE_OP" allow
done
adb -s "$BRIDGE_SERIAL" shell cmd appops set local.tclbridge RUN_ANY_IN_BACKGROUND allow
adb -s "$BRIDGE_SERIAL" shell cmd appops set local.tclbridge SYSTEM_ALERT_WINDOW allow
adb -s "$BRIDGE_SERIAL" shell cmd appops set local.tclbridge TURN_SCREEN_ON allow
adb -s "$BRIDGE_SERIAL" shell pm grant local.tclbridge android.permission.POST_NOTIFICATIONS
adb -s "$BRIDGE_SERIAL" shell cmd deviceidle whitelist +local.tclbridge
adb -s "$BRIDGE_SERIAL" shell cmd appops write-settings
if [[ "$BRIDGE_STANDBY" == '--standby-compat' ]]; then
 echo 'Compatibilidade de standby solicitada: pode aumentar o consumo em espera.'
 adb -s "$BRIDGE_SERIAL" shell pm grant --user 0 local.tclbridge android.permission.WRITE_SECURE_SETTINGS
 adb -s "$BRIDGE_SERIAL" shell settings put system tcl_sys_network_standby_enabled 1
 adb -s "$BRIDGE_SERIAL" shell am broadcast -n local.tclbridge/.BootReceiver -a local.tclbridge.CONFIGURE_STANDBY --ez enabled true
fi
adb -s "$BRIDGE_SERIAL" shell am start -n local.tclbridge/.MainActivity
adb -s "$BRIDGE_SERIAL" shell mkdir -p /sdcard/Android/data/local.tclbridge/files
BRIDGE_STATE=$(adb -s "$BRIDGE_SERIAL" shell am broadcast -n local.tclbridge/.BootReceiver -a local.tclbridge.DIAG)
if [[ "$BRIDGE_STATE" != *"profile_private=true"* ]]; then
 adb -s "$BRIDGE_SERIAL" push "$BRIDGE_PROFILE" /sdcard/Android/data/local.tclbridge/files/device-profile.json
else
 echo "Perfil privado existente preservado; nenhum perfil foi enviado."
fi
printf '\nNa TV, selecione Iniciar/Vincular conta. O perfil é importado ao armazenamento privado. Não compartilhe tokens ou identificadores em issues.\n'
