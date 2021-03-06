package se.pp.forsberg.polytope.test;

import static org.junit.Assert.assertEquals;
import static se.pp.forsberg.polytope.AffineTransform.W;
import static se.pp.forsberg.polytope.AffineTransform.X;
import static se.pp.forsberg.polytope.AffineTransform.Y;
import static se.pp.forsberg.polytope.AffineTransform.Z;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.junit.Test;

import se.pp.forsberg.polytope.AffineTransform;
import se.pp.forsberg.polytope.Edge;
import se.pp.forsberg.polytope.Point;
import se.pp.forsberg.polytope.Polytope;
import se.pp.forsberg.polytope.Vertex;
import se.pp.forsberg.polytope.solver.Equivalences;
import se.pp.forsberg.polytope.swing.AnimatedPolytopeModel;
import se.pp.forsberg.polytope.swing.BasicPolytopeModel;
import se.pp.forsberg.polytope.swing.JPolytope;
import se.pp.forsberg.polytope.swing.PolytopeModel;

public class TestPolytope {
  
  public static void main(String[] args) {
    try {
      testAnimation();
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
  
  private static void show(Polytope p) throws HeadlessException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    show(new BasicPolytopeModel(p));
  }
  private static void show(final PolytopeModel model) throws HeadlessException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    JFrame frame = new JFrame() {
      private static final long serialVersionUID = 1L;
      {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //JPolytope jP = new JPolytope(new SpinningPolytopeModel(model));
        JPolytope jP = new JPolytope(model);
        jP.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        getContentPane().add(jP, BorderLayout.CENTER);
        setTitle("Test polytopes");
        pack();
        setLocationRelativeTo(null);
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
  @SuppressWarnings("unused")
	static void testAnimation() throws HeadlessException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    final double TRIANGLE_ANGLE = Math.acos(1.0/2);
    final double TETRAHEDRON_ANGLE = Math.acos(1.0/3);
    final double SIMPLEX_ANGLE = Math.acos(1.0/4);
    
    PolytopeModel p = new AnimatedPolytopeModel();
    //PolytopeModel p = new BasicPolytopeModel();
    show(p);

    Polytope f1 = Polytope.getEmpty(2);
    p.setPolytope(f1);
    
    Vertex v1 = Polytope.get(-100, 0);
    Vertex v2 = Polytope.get(100, 0);
    Edge e1 = (Edge) Polytope.get(v1, v2);
    p.add(e1);
    Edge e2 = (Edge) p.copyAndRotate(e1, v1, TRIANGLE_ANGLE);
    Edge e3 = (Edge) p.close();
    
    Polytope c1 = Polytope.getEmpty(3);
    c1.add(f1);
    p.setPolytope(c1);
    Polytope f2 = p.copyAndRotate(f1, e1, TETRAHEDRON_ANGLE);
    Polytope f3 = p.copyAndRotate(f1, e2, TETRAHEDRON_ANGLE);
    Polytope f4 = p.close();
    
    Polytope simplex = Polytope.getEmpty(4);
    simplex.add(c1);
    p.setPolytope(simplex);
    Polytope c2 = p.copyAndRotate(c1, f1, SIMPLEX_ANGLE);
    Polytope c3 = p.copyAndRotate(c1, f2, SIMPLEX_ANGLE);
    Polytope c4 = p.copyAndRotate(c1, f3, SIMPLEX_ANGLE);
    Polytope c5 = p.close();
    simplex.center();
    
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
  
  @Test
  public void testEquivalence() {
    se.pp.forsberg.polytope.solver.Vertex v11 = new se.pp.forsberg.polytope.solver.Vertex();
    se.pp.forsberg.polytope.solver.Vertex v12 = new se.pp.forsberg.polytope.solver.Vertex();
    
    List<Equivalences> eqvs = v11.waysToEquate(v12).collect(Collectors.toList());
    assertEquals(1, eqvs.size());
    Map<se.pp.forsberg.polytope.solver.Polytope, se.pp.forsberg.polytope.solver.Polytope> eqv = eqvs.get(0).p1p2;
    assertEquals(1, eqv.size());
    assertEquals(v12, eqv.get(v11));

    se.pp.forsberg.polytope.solver.Vertex v21 = new se.pp.forsberg.polytope.solver.Vertex();
    se.pp.forsberg.polytope.solver.Vertex v22 = new se.pp.forsberg.polytope.solver.Vertex();
    se.pp.forsberg.polytope.solver.Edge e11 = new se.pp.forsberg.polytope.solver.Edge(v11, v21);
    se.pp.forsberg.polytope.solver.Edge e12 = new se.pp.forsberg.polytope.solver.Edge(v12, v22);
    eqvs = e11.waysToEquate(e12).collect(Collectors.toList());
    assertEquals(2, eqvs.size());
    eqvs.stream().forEach(m -> assertEquals(e12, m.p1p2.get(e11)));
    
    se.pp.forsberg.polytope.solver.Vertex v31 = new se.pp.forsberg.polytope.solver.Vertex();
    se.pp.forsberg.polytope.solver.Vertex v32 = new se.pp.forsberg.polytope.solver.Vertex();
    se.pp.forsberg.polytope.solver.Edge e21 = new se.pp.forsberg.polytope.solver.Edge(v21, v31);
    se.pp.forsberg.polytope.solver.Edge e22 = new se.pp.forsberg.polytope.solver.Edge(v22, v32);
    se.pp.forsberg.polytope.solver.Edge e31 = new se.pp.forsberg.polytope.solver.Edge(v31, v11);
    se.pp.forsberg.polytope.solver.Edge e32 = new se.pp.forsberg.polytope.solver.Edge(v32, v12);
    se.pp.forsberg.polytope.solver.Polytope f11 = new se.pp.forsberg.polytope.solver.Polytope(2);
    se.pp.forsberg.polytope.solver.Polytope f12 = new se.pp.forsberg.polytope.solver.Polytope(2);
    f11.add(e11); f11.add(e21); f11.add(e31);
    f12.add(e12); f12.add(e22); f12.add(e32);
    eqvs = f11.waysToEquate(f12).collect(Collectors.toList());
    assertEquals(6, eqvs.size());
    eqvs.stream().forEach(m ->
      assertEquals(f12, m.p1p2.get(f11))
    );
    
    se.pp.forsberg.polytope.solver.Vertex v41 = new se.pp.forsberg.polytope.solver.Vertex();
    se.pp.forsberg.polytope.solver.Vertex v42 = new se.pp.forsberg.polytope.solver.Vertex();
    se.pp.forsberg.polytope.solver.Edge e41 = new se.pp.forsberg.polytope.solver.Edge(v11, v41);
    se.pp.forsberg.polytope.solver.Edge e42 = new se.pp.forsberg.polytope.solver.Edge(v12, v42);
    se.pp.forsberg.polytope.solver.Edge e51 = new se.pp.forsberg.polytope.solver.Edge(v21, v41);
    se.pp.forsberg.polytope.solver.Edge e52 = new se.pp.forsberg.polytope.solver.Edge(v22, v42);
    se.pp.forsberg.polytope.solver.Edge e61 = new se.pp.forsberg.polytope.solver.Edge(v31, v41);
    se.pp.forsberg.polytope.solver.Edge e62 = new se.pp.forsberg.polytope.solver.Edge(v32, v42);
    se.pp.forsberg.polytope.solver.Polytope f21 = new se.pp.forsberg.polytope.solver.Polytope(2);
    se.pp.forsberg.polytope.solver.Polytope f22 = new se.pp.forsberg.polytope.solver.Polytope(2);
    f21.add(e11); f21.add(e41); f21.add(e51);
    f22.add(e12); f22.add(e42); f22.add(e52);
    se.pp.forsberg.polytope.solver.Polytope f31 = new se.pp.forsberg.polytope.solver.Polytope(2);
    se.pp.forsberg.polytope.solver.Polytope f32 = new se.pp.forsberg.polytope.solver.Polytope(2);
    f31.add(e21); f31.add(e51); f31.add(e61);
    f32.add(e22); f32.add(e52); f32.add(e62);
    se.pp.forsberg.polytope.solver.Polytope f41 = new se.pp.forsberg.polytope.solver.Polytope(2);
    se.pp.forsberg.polytope.solver.Polytope f42 = new se.pp.forsberg.polytope.solver.Polytope(2);
    f41.add(e31); f41.add(e61); f41.add(e41);
    f42.add(e32); f42.add(e62); f42.add(e42);
    se.pp.forsberg.polytope.solver.Polytope tetrahedron1 = new se.pp.forsberg.polytope.solver.Polytope(3);
    se.pp.forsberg.polytope.solver.Polytope tetrahedron2 = new se.pp.forsberg.polytope.solver.Polytope(3);
    tetrahedron1.add(f11); tetrahedron1.add(f21); tetrahedron1.add(f31); tetrahedron1.add(f41);
    tetrahedron2.add(f12); tetrahedron2.add(f22); tetrahedron2.add(f32); tetrahedron2.add(f42);
    eqvs = tetrahedron1.waysToEquate(tetrahedron2).collect(Collectors.toList());
    assertEquals(24, eqvs.size());
    eqvs.stream().forEach(m -> assertEquals(tetrahedron2, m.p1p2.get(tetrahedron1)));
  }
  
  static Spliterator.OfInt takeWhile(
      Spliterator.OfInt splitr, Predicate<Integer> predicate) {
    return new Spliterators.AbstractIntSpliterator(splitr.estimateSize(), 0) {
      boolean stillGoing = true;
      @Override public boolean tryAdvance(IntConsumer consumer) {
        if (stillGoing) {
          boolean hadNext = splitr.tryAdvance((int elem) -> {
            if (predicate.test(elem)) {
              consumer.accept(elem);
            } else {
              stillGoing = false;
            }
          });
          return hadNext && stillGoing;
        }
        return false;
      }
    };
  }
  static IntStream takeWhile(IntStream stream, Predicate<Integer> predicate) {
    return StreamSupport.intStream(takeWhile(stream.spliterator(), predicate), false);
 }

  
  public static IntStream from(int start) {
    int[] i = {start};
    return IntStream.generate(() -> i[0]++);
  }
  public static IntStream primes() {
    return from(2).filter(n -> !takeWhile(primes(), p -> p < n).anyMatch(p -> n > p && ((n % p) == 0)));
  }
  
//  @Test
//  public void testPrimes() {
//    primes().limit(10).forEach(System.out::println);
//   }

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
