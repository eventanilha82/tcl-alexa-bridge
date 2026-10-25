#!/usr/bin/env python3
"""Check the source bundle, excluding only local build/private/git directories."""
import pathlib,re,json,sys
root=pathlib.Path(__file__).resolve().parents[1];errors=[];count=0
for p in root.rglob('*'):
 if not p.is_file() or any(part in ['.git','.private','build','__pycache__'] for part in p.relative_to(root).parts):continue
 count+=1;rel=str(p.relative_to(root))
 if p.suffix.lower() in ['.apk','.dex','.jar','.p12','.jks','.keystore','.pem','.key','.zip']:
  errors.append(rel+': artefato binário/vendor/assinatura não deve ir no pacote fonte');continue
 text=p.read_text(errors='replace')
 if p.resolve()!=pathlib.Path(__file__).resolve() and ('/Users/' in text or '/home/' in text):errors.append(rel+': caminho pessoal absoluto')
 if p.suffix=='.java' and ('ClientSigningMaterial' in text or 'PKCS8EncodedKeySpec' in text):errors.append(rel+': material de assinatura incorporado')
 if re.search(r'-----BEGIN (?:RSA )?PRIVATE KEY-----',text):errors.append(rel+': chave privada')
 if re.search(r'eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}',text):errors.append(rel+': possível JWT')
 if p.name=='device-profile.json':
  try:
   profile=json.loads(text)
   if any(profile.get(k) for k in ['device_id','mac','client_type','software_version','tv_name']):errors.append(rel+': perfil individual no pacote')
  except ValueError:errors.append(rel+': JSON de perfil inválido')
if errors:print('\n'.join(errors),file=sys.stderr);raise SystemExit(1)
print(f'Pacote fonte: {count} arquivos verificados; sem os padrões proibidos. Faça revisão humana antes de publicar.')
