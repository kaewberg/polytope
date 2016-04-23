package se.pp.forsberg.polytope.solver;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Vertex extends Polytope {

  public Vertex() {
    super(0);
    setName("Vertex");
  }
  
  @Override
  public Polytope copy() {
    return copyVertex();
  }
  @Override
  public Polytope copy(Map<Polytope, Polytope> replacementMap) {
    return copyVertex(replacementMap);
  }

  public Vertex copyVertex() {
    return copyVertex(new HashMap<Polytope, Polytope>());
  }
  public Vertex copyVertex(Map<Polytope, Polytope> replacementMap) {
    if (replacementMap.containsKey(this)) {
      return (Vertex) replacementMap.get(this);
    }
    Vertex v = new Vertex();
    replacementMap.put(this,  v);
    copyCommon(v);
    return v;
  }
  
  @Override
  protected Map<Polytope, Polytope> findEquivalencies(Polytope p) {
    if (!(p instanceof Vertex)) {
      throw new IllegalArgumentException("Not equivalent");
    }
    Map<Polytope, Polytope> result = new HashMap<Polytope, Polytope>();
    result.put(this, p);
    return result;
  }
  
  @Override
  public Stream<Map<Polytope, Polytope>> waysToEquate(Polytope other) {
    if (facets == null || other == null || n != other.n) {
      return Stream.empty();
    }
    Map<Polytope, Polytope> result = new HashMap<Polytope, Polytope>();
    result.put(this,  other);
    return Stream.of(result);
  }
  
  protected boolean anchor(Polytope other, Map<Polytope, Polytope> equivalences) {
    equivalences.put(this, other);
    return true;
  }
  
}
