package org.libDeflate;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class NioWriter extends Writer {
 public void flush() {
 }
 public void write(char[] cbuf, int off, int len) throws IOException {
  write(CharBuffer.wrap(cbuf, off, len));
 }
 public void write(String str) throws IOException {
  write(CharBuffer.wrap(str));
 }
 public static void write(ByteBuffer buf, WritableByteChannel wt) throws IOException {
  buf.flip();
  while (buf.hasRemaining())
   wt.write(buf);
  buf.clear();
 }
 public void write(CharBuffer str) throws IOException {
  ByteBuffer buf=this.buf;
  CharsetEncoder en=this.en;
  WritableByteChannel wt=this.wt;
  while (str.hasRemaining()) {
   if (en.encode(str, buf, false).isOverflow())
    write(buf, wt);
  }
 }
 public void close() throws IOException {
  ByteBuffer buf=this.buf;
  WritableByteChannel wt = this.wt;
  CharBuffer str=CharBuffer.allocate(0);
  while (en.encode(str, buf, true).isOverflow())
   write(buf, wt);
  while (en.flush(buf).isOverflow())
   write(buf, wt);
  write(buf, wt); 
  wt.close();
 }
 public ByteBuffer buf;
 public WritableByteChannel wt;
 public CharsetEncoder en;
 public NioWriter(WritableByteChannel out, int size, Charset set) {
  wt = out;
  buf = ByteBuffer.allocateDirect(size);
  en = set.newEncoder();
 }
}
