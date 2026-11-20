#!/usr/bin/env python3
"""Check the actual local TCL APK contracts without executing or redistributing vendor code."""
import json,struct,zipfile,sys,hashlib
from pathlib import Path
BASE='Lcom/tcl/ttvs/alexa/'
FIELDS={
 'Lcom/tcl/ff/component/utils/common/Utils;':{'sApp':'Landroid/app/Application;'},
 BASE+'core/AlexaManager;':{
  'sInstance':BASE+'core/AlexaManager;','mFileInfo':BASE+'core/device/FileInfo;',
  'mHeartRunnable':'Ljava/lang/Runnable;','mHandler':BASE+'core/AlexaManager$MsgHandler;',
  'isDebug':'I','isDiscovered':'Z','isNeedQuery':'Z','isGetActiveCode':'Z','isGetTokenPower':'Z',
  'mDisposable':'Lio/reactivex/disposables/Disposable;',
  'mWebSocketClient':BASE+'core/CustomWebClient;','udpClient':BASE+'core/service/UdpClient;'},
 BASE+'core/device/DeviceInfo;':dict.fromkeys(['city','client_type','dev_desp','device_id','mac','netWork_standby','software_version','tv_name'],'Ljava/lang/String;'),
 BASE+'core/device/FileInfo;':dict.fromkeys(['server_host','server_domain','software_version','uuid','tcl_token','tcl_refreshtoken','wwa_status','avgs_login'],'Ljava/lang/String;'),
 BASE+'core/WebSocketManager;':{'mInvokeHandler':BASE+'IAlexaInvoke;','mServiceConnection':'Landroid/content/ServiceConnection;','mIsBind':'Z','mBindAlexaServiceState':'Ljava/lang/Enum;'},
 BASE+'core/WebSocketManager$BindAlexaServiceState;':{'NOT_BIND':BASE+'core/WebSocketManager$BindAlexaServiceState;'}
}
FIELDS[BASE+'core/device/DeviceInfo;']['country']='I'
FIELDS[BASE+'core/device/FileInfo;'].update({'tcl_token_expires_at':'J','device':BASE+'core/device/DeviceInfo;'})
METHODS={
 BASE+'core/AlexaManager;':{'getActiveCodeImpl':'()V','getAVGStateFromServer':'()I','initSocketClient':'()V','refreshTclTokenFromServer':'(ILjava/lang/String;)I','setPowerStatus':'(Ljava/lang/String;)V'},
 BASE+'core/WebSocketManager;':{'getInstance':'(Landroid/content/Context;)'+BASE+'core/WebSocketManager;','bindRemoteService':'(ILjava/lang/String;)V','unBindRemoteService':'()I'},
 BASE+'IAlexaInvoke;':{'handleActionInvoke':'(ILjava/lang/String;I'+BASE+'IResultCallback;)V'},
 BASE+'IResultCallback;':{'resultCall':'(ILjava/lang/String;I)V'}
}
CONSTRUCTORS={BASE+'core/AlexaManager;':'(Landroid/content/Context;)V',BASE+'core/device/DeviceInfo;':'()V',BASE+'core/device/FileInfo;':'()V',BASE+'core/AlexaManager$MsgHandler;':'(Ljava/lang/ref/WeakReference;)V'}
TARGETS=set(FIELDS)|set(METHODS)|set(CONSTRUCTORS)
def inspect(path):
 result={}
 with zipfile.ZipFile(path) as z:
  for entry in z.namelist():
   if not (entry.startswith('classes') and entry.endswith('.dex')):continue
   d=z.read(entry)
   if len(d)<112 or d[:4]!=b'dex\n' or d[4:7] not in (b'035',b'037',b'038',b'039',b'040'):
    raise ValueError('Unsupported DEX header in '+entry)
   def u4(p):return struct.unpack_from('<I',d,p)[0]
   def u2(p):return struct.unpack_from('<H',d,p)[0]
   def leb(p):
    n=s=0
    while True:
     v=d[p];p+=1;n|=(v&127)<<s
     if v<128:return n,p
     s+=7
   so,to,fo,mo,co=[u4(p) for p in (60,68,84,92,100)]
   def string(i):
    _,p=leb(u4(so+i*4));end=d.index(0,p);return d[p:end].decode('utf8','replace')
   def typ(i):return string(u4(to+i*4))
   def proto(i):
    p=u4(76)+i*12;args=u4(p+8)
    params='' if not args else ''.join(typ(u2(args+4+j*2)) for j in range(u4(args)))
    return '('+params+')'+typ(u4(p+4))
   for k in range(u4(96)):
    p=co+k*32;name=typ(u4(p))
    if name not in TARGETS:continue
    q=u4(p+24)
    if not q:continue
    counts=[]
    for _ in range(4):n,q=leb(q);counts.append(n)
    out={'dex':entry,'class_access':u4(p+4),'fields':{},'methods':{}}
    for count,kind in zip(counts,('fields','fields','methods','methods')):
     idx=0
     for _ in range(count):
      delta,q=leb(q);idx+=delta;access,q=leb(q)
      if kind=='fields':
       base=fo+idx*8;member=string(u4(base+4));info={'access':access,'public':bool(access&1),'private':bool(access&2),'type':typ(u2(base+2))}
      else:
       _,q=leb(q);base=mo+idx*8;member=string(u4(base+4));info={'access':access,'public':bool(access&1),'private':bool(access&2),'descriptor':proto(u2(base+2))}
      out[kind].setdefault(member,[]).append(info)
    result[name]=out
 return result
def check(path):
 raw=inspect(path);checks=0;legacy_blockers=[]
 for cls,fields in FIELDS.items():
  for name,expected in fields.items():
   rows=raw.get(cls,{}).get('fields',{}).get(name,[])
   if len(rows)!=1 or rows[0]['type']!=expected:raise AssertionError(cls+'.'+name+': absent or changed type')
   checks+=1
   if not rows[0]['public']:legacy_blockers.append(cls+'.'+name)
 for cls,methods in METHODS.items():
  for name,descriptor in methods.items():
   rows=raw.get(cls,{}).get('methods',{}).get(name,[])
   if not any(r['public'] and r['descriptor']==descriptor for r in rows):raise AssertionError(cls+'.'+name+': public method contract changed')
   checks+=1
 for cls,descriptor in CONSTRUCTORS.items():
  rows=raw.get(cls,{}).get('methods',{}).get('<init>',[])
  matching=[r for r in rows if r['descriptor']==descriptor]
  if len(matching)!=1:raise AssertionError(cls+': constructor signature changed')
  checks+=1
  if not matching[0]['public']:legacy_blockers.append(cls+'.<init>')
 return {'apk':Path(path).name,'sha256':hashlib.sha256(Path(path).read_bytes()).hexdigest(),'checks':checks,'passed':True,'legacy_public_lookup_blockers':len(legacy_blockers)}
if __name__=='__main__':
 if len(sys.argv)<2:raise SystemExit('Usage: test-core-contract.py local-core.apk [more-core.apk ...]')
 for path in sys.argv[1:]:print(json.dumps(check(path)))
