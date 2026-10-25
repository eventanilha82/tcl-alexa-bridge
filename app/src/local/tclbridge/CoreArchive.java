package local.tclbridge;
import android.content.Context;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.*;

/** Private runtime copy: storage mapping and a narrowly scoped legacy SDK metadata adapter. */
final class CoreArchive {
  static final String SOURCE="/data/data/com.tcl.ttvs/files/alexa/";
  static final String DESTINATION="/data/data/local.tclbridge/files/aa/";
  static String lastSource;static File prepared;
  static synchronized File prepare(Context context,String apk)throws Exception{
    if(SOURCE.length()!=DESTINATION.length())throw new IllegalStateException("Storage mapping length mismatch");
    boolean sdkShim=false;
    try{java.lang.reflect.Field sdk=Class.forName("tcl.os.TBuild$VERSION",false,context.getClassLoader()).getField("SDK_INT");sdkShim=sdk.getType()!=int.class||!java.lang.reflect.Modifier.isStatic(sdk.getModifiers());}catch(ReflectiveOperationException|LinkageError e){sdkShim=true;}
    if(sdkShim)try{Class.forName("com.tcl.os.ITclDeviceInfoManager",false,context.getClassLoader());}catch(ClassNotFoundException e){throw new IOException("TCL SDK metadata unavailable on this firmware");}
    File original=new File(apk);String sourceKey=apk+":"+original.length()+":"+original.lastModified()+":"+sdkShim;
    if(sourceKey.equals(lastSource)&&prepared!=null&&prepared.isFile()&&!prepared.canWrite())return prepared;
    MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream input=new FileInputStream(original)){byte[] buffer=new byte[65536];int n;while((n=input.read(buffer))!=-1)digest.update(buffer,0,n);}
    StringBuilder hash=new StringBuilder();for(byte value:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
    File cache=new File(context.getCodeCacheDir(),"ttvs-"+hash.toString()+"-storage-v3-"+(sdkShim?"trom":"native")+".jar");
    if(cache.exists()){if(validRuntime(cache)){lastSource=sourceKey;prepared=cache;return cache;}if(!cache.delete())throw new IOException("Cannot replace invalid core cache");}
    File temp=new File(context.getCodeCacheDir(),"core-runtime.tmp");if(temp.exists()&&!temp.delete())throw new IOException("Cannot replace runtime staging file");
    int replacements=0,sdkChanges=0;
    try(ZipFile source=new ZipFile(original);FileOutputStream file=new FileOutputStream(temp)){
      if(!temp.setReadOnly())throw new IOException("Runtime must be read-only before loading");
      try(ZipOutputStream output=new ZipOutputStream(file)){
        java.util.Enumeration<? extends ZipEntry> entries=source.entries();
        while(entries.hasMoreElements()){
          ZipEntry entry=entries.nextElement();String name=entry.getName();
          if(!name.matches("classes[0-9]*\\.dex"))continue;
          ByteArrayOutputStream bytes=new ByteArrayOutputStream();
          try(InputStream in=source.getInputStream(entry)){byte[] buffer=new byte[65536];int count;while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);}
          byte[] data=bytes.toByteArray();int count=replace(data,SOURCE.getBytes(StandardCharsets.UTF_8),DESTINATION.getBytes(StandardCharsets.UTF_8));replacements+=count;
          int adapted=sdkShim?DexSdkCompat.patch(data):0;sdkChanges+=adapted;
          if(count>0||adapted>0){byte[] signature=MessageDigest.getInstance("SHA-1").digest(java.util.Arrays.copyOfRange(data,32,data.length));System.arraycopy(signature,0,data,12,20);
            Adler32 checksum=new Adler32();checksum.update(data,12,data.length-12);ByteBuffer.wrap(data,8,4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)checksum.getValue());}
          output.putNextEntry(new ZipEntry(name));output.write(data);output.closeEntry();
        }
      }
    }
    if(replacements==0){temp.delete();throw new IOException("TTVSCore storage layout unsupported");}
    if(sdkShim&&sdkChanges!=1){temp.delete();throw new IOException("TTVSCore SDK compatibility layout unsupported");}
    if(!validRuntime(temp)){temp.delete();throw new IOException("Runtime validation failed");}
    if(!temp.renameTo(cache))throw new IOException("Cannot commit runtime copy");lastSource=sourceKey;prepared=cache;return cache;
  }
  static boolean validRuntime(File file){
    if(!file.isFile()||file.canWrite())return false;java.util.Set<String> names=new java.util.HashSet<>();long total=0;
    try(ZipInputStream input=new ZipInputStream(new FileInputStream(file))){ZipEntry entry;byte[] buffer=new byte[65536];
      while((entry=input.getNextEntry())!=null){String name=entry.getName();if(!name.matches("classes[0-9]*\\.dex")||!names.add(name))return false;
        byte[] header=new byte[8];int position=0;while(position<8){int n=input.read(header,position,8-position);if(n<0)return false;position+=n;}
        if(header[0]!='d'||header[1]!='e'||header[2]!='x'||header[3]!='\n'||header[7]!=0)return false;
        long size=8;int n;while((n=input.read(buffer))!=-1){size+=n;if(size>64L*1024*1024)return false;}total+=size;if(total>128L*1024*1024)return false;input.closeEntry();
      }
      return names.contains("classes.dex");
    }catch(IOException e){return false;}
  }
  static int replace(byte[] data,byte[] from,byte[] to){int count=0;outer:for(int i=0;i<=data.length-from.length;i++){
    for(int j=0;j<from.length;j++)if(data[i+j]!=from[j])continue outer;
    System.arraycopy(to,0,data,i,to.length);count++;i+=from.length-1;
  }return count;}
}
