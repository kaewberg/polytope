package se.pp.forsberg.polytope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static se.pp.forsberg.polytope.AffineTransform.Y;

/**
 * Polytope: a N-dimensional generalization of polygons/polyhedrons.
 * 
 * A N-(dimensional) polytope consits of a set on N-1 polytopes
 * connected by their N-2 polytopes and folded in the N:th dimension.
 * 
 * Example: a 3-dimensional cube consists of 6 2-dimensional square
 * connected by there 1-dimensional edges and folded in the third dimension. 
 * 
 * @author k287750
 *
 */
public class Polytope implements Comparable<Polytope>, Cloneable {
  
  private static final double EPSILON = 0.0000001;
  /**
   * Dimensionality of this polytope
   */
  protected int n;
  /**
   *  n-1 polytope facets in this n-polytope
   */
  protected Set<Polytope> facets = new HashSet<Polytope>();
  /**
   *  Map from (n-2) polytope ridges to (n-1) polytope facets connected to that ridge 
   */
//  protected Map<Polytope, Set<Polytope>> ridgeToFacetMap = new HashMap<Polytope, Set<Polytope>>();
  /**
   * Is the polytope closed (that is, every facet connected to one other facet along each ridge)?
   */
  protected boolean closed = false;
  
  // Avoid cumulative transformation errors
  private Map<Polytope, Point> referenceCoordinates = new IdentityHashMap<Polytope, Point>();
  private AffineTransform transformFromReference = new AffineTransform();
  
  //  Attributes used for solved polytypes
  int id = -1;
  String name = "";
  
  /**
   * Construct an empty n-polytope
   * @param n Dimensionality
   */
  protected Polytope(int n) {
    this.n = n;
  }
  /**
   * Create a n-polytope from a set of (n-1)-polytopes
   * @param facet First facets
   * @param facets Remaining facets
   */
  private Polytope(Polytope... facets) {
    if (facets.length == 0) {
      throw new IllegalArgumentException("Empty polytope");
    }
    this.n = facets[0].n + 1;
    for (Polytope facet: facets) {
      add(facet);
    }
    validate();
  }
  
  /**
   * Create a n-polytope from a set of (n-1)-polytopes
   * @param facet First facets
   * @param facets Remaining facets
   * @return A n-polytope consisting of the (n-1) polytop facets. If n == 2
   * the Edge subclass will be returned.
   */
  public static Polytope get(Polytope... facets) {
    int n = facets[0].n + 1;
    if (n == 1) {
      if (facets.length != 2 || !(facets[0] instanceof Vertex) || !(facets[1] instanceof Vertex)) {
        throw new IllegalArgumentException("Edge may only connect two vertices");
      }
      return new Edge((Vertex) facets[0], (Vertex) facets[1]);
    }
    return new Polytope(facets);
  }
  
  public static Polytope getEmpty(int dimensionality) {
    if (dimensionality < 2) {
      throw new IllegalArgumentException("Use new Vertex or Edge");
    }
    return new Polytope(dimensionality);
  }

  /**
   * Create a 0-polytope (point) with the specified coordinates
   * @param coordinates
   * @return A Vertex object with an assigned coordinate
   */
  public static Vertex get(double... coordinates) {
    return new Vertex(coordinates);
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
    closed = true;
  }

  private Map<Polytope, Set<Polytope>> getRidgeToFacetMap() {
    Map<Polytope, Set<Polytope>> result = new IdentityHashMap<Polytope, Set<Polytope>>();
    for (Polytope facet: facets) {
      for (Polytope ridge: facet.facets) {
        Set<Polytope> facetsByRidge = result.get(ridge);
        if (facetsByRidge == null) {
          facetsByRidge = new HashSet<Polytope>();
          result.put(ridge, facetsByRidge);
        }
        facetsByRidge.add(facet);
      }
    }
    return result;
  }
  /**
   * Add a (n-1) polytope facet to this n-polytope
   * @param facet
   */
  public void add(Polytope facet) {
    if (facet.n != n-1) {
      throw new IllegalArgumentException(String.format("%d-polytope must consist of %d polytopes", n, n-1));
    }
    facets.add(facet);
  }
  
