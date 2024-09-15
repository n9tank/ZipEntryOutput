package org.libDeflate;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.InputStream;
import java.io.IOException;

public class ZipInputGet implements InputGet {
 public ZipFile zip;
 public ZipEntry en;
 public boolean raw;
 public InputStream io() throws IOException {
  InputStream in=zip.getInputStream(en);
  if (raw)in = ZipEntryInput.getRaw(in, en);
   return in;
 }
 public ZipInputGet(ZipFile zip, ZipEntry en, boolean raw) {
  this.zip = zip;
  this.en = en;
  this.raw = raw;
 }
}
