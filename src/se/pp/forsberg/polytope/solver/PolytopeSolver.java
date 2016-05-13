package se.pp.forsberg.polytope.solver;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import se.pp.forsberg.polytope.solver.Angle.TrinaryAngle;
import se.pp.forsberg.polytope.solver.WorkInProgress.CornerAngles;
import se.pp.forsberg.polytope.solver.WorkInProgress.DihedralAngle;
import se.pp.forsberg.polytope.solver.WorkInProgress.FacetChain; 

// As per usual terminology, in a n-polytope
// a n-1 polytope is a facet
// a n-2 polytope is a ridge
// in addition, a n-3 polytope is a corner
// Facets are connected pair-wise at ridges, and the angle between two facets around an ridge is the dihedral angle.
public class PolytopeSolver {
  
  private List<Polytope> solved = new ArrayList<Polytope>();
  private Map<Integer, List<Polytope>> solvedByDimension = new HashMap<Integer, List<Polytope>>();
  private Map<String, Polytope> nameToPolytopeMap = new HashMap<String, Polytope>();
  
  private final static File spoolFile = new File(new File(System.getProperty("user.home")), "polytopes.txt");
  
  // (Re-)Starting point of calculation is currently a work in progress with just one facet chain set
  private class StartingPoint {
    WorkInProgress p;
    private int ids[];
    public StartingPoint(WorkInProgress p) {
      this.p = p;
      ids = new int[p.facets.size()];
      int i = 0;
      for (Polytope facet: p.facets) {
        ids[i++] = facet.id;
      }
      Arrays.sort(ids);
      // Also add single completed facetChain
      Polytope firstFacet = p.facets.iterator().next();
      Polytope lastFacet = firstFacet;
      Polytope lastRidge = null;
      Map<Polytope, Set<Polytope>> ridgeToFacetMap = p.getRidgeToFacetMap();
      List<Polytope> facets = new ArrayList<Polytope>();
      List<Polytope> ridges = new ArrayList<Polytope>();
      boolean done = false;
      while (!done) {
        Polytope facet = lastFacet;
        Polytope ridge = lastRidge; 
        Polytope nextRidge = ridgeToFacetMap.keySet().stream().filter(r -> facet.facets.contains(r) && ridgeToFacetMap.get(r).size() == 2 && r != ridge).findAny().get();
        Polytope nextFacet = ridgeToFacetMap.get(nextRidge).stream().filter(f -> f != facet).findAny().get();
        lastFacet = nextFacet;
        lastRidge = nextRidge;
        facets.add(facet);
        ridges.add(nextRidge);
        done = nextFacet == firstFacet;
      }
      Set<Polytope> corners = new HashSet<Polytope>(ridges.get(0).facets);
      corners.retainAll(ridges.get(1).facets);
      Polytope corner = corners.iterator().next();
      FacetChain chain = p.newFacetChain(corner);
      for (i = 0; i < facets.size(); i++) {
        chain.add(ridges.get(i), facets.get(i));
      }
      p.finishedCorners.put(corner, chain);
    }
    public StartingPoint(int dimension, int n) {
      // Fake
      Polytope facet = solvedByDimension.get(dimension-1).get(0);
      int id = facet.id;
      ids = new int[n];
      p = new WorkInProgress(dimension);
      for (int i = 0; i < n; i++) {
        ids[i] = id;
        p.add(facet.copy());
      }
      
    }
    @Override
    public String toString() {
      return "\n" + p.toString(false);
    }
    public String shortDescription() {
      String[] result = new String[ids.length];
      for (int i = 0; i < ids.length; i++) {
        result[i] = solved.get(ids[i]).getName();
      }
      return Arrays.toString(result);
    }
  }
  
  public static void main(String[] arguments) {
    new PolytopeSolver().solve();
  }

  private void solve() {
    continuePrevious();
  }

  Pattern polytopeDefinition = Pattern.compile("^(\\d+)-polytope (\\d+) (\\w+)");
  Pattern facetDefinition = Pattern.compile("^\\s+(\\w+)-(\\d+)((\\s+(\\w+)-(\\d+))*)");
  Pattern facetDefinition2 = Pattern.compile("\\s+(\\w+)-(\\d+)");
  Pattern angleDefinition = Pattern.compile("^\\s+v(\\d+) (.*)");
  Pattern ridgeAngleDefinition = Pattern.compile("^\\s+(\\w+)-(\\d+)\\s+(\\w+)-(\\d+)\\s+(\\w+)-(\\d+)\\sv(\\d+)");
  Pattern rationalPi = Pattern.compile("^(\\d*)PI(/(\\d+))?$");
  Pattern startingPointDefinition = Pattern.compile("^Currently trying");
  
