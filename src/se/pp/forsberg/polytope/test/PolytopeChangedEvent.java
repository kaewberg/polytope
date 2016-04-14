package se.pp.forsberg.polytope.test;

import se.pp.forsberg.polytope.Polytope;

public class PolytopeChangedEvent {

  private Polytope polytope;
  public PolytopeChangedEvent(Polytope polytope) {
    this.polytope = polytope;
  }
  
  public Polytope getPolytope() {
    return polytope;
  }
}
