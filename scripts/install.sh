#!/usr/bin/env bash
set -euo pipefail
BRIDGE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRIDGE_SERIAL="${1:?Uso: install.sh IP_DA_TV:5555}"
[[ $# -eq 1 ]] || { echo 'Uso: install.sh IP_DA_TV:5555' >&2; exit 2; }
[[ -f "$BRIDGE_ROOT/build/alexa-bridge.apk" ]] || { echo 'Compile primeiro: ./scripts/build.sh' >&2; exit 1; }
adb connect "$BRIDGE_SERIAL"
adb -s "$BRIDGE_SERIAL" install --no-incremental -r "$BRIDGE_ROOT/build/alexa-bridge.apk"
adb -s "$BRIDGE_SERIAL" shell am start -n local.tclbridge/.MainActivity
printf '\nContinue no assistente da TV. Não é necessário enviar perfil ou tokens.\n'
