package org.libDeflate;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import android.util.Log;

public class ByteBufIo extends OutputStream implements WritableByteChannel,BufIo {
 public void write(int b) {
  throw new RuntimeException();
 }
 public boolean isOpen() {
  return true;
 }
 public ByteBuffer buf;
 public WritableByteChannel wt;
 public ByteBufIo(WritableByteChannel out, int size) {
  wt = out;
  ByteBuffer buf = ByteBuffer.allocateDirect(size);
  buf.order(ByteOrder.LITTLE_ENDIAN);
  this.buf = buf;
 }
 public void close() throws IOException {
  try {
   flush();
  } finally {
   wt.close();
  }
 }
 public void flush() throws IOException {
  ByteBuffer buf=this.buf;
  if (buf == null)return;
  NioWriter.write(buf, wt);
  buf.clear();
 }
 public void flushIo() throws IOException {
  getBuf((buf.capacity() & 4095) + 1);
 }
 public ByteBuffer getBuf(int page) throws IOException {
  ByteBuffer buf=this.buf;
  int pos=buf.position();
  int cy=buf.capacity() - page;
  if (pos > cy) {
   int len = pos & -4096;
   WritableByteChannel wt=this.wt;
   buf.rewind();
   buf.limit(len);
   while (buf.hasRemaining())
    wt.write(buf);
   buf.limit(pos);
   buf.position(len); 
   buf.compact();
  }
  return buf;
 }
 public void put(byte brr[], int off, int len) throws IOException {
  WritableByteChannel wt=this.wt;
  if (wt != null) {
   ByteBuffer buf=ByteBuffer.wrap(brr, off, len);
   while (buf.hasRemaining())
    wt.write(buf);
  }
 }
 public void write(byte brr[], int off, int len) throws IOException {
  ByteBuffer buf=this.buf;
  int cy=buf.capacity() & -4096;
  int limt=Math.max(0, cy - buf.position());
  int wlen=len - limt;
  if (limt < cy || wlen < 0 || buf.isDirect())
   buf.put(brr, off, Math.min(len, limt));
  else wlen = len;
  if (wlen > 0) {
   flushIo();
   off += limt;
   if (wlen >= cy) {
    int rlen=wlen & 4095;
    put(brr, off , wlen & -4096);
    off += wlen;
    wlen = rlen;
   }
   buf.put(brr, off, wlen);
  }
 }
 public int write(ByteBuffer put) throws IOException {
  int len=put.limit();
  int rsize=len;
  if (!put.isDirect()) {
   write(put.array(), 0, len);
  } else {
   ByteBuffer buf=this.buf;
   int cy=buf.capacity() & -4096;
   int limt=Math.max(0, cy - buf.position());
   int wlen=len - limt;
   if (wlen > 0)put.limit(limt);
   if (limt < cy || wlen < 0)buf.put(put);
   limt = put.position();
   wlen = len - limt;
   if (wlen > 0) {
    flushIo();
    if (wlen >= cy) {
     WritableByteChannel wt=this.wt;
     if (wt != null) {
      put.limit(limt + (wlen & -4096));
      while (put.hasRemaining())
       wt.write(put);
     }
    }
    put.limit(len);
    buf.put(put);
   }
  }
  return rsize;
 }
}
