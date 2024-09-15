package org.libDeflate;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import me.steinborn.libdeflate.LibdeflateCRC32;

public class DeflateOutput extends OutputStream implements WritableByteChannel {
 public ParallelDeflate lock;
 public libDeflate def;
 public LibdeflateCRC32 crc;
 public ZipEntryM ze;
 public BufIo buf;
 public DeflateOutput(ParallelDeflate para, ZipEntryM en, BufIo out) {
  lock = para;
  this.def = new libDeflate(en.mode);
  this.buf = out;
  if (!en.notFix)crc = new LibdeflateCRC32();
  ze = en;
 }
 public void write(int b) {
  throw new RuntimeException();
 }
 public boolean isOpen() {
  return true;
 }
 public static int whilePut(ByteBuffer src, BufIo buf, libDeflate def, ZipEntryOutput zip) throws IOException {
  int len=0;
  int size=src.limit();
  int pos;
  int read=zip.readPage;
  int out=zip.outPage;
  while (size > (pos = src.position())) {
   src.limit(Math.min(pos + read, size));
   len += def.compress(src, buf.getBuf(out));
  }
  return len;
 }
 public int write(ByteBuffer src) throws IOException {
  ParallelDeflate para=lock;
  ZipEntryOutput out=para.zipout;
  ParallelDeflate.crc(src, crc);
  buf = para.toZip(buf, ze);
  int len=whilePut(src, buf, def, out);
  if (buf instanceof ByteBufIo)out.upLength(len);
  return len;
 }
 public void write(byte[] b, int off, int len) throws IOException {
  write(ByteBuffer.wrap(b, off, len));
 }
 public boolean iswrok() {
  return buf instanceof ByteBufIo;
 }
 public void close() {
  ParallelDeflate.fixEntry(def, crc, ze);
  def.close();
 }
}
