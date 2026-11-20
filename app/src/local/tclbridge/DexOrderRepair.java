package local.tclbridge;

import java.io.IOException;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

/** Rebuild references only when a private path substitution disrupts DEX string ordering. */
final class DexOrderRepair {
  static boolean ordered(DexBackedDexFile dex) {
    String previous=null;
    for(StringReference ref:dex.getStringReferences()) {
      String value=ref.getString();
      if(previous!=null&&previous.compareTo(value)>=0)return false;
      previous=value;
    }
    return true;
  }
  static byte[] repair(byte[] data)throws IOException {
    try {
      DexBackedDexFile dex=new DexBackedDexFile(null,data);
      if(ordered(dex))return data;
      MemoryDataStore output=new MemoryDataStore(data.length);
      try {
        DexPool.writeTo(output,dex);
        byte[] result=output.getData();
        if(result.length>64*1024*1024||!ordered(new DexBackedDexFile(null,result)))
          throw new IOException("Rebuilt DEX string ordering invalid");
        return result;
      }finally{output.close();}
    }catch(RuntimeException error){throw new IOException("Cannot rebuild runtime DEX",error);}
  }
}
