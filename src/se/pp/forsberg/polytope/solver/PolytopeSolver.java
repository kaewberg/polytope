package se.pp.forsberg.polytope.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolytopeSolver {
  
  private Vertex vertex;
  private Edge edge;
  private List<Polytope> solved = new ArrayList<Polytope>();
  private Map<Integer, List<Polytope>> solvedByDimension = new HashMap<Integer, List<Polytope>>();
  
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

  private void addBasic() {
    List<Polytope> zeroD = new ArrayList<Polytope>();
    vertex = new Vertex();
    vertex.setName("Vertex");
    vertex.setId(solved.size());
    solved.add(vertex);
    zeroD.add(vertex);
    solvedByDimension.put(0, zeroD);
    
    List<Polytope> oneD = new ArrayList<Polytope>();
    edge = new Edge(vertex.copyVertex(), vertex.copyVertex());
    edge.setName("Edge");
    edge.setId(solved.size());
    solved.add(edge);
    oneD.add(edge);
    solvedByDimension.put(1, zeroD);
    
    List<Polytope> twoD = new ArrayList<Polytope>();
    Polytope p = getPolygon(3);
    p.setName("Triangle");
    p.setId(solved.size());
    solved.add(p);
    twoD.add(p);
    p = getPolygon(4);
    p.setName("Square");
    p.setId(solved.size());
    solved.add(p);
    twoD.add(p);
    p = getPolygon(5);
    p.setName("Pentagon");
    p.setId(solved.size());
    solved.add(p);
    twoD.add(p);
    p = getPolygon(6);
    p.setName("Hexagon");
    p.setId(solved.size());
    solved.add(p);
    twoD.add(p);
    p = getPolygon(8);
    p.setName("Octagon");
    p.setId(solved.size());
    solved.add(p);
    twoD.add(p);
    p = getPolygon(10);
    p.setName("Dekagon");
    p.setId(solved.size());
    solved.add(p);
    twoD.add(p);
    solvedByDimension.put(2, twoD);
  }

//  private Polytope getPolygon(int i) {
//    // Isoceles triangle sides x base 1
//    // top angle 2pi/i
//    // Right triangle hypotenuse x base 0,5 
//    // Angles pi/i pi/2 pi-pi/i-pi/2=pi/2-pi/i
//    // cos(pi-pi/i)h = 0,5
//    // 0,5/sin(pi/i) = x/sin(PI/2)
//    // x = 0,5sin(pi/i)
//    Polytope p = Polytope.getEmpty(2);
//    double d = 0.5 * Math.sin(Math.PI/2);
//    for (int n = 0; n < i-1; n++) {
//      double v = n * 2*Math.PI/i;
//      p.add(Polytope.get(Math.cos(v)*d, Math.sin(v)*d));
//    }
//    p.close();
//    return p;
//  }
  
  private Polytope getPolygon(int n) {
    Angle angle = new Angle.RationalPi(n-2, n); // (n-2)PI/n
    Edge e[] = new Edge[n];
    for (int i = 0; i < n; i ++) {
      e[i] = edge.copyEdge();
    }
    Vertex first = e[0].getVertex();
    Vertex last = e[n-1].getVertex();
    e[0].equate(first, last);
    Vertex previous = last;
    for (int i = 0; i < n-1; i ++) {
      Vertex v1 = (Vertex) e[i].otherFacet(previous);
      Vertex v2 = (Vertex) (i == n-2? e[i+1].otherFacet(last) : e[i+1].getVertex());
      e[i+1].equate(v2, v1);
      previous = v1;
    }
    Polytope p = new Polytope(2);
    for (int i = 0; i < n-1; i ++) {
      p.add(e[i]);
    }
    last = e[0].getVertex();
    for (int i = 0; i < n; i ++) {
      p.setAngle(last, angle);
      last = (Vertex) e[i].otherFacet(last);
    }
    return p;
  }

  private void fakeSolve() {
    // TODO Auto-generated method stub
    
  }
}
