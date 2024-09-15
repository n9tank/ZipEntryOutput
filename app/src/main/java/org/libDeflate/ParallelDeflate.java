package org.libDeflate;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import me.steinborn.libdeflate.LibdeflateCRC32;
import me.steinborn.libdeflate.LibdeflateJavaUtils;
import java.util.concurrent.Callable;

public class ParallelDeflate implements AutoCloseable {
 public class DeflateWriter implements Callable,AutoCloseable {
  public InputGet in;
  public boolean raw;
  public IoWriter io;
  public FileOrBufOutput outflush;
  public ZipEntryM zip;
  public File fc;
  public DeflateWriter(InputGet input, ZipEntryM ze, boolean raw) {
   in = input;
   zip = ze;
   this.raw = raw;
  }
  public DeflateWriter(IoWriter out, ZipEntryM ze) {
   io = out;
   zip = ze;
  }
  public DeflateWriter(File out, ZipEntryM ze) {
   fc = out;
   zip = ze;
  }
  public void join() throws Exception {
   ZipEntryOutput out=zipout;
   IoWriter wt=io;
   out.putEntry(zip, !((wt != null || fc != null) && (zip.mode <= 0 && !zip.notFix)));
   if (wt != null) {
    write(wt, true, zip);
    return;
   }
   InputGet ing=this.in;
   InputStream input=ing == null ?null: ing.io();
   if (raw)copyIo(input, zip);
   else {
    FileOrBufOutput outf=outflush;
    try {
     if (outf == null) {
      if (input != null)deflate(input, out, zip);
      else addFile(fc, out, zip);
     } else outf.writeTo(out);
    } finally {
     if (outf != null)outf.close();
    }
   }
  }
  public Object call() throws Exception {
   boolean wroking=false;
   try {
    if (wroking = !wrok.getAndSet(true))
     join();
    else if (outflush == null) {
     ZipEntryM zip=this.zip;
     int mode=zip.mode;
     IoWriter wt=io;
     if (wt == null) {
      if (!raw && mode > 0) {
       InputGet ing=this.in;
       InputStream in;
       boolean is;
       if (is = ing != null)in = ing.io();
       else in = null;
       FileOrBufOutput cio=new FileOrBufOutput(bufSize((int)Math.min(is ?in.available(): fc.length(), 8192l)));
       outflush = cio;
       if (is)wroking = deflate(in, cio, zip);
       else wroking = addFile(fc, cio, zip);
      }
     } else if (mode > 0) {
      io = null;
      wroking = (outflush = write(wt, false, zip)) == null;
     }
     boolean upwrok=wroking || !wrok.getAndSet(true);
     if (upwrok) {
      if (!wroking)join();
      wroking = upwrok;
     } else list.push(this);
    }
   } catch (Exception e) {
    on.onError(e);
   }
   if (wroking)clearList(false);
   check();
   return null;
  }
  public void close() throws Exception {
   FileOrBufOutput cs = outflush;
   if (cs != null)cs.close();
  }
 }
 public int bufSize(int size) {
  return Math.min(zipout.outBuf.buf.capacity(), LibdeflateJavaUtils.getBufSize(size, 0));
 }
 public static void crc(ByteBuffer src, LibdeflateCRC32 crc) {
  if (crc != null) {
   crc.update(src);
   src.rewind();
  }
 }
 public void clearList(boolean close) {
  clearList(list, close);
  wrok.set(false);
 }
 public void clearList(ConcurrentLinkedDeque<DeflateWriter> obj, boolean close) {
  if (obj == null)return;
  while (!obj.isEmpty()) {
   DeflateWriter def=obj.pop();
   try {
    if (close)def.close();
    else def.join();
   } catch (Exception e) {
    on.onError(e);
   }
  }
 }
 public void check() {
  boolean async=this.async;
  if (async) {
   LongAdder la=io;
   la.decrement();
   if (la.sum() >= 0)return;
   if (flist == null) {
    on.onClose();
    return;
   }
   clearList(false);
  }
  if (!async || io.sum() < 0) {
   try {
    zipout.close();
   } catch (Exception e) {
    on.onError(e);
   }
   on.onClose();
  }
 }
 public void close() throws Exception {
  if (!async|| flist != null)check();
 }
 public boolean cancel() {
  Vector<Future> list=flist;
  if (list == null)return false;
  this.flist = null;
  for (Future fu:list)
   fu.cancel(true);
  zipout.cancel();
  clearList(true);
  return true;
 }
 public void addTask(DeflateWriter run) {
  io.increment();
  flist.add(pool.submit(run));
 }
 public static ExecutorService pool=Executors.newWorkStealingPool();
 public volatile Vector<Future> flist;
 public ConcurrentLinkedDeque list;
 public AtomicBoolean wrok;
 public LongAdder io;
 public ZipEntryOutput zipout;
 public boolean async;
 public ErrorHandler on;
 public ParallelDeflate(ZipEntryOutput out, boolean async) {
  zipout = out;
  if (async) {
   list = new ConcurrentLinkedDeque();
   flist = new Vector();
   wrok = new AtomicBoolean();
   this.async = async;
   io = new LongAdder();
  }
 }
 public FileOrBufOutput write(IoWriter io, boolean iswrok, ZipEntryM zip) throws Exception {
  FileOrBufOutput is=null;
  try {
   if (io.out == null) {
    int size=io.bufSize;
    BufIo wt;
    if (!iswrok && wrok.getAndSet(true))wt = is = new FileOrBufOutput(bufSize(size));
    else wt = zipout.outBuf;
    io.out = new DeflateOutput(this, zip, wt);
   }
   io.flush();
  } finally {
   WritableByteChannel out= io.out;
   if (out instanceof DeflateOutput) {
    DeflateOutput def=(DeflateOutput)out;
    if (def.iswrok()) is = null;
   }
  }
  return is;
 }
 public void with(IoWriter io, ZipEntryM zip) throws Exception {
  ZipEntryOutput zipout=this.zipout;
  if (zip.mode <= 0 || !async)
   io.out = zip.mode <= 0 && zip.notFix ?new NoClose(zipout): zipout.outDef;
  if (!async) {
   zipout.putEntry(zip);
   io.flush();
  } else addTask(new DeflateWriter(io, zip));
 }
 public BufIo toZip(BufIo out, ZipEntryM zip) throws IOException {
  ZipEntryOutput zipput=zipout;
  if (out instanceof FileOrBufOutput && !wrok.getAndSet(true)) {
   zipput.putEntry(zip, true);
   FileOrBufOutput file=((FileOrBufOutput)out);
   try {
    file.writeTo(zipput);
   } finally {
    file.close();
   }
   return zipput.outBuf;
  }
  return out;
 }
 public void writeToZip(File file, ZipEntryM zip) throws IOException {
  if (!async) {
   ZipEntryOutput data=zipout;
   data.putEntry(zip);
   addFile(file, data, zip);
  } else {
   addTask(new DeflateWriter(file, zip));
  }
 }
 public static BufIo unIo(WritableByteChannel wt) {
  if (wt instanceof ZipEntryOutput) {
   return ((ZipEntryOutput)wt).outBuf;
  } else return (BufIo)wt;
 }
 public boolean addFile(File file, WritableByteChannel out, ZipEntryM zip) throws IOException {
  FileChannel nio=new FileInputStream(file).getChannel();
  long size=nio.size();
  if (zip.mode > 0) {
   DeflateOutput dio=new DeflateOutput(this, zip, unIo(out));
   try {
    nio.transferTo(0, size, dio);
   } finally {
    dio.close();
    nio.close();
   }
   return dio.iswrok();
  } else {
   ZipEntryOutput data= zipout;
   try {
    WritableByteChannel wt;
    boolean fixSize;
    if (zip.notFix) {
     wt = data.getNio();
     fixSize = true;
    } else {
     wt = data.outDef;
     fixSize = false;
    }
    nio.transferTo(0, size, wt);
    if (fixSize)data.upLength(size);
   } finally {
    nio.close();
   }
   return true;
  }
 }
 public boolean deflate(InputStream in, WritableByteChannel out, ZipEntryM zip) throws IOException {
  int len=Math.min(in.available(), 8192);
  byte[] buf = new byte[len];
  DeflateOutput def=new DeflateOutput(this, zip, unIo(out));
  int i;
  try {
   while ((i = in.read(buf)) > 0)
    def.write(buf, 0, i);
  } finally {
   in.close();
   def.close();
  }
  return def.iswrok();
 }
 public static void fixEntry(libDeflate def, LibdeflateCRC32 crc, ZipEntryM zip) {
  if (crc != null)zip.crc = (int)crc.getValue();
  zip.size = def.rby;
  zip.csize = def.wby;
 }
 public byte[] copybuf;
 public byte[] getBuf() {
  byte buf[]=copybuf;
  if (buf == null)copybuf = buf = new byte[16384];
  return buf;
 }
 public void copyIo(InputStream in, ZipEntryM zip) throws IOException {
  ZipEntryOutput out=zipout;
  byte buf[]=getBuf();
  int i;
  LibdeflateCRC32 crc=!zip.notFix ?new LibdeflateCRC32(): null;
  try {
   while ((i = in.read(buf)) > 0) {
    if (crc != null)crc.update(buf, 0, i);
    out.write(buf, 0, i);
   }
  } finally {
   in.close();
  }
  fixEntry(out, crc, zip);
 }
 public static void fixEntry(ZipEntryOutput out, LibdeflateCRC32 crc, ZipEntryM zip) {
  int size = (int)(out.off - out.last);
  if (zip.mode <= 0) {
   if (crc != null)zip.crc = (int)crc.getValue();
   zip.size = size;
  }
  zip.csize = size;
 }
 public void copyToZip(InputStream in, ZipEntryM zip) throws IOException {
  zipout.putEntry(zip, true);
  copyIo(in, zip);
 }
 public void copyToZip(InputGet ing, ZipEntryM zip) throws IOException {
  addTask(new DeflateWriter(ing, zip, true));
 }
 public void writeToZip(InputStream in, ZipEntryM zip) throws IOException {
  ZipEntryOutput out=zipout;
  boolean def=zip.mode <= 0;
  out.putEntry(zip, def);
  if (def)copyIo(in, zip);
  else {
   ZipEntryOutput.DeflaterIo defio=out.outDef;
   byte buf[]=getBuf();
   int i;
   while ((i = in.read(buf)) > 0)
    defio.write(buf, 0, i);
  }
 }
 public void writeToZip(InputGet ing, ZipEntryM zip) throws IOException {
  addTask(new DeflateWriter(ing, zip, zip.mode <= 0));
 }
}
