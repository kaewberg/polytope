package se.pp.forsberg.polytope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IdentityHashSet<T> implements Set<T> {
  
  private HashSet<Wrapper> set = new HashSet<Wrapper>();
  
  private class Wrapper {
    Object t;
    public Wrapper(Object t) {
      this.t = t;
    }
    @Override
    public int hashCode() {
      return System.identityHashCode(t);
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
      if (other == null || !(other instanceof IdentityHashSet.Wrapper)) {
        return false;
      }
      return t == ((Wrapper) other).t;
    }
  }

  @Override
  public boolean add(T e) {
    return set.add(new Wrapper(e));
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    List<Wrapper> c2 = new ArrayList<Wrapper>();
    for (T item: c) {
      c2.add(new Wrapper(item));
    }
    return set.addAll(c2);
  }

  @Override
  public void clear() {
    set.clear();
  }

  @Override
  public boolean contains(Object o) {
    return set.contains(new Wrapper(o));
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    List<Wrapper> c2 = new ArrayList<Wrapper>();
    for (Object item: c) {
      c2.add(new Wrapper(item));
    }
    return set.containsAll(c2);
  }

  @Override
  public boolean isEmpty() {
    return set.isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      Iterator<Wrapper> it = set.iterator();
      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @SuppressWarnings("unchecked")
      @Override
      public T next() {
        return (T) it.next().t;
      }
      
    };
  }

  @Override
  public boolean remove(Object o) {
    return set.remove(new Wrapper(o));
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    List<Wrapper> c2 = new ArrayList<Wrapper>();
    for (Object item: c) {
      c2.add(new Wrapper(item));
    }
    return set.removeAll(c2);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    List<Wrapper> c2 = new ArrayList<Wrapper>();
    for (Object item: c) {
      c2.add(new Wrapper(item));
    }
    return set.retainAll(c2);
  }

  @Override
  public int size() {
    return set.size();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object[] toArray() {
    List<T> c2 = new ArrayList<T>();
    for (Wrapper item: set) {
      c2.add((T) item.t);
    }
    return c2.toArray();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T2> T2[] toArray(T2[] a) {
    List<T2> c2 = new ArrayList<T2>();
    for (Wrapper item: set) {
      c2.add((T2) item.t);
    }
    return c2.toArray(a);
  }

}
