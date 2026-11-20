package local.tclbridge;
import java.util.zip.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

public final class DexOrderRepairTest {
  static int replace(byte[] d,byte[] a,byte[] b){int count=0;outer:for(int i=0;i<=d.length-a.length;i++){
    for(int j=0;j<a.length;j++)if(d[i+j]!=a[j])continue outer;
    System.arraycopy(b,0,d,i,b.length);count++;i+=a.length-1;
  }return count;}
  public static void main(String[] args)throws Exception {
    for(String path:args){int changed=0,rebuilt=0;
      try(ZipFile apk=new ZipFile(path)){
        java.util.Enumeration<? extends ZipEntry> entries=apk.entries();
        while(entries.hasMoreElements()){
          ZipEntry entry=entries.nextElement();if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
          byte[] original;try(InputStream in=apk.getInputStream(entry)){original=read(in);}
          byte[] data=original.clone();
          int count=replace(data,"/data/data/com.tcl.ttvs/files/alexa/".getBytes(StandardCharsets.UTF_8),
                                 "/data/data/local.tclbridge/files/aa/".getBytes(StandardCharsets.UTF_8));
          changed+=count;byte[] fixed=DexOrderRepair.repair(data);
          if(fixed!=data){
            rebuilt++;
            DexBackedDexFile before=new DexBackedDexFile(null,data),after=new DexBackedDexFile(null,fixed);
            if(before.getClasses().size()!=after.getClasses().size()||before.getFieldSection().size()!=after.getFieldSection().size()||before.getMethodSection().size()!=after.getMethodSection().size())throw new AssertionError("DEX definitions changed");
          }
          if(!DexOrderRepair.ordered(new DexBackedDexFile(null,fixed)))throw new AssertionError("Order invalid");
          if(count==0&&!java.util.Arrays.equals(data,fixed))throw new AssertionError("Unchanged DEX rebuilt");
        }
      }
      System.out.println(new File(path).getName()+" paths="+changed+" rebuilt="+rebuilt+" ordered=true");
    }
  }
  static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
}
