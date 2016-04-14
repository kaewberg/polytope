package se.pp.forsberg.polytope;

import java.util.Collection;

/**
 * An any-dimensional affine transform.
 * Unlike classic affine transform, the constant part (for translations)
 * is kept in the first column
 * @author k287750
 */
public class AffineTransform implements Cloneable {
  Matrix m = new Matrix();
  
  /**
   *  Constants for use in the rotate method, ie rotate(X, Y, Math.PI/4);
   *  rotates the transform in the x/y plane
   */
  public final static int X = 0, Y = 1, Z = 2, W = 3;
  
  /**
   * Create identity transform
   */
  public AffineTransform() {
  }

  /**
   * Convenience factory method to get identity transform
   */
  public static AffineTransform getIdentityTransform() {
   return new AffineTransform();
  }
  
  /**
   * Create a translation matrix. Unlike school book example the constant part is
   * kept in the first column rather than the last, because we need to handle a 
   * variable number of dimensions.
   * <pre>
   * [1 0 0]
   * [x 1 0]
   * [y 0 1]
   *        ...
   * </pre>
   * @param distances Array of values to translate by [x, y, z, ...] 
   */
  public static AffineTransform getTranslateInstance(double... distances) {
    AffineTransform result = new AffineTransform();
    // We only need to set the elements that differ from identity matrix
    for (int i = distances.length-1; i >= 0; i--) {
      result.m.set(i+1, 0, distances[i]);
    }
    return result;
  }
  /**
   * Create a scale matrix.
   * <pre>
   * [1 0 0]
   * [0 x 0]
   * [0 0 y]
   *        ...
   * </pre>
   * @param scales Array of values to scale by [x, y, z, ...] 
   */
  public static AffineTransform getScaleInstance(double... scales) {
    AffineTransform result = new AffineTransform();
    for (int i = scales.length; i > 0; i--) {
      result.m.set(i, i, scales[i-1]);
    }
    return result;
  }
  /**
   * Create a rotational matrix
   * Examples:
   * <pre>
   * [1   0      0]
   * [1 cosv -sinv]  2d case, x/y plane
   * [0 sinv  cosv]
   *
   * [1 0    0     0]
   * [0 1    0     0]
   * [0 0 cosv -sinv]   3d y/z plane (about x axis)
   * [0 0 sinv  cosv]
   *
   * [1    0 0     0]
   * [0 cosv 0 -sinv]
   * [0    0 1     0]    3d x/z plane (about y axis)
   * [0 sinv 0  cosv]
   *
   * [1    0    0  0]
   * [0 cosv -sinv 0]
   * [0 sinv  cosv 0]    3d x/y plane (about z axis)
   * [0    0     0 1]
   *
   * [1    0 0 0     0]
   * [0 cosv 0 0 -sinv]
   * [0    0 1 0     0]    4d x/w plane
   * [0    0 0 1     0]
   * [0 sinv 0 0  cosv]
   * </pre>
   * @param dimension1 First axis of plane of rotation
   * @param dimension2 Second axis of plane of rotation
   * @param v Angle to rotate (radians)
   */
  public static AffineTransform getRotateInstance(int dimension1, int dimension2, double v) {
    AffineTransform result = new AffineTransform();
    if (dimension2 < dimension1) {
      result.m.set(dimension1+1, dimension1+1, Math.cos(v));
      result.m.set(dimension1+1, dimension2+1, -Math.sin(v));
      result.m.set(dimension2+1, dimension1+1, Math.sin(v));
      result.m.set(dimension2+1, dimension2+1, Math.cos(v));
    } else {
      result.m.set(dimension2+1, dimension2+1, Math.cos(v));
      result.m.set(dimension2+1, dimension1+1, Math.sin(v));
      result.m.set(dimension1+1, dimension2+1, -Math.sin(v));
      result.m.set(dimension1+1, dimension1+1, Math.cos(v)); 
    }
    return result;
  }
  /**
   * Concatenate with another transform
   * @param t
   */
  public AffineTransform concatenate(AffineTransform t) {
    m.concatenate(t.m);
    return this;
  }
  
