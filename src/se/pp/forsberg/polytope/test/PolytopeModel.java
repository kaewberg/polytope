package se.pp.forsberg.polytope.test;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import se.pp.forsberg.polytope.AffineTransform;
import se.pp.forsberg.polytope.Point;
import se.pp.forsberg.polytope.Polytope;
import se.pp.forsberg.polytope.Vertex;

public class PolytopeModel {
  private Polytope polytope;
  private List<PolytopeListener> listeners = new ArrayList<PolytopeListener>();
  
  public PolytopeModel(Polytope polytope) {
    this.polytope = polytope;
  }
  
  protected void firePolytopeChanged() {
    PolytopeChangedEvent e = new PolytopeChangedEvent(polytope);
    for (PolytopeListener listener: listeners) {
      listener.polytopeChanged(e);
    }
  }
  
  public void scale(double... scales) {
    polytope.scale(scales);
    firePolytopeChanged();
  }
  public void translate(double... distances) {
    polytope.translate(distances);
    firePolytopeChanged();
  }
  public void rotate(int dimension1, int dimension2, double v) {
    polytope.rotate(dimension1, dimension2, v);
    firePolytopeChanged();
  }

  public void transform(AffineTransform transform) {
    polytope.transform(transform);
    firePolytopeChanged();
  }
  
  // Polytope must be centered around origo
  public Rectangle2D getBoundingBox() {
    double diameter = 0;
    for (Vertex vertex1: polytope.getVertices()) {
      for (Vertex vertex2: polytope.getVertices()) {
         diameter = Math.max(diameter, vertex1.getCoordinates().distance(vertex2.getCoordinates()));
      }
    }
    return new Rectangle2D.Double(0, 0, diameter, diameter);
  }

  public Polytope getPolytope() {
    return polytope;
  }

  public void addPolytopeListener(PolytopeListener listener) {
    listeners.add(listener);
  }
  public void removePolytopeListener(PolytopeListener listener) {
    listeners.remove(listener);
  }
}
