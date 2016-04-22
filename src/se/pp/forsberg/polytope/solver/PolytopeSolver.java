package se.pp.forsberg.polytope.solver;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.lang.reflect.ReflectPermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.print.attribute.standard.MediaSize.Other;

import se.pp.forsberg.polytope.solver.Angle.TrinaryAngle;

public class PolytopeSolver {
  
  private List<Polytope> solved = new ArrayList<Polytope>();
  private Map<Integer, List<Polytope>> solvedByDimension = new HashMap<Integer, List<Polytope>>();
  private Map<String, Polytope> nameToPolytopeMap = new HashMap<String, Polytope>();
  
  public static void main(String[] arguments) {
    new PolytopeSolver().solve();
  }

  private void solve() {
    if (!continuePrevious()) {
      addBasic();
      //solve(3);
      fakeSolve();
    }
  }

  private boolean continuePrevious() {
    return false;
  }
  
  private void add(Polytope p, String name) {
    p.setName(name);
    p.setId(solved.size());
    List<Polytope> polytopesForDimension = solvedByDimension.get(p.getDimensions());
    if (polytopesForDimension == null) {
      polytopesForDimension = new ArrayList<Polytope>();
      solvedByDimension.put(p.getDimensions(), polytopesForDimension);
    }
    polytopesForDimension.add(p);
    solved.add(p);
    nameToPolytopeMap.put(name, p);
    System.out.println("----------------------------------------------");
    System.out.println(p);
  }

  private Polytope get(String name) {
    return nameToPolytopeMap.get(name);
  }

  private void addBasic() {
    add(new Vertex(), "Vertex");
    
    add(new Edge(copyVertex(), copyVertex()), "Edge");
    
    add(getPolygon(3), "Triangle");
    add(getPolygon(4), "Square");
    add(getPolygon(5), "Pentagon");
    add(getPolygon(6), "Hexagon");
    add(getPolygon(8), "Octagon");
    add(getPolygon(10), "Dekagon");
  }
  
  private Polytope getPolygon(int n) {
    Angle angle = new Angle.RationalPi(n-2, n); // (n-2)PI/n
    Edge e[] = new Edge[n];
    for (int i = 0; i < n; i ++) {
      e[i] = copyEdge();
    }
    Vertex first = e[0].getVertex();
    Vertex last = e[n-1].getVertex();
    e[0].equate(first, last);
    Vertex previous = last;
    for (int i = 0; i < n-1; i ++) {
      Vertex v1 = (Vertex) e[i].getOtherVertex(previous);
      Vertex v2 = (Vertex) (i == n-2? e[i+1].getOtherVertex(last) : e[i+1].getVertex());
      e[i+1].equate(v2, v1);
      previous = v1;
    }
    Polytope p = new Polytope(2);
    for (int i = 0; i < n; i ++) {
      p.add(e[i]);
    }
    last = e[0].getVertex();
    final Vertex vlast = last;
    if (e[1].containsComponent(vertex -> vertex == vlast)) {
      last = e[0].getOtherVertex(last);
    }
    for (int i = 0; i < n; i ++) {
      p.setAngle(last, angle);
      last = (Vertex) e[i].getOtherVertex(last);
    }
    return p;
  }

  private Vertex getVertex() {
    return (Vertex) solvedByDimension.get(0).get(0);
  }
  private Edge getEdge() {
    return (Edge) solvedByDimension.get(1).get(0);
  }
  private Vertex copyVertex() {
    return getVertex().copyVertex();
  }
  private Edge copyEdge() {
    return getEdge().copyEdge();
  }
  private Polytope copy(String name) {
    return get(name).copy();
  }
  
  private void fakeSolve() {
    // New version, more like final goal
    
    // Fold 3 facets < 360 around a ridge
    waysToConnect3(2).forEach(
        way -> {
        Polytope p = new Polytope(3);
        p.add(way.f1);
        p.add(way.f2);
        p.add(way.f3);
        Angle[] angles = fold3(way.f1.getAngle(way.ridge), way.f2.getAngle(way.ridge), way.f3.getAngle(way.ridge));
        Polytope f1f2 = way.f1.facets.stream().filter(ridge -> way.f2.facets.contains(ridge)).findAny().get();
        Polytope f1f3 = way.f1.facets.stream().filter(ridge -> way.f3.facets.contains(ridge)).findAny().get();
        Polytope f2f3 = way.f2.facets.stream().filter(ridge -> way.f3.facets.contains(ridge)).findAny().get();
        p.setAngle(f2f3, angles[0]);
        p.setAngle(f1f3, angles[1]);
        p.setAngle(f1f2, angles[2]);
        
        // Fill remaining holes
    });
  }
  private class WayToConnect3 {
    public Polytope f1, f2, f3;
    public Polytope ridge;
    //public Map<Polytope, Polytope> equivalences;
    public WayToConnect3(Polytope f1, Polytope f2, Polytope f3, Polytope ridge) {
      this.f1 = f1;
      this.f2 = f2;
      this.f3 = f3;
      this.ridge = ridge;
    }
  }
  private Stream<WayToConnect3> waysToConnect3(int n) {
    return solvedByDimension.get(n).stream().flatMap(
        f1 -> solvedByDimension.get(n).stream().flatMap(
            f2 -> solvedByDimension.get(n).stream().flatMap(
                f3 -> waysToConnect(f1.copy(), f2.copy(), f3.copy())
            )));
  }

