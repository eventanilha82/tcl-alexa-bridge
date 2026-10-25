package local.tclbridge;

import com.cgutman.adblib.AdbCrypto;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

/** One local, user-authorized setup session. No listener, remote target or key storage. */
final class LocalAdb implements Closeable {
  static final int CNXN=0x4e584e43,AUTH=0x48545541,OPEN=0x4e45504f,OKAY=0x59414b4f,WRTE=0x45545257,CLSE=0x45534c43;
  private final Socket socket=new Socket();private final int port;private DataInputStream in;private OutputStream out;private int serial;private volatile boolean closed;
  LocalAdb(){this(5555);}LocalAdb(int loopbackPort){if(loopbackPort<1||loopbackPort>65535)throw new IllegalArgumentException();port=loopbackPort;}
  void connect(Consumer<String> progress)throws Exception{
    if(closed)throw new IOException("Configuração cancelada");
    progress.accept("Preparando autorização desta TV…");
    AdbCrypto key=AdbCrypto.generateAdbKeyPair(bytes->Base64.getEncoder().encodeToString(bytes));
    socket.connect(new InetSocketAddress("127.0.0.1",port),3000);socket.setSoTimeout(120000);socket.setTcpNoDelay(true);
    in=new DataInputStream(socket.getInputStream());out=socket.getOutputStream();send(CNXN,0x01000000,4096,"host::\0".getBytes(StandardCharsets.UTF_8));
    boolean signed=false,published=false;long deadline=System.nanoTime()+120000000000L;
    while(!closed&&System.nanoTime()<deadline){
      Packet p=read();
      if(p.command==CNXN){socket.setSoTimeout(15000);progress.accept("Autorização confirmada");return;}
      if(p.command!=AUTH||p.arg0!=1||p.data.length!=20)throw new IOException("Resposta de autorização incompatível");
      if(!signed){send(AUTH,2,0,key.signAdbTokenPayload(p.data));signed=true;}
      else if(!published){progress.accept("Na mensagem do Android, selecione Permitir. A chave será descartada ao terminar.");send(AUTH,3,0,key.getAdbPublicKeyPayload());published=true;}
      else throw new IOException("Autorização recusada. Tente novamente e confirme na TV.");
    }
    throw new SocketTimeoutException("Autorização não concluída");
  }
  String shell(String fixedCommand)throws IOException{
    if(closed)throw new IOException("Sessão encerrada");
    // Package-private callers supply fixed commands only; never exposed through intents or text fields.
    String command="( "+fixedCommand+" ); printf '\\n__TCLBRIDGE_EXIT__%s\\n' \"$?\"";
    int id=++serial,remote=0;send(OPEN,id,0,("shell:"+command+"\0").getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream data=new ByteArrayOutputStream();
    int stale=0;
    for(;;){Packet p=read();
      // adbd may emit extra CLOSE packets after late ACKs for the previous shell.
      if(p.arg1>0&&p.arg1<id&&(p.command==CLSE||p.command==OKAY)&&++stale<=16)continue;
      if(p.arg1!=id)throw new IOException("Resposta inesperada ao preparar a TV. Tente novamente.");
      if(p.command==OKAY){remote=p.arg0;continue;}
      if(p.command==WRTE){remote=p.arg0;if(data.size()+p.data.length>262144)throw new IOException("Resposta de configuração muito longa");data.write(p.data);send(OKAY,id,remote,new byte[0]);continue;}
      if(p.command==CLSE){String value=data.toString("UTF-8");int mark=value.lastIndexOf("\n__TCLBRIDGE_EXIT__");
        if(mark<0||!value.substring(mark).trim().equals("__TCLBRIDGE_EXIT__0"))throw new IOException("A TV recusou uma etapa da preparação");return value.substring(0,mark).trim();}
      throw new IOException("Resposta de configuração inesperada");
    }
  }
  private void send(int command,int a,int b,byte[] data)throws IOException{
    int sum=0;for(byte value:data)sum+=value&255;
    byte[] header=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).putInt(command).putInt(a).putInt(b).putInt(data.length).putInt(sum).putInt(command^0xffffffff).array();
    out.write(header);out.write(data);out.flush();
  }
  private Packet read()throws IOException{
    byte[] header=new byte[24];in.readFully(header);ByteBuffer b=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
    int command=b.getInt(),a=b.getInt(),c=b.getInt(),length=b.getInt(),checksum=b.getInt(),magic=b.getInt();
    if(magic!=(command^0xffffffff)||length<0||length>1048576)throw new IOException("Pacote de configuração inválido");
    byte[] data=new byte[length];in.readFully(data);int sum=0;for(byte value:data)sum+=value&255;if(sum!=checksum)throw new IOException("Integridade da resposta inválida");return new Packet(command,a,c,data);
  }
  public void close(){closed=true;try{socket.close();}catch(IOException ignored){}}
  boolean isClosed(){return closed;}
  private static final class Packet{final int command,arg0,arg1;final byte[] data;Packet(int c,int a,int b,byte[] d){command=c;arg0=a;arg1=b;data=d;}}
}
