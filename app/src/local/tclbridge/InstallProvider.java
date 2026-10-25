package local.tclbridge;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;

/** Grants the system installer access to one verified APK, never arbitrary app files. */
public class InstallProvider extends ContentProvider {
  public boolean onCreate(){return true;}
  private File file(Uri uri)throws FileNotFoundException{if(!"/SmartAlexa.apk".equals(uri.getPath())||uri.getQuery()!=null)throw new FileNotFoundException();File f=new File(getContext().getCacheDir(),"install/SmartAlexa.apk");if(!f.isFile())throw new FileNotFoundException();return f;}
  public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
  public String getType(Uri uri){return "application/vnd.android.package-archive";}
  public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File f=file(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++)values[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?"SmartAlexa.apk":OpenableColumns.SIZE.equals(columns[i])?f.length():null;cursor.addRow(values);return cursor;}catch(FileNotFoundException e){return null;}}
  public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
