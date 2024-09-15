package org.libDeflate;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

public abstract class IoWriter {
 public int bufSize;
 public WritableByteChannel out;
 public abstract void flush()throws Exception;
}
