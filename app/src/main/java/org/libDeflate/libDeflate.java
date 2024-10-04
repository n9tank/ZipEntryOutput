package org.libDeflate;
import java.nio.ByteBuffer;
import me.steinborn.libdeflate.LibdeflateCompressor;

public class libDeflate extends LibdeflateCompressor {
 public int rby;
 public int wby;
 public libDeflate(int lvl) {
  super(lvl, 0);
 }
 public int compress(ByteBuffer src, ByteBuffer drc) {
  rby += src.remaining();
  int i=super.compress(src, drc);
  wby += i;
  return i;
 }
}