  /**
   * Add a (n-1) polytope facet to this n-polytope, and connect any matching (n-2) polytopes (ridges)
   * @param facet
   */
  public void connect(Polytope facet) {
    add(facet);
    coalesce();
    /*
//    // DEBUG
//    Vertex my111 = null;
//    for (Vertex mine: getVertices()) {
//      if (mine.equals(new Vertex(1,1,1))) {
//        my111 = mine;
//      }
//    }
    
    Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
    for (Vertex mine: getVertices()) {
      for (Vertex his: facet.getVertices()) {
        if (mine.equals(his) && mine != his) {
          if (!replacements.containsKey(his)) {
            replacements.put(his, mine);
            System.out.println("Replace " + his + " with " + mine);
          }/* else {
            for (Polytope v: replacements.keySet()) {
              if (v.equals(his)) {
                if (v != his) {
                  System.err.println("!");
                }
              }
            }
          }* /
        }
      }
    }
    for (int d = 1; d < n; d++) {
      for (Polytope his: facet.getByDimension(d)) {
        boolean allReplaced = true;
        Set<Polytope> removals = new HashSet<Polytope>();
        Set<Polytope> additions = new HashSet<Polytope>();
        for (Polytope hisFacet: his.facets) {
          Polytope myFacet = replacements.get(hisFacet);
          if (myFacet == null) {
            allReplaced = false;
          } else {
            removals.add(hisFacet);
            additions.add(myFacet);
          }
        }
        boolean hole = true;
        if (allReplaced) {
          if (!replacements.containsKey(his)) {
            for (Polytope mine: getByDimension(d)) {
              if (mine.equals(his)) {
                replacements.put(his, mine);
                System.out.println("Replace " + his + " with " + mine);
                hole = false;
                break;
              }
            }
           
          }
        }
        if (!allReplaced || hole) {
//          System.out.println("Has " + his.facets);
//          System.out.println("Remove " + removals);
          his.facets.removeAll(removals);
//          System.out.println("Has " + his.facets);
//          System.out.println("Add " + additions);
          his.facets.addAll(additions);
//          System.out.println("Has " + his.facets);
        }
//        checkValid(new HashMap<Vertex, Vertex>());
      }

//      // DEBUG
//      Vertex his111 = null;
//      Polytope hisEdge = null;
//      for (Polytope p: facet.getByDimension(d)) {
//        for (Vertex his: p.getVertices()) {
//          if (his.equals(new Vertex(1,1,1))) {
//            his111 = his;
//            hisEdge = p;
//          }
//        }
//      }
//      if (my111 != null && his111 != null && my111 != his111) {
//        Polytope repl = replacements.get(his111); 
//        System.err.println("!");
//      }
    }
//    facet.checkValid(new HashMap<Vertex, Vertex>());
    add(facet);
//    checkValid(new HashMap<Vertex, Vertex>());
    */
  }
  /**
   * Create a copy of a facet and then turn it at the specified angle around the ridge.
   * The positive direction of the angle is defined such that repeated copy + rotations of 0 - PI guarantee a convex result
   * @param facet Facet to copy
   * @param ridge Ridge to rotate about
   * @param angle Angle to rotate in radians
   * @return The new facet will be added and returned 
   */
  public Polytope copyAndRotate(Polytope facetToCopy, Polytope ridge, double angle) {
    Set<Polytope> ridgeParts = new IdentityHashSet<Polytope>();
    ridge.collectAllPolytopes(ridgeParts);
    Polytope facet = facetToCopy.cloneExcept(ridgeParts);
    facet.rotate(ridge, angle);
    add(facet);
    coalesce();
    return facet;
  }
//  public Polytope copyAndRotate(Polytope facetToCopy, Polytope ridge, double angle) {
//    Set<Polytope> ridgeParts = new IdentityHashSet<Polytope>();
//    ridge.collectAllPolytopes(ridgeParts);
//    Polytope facet = facetToCopy.cloneExcept(ridgeParts);
//    facet.rotate(ridge, angle);
//    int sign = 0;
//    for (Polytope ridge2: facet.getFacets()) {
//      if (ridge2 != ridge) {
//        for (Vertex vertex: ridge2.getVertices()) {
//          if (Math.abs(vertex.getCoordinates().getCoordinate(n-1)) > EPSILON) {
//            sign = vertex.getCoordinates().getCoordinate(n-1) > 0? 1 : -1;
//            break;
//          }
//        }
//        break;
//      }
//    }
//    angle = normalize(angle);
//    int angleSign = angle > 0? 1 : -1;
//    if (sign != angleSign) {
//      facet = facetToCopy.cloneExcept(ridgeParts);
//      facet.rotate(ridge, -angle);
//    }
//    add(facet);
//    coalesce();
//    return facet;
//  }
  
