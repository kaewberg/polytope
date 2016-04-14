package se.pp.forsberg.polytope.test;

import static org.junit.Assert.assertEquals;
import static se.pp.forsberg.polytope.AffineTransform.W;
import static se.pp.forsberg.polytope.AffineTransform.X;
import static se.pp.forsberg.polytope.AffineTransform.Y;
import static se.pp.forsberg.polytope.AffineTransform.Z;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.junit.Test;

import se.pp.forsberg.polytope.AffineTransform;
import se.pp.forsberg.polytope.Edge;
import se.pp.forsberg.polytope.Point;
import se.pp.forsberg.polytope.Polytope;
import se.pp.forsberg.polytope.Vertex;

public class TestPolytope {
  
  public static void main(String[] args) {
    try {
      testSimplex();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unused")
  private static void testTetrahedron() {
    //           v1
    //           e4
    //     e3    v4    e1
    //        e6     e5
    // v3        e2          v2
    
    Polytope v1 = Polytope.getVertex();
    Polytope v2 = Polytope.getVertex();
    Polytope v3 = Polytope.getVertex();
    Polytope v4 = Polytope.getVertex();
    
    Polytope e1 = Polytope.get(v1, v2);
    Polytope e2 = Polytope.get(v2, v3);
    Polytope e3 = Polytope.get(v3, v1);
    Polytope e4 = Polytope.get(v1, v4);
    Polytope e5 = Polytope.get(v2, v4);
    Polytope e6 = Polytope.get(v3, v4);
    
    Polytope f1 = Polytope.get(e1, e2, e3);
    Polytope f2 = Polytope.get(e1, e4, e5);
    Polytope f3 = Polytope.get(e2, e5, e6);
    Polytope f4 = Polytope.get(e3, e4, e6);
    
    Polytope tetrahedon = Polytope.get(f1, f2, f3, f4);
    
    System.out.println("Tetrahedron");
    System.out.println(tetrahedon);
  }
  
  @SuppressWarnings("unused")
  private static void testCube() {
    //  v1       e1        v2
    //    e9            e10
    //      v5   e5    v6
    //  e4                 e2
    //      e8         e6
    //      v8   e7    v7
    //   e12             e11
    //  v4       e3        v3
    
    Polytope v1 = Polytope.get(0,0,0);
    Polytope v2 = Polytope.get(0,1,0);
    Polytope v3 = Polytope.get(1,1,0);
    Polytope v4 = Polytope.get(1,0,0);
    Polytope v5 = Polytope.get(0,0,1);
    Polytope v6 = Polytope.get(0,1,1);
    Polytope v7 = Polytope.get(1,1,1);
    Polytope v8 = Polytope.get(1,0,1);
    
    Polytope e1 = Polytope.get(v1, v2);
    Polytope e2 = Polytope.get(v2, v3);
    Polytope e3 = Polytope.get(v3, v4);
    Polytope e4 = Polytope.get(v4, v1);
    Polytope e5 = Polytope.get(v5, v6);
    Polytope e6 = Polytope.get(v6, v7);
    Polytope e7 = Polytope.get(v7, v8);
    Polytope e8 = Polytope.get(v8, v5);
    Polytope e9 = Polytope.get(v1, v5);
    Polytope e10 = Polytope.get(v2, v6);
    Polytope e11 = Polytope.get(v3, v7);
    Polytope e12 = Polytope.get(v4, v8);
    
    Polytope f1 = Polytope.get(e1, e2, e3, e4);
    Polytope f2 = Polytope.get(e5, e6, e7, e8);
    Polytope f3 = Polytope.get(e1, e10, e5, e9);
    Polytope f4 = Polytope.get(e2, e11, e6, e10);
    Polytope f5 = Polytope.get(e3, e12, e7, e11);
    Polytope f6 = Polytope.get(e4, e9,  e8, e12);
    
    Polytope cube = Polytope.get(f1, f2, f3, f4, f5, f6);
    
    System.out.println("Cube");
    System.out.println(cube);
//    Point p = new Point(-0.5, -0.5, -0.5);
//    cube.translate(p);
    cube.rotate(Y, Z, Math.PI/4);
    System.out.println(cube);
  }
  
  private static void show(final Polytope p) throws HeadlessException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    
    JFrame frame = new JFrame() {
      private static final long serialVersionUID = 1L;
      private final AffineTransform transform;
      private final PolytopeModel polytope = new PolytopeModel(p);
      {
        transform = AffineTransform.getIdentityTransform();
        double v = 0.0013;
        for (int d1 = 0; d1 < p.getDimensions()-1; d1++) {
          for (int d2 = d1+1; d2 < p.getDimensions(); d2++) {
            for (int i = 0; i < p.getDimensions()-1; i++) {
              transform.rotate(d1, d2, v);
              v += 0.0003;
            }
          }
        }
        
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPolytope jP = new JPolytope(polytope);
        jP.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        getContentPane().add(jP, BorderLayout.CENTER);
        setTitle("Test polytopes");
        pack();
        setLocationRelativeTo(null);
        new Timer(20, new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            polytope.transform(transform);
          }
        }).start();
      }
    };
    frame.setVisible(true);
  }
  
  @SuppressWarnings("unused")
  private static void testConnect() throws HeadlessException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    // Square
    Polytope v1 = Polytope.get(0,0);
    Polytope v2 = Polytope.get(0,1);
    Polytope v3 = Polytope.get(1,1);
    Polytope v4 = Polytope.get(1,0);
    Polytope e1 = Polytope.get(v1, v2);
    Polytope e2 = Polytope.get(v2, v3);
    Polytope e3 = Polytope.get(v3, v4);
    Polytope e4 = Polytope.get(v4, v1);
    Polytope f1 = Polytope.get(e1, e2, e3, e4);
    
    Polytope c1 = Polytope.getEmpty(3);
    c1.connect(f1);
    Polytope f2 = f1.realClone();
    f2.rotate(X, Z, Math.PI/2);
    c1.connect(f2);
    Polytope f3 = f1.realClone();
    f3.rotate(Y, Z, Math.PI/2);
    c1.connect(f3);
    Polytope f4 = f1.realClone();
    f4.translate(0, 0, 1);
    c1.connect(f4);
    Polytope f5 = f2.realClone();
    f5.translate(1, 0, 0);
    c1.connect(f5);
    Polytope f6 = f3.realClone();
    f6.translate(0, 1, 0);
    c1.connect(f6);
    c1.validate();
    c1.translate(-0.5, -0.5, -0.5, -0.5);
    c1.scale(2, 2, 2, 2);

    final Polytope tesseract = Polytope.getEmpty(4);
    tesseract.connect(c1);
    Polytope c2 = c1.realClone();
    c2.scale(1, 1, 1, -1);
    tesseract.connect(c2);
    
    Polytope c3 = c1.realClone();
    c3.rotate(X, W, Math.PI/2);
    tesseract.connect(c3);
    Polytope c4 = c3.realClone();
    c4.scale(-1, 1, 1, 1);
    tesseract.connect(c4);
    
    Polytope c5 = c1.realClone();
    c5.rotate(Y, W, Math.PI/2);
    tesseract.connect(c5);
    Polytope c6 = c5.realClone();
    c6.scale(1, -1, 1, 1);
    tesseract.connect(c6);
    Polytope c7 = c1.realClone();
    c7.rotate(Z, W, Math.PI/2);
    tesseract.connect(c7);
    Polytope c8 = c7.realClone();
    c8.scale(1, 1, -1, 1);
    tesseract.connect(c8);
    tesseract.validate();
    
    System.out.println(tesseract);
    tesseract.scale(100, 100, 100, 100);
    show(tesseract);
  }
  
  @Test
  public void testVertex() {
    Polytope p1 = Polytope.get(1.0, 0.0, 1.0);
    Polytope p2 = Polytope.get(1.0, 6.123233995736766E-17, 1.0);
    assertEquals(p1, p2);
    assertEquals(p1.hashCode(), p2.hashCode());
  }
  
  @SuppressWarnings("unused")
  private static void testRotate() throws HeadlessException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    Vertex v1 = Polytope.get(0, 0);
    Vertex v2 = Polytope.get(1, 0);
    Edge e1 = (Edge) Polytope.get(v1, v2); // (0,0)-(1,0)
    Polytope f1 = Polytope.getEmpty(2);
    f1.add(e1);
    Edge e2 = (Edge) f1.copyAndRotate(e1, v1, Math.PI/2); // (0,0)-(0,1)
    Edge e3 = (Edge) f1.copyAndRotate(e1, v2, Math.PI/2); // (1,0)-(1,1)
    Edge e4 = (Edge) f1.close();
    
    Polytope c1 = Polytope.getEmpty(3);
    c1.add(f1);
    Polytope f2 = c1.copyAndRotate(f1, e1, Math.PI/2);
    Polytope f3 = c1.copyAndRotate(f1, e2, Math.PI/2);
    Polytope f4 = c1.copyAndRotate(f1, e3, Math.PI/2);
    Polytope f5 = c1.copyAndRotate(f1, e4, Math.PI/2);
    Polytope f6 = c1.close();
    
    Polytope tesseract = Polytope.getEmpty(4);
    tesseract.add(c1);
    Polytope c2 = tesseract.copyAndRotate(c1, f1, Math.PI/2);
    Polytope c3 = tesseract.copyAndRotate(c1, f2, Math.PI/2);
    Polytope c4 = tesseract.copyAndRotate(c1, f3, Math.PI/2);
    Polytope c5 = tesseract.copyAndRotate(c1, f4, Math.PI/2);
    Polytope c6 = tesseract.copyAndRotate(c1, f5, Math.PI/2);
    Polytope c7 = tesseract.copyAndRotate(c1, f6, Math.PI/2);
    tesseract.close();

    System.out.println(tesseract);
    c1.translate(-0.5, -0.5, -0.5, -0.5);
    tesseract.scale(200, 200, 200, 200);
    show(tesseract);
  }
  static void testSimplex() throws HeadlessException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    final double TRIANGLE_ANGLE = Math.acos(1/2);
    final double TETRAHEDRON_ANGLE = Math.acos(1/3);
    final double SIMPLEX_ANGLE = Math.acos(1/4);
    
    Vertex v1 = Polytope.get(0, 0);
    Vertex v2 = Polytope.get(1, 0);
    Edge e1 = (Edge) Polytope.get(v1, v2); // (0,0)-(1,0)
    Polytope f1 = Polytope.getEmpty(2);
    f1.add(e1);
    Edge e2 = (Edge) f1.copyAndRotate(e1, v1, TRIANGLE_ANGLE);
    Edge e3 = (Edge) f1.close();
    
    Polytope c1 = Polytope.getEmpty(3);
    c1.add(f1);
    Polytope f2 = c1.copyAndRotate(f1, e1, TETRAHEDRON_ANGLE);
    Polytope f3 = c1.copyAndRotate(f1, e2, TETRAHEDRON_ANGLE);
    Polytope f4 = c1.close();
    
    Polytope simplex = Polytope.getEmpty(4);
    simplex.add(c1);
    Polytope c2 = simplex.copyAndRotate(c1, f1, SIMPLEX_ANGLE);
    Polytope c3 = simplex.copyAndRotate(c1, f1, SIMPLEX_ANGLE);
    Polytope c4 = simplex.copyAndRotate(c1, f1, SIMPLEX_ANGLE);
    Polytope c5 = simplex.close();
    simplex.center();
    simplex.scale(200, 200, 200, 200);
    show(simplex);
    
  }
  
  @Test
  public void testIdentityTransform() {
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    assertEqual(getMatrix(t1), getMatrix(t2));
  }
  @Test
  public void testTranslateTransform() {
    double tx = -0.3, ty = 0.3;
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    t1.translate(tx, ty);
    t2.translate(tx, ty);
    assertEqual(getMatrix(t1), getMatrix(t2));
  }
  @Test
  public void testScaleTransform() {
    double sx = 1.5, sy = -2.5;
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    t1.scale(sx, sy);
    t2.scale(sx, sy);
    assertEqual(getMatrix(t1), getMatrix(t2));
  }
  @Test
  public void testRotateTransform() {
    double theta = -Math.PI/6;
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    t1.rotate(theta);
    t2.rotate(theta);
    assertEqual(getMatrix(t1), getMatrix(t2));
  }
  @Test
  public void testCompoundTransform() {
    double tx = -0.3, ty = 0.3;
    double sx = 1.5, sy = -2.5;
    double theta = -Math.PI/6;
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    t1.translate(tx, ty);
    t2.translate(tx, ty);
    t1.scale(sx, sy);
    t2.scale(sx, sy);
    t1.rotate(theta);
    t2.rotate(theta);
    assertEqual(getMatrix(t1), getMatrix(t2));
  }
  @Test
  public void testIdentityTransformation() {
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    double x = 0.23, y = -0.15;
    Point2D p1 = new Point2D.Double(x, y);
    Point p2 = new Point(x, y);
    assertEqual(getArray(t1.transform(p1, null)), getArray(t2.transform(p2)));
  }

  @Test
  public void testTranslateTransformation() {
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    double x = 0.23, y = -0.15;
    double tx = -0.3, ty = 0.3;
    Point2D p1 = new Point2D.Double(x, y);
    Point p2 = new Point(x, y);
    t1.translate(tx, ty);
    t2.translate(tx, ty);
    assertEqual(getArray(t1.transform(p1, null)), getArray(t2.transform(p2)));
  }
  @Test
  public void testScaleTransformation() {
    double sx = 1.5, sy = -2.5;
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    t1.scale(sx, sy);
    t2.scale(sx, sy);
    double x = 0.23, y = -0.15;
    Point2D p1 = new Point2D.Double(x, y);
    Point p2 = new Point(x, y);
    assertEqual(getArray(t1.transform(p1, null)), getArray(t2.transform(p2)));
  }
  @Test
  public void testRotateTransformation() {
    double theta = -Math.PI/6;
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    t1.rotate(theta);
    t2.rotate(theta);
    double x = 0.23, y = -0.15;
    Point2D p1 = new Point2D.Double(x, y);
    Point p2 = new Point(x, y);
    assertEqual(getArray(t1.transform(p1, null)), getArray(t2.transform(p2)));
  }
  @Test
  public void testCompoundTransformation() {
    double tx = -0.3, ty = 0.3;
    double sx = 1.5, sy = -2.5;
    double theta = -Math.PI/6;
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    t1.translate(tx, ty);
    t2.translate(tx, ty);
    t1.scale(sx, sy);
    t2.scale(sx, sy);
    t1.rotate(theta);
    t2.rotate(theta);
    double x = 0.23, y = -0.15;
    Point2D p1 = new Point2D.Double(x, y);
    Point p2 = new Point(x, y);
    assertEqual(getArray(t1.transform(p1, null)), getArray(t2.transform(p2)));
  }
  
  @Test
  public void testRotateAbout1() {
    double x0 = 0, y0 = 0, x1 = Math.sqrt(0.5), y1 = Math.sqrt(0.5);
    AffineTransform t = new AffineTransform();
    t.translate(Math.sqrt(0.5), Math.sqrt(0.5));
    t.rotate(-Math.PI/4);
    t.translate(-Math.sqrt(0.5), -Math.sqrt(0.5));;
    double[] result = flatten(t.transform(x0, y0), t.transform(x1, y1));

    assertEqual(new double[]{ Math.sqrt(0.5)-1, Math.sqrt(0.5), Math.sqrt(0.5), Math.sqrt(0.5) }, result);
  }
  @Test
  public void testRotateAbout2() {
    double x0 = 0, y0 = 0, x1 = Math.sqrt(0.5), y1 = Math.sqrt(0.5);
    Polytope v0 = Polytope.get(x0, y0);
    Polytope v1 = Polytope.get(x1, y1);
    Polytope e0 = Polytope.get(v0, v1);
    Polytope f0 = Polytope.getEmpty(2);
    Edge e1 = (Edge) f0.copyAndRotate(e0, v1, -Math.PI/4);
    f0.add(e1);
    Vertex v2 = e1.getVertex1();
    Vertex v3;
    if (v2.equals(v1)) {
      v3 = e1.getVertex1();
      v2 = e1.getVertex2();
    } else {
      v3 = e1.getVertex2();
    }
    double[] result = flatten(v2.getCoordinates().getCoordinates(), v3.getCoordinates().getCoordinates());

    assertEqual(new double[]{ Math.sqrt(0.5)-1, Math.sqrt(0.5), Math.sqrt(0.5), Math.sqrt(0.5) }, result);
  }
  @Test
  public void testInvert() throws NoninvertibleTransformException {
    double tx = -0.3, ty = 0.3;
    double sx = 1.5, sy = -2.5;
    double theta = -Math.PI/6;
    java.awt.geom.AffineTransform t1 = new java.awt.geom.AffineTransform();
    AffineTransform t2 = new AffineTransform();
    t1.translate(tx, ty);
    t2.translate(tx, ty);
    t1.scale(sx, sy);
    t2.scale(sx, sy);
    t1.rotate(theta);
    t2.rotate(theta);
    t1.invert();
    t2.invert();
    assertEqual(getMatrix(t1), getMatrix(t2));
  }

  private double[] flatten(double[]... arrays) {
    int length = 0;
    for (double[] array: arrays) {
      length += array.length;
    }
    double[] result = new double[length];
    int offset = 0;
    for (double[] array: arrays) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

  private double[] getArray(Point2D p) {
    return new double[] { p.getX(), p.getY() };
  }

  private double[] getArray(Point p) {
    return new double[] { p.get(0), p.get(1) };
  }

  private double[][] getMatrix(java.awt.geom.AffineTransform t) {
    return new double[][] {
      { 1, 0, 0 },
      { t.getTranslateX(), t.getScaleX(), t.getShearX() },
      { t.getTranslateY(), t.getShearY(), t.getScaleY() }};
  }

  private double[][] getMatrix(AffineTransform t) {
    return new double[][] {
      { t.get(0,0), t.get(0,1), t.get(0,2) },
      { t.get(1,0), t.get(1,1), t.get(1,2) },
      { t.get(2,0), t.get(2,1), t.get(2,2) }};
  }
  private static double EPSILON = 0.000000001;
  private static void assertEqual(double[][] m1, double[][]m2) {
    for (int y = 0; y < 2; y++) {
      for (int x = 0; x < 2; x++) {
        if (Math.abs(m1[y][x] - m2[y][x]) > EPSILON) {
          System.out.println(toString(m1) + "\n!=\n" + toString(m2) + "\n");
          assertEquals(toString(m1), toString(m2));
          throw new IllegalArgumentException("Should not happen");
        }
      }
    }
    // DEBUG
//    System.out.println(toString(m1) + "\n=\n" + toString(m2) + "\n");
  }
  private static void assertEqual(double[] a1, double[] a2) {
    for (int i = 0; i < 2; i++) {
      if (Math.abs(a1[i] - a2[i]) > EPSILON) {
        System.out.println(toString(a1) + "\n!=\n" + toString(a2) + "\n");
        assertEquals(toString(a1), toString(a2));
        throw new IllegalArgumentException("Should not happen");
      }
    }
    // DEBUG
//    System.out.println(toString(a1) + "\n=\n" + toString(a2) + "\n");
  }

  private static String toString(double[][] m) {
    StringBuilder result = new StringBuilder();
    result.append('[');
    for (int y = 0; y < m.length; y++) {
      result.append('[');
      for (int x = 0; x < m[y].length; x++) {
        if (x > 0) {
          result.append(',');
        }
        result.append(m[y][x]);
      }
      result.append(']');
    }
    result.append(']');
    return result.toString();
  }
  private static String toString(double[] a) {
    StringBuilder result = new StringBuilder();
    result.append('[');
    for (int x = 0; x < a.length; x++) {
      if (x > 0) {
        result.append(',');
      }
      result.append(a[x]);
    }
    result.append(']');
    return result.toString();
  }
}
