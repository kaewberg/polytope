package se.pp.forsberg.polytope.solver;

import java.util.Iterator;

public class Edge extends Polytope {

  private final double length; 
  
  public Edge(Vertex v1, Vertex v2) {
    this(v1, v2, 1.0);
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
    Iterator<Polytope> it = facets.iterator();
    Edge e = new Edge((Vertex) it.next().copy(), (Vertex) it.next().copy(), length);
    copyCommon(e);
    return e;
  }
  public Vertex getVertex() {
    return (Vertex) facets.iterator().next();
  }
}
