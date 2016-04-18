package se.pp.forsberg.polytope.swing;

import java.awt.geom.Rectangle2D;

import se.pp.forsberg.polytope.AffineTransform;
import se.pp.forsberg.polytope.Point;
import se.pp.forsberg.polytope.Polytope;

public interface PolytopeModel {
  public void setPolytope(Polytope polytope);

  void scale(double... scales);
  void translate(double... distances);
  void rotate(int dimension1, int dimension2, double v);
  void transform(AffineTransform transform);
  Point midpoint();

  Rectangle2D getBoundingBox();
  Polytope getPolytope();

  void addPolytopeListener(PolytopeListener listener);
  void removePolytopeListener(PolytopeListener listener);

  public void add(Polytope facet);
  public Polytope copyAndRotate(Polytope facetToCopy, Polytope ridgeToRotateAbour, double angle);
  public Polytope close();
}