  public AffineTransform translate(double... distances) {
    concatenate(getTranslateInstance(distances));
    return this;
  }
  public AffineTransform scale(double... scales) {
    concatenate(getScaleInstance(scales));
    return this;
  }
  public AffineTransform rotate(int dimension1, int dimension2, double v) {
    concatenate(getRotateInstance(dimension1, dimension2, v));
    return this;
  }
  /**
   * 2d rotation for compability
   * @param v
   */
  public AffineTransform rotate(double v) {
    return rotate(X, Y, v);
  }
  
  @Override
  protected Object clone() throws CloneNotSupportedException {
    return realClone();
  }
  AffineTransform realClone() {
    AffineTransform result = new AffineTransform();
    result.m = m.realClone();
    return result;
  }

  //              [1]
  //              [x]
  //              [y]
  //
  // [1 0 0]      [1]
  // [m a b] [ax + by + m]
  // [n c d] [cx + dy + n]     [-x y z]
  //
  // [[0.0 0.0], [1.0, 1.0], [0.0, 1.0], [1.0 0.0]]
  //
  //                            (1)
  //                            [1]
  //                            [1]
  //                            [0]
  // [1,000 0,000 0,000  0,000] (1)
  // [0,000 0,000 0,000 -1,000] [0]
  // [0,000 0,000 1,000  0,000] [1]
  // [0,000 1,000 0,000  0,000] [1]
  /**
   * Transform a set of coordinates
   * @param coordinates
   * @return
   */
  public double[] transform(double... coordinates) {
    double[] result = new double[(int) Math.max(coordinates.length, m.nominalSize()-1)];
    for (int y = 1; y <= result.length; y++) {
      for (int x = 0; x <= result.length; x++) {
        result[y-1] += m.get(y, x) * ((x == 0)? 1 : ((x <= coordinates.length)? coordinates[x-1] : 0));
      }
    }
    return result;
  }
  /**
   * Invert the transform
   * @return 
   */
  public AffineTransform invert() {
    m.invert();
    return this;
  }

  /**
   * Transform a list of objects
   * @param polyopes
   */
  public void transform(Collection<? extends Polytope> polyopes) {
    for (Polytope p: polyopes) {
      p.transform(this);
    }
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder(m.toString());
    int d = m.nominalSize()-1;
    if (d < 2) {
      d = 2;
    }
    double v1[] = new double[d];
    double v2[] = new double[d];
    v2[1] = 1;
    stringBuilder.append("\nExample transform: (");
    for (int i = 0; i < d; i++) {
      if (i > 0) {
        stringBuilder.append(" ");
      }
      stringBuilder.append(String.format("%.3f", v1[i]));
    }
    stringBuilder.append(")-(");
    for (int i = 0; i < d; i++) {
      if (i > 0) {
        stringBuilder.append(" ");
      }
      stringBuilder.append(String.format("%.3f", v2[i]));
    }
    stringBuilder.append(") -> (");
    v1 = transform(v1);
    v2 = transform(v2);
    for (int i = 0; i < d; i++) {
      if (i > 0) {
        stringBuilder.append(" ");
      }
      stringBuilder.append(String.format("%.3f", v1[i]));
    }
    stringBuilder.append(")-(");
    for (int i = 0; i < d; i++) {
      if (i > 0) {
        stringBuilder.append(" ");
      }
      stringBuilder.append(String.format("%.3f", v2[i]));
    }
    stringBuilder.append(")");
    return stringBuilder.toString();
  }

  public double get(int y, int x) {
    return m.get(y, x);
  }

  public Point transform(Point p2) {
    return new Point(transform(p2.getCoordinates()));
  }
}
