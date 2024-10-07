package org.libDeflate;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.io.BufferedWriter;
import java.nio.charset.Charset;

public abstract class IoWriter {
 public int bufSize;
 public WritableByteChannel out;
 public BufferedWriter getWriter(Charset set) {
  //字符流，不支持原始模式（这里写内联有些麻烦）
  return new BufferedWriter(new BufWriter((BufOutput)out, set));
 }
 public abstract void flush()throws Exception;
}
