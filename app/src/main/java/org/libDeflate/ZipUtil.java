package org.libDeflate;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;

public class ZipUtil {
 //这个不属于标准库，所以性能啥的咱不管
 public static ZipEntryM newEntry(String name, int lvl) {
  ZipEntryM zip= new ZipEntryM(name, lvl);
  zip.crc = 0xff;
  zip.notFix = true;
  return zip;
 }
 public static void addRandomHead(ZipEntryOutput out, int rnd[]) throws Exception {
  int len=rnd.length;
  if (len == 1) {
   ByteBuffer buf=ByteBuffer.allocateDirect(4);
   buf.putInt(0x504b0304);
   buf.flip();
   out.write(buf);
  } else {
   Random ran=new Random();
   byte[] buf=new byte[1024];
   int c=0;
   int blen=buf.length;
   ZipEntryOutput.DeflaterIo def=out.outDef;
   for (int i=0;i < len;++i) {
    ZipEntryM ze=newEntry(null, 1);
    out.putEntry(ze, false, true);
    int add=rnd[i] + ran.nextInt(rnd[++i]);
    while (add > 0) {
     int wlen=Math.min(add, blen);
     if (blen - c < wlen) {
      ran.nextBytes(buf);
      c = 0;
     }
     def.write(buf, c, wlen);
     add -= wlen;
     c += add;
    }
   }
  }
 }
}
