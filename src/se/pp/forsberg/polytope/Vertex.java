package se.pp.forsberg.polytope;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class Vertex extends Polytope {
  
  private static AtomicLong serialCounter = new AtomicLong();
  private long serial;
  private Point coordinates;
  
  public Vertex(double... coordinates) {
    super(0);
    if (coordinates.length > 0) {
      this.coordinates = new Point(coordinates); 
    }
    serial = serialCounter.getAndIncrement();
  }

  @Override
  public int hashCode() {
    if (coordinates == null) {
      return System.identityHashCode(this);
    } else {
      return coordinates.hashCode();
    }
  }
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Polytope)) {
      return false;
    }
    return compareTo((Polytope) other) == 0;
  }
  
  @Override
  public String toString() {
//    StringBuilder result = new StringBuilder();
//    toString(0, result);
//    return result.toString();
    if (coordinates == null) {
      return "<v" + serial + " - unset>";
    } else {
      return coordinates.toString();
    }
  }
  
  @Override
  protected void toString(int i, StringBuilder result) {
    int digits = (int) Math.ceil(Math.log10(serialCounter.get()));
    if (i > 0) {
      result.append(String.format("%" + i + "sv%0" + digits + "d", "", serial));
    } else {
      result.append(String.format("v%0" + digits + "d", serial));
    }
  }
  
  @Override
  public int compareTo(Polytope o) {
    if (!(o instanceof Vertex)) {
      return super.compareTo(o);
    }
    Vertex other = (Vertex) o;
    if (coordinates == null) {
      if (other.coordinates == null) {
        return new Long(serial).compareTo(other.serial);
      }
      return -1;
    }
    if (other.coordinates == null) {
      return 1;
    }
    return coordinates.compareTo(other.coordinates);
  }
  
  @Override
  public void transform(AffineTransform t) {
    if (coordinates != null) {
      coordinates.transform(t);
    }
  }
  
  @Override
  protected void collectVertices(Set<Vertex> vertices) {
//    if (vertices.contains(this)) {
//      for (Vertex vertex: vertices) {
//        if (vertex.equals(this)) {
//          if (vertex != this) {
//            System.err.println("!");
//          }
//          break;
//        }
//      }
//    }
    vertices.add(this);
  }

  public Point getCoordinates() {
    return coordinates;
  }
  
  @Override
  protected Polytope realClone(java.util.Map<Polytope,Polytope> alreadyCloned, java.util.Set<Polytope> except) {
    if (coordinates == null) {
      return new Vertex();
    } else {
      return new Vertex(coordinates.getCoordinates());
    }
  }

//  public double[] getCoordinatesAsArray(int n) {
//    if (coordinates == null) {
//      return null;
//    }
//    if (coordinates.getDimensions() > n) {
//      throw new IllegalArgumentException("Too many dimensions");
//    }
//    double[] result = new double[n];
//    for (int i = 0; i < n; i ++) {
//      result[i] = coordinates.getCoordinate(i);
//    }
//    return result;
//  }
}
