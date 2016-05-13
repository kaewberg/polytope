package se.pp.forsberg.polytope.solver;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import se.pp.forsberg.polytope.solver.Angle.TrinaryAngle;

// Solve three facets connected around a (n-3) face
public class Fold3Solution {
  // The three facets internal angles at (n-3) face
  public Angle v1, v2, v3;
  // The three dihedral angles between pairs of facets
  public Angle v12, v23, v31;

  // v12 given v1 v2 v3
  private static TrinaryAngle.Value valueGivenFacetAngles = (v1, v2,
      v3) -> acos((cos(v3) - cos(v1) * cos(v2)) / (sin(v1) * sin(v2)));
  private static String descriptionGivenFacetAngles = "acos((cos(%3$s) - cos(%1$s)*cos(%2$s))/(sin(%1$s)*sin(%2$s)))";
  // v3 given v1 v2 v12
  private static TrinaryAngle.Value valueGivenTwoFacetAnglesAndDihedralAngle = (v1, v2,
      v12) -> acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2));
  private static String descriptionGivenTwoFacetAnglesAndDihedralAngle = "acos(cos(%3$s)*sin(%1$s)*sin(%2$s) + cos(%1$s)*cos(%2$s))";

  private Fold3Solution() {
  }

  // Relations

  // dihedral angles given facet angles
  // cos(v12) = (cos(v3) - cos(v1)*cos(v2))/(sin(v1)*sin(v2)))
  // cos(v23) = (cos(v1) - cos(v2)*cos(v3))/(sin(v2)*sin(v3)))
  // cos(v31) = (cos(v2) - cos(v3)*cos(v1))/(sin(v3)*sin(v1)))

  // facet angles given two other facet angles and one dihedral angle
  // cos(v1) = cos(v23)sin(v2)sin(v3) + cos(v2)cos(v3)
  // cos(v2) = cos(v31)sin(v3)sin(v1) + cos(v3)cos(v1)
  // cos(v3) = cos(v12)sin(v1)sin(v2) + cos(v1)cos(v2)

  public static Fold3Solution givenFacetAngles(Angle v1, Angle v2, Angle v3) {
    Fold3Solution result = new Fold3Solution();
    result.v1 = v1;
    result.v2 = v2;
    result.v3 = v3;
    result.v12 = new TrinaryAngle(v1, v2, v3, valueGivenFacetAngles, descriptionGivenFacetAngles);
    result.v23 = new TrinaryAngle(v2, v3, v1, valueGivenFacetAngles, descriptionGivenFacetAngles);
    result.v31 = new TrinaryAngle(v1, v1, v2, valueGivenFacetAngles, descriptionGivenFacetAngles);
    return result;
  }

  public static Fold3Solution givenTwoFacetAnglesAndDihedralAngle(Angle facetAngle1, Angle facetAngle2,
      Angle dihedralAngle) {
    Fold3Solution result = new Fold3Solution();
    result.v1 = facetAngle1;
    result.v2 = facetAngle2;
    result.v12 = dihedralAngle;

    // Simplify if possible,
    // if v12 = acos((cos(a3) - cos(a1)*cos(a2))/(sin(a1)*sin(a2)))
    // and a1 == v1 && a2 == v2 || a1 == v2 && a2 == v1
    // v3 = a3
    if (result.v12 instanceof TrinaryAngle) {
      TrinaryAngle v12 = (TrinaryAngle) result.v12;
      if (v12.v1.equals(result.v1) && v12.v2.equals(result.v2)
          || v12.v1.equals(result.v2) && v12.v2.equals(result.v2)) {
        result.v3 = v12.v3;
      }
    }
    if (result.v3 == null) {
      result.v3 = new TrinaryAngle(facetAngle1, facetAngle2, dihedralAngle, valueGivenTwoFacetAnglesAndDihedralAngle,
          descriptionGivenTwoFacetAnglesAndDihedralAngle);
    }
    result.v23 = new TrinaryAngle(result.v2, result.v3, result.v1, valueGivenFacetAngles,
        descriptionGivenFacetAngles);
    result.v31 = new TrinaryAngle(result.v3, result.v1, result.v2, valueGivenFacetAngles,
        descriptionGivenFacetAngles);
    return result;
  }

  @Override
  public String toString() {
    return "v1 = " + v1 + "\nv2 = " + v2 + "\nv3 = " + v3 + "\nv12 = " + v12 + "\nv23 = " + v23 + "\nv31 = " + v31;
  }
}
