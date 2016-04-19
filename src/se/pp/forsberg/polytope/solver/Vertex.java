package se.pp.forsberg.polytope.solver;

public class Vertex extends Polytope {

  public Vertex() {
    super(0);
    setName("Vertex");
  }
  
  @Override
  public Polytope copy() {
    return copyVertex();
  }

  public Vertex copyVertex() {
    Vertex v = new Vertex();
    copyCommon(v);
    return v;
  }
  
}
