package se.pp.forsberg.polytope.solver;

import static java.lang.Math.PI;

import java.util.ArrayList;
import java.util.Collections;
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
//    Map<Polytope, Polytope> equivalences;
    double angularSum = 0;

    private FacetChain(Polytope corner) {
      if (!WorkInProgress.this.stream().anyMatch(polytope -> polytope == corner) || corner == null) {
        throw new IllegalArgumentException("Polytope does not contain corner");
      }
      this.corner = corner;
    }
    WorkInProgress getWorkInProgress() {
      return WorkInProgress.this;
    }

    public boolean add(Polytope ridge, Polytope facet) {
      if (ridge == null || facet == null) {
        throw new IllegalArgumentException("Null ridge or corner");
      }
      if (!facet.ridgeAngles.containsKey(corner) || corner == null) {
        throw new IllegalArgumentException("facet does not contain corner");
      }
      double angle = facet.getAngle(corner).getAngle();
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
      Optional<Equivalences> equivalences = lastRidge().waysToEquate(firstRidge())
        // Such that the common corner is fixed
        .filter(eqv -> deepEquivalent(eqv.p1p2, corner))
        // There can be only one
        .findAny();
      if (!equivalences.isPresent()) {
        return false;
      }
      // We must not equate any corners that are already part of a single solved facet
      Map<Polytope, Polytope> p1p2 = equivalences.get().p1p2;
      for (Polytope p1: p1p2.keySet()) {
        Polytope p2 = p1p2.get(p1);
        if (finishedCorners.values().stream()
           .flatMap(chain -> chain.facets.stream())
           .map(facet -> facet.stream().filter(p -> p.n == n-2).collect(Collectors.toSet()))
           .anyMatch(corners -> corners.contains(p1) && corners.contains(p2))) {
          System.out.println("Skipping equate, would destroy other facet");
          return false;
        }
      }
//      FacetChain before = copyWorkInProgress();
      WorkInProgress.this.check();
      for (Polytope facet: facets) {
        facet.equate(equivalences.get().p1p2);
      }
      WorkInProgress.this.check();
      return true;
    }
    public boolean canClose() {
      if (facets.size() < 3) {
        return false;
      }
      // All ways to equate the two unconnected ridges 
      Optional<Equivalences> equivalences = lastRidge().waysToEquate(firstRidge())
        // Such that the common corner is fixed
        .filter(eqv -> deepEquivalent(eqv.p1p2, corner))
        // There can be only one
        .findAny();
      if (!equivalences.isPresent()) {
        return false;
      }
      return true;
    }
    
//    // Copy and map equivalences
//    public FacetChain copy(Map<Polytope, Polytope> equivalences) {
//      FacetChain result = new FacetChain(equivalent(equivalences, corner));
//      for (Polytope facet: facets) {
//        result.facets.add(equivalent(equivalences, facet));
//      }
//      for (Polytope ridge: ridges) {
//        result.ridges.add(equivalent(equivalences, ridge));
//      }
//      result.angularSum = angularSum;
//      return result;
//    }
    // Extended copy, also copy the entire enclosing WorkInProgress to make sure we can't modify 
    // any of the old polytopes
    public FacetChain copyWorkInProgress() {
      Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
      return copyWorkInProgress(replacements);
    }
    public FacetChain copyWorkInProgress(Map<Polytope, Polytope> replacements) {
      if (!replacements.isEmpty()) {
        throw new IllegalArgumentException("Attemtpt to sneak in replacements");
      }
      WorkInProgress wiP = WorkInProgress.this.copyWiP(replacements);
      // This may not be a finished corner 
      FacetChain result = wiP.finishedCorners.get(replacements.get(corner));
      if (result != null) {
        return result;
      }
      result = wiP.new FacetChain(replacements.get(corner));
      for (Polytope facet: facets) {
        if (replacements.containsKey(facet)) {
          result.facets.add(replacements.get(facet));
        } else {
          result.facets.add(facet.copy(replacements));
        }
      }
      for (Polytope ridge: ridges) {
        if (replacements.containsKey(ridge)) {
          result.ridges.add(replacements.get(ridge));
        } else {
          result.ridges.add(ridge.copy(replacements));
        }
      }
      result.angularSum = angularSum;
      return result;
    }