  private void continuePrevious() {
    StartingPoint startingPoint;
    
    add(new Vertex(), "Vertex");
    add(new Edge(copyVertex(), copyVertex()), "Edge");
    
    try {
      BufferedReader in = new BufferedReader(new FileReader(spoolFile));
      //Set<String> definedNames = new HashSet<String>();
      List<Angle> angles = new ArrayList<Angle>();
      String line;
      int[] lineNumber = {0};
      Polytope p;
      while ((p = readPolytope(in, angles, lineNumber)) != null) {
        if (p.n < 2) {
          continue;
        }
        solved.add(p.id, p);
        List<Polytope> byDimension = solvedByDimension.get(p.n);
        if (byDimension == null) {
          byDimension = new ArrayList<Polytope>();
          solvedByDimension.put(p.n, byDimension);
        }
        byDimension.add(p);
        nameToPolytopeMap.put(p.getName(), p);
      }
      // After main definitions we expect a line, then current starting point
      if ((line = line(in, lineNumber)) == null) {
        throw new IOException("Line " + lineNumber[0] + ": Syntax error on line " + line);
      }
      Matcher match = startingPointDefinition.matcher(line);
      if (!match.matches()) {
        throw new IOException("Line " + lineNumber[0] + ": Invalid starting point " + line);
      }
      // Starting point currently is a WorkInProgress
      startingPoint = new StartingPoint(readPolytope(in, angles, WorkInProgress.class, lineNumber));
    } catch (Exception e) {
      e.printStackTrace();
      solved.clear();
      solvedByDimension.clear();
      nameToPolytopeMap.clear();
      addBasic();
      startingPoint = getStartingPoint(3);
    }
    solve(startingPoint);
  }
  private StartingPoint getStartingPoint(int n) {
    // Item 0 should always be the simplex and can be connected any which way to other simplexes
    // 3 simplexes around a corner
    Polytope facet1 = solvedByDimension.get(n-1).get(0).copy();
    Polytope facet2 = solvedByDimension.get(n-1).get(0).copy();
    Polytope facet3 = solvedByDimension.get(n-1).get(0).copy();
    // Random ridge for each simplex
    Polytope ridge12 = facet1.facets.iterator().next();
    Polytope ridge23 = facet2.facets.iterator().next();
    Polytope ridge31 = facet3.facets.iterator().next();
    // Random corner to equate
    Polytope corner1 = ridge12.facets.iterator().next();
    Polytope corner2 = ridge23.facets.iterator().next();
    Polytope corner3 = ridge31.facets.iterator().next();
    // Another ridge at same corner
    Polytope ridge13 = facet1.facets.stream().filter(ridge -> ridge != ridge12 && ridge.facets.contains(corner1)).findAny().get();
    Polytope ridge21 = facet2.facets.stream().filter(ridge -> ridge != ridge23 && ridge.facets.contains(corner2)).findAny().get();
    Polytope ridge32 = facet3.facets.stream().filter(ridge -> ridge != ridge31 && ridge.facets.contains(corner3)).findAny().get();
    // Connect everything
    Map<Polytope, Polytope> equivalences = new HashMap<Polytope, Polytope>();
    equivalences.put(corner2, corner1);
    equivalences.put(corner3, corner1);
    ridge23.equate(equivalences);
    ridge31.equate(equivalences);
    ridge21.equate(equivalences);
    ridge32.equate(equivalences);
    ridge13.equate(equivalences);
    equivalences.put(ridge21, ridge12);
    equivalences.put(ridge32, ridge23);
    equivalences.put(ridge13, ridge31);
    facet1.equate(equivalences);
    facet2.equate(equivalences);
    facet3.equate(equivalences);
    WorkInProgress result = new WorkInProgress(n);
    result.add(facet1);
    result.add(facet2);
    result.add(facet3);
    return new StartingPoint(result);
  }
  private StartingPoint getStartingPoint(int dimension, int n) {
    StartingPoint firstStartingPoint = new StartingPoint(dimension, n);
    return startingPointsIncluding(firstStartingPoint).findAny().get();
  }
  private Polytope readPolytope(BufferedReader in, List<Angle> angles, int... lineNumber) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    return readPolytope(in, angles, Polytope.class, lineNumber);
  }
  private <T extends Polytope> T readPolytope(BufferedReader in, List<Angle> angles, Class<T> clazz, int... lineNumber) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
    String line = line(in, lineNumber);
    if (line == null) {
      return null;
    }
    Matcher match = polytopeDefinition.matcher(line);
    if (!match.matches()) {
      return null;
    }
    int n = Integer.parseInt(match.group(1));
    int id = Integer.parseInt(match.group(2));
    String name = match.group(3);
    T p = clazz.getConstructor(int.class).newInstance(n);
    p.setId(id);
    p.setName(name);
    Map<String, List<Polytope>> facets = new HashMap<String, List<Polytope>>();
    Map<String, List<Polytope>> ridges = new HashMap<String, List<Polytope>>();
    while ((line = line(in, lineNumber)) != null && !line.startsWith("-----")) {
      match = facetDefinition.matcher(line);
      if (!match.matches()) {
        break;
      }
      String facetName = match.group(1);
      int facetId = Integer.parseInt(match.group(2));
      String ridgeList = match.group(3);
      Polytope facet = get(facetName).copy();
      Map<String, List<Polytope>> facetRidges = new HashMap<String, List<Polytope>>();
      for (Polytope ridge: facet.facets) {
        safeAdd(facetRidges, ridge.getName(), ridge);
      }
      Map<Polytope, Polytope> equivalences = new HashMap<Polytope, Polytope>();
      match = facetDefinition2.matcher(ridgeList);
//      int c = match.groupCount();
//      for (int i = 0; i <= match.groupCount(); i++) {
//        System.out.println(match.group(i));
//      }
      int i = 0;
      while (match.find()) {
//      for (int i = 0; i < (match.groupCount()-2) / 3; i++) {
        String ridgeName = match.group(1);
        int ridgeId = Integer.parseInt(match.group(2));
        Polytope oldRidge = safeGet(ridges, ridgeName, ridgeId);
        if (oldRidge != null) {
          equivalences.put(facetRidges.get(ridgeName).get(i), oldRidge);
        } else {
          safePut(ridges, ridgeName, ridgeId, facetRidges.get(ridgeName).get(i));
        }
        i++;
      }
      facet.equate(equivalences);
      safePut(facets, facetName, facetId, facet);
      p.add(facet);
    }
    while (line != null && !line.startsWith("-----")) {
      match = angleDefinition.matcher(line);
      if (!match.matches()) {
        break;
      }
      id = Integer.parseInt(match.group(1));
      String definition = match.group(2);
      match = rationalPi.matcher(definition);
      if (match.matches()) {
        int nominator = match.group(1).isEmpty()? 1 : Integer.parseInt(match.group(1));
        int denominator = match.group(2).isEmpty()? 1 : Integer.parseInt(match.group(3));
        angles.add(id, new Angle.RationalPi(nominator, denominator));
      } else {
        throw new IOException("Line " + lineNumber[0] + ": Syntax error on angle definition " + definition);
      }
      line = line(in, lineNumber);
    }
    while (line != null && !line.startsWith("-----")) {
      match = ridgeAngleDefinition.matcher(line);
      if (!match.matches()) {
        break;
      }
      String ridgeName = match.group(1);
      int ridgeId = Integer.parseInt(match.group(2));
      String facetName1 = match.group(3);
      int facetId1 = Integer.parseInt(match.group(4));
      String facetName2 = match.group(5);
      int facetId2 = Integer.parseInt(match.group(6));
      int angleId = Integer.parseInt(match.group(7));
      Polytope ridge = safeGet(ridges, ridgeName, ridgeId);
      Polytope facet1 = safeGet(facets, facetName1, facetId1);
      Polytope facet2 = safeGet(facets, facetName2, facetId2);
      Angle angle = angles.get(angleId);
      if (ridge == null) {
        throw new IOException("Line " + lineNumber[0] + ": Undefined ridge " + ridgeName + "-" + ridgeId + " in " + line);
      }
      if (facet1 == null) {
        throw new IOException("Line " + lineNumber[0] + ": Undefined facet " + facetName1 + "-" + facetId1 + " in " + line);
      }
      if (facet2 == null) {
        throw new IOException("Line " + lineNumber[0] + ": Undefined facet " + facetName2 + "-" + facetId2 + " in " + line);
      }
      if (angle == null) {
        throw new IOException("Line " + lineNumber[0] + ": Undefined angle v" + angleId + " in " + line);
      }
      p.setAngle(ridge, angle);
      line = line(in, lineNumber);
    }
    return p;
  }

  private static <Key, Value> Value safeGet(Map<Key, List<Value>> map, Key key, int i) {
    List<Value> list = map.get(key);
    if (list == null) {
      return null;
    }
    if (list.size() <= i) {
      return null;
    }
    return list.get(i);
  }

  private static <Key, Value> void safeAdd(Map<Key, List<Value>> map, Key key, Value value) {
    safePut(map, key, -1, value);
  }
  private static <Key, Value> void safePut(Map<Key, List<Value>> map, Key key, int i, Value value) {
    List<Value> list = map.get(key);
    if (list == null) {
      list = new ArrayList<Value>();
      map.put(key, list);
    }
    if (i < 0) {
      list.add(value);
    } else {
      list.add(i, value);
    }
  }

  private String line(BufferedReader in, int... lineNumber) throws IOException {
    String result;
    while ((result = in.readLine()) != null) {
      if (lineNumber.length > 0) {
        lineNumber[0]++;
      }
      //result = result.trim();
      if (!result.startsWith("#") && !result.isEmpty()) {
        return result;
      }
    }
    return result;
  }

  private synchronized void spool(StartingPoint startingPoint) {
    try (PrintStream out =  new PrintStream(spoolFile)) {
      Set<String> definedNames = new HashSet<String>();
      Map<Angle, String> angleNames = new HashMap<Angle, String>();
      for (int i = 0; solvedByDimension.containsKey(i); i++) {
        for (Polytope p: solvedByDimension.get(i)) {
          StringBuilder stringBuilder = new StringBuilder();
          p.toString(false, stringBuilder, definedNames, angleNames);
          out.println(stringBuilder.toString());
          out.println("----------------------------------------------");
        }
      }
      out.println("Currently trying " + startingPoint);
    } catch (FileNotFoundException e) {
      System.err.println("Spool failure!");
      e.printStackTrace();
    }
  }
  
  private void add(Polytope p) {
    int d = p.getDimensions();
    int n = (solvedByDimension.get(d) == null)? 0 : solvedByDimension.get(d).size();
    add(p, "p" + d + "-" + n);
  }
  private void add(Polytope p, String name) {
    List<Polytope> polytopesForDimension = solvedByDimension.get(p.getDimensions());
    if (polytopesForDimension == null) {
      polytopesForDimension = new ArrayList<Polytope>();
      solvedByDimension.put(p.getDimensions(), polytopesForDimension);
    }
    for (Polytope known: polytopesForDimension) {
      if (p.equivalent(known)) {
        System.out.println("Rediscovered " + known.getName());
        return;
      }
    }
    System.out.println("New polytope " + name + " discovered!");
    p.setName(name);
    p.setId(solved.size());
    polytopesForDimension.add(p);
    solved.add(p);
    nameToPolytopeMap.put(name, p);
    System.out.println("----------------------------------------------");
    System.out.println(p);
  }


  private Polytope get(String name) {
    return nameToPolytopeMap.get(name);
  }

  private void addBasic() {
    add(new Vertex(), "Vertex");
    add(new Edge(copyVertex(), copyVertex()), "Edge");
    
    add(getPolygon(3), "Triangle");
    add(getPolygon(4), "Square");
    add(getPolygon(5), "Pentagon");
    add(getPolygon(6), "Hexagon");
    add(getPolygon(8), "Octagon");
    add(getPolygon(10), "Dekagon");
  }
  
  private Polytope getPolygon(int n) {
    Angle angle = new Angle.RationalPi(n-2, n); // (n-2)PI/n
    Edge e[] = new Edge[n];
    for (int i = 0; i < n; i ++) {
      e[i] = copyEdge();
    }
    Vertex first = e[0].getVertex();
    Vertex last = e[n-1].getVertex();
    e[0].equate(first, last);
    Vertex previous = last;
    for (int i = 0; i < n-1; i ++) {
      Vertex v1 = (Vertex) e[i].getOtherVertex(previous);
      Vertex v2 = (Vertex) (i == n-2? e[i+1].getOtherVertex(last) : e[i+1].getVertex());
      e[i+1].equate(v2, v1);
      previous = v1;
    }
    Polytope p = new Polytope(2);
    for (int i = 0; i < n; i ++) {
      p.add(e[i]);
    }
    last = e[0].getVertex();
    final Vertex vlast = last;
    if (e[1].containsComponent(vertex -> vertex == vlast)) {
      last = e[0].getOtherVertex(last);
    }
    for (int i = 0; i < n; i ++) {
      p.setAngle(last, angle);
      last = (Vertex) e[i].getOtherVertex(last);
    }
    return p;
  }

  private Vertex getVertex() {
    return (Vertex) solvedByDimension.get(0).get(0);
  }
  private Edge getEdge() {
    return (Edge) solvedByDimension.get(1).get(0);
  }
  private Vertex copyVertex() {
    return getVertex().copyVertex();
  }
  private Edge copyEdge() {
    return getEdge().copyEdge();
  }
  @SuppressWarnings("unused")
  private Polytope copy(String name) {
    return get(name).copy();
  }
  // Solution order
  // 1) dimensions
  // 2) Starting point, ie first facet chain
  //    ordered such that 3 facets < 4 facets
  //    facet1.id <= facet2.id <= ....
  //    sum of ids
  // 3) Similarily calculated order of following facet chains
  private void solve(StartingPoint startingPoint) {
    int n = startingPoint.p.n;
    while (true) {
      int theN = n;
      startingPointsIncluding(startingPoint).forEachOrdered(
        start -> {
        spool(start);
        solve(theN, start);
        });
      n++;
      startingPoint = getStartingPoint(n);
    }
  }

  private Stream<StartingPoint> startingPointsIncluding(StartingPoint firstStartingPoint) {
    // 3 - 5 facets around first corner
    return IntStream.range(firstStartingPoint.ids.length, 6)
        .mapToObj(n -> new Integer(n))
        .flatMap(n -> startingPointsIncluding(n,
                                (n > firstStartingPoint.ids.length)?
                                getStartingPoint(firstStartingPoint.p.n, n) :
                                firstStartingPoint));
  }
  private Stream<StartingPoint> startingPointsIncluding(int n, StartingPoint firstStartingPoint) {
    // n facets around first corner
    // order so that starting point ids are increasing (or equal)
    // A bit of ugliness because I want to use lambdas...
    switch (n) {
    case 3: return startingPoints3Including(firstStartingPoint);
    case 4: return startingPoints4Including(firstStartingPoint);
    case 5: return startingPoints5Including(firstStartingPoint);
    default: throw new IllegalArgumentException("can only fold 3-5 polytopes around a corner");
    }
  }
  private Stream<StartingPoint> startingPoints3Including(StartingPoint firstStartingPoint) {
    int n = firstStartingPoint.p.n;
    return solvedByDimension.get(n-1).stream()
      .filter(facet -> facet.id >= firstStartingPoint.ids[0])
      .flatMap(f1 -> solvedByDimension.get(n-1).stream()
        .filter(facet -> f1.id > firstStartingPoint.ids[0]?
                         facet.id >= f1.id :
                         facet.id >= firstStartingPoint.ids[1])
        .flatMap(f2 -> solvedByDimension.get(n-1).stream()
          .filter(facet -> (f1.id > firstStartingPoint.ids[0] ||
                            f1.id == firstStartingPoint.ids[0] && f2.id > firstStartingPoint.ids[1])?
                           facet.id >= f2.id :
                           facet.id >= firstStartingPoint.ids[2])
          .flatMap(f3 -> waysToConnect(f1.copy(), f2.copy(), f3.copy())
       ))); 
  }
  private Stream<StartingPoint> startingPoints4Including(StartingPoint firstStartingPoint) {
    int n = firstStartingPoint.p.n;
    return solvedByDimension.get(n-1).stream()
      .filter(facet -> facet.id >= firstStartingPoint.ids[0])
      .flatMap(f1 -> solvedByDimension.get(n-1).stream()
        .filter(facet -> f1.id > firstStartingPoint.ids[0]?
                         facet.id >= f1.id :
                         facet.id >= firstStartingPoint.ids[1])
        .flatMap(f2 -> solvedByDimension.get(n-1).stream()
          .filter(facet -> (f1.id > firstStartingPoint.ids[0] ||
                            f1.id == firstStartingPoint.ids[0] && f2.id > firstStartingPoint.ids[1])?
                           facet.id >= f2.id :
                           facet.id >= firstStartingPoint.ids[2])
          .flatMap(f3 -> solvedByDimension.get(n-1).stream()
            .filter(facet -> (f1.id > firstStartingPoint.ids[0] ||
                              f1.id == firstStartingPoint.ids[0] && f2.id > firstStartingPoint.ids[1] ||
                              f1.id == firstStartingPoint.ids[0] && f2.id == firstStartingPoint.ids[1] && f3.id > firstStartingPoint.ids[2])?
                              facet.id >= f3.id :
                              facet.id >= firstStartingPoint.ids[3])
            .flatMap(f4 -> waysToConnect(f1.copy(), f2.copy(), f3.copy(), f4.copy())
       )))); 
  }
  private Stream<StartingPoint> startingPoints5Including(StartingPoint firstStartingPoint) {
    int n = firstStartingPoint.p.n;
    return solvedByDimension.get(n-1).stream()
      .filter(facet -> facet.id >= firstStartingPoint.ids[0])
      .flatMap(f1 -> solvedByDimension.get(n-1).stream()
        .filter(facet -> f1.id > firstStartingPoint.ids[0]?
                         facet.id >= f1.id :
                         facet.id >= firstStartingPoint.ids[1])
        .flatMap(f2 -> solvedByDimension.get(n-1).stream()
          .filter(facet -> (f1.id > firstStartingPoint.ids[0] ||
                            f1.id == firstStartingPoint.ids[0] && f2.id > firstStartingPoint.ids[1])?
                           facet.id >= f2.id :
                           facet.id >= firstStartingPoint.ids[2])
          .flatMap(f3 -> solvedByDimension.get(n-1).stream()
            .filter(facet -> (f1.id > firstStartingPoint.ids[0] ||
                              f1.id == firstStartingPoint.ids[0] && f2.id > firstStartingPoint.ids[1] ||
                              f1.id == firstStartingPoint.ids[0] && f2.id == firstStartingPoint.ids[1] && f3.id > firstStartingPoint.ids[2])?
                              facet.id >= f3.id :
                              facet.id >= firstStartingPoint.ids[3])
            .flatMap(f4 -> solvedByDimension.get(n-1).stream()
              .filter(facet -> (f1.id > firstStartingPoint.ids[0] ||
                                f1.id == firstStartingPoint.ids[0] && f2.id > firstStartingPoint.ids[1] ||
                                f1.id == firstStartingPoint.ids[0] && f2.id == firstStartingPoint.ids[1] && f3.id > firstStartingPoint.ids[2] ||
                                f1.id == firstStartingPoint.ids[0] && f2.id == firstStartingPoint.ids[1] && f3.id == firstStartingPoint.ids[2] && f4.id > firstStartingPoint.ids[3])?
                                facet.id >= f4.id :
                                facet.id >= firstStartingPoint.ids[4])
              .flatMap(f5 -> waysToConnect(f1.copy(), f2.copy(), f3.copy(), f4.copy(), f5.copy())
       ))))); 
  }

  private void solve(int n, StartingPoint startingPoint) {
    waysToSolve(startingPoint.p).forEach(p -> add(p));
  }
  // Ways to complete a partially constructed polytope
  // General rule, whenever we return a stream of options based on a initial value,
  // we should return copies of the objects
  private Stream<WorkInProgress> waysToSolve(WorkInProgress p) {
    
    // Select a random unfinished corner 
    Set<Polytope> finishedCorners = p.finishedCorners.keySet();
    Optional<Polytope> unfinishedCorner = p.facets.stream().flatMap(
        facet -> facet.facets.stream().flatMap(
            ridge -> ridge.facets.stream()))
        .filter(corner -> !finishedCorners.contains(corner)).findAny();
    // Done if there are no unfinished corners
    if (!unfinishedCorner.isPresent()) {
      if (p.solveAngles()) {
        return Stream.of(p);
      } else {
        return Stream.empty();
      }
    }
    // and build a facet chain around it
    return waysToSolve(p, unfinishedCorner.get())
    // Then recurse until done
        .flatMap(wip -> waysToSolve(wip));
  }
  
  // Ways to complete one incomplete corner of a partially constructed polytope
  // while respecting neighboring facet chains
  private Stream<WorkInProgress> waysToSolve(WorkInProgress p, Polytope corner) {
    //p = p.copyWiP();

    // Create a FacetChain based on already existing facets
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = p.getRidgeToFacetMap();
    
    Set<Polytope> neighborRidges = p.facets.stream()
      .flatMap(facet -> facet.facets.stream()
        .filter(ridge -> ridge.facets.contains(corner)))
       .collect(Collectors.toSet());
    
    // If algorithm works as supposed, neighboring facets will form a partial, connected facet chain.
    Map<Polytope, List<Polytope>> facetToFacetMap = new HashMap<Polytope, List<Polytope>>();
    Polytope openFacet = null;
    for (Polytope ridge: neighborRidges) {
      Set<Polytope> facets = new HashSet<Polytope>(ridgeToFacetMap.get(ridge));
      Iterator<Polytope> it = facets.iterator();
      if (facets.size() == 2) {
        Polytope facet1 = it.next();
        Polytope facet2 = it.next();
        safeAdd(facetToFacetMap, facet1, facet2);
        safeAdd(facetToFacetMap, facet2, facet1);
      } else {
        openFacet = it.next();
      }
    }
    FacetChain facetChain = p.newFacetChain(corner);
    if (facetToFacetMap.isEmpty()) {
      // Just a single facet. Choose any neighborRidge.
      Set<Polytope> ridges = new HashSet<Polytope>(openFacet.facets);
      ridges.retainAll(neighborRidges);
      Polytope ridge = ridges.iterator().next();
      facetChain.add(ridge, openFacet);
    } else {
      // Choose ridge not in neighborRidges containing corner
      // Just a minute! neighborRidges is all ridges containing corner??
      // We actually mean open ridges containing corner 
      // Needs more thought...
      // Example: tetrahedron
      // start with 3 triangles around p1
      // next add fan around p2
      // We now have all four facets, but the ridge p3-p4 is not connected
      // That is, we have two ridges with the same corners, but they have not been equated
      // When completing p3 we should have two open ridges containing the same set of corners and equate them... probably in waysToComplete that return single way?
      // Finally, when completing p4 we should have no open ridges at all
      Polytope ridge1;
      Polytope nextToLastFacet = null;
      if (openFacet == null) {
        // Already done, but continue filling in the facetChains in case some other corner needs completing
        // We can pick any facet
        ridge1 = neighborRidges.iterator().next();
        openFacet = ridgeToFacetMap.get(ridge1).iterator().next(); // Not really open
        nextToLastFacet = facetToFacetMap.get(openFacet).iterator().next();
      } else {
        Optional<Polytope> maybeRidge = openFacet.facets.stream().filter(ridge -> ridge.facets.contains(corner) && ridgeToFacetMap.get(ridge).size() < 2).findAny();
        if (!maybeRidge.isPresent()) {
          throw new IllegalArgumentException("Logic error, ridge does not exist");
        }
        ridge1 = maybeRidge.get();
      }
      facetChain.add(ridge1, openFacet);
      // Then follow facetToFacet map
      Polytope lastFacet = openFacet;
      Polytope facet;
      while ((facet = getOther(facetToFacetMap, lastFacet, nextToLastFacet)) != null && facet != openFacet)  {
        Set<Polytope> ridges = new HashSet<Polytope>(lastFacet.facets);
        ridges.retainAll(facet.facets);
        facetChain.add(ridges.iterator().next(), facet);
        nextToLastFacet = lastFacet;
        lastFacet = facet;
      }
    }
    // Problem due to not enough immutability!
    // Completing a facetChain WILL modify the polytope it's created from (equating edges) ruining backtracking!
    // Example: start with fan of 3 triangles.
    // Try adding another chain of 3 triangles
    // After some steps, tetrahedron, and fourth triangle has had it's edges equated
    // backtrack, extend to chain of four triangles ->
    // Fourth triangle will be attached to an edge that's already closed!
    // Solution: add link from facetChain to WorkInProgress, and copy whole workInProgress whenever a facetChain is closed
    
    facetChain.getWorkInProgress().check();
    return waysToComplete(facetChain).flatMap(
        chain -> {
        //Polytope pDbg = p;
        Map<Polytope, Polytope> equivalences = new HashMap<Polytope, Polytope>();
//        WorkInProgress result = p.copyWiP(equivalences);
        FacetChain chainCopy = chain.copyWorkInProgress(equivalences);
        WorkInProgress result = chainCopy.getWorkInProgress();
//        chainCopy.close();
        result.addFinishedCorner(chainCopy);
        result.facets.addAll(chainCopy.facets);
        result.coalesceRidges();
        result.check();
        System.out.println("Finished a corner... Current finished corners:");
        for (Polytope corner2: result.finishedCorners.keySet()) {
          System.out.println(result.finishedCorners.get(corner2));
        }
        return waysToSolve(result);
    });
  }
 
  private <Key, Value> Value getOther(Map<Key, List<Value>> map,  Key key, Value exclude) {
    List<Value> values = map.get(key);
    if (values == null) {
      return null;
    }
    for (Value value: values) {
      if (value != exclude) {
        return value;
      }
    }
    return null;
  }

  private Stream<FacetChain> waysToComplete(FacetChain facetChain) {
    Polytope facet = facetChain.facets.get(facetChain.facets.size() - 1);
    Polytope firstRidge = facetChain.firstRidge();
    Polytope lastRidge = facetChain.lastRidge();
    if (firstRidge == lastRidge) {
      // Already completed
      facetChain.getWorkInProgress().check();
      return Stream.of(facetChain);
    } else if (firstRidge.facets.equals(lastRidge.facets)) {
      // Unconnected but equivalent. Equate.
      Map<Polytope, Polytope> equivalences = new HashMap<Polytope, Polytope>();
      FacetChain result1 = facetChain.copyWorkInProgress(equivalences);
      Polytope newFirstRidge = equivalences.get(firstRidge);
      Polytope newLastRidge = equivalences.get(lastRidge);
      equivalences = new HashMap<Polytope, Polytope>();
      equivalences.put(newLastRidge, newFirstRidge);
      for (Polytope facet1: result1.facets) {
        facet1.equate(equivalences);
      }
      result1.getWorkInProgress().check();
      return Stream.of(result1);
    }
    FacetChain result1 = facetChain.copyWorkInProgress();
    return Stream.concat(
        result1.close()? Stream.of(result1) : Stream.empty(),
        waysToSelect1(facet.n).flatMap(
            newFacet -> facet.waysToConnect(newFacet, lastRidge).flatMap(
                equivalences -> {
                  Polytope facetDbg = facet;
                  Polytope newFacetDbg = newFacet;
                  Polytope lastRidgeDbg = lastRidge;
                  FacetChain facetChainDbg = facetChain;
                  Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
                  FacetChain result = facetChain.copyWorkInProgress(replacements);
                  Polytope newFacetCopy = newFacet.copy(compose(equivalences.p2p1, replacements));
//                  newFacetCopy.equate(compose(equivalences.p2p1, replacements));
//                  Equivalences orgEqv = new Equivalences(equivalences);
//                  Polytope newFacetCopy = newFacet.copy(equivalences.p2p1);
//                  result.add(lastRidge, newFacetCopy);
                  result.add(replacements.get(lastRidge), newFacetCopy);
//                  return result;
                  result.getWorkInProgress().check();
                  return waysToComplete(result);
                })));
  }
  private static <Key, Value> Map<Value, Key> invert(Map<Key, Value> map) {
    Map<Value, Key> result = new HashMap<Value, Key>();
    for (Key key: map.keySet()) {
      result.put(map.get(key), key);
    }
    return result;
  }

  private static <T1, T2, T3> Map<T1, T3> compose(Map<T1, T2> m1, Map<T2, T3> m2) {
    Map<T1, T3> result = new HashMap<T1, T3>();
    for (T1 t1: m1.keySet()) {
      T2 t2 = m1.get(t1);
      if (t2 != null && m2.containsKey(t2)) {
        result.put(t1,  m2.get(t2));
      }
    }
    return result;
  }

  private Stream<Polytope> waysToSelect1(int n) {
    return solvedByDimension.get(n).stream();
  }
  
