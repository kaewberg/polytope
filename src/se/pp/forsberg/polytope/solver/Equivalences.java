package se.pp.forsberg.polytope.solver;

import java.util.HashMap;
import java.util.Map;

public class Equivalences {
  public Map<Polytope, Polytope> p1p2;
  public Map<Polytope, Polytope> p2p1;
  
  public Equivalences() {
    p1p2 = new HashMap<Polytope, Polytope>();
    p2p1 = new HashMap<Polytope, Polytope>();
  }
  
  
  public Equivalences(Equivalences equivalences) {
    p1p2 = new HashMap<Polytope, Polytope>(equivalences.p1p2);
    p2p1 = new HashMap<Polytope, Polytope>(equivalences.p2p1);
  }

  public void add(Polytope p1, Polytope p2) {
    p1p2.put(p1,  p2);
    p2p1.put(p2, p1);
  }
  
  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (Polytope p1: p1p2.keySet()) {
      stringBuilder.append(p1).append(" <=>\n").append(p1p2.get(p1));
    }
    return stringBuilder.toString();
  }
}
