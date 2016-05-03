package se.pp.forsberg.polytope.solver;

import static java.lang.Math.PI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.pp.forsberg.polytope.solver.WorkInProgress.FacetChain;

class WorkInProgress extends Polytope {

  // Facets around an corner
  class FacetChain {
    Polytope corner;
    List<Polytope> facets = new ArrayList<Polytope>();
    List<Polytope> ridges = new ArrayList<Polytope>();
    Map<Polytope, Polytope> equivalences;
    double angularSum = 0;

    public FacetChain(Polytope corner) {
      this.corner = corner;
    }

    public boolean add(Polytope ridge, Polytope facet) {
      double angle = facet.getAngle(ridge).getAngle();
      if (angularSum + angle > 2 * PI) {
        return false;
      }
      ridges.add(ridge);
      facets.add(facet);
      angularSum += angle;
      return true;
    }
    public Polytope firstRidge() {
      return ridges.get(0);
    }
    public Polytope lastRidge() {
      // Last facet
      Polytope facet2 = facets.get(facets.size() - 1);
      // Connected ridge of last facet
      Polytope ridge2 = ridges.get(ridges.size() - 1);
      // Unconnected ridge of last facet
      return facet2.facets.stream().filter(ridge -> ridge != ridge2 && ridge.facets.contains(corner)).findAny().get();
    }
    public boolean close() {
      if (facets.size() < 3) {
        return false;
      }
      // All ways to equate the two unconnected ridges 
      Optional<Map<Polytope, Polytope>> equivalences = lastRidge().waysToEquate(firstRidge())
        // Such that the common corner is fixed
        .filter(eqv -> deepEquivalent(eqv, corner))
        // There can be only one
        .findAny();
      if (!equivalences.isPresent()) {
        return false;
      }
      lastRidge().equate(equivalences.get());
      return true;
    }

    // Copy and map equivalences
    public FacetChain copy(Map<Polytope, Polytope> equivalences) {
      FacetChain result = new FacetChain(equivalent(equivalences, corner));
      for (Polytope facet: facets) {
        result.facets.add(equivalent(equivalences, facet));
      }
      for (Polytope ridge: ridges) {
        result.ridges.add(equivalent(equivalences, ridge));
      }
      result.angularSum = angularSum;
      return result;
    }
    // Simple copy
    public FacetChain copy() {
      FacetChain result = new FacetChain(corner);
      result.facets.addAll(facets);
      result.ridges.addAll(ridges);
      result.angularSum = angularSum;
      return result;
    }
  }
  Map<Polytope, FacetChain> finishedCorners = new HashMap<Polytope, FacetChain>();
  
  public WorkInProgress(int n) {
    super(n);
  }
  // Check that polytope maps to itself in an equivalence map, as well as all subpolytopes
  public static boolean deepEquivalent(Map<Polytope, Polytope> equivalences, Polytope p) {
    if (equivalences.get(p) != p) {
      return false;
    }
    for (Polytope facet: p.facets) {
      if (!deepEquivalent(equivalences, facet)) {
        return false;
      }
    }
    return true;
  }
  public static Polytope equivalent(Map<Polytope, Polytope> equivalences, Polytope p) {
    Polytope equivalent = equivalences.get(p);
    return equivalent == null? p : equivalent;
  }
  public WorkInProgress(FacetChain facetChain) {
    super(facetChain.facets.get(0).n + 1);
    finishedCorners.put(facetChain.corner, facetChain);
  }

  public Stream<Polytope> unfinishedCorners() {
    return facets.stream().flatMap(
        facet -> facet.facets.stream().flatMap(
            ridge -> ridge.facets.stream().filter(
                corner -> !finishedCorners.containsKey(corner))));
  }

  public Map<Polytope, Set<Polytope>> getRidgeToFacetMap() {
    Map<Polytope, Set<Polytope>> result = new HashMap<Polytope, Set<Polytope>>();
    for (Polytope facet: facets) {
      for (Polytope ridge: facet.facets) {
        Set<Polytope> ridgeFacets = result.get(ridge);
        if (ridgeFacets == null) {
          ridgeFacets = new HashSet<Polytope>();
          result.put(ridge, ridgeFacets);
        }
        ridgeFacets.add(facet);
      }
    }
    return result;
  }

  // Find all facets around this corner,
  // return list of
  // 1) facet angles
  // 2) dihedral angles between facets
  public class DihedralAngle {
    public DihedralAngle(Polytope ridge, Set<Polytope> facets, Angle angle) {
      this.ridge = ridge;
      Iterator<Polytope> it = facets.iterator();
      facet1 = it.next();
      facet2 = it.next();
      this.angle = angle;
    }
    public Polytope ridge;
    public Polytope facet1, facet2;
    public Angle angle;
  }
  public class CornerAngles {
    public CornerAngles(Polytope corner) {
      this.corner = corner;
    }
    public Polytope corner;
    public Map<Polytope, Angle> facetAngles = new HashMap<Polytope, Angle>();
    public Map<Polytope, DihedralAngle> dihedralAngles = new HashMap<Polytope, DihedralAngle>();
    
