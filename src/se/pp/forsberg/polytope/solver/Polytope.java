package se.pp.forsberg.polytope.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;


public class Polytope {

  protected final int n;
  protected final Set<Polytope> facets = new HashSet<Polytope>();
  protected final Map<Polytope, Angle> ridgeAngles = new HashMap<Polytope, Angle>();
  private String name;
  protected int id = -1;
  private static final boolean DEBUG = true;
  
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
    if (!stream().anyMatch(p -> p == ridge)) {
      throw new IllegalArgumentException("No such ridge");
    }
    if (ridgeAngles.containsKey(ridge)) {
      throw new IllegalArgumentException("Angle already set");
    }
    ridgeAngles.put(ridge, v);
  }
  

//  public Polytope getComponent(Predicate<Polytope> predicate) {
//    if (predicate.test(this)) {
//      return this;
//    }
//    for (Polytope facet: facets) {
//      Polytope result = facet.getComponent(predicate);
//      if (result != null) {
//        return result;
//      }
//    }
//    return null;
//  }
//
//  protected Set<Polytope> getComponents(Predicate<Polytope> predicate) {
//    return collectComponents(predicate, new HashSet<Polytope>());
//  }
//
//  protected Set<Polytope> collectComponents(Predicate<Polytope> predicate, HashSet<Polytope> result) {
//    if (predicate.test(this)) {
//      result.add(this);
//      return result;
//    }
//    for (Polytope facet: facets) {
//      facet.collectComponents(predicate, result);
//    }
//    return result;
//  }
//
//  protected Set<Polytope> getComponentsDeep(Predicate<Polytope> predicate) {
//    return collectComponentsDeep(predicate, new HashSet<Polytope>());
//  }
//
//  protected Set<Polytope> collectComponentsDeep(Predicate<Polytope> predicate, HashSet<Polytope> result) {
//    if (predicate.test(this)) {
//      result.add(this);
//    }
//    for (Polytope facet: facets) {
//      facet.collectComponents(predicate, result);
//    }
//    return result;
//  }

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
    Map<Polytope, Polytope> replacementMap = new HashMap<Polytope, Polytope>();
    return copy(replacementMap);
  }
  public Polytope copy(Map<Polytope, Polytope> replacementMap) {
    if (replacementMap.containsKey(this)) {
      return replacementMap.get(this);
    }
    Polytope p = new Polytope(n);
    copyCommon(p, replacementMap);
    return p;
  }
  public void copyCommon(Polytope p, Map<Polytope, Polytope> replacementMap) {
    replacementMap.put(this, p);
    for (Polytope facet: facets) {
      p.facets.add(facet.copy(replacementMap));
    }
    for (Polytope ridge: ridgeAngles.keySet()) {
      p.ridgeAngles.put(replacementMap.get(ridge), ridgeAngles.get(ridge));
    }
    copyCommon(p);
  }
  protected void copyCommon(Polytope p) {
    p.name = name;
    p.id = id;
  }
  
  public Polytope getOtherFacet(Polytope facet, Polytope ridge) {
    return getComponent(p -> p != facet && p.facets.contains(ridge));
  }
  
  public Polytope getRandomFacet() {
    return facets.iterator().next();
  }
  
  