//    // Simple copy
//    public FacetChain copy() {
//      FacetChain result = new FacetChain(corner);
//      result.facets.addAll(facets);
//      result.ridges.addAll(ridges);
//      result.angularSum = angularSum;
//      return result;
//    }
    @Override
    public String toString() {
      String names = this.facets.stream().map(facet -> facet.getName()).collect(Collectors.toList()).toString();
      return corner.toString().trim() + ' ' + names;
    }

  }
  public Map<Polytope, FacetChain> finishedCorners = new HashMap<Polytope, FacetChain>();
  
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
//  public static Polytope equivalent(Map<Polytope, Polytope> equivalences, Polytope p) {
//    Polytope equivalent = equivalences.get(p);
//    if (equivalent == null) {
//      p.equate(equivalences);
//      return p;
////      return p.copy(equivalences);
//    }
//    return equivalent;
//  }
  public WorkInProgress(FacetChain facetChain) {
    super(facetChain.facets.get(0).n + 1);
    finishedCorners.put(facetChain.corner, facetChain);
  }

  public WorkInProgress(Polytope p) {
    super(p.n);
    p.copyCommon(this, new HashMap<Polytope, Polytope>());
  }
  public Stream<Polytope> unfinishedCorners() {
    return facets.stream().flatMap(
        facet -> facet.facets.stream().flatMap(
            ridge -> ridge.facets.stream().filter(
                corner -> !finishedCorners.containsKey(corner))));
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
    check();
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
    Set<Polytope> ridges = stream().filter(ridge -> ridge.n == n-2).collect(Collectors.toSet());
    Set<Polytope> ridgesWithAngles = new HashSet<Polytope>(ridgeAngles.keySet());
    if (!ridges.equals(ridgesWithAngles)) {
      throw new IllegalArgumentException("Not all angles determined");
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
  public WorkInProgress copyWiP() {
    return copyWiP(Collections.emptyMap());
  }
  public WorkInProgress copyWiP(Map<Polytope, Polytope> replacementMap) {
    if (replacementMap.containsKey(this)) {
      return (WorkInProgress) replacementMap.get(this);
    }
    WorkInProgress p = new WorkInProgress(n);
    copyCommon(p, replacementMap);
    for (Polytope corner: finishedCorners.keySet()) {
      Polytope finishedCorner = replacementMap.get(corner);
      FacetChain chain = finishedCorners.get(corner);
      FacetChain chainCopy = p.new FacetChain(finishedCorner);
      for (Polytope facet: chain.facets) {
        chainCopy.facets.add(facet.copy(replacementMap));
      }
      for (Polytope ridge: chain.ridges) {
        chainCopy.ridges.add(ridge.copy(replacementMap));
      }
      chainCopy.angularSum = chain.angularSum;
      p.finishedCorners.put(chainCopy.corner, chainCopy);
//      p.finishedCorners.put(replacementMap.get(corner), finishedCorners.get(corner).copy(replacementMap));
    }
    return p;
  }
  // Combine any ridges containing the same corners
  public void coalesceRidges() {
    Map<Polytope, Polytope> equivalences = new HashMap<Polytope, Polytope>();
    Set<Polytope> ridges = facets.stream().flatMap(facet -> facet.facets.stream()).collect(Collectors.toSet());
    for (Polytope ridge: ridges) {
      if (equivalences.keySet().contains(ridge)) {
        continue;
      }
      facets.stream().flatMap(facet -> facet.facets.stream())
        .filter(ridge2 ->  ridge2 != ridge && ridge2.facets.equals(ridge.facets))
        .forEach(ridge2 -> equivalences.put(ridge2, ridge));
    }
    equate(equivalences);
  }
  public boolean solveAngles() {
    // Simultaneously solve all dihedral angles...
    // Example: octahedron
    //        p1
    //  p2  p3   p4  p5   
    //        p6
    // f1 = p1p2p3
    // f2 = p1p3p4
    // f3 = p1p4p5
    // f4 = p1p5p2
    // f5 = p6p2p3
    // f6 = p6p3p4
    // f7 = p6p4p5
    // f8 = p6p5p2
    //
    // general relations for three polytopes around an corner
    //
    // dihedral angles given facet angles
    // cos(v12) = (cos(v3) - cos(v1)*cos(v2))/(sin(v1)*sin(v2)))
    // cos(v23) = (cos(v1) - cos(v2)*cos(v3))/(sin(v2)*sin(v3)))
    // cos(v31) = (cos(v2) - cos(v3)*cos(v1))/(sin(v3)*sin(v1)))
    
    // facet angles given two other facet angles and one dihedral angle
    // cos(v1) = cos(v23)sin(v2)sin(v3) + cos(v2)cos(v3)
    // cos(v2) = cos(v31)sin(v3)sin(v1) + cos(v3)cos(v1)
    // cos(v3) = cos(v12)sin(v1)sin(v2) + cos(v1)cos(v2)
    //
    // Four polytopes around an corner can be decomposed into two merged instances of three around a corner
    // This corresponds to halving a baseless square pyramid
    // Given facet angles for the square pyramid with apex at p1 are v1 v2 v3 v4
    // There is an unknown top angle for the halves, t1
    // cos(v12) = (cos(t1) - cos(v1)*cos(v2))/(sin(v1)*sin(v2)))
    // cos(v2t) = (cos(v1) - cos(v2)*cos(t1))/(sin(v2)*sin(t1)))
    // cos(v1t) = (cos(v2) - cos(t1)*cos(v1))/(sin(t1)*sin(v1)))
    // cos(v34) = (cos(t1) - cos(v3)*cos(v4))/(sin(v3)*sin(v4)))
    // cos(v4t) = (cos(v3) - cos(v4)*cos(t1))/(sin(v4)*sin(t1)))
    // cos(v3t) = (cos(v4) - cos(t1)*cos(v3))/(sin(t1)*sin(v3)))
    // v23 = v2t+v3t
    // v41 = v1t+v4t
    // And we want the four dihedral angles
    // v12 v23 v34 v41. 5 unknowns, four givens
    // Form similar equation systems for all vertices
    // cos(v12) = (cos(t2) - cos(v1)*cos(v2))/(sin(v1)*sin(v2))) <-
    // cos(v2t) = (cos(v1) - cos(v2)*cos(t2))/(sin(v2)*sin(t2)))
    // cos(v1t) = (cos(v2) - cos(t2)*cos(v1))/(sin(t1)*sin(v1)))
    // cos(v65) = (cos(t2) - cos(v6)*cos(v5))/(sin(v6)*sin(v5)))
    // cos(v5t) = (cos(v6) - cos(v5)*cos(t2))/(sin(v5)*sin(t2)))
    // cos(v6t) = (cos(v5) - cos(t2)*cos(v6))/(sin(t1)*sin(v6)))
    // Marked equation establishes that t1 == t2
    // Lots of work.
    // New insight:
    // Any corner having 4+ facets around it that form a generalized baseless pyramid
    // (Example 3d: facets around a corner in an icosahedron form a baseless pentagonal pyramid)
    // can be sliced away to leave a polytope having one less corner, and fewer polytopes per corner
    // In the icosahedron example, three slices are enough to make the dihedral angles trivial to calculate.
    // This procedure should solve all the Johnsson bodies.

    // Anyways, lets first handle the special case of only three polytopes around each corner
    finishedCorners.values().stream().filter(chain -> chain.facets.size() == 3).forEach(chain -> solve3(chain));
    // Then, a stab at solving remaining corners using above angles
    finishedCorners.values().stream().filter(chain -> chain.facets.size() == 4).forEach(chain -> solve4(chain));
    validate();
    return true;
  }
 
  private void solve3(FacetChain chain) {
    Fold3Solution angles = Fold3Solution.givenFacetAngles(
        chain.facets.get(0).getAngle(chain.corner),
        chain.facets.get(1).getAngle(chain.corner),
        chain.facets.get(2).getAngle(chain.corner));
    if (!ridgeAngles.containsKey(chain.ridges.get(0))) {
      setAngle(chain.ridges.get(0), angles.v31);
    }
    if (!ridgeAngles.containsKey(chain.ridges.get(1))) {
      setAngle(chain.ridges.get(1), angles.v12);
    }
    if (!ridgeAngles.containsKey(chain.ridges.get(2))) {
      setAngle(chain.ridges.get(2), angles.v23);
    }
  }

  private void solve4(FacetChain chain) {
    // Find given dihedral angle (if any)
    if (chain.)
  }
  public FacetChain newFacetChain(Polytope corner) {
    return new FacetChain(corner);
  }
  public void addFinishedCorner(FacetChain chain) {
    if (stream().noneMatch(p -> p == chain.corner)) {
      throw new IllegalArgumentException("Polytope does not contain corner");
    }
    finishedCorners.put(chain.corner, chain);
  }

}
