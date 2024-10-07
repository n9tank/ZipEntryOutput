package org.libDeflate;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class BufWriter extends Writer {
 public void flush() {
 }
 public void write(char[] cbuf, int off, int len) {
  write(CharBuffer.wrap(cbuf, off, len));
 }
 public void write(String str) {
  write(CharBuffer.wrap(str));
 }
 public void write(CharBuffer str) {
  BufOutput put=this.buf;
  ByteBuffer buf=put.buf;
  CharsetEncoder en=this.en;
  while (str.hasRemaining()) {
   if (en.encode(str, buf, false).isOverflow())
    buf = put.capacity2();
  }
 }
 public void close(){
  BufOutput put=this.buf;
  ByteBuffer buf=put.buf;
  CharBuffer str=CharBuffer.allocate(0);
  while (en.encode(str, buf, true).isOverflow())
   buf = put.capacity2();
  while (en.flush(buf).isOverflow())
   buf = put.capacity2();
 }
 public BufOutput buf;
 public CharsetEncoder en;
 public BufWriter(BufOutput out, Charset set) {
  buf = out;
  en = set.newEncoder();
 }
}
