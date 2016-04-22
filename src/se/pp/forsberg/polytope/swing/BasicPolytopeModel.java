package se.pp.forsberg.polytope.swing;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import se.pp.forsberg.polytope.AffineTransform;
import se.pp.forsberg.polytope.Point;
import se.pp.forsberg.polytope.Polytope;
import se.pp.forsberg.polytope.Vertex;

public class BasicPolytopeModel implements PolytopeModel {
  protected Polytope polytoppe;
  private List<PolytopeListener> listeners = new ArrayList<PolytopeListener>();
  
  public BasicPolytopeModel() {
  }
  
  public BasicPolytopeModel(Polytope polytope) {
    setPolytope(polytope);
  }
  
  @Override
  public void setPolytope(Polytope polytope) {
    this.polytoppe = polytope;
    firePolytopeChanged();
  }
  
  protected void firePolytopeChanged() {
    final PolytopeChangedEvent e = new PolytopeChangedEvent(polytoppe);
    for (final PolytopeListener listener: listeners) {
      if (SwingUtilities.isEventDispatchThread()) {
        listener.polytopeChanged(e);
      } else {
        try {
          SwingUtilities.invokeAndWait(new Runnable() { @Override
            public void run() {
              listener.polytopeChanged(e);
            }});
        } catch (InvocationTargetException e1) {
          e1.printStackTrace();
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }
    }
  }
  
  // Polytope should be centered around origo
  @Override
  public Rectangle2D getBoundingBox() {
    double diameter = 0;
    Polytope polytope = getPolytope();
    if (polytope == null || polytope.isEmpty()) {
      return null;
    }
    for (Vertex vertex1: polytope.getVertices()) {
      for (Vertex vertex2: polytope.getVertices()) {
         diameter = Math.max(diameter, vertex1.getCoordinates().distance(vertex2.getCoordinates()));
      }
    }
    return new Rectangle2D.Double(0, 0, diameter, diameter);
  }
  protected void runOnEventDispatchThread(Runnable runnable) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      try {
        SwingUtilities.invokeAndWait(runnable);
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public Polytope getPolytope() {
    if (!SwingUtilities.isEventDispatchThread()) {
      System.out.println("damn");
    }
    return polytoppe;
  }
  @Override
  public void addPolytopeListener(PolytopeListener listener) {
    listeners.add(listener);
  }
  @Override
  public void removePolytopeListener(PolytopeListener listener) {
    listeners.remove(listener);
  }

  @Override public void translate(double... distances) { getPolytope().translate(distances); firePolytopeChanged(); }
  @Override public void scale(double... scales) { getPolytope().scale(scales); firePolytopeChanged(); }
  @Override public void rotate(int dimension1, int dimension2, double v) { getPolytope().rotate(dimension1, dimension2, v); firePolytopeChanged(); }
  @Override public void transform(AffineTransform transform) { getPolytope().transform(transform); firePolytopeChanged(); }
  @Override public void add(final Polytope facet) { getPolytope().add(facet); firePolytopeChanged(); }
  @Override public Polytope copyAndRotate(Polytope facetToCopy, Polytope ridgeToRotateAbout, double angle) { Polytope result = getPolytope().copyAndRotate(facetToCopy, ridgeToRotateAbout, angle); firePolytopeChanged(); return result; }
  @Override public Polytope close() { Polytope result = getPolytope().close(); firePolytopeChanged(); return result; }
  @Override public Point midpoint() { return getPolytope().midpoint(); }
}
