package org.libDeflate;
import java.util.Vector;
import java.util.List;

public interface UIPost extends Runnable {
 public void accept(List<Throwable> err);
}
