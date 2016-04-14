package se.pp.forsberg.polytope;

import java.util.Arrays;

public class Point implements Comparable<Point>{
  private static final int INVEPSILON = 1000000;
  private double coordinates[];
  
  public Point(double... coordinates) {
    this.coordinates = coordinates;
    normalize();
  }
  
  public double[] getCoordinates() {
    return coordinates;
  }
  public double get(int dimension) {
    return (dimension >= coordinates.length)? 0:coordinates[dimension];
  }
  
  @Override
  public String toString() {
    return Arrays.toString(coordinates);
  }

  public int getDimensions() {
    return coordinates.length;
  }
  //       [x]
  //       [y]
  // [a b] [ax + bx]
  // [c d] [cx + by]
  public void transform(AffineTransform t) {
    coordinates = t.transform(coordinates);
    normalize();
  }

  private void normalize() {
    int d = 1;
    for (int i = 0; i < coordinates.length; i++) {
      if (coordinates[i] != 0) {
        d = i+1;
      }
    }
    if (d < coordinates.length) {
      double c[] = new double[d];
      System.arraycopy(coordinates, 0, c, 0, c.length);
      coordinates = c;
    }
  }

  public double getCoordinate(int dimension) {
    if (dimension >= coordinates.length) {
      return 0;
    }
    return coordinates[dimension];
  }
  public double distance(Point p) {
    int d = getDimensions();
    if (p.getDimensions() > d) {
      d = p.getDimensions();
    }
    double sq = 0;
    for (int i = 0; i < d; i++) {
      double d1 = getCoordinate(i);
      double d2 = p.getCoordinate(i);
      sq += (d1-d2)*(d1-d2);
    }
    return Math.sqrt(sq);
  }

  @Override
  public int compareTo(Point other) {
    int d = getDimensions();
    if (other.getDimensions() > d) {
      d = other.getDimensions();
    }
    for (int i = 0; i < d; i++) {
      int diff = (int) (Math.abs(getCoordinate(i) - other.getCoordinate(i)) * INVEPSILON + 0.5);
      if (diff != 0) {
        return new Double(getCoordinate(i)).compareTo(other.getCoordinate(i));
      }
    }
    return 0;
  }
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Point)) {
      return false;
    }
    return compareTo((Point) other) == 0;
  }
  @Override
  public int hashCode() {
    int result = 0;
    for (double d: coordinates) {
      result ^= new Integer((int) (d*INVEPSILON + 0.5)).hashCode();
    }
    return result;
  }
}