//  // Recursively replaces any references of component1 with component2
//  // May lead to higher dimensional components also being equated
//  public void equate(Polytope component1, Polytope component2) {
//    Optional<Map<Polytope, Polytope>> ways = component1.waysToEquate(component2).findAny();
//    if (!ways.isPresent()) {
//      throw new IllegalArgumentException("Can't equate, not equivalent");
//    }
//    Map<Polytope, Polytope> replacements = ways.get();
//    for (int d = 1; d < n; d++) {
//      final int d2 = d;
//      // Operate on copy to avoid concurrent modification
//      stream().filter(p -> p.n == d2).collect(Collectors.toSet()).stream().forEach(p -> {
//        if (replacements.containsKey(p)) {
//          return;
//        }
//        Set<Polytope> removals = new HashSet<Polytope>();
//        Set<Polytope> additions = new HashSet<Polytope>();
//        for (Polytope facet: p.facets) {
//          Polytope replacement = replacements.get(facet);
//          if (replacement != null) {
//            removals.add(facet);
//            additions.add(replacement);
//          }
//        }
//        if (!removals.isEmpty() || !additions.isEmpty()) {
//          p.facets.removeAll(removals);
//          p.facets.addAll(additions);
//        }
//      });
//    }
//    Set<Polytope> removals = new HashSet<Polytope>();
//    Set<Polytope> additions = new HashSet<Polytope>();
//    for (Polytope facet: this.facets) {
//      Polytope replacement = replacements.get(facet);
//      if (replacement != null) {
//        removals.add(facet);
//        additions.add(replacement);
//      }
//    }
//    if (!removals.isEmpty() || !additions.isEmpty()) {
//      facets.removeAll(removals);
//      facets.addAll(additions);
//    }
//    Set<Polytope> angleRemovals = new HashSet<Polytope>();
//    Map<Polytope, Angle> angleAdditions = new HashMap<Polytope, Angle>();
//    for (Polytope ridge: ridgeAngles.keySet()) {
//      if (replacements.containsKey(ridge)) {
//        angleRemovals.add(ridge);
//        angleAdditions.put(replacements.get(ridge), ridgeAngles.get(ridge));
//      }
//    }
//    if (!angleRemovals.isEmpty() || !angleAdditions.isEmpty()) {
//      for (Polytope ridge: angleRemovals) {
//        ridgeAngles.remove(ridge);
//      }
//      for (Polytope ridge: angleAdditions.keySet()) {
//        ridgeAngles.put(ridge, angleAdditions.get(ridge));
//      }
//    }
//  }
  
  protected  Map<Polytope, Polytope> findEquivalencies(Polytope p) {
    throw new IllegalArgumentException("Can't equate, not equivalent");
  }

//  @Override
//  public String toString() {
//    Set<String> definedNames = new HashSet<String>();
//    StringBuilder stringBuilder = new StringBuilder();
//    Map<Angle, String> angleNames = new HashMap<Angle, String>();
//    toString(false, stringBuilder, definedNames, angleNames);
//    return stringBuilder.toString();
//  }
  // 0-polytope 1 Vertex
  // 1-polytope 2 Edge
  //   Vertex-0 Vertex-1
  // 2-polytope 3 Triangle
  //   Edge-0 Vertex-0 Vertex-1
  //   Edge-1 Vertex-1 Vertex-2
  //   Edge-2 Vertex-2 Vertex-0
  //   v0 PI/3
  //   Angle-0 Vertex-0 Edge-0 Edge-1 v0
  //   Angle-1 Vertex-1 Edge-1 Edge-2 v0
  //   Angle-2 Vertex-2 Edge-2 Edge-0 v0
  // 3-polytope 7 Tetrahedron
  //   Triangle-0 Edge-0 Edge-1 Edge-2
  //   Triangle-1 Edge-0 Edge-3 Edge-4
  //   Triangle-2 Edge-1 Edge-4 Edge-5
  //   Triangle-3 Edge-2 Edge-3 Edge-5
  //   v1 f(v0)
  //   Angle-0 Triangle-0 Triangle-1 v1
  //   Angle-1 Triangle-0 Triangle-2 v1
  //   Angle-2 Triangle-0 Triangle-3 v1
  //   Angle-3 Triangle-1 Triangle-2 v1
