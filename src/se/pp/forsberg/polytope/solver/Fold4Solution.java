package se.pp.forsberg.polytope.solver;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import se.pp.forsberg.polytope.solver.Angle.QuintaryAngle;
import se.pp.forsberg.polytope.solver.Angle.TrinaryAngle;

// Solve four facets connected around a (n-3) face
public class Fold4Solution {
  // The facets internal angles at (n-3) face
  public Angle v1, v2, v3, v4;
  // The dihedral angles between pairs of facets
  public Angle v12, v23, v34, v41;

  // Right, lets think a little
  // The corner can be seen as a baseless square pyramid
  /// It can be sliced down the middle creating two facet chains with 3 facets each
  // This will have one unknown top angle, v5
  // v1 v2 v5
  // v3 v4 v5
  // If we know one dihedral angle, v1v2
  // v5 = Fold3Solution.givenTwoFacetAnglesAndDihedralAngle(v1, v2, v1v2).v3
  // v5 = acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2))
  // v15 = acos((cos(v2) - cos(v1) * cos(v5)) / (sin(v1) * sin(v5)))
  // v25 = acos((cos(v1) - cos(v2) * cos(v5)) / (sin(v2) * sin(v5)))
  // v35 = acos((cos(v4) - cos(v3) * cos(v5)) / (sin(v3) * sin(v5)))
  // v45 = acos((cos(v3) - cos(v4) * cos(v5)) / (sin(v4) * sin(v5)))
  // v23 = v25 + v35
  // v41 = v15 + v45
  
  // v12 = acos((cos(v5) - cos(v1) * cos(v2)) / (sin(v1) * sin(v2))
  // v34 = acos((cos(v5) - cos(v3) * cos(v4)) / (sin(v3) * sin(v4))
  // v23 = acos((cos(v1) - cos(v2) * cos(v5)) / (sin(v2) * sin(v5)) + acos((cos(v4) - cos(v3) * cos(v5)) / (sin(v3) * sin(v5))
  // v41 = acos((cos(v2) - cos(v1) * cos(v5)) / (sin(v1) * sin(v5)) + acos((cos(v3) - cos(v4) * cos(v5)) / (sin(v4) * sin(v5))
  
  // v12 = acos((cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2))) - cos(v1) * cos(v2)) / (sin(v1) * sin(v2))
  // v34 = acos((cos(acos(cos(v12) * sin(v3) * sin(v4) + cos(v1) * cos(v2))) - cos(v3) * cos(v4)) / (sin(v3) * sin(v4))
  // v23 = acos((cos(v1) - cos(v2) * cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) / (sin(v2) * sin(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) + acos((cos(v4) - cos(v3) * cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) / (sin(v3) * sin(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2))))
  // acos((cos(v1) - cos(v2) * cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) / (sin(v2) * sin(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) + acos((cos(v4) - cos(v3) * cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) / (sin(v3) * sin(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2))))
  // v41 = acos((cos(v2) - cos(v1) * cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) / (sin(v1) * sin(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) + acos((cos(v3) - cos(v4) * cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) / (sin(v4) * sin(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2))))
  //
  // Time for Wolfram
  // v12 = v12    Well, duh
  // v34 = acos(csc(v3) csc(v4) (cos(v1) cos(v2) + cos(v12) sin(v3) sin(v4)-cos(v3) cos(v4)))
  //     = acos((cos(v1)*cos(v2) + cos(v12)*sin(v3)*sin(v4) - cos(v3)*cos(v4))/(sin(v3) * sin(v4))
  // v41 = 
  
  
  private static QuintaryAngle.Value valueGivenFacetAnglesAndNearDihedralAngle =
      (v1, v2, v3, v4, v12) ->
  acos((cos(v1) - cos(v2) * cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) / (sin(v2) * sin(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2))))) +
  acos((cos(v4) - cos(v3) * cos(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))) / (sin(v3) * sin(acos(cos(v12) * sin(v1) * sin(v2) + cos(v1) * cos(v2)))));
  
  private static String descriptionGivenFacetAnglesAndNearDihedralAngle =
      "acos((cos(%1$s) - cos(%2$s) * cos(acos(cos(%5$s) * sin(%1$s) * sin(%2$s) + cos(%1$s) * cos(%2$s)))) / (sin(%2$s) * sin(acos(cos(%5$s) * sin(%1$s) * sin(%2$s) + cos(%1$s) * cos(%2$s))))) +  acos((cos(%4$s) - cos(%3$s) * cos(acos(cos(%5$s) * sin(%1$s) * sin(%2$s) + cos(%1$s) * cos(%2$s)))) / (sin(%3$s) * sin(acos(cos(%5$s) * sin(%1$s) * sin(%2$s) + cos(%1$s) * cos(%2$s)))))";
  
  private static QuintaryAngle.Value valueGivenFacetAnglesAndOppositeDihedralAngle =
      (v1, v2, v3, v4, v12) -> 
  acos((cos(v1)*cos(v2) + cos(v12)*sin(v3)*sin(v4) - cos(v3)*cos(v4))/(sin(v3) * sin(v4)));
  
  private static String descriptionGivenFacetAnglesAndOppositeDihedralAngle =
      "acos((cos(%1$s)*cos(%2$s) + cos(%5$s)*sin(%3$s)*sin(%4$s) - cos(%3$s)*cos(%4$s))/(sin(%3$s) * sin(%4$s)))";
  
  private Fold4Solution() {
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

  public static Fold4Solution givenFacetAnglesAndOneDihedralAngle(Angle v1, Angle v2, Angle v3, Angle v4, Angle v12) {
    Fold4Solution result = new Fold4Solution();
    result.v1 = v1;
    result.v2 = v2;
    result.v3 = v3;
    result.v4 = v4;
    result.v12 = v12;
    result.v34 = new QuintaryAngle(v1, v2, v3, v4, v12, valueGivenFacetAnglesAndOppositeDihedralAngle, descriptionGivenFacetAnglesAndOppositeDihedralAngle);
    result.v23 = new QuintaryAngle(v1, v2, v3,  v4, v12, valueGivenFacetAnglesAndNearDihedralAngle, descriptionGivenFacetAnglesAndNearDihedralAngle);
    result.v41 = new QuintaryAngle(v2, v1, v3,  v2, v12, valueGivenFacetAnglesAndNearDihedralAngle, descriptionGivenFacetAnglesAndNearDihedralAngle);
    return result;
  }

  @Override
  public String toString() {
    return "v1 = " + v1 + "\nv2 = " + v2 + "\nv3 = " + v3 + "\nv4 = " + v4 + 
           "\nv12 = " + v12 + "\nv23 = " + v23 + "\nv34 = " + v34 + "\nv41 = " + v41;
  }
}
