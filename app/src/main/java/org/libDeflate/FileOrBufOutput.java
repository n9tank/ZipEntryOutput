package org.libDeflate;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import android.util.Log;


public class FileOrBufOutput implements BufIo {
 public boolean isOpen() {
  return true;
 }
 ByteBuffer buf;
 FileChannel io;
 File cache;
 long last;
 public FileOrBufOutput(int size) {
  buf = ByteBuffer.allocateDirect(size);
 }
 public static File openTmp() throws IOException {
  File f=Files.createTempFile("", "").toFile();
  f.deleteOnExit();
  return f;
 }
 public FileChannel openFile() throws IOException {
  FileChannel fio=io;
  if (fio == null)io = fio = new RandomAccessFile(cache = openTmp(), "rw").getChannel();
  return fio;
 }
 public ByteBuffer getBuf(int page) throws IOException {
  ByteBuffer buf=this.buf;
  int pos=buf.position();
  int cy=buf.capacity() - page;
  if (cy >= page && pos > cy) {
   int len = pos & -4096;
   buf.rewind();
   buf.limit(len);
   WritableByteChannel wt=openFile();
   while (buf.hasRemaining())
    wt.write(buf);
   buf.limit(pos);
   buf.position(len); 
   buf.compact();
  }
  return buf;
 }
 public int write(ByteBuffer src) throws IOException {
  throw new RuntimeException();
 }
 public void writeTo(ZipEntryOutput zip) throws IOException {
  ByteBufIo zbuf=zip.outBuf;
  zip.upLength(writeTo(last <= 0 ?zbuf: zip.getNio(), zbuf));
 }
 public long writeTo(WritableByteChannel out, WritableByteChannel zbuf) throws IOException {
  FileChannel fio=io;
  long len=last;
  if (fio != null) {
   fio.position(0);
   fio.transferTo(0, len, out);
  }
  ByteBuffer buf=this.buf;
  len += buf.position();
  buf.flip();
  while (buf.hasRemaining())
   zbuf.write(buf);
  return len;
 }
 public void close() throws IOException {
  FileChannel fio=io;
  if (fio != null) {
   try {
    fio.close();
   } finally {
    cache.delete();
   }
  }
 }
}
