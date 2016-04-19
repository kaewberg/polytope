package se.pp.forsberg.polytope.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Polytope {

  protected final int n;
  protected final Set<Polytope> facets = new HashSet<Polytope>();
  protected final Map<Polytope, Angle> ridgeAngles = new HashMap<Polytope, Angle>();
  protected String name;
  protected int id = -1;
  
  public Polytope(int n) {
    this.n = n;
    this.name = "p" + n;
  }
  
  public void add(Polytope facet) {
    if (facet.n != n-1) {
      throw new IllegalArgumentException("Facet in " + n + "-polytope must be " + (n-1) + " polytope");
    }
    facets.add(facet);
  }
  public void setAngle(Polytope ridge, Angle v) {
    if (!getComponents(p -> p.n == n-2).contains(ridge)) {
      throw new IllegalArgumentException("No such ridge");
    }
    if (ridgeAngles.containsKey(ridge)) {
      throw new IllegalArgumentException("Angle already set");
    }
    ridgeAngles.put(ridge, v);
  }
  

  protected Polytope getComponent(Predicate<Polytope> predicate) {
    if (predicate.test(this)) {
      return this;
    }
    for (Polytope facet: facets) {
      Polytope result = facet.getComponent(predicate);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  protected Set<Polytope> getComponents(Predicate<Polytope> predicate) {
    return collectComponents(predicate, new HashSet<Polytope>());
  }

  protected Set<Polytope> collectComponents(Predicate<Polytope> predicate, HashSet<Polytope> result) {
    if (predicate.test(this)) {
      result.add(this);
      return result;
    }
    for (Polytope facet: facets) {
      facet.collectComponents(predicate, result);
    }
    return result;
  }

  protected Set<Polytope> getComponentsDeep(Predicate<Polytope> predicate) {
    return collectComponentsDeep(predicate, new HashSet<Polytope>());
  }

  protected Set<Polytope> collectComponentsDeep(Predicate<Polytope> predicate, HashSet<Polytope> result) {
    if (predicate.test(this)) {
      result.add(this);
    }
    for (Polytope facet: facets) {
      facet.collectComponents(predicate, result);
    }
    return result;
  }

  public void setName(String name) {
    this.name = name;
  }
  public String getName() {
    return name;
  }
  public void setId(int id) {
    this.id = id;
  }
  public int getId() {
    return id;
  }
  
  public Polytope copy() {
    Polytope p = new Polytope(n);
    for (Polytope facet: facets) {
      p.facets.add(facet.copy());
    }
    for (Polytope ridge: ridgeAngles.keySet()) {
      p.ridgeAngles.put(ridge, ridgeAngles.get(ridge));
    }
    copyCommon(p);
    return p;
  }
  protected void copyCommon(Polytope p) {
    p.name = name;
    p.id = id;
  }
  
  public Polytope otherFacet(Polytope ridge) {
    return getComponent(p -> p != this && p.facets.contains(ridge));
  }
  
  // Recursively replaces any references of component1 with component2
  // May lead to higher dimensional components also being equated
  public void equate(Polytope component1, Polytope component2) {
    if (component1.n != component2.n) {
      throw new IllegalArgumentException("Can't equate, different dimensionality");
    }
    Map<Polytope, Polytope> usedSoFar = new HashMap<Polytope, Polytope>();
    Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
    replacements.put(component1, component2);
    for (int d = component1.n; d < n; d++) {
      final int d2 = d;
      for (Polytope p: getComponents(p -> p.n == d2)) {
        Polytope previous = usedSoFar.get(p);
        if (previous != null) {
          System.out.println("Replace " + p + " with " + previous);
          replacements.put(p, previous);
        } else {
          usedSoFar.put(p, p);
          Set<Polytope> removals = new HashSet<Polytope>();
          Set<Polytope> additions = new HashSet<Polytope>();
          for (Polytope facet: p.facets) {
            Polytope replacement = replacements.get(facet);
            if (replacement != null) {
              removals.add(facet);
              additions.add(replacement);
            }
          }
          if (!removals.isEmpty() || !additions.isEmpty()) {
            p.facets.removeAll(removals);
            p.facets.addAll(additions);
          }
        }
      }
    }
  }
  
  @Override
  public String toString() {
    Map<String, Integer> nameCounter = new HashMap<String, Integer>();
    Map<Polytope, String> names = new HashMap<Polytope, String>();
    Map<Angle, String> angleNames = new HashMap<Angle, String>();
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("Polytope(").append(n).append(") ").append(id).append(" ").append(getName()).append("\n");
    for (int d = 1; d < n-1; d++) {
      final int d2 = d;
      for (Polytope p: getComponents(p -> p.n == d2)) {
        String name = p.getName();
        int count = nameCounter.containsKey(name)? nameCounter.get(name) + 1 : 0;
        String fullName = name + "-" + count;
        nameCounter.put(name, count);
        names.put(p,  fullName);
        stringBuilder.append(fullName);
        
        // Polytope(2) 3 Triangle
        //   Edge-0 Vertex-0 Vertex-1
        //   Edge-1 Vertex-1 Vertex-2
        //   Edge-2 Vertex-2 Vertex-0
        //   Angle-0 Vertex-0 Edge-0 Edge-1 PI/3
        //   Angle-1 Vertex-1 Edge-1 Edge-2 PI/3
        //   Angle-2 Vertex-2 Edge-2 Edge-0 PI/3
        
        // Polytope(3) 7 Tetrahedron
        //   Edge-0 Vertex-0 Vertex-1
        //   Edge-1 Vertex-1 Vertex-2
        //   Edge-2 Vertex-2 Vertex-0
        //   Edge-3 Vertex-0 Vertex-4
        //   Edge-4 Vertex-1 Vertex-4
        //   Edge-5 Vertex-2 Vertex-4
        //   Angle-0 Vertex-0 Edge-0 Edge-1 PI/3
        //   Angle-1 Vertex-1 Edge-1 Edge-2 PI/3
        //   Angle-2 Vertex-2 Edge-2 Edge-0 PI/3
        //   Angle-3 Vertex-0 Edge-0 Edge-4 PI/3
        //   Angle-4 Vertex-0 Edge-3 Edge-4 PI/3
        //   Angle-5 Vertex-1 Edge-1 Edge-4 PI/3
        //   Angle-6 Vertex-1 Edge-2 Edge-4 PI/3
        //   Angle-7 Vertex-2 Edge-0 Edge-5 PI/3
        //   Angle-8 Vertex-2 Edge-2 Edge-5 PI/3
        //   Triangle-0 Edge-0 Edge-1 Edge-2
        //   Triangle-1 Edge-0 Edge-1 Edge-4
        //   Triangle-2 Edge-1 Edge-2 Edge-5
        //   Triangle-3 Edge-2 Edge-0 Edge-6
        //   Angle-9 
        
        for (Polytope facet: p.facets) {
          String facetFullName = names.get(facet);
          stringBuilder.append(" (").append(facet);
          // TODO
        }
      }
    }
    return stringBuilder.toString();
  }
}
