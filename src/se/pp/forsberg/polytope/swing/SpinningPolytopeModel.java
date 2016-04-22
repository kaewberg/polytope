package se.pp.forsberg.polytope.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import se.pp.forsberg.polytope.AffineTransform;
import se.pp.forsberg.polytope.Point;
import se.pp.forsberg.polytope.Polytope;

public class SpinningPolytopeModel implements PolytopeModel {

  private PolytopeModel realModel;
  private List<PolytopeListener> listeners = new ArrayList<PolytopeListener>();
  private Polytope realPolytope, transformedPolytope;
  private AffineTransform transform, partialTransform;
  
  public SpinningPolytopeModel(PolytopeModel model) {
    this();
    setModel(model);
  }
  private void setModel(final PolytopeModel model) {
    this.realModel = model;
    setupTransform();
    model.addPolytopeListener(new PolytopeListener() {
      @Override public void polytopeChanged(PolytopeChangedEvent e) {
        setupTransform();
      }
    });
  }
  private void setupTransform() {
    if (realModel == null) {
      realPolytope = null;
      return;
    }
    Polytope polytope = realModel.getPolytope();
    if (polytope == null) {
      realPolytope = null;
      return;
    }
    if (polytope == realPolytope) {
      return;
    }
    realPolytope = polytope;
    final int dimensions = polytope.getDimensions();
    transform = AffineTransform.getIdentityTransform();
    partialTransform = AffineTransform.getIdentityTransform();
    double v = 0.0013;
    for (int d1 = 0; d1 < dimensions-1; d1++) {
      for (int d2 = d1+1; d2 < dimensions; d2++) {
        for (int i = 0; i < dimensions-1; i++) {
          partialTransform.rotate(d1, d2, v);
          v += 0.0003;
        }
      }
    }
  }
  public SpinningPolytopeModel() {
    new Timer(20, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if (realPolytope == null) {
          return;
        }
        transform.concatenate(partialTransform);
        transformedPolytope = realPolytope.realClone();
        transformedPolytope.transform(transform);
        SpinningPolytopeModel.this.firePolytopeChanged();
      }
    }).start();
  }

  @Override public Polytope getPolytope() {
    return transformedPolytope;
  }

  @Override
  public void addPolytopeListener(PolytopeListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removePolytopeListener(PolytopeListener listener) {
    listeners.remove(listener);
  }
  protected void firePolytopeChanged() {
    final PolytopeChangedEvent e = new PolytopeChangedEvent(transformedPolytope);
    for (final PolytopeListener listener: listeners) {
      listener.polytopeChanged(e);
    }
  }

  @Override public void setPolytope(Polytope polytope) { realModel.setPolytope(polytope); }
  @Override public void scale(double... scales) { realModel.scale(scales); }
  @Override public void translate(double... distances) { realModel.translate(distances); }
  @Override public void rotate(int dimension1, int dimension2, double v) { realModel.rotate(dimension1, dimension2, v); }
  @Override public void transform(AffineTransform transform) {realModel.transform(transform); }
  @Override public Rectangle2D getBoundingBox() { return realModel.getBoundingBox(); }
  @Override public void add(Polytope facet) { realModel.add(facet); }
  @Override public Polytope copyAndRotate(Polytope facetToCopy, Polytope ridgeToRotateAbout, double angle) { return realModel.copyAndRotate(facetToCopy, ridgeToRotateAbout, angle); }
  @Override public Polytope close() { return realModel.close(); }
  @Override public Point midpoint() { return transformedPolytope == null? new Point() : transformedPolytope.midpoint(); }
}
