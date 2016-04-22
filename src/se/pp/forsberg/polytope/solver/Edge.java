package se.pp.forsberg.polytope.solver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Edge extends Polytope {

  private final double length; 
  
  public Edge(Vertex v1, Vertex v2) {
    this(v1, v2, 1.0);
    setName("Edge");
  }
  public Edge(Vertex v1, Vertex v2, double length) {
    super(1);
    add(v1);
    add(v2);
    this.length = length;
  }

  public Edge() {
    this(new Vertex(), new Vertex());
  }
  @Override
  public Polytope copy() {
    return copyEdge();
  }
  public Edge copyEdge() {
    return copyEdge(new HashMap<Polytope, Polytope>());
  }
  @Override
  public Polytope copy(Map<Polytope, Polytope> replacementMap) {
    return copyEdge(replacementMap);
  }
  private Edge copyEdge(Map<Polytope, Polytope> replacementMap) {
    if (replacementMap.containsKey(this)) {
      return (Edge) replacementMap.get(this);
    }
    Iterator<Polytope> it = facets.iterator();
    Edge e = new Edge((Vertex) it.next().copy(replacementMap), (Vertex) it.next().copy(replacementMap), length);
    replacementMap.put(this, e);
    copyCommon(e);
    return e;
  }
  public Vertex getVertex() {
    return (Vertex) facets.iterator().next();
  }
  
  @Override
  protected Map<Polytope, Polytope> findEquivalencies(Polytope p) {
    if (!(p instanceof Edge)) {
      throw new IllegalArgumentException("Not equivalent");
    }
    Iterator<Polytope> it1 = facets.iterator();
    Iterator<Polytope> it2 = p.facets.iterator();
    Map<Polytope, Polytope> result = new HashMap<Polytope, Polytope>();
    result.put(it1.next(), it2.next());
    result.put(it1.next(), it2.next());
    result.put(this, p);
    
    return result;
  }
  public Vertex getOtherVertex(Vertex v) {
    return (Vertex) getComponent(p -> p.n == 0 && p != v);
  }
  
  @Override
  protected boolean anchor(Polytope other, Map<Polytope, Polytope> equivalences) {
    Iterator<Polytope> it = facets.iterator(); 
    Vertex v1 = (Vertex) it.next();
    Vertex v2 = (Vertex) it.next();
    if (equivalences.containsKey(v1)) {
      Vertex othersVertex = (Vertex) equivalences.get(v1);
      Edge otherEdge = (Edge) other.getComponent(
          e -> !equivalences.containsValue(e) && e.facets.contains(othersVertex));
      Iterator<Polytope> it2 = otherEdge.facets.iterator();
      Vertex v12 = (Vertex) it2.next();
      Vertex v22 = (Vertex) it2.next();
      if (equivalences.get(v1) == v12) {
        equivalences.put(v2, v22);
      } else {
        equivalences.put(v2, v12);
      }
    } else if (equivalences.containsKey(v2)) {
      Vertex othersVertex = (Vertex) equivalences.get(v2);
      Edge otherEdge = (Edge) other.getComponent(
          e -> !equivalences.containsValue(e) && e.facets.contains(othersVertex));
      Iterator<Polytope> it2 = otherEdge.facets.iterator();
      Vertex v12 = (Vertex) it2.next();
      Vertex v22 = (Vertex) it2.next();
      if (equivalences.get(v2) == v12) {
        equivalences.put(v1, v22);
      } else {
        equivalences.put(v1, v12);
      }
    } else {
      return false;
    }
    equivalences.put(this, other);
    return true;
  }

  public void equate(Vertex v1, Vertex v2) {
    if (!facets.contains(v1)) {
      throw new IllegalArgumentException("No such vertex");
    }
    facets.remove(v1);
    facets.add(v2);
  }
  
}