//  private void fakeSolve() {
//    // New version, more like final goal
//    
//    // Fold 3 facets < 360 around a ridge
//    int n = 2;
//    waysToConnect3(n).forEach(
//        way -> {
//        WorkInProgress p = new WorkInProgress(n+1);
//        p.add(way.f1);
//        p.add(way.f2);
//        p.add(way.f3);
//        Fold3Solution solution = Fold3Solution.givenFacetAngles(way.f1.getAngle(way.ridge), way.f2.getAngle(way.ridge), way.f3.getAngle(way.ridge));
//        Polytope f1f2 = way.f1.facets.stream().filter(ridge -> way.f2.facets.contains(ridge)).findAny().get();
//        Polytope f1f3 = way.f1.facets.stream().filter(ridge -> way.f3.facets.contains(ridge)).findAny().get();
//        Polytope f2f3 = way.f2.facets.stream().filter(ridge -> way.f3.facets.contains(ridge)).findAny().get();
//        p.setAngle(f1f2, solution.v12);
//        p.setAngle(f2f3, solution.v23);
//        p.setAngle(f1f3, solution.v31);
//        close(p);
//    });
//  }
  private void close(WorkInProgress p) {
    // Try closing polytope
    // Add all open ridges to final facet
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = p.getRidgeToFacetMap();
    Set<Polytope> openRidges = ridgeToFacetMap.keySet().stream().filter(ridge -> ridgeToFacetMap.get(ridge).size() < 2).collect(Collectors.toSet());
    Polytope finalFacet = new Polytope(p.getDimensions() - 1);
    Set<Polytope> corners = new HashSet<Polytope>();
    openRidges.stream().forEach(ridge -> {
      finalFacet.add(ridge);
      corners.addAll(ridge.facets);
    });
    // Solve remaining angles
    boolean done = true;
    Map<Polytope, Angle> newAngles = new HashMap<Polytope, Angle>();
    for (Polytope corner: corners) {
      CornerAngles cornerAngles = p.getAngles(corner);
      System.out.println(cornerAngles);
      if (cornerAngles.facetAngles.size() != 2 || cornerAngles.dihedralAngles.size() != 1) {
        done = false;
        break;
      }
      Iterator<Polytope> it = cornerAngles.facetAngles.keySet().iterator();
      Polytope facet1 = it.next();
      Polytope facet2 = it.next();
      DihedralAngle dihedralAngle = cornerAngles.dihedralAngles.values().iterator().next();
      // Sanity check, the two facets must be the same as for the dihedral angle
      if (!(facet1 == dihedralAngle.facet1 && facet2 == dihedralAngle.facet2 ||
            facet1 == dihedralAngle.facet2 && facet2 == dihedralAngle.facet1)) {
        done = false;
        break;
      }
      Fold3Solution solution2 = Fold3Solution.givenTwoFacetAnglesAndDihedralAngle(cornerAngles.facetAngles.get(facet1), cornerAngles.facetAngles.get(facet2), dihedralAngle.angle);
      System.out.println(solution2);
      Polytope ridgeF1F3 = facet1.facets.stream().filter(ridge -> finalFacet.facets.contains(ridge)).findAny().get();
      Polytope ridgeF2F3 = facet2.facets.stream().filter(ridge -> finalFacet.facets.contains(ridge)).findAny().get();
      newAngles.put(ridgeF1F3, solution2.v31 );
      newAngles.put(ridgeF2F3, solution2.v23);
    }
    // Every corner was closed, we are done
    if (done) {
      // Close a copy, we need to continue
      Map<Polytope, Polytope> replacementMap = new HashMap<Polytope, Polytope>();
      WorkInProgress pDone = (WorkInProgress) p.copy(replacementMap);
      pDone.add(finalFacet.copy(replacementMap));
      pDone.validate();
      add(pDone);
    }
    // Next, pick a random open (n-3) face and try every way to
  }
  
  
  
  
  

  private class WayToConnect3 {
    public Polytope f1, f2, f3;
    public Polytope ridge;
    //public Map<Polytope, Polytope> equivalences;
    public WayToConnect3(Polytope f1, Polytope f2, Polytope f3, Polytope ridge) {
      this.f1 = f1;
      this.f2 = f2;
      this.f3 = f3;
      this.ridge = ridge;
    }
  }
  private Stream<StartingPoint> waysToConnect3(int n) {
    return solvedByDimension.get(n).stream().flatMap(
        f1 -> solvedByDimension.get(n).stream().flatMap(
            f2 -> solvedByDimension.get(n).stream().flatMap(
                f3 -> waysToConnect(f1.copy(), f2.copy(), f3.copy())
            )));
  }
  
  // Reworked version creating a starting point of 3-5 facets around a corner
  private Stream<StartingPoint> waysToConnect(Polytope f1, Polytope f2, Polytope f3, Polytope f4, Polytope f5) {
    // For all ways to connect f1 to f2
    return f1.waysToConnect(f2).flatMap(
        equivalencesf1f2 -> {
        // The equivalences returned by waysToConnect contains a single facet (n-1) mapping and mapping of subpolytopes.
        // The mapping is supposed to transform f1's subpolytopes to f2 terms.
        // In other words, the keys of the map are in f1 terms, the values in f2 terms.
        Polytope facetf1f2_1 = equivalencesf1f2.p1p2.keySet().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
        Polytope facetf1f2_2 = equivalencesf1f2.p1p2.get(facetf1f2_1);
        Polytope f1_2 = f1.copy(equivalencesf1f2.p1p2); // Map f1 to f2 terms (ie connect them around facetf1f2
        // For each ridge in facet, find neighbor facet in f2
        return facetf1f2_2.facets.stream().flatMap(
            // This is the ridge we will keep folding around (in f2 terms hence the 2)
            ridge2 -> {
            Polytope neighborInF1_2 = f1_2.getOtherFacet(facetf1f2_2, ridge2);
            Polytope neighborInF2 = f2.getOtherFacet(facetf1f2_2, ridge2);
            // For all ways to connect neighbor in f2 to f3
            return f2.waysToConnect(f3, neighborInF2).flatMap(
                equivalencesf2f3 -> {
                Polytope facetf2f3 = equivalencesf2f3.p2p1.keySet().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
                Polytope f1_3 = f1_2.copy(equivalencesf2f3.p1p2); // Keep mapping and connecting
                Polytope f2_3 = f2.copy(equivalencesf2f3.p1p2);
                Polytope neighborInF1_3 = neighborInF1_2.copy(equivalencesf2f3.p1p2);
                // Keep using same ridge (but in new terms)
                Polytope ridge3 = equivalencesf2f3.p1p2.get(ridge2);
                Polytope neighborInF3 = f3.getOtherFacet(facetf2f3, ridge3);
                return f3.waysToConnect(f4, neighborInF3).flatMap(
                    equivalencesf3f4 -> {
                    Polytope facetf3f4 = equivalencesf3f4.p2p1.keySet().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
                    Polytope f1_4 = f1_3.copy(equivalencesf3f4.p1p2);
                    Polytope f2_4 = f2_3.copy(equivalencesf3f4.p1p2);
                    Polytope f3_4 = f3.copy(equivalencesf3f4.p1p2);
                    Polytope ridge4 = equivalencesf3f4.p1p2.get(ridge3);
                    Polytope neighborInF1_4 = neighborInF1_3.copy(equivalencesf3f4.p1p2);
                    Polytope neighborInF4 = f4.getOtherFacet(facetf3f4, ridge4);
                    // And finally, f5 should connect to both f1 and f4 (using neighboring facets)
                    // Note that mapping is from f5 to f1_4 and f4
                    return f5.waysToConnectFacets(neighborInF1_4, neighborInF4).map(
                        equivalencesf5f4 -> {
                        Polytope f5_4 = f5.copy(equivalencesf5f4.p1p2);
                        WorkInProgress wip = new WorkInProgress(f1.n + 1);
                        wip.add(f1_4);
                        wip.add(f2_4);
                        wip.add(f3_4);
                        wip.add(f4);
                        wip.add(f5_4);
                        return new StartingPoint(wip);
                        });
                    });
                });
            });
         });
  }
  // Might have been able to do a single version with different number of facets, but my brain is melting after writing the above :-)
  private Stream<StartingPoint> waysToConnect(Polytope f1, Polytope f2, Polytope f3, Polytope f4) {
    return f1.waysToConnect(f2).flatMap(
        equivalencesf1f2 -> {
        Polytope facetf1f2 = equivalencesf1f2.p2p1.keySet().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
        Polytope f1_2 = f1.copy(equivalencesf1f2.p1p2);
        return facetf1f2.facets.stream().flatMap(
            ridge2 -> {
            Polytope neighborInF2 = f2.getOtherFacet(facetf1f2, ridge2);
            Polytope neighborInF1_2 = f1_2.getOtherFacet(facetf1f2, ridge2);
            return f2.waysToConnect(f3, neighborInF2).flatMap(
                equivalencesf2f3 -> {
                Polytope facetf2f3 = equivalencesf2f3.p2p1.keySet().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
                Polytope f1_3 = f1_2.copy(equivalencesf2f3.p1p2);
                Polytope f2_3 = f2.copy(equivalencesf2f3.p1p2);
                // Keep using same ridge (but in new terms)
                Polytope ridge3 = equivalencesf2f3.p1p2.get(ridge2);
                Polytope neighborInF3 = f3.getOtherFacet(facetf2f3, ridge3);
                Polytope neighborInF1_3 = neighborInF1_2.copy(equivalencesf2f3.p1p2);
                return f4.waysToConnectFacets(neighborInF1_3, neighborInF3).map(
                    equivalencesf4f3 -> {
                    Polytope f4_3 = f4.copy(equivalencesf4f3.p1p2);
                    WorkInProgress wip = new WorkInProgress(f1.n + 1);
                    wip.add(f1_3);
                    wip.add(f2_3);
                    wip.add(f3);
                    wip.add(f4_3);
                    return new StartingPoint(wip);
                    });
                });
            });
         });
  }
  private Stream<StartingPoint> waysToConnect(Polytope f1, Polytope f2, Polytope f3) {
    return f1.waysToConnect(f2).flatMap(
        equivalencesf1f2 -> {
          Polytope dbg_f1 = f1;
          Polytope dbg_f2 = f2;
          Polytope dbg_f3 = f3;
        Polytope facetf1f2 = equivalencesf1f2.p2p1.keySet().stream().filter(facet -> f2.facets.contains(facet)).findFirst().get();
        Polytope f1_2 = f1.copy(equivalencesf1f2.p1p2);
        
        return facetf1f2.facets.stream().flatMap(
            ridge2 -> {
            Polytope neighborInF2 = f2.getOtherFacet(facetf1f2, ridge2);
            Polytope neighborInF1_2 = f1_2.getOtherFacet(facetf1f2, ridge2);
            return f3.waysToConnectFacets(neighborInF1_2, neighborInF2).map(
                equivalencesf3f2 -> {
                  Polytope dbG_f1 = f1;
                  Polytope dbG_f2 = f2;
                  Polytope dbG_f1_2 = f1_2;
                  Polytope dbG_f3 = f3;
                Polytope f3_2 = f3.copy(equivalencesf3f2.p1p2);
                WorkInProgress wip = new WorkInProgress(f1.n + 1);
                wip.add(f1_2);
                wip.add(f2);
                wip.add(f3_2);
                StartingPoint result = new StartingPoint(wip);
                System.out.println("Trying " + result.shortDescription());
                return result;
                });
            });
         });
  }

