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
import java.nio.file.Files;
import java.sql.Types;
import java.util.ArrayList;
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
import java.util.stream.Stream;

import se.pp.forsberg.polytope.solver.Angle.TrinaryAngle;
import se.pp.forsberg.polytope.solver.WorkInProgress.CornerAngles;
import se.pp.forsberg.polytope.solver.WorkInProgress.DihedralAngle;
import se.pp.forsberg.polytope.solver.WorkInProgress.FacetChain; 

// As per usual terminology, in a n-polytope
// a n-1 polytope is a facet
// a n-2 polytope is a ridge
// in addition, a n-3 polytope is a corner
public class PolytopeSolver {
  
  private List<Polytope> solved = new ArrayList<Polytope>();
  private Map<Integer, List<Polytope>> solvedByDimension = new HashMap<Integer, List<Polytope>>();
  private Map<String, Polytope> nameToPolytopeMap = new HashMap<String, Polytope>();
  
  private final static File spoolFile = new File(new File(System.getProperty("user.home")), "polytopes.txt");
  
  // (Re-)Starting point of calculation is currently a work in progress with just one facet chain set
  private class StartingPoint {
    WorkInProgress p;
    public StartingPoint(WorkInProgress p) {
      this.p = p;
    }
    @Override
    public String toString() {
      return "\n" + p.toString();
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
  Pattern startingPointDefinition = Pattern.compile("Currently trying");
  
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
      if (!line.startsWith("----") || (line = line(in, lineNumber)) == null) {
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
      Polytope f1 = get("Triangle").copy();
      Polytope f2 = get("Triangle").copy();
      Polytope f3 = get("Triangle").copy();
      Vertex v1 = (Vertex) get("Vertex").copy();
      Vertex v2 = (Vertex) get("Vertex").copy();
      Vertex v3 = (Vertex) get("Vertex").copy();
      Vertex v4 = (Vertex) get("Vertex").copy();
      Edge e1 = new Edge(v1, v2); 
      Edge e2 = new Edge(v2, v3); 
      Edge e3 = new Edge(v3, v1); 
      Edge e4 = new Edge(v1, v4);
      Edge e5 = new Edge(v2, v4);
      Edge e6 = new Edge(v3, v4);
      Map<Polytope, Polytope> equivalences = new HashMap<Polytope, Polytope>();
      Iterator<Polytope> it = f1.facets.iterator();
      equivalences.put(it.next(), e1);
      equivalences.put(it.next(), e2);
      equivalences.put(it.next(), e3);
      f1.equate(equivalences);
      it = f2.facets.iterator();
      equivalences.put(it.next(), e1);
      equivalences.put(it.next(), e4);
      equivalences.put(it.next(), e5);
      f2.equate(equivalences);
      it = f3.facets.iterator();
      equivalences.put(it.next(), e2);
      equivalences.put(it.next(), e5);
      equivalences.put(it.next(), e6);
      f3.equate(equivalences);
      
      WorkInProgress p = new WorkInProgress(3);
      p.add(f1);
      p.add(f2);
      p.add(f3);
      startingPoint = new StartingPoint(p);
    }
    solve(startingPoint);
  }
  private Polytope readPolytope(BufferedReader in, List<Angle> angles, int... lineNumber) {
    return readPolytope(in, angles, Polytope.class, lineNumber);
  }
  private <T extends Polytope> T readPolytope(BufferedReader in, List<Angle> angles, Class<T> clazz, int... lineNumber) {
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

  private void spool(StartingPoint startingPoint) {
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
  private void solve(StartingPoint startingPoint) {
    int n = startingPoint.p1.n + 1;
    while (true) {
      solve(n, startingPoint);
      Polytope p = solvedByDimension.get(n).get(0);
      startingPoint = new StartingPoint(p, p, p);
      n++;
    }
  }

  private void solve(int n, StartingPoint startingPoint) {
    waysToSolve(new WorkInProgress(n), startingPoint).forEach(p -> add(p));
  }
  // Ways to complete a partially constructed polytope
  // General rule, whenever we return a stream of options based on a initial value,
  // we should return copies of the objects
  private Stream<WorkInProgress> waysToSolve(WorkInProgress p, StartingPoint startingPoint) {
    // If empty, restart at starting point
    if (p.isEmpty()) {
      Polytope p1 = startingPoint.p1.copy();
      Polytope p2 = startingPoint.p2.copy();
      Polytope p3 = startingPoint.p3.copy();
      p.add(p1);
      return waysToSelect1(p.n-1).flatMap(facet -> {
        WorkInProgress p2 = new WorkInProgress(p.n);
        p2.add(facet);
        return waysToSolve(p2);
      });
    }
    // Otherwise, select a random unfinished corner 
    Set<Polytope> finishedCorners = p.finishedCorners.keySet();
    Optional<Polytope> unfinishedCorner = p.facets.stream().flatMap(
        facet -> facet.facets.stream().flatMap(
            ridge -> facet.facets.stream()))
        .filter(corner -> !finishedCorners.contains(corner)).findAny();
    // Done if there are no unfinished corners
    if (!unfinishedCorner.isPresent()) {
      return Stream.of(p);
    }
    // and build a facet chain around it
    return waysToSolve(p, unfinishedCorner.get())
    // Then recurse until done
        .flatMap(wip -> waysToSolve(wip));
  }
  
  // Ways to complete one incomplete corner of a partially constructed polytope
  // while respecting neighboring facet chains
  private Stream<WorkInProgress> waysToSolve(WorkInProgress p, Polytope corner) {
    // Create a FacetChain based on already existing facets
    
    Map<Polytope, Set<Polytope>> ridgeToFacetMap = p.getRidgeToFacetMap();
    
    Set<Polytope> neighborRidges = p.facets.stream()
      .flatMap(facet -> facet.facets.stream()
        .filter(ridge -> ridge.facets.contains(corner)))
       .collect(Collectors.toSet());
    
    // If algorithm works as supposed, neighboring facets will form a partial, connected facet chain.
    Map<Polytope, Polytope> facetToFacetMap = new HashMap<Polytope, Polytope>();
    Polytope openFacet = null;
    for (Polytope ridge: neighborRidges) {
      Set<Polytope> facets = new HashSet<Polytope>(ridgeToFacetMap.get(ridge));
      Iterator<Polytope> it = facets.iterator();
      if (facets.size() == 2) {
        Polytope facet1 = it.next();
        Polytope facet2 = it.next();
        facetToFacetMap.put(facet1,  facet2);
        facetToFacetMap.put(facet2,  facet1);
      } else {
        openFacet = it.next();
      }
    }
    FacetChain facetChain = p.new FacetChain(corner);
    if (facetToFacetMap.isEmpty()) {
      // Just a single facet. Choose any neighborRidge.
      Set<Polytope> ridges = new HashSet<Polytope>(openFacet.facets);
      ridges.retainAll(neighborRidges);
      Polytope ridge = ridges.iterator().next();
      facetChain.add(ridge, openFacet);
    } else {
      // Choose ridge not in neighborRidges containing corner
      Polytope ridge1 = openFacet.facets.stream().filter(ridge -> !neighborRidges.contains(ridge) && ridge.facets.contains(corner)).findAny().get();
      facetChain.add(ridge1, openFacet);
      // Then follow facetToFacet map
      Polytope lastFacet = openFacet;
      Polytope facet;
      while ((facet = facetToFacetMap.get(lastFacet)) != null)  {
        Set<Polytope> ridges = new HashSet<Polytope>(lastFacet.facets);
        ridges.retainAll(facet.facets);
        facetChain.add(facet, ridges.iterator().next());
      }
    }
    return waysToComplete(facetChain).flatMap(
        chain -> {
        Map<Polytope, Polytope> equivalences = new HashMap<Polytope, Polytope>();
        WorkInProgress result = p.copyWiP(equivalences);
        FacetChain chainCopy = facetChain.copy(equivalences);
        result.finishedCorners.put(corner, chainCopy);
        return waysToSolve(result);
    });
  }
 
  private Stream<FacetChain> waysToComplete(FacetChain facetChain) {
    Polytope facet = facetChain.facets.get(facetChain.facets.size() - 1);
    Polytope ridge = facetChain.lastRidge();
    FacetChain result1 = facetChain.copy();
    return Stream.concat(
        result1.close()? Stream.of(result1) : Stream.empty(),
        waysToSelect1(facet.n).flatMap(
            newFacet -> newFacet.waysToConnect(facet, ridge).map(
                equivalences -> {
                  FacetChain result = facetChain.copy();
                  Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
                  Polytope newFacetCopy = newFacet.copy(replacements);
                  newFacetCopy.equate(compose(equivalences, replacements));
                  result.add(ridge, newFacetCopy);
                  return result;
                })));
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
  
  private void fakeSolve() {
    // New version, more like final goal
    
    // Fold 3 facets < 360 around a ridge
    int n = 2;
    waysToConnect3(n).forEach(
        way -> {
        WorkInProgress p = new WorkInProgress(n+1);
        p.add(way.f1);
        p.add(way.f2);
        p.add(way.f3);
        Fold3Solution solution = Fold3Solution.givenFacetAngles(way.f1.getAngle(way.ridge), way.f2.getAngle(way.ridge), way.f3.getAngle(way.ridge));
        Polytope f1f2 = way.f1.facets.stream().filter(ridge -> way.f2.facets.contains(ridge)).findAny().get();
        Polytope f1f3 = way.f1.facets.stream().filter(ridge -> way.f3.facets.contains(ridge)).findAny().get();
        Polytope f2f3 = way.f2.facets.stream().filter(ridge -> way.f3.facets.contains(ridge)).findAny().get();
        p.setAngle(f1f2, solution.v12);
        p.setAngle(f2f3, solution.v23);
        p.setAngle(f1f3, solution.v31);
        close(p);
    });
  }
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
  
  // Solve three facets connected around a (n-3) face
  private static class Fold3Solution {
    // The three facets internal angles at (n-3) face
    public Angle v1, v2, v3;
    // The three dihedral angles between pairs of facets 
    public Angle v12, v23, v31;
    
    // v12 given v1 v2 v3
    private static TrinaryAngle.Value valueGivenFacetAngles = (v1, v2, v3) -> acos((cos(v3) - cos(v1)*cos(v2))/(sin(v1)*sin(v2)));
    private static String descriptionGivenFacetAngles = "acos((cos(%3$s) - cos(%1$s)*cos(%2$s))/(sin(%1$s)*sin(%2$s)))";
    // v3 given v1 v2 v12
    private static TrinaryAngle.Value valueGivenTwoFacetAnglesAndDihedralAngle = (v1, v2, v12) -> acos(cos(v12)*sin(v1)*sin(v2) + cos(v1)*cos(v2));
    private static String descriptionGivenTwoFacetAnglesAndDihedralAngle = "acos(cos(%3$s)*sin(%1$s)*sin(%2$s) + cos(%1$s)*cos(%2$s))";
    
    private Fold3Solution() {}
    
    // Relations
    
    // dihedral angles given facet angles
    // cos(v12) = (cos(v3) - cos(v1)*cos(v2))/(sin(v1)*sin(v2)))
    // cos(v23) = (cos(v1) - cos(v2)*cos(v3))/(sin(v2)*sin(v3)))
    // cos(v31) = (cos(v2) - cos(v3)*cos(v1))/(sin(v3)*sin(v1)))
    
    // facet angles given two other facet angles and one dihedral angle
    // cos(v1) = cos(v23)sin(v2)sin(v3) + cos(v2)cos(v3)
    // cos(v2) = cos(v31)sin(v3)sin(v1) + cos(v3)cos(v1)
    // cos(v3) = cos(v12)sin(v1)sin(v2) + cos(v1)cos(v2)
    
    private static Fold3Solution givenFacetAngles(Angle v1, Angle v2, Angle v3) {
      Fold3Solution result = new Fold3Solution();
      result.v1 = v1;
      result.v2 = v2;
      result.v3 = v3;
      result.v12 = new TrinaryAngle(v1, v2, v3, valueGivenFacetAngles, descriptionGivenFacetAngles);
      result.v23 = new TrinaryAngle(v2, v3, v1, valueGivenFacetAngles, descriptionGivenFacetAngles);
      result.v31 = new TrinaryAngle(v1, v1, v2, valueGivenFacetAngles, descriptionGivenFacetAngles);
      return result;
    }
    private static Fold3Solution givenTwoFacetAnglesAndDihedralAngle(Angle facetAngle1, Angle facetAngle2, Angle dihedralAngle) {
      Fold3Solution result = new Fold3Solution();
      result.v1 = facetAngle1;
      result.v2 = facetAngle2;
      result.v12 = dihedralAngle;
      
      // Simplify if possible,
      // if  v12 = acos((cos(a3) - cos(a1)*cos(a2))/(sin(a1)*sin(a2)))
      // and a1 == v1 && a2 == v2 || a1 == v2 && a2 == v1
      // v3 = a3
      if (result.v12 instanceof TrinaryAngle) {
        TrinaryAngle v12 = (TrinaryAngle) result.v12;
        if (v12.v1.equals(result.v1) && v12.v2.equals(result.v2) || v12.v1.equals(result.v2) && v12.v2.equals(result.v2)) {
          result.v3 = v12.v3;
        }
      }
      if (result.v3 == null) {
        result.v3 = new TrinaryAngle(facetAngle1, facetAngle2, dihedralAngle, valueGivenTwoFacetAnglesAndDihedralAngle, descriptionGivenTwoFacetAnglesAndDihedralAngle);
      }
      result.v23 =  new TrinaryAngle(result.v2, result.v3, result.v1, valueGivenFacetAngles, descriptionGivenFacetAngles);
      result.v31 =  new TrinaryAngle(result.v3, result.v1, result.v2, valueGivenFacetAngles, descriptionGivenFacetAngles);
      return result;
    }
    
    @Override
    public String toString() {
      return "v1 = " + v1 +
           "\nv2 = " + v2 +
           "\nv3 = " + v3 +
           "\nv12 = " + v12 +
           "\nv23 = " + v23 +
           "\nv31 = " + v31;
    }
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
  private Stream<WayToConnect3> waysToConnect3(int n) {
    return solvedByDimension.get(n).stream().flatMap(
        f1 -> solvedByDimension.get(n).stream().flatMap(
            f2 -> solvedByDimension.get(n).stream().flatMap(
                f3 -> waysToConnect(f1.copy(), f2.copy(), f3.copy())
            )));
  }

  private Stream<WayToConnect3> waysToConnect(Polytope f1, Polytope f2, Polytope f3) {
    // For all ways to connect f1 to f2
    return f2.waysToConnect(f1).flatMap(
        equivalencesf2f1 -> {
        // Connect, find facet that was connected
        connectAndCopy(f2, f1, equivalencesf2f1);
        Polytope f1Copy = equivalencesf2f1.get(f1);
        Polytope f2Copy = equivalencesf2f1.get(f2);
        Polytope facetf1f2 = equivalencesf2f1.values().stream().filter(p -> p.n == f1.n - 1).findFirst().get();
        // For each ridge in facet, find neighbor facets in f1 and f2
        return facetf1f2.facets.stream().flatMap(
            ridge -> {
            Polytope neighborInF1 = f1Copy.getOtherFacet(facetf1f2, ridge);
            Polytope neighborInF2 = f2Copy.getOtherFacet(facetf1f2, ridge);
            // Connect both ridges to  f3
            //return Stream.of(new WayToConnect3());
            return f3.waysToConnectFacets(neighborInF1, neighborInF2)
                .map(
                equivalencesf3f1f2-> {
                Map<Polytope, Polytope> replacements = new HashMap<Polytope, Polytope>();
                Polytope f3Copy = f3.copy(replacements);
                Map<Polytope, Polytope> equivalencesCopy = new HashMap<Polytope, Polytope>();
                for (Polytope p: equivalencesf3f1f2.keySet()) {
                  equivalencesCopy.put(replacements.get(p), equivalencesf3f1f2.get(p));
                }
                f3Copy.equate(equivalencesCopy);
                return new WayToConnect3(f1Copy, f2Copy, f3Copy, ridge);
            }).filter(way -> way.f1.getAngle(way.ridge).getAngle() +
                             way.f2.getAngle(way.ridge).getAngle() +
                             way.f3.getAngle(way.ridge).getAngle() < 2*Math.PI);
        });
    });
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
  }

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