//  protected String toString(boolean definitions) {
//    StringBuilder stringBuilder = new StringBuilder();
//    toString(definitions, stringBuilder, new HashSet<String>(), new HashMap<Angle, String>());
//    return stringBuilder.toString();
//  }
//  protected void toString(boolean definitions, StringBuilder stringBuilder, Set<String> definedNames, Map<Angle, String> angleNames) {
//    String name = getName();
//    if (definedNames.contains(name)) {
//      return;
//    }
//    definedNames.add(name);
//
//    // Recursively print definitions of component polytopes
//    if (definitions) {
//      for (Polytope facet: facets) {
//        facet.toString(definitions, stringBuilder, definedNames, angleNames);
//      }
//    }
//    
//    Map<String, Integer> nameCounter = new HashMap<String, Integer>();
//    Map<Polytope, String> names = new HashMap<Polytope, String>();
//    
//    stringBuilder.append(n).append("-polytope ").append(id).append(' ').append(name);
//    if (DEBUG) {
//      stringBuilder.append('@').append(System.identityHashCode(this));
//    }
//    stringBuilder.append("\r\n");
//    Map<Polytope, Set<Polytope>> ridgeToFacetMap = new HashMap<Polytope, Set<Polytope>>();
//    for (Polytope facet: facets) {
//      String fullName = name(facet, names, nameCounter);
//      stringBuilder.append("  ").append(fullName);
//      List<String> ridgeNames = new ArrayList<>();
//      for (Polytope ridge: facet.facets) {
//       ridgeNames.add(name(ridge, names, nameCounter));
//      }
//      ridgeNames.stream().sorted().forEach(
//        ridgeFullName -> stringBuilder.append(' ').append(ridgeFullName)
//      );
//      stringBuilder.append("\r\n");
//      for (Polytope ridge: facet.facets) {
//        Set<Polytope> ridgeFacets = ridgeToFacetMap.get(ridge);
//        if (ridgeFacets == null) {
//          ridgeFacets = new HashSet<Polytope>();
//          ridgeToFacetMap.put(ridge, ridgeFacets);
//        }
//        ridgeFacets.add(facet);
//      }
//    }
  // New version, looks like this:
  // 3-polytope 9 TriangularBipyramid
  //  Edge-0 Vertex-0 Vertex-1
  //  Edge-1 Vertex-0 Vertex-2
  //  Edge-2 Vertex-0 Vertex-3
  //  Edge-3 Vertex-1 Vertex-2
  //  Edge-4 Vertex-2 Vertex-3
  //  Edge-5 Vertex-1 Vertex-3
  //  Edge-6 Vertex-1 Vertex-4
  //  Edge-7 Vertex-2 Vertex-4
  //  Edge-8 Vertex-3 Vertex-4
  //  Triangle-0 Edge-0 Edge-1 Edge-3
  //  Triangle-1 Edge-1 Edge-2 Edge-4
  //  Triangle-2 Edge-0 Edge-2 Edge-5
  //  Triangle-3 Edge-3 Edge-6 Edge-7
  //  Triangle-4 Edge-4 Edge-7 Edge-8
  //  Triangle-5 Edge-5 Edge-6 Edge-8
  //  v7 2*acos(1/3)
  //  Edge-0 v6
  //  Edge-1 v6
  //  Edge-2 v6
  //  Edge-3 v7
  //  Edge-4 v7
  //  Edge-5 v7
  //  Edge-6 v6
  //  Edge-7 v6
  //  Edge-8 v6

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    Map<Angle, Integer> definedAngles = new HashMap<Angle, Integer>();
    List<Angle> angles = new ArrayList<Angle>();
    toString(stringBuilder, definedAngles, angles);
    return stringBuilder.toString();
  }
  
  Stream<Polytope> depthFirst() {
    final Set<Polytope> closedSet = Collections.synchronizedSet(new HashSet<>(1000));
    return depthFirst(closedSet).parallel();
  }
  
  private Stream<Polytope> depthFirst(Set<Polytope> closedSet) {
    if (!closedSet.add(this)) {
      return Stream.empty();
    } else {
      return Stream.concat(facets.stream().flatMap(n -> n.depthFirst(closedSet)), Stream.of(this));
    }
  }

  public void toString(StringBuilder stringBuilder, Map<Angle, Integer> definedAngles, List<Angle> angles) {
    Map<Polytope, String> polytopeNames = new HashMap<Polytope, String>();
    Map<Integer, Set<String>> typesPerDimension = new HashMap<Integer, Set<String>>();
    Map<String, List<Polytope>> polytopesPerType = new HashMap<String, List<Polytope>>();
    Map<Pair<Polytope>, Angle> ridgeToAngleMap = new HashMap<Pair<Polytope>, Angle>();
    
    String name = getName();
    stringBuilder.append(n).append("-polytope ").append(id).append(' ').append(name).append("\r\n");
    // Step 0) Collect names
    for (int i = 0; i < n; i++) {
      int dimension = i;
      stream().filter(p -> p.n == dimension).forEach(p ->
        p.collectDefinitions(polytopeNames, typesPerDimension, polytopesPerType, definedAngles, angles, ridgeToAngleMap));
    }
    // Step 1) Print all subcomponents starting with edges
    for (int d = 1; d < n; d++) {
      List<String> types = new ArrayList<String>(typesPerDimension.get(d));
      Collections.sort(types);
      for (String type: types) {
        for (Polytope p: polytopesPerType.get(type)) {
          stringBuilder.append("  ").append(polytopeNames.get(p));
          List<String> facets = p.facets.stream().map(facet -> polytopeNames.get(facet)).collect(Collectors.toList());
          Collections.sort(facets);
          for (String facet: facets) {
            stringBuilder.append(' ').append(facet);
          }
          stringBuilder.append("\r\n");
        }
      }
    }
    // Step 2) Print angle definitions
    for (int i = 0; i < angles.size(); i++) {
      Angle angle = angles.get(i);
      stringBuilder.append("  v").append(i).append(' ').append(angle).append("\r\n");
    }
    // Step 3) Recursively print angles between subcomponents
    toStringAngles(stringBuilder, polytopeNames, definedAngles, angles, ridgeToAngleMap);
  }
  private void collectDefinitions(Map<Polytope, String> polytopeNames, Map<Integer, Set<String>> typesPerDimension, Map<String, List<Polytope>> polytopesPerType, Map<Angle, Integer> definedAngles, List<Angle> angles, Map<Pair<Polytope>, Angle> ridgeToAngleMap) {
    if (polytopeNames.containsKey(this)) {
      return;
    }
    String type = getName();
    Set<String> typesForDimension = typesPerDimension.get(n);
    if (typesForDimension == null) {
      typesForDimension = new HashSet<String>();
      typesPerDimension.put(n, typesForDimension);
    }
    typesForDimension.add(type);
    List<Polytope> otherPolytopes = polytopesPerType.get(type);
    if (otherPolytopes == null) {
      otherPolytopes = new ArrayList<Polytope>();
      polytopesPerType.put(type, otherPolytopes);
    }
    String fullName = type + '-' + otherPolytopes.size();
    otherPolytopes.add(this);
    polytopeNames.put(this, fullName);
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = getRidgeToFacetMap();
    for (Polytope ridge: ridgeToFacetMap.keySet()) {
      if (ridgeToFacetMap.get(ridge).size() < 2) {
        continue;
      }
      Angle angle = ridgeAngles.get(ridge);
      if (angle == null) {
        angle = new Angle.Unknown();
      }
      if (!definedAngles.containsKey(angle)) {
        definedAngles.put(angle, angles.size());
        angles.add(angle);
        Iterator<Polytope> facets = ridgeToFacetMap.get(ridge).iterator();
        ridgeToAngleMap.put(new Pair<Polytope>(facets.next(), facets.next()), angle);
      }
    }
  }

  private void toStringAngles(StringBuilder stringBuilder, Map<Polytope, String> definedPolytopes, Map<Angle, Integer> definedAngles, List<Angle> angles, Map<Pair<Polytope>, Angle> ridgeToAngleMap) {
//    for (Polytope facet: facets) {
//      facet.toStringAngles(stringBuilder, definedPolytopes, definedAngles, angles, ridgeToAngleMap);
//    }
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = getRidgeToFacetMap();
    List<Polytope> ridges = new ArrayList<Polytope>(ridgeToFacetMap.keySet());
    Collections.sort(ridges, (r1, r2) -> definedPolytopes.get(r1).compareTo(definedPolytopes.get(r2)));
    for (Polytope ridge: ridges) {
      Angle angle = ridgeToAngleMap.get(ridge);
      if (angle == null) {
        continue;
      }
      stringBuilder.append("  ").append(definedPolytopes.get(ridge)).append(" v").append(definedAngles.get(angle));
    }
  }

  protected void toStringComponents(StringBuilder stringBuilder, Map<Polytope, String> definedPolytopes, Map<String, List<Polytope>> namedPolytopes, Map<Angle, Integer> definedAngles, List<Angle> angles, Map<Pair<Polytope>, Angle> ridgeToAngleMap) {
    if (definedPolytopes.containsKey(this)) {
      return;
    }
    stringBuilder.append("  ").append(definedPolytopes.get(this));
    List<String> names = facets.stream().map(facet -> definedPolytopes.get(facet)).collect(Collectors.toList());
    Collections.sort(names);
    for (String facet: names) {
      stringBuilder.append(' ').append(facet);
    }
    stringBuilder.append("\r\n");
   
  }

  private static String name(Polytope facet, Map<Polytope, String> names, Map<String, Integer> nameCounter) {
    String fullName = names.get(facet);
    if (fullName == null) {
      String name = facet.getName();
      int count = nameCounter.containsKey(name)? nameCounter.get(name) + 1 : 0;
      fullName = name + "-" + count;
      if (DEBUG) {
        fullName += '@' + System.identityHashCode(facet);
      }
      
      nameCounter.put(name, count);
      names.put(facet,  fullName);
    }
    return fullName;
  }

  public int getDimensions() {
    return n;
  }

  public Angle getAngle(Polytope ridge) {
    return ridgeAngles.get(ridge);
  }

  public Stream<Polytope> stream() {
    return Stream.concat(
        Stream.of(this),
        facets.stream().flatMap(Polytope::stream));
  }
  public Polytope getComponent(Predicate<Polytope> predicate) {
    Optional<Polytope> result = stream().filter(predicate).findAny();
    if (!result.isPresent()) {
      System.out.println("Ooops");
    }
    return result.get();
  }
  public boolean containsComponent(Predicate<Polytope> predicate) {
    return stream().filter(predicate).findAny().isPresent();
  }
  
  // Find all ways this polytope can be equated to other.
  // For instance, a square can be equated to another square in eight ways (4 rotations * 2 mirror)
  // A cube can be equated to another in 48 ways (6 faces * 4 edges * 2 mirror).
  //
  // Pick any random facet. Find all facets in other that can be equated. Check if rest of polytope matches.
  public Stream<Equivalences> waysToEquate(Polytope other) {
    // Some failfast optimizations
    if (facets == null || other == null || n != other.n || facets.size() != other.facets.size()) {
      return Stream.empty();
    }
    if (facets.size() < 1) {
      throw new IllegalArgumentException("Should not happen");
    }
    // Random facet
    Polytope facet = facets.iterator().next();
    return other.stream()
        .filter(p -> p.n == n-1)              // Facets in "other"
        .flatMap(p ->  facet.waysToEquate(p)) // All ways to equate others facets to facet
        .filter(eqv -> anchor(other, eqv));   // Rest of polytope must also match 

  }
  // Can a given set of equivalences be extended in one and only one way to cover entire polytope?
  protected boolean anchor(Polytope other, Equivalences equivalences) {
    // We don't want to modify the original if anchor fails
    Equivalences result = new Equivalences(equivalences);
    
    // All facets not already equivalent
    Set<Polytope> remaining = new HashSet<Polytope>(facets);
    Set<Polytope> remainingOther = new HashSet<Polytope>(other.facets);
    remaining.removeAll(equivalences.p1p2.keySet());
    remainingOther.removeAll(equivalences.p2p1.keySet());
    int last = remaining.size();
    
    // Extend equivalence to facets containing ridges already equivalent
    while (!remaining.isEmpty()) {
      Set<Polytope> done = new HashSet<Polytope>();
      Set<Polytope> doneOther = new HashSet<Polytope>();
      for (Polytope facet: remaining) {
        for (Polytope ridge: facet.facets) {
          if (equivalences.p1p2.containsKey(ridge)) {
            Polytope neighborFacet = getOtherFacet(facet, ridge);
            Polytope othersNeighborFacet = equivalences.p1p2.get(neighborFacet);
            Polytope othersRidge = equivalences.p1p2.get(ridge);
            Polytope newEquivalent = other.getOtherFacet(othersNeighborFacet, othersRidge);
            if (!facet.anchor(newEquivalent, equivalences)) {
              return false;
            }
            equivalences.add(facet, newEquivalent);
            done.add(facet);
            doneOther.add(newEquivalent);
            break;
          }
        }
      }
      remaining.removeAll(done);
      remainingOther.removeAll(doneOther);
      // No progress and not done
      if (remaining.size() == last) {
        return false;
      }
      last = remaining.size();
    }
    for (Polytope p: result.p1p2.keySet()) {
      equivalences.add(p, result.p1p2.get(p));
    }
    equivalences.add(this, other);
    return true;
  }

  public boolean equivalent(Polytope other) {
    return waysToEquate(other).iterator().hasNext();
  }

  // Find all ways this polytope can be connnected to the other
  // For instance, a triangle can be connected to a square in 24 ways (3 sides * 4 sides * 2 flip)
  // (but they are all equivalent)
  // For all ways of selecting a facet from this and one from other,
  // select all ways to equate the facets (possibly 0)
  // For instance with a triangular prism and a cube we can't connect the triangular faces
  public Stream<Equivalences> waysToConnect(Polytope other) {
    return facets.stream().flatMap(
        // For each facet in this
        facet -> waysToConnect(other, facet));
  }
  // Find all ways this polytope can be connnected to the other using the specified facet
  public Stream<Equivalences> waysToConnect(Polytope other, Polytope facet) {
    // For each facet in other
    // All ways to equate
    return other.facets.stream().flatMap(otherFacet -> facet.waysToEquate(otherFacet));
  }
  // Find all ways this polytope can be connnected to both specified facets (which must be connected)
  public Stream<Equivalences> waysToConnectFacets(Polytope f1, Polytope f2) {
    // For each facet
    return facets.stream().flatMap(
    // For each neighbor facet
        facet -> facet.facets.stream().flatMap(
            ridge -> {
            Polytope neighbor = getOtherFacet(facet, ridge);
            // Attempt to connect f1
            return facet.waysToEquate(f1).flatMap(
                // And f2
                equivalencesWithF1 -> neighbor.waysToEquate(f2)
                // In such a way that the ridge is the same
                  .filter(equivalencesWithF2 -> equivalencesWithF1.p1p2.get(ridge) == equivalencesWithF2.p1p2.get(ridge))
                  .map(
                      equivalencesWithF2 -> {
                      Equivalences result = new Equivalences(equivalencesWithF1);
                      for (Polytope p: equivalencesWithF2.p1p2.keySet()) {
                        result.add(p, equivalencesWithF2.p1p2.get(p));
                      }
                      return result;
                   })
                );
            }));
  }
  // Better equate, operate on a whole map of equivalences (such as returned by waysToEquate or wasyToConnect)
  
  // Recursively replaces any references of components mentioned in equivalences with their equivalents
  public void equate(Map<Polytope, Polytope> equivalences) {
    for (int d = 1; d < n; d++) {
      final int d2 = d;
      // Operate on copy to avoid concurrent modification
      stream().filter(p -> p.n == d2).collect(Collectors.toSet()).stream().forEach(p -> {
        if (equivalences.containsKey(p)) {
          return;
        }
        Set<Polytope> removals = new HashSet<Polytope>();
        Set<Polytope> additions = new HashSet<Polytope>();
        for (Polytope facet: p.facets) {
          Polytope replacement = equivalences.get(facet);
          if (replacement != null) {
            removals.add(facet);
            additions.add(replacement);
          }
        }
        if (!removals.isEmpty() || !additions.isEmpty()) {
          p.facets.removeAll(removals);
          p.facets.addAll(additions);
        }
      });
    }
    Set<Polytope> removals = new HashSet<Polytope>();
    Set<Polytope> additions = new HashSet<Polytope>();
    for (Polytope facet: this.facets) {
      Polytope replacement = equivalences.get(facet);
      if (replacement != null) {
        removals.add(facet);
        additions.add(replacement);
      }
    }
    if (!removals.isEmpty() || !additions.isEmpty()) {
      if (facets.size() + additions.size() - removals.size() < n) {
        throw new IllegalArgumentException("Logic error, to few facets after equating");
      }
      facets.removeAll(removals);
      facets.addAll(additions);
    }
    Set<Polytope> angleRemovals = new HashSet<Polytope>();
    Map<Polytope, Angle> angleAdditions = new HashMap<Polytope, Angle>();
    for (Polytope ridge: ridgeAngles.keySet()) {
      if (equivalences.containsKey(ridge)) {
        angleRemovals.add(ridge);
        angleAdditions.put(equivalences.get(ridge), ridgeAngles.get(ridge));
      }
    }
    if (!angleRemovals.isEmpty() || !angleAdditions.isEmpty()) {
      for (Polytope ridge: angleRemovals) {
        ridgeAngles.remove(ridge);
      }
      for (Polytope ridge: angleAdditions.keySet()) {
        ridgeAngles.put(ridge, angleAdditions.get(ridge));
      }
    }
  }