//  private Stream<WayToConnect3> waysToConnect(Polytope f1, Polytope f2, Polytope f3) {
//    // For all ways to connect f1 to f2
//    return f2.waysToConnect(f1).flatMap(
//        equivalencesf2f1 -> {
//        // Connect, find facet that was connected
//        connectAndCopy(f2, f1, equivalencesf2f1);
//        Polytope f1Copy = equivalencesf2f1.get(f1);
//        Polytope f2Copy = equivalencesf2f1.get(f2);
//        Polytope facetf1f2 = equivalencesf2f1.values().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
//        // For each ridge in facet, find neighbor facets in f1 and f2
//        return facetf1f2.facets.stream().flatMap(
//            ridge -> {
//            Polytope neighborInF1 = f1Copy.getOtherFacet(facetf1f2, ridge);
//            Polytope neighborInF2 = f2Copy.getOtherFacet(facetf1f2, ridge);
//            // Connect both ridges to  f3
//            //return Stream.of(new WayToConnect3());
//            return f3.waysToConnectFacets(neighborInF1, neighborInF2)
//                .map(
//                equivalencesf3f1f2-> {
//                Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
//                Polytope f3Copy = f3.copy(replacements);
//                Map<Polytope, Polytope> equivalencesCopy = new HashMap<Polytope, Polytope>();
//                for (Polytope p: equivalencesf3f1f2.keySet()) {
//                  equivalencesCopy.put(replacements.get(p), equivalencesf3f1f2.get(p));
//                }
//                f3Copy.equate(equivalencesCopy);
//                return new WayToConnect3(f1Copy, f2Copy, f3Copy, ridge);
//            }).filter(way -> way.f1.getAngle(way.ridge).getAngle() +
//                             way.f2.getAngle(way.ridge).getAngle() +
//                             way.f3.getAngle(way.ridge).getAngle() < 2*Math.PI);
//        });
//    });
//        return facetf1f2.facets.stream().flatMap(
//            ridge ->
//            return new WayToConnect3();
//            )});
//            .flatMap(
//                facetf1f3 -> f3.facets.stream().flatMap(
//                    facetf3 -> facetf3.waysToEquate(facetf1f3).flatMap(
//                        equivalencesf3f1 -> {
//                          f3.equate(equivalencesf3f1);
//                          Polytope facetf1f3 = equivalences.values().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
//                          return new WayToConnect3();
//                        }
//                    )
//                )
//            )
//        });
//  }

