package local.tclbridge;
import java.io.*;import java.nio.*;import java.nio.file.*;import java.security.*;import java.util.zip.*;
public class DexSdkCompatTest {
 public static void main(String[] args)throws Exception{
  int changed=0;
  try(ZipFile apk=new ZipFile(args[0])){java.util.Enumeration<? extends ZipEntry> entries=apk.entries();while(entries.hasMoreElements()){ZipEntry entry=entries.nextElement();if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;byte[] original;try(InputStream in=apk.getInputStream(entry);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);original=out.toByteArray();}byte[] data=original.clone();int count=DexSdkCompat.patch(data);changed+=count;if(count>0){int delta=0;for(int i=0;i<data.length;i++)if(data[i]!=original[i])delta++;if(delta<2||delta>512)throw new AssertionError("Unexpected patch scope");byte[] sha=MessageDigest.getInstance("SHA-1").digest(java.util.Arrays.copyOfRange(data,32,data.length));System.arraycopy(sha,0,data,12,20);Adler32 sum=new Adler32();sum.update(data,12,data.length-12);ByteBuffer.wrap(data,8,4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)sum.getValue());Files.write(Paths.get(args[1]),data);System.out.println("SDK metadata patch: one method; changed bytes="+delta);}}}
  if(changed!=1)throw new AssertionError("Expected exactly one SDK check");
  try{DexSdkCompat.patch(new byte[128]);throw new AssertionError("Invalid DEX accepted");}catch(IOException expected){}
  System.out.println("Real installed DEX and malformed-header checks passed.");
 }
}
