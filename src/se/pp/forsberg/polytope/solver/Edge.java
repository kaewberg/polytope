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
  public void check() {
    if (facets.size() != 2) {
      throw new IllegalArgumentException("Edge with more than two vertices?!");
    }
    Iterator<Polytope> it = facets.iterator();
    Polytope v1 = it.next();
    Polytope v2 = it.next();
    if (v1 == null || v2 == null) {
      throw new IllegalArgumentException("Null vertex?!");
    }
    if (v1 == v2) {
      throw new IllegalArgumentException("Circular edge?!");
    }
    if (!(v1 instanceof Vertex && v2 instanceof Vertex)) {
      throw new IllegalArgumentException("Invalid dimensionality");
    }
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
  protected boolean anchor(Polytope other, Equivalences equivalences) {
    Iterator<Polytope> it = facets.iterator(); 
    Vertex v1 = (Vertex) it.next();
    Vertex v2 = (Vertex) it.next();
    if (equivalences.p1p2.containsKey(v1)) {
      Vertex othersVertex = (Vertex) equivalences.p1p2.get(v1);
      Edge otherEdge = (Edge) other.getComponent(
          e -> !equivalences.p2p1.containsKey(e) && e.facets.contains(othersVertex));
      Iterator<Polytope> it2 = otherEdge.facets.iterator();
      Vertex v12 = (Vertex) it2.next();
      Vertex v22 = (Vertex) it2.next();
      if (equivalences.p1p2.get(v1) == v12) {
        equivalences.p1p2.put(v2, v22);
        equivalences.p2p1.put(v22, v2);
      } else {
        equivalences.p1p2.put(v2, v12);
        equivalences.p2p1.put(v12, v2);
      }
    } else if (equivalences.p1p2.containsKey(v2)) {
      Vertex othersVertex = (Vertex) equivalences.p1p2.get(v2);
      Edge otherEdge = (Edge) other.getComponent(
          e -> !equivalences.p2p1.containsKey(e) && e.facets.contains(othersVertex));
      Iterator<Polytope> it2 = otherEdge.facets.iterator();
      Vertex v12 = (Vertex) it2.next();
      Vertex v22 = (Vertex) it2.next();
      if (equivalences.p1p2.get(v2) == v12) {
        equivalences.p1p2.put(v1, v22);
        equivalences.p2p1.put(v22, v1);
      } else {
        equivalences.p1p2.put(v1, v12);
        equivalences.p2p1.put(v12, v1);
      }
    } else {
      return false;
    }
    equivalences.p1p2.put(this, other);
    equivalences.p2p1.put(other, this);
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