//  private void fakeSolveOld() {
//    
//    // Add three triangles around a vertex
//    Polytope f1 = copy("Triangle");
//    Polytope f2 = copy("Triangle");
//    Polytope f3 = copy("Triangle");
//    
//    // Random edge of first triangle
//    Polytope f1e1 = f1.getRandomFacet();
//    // Random vertex to fold around
//    Polytope f1e1v1 = f1e1.getRandomFacet();
//    
//    // Random edge of triangle 2 to connect to above edge 
//    Polytope f2e1 = f2.getRandomFacet();
//    // Connect edges
//    f2.equate(f2e1, f1e1);     f2e1 = f1e1;
//    
//    // The two open edges of triangle 1 and triangle 2 that adjoin the vertex
//    Polytope f1e2 = f1.getOtherFacet(f1e1, f1e1v1);
//    Polytope f2e2 = f2.getOtherFacet(f1e1, f1e1v1);
//    
//    // Two random edges of triangle 3 to connect to the above
//    Polytope f3e1 = f3.getRandomFacet();
//    Polytope f3e1v1 = f3e1.getRandomFacet();
//    Polytope f3e2 = f3.getOtherFacet(f3e1, f3e1v1);
//    // Connect
//    f3.equate(f3e1, f1e2);     f3e1 = f1e2;
//    f3.equate(f3e2, f2e2);     f3e2 = f2e2;
//    
//    // Calculate angles
//    Angle angles[] = fold3(f1.getAngle(f1e1v1), f2.getAngle(f1e1v1), f3.getAngle(f1e1v1));
//    Angle f2f3 = angles[0];
//    Angle f1f3 = angles[1];
//    Angle f1f2 = angles[2];
//    
//    // Fold
//    Polytope tetrahedron = new Polytope(3);
//    tetrahedron.add(f1);
//    tetrahedron.add(f2);
//    tetrahedron.add(f3);
//    tetrahedron.setAngle(f1e1, f1f2);
//    tetrahedron.setAngle(f1e2, f1f3);
//    tetrahedron.setAngle(f2e2, f2f3);
//    
//    // Three remaining open edges
//    Polytope f1e3 = f1.getComponent(edge -> edge.n == 1 && edge != f1e1 && edge != f1e2);
//    Polytope f2e3 = f2.getComponent(edge -> edge.n == 1 && edge != f1e1 && edge != f2e2);
//    Polytope f3e3 = f3.getComponent(edge -> edge.n == 1 && edge != f1e2 && edge != f2e2);
//    
//    // Connect
//    Polytope f4 = copy("Triangle");
//    Polytope f4e1 = f4.getRandomFacet();
//    Polytope f4e2 = f4.getComponent(edge -> edge.n == 1 && edge != f4e1);
//    Polytope e2 = f4e2;
//    Polytope f4e3 = f4.getComponent(edge -> edge.n == 1 && edge != f4e1 && edge != e2);
//    f4.equate(f4e1, f1e3);   //f4e1 = f1e3
//    Polytope f4e1v1 = f4e1.getRandomFacet();
//    if (!f2e3.containsComponent(v -> v == f4e1v1)) {
//      Polytope t = f4e2;
//      f4e2 = f4e3;
//      f4e3 = t;
//    }
//    f4.equate(f4e2, f2e3);   //f4e2 = f2e3;
//    f4.equate(f4e3, f3e3);   //f4e3 = f3e3;
//   
//    // Cheat, should be recalculated
//    Angle f1f4 = f1f2;
//    Angle f2f4 = f1f2;
//    Angle f3f4 = f1f2;
//    tetrahedron.add(f4);
//    tetrahedron.setAngle(f1e3, f1f4);
//    tetrahedron.setAngle(f2e3, f2f4);
//    tetrahedron.setAngle(f3e3, f3f4);
//    
//    add(tetrahedron, "Tetrahedron");
//    
//  }
  
  private Polytope connectAndCopy(Polytope f1, Polytope f2, Map<Polytope, Polytope> equivalencesf1f2) {
    Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
    Polytope f1Copy = f1.copy(replacements);
    Polytope f2Copy = f2.copy(replacements);
    Map<Polytope, Polytope> equivalencesf1copyf2copy = new HashMap<Polytope, Polytope>();
    for (Polytope p: equivalencesf1f2.keySet()) {
      equivalencesf1copyf2copy.put(replacements.get(p), replacements.get(equivalencesf1f2.get(p)));
    }
    equivalencesf1f2.clear();
    for (Polytope p: equivalencesf1copyf2copy.keySet()) {
      equivalencesf1f2.put(p, equivalencesf1copyf2copy.get(p));
    }
    Polytope result = new Polytope(f1.getDimensions() + 1);
    result.add(f1Copy);
    result.add(f2Copy);
    f1Copy.equate(equivalencesf1f2);
    equivalencesf1f2.put(f1, f1Copy);
    equivalencesf1f2.put(f2, f2Copy);
    return result;
  }

 
}
