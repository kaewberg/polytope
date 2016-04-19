package se.pp.forsberg.polytope.test;

import static java.lang.Math.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class TestFold {
  
  
  public static void main(String[] arguments) {
    System.out.println("Tetrahedron, 70.528779 degrees");
    fold3(Math.PI/3, Math.PI/3, Math.PI/3);
    System.out.println("Cube, 90 degrees");
    fold3(Math.PI/2, Math.PI/2, Math.PI/2);
    System.out.println("Triangular prism");
    fold3(Math.PI/2, Math.PI/3, Math.PI/2);
    System.out.println("Triangle square pentagon");
    fold3(Math.PI/3, Math.PI/2, 3*Math.PI/5);
  }

  private static void fold3(double v1, double v2, double v3) {
    double cv1 = Math.cos(v1), cv2 = Math.cos(v2), cv3 = Math.cos(v3);
    double sv1 = Math.sin(v1), sv2 = Math.sin(v2), sv3 = Math.sin(v3);
    
    double f1f2 = acos((cv3 - cv1*cv2)/(sv1*sv2));
    double f2f3 = acos((cv1 - cv2*cv3)/(sv2*sv3));
    double f3f1 = acos((cv2 - cv3*cv1)/(sv3*sv1));
    
    System.out.println("Fold polygons f1, f2, f3 with angles " + a(v1) + ", " + a(v2) + ", " + a(v3) + " around a point -> dihedral angles ");
    System.out.println("  f1f2 = "+ a(f1f2));
    System.out.println("  f2f3 = "+ a(f2f3));
    System.out.println("  f3f1 = "+ a(f3f1));
  }

  private static NumberFormat numberFormat = new DecimalFormat("###.#°");
  private static String a(double v) {
    return numberFormat.format(v*180/PI);
  }
}
