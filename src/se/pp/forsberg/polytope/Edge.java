package se.pp.forsberg.polytope;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Edge extends Polytope {
  
  private Edge() {
    super(1);
  }
  
  Edge(Vertex vertex1, Vertex vertex2) {
    super(1);
    add(vertex1);
    add(vertex2);
    validate();
  }
  
  @Override
  public void validate() {
    if (facets.size() != 2) {
      throw new IllegalArgumentException("Edge must consist of two vertices");
    }
    Iterator<Polytope> it = facets.iterator();
    Polytope vertex1 = it.next();
    Polytope vertex2 = it.next();
    if (!(vertex1 instanceof Vertex) || !(vertex2 instanceof Vertex)) {
      throw new IllegalArgumentException("Edge must consist of two vertices");
    }
    if (vertex1.equals(vertex2)) {
      throw new IllegalArgumentException("Circular edge");
    }
  }
  
  @Override
  public void add(Polytope vertex) {
    facets.add(vertex);
  }
  
  @Override
  protected void toString(int i, StringBuilder result) {
    if (i > 0) {
      result.append(String.format("%" + i + "s", ""));
    }
    Iterator<Polytope> it = facets.iterator();
    result.append("Polytope(1){").append(it.next())
          .append(',')
          .append(it.next())
          .append('}');
  }
  
  @Override
  protected void collectEdges(Set<Edge> edges) {
    edges.add(this);
  }

  public Vertex getVertex1() {
    return (Vertex) facets.iterator().next();
  }

  public Vertex getVertex2() {
    Iterator<Polytope> it = facets.iterator();
    it.next();
    return (Vertex) it.next();
  }
  @Override
  protected Polytope realClone(java.util.Map<Polytope,Polytope> alreadyCloned, java.util.Set<Polytope> except) {
    Vertex v1 = getVertex1();
    Vertex v2 = getVertex2();
    if (alreadyCloned.containsKey(v1)) {
      v1 = (Vertex) alreadyCloned.get(v1);
    } else {
      Vertex v = v1;
      if (!except.contains(v)) {
        v = (Vertex) v1.realClone(alreadyCloned, except);
      }
      alreadyCloned.put(v1, v);
      v1 = v;
    }
    if (alreadyCloned.containsKey(v2)) {
      v2 = (Vertex) alreadyCloned.get(v2);
    } else {
      Vertex v = v2;
      if (!except.contains(v)) {
        v = (Vertex) v2.realClone(alreadyCloned, except);
      }
      alreadyCloned.put(v2, v);
      v2 = v;
    }
    return new Edge(v1, v2);
  }

  public Vertex otherVertex(Vertex v) {
    Iterator<Polytope> it = facets.iterator();
    Vertex v2 = (Vertex) it.next();
    if (v != v2) {
      return v2;
    }
    return (Vertex) it.next();
  }
}
