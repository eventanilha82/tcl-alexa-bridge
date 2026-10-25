package local.tclbridge;
import java.io.IOException;
import java.nio.*;
import java.nio.charset.StandardCharsets;

/** TROM exposes the new device-info API but omits the legacy TBuild.VERSION.SDK_INT field. */
final class DexSdkCompat {
  static final String TYPE="Lcom/tcl/ttvs/alexa/core/utils/PropertyUtil;";
  private final byte[] data;private final ByteBuffer view;private final int strings,types,methods;
  DexSdkCompat(byte[] d)throws IOException{data=d;view=ByteBuffer.wrap(d).order(ByteOrder.LITTLE_ENDIAN);if(d.length<112||d[0]!='d'||d[1]!='e'||d[2]!='x'||u4(32)!=d.length)throw new IOException("DEX header unsupported");strings=u4(60);types=u4(68);methods=u4(92);}
  int u4(int at)throws IOException{range(at,4);int v=view.getInt(at);if(v<0)throw new IOException("DEX offset unsupported");return v;}
  int u2(int at)throws IOException{range(at,2);return view.getShort(at)&65535;}
  void range(int start,long size)throws IOException{if(start<0||size<0||(long)start+size>data.length)throw new IOException("Invalid DEX bounds");}
  int leb(int[] cursor)throws IOException{long v=0;for(int n=0;n<5;n++){range(cursor[0],1);int b=data[cursor[0]++]&255;v|=(long)(b&127)<<(7*n);if((b&128)==0){if(v>Integer.MAX_VALUE)throw new IOException("Invalid DEX integer");return(int)v;}}throw new IOException("Invalid DEX integer");}
  String string(int index)throws IOException{if(index<0||index>=u4(56))throw new IOException("Invalid DEX string");int offset=u4(strings+index*4);int[] c={offset};leb(c);int start=c[0];while(c[0]<data.length&&data[c[0]]!=0&&c[0]-start<=65536)c[0]++;range(c[0],1);if(c[0]-start>65536)throw new IOException("Invalid DEX string size");return new String(data,start,c[0]-start,StandardCharsets.UTF_8);}
  String type(int index)throws IOException{if(index<0||index>=u4(64))throw new IOException("Invalid DEX type");return string(u4(types+index*4));}
  int patch()throws IOException{
    int definitions=u4(100),count=u4(96);range(definitions,(long)count*32);
    for(int i=0;i<count;i++){int item=definitions+i*32;if(!TYPE.equals(type(u4(item))))continue;
      int offset=u4(item+24);if(offset==0)throw new IOException("SDK metadata class has no implementation");int[] cursor={offset};int statics=leb(cursor),fields=leb(cursor),direct=leb(cursor),virtual=leb(cursor);
      if((long)statics+fields+direct+virtual>1000000)throw new IOException("Invalid DEX class data");
      for(int n=0;n<statics+fields;n++){leb(cursor);leb(cursor);}
      int changes=0;for(int group:new int[]{direct,virtual}){int method=0;for(int n=0;n<group;n++){
        method+=leb(cursor);int access=leb(cursor),code=leb(cursor);if(method<0||method>=u4(88))throw new IOException("Invalid DEX method");int entry=methods+method*8;
        if(!"isTclWidgetUseNewSdk".equals(string(u4(entry+4))))continue;
        int proto=u2(entry+2),protos=u4(76);if(proto>=u4(72)||(access&8)==0||code==0)throw new IOException("SDK metadata method unsupported");
        if(!"Z".equals(string(u4(protos+proto*12)))||u4(protos+proto*12+8)!=0||u2(code)<1||u2(code+2)!=0||u2(code+6)!=0)throw new IOException("SDK metadata signature unsupported");
        int instructions=u4(code+12);if(instructions<2||instructions>256)throw new IOException("SDK metadata body unsupported");range(code+16,(long)instructions*2);
        java.util.Arrays.fill(data,code+16,code+16+instructions*2,(byte)0);data[code+16]=0x12;data[code+17]=0x10;data[code+18]=0x0f;changes++;
      }}
      if(changes!=1)throw new IOException("SDK metadata adapter not found exactly once");return changes;
    }
    return 0;
  }
  static int patch(byte[] data)throws IOException{return new DexSdkCompat(data).patch();}
}