  private double normalize(double angle) {
    while (angle < -Math.PI) {
      angle += Math.PI*2;
    }
    while (angle > Math.PI) {
      angle -= Math.PI*2;
    }
    return angle;
  }
  private boolean handedness(Polytope facet) {
    double center[] = new double[n];
    Set<Vertex> vertices = getVertices();
    for (Vertex vertex: vertices) {
      for (int i = 0; i < center.length; i++) {
        center[i] += vertex.getCoordinates().getCoordinate(i) / vertices.size();
      }
    }
    double distances[] = new double[n];
    for (Vertex vertex: facet.getVertices()) {
      for (int i = 0; i < distances.length; i++) {
        distances[i] += center[i] - vertex.getCoordinates().getCoordinate(i);
      }
    }
    boolean result = true;
    for (int i = 0; i < distances.length; i++) {
      if (distances[i] < -EPSILON) {
        result = !result;
      }
    }
    return result;
  }
  private void collectAllPolytopes(Set<Polytope> result) {
    result.add(this);
    for (Polytope facet: facets) {
      facet.collectAllPolytopes(result);
    }
  }
  public void coalesce() {
    Map<Polytope, Polytope> usedSoFar = new HashMap<Polytope, Polytope>();
    Map<Polytope, Polytope> replacements = new IdentityHashMap<Polytope, Polytope>();
    for (int d = 0; d < n; d++) {
      Set<Polytope> polytopes = getByDimension(d);
      for (Polytope p: polytopes) {
        Polytope previous = usedSoFar.get(p);
        if (previous != null) {
          System.out.println("Replace " + p + " with " + previous);
          replacements.put(p, previous);
        } else {
          usedSoFar.put(p, p);
          Polytope test = Polytope.get(1.0, 1.0, 0.9999999999999999, -1.0);
          Polytope test2 = usedSoFar.get(test);
          
          Set<Polytope> removals = new HashSet<Polytope>();
          Set<Polytope> additions = new HashSet<Polytope>();
          for (Polytope facet: p.facets) {
            Polytope replacement = replacements.get(facet);
            if (replacement != null) {
              removals.add(facet);
              additions.add(replacement);
            }
          }
//          int before = p.facets.size();
          if (!removals.isEmpty() || !additions.isEmpty()) {
            p.facets.removeAll(removals);
            p.facets.addAll(additions);
          }
//          int after = p.facets.size();
//          assertEquals(after, before - removals.size() + additions.size());
//          assertEquals(after, before);
        }
      }
    }
  }

//  // Replace one ridge by another
//  private static void addReplacements(Polytope ridge1, Polytope ridge2, Map<Polytope, Polytope> replacements) {
//    for (Polytope p1: ridge1.facets) {
//      for (Polytope p2: ridge2.facets) {
//        if (p1.hasSameCoordinates(p2)) {
//          addReplacements(p1, p2, replacements);
//        }
//      }
//    }
//    replacements.put(ridge1, ridge2);
//  }
//  private boolean hasSameCoordinates(Polytope other) {
//    return getVertices().equals(other.getVertices());
//  }
//  private Set<Polytope> getRidges() {
//    Set<Polytope> result = new HashSet<Polytope>();
//    collectByDimension(n-2, result);
//    return  result;
//  }

private void assertEquals(int i1, int i2) {
    if (i1 != i2) {
      throw new IllegalArgumentException("!");
    }
  }
  //  private void checkValid(Map<Vertex, Vertex> vertices) {
//    if (this instanceof Vertex) {
//      Vertex other = vertices.get(this);
//      if (other == null) {
//        vertices.put((Vertex) this, (Vertex) this);
//      } else if (other != this) {
//        System.err.println("!");
//      }
//    } else {
//      for (Polytope facet: facets) {
//        facet.checkValid(vertices);
//      }
//    }
//  }
  private Set<Polytope> getByDimension(int d) {
    Set<Polytope> result = new IdentityHashSet<Polytope>();
    collectByDimension(d, result);
    return result;
  }
  private void collectByDimension(int d, Set<Polytope> result) {
    if (d == n) {
      result.add(this);
    } else {
      for (Polytope facet: facets) {
        facet.collectByDimension(d, result);
      }
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
  /**
   * Sort all component polytopes in this n-polytope int sets by dimensionality.
   * @param polytopesByDimension
   */
  private void collectPolytopes(Map<Integer, Set<Polytope>> polytopesByDimension) {
    Set<Polytope> polytopes = polytopesByDimension.get(n-1);
    if (polytopes == null) {
      polytopes = new IdentityHashSet<Polytope>();
      polytopesByDimension.put(n-1, polytopes);
    }
    for (Polytope facet: facets) {
      polytopes.add(facet);
      facet.collectPolytopes(polytopesByDimension);
    }
  }
  /**
   * Create a set of all 0-polytopes (vertices) in this polytope.
   * @return
   */
  public Set<Vertex> getVertices() {
    Set<Vertex> vertices = new IdentityHashSet<Vertex>();
    collectVertices(vertices);
    return vertices;
  }
  /**
   * Create a set of all 0-polytopes (vertices) in this polytope.
   * @param vertices
   */
  protected void collectVertices(Set<Vertex> vertices) {
    for (Polytope facet: facets) {
      facet.collectVertices(vertices);
    }
  }
  /**
   * Create a set of all 1-polytopes (edges) in this polytope.
   * @return
   */
  public Set<Edge> getEdges() {
    Set<Edge> edges = new IdentityHashSet<Edge>();
    collectEdges(edges);
    return edges;
  }
  /**
   * Create a set of all 1-polytopes (edges) in this polytope.
   * @param edges
   */
  protected void collectEdges(Set<Edge> edges) {
    for (Polytope facet: facets) {
      facet.collectEdges(edges);
    }
  }
    
  public void transform(AffineTransform t) {
//    String s = t.toString();
    Set<Vertex> vertices = new HashSet<Vertex>();
    collectVertices(vertices);
    for (Vertex vertex: vertices) {
      vertex.transform(t);
    }
    updateHashSets();
  }
  
  private void updateHashSets() {
    Set<Polytope> newFacets = new HashSet<Polytope>();
    for (Polytope facet: facets) {
      facet.updateHashSets();
      newFacets.add(facet);
    }
    facets = newFacets;
  }
  /**
   * Translate all coordinates in this polytope
   * @param distances
   */
  public void translate(double... distances) {
    transform(AffineTransform.getTranslateInstance(distances));
  }
  /**
   * Scale all coordinates in this polytope around origo
   * @param scales
   */
  public void scale(double... scales) {
    transform(AffineTransform.getScaleInstance(scales));
  }
  /**
   * Rotate all coordinates in this polytope in the specified plane 
   * A 2d rotation would be rotate(0, 1, v);
   * A 3d rotation around x axis (that is, in y-z plane) would be rotate(1, 2, v);
   * 
   * @param dimension1 First axis defining plane of rotation
   * @param dimension2 Second axis defining plane of rotation
   * @param v Angle, radians
   */
  public void rotate(int dimension1, int dimension2, double v) {
    transform(AffineTransform.getRotateInstance(dimension1, dimension2, v));
  }
  
  /**
   * Rotate an (n-1) polytope i dimension n around one of its facets
   * (2d: rotate edge around point,
   *  3d: rotate surface around edge
   *  4d: rotate volume around surface)
   *  
   * Accomplished by rotating/translating the object so that the facets n-th coordinates are all zero,
   * rotating in the N-1/N plane and the rotating/translating back.
   * The angle is in the direction of rotation that will make the N:th dimensional coordinates poitive
   * (ie make the complete polytope convex)
   * @param facet
   * @param angle
   */
  public void rotate(Polytope facet, double angle) {
    // "Flatten" the facet to rotate about, ie make it use the least possible number of dimensions,
    // ie rotate/translate it so that it has one edge from (0,0) along positive x-axis,
    // one neighboring facet in the x/y plane,
    // one neighboring volume in the x/y/z space etc
    AffineTransform flatten = facet.getFlatteningTransform();
    // then rotate in the x/n plane
    AffineTransform rotate = AffineTransform.getRotateInstance(n-1, n, angle);
    AffineTransform unflatten = flatten.realClone().invert();
    transform(flatten);
    
    angle = normalize(angle);
    int angleSign = angle > 0? 1 : -1;
    Set<Vertex> facetVertices = facet.getVertices();
    Vertex vertex = null;
    for (Vertex v: getVertices()) {
      if (!facetVertices.contains(v)) {
        vertex = (Vertex) v.realClone();
        break;
      }
    }
    vertex.transform(rotate);
    int sign = vertex.getCoordinates().getCoordinate(n) > 0? 1 : -1;
    if (sign != angleSign) {
      angle = -angle;
    }
    transform(unflatten.rotate(n-1, n, angle));
  }
  
//  public void rotate(Polytope facet, double angle) {
//    AffineTransform flatten = facet.getFlatteningTransform();
//    AffineTransform unflatten = flatten.realClone().invert();
//    transform(unflatten.rotate(n-1, n, angle).concatenate(flatten));
//  }
  /**
   * The transform that make this polytope flat, ie the n-polytope has all coordinates
   * in dimension > N = 0.
   * NB, will only work as expected if the polytope is flat!
   * @return
   */
  private AffineTransform getFlatteningTransform() {
    AffineTransform t = new AffineTransform();
    List<AffineTransform> stages = getFlatteningTransformStages();
    for (int i = stages.size()-1; i >= 0; i--) {
      t.concatenate(stages.get(i));
    }
    return t;
  }
  private List<AffineTransform> getFlatteningTransformStages() {
    Iterator<Vertex> it = getVertices().iterator();
    List<Vertex> vertices = new ArrayList<Vertex>();
    for (int i = 0; i <= n; i++) {
      vertices.add((Vertex) it.next().realClone());
    }
    // Translate first point to origo
    Point p = vertices.get(0).getCoordinates();
    double[] distances = new double[n+2];
    for (int i = 0; i <= n+1; i ++) {
      distances[i] = -p.getCoordinate(i);
    }
    List<AffineTransform> stages = new ArrayList<AffineTransform>();
    AffineTransform t = AffineTransform.getTranslateInstance(distances);
    stages.add(t);
    t.transform(vertices);
    
    for (int d = 0; d < n; d++) {
      // Rotate second point to x axis
      // rotate x/y, then x/z, then x/w until second point is on the x axis 
      // Rotate third point to x/y plane
      // rotate y/z, then y/w until third point is on the x/y plane 
      // and so on
      p = vertices.get(d+1).getCoordinates();
      for (int i = d+1; i <= n; i++) {
        t = AffineTransform.getRotateInstance(d, i, -Math.atan2(p.getCoordinate(i), p.getCoordinate(d)));
        stages.add(t);
        t.transform(vertices);
      }
    }
    return stages;
  }
  /**
   * Return all facets
   * @return
   */
  public Set<Polytope> getFacets() {
    return facets;
  }

  
  @Override
  public int hashCode() {
    int result = n;
    for (Polytope facet: facets) {
      result ^= facet.hashCode();
    }
    return result;
  }
  @Override
  public int compareTo(Polytope o) {
    if (o == null) {
      return -1;
    }
    int result;
    result = new Integer(n).compareTo(o.n);
    if (result != 0) {
      return result;
    }
    ArrayList<Polytope> facets1 = new ArrayList<Polytope>(facets);
    ArrayList<Polytope> facets2 = new ArrayList<Polytope>(o.facets);
    Collections.sort(facets1);
    Collections.sort(facets2);
    result = new Integer(facets1.size()).compareTo(facets2.size());
    if (result != 0) {
      return result;
    }
    for (int i = 0; i < facets1.size(); i++) {
      result = facets1.get(i).compareTo(facets2.get(i));
      if (result != 0) {
        return result;
      }
    }
    return 0;
  }
  public boolean equals(Object other) {
    if (!(other instanceof Polytope)) {
      return false;
    }
    return compareTo((Polytope) other) == 0;
  }
  
  @Override
  public String toString() {
//    StringBuilder result = new StringBuilder();
//    toString(0, result);
//    return result.toString();
    return toStringConstructive();
  }
  protected void toString(int i, StringBuilder result) {
    if (i > 0) {
      result.append(String.format("%" + i + "s", ""));
    }
    result.append("Polytope(" + n + "){\n");
    boolean first = true;
    for (Polytope facet: facets) {
      if (first) {
        first = false;
      } else {
        result.append(",\n");
      }
      facet.toString(i+1, result);
    }
    result.append('\n');
    if (i > 0) {
      result.append(String.format("%" + i + "s}", ""));
    }
  }
  
  /**
   * Constructive description, ie. first specify vertices, then edges in terms of
   * vertices, then faces in terms of edges, and so on.
   * @return Constructive description
   */
  protected String toStringConstructive() {
    Map<Integer, Set<Polytope>> polytopesByDimension = new HashMap<Integer, Set<Polytope>>();
    collectPolytopes(polytopesByDimension);
    StringBuilder stringBuilder = new StringBuilder("Polytope(" + n + "){\n");
    Map<Polytope, String> nameMap = new IdentityHashMap<Polytope, String>();
//    for (Polytope polytope: polytopesByDimension.get(0)) {
//      nameMap.put(polytope, polytope.toString());
//    }
    for (int n = 0; n < this.n; n++) {
      if (!polytopesByDimension.containsKey(n)) {
        continue;
      }
      List<Polytope> polytopes = new ArrayList<Polytope>(polytopesByDimension.get(n));
      Collections.sort(polytopes);
      int i = 0;
      for (Polytope polytope: polytopes) {
        nameMap.put(polytope, "p" + n + "-" + i);
        i++;
      }
    }
    for (int n = 0; n < this.n; n++) {
      if (!polytopesByDimension.containsKey(n)) {
        continue;
      }
      List<Polytope> polytopes = new ArrayList<Polytope>(polytopesByDimension.get(n));
      Collections.sort(polytopes);
      for (Polytope polytope: polytopes) {
        stringBuilder.append("  ").append(nameMap.get(polytope)).append('=');
        boolean first = true;
        if (n == 0) {
          stringBuilder.append(polytope);
        } else {
          for (Polytope facet: polytope.facets) {
            if (first) {
              first = false;
            } else {
              stringBuilder.append(',');
            }
            stringBuilder.append(nameMap.get(facet));
          }
        }
        stringBuilder.append('\n');
      }
    }
    
    stringBuilder.append("  p").append(n).append("-0=");
    boolean first = true;
    for (Polytope facet: facets) {
      if (first) {
        first = false;
      } else {
        stringBuilder.append(',');
      }
      stringBuilder.append(nameMap.get(facet));
    }
    stringBuilder.append("\n}");
    return stringBuilder.toString();
  }
  
  @Override
  public Object clone() throws CloneNotSupportedException {
    return realClone();
  }
  public Polytope realClone() {
    Map<Polytope, Polytope> alreadyCloned = new IdentityHashMap<Polytope, Polytope>();
    return realClone(alreadyCloned, new HashSet<Polytope>());
  }
  protected Polytope cloneExcept(Set<Polytope> except) {
    Map<Polytope, Polytope> alreadyCloned = new IdentityHashMap<Polytope, Polytope>();
    return realClone(alreadyCloned, except);
  }
  protected Polytope realClone(Map<Polytope, Polytope> alreadyCloned, Set<Polytope> except) {
    Polytope result = new Polytope(n);
    for (Polytope facet: facets) {
      if (alreadyCloned.containsKey(facet)) {
        result.add(alreadyCloned.get(facet));
      } else {
        Polytope clone;
        if (except.contains(facet)) {
          clone = facet;
        } else {
          clone = facet.realClone(alreadyCloned, except);
        }
        alreadyCloned.put(facet, clone);
        result.add(clone);
      }
    }
    result.closed = closed;
    return result;
  }
  public int getDimensions() {
    return n;
  }
  public Polytope close() {
    Set<Polytope> openRidges = new HashSet<Polytope>();
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = getRidgeToFacetMap();
    for (Polytope ridge: ridgeToFacetMap.keySet()) {
      if (ridgeToFacetMap.get(ridge).size() < 2) {
        openRidges.add(ridge);
      }
    }
    Polytope facet;
    if (n-1 == 1) {
      if (openRidges.size() != 2) {
        throw new IllegalArgumentException("Wrong number of vertices");
      }
      Iterator<Polytope> it = openRidges.iterator();
      facet = Polytope.get(it.next(), it.next());
    } else {
      facet = Polytope.getEmpty(n-1);
    }
    for (Polytope ridge: openRidges) {
      facet.add(ridge);
    }
    facet.validate();
    add(facet);
    validate();
    return facet;
  }
  public static Polytope getVertex() {
    return new Vertex();
  }
  public void center() {
    Set<Vertex> vertices = getVertices();
    int n = vertices.size();
    double c[] = new double[n];
    for (Vertex vertex: vertices) {
      for (int d = 0; d < n; d++) {
        c[d] -= vertex.getCoordinates().getCoordinate(d) / n;
      }
    }
    translate(c);
  }
public void setName(String name) {
	this.name = name;
}
public void setId(int id) {
	this.id = id;
}
}