  private Stream<WayToConnect3> waysToConnect(Polytope f1, Polytope f2, Polytope f3) {
    // For all ways to connect f1 to f2
    return f2.waysToConnect(f1).flatMap(
        equivalencesf2f1 -> {
        // Connect, find facet that was connected
        connectAndCopy(f2, f1, equivalencesf2f1);
        Polytope f1Copy = equivalencesf2f1.get(f1);
        Polytope f2Copy = equivalencesf2f1.get(f2);
        Polytope facetf1f2 = equivalencesf2f1.values().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
        // For each ridge in facet, find neighbor facets in f1 and f2
        return facetf1f2.facets.stream().flatMap(
            ridge -> {
            Polytope neighborInF1 = f1Copy.getOtherFacet(facetf1f2, ridge);
            Polytope neighborInF2 = f2Copy.getOtherFacet(facetf1f2, ridge);
            // Connect both ridges to  f3
            //return Stream.of(new WayToConnect3());
            return f3.waysToConnectFacets(neighborInF1, neighborInF2)
                .map(
                equivalencesf3f1f2-> {
                Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
                Polytope f3Copy = f3.copy(replacements);
                Map<Polytope, Polytope> equivalencesCopy = new HashMap<Polytope, Polytope>();
                for (Polytope p: equivalencesf3f1f2.keySet()) {
                  equivalencesCopy.put(replacements.get(p), replacements.get(equivalencesCopy.get(p)));
                }
                
                f3Copy.equate(equivalencesCopy);
                return new WayToConnect3(f1Copy, f2Copy, f3Copy, ridge);
            }).filter(way -> way.f1.getAngle(way.ridge).getAngle() +
                             way.f2.getAngle(way.ridge).getAngle() +
                             way.f3.getAngle(way.ridge).getAngle() < 2*Math.PI);
        });
    });
//        return facetf1f2.facets.stream().flatMap(
//            ridge ->
//            return new WayToConnect3();
//            )});
//            .flatMap(
//                facetf1f3 -> f3.facets.stream().flatMap(
//                    facetf3 -> facetf3.waysToEquate(facetf1f3).flatMap(
//                        equivalencesf3f1 -> {
//                          f3.equate(equivalencesf3f1);
//                          Polytope facetf1f3 = equivalences.values().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
//                          return new WayToConnect3();
//                        }
//                    )
//                )
//            )
//        });
  }

//  private void fakeSolveOld() {
//    
//    // Add three triangles around a vertex
//    Polytope f1 = copy("Triangle");
//    Polytope f2 = copy("Triangle");
//    Polytope f3 = copy("Triangle");
//    
//    // Random edge of first triangle
//    Polytope f1e1 = f1.getRandomFacet();
//    // Random vertex to fold around
//    Polytope f1e1v1 = f1e1.getRandomFacet();
//    
//    // Random edge of triangle 2 to connect to above edge 
//    Polytope f2e1 = f2.getRandomFacet();
//    // Connect edges
//    f2.equate(f2e1, f1e1);     f2e1 = f1e1;
//    
//    // The two open edges of triangle 1 and triangle 2 that adjoin the vertex
//    Polytope f1e2 = f1.getOtherFacet(f1e1, f1e1v1);
//    Polytope f2e2 = f2.getOtherFacet(f1e1, f1e1v1);
//    
//    // Two random edges of triangle 3 to connect to the above
//    Polytope f3e1 = f3.getRandomFacet();
//    Polytope f3e1v1 = f3e1.getRandomFacet();
//    Polytope f3e2 = f3.getOtherFacet(f3e1, f3e1v1);
//    // Connect
//    f3.equate(f3e1, f1e2);     f3e1 = f1e2;
//    f3.equate(f3e2, f2e2);     f3e2 = f2e2;
//    
//    // Calculate angles
//    Angle angles[] = fold3(f1.getAngle(f1e1v1), f2.getAngle(f1e1v1), f3.getAngle(f1e1v1));
//    Angle f2f3 = angles[0];
//    Angle f1f3 = angles[1];
//    Angle f1f2 = angles[2];
//    
//    // Fold
//    Polytope tetrahedron = new Polytope(3);
//    tetrahedron.add(f1);
//    tetrahedron.add(f2);
//    tetrahedron.add(f3);
//    tetrahedron.setAngle(f1e1, f1f2);
//    tetrahedron.setAngle(f1e2, f1f3);
//    tetrahedron.setAngle(f2e2, f2f3);
//    
//    // Three remaining open edges
//    Polytope f1e3 = f1.getComponent(edge -> edge.n == 1 && edge != f1e1 && edge != f1e2);
//    Polytope f2e3 = f2.getComponent(edge -> edge.n == 1 && edge != f1e1 && edge != f2e2);
//    Polytope f3e3 = f3.getComponent(edge -> edge.n == 1 && edge != f1e2 && edge != f2e2);
//    
//    // Connect
//    Polytope f4 = copy("Triangle");
//    Polytope f4e1 = f4.getRandomFacet();
//    Polytope f4e2 = f4.getComponent(edge -> edge.n == 1 && edge != f4e1);
//    Polytope e2 = f4e2;
//    Polytope f4e3 = f4.getComponent(edge -> edge.n == 1 && edge != f4e1 && edge != e2);
//    f4.equate(f4e1, f1e3);   //f4e1 = f1e3
//    Polytope f4e1v1 = f4e1.getRandomFacet();
//    if (!f2e3.containsComponent(v -> v == f4e1v1)) {
//      Polytope t = f4e2;
//      f4e2 = f4e3;
//      f4e3 = t;
//    }
//    f4.equate(f4e2, f2e3);   //f4e2 = f2e3;
//    f4.equate(f4e3, f3e3);   //f4e3 = f3e3;
//   
//    // Cheat, should be recalculated
//    Angle f1f4 = f1f2;
//    Angle f2f4 = f1f2;
//    Angle f3f4 = f1f2;
//    tetrahedron.add(f4);
//    tetrahedron.setAngle(f1e3, f1f4);
//    tetrahedron.setAngle(f2e3, f2f4);
//    tetrahedron.setAngle(f3e3, f3f4);
//    
//    add(tetrahedron, "Tetrahedron");
//    
//  }
  
