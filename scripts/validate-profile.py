#!/usr/bin/env python3
"""Validate a private profile without printing its identifying fields."""
import json,re,sys,pathlib
p=pathlib.Path(sys.argv[1]);profile=json.loads(p.read_text())
required=['device_id','mac','client_type','country','software_version']
for field in required:
 value=str(profile.get(field,'')).strip()
 if not value or any(word in value.lower() for word in ['example','your_','<','placeholder']):raise SystemExit('Campo ausente/exemplo: '+field)
if not str(profile['country']).isdigit():raise SystemExit('country precisa do código numérico nativo TCL')
addresses=re.findall(r'(?i)[0-9a-f]{2}(?::[0-9a-f]{2}){5}',profile['mac'])
if not addresses:raise SystemExit('MAC ausente/inválido')
for mac in addresses:
 raw=bytes.fromhex(mac.replace(':',''))
 if raw in [b'\x02'+b'\0'*5,b'\0'*6,b'\xff'*6] or raw[0]&1:raise SystemExit('MAC mascarado/inválido')
if any('token' in k.lower() or 'password' in k.lower() or 'private' in k.lower() for k in profile):raise SystemExit('Não coloque credenciais no perfil')
print('Perfil validado localmente; valores não exibidos.')
