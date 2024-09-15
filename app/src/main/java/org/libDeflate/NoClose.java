package org.libDeflate;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class NoClose implements WritableByteChannel {
 WritableByteChannel wt;
 public NoClose(WritableByteChannel wt) {
  this.wt = wt;
 }
 public void close()  {
 }
 public boolean isOpen() {
  return true;
 }
 public int write(ByteBuffer src) throws IOException {
  return wt.write(src);
 }
}