  private Polytope connectAndCopy(Polytope f1, Polytope f2, Map<Polytope, Polytope> equivalencesf1f2) {
    Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
    Polytope f1Copy = f1.copy(replacements);
    Polytope f2Copy = f2.copy(replacements);
    Map<Polytope, Polytope> equivalencesf1copyf2copy = new HashMap<Polytope, Polytope>();
    for (Polytope p: equivalencesf1f2.keySet()) {
      equivalencesf1copyf2copy.put(replacements.get(p), replacements.get(equivalencesf1f2.get(p)));
    }
    equivalencesf1f2.clear();
    for (Polytope p: equivalencesf1copyf2copy.keySet()) {
      equivalencesf1f2.put(p, equivalencesf1copyf2copy.get(p));
    }
    Polytope result = new Polytope(f1.getDimensions() + 1);
    result.add(f1Copy);
    result.add(f2Copy);
    f1Copy.equate(equivalencesf1f2);
    equivalencesf1f2.put(f1, f1Copy);
    equivalencesf1f2.put(f2, f2Copy);
    return result;
  }

  private static Angle[] fold3(Angle v1, Angle v2, Angle v3) {
    TrinaryAngle.Value value = (a1, a2, a3) -> acos((cos(a3) - cos(a1)*cos(a2))/(sin(a1)*sin(a2)));
    String description = "acos((cos(%3$s) - cos(%1$s)*cos(%2$s))/(sin(%1$s)*sin(%2$s)))";
    Angle result[] = new Angle[3];
    result[0] = new TrinaryAngle(v1, v2, v3, value, description);
    result[1] = new TrinaryAngle(v2, v3, v1, value, description);
    result[2] = new TrinaryAngle(v1, v1, v2, value, description);
//    System.out.println("Fold polytopes p1, p2, p3 with angles " + a(v1.getAngle()) + ", " + a(v2.getAngle()) + ", " + a(v3.getAngle()) + " around a common sub-ridge -> dihedral angles ");
//    System.out.println("  p1p2 = "+ a(result[0].getAngle()) + ", " + result[0]);
//    System.out.println("  p2p3 = "+ a(result[1].getAngle()) + ", " + result[1]);
//    System.out.println("  p3p1 = "+ a(result[2].getAngle()) + ", " + result[2]);
    return result;
  }
}
