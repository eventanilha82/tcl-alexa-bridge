#!/usr/bin/env bash
set -euo pipefail
BRIDGE_SERIAL="${1:?Uso: diagnostics.sh IP:5555}"
echo 'Resumo da ponte; revise antes de publicar.'
adb -s "$BRIDGE_SERIAL" shell dumpsys activity service local.tclbridge/.BridgeService
for BRIDGE_PACKAGE in local.tclbridge com.tcl.ttvs com.tcl.smartalexa; do
 echo "Package: $BRIDGE_PACKAGE"
 adb -s "$BRIDGE_SERIAL" shell dumpsys package "$BRIDGE_PACKAGE" | sed -n '/versionName=/p'
done
adb -s "$BRIDGE_SERIAL" shell am broadcast -n local.tclbridge/.BootReceiver -a local.tclbridge.DIAG