//  public void equate(Edge e1, Edge e2) {
//    Optional<Map<Polytope, Polytope>> ways = e1.waysToEquate(e2).findAny();
//  if (!ways.isPresent()) {
//    throw new IllegalArgumentException("Can't equate, not equivalent");
//  }
//  Map<Polytope, Polytope> equivalences = ways.get();

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
  
  public void check() {
    for (Polytope facet: facets) {
      if (facet.n != n-1) {
        throw new IllegalArgumentException("Invalid dimensionality");
      }
      facet.check();
    }
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = getRidgeToFacetMap();
    for (Polytope ridge: ridgeToFacetMap.keySet()) {
      if (ridgeToFacetMap.get(ridge).size() > 2) {
        throw new IllegalArgumentException("More than two facets around a ridge?!");
      }
    }
  }

//  public static Polytope connectAndCopy(Polytope f1, Polytope f2, Map<Polytope, Polytope> equivalencesf1f2) {
//    Map<Polytope, Polytope> newPolytopes = new HashMap<Polytope, Polytope>();
//    Polytope f1Copy = f1.copy(newPolytopes);
//    Polytope f2Copy = f2.copy(newPolytopes);
//    Polytope result = new Polytope(f1.n + 1);
//    f1.equate(equivalencesf1f2);
//    f1.add(f1Copy);
//    return result.connectCopies(f1, f2, equivalencesf1f2);
//  }
//
//  private Polytope connectCopies(Polytope f1, Polytope f2, Map<Polytope, Polytope> equivalencesf1f2, Map<Polytope, Polytope> newPolytopes) {
//    for (Polytope facet: facets) {
//  }
  
}
