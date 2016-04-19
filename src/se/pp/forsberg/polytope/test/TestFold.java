package se.pp.forsberg.polytope.test;

import static java.lang.Math.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class TestFold {
  
  
  public static void main(String[] arguments) {
    System.out.println("Cube, 90 degrees");
    fold3(PI/2, PI/2, PI/2);
    System.out.println("\nTriangular prism");
    fold3(PI/2, PI/3, PI/2);
    System.out.println("\nTriangle square pentagon");
    fold3(PI/3, PI/2, 3*PI/5);
    
    System.out.println("\nTetrahedron (3-simplex), 70.528779 degrees");
    fold3(PI/3, PI/3, PI/3);
    System.out.println("\n5-cell (4-simplex), " + a(acos(1.0/4)));
    fold3(acos(1.0/3), acos(1.0/3), acos(1.0/3));
    System.out.println("\n5-simplex, " + a(acos(1.0/5)));
    fold3(acos(1.0/4), acos(1.0/4), acos(1.0/4));
    System.out.println("\n6-simplex, " + a(acos(1.0/6)));
    fold3(acos(1.0/5), acos(1.0/5), acos(1.0/5));
  }

  private static void fold3(double v1, double v2, double v3) {
    double cv1 = cos(v1), cv2 = cos(v2), cv3 = cos(v3);
    double sv1 = sin(v1), sv2 = sin(v2), sv3 = sin(v3);
    
    double p1p2 = acos((cv3 - cv1*cv2)/(sv1*sv2));
    double p2p3 = acos((cv1 - cv2*cv3)/(sv2*sv3));
    double p3p1 = acos((cv2 - cv3*cv1)/(sv3*sv1));
    
    System.out.println("Fold polytopes p1, p2, p3 with angles " + a(v1) + ", " + a(v2) + ", " + a(v3) + " around a common sub-ridge -> dihedral angles ");
    System.out.println("  p1p2 = "+ a(p1p2));
    System.out.println("  p2p3 = "+ a(p2p3));
    System.out.println("  p3p1 = "+ a(p3p1));
    
    // If all angles are common
    // For instance tetrahedron
    // v2 = acos((cos(v) - cos^2(v))/sin^2(v));
    // Reused in 4d
    // v3 = acos((cos(v2) - cos^2(v2))/sin^2(v2));
    // v3 = acos((cos(acos((cos(v) - cos^2(v))/sin^2(v))) - cos^2(acos((cos(v) - cos^2(v))/sin^2(v))))/sin^2(acos((cos(v) - cos^2(v))/sin^2(v))));
    // v3 = acos(((cos(v) - cos^2(v))*csc^2(v) - (cos(v) - cos^2(v))^2*csc^4(v)) / (1 - (cos(v) - cos^2(v))^2*csc^4(v)))
  }

  private static NumberFormat numberFormat = new DecimalFormat("###.#°");
  private static String a(double v) {
    return numberFormat.format(v*180/PI);
  }
}
