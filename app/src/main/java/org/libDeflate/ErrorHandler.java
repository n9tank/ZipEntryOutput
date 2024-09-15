package org.libDeflate;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.Future;
import java.util.Vector;

public abstract class ErrorHandler {
 public ParallelDeflate para;
 public boolean ignore;
 public Vector<Exception> list=new Vector();
 public ErrorHandler(ParallelDeflate para) {
  this.para = para;
 }
 public boolean onError(Exception err) { 
  list.add(err);
  return !ignore && para.cancel();
 }
 public abstract void onClose();
}
