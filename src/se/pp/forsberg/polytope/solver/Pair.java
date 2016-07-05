package se.pp.forsberg.polytope.solver;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class Pair<T> {
  public final T t1;
  public final T t2;
  
  Pair(T t1, T t2) {
    this.t1 = t1;
    this.t2 = t2;
  }
  
  public Pair(Collection<T> c) {
    if (c.size() != 2) {
      throw new IllegalArgumentException("Bad pair, not 2");
    }
    Iterator<T> it = c.iterator();
    t1 = it.next();
    t2 = it.next();
  }

  public T getFirst(Comparator<T> comparator) {
    if (comparator.compare(t1, t2) <= 0) {
      return t1;
    } else {
      return t2;
    }
  }
  public T getSecond(Comparator<T> comparator) {
    if (comparator.compare(t1, t2) <= 0) {
      return t2;
    } else {
      return t1;
    }
  }
  
  @Override
  public int hashCode() {
    return t1.hashCode() ^ t2.hashCode();
  }
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Pair<?>)) return false;
    Pair<?> p = (Pair<?>) obj;
    return t1.equals(p.t1) && t2.equals(p.t2) ||
        t1.equals(p.t2) && t2.equals(p.t1);
  }
  
}