    public List<Polytope> getFacetChain() {
      if (facetAngles.isEmpty()) {
        return null;
      }
      List<Polytope> result = new ArrayList<Polytope>();
      Polytope first;
      Polytope last;
      Polytope lastLast = null;
      first = last = facetAngles.keySet().iterator().next();
      while (last != first || result.isEmpty()) {
        Polytope p = null;
        DihedralAngle v = null; 
        for (DihedralAngle dihedralAngle: dihedralAngles.values()) {
          if (dihedralAngle.facet1 == last && dihedralAngle.facet2 != lastLast) {
            p = dihedralAngle.facet2;
            v = dihedralAngle;
            break;
          } else if (dihedralAngle.facet2 == last && dihedralAngle.facet1 != lastLast) {
            p = dihedralAngle.facet1;
            v = dihedralAngle;
            break;
          }
        }
        if (p == null) {
          return null;
        }
        result.add(p);
        result.add(v.ridge);
        lastLast = last;
        last = p;
      }
      return result;
    }
    
    @Override
    public String toString() {
      List<Polytope> facetChain = getFacetChain();
      if (facetChain == null) {
        return "<Invalid facet chain>";
      }
      StringBuilder facetAnglesSb = new StringBuilder("");
      StringBuilder dihedralAnglesSb = new StringBuilder("");
      for (int i = 0; i < facetChain.size(); i += 2) {
        facetAnglesSb.append("v").append(i+1).append(" = ").append(facetAngles.get(facetChain.get(i))).append("\n");
        int j = (i + 1) % facetChain.size();
        dihedralAnglesSb.append("v").append(i+1).append(j+1).append(" = ").append(dihedralAngles.get(facetChain.get(i+1)).angle);
      }
      return facetAnglesSb.toString() + dihedralAnglesSb.toString();
    }
  }
  // By corner I mean a (n-3) face 
  public CornerAngles getAngles(Polytope corner) {
    CornerAngles result = new CornerAngles(corner);
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = getRidgeToFacetMap();
    Set<Polytope> neighboringRidges = ridgeToFacetMap.keySet().stream().filter(ridge -> ridge.facets.contains(corner)).collect(Collectors.toSet());
    Set<Polytope> neighboringFacets = new HashSet<Polytope>();
    neighboringRidges.stream().forEach(ridge -> {
      Set<Polytope> facets = ridgeToFacetMap.get(ridge);
      if (facets.size() == 2) {
        result.dihedralAngles.put(ridge, new DihedralAngle(ridge, facets, getAngle(ridge)));
      }
      neighboringFacets.addAll(facets);
    });
    neighboringFacets.stream().forEach(facet -> result.facetAngles.put(facet, facet.getAngle(corner)));
    return result;
  }
  /**
   * A polytope is valid (ie. closed) if each ridge belongs to exactly two facets
   * AND all facets are connected to each other (recursively)
   */
  public void validate() {
    if (facets.size() < 3) {
      throw new IllegalArgumentException("Invalid polytope");
    }
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = getRidgeToFacetMap();
    for (Polytope ridge: ridgeToFacetMap.keySet()) {
      if (ridgeToFacetMap.get(ridge).size() != 2) {
        throw new IllegalArgumentException("Unclosed ridge " + ridge);
      }
    }
    Set<Polytope> reachableFacets = new HashSet<Polytope>();
    collectConnectedFacets(facets.iterator().next(), reachableFacets);
    if (!reachableFacets.equals(facets)) {
      throw new IllegalArgumentException("Disconnected polytope");
    }
  }
  /**
   * Create a set of all facets reachable from the specified facet by moving to neighbors
   * along the ridges. 
   * @param facet Starting face
   * @param ridgeToFacetMap Map of ridge to neighboring facets
   * @param reachableFacets Return value, all facets (recursively) reachable from facet.
   */
  private void collectConnectedFacets(Polytope facet, Set<Polytope> reachableFacets) {
    if (reachableFacets.contains(facet)) {
      return;
    }
    reachableFacets.add(facet);
    for (Polytope ridge: facet.facets) {
      for (Polytope neighboringFacet: getRidgeToFacetMap().get(ridge)) {
        collectConnectedFacets(neighboringFacet, reachableFacets);
      }
    }
  }
  public boolean isEmpty() {
    return facets.isEmpty();
  }
  public WorkInProgress copyWiP(Map<Polytope, Polytope> replacementMap) {
    if (replacementMap.containsKey(this)) {
      return (WorkInProgress) replacementMap.get(this);
    }
    WorkInProgress p = new WorkInProgress(n);
    p.copyCommon(p, replacementMap);
    // TODO copyWiP();
    return p;
  }

}
