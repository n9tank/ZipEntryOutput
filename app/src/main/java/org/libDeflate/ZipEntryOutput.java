package org.libDeflate;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import me.steinborn.libdeflate.LibdeflateCRC32;
import me.steinborn.libdeflate.LibdeflateJavaUtils;


public class ZipEntryOutput extends OutputStream implements WritableByteChannel {
 public class DeflaterIo extends OutputStream implements WritableByteChannel {
  public libDeflate def;
  public LibdeflateCRC32 crc;
  public void write(int b) {
   throw new RuntimeException();
  }
  public boolean isOpen() {
   return true;
  }
  public void putEntry(ZipEntryM zip) {
   int mode=zip.mode;
   if (zip.notFix)crc = null;
   else crc = new LibdeflateCRC32();
   if (mode > 0)def = new libDeflate(mode);
  }
  public int write(ByteBuffer src) throws IOException {
   ZipEntryOutput zip=ZipEntryOutput.this;
   boolean only=zip.entry.mode <= 0;
   ParallelDeflate.crc(src, crc);
   if (only)return zip.write(src);
   else {
    int len=DeflateOutput.whilePut(src, zip.outBuf, def, zip);
    upLength(len);
    return len;
   }
  } 
  public void write(byte[] b, int off, int len) throws IOException {
   write(ByteBuffer.wrap(b, off, len));
  }
  public void close() {
   libDeflate def=this.def;
   if (def != null) {
    def.close();
    this.def = null;
   }
  }
 }
 public static final int AsInput=1;
 public static final int onlyInput=2;
 public static final int rcise=4;
 public static final int enmode=8;
 public static final int openJDk8opt=16;
 public static final int igonUtf8=32;
 public boolean pk78;
 public long last;
 public DeflaterIo outDef=new DeflaterIo();
 public ArrayList<ZipEntryM> list=new ArrayList();
 public long off;
 public long headOff;
 public ByteBufIo outBuf;
 public File outFile;
 public FileChannel rnio;
 public ZipEntryM entry;
 public byte flag=1;
 public CharsetEncoder charsetEncoder;
 public int outPage;
 public int readPage;
 public ByteBuffer tbuf;
 public CharsetEncoder utf8=StandardCharsets.UTF_8.newEncoder();
 public void page(int size) {
  readPage = size >> 1;
  outPage = LibdeflateJavaUtils.getBufSize(readPage, 0);
  ByteBuffer buf=ByteBuffer.allocateDirect(16);
  buf.order(ByteOrder.LITTLE_ENDIAN);
  tbuf = buf;
 }
 public ZipEntryOutput(File out) throws FileNotFoundException {
  //文件模式需要支持并发写出，鸽了。
  this(out, 16384, 32, null);
 }
 public ZipEntryOutput(File file, int size, int off, CharsetEncoder utf) throws FileNotFoundException {
  FileChannel wt = new FileOutputStream(outFile = file).getChannel();
  rnio = wt;
  outBuf = new ByteBufIo(wt, size + off);
  charsetEncoder = utf;
  page(size);
 }
 public ZipEntryOutput(WritableByteChannel wt) {
  this(wt, 16384, 32, null);
 }
 public ZipEntryOutput(WritableByteChannel wt, int size, int off, CharsetEncoder utf) {
  outBuf = new ByteBufIo(wt, size + off);
  charsetEncoder = utf;
  page(size);
 }
 public void cancel() {
  File out=outFile;
  if (out != null) {
   out.delete();
   out = null;
  } 
  list = null;
  outBuf.buf = null;
  try {
   close();
  } catch (Exception e) {
  }
 }
 public boolean isOpen() {
  return true;
 }
 public WritableByteChannel getNio() throws IOException {
  ByteBufIo buf=outBuf;
  WritableByteChannel wt = buf.wt;
  if (wt != null)buf.flush();
  else wt = buf;
  return wt;
 }
 public void upLength(long i) {
  if (list.size() == 0) {
   headOff += i;
  }
  off += i;
 }
 public void write(int b) {
  throw new RuntimeException();
 }
 public void write(byte[] b, int off, int len) throws IOException {
  outBuf.write(b, off, len);
  upLength(len);
 }
 public int write(ByteBuffer src) throws IOException {
  int i=outBuf.write(src);
  upLength(i);
  return i;
 }
 public void closeEntry() throws IOException {
  ZipEntryM ze=entry;
  if (ze != null) {
   ZipEntryOutput.DeflaterIo defo=outDef;
   if (ze.size < 0) {
    LibdeflateCRC32 crc=defo.crc;
    if (ze.mode > 0)
     ParallelDeflate.fixEntry(defo.def, crc, ze);
    else ParallelDeflate.fixEntry(this, crc, ze);
    if ((flag & AsInput) > 0 && rnio == null) {
     writeEntryFix(ze);
    }
   }
   defo.close();
   entry = null;
  }
 }
 public void putEntry(ZipEntryM zip) throws IOException {
  putEntry(zip, false, false);
 }
 public void putEntry(ZipEntryM zip, boolean raw) throws IOException {
  putEntry(zip, raw, false);
 }
 public void putEntry(ZipEntryM zip, boolean raw, boolean onlyIn) throws IOException {
  closeEntry();
  if (!raw)outDef.putEntry(zip);
  entry = zip;  
  zip.start = off;
  if (!onlyIn)list.add(zip);
  writeEntry(zip);
  last = off;
 }
 public void writeEntryModify(ZipEntryM zip, long pos) throws IOException {
  int size=zip.size;
  if (zip.notFix || size <= 0)return;
  long start=zip.start + 14;
  ByteBuffer buff= tbuf;
  if (start >= pos) {
   buff = outBuf.buf;
   buff.position((int)(start - pos));
  } else buff = tbuf;
  buff.putInt(size);
  buff.putInt(zip.csize);
  buff.putInt(zip.crc);
  if (start < pos) {
   if (start + 12 >= pos) {
    ByteBuffer buf = outBuf.buf;
    int off=(int)(pos - start);
    int len=12 - off;
    buf.rewind();
    buf.limit(len);
    buff.position(off);
    buf.put(buff);
    buff.rewind();
    buff.limit(off);
    buf.clear();
   } else buff.flip();
   FileChannel nio=rnio;
   nio.position(start);
   nio.write(buff);
  }
  buff.clear();
 }
 public void writeEntry(ZipEntryM zip) throws IOException {
  boolean utf8;
  boolean skip;
  CharsetEncoder charsetEncoder=this.charsetEncoder;
  if ((flag & AsInput) > 0) {
   utf8 = zip.utf(charsetEncoder);
   skip = false;
  } else {
   utf8 = false;
   skip = true;
  }
  ByteBufIo out=outBuf;
  ByteBuffer buff=out.getBuf(1024);
  int pos=buff.position();
  buff.putInt(0x04034b50);
  putBits(buff, utf8, zip);
  fill(buff, pos + 26);
  buff.position(pos + 28);
  fill(buff, pos + 30);
  int len;
  if (!skip)len = zip.encode(charsetEncoder, this, utf8);
  else len = 0;
  if (len > 0) fixNameSize(off + 28, len);
  else buff.putShort(pos + 26, (short)(buff.position() - 30 - pos));
  upLength(len + 30);
 }
 public short globalBit(boolean data, boolean utf8) {
  short bit=0;
  if (data)bit |= 8;
  if ((flag & igonUtf8) == 0 && utf8)bit |= 2048;
  return bit;
 }
 public void fixNameSize(long g, int size) throws IOException {
  FileChannel rnio=this.rnio;
  rnio.position(g);
  ByteBuffer tbuf=this.tbuf;
  tbuf.putShort((short)size);
  tbuf.flip();
  rnio.write(tbuf);
  rnio.position(rnio.size());
  //否则需要一个队列
  tbuf.clear();
 }
 public void writeEntryFix(ZipEntryM zip) throws IOException {
  int size=zip.size;
  if (zip.notFix || size <= 0)return;
  ByteBufIo out=outBuf;
  ByteBuffer buff=out.getBuf(16);
  buff.putInt(0x08074b50);
  buff.putInt(zip.crc);
  buff.putInt(zip.csize);
  buff.putInt(size);
  upLength(16);
 }
 public void finish(ZipEntryM[] badlist) throws IOException {
  closeEntry();
  FileChannel nio=rnio;
  int flag=this.flag;
  if ((flag & AsInput) > 0 && nio != null) {
   long pos=nio.size();
   ByteBuffer buf=outBuf.buf;
   int cpos=buf.position();
   for (ZipEntryM ze:list)writeEntryModify(ze, pos);
   buf.position(cpos);
   nio.position(pos);
  }
  if ((flag & onlyInput) == 0) {
   long size=off;
   if (badlist != null) {
    Collections.addAll(list, badlist);
    Collections.shuffle(list);
   }
   for (ZipEntryM ze:list) {
    writeEntryEnd(ze);
   }
   writeEnd((int)(off - size));
  }
  list = null;
 }
 public void close() throws IOException {
  try {
   if (list != null) finish(null);
  } finally {
   ZipEntryOutput.DeflaterIo out=outDef;
   if (out != null) {
    outDef = null;
    out.close();
    outBuf.close();
   }
  }
 }
 public void putBits(ByteBuffer buff, boolean utf8, ZipEntryM zip) {
  short mode=zip.mode;
  int flag=this.flag;
  buff.putShort((short)0);//ver 该值没有任何作用
  boolean data;
  buff.putShort(globalBit(data = (!zip.notFix && (flag & AsInput) > 0 && rnio == null), utf8));
  pk78 |= data;
  boolean enmode=(flag & this.enmode) == 0;
  buff.putShort((short)(enmode && mode <= 0 ?0: 8));
  buff.putInt(zip.xdostime);
  buff.putInt(zip.crc);
  buff.putInt(enmode && mode <= 0 ?0 : ((flag & rcise) > 0 ?-1: zip.csize));
  int size=zip.size;
  buff.putInt(Math.max(0, ((flag & openJDk8opt) > 0 && mode > 0 && size > 65534 ?65534: size)));
 }
 public void writeEntryEnd(ZipEntryM zip) throws IOException {
  CharsetEncoder charsetEncoder=this.charsetEncoder;
  boolean utf8=zip.utf(charsetEncoder);
  ByteBufIo out=outBuf;
  ByteBuffer buff=out.getBuf(1024);
  int pos=buff.position();
  buff.putInt(0x02014b50);
  buff.putShort((short)0);
  putBits(buff, utf8, zip);
  fill(buff, pos + 28);
  buff.position(pos + 30);
  fill(buff, pos + 42);
  buff.putInt((int)(zip.start - headOff));
  int len=zip.encode(charsetEncoder, this, utf8);
  if (len > 0)fixNameSize(off + 28, len);
  else buff.putShort(pos + 28, (short)(buff.position() - 46 - pos));
  upLength(len + 26);
 }
 public static void fill(ByteBuffer buf, int pos) {
  int i=buf.position();
  byte b=0;
  for (;i <= pos;++i)buf.put(i, b);
  buf.position(pos);
 }
 public void writeEnd(int size)throws IOException {
  ByteBuffer buff= outBuf.getBuf(24);
  int pos=buff.position();
  int len= pos + (pk78 ?24: 22);
  buff.putInt(0X06054B50);
  fill(buff, pos + 8);
  short num=(short)list.size();
  buff.putShort(num);
  buff.putShort(num);
  buff.putInt(size);
  buff.putInt((int)(off - size - headOff));
  fill(buff, len);
  upLength(len);
 }
}
