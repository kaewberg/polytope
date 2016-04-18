package se.pp.forsberg.polytope.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import se.pp.forsberg.polytope.Edge;
import se.pp.forsberg.polytope.Point;

/**
 * Swing component to display a polytope
 * @author k287750
 *
 */
public class JPolytope extends JComponent {
  private static final long serialVersionUID = 1L;
  private PolytopeModel model;

  public JPolytope(final PolytopeModel model) {
    this.model = model;
    model.addPolytopeListener(new PolytopeListener() {
      @Override
      public void polytopeChanged(PolytopeChangedEvent e) {
        repaint();
      }
    });
    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        Insets i = getInsets();
        int w = getWidth() - i.left - i.right;
        int h = getHeight() - i.top - i.bottom;
        int sz = w;
        if (h < sz) {
          sz = h;
        }
        Rectangle2D r = model.getBoundingBox();
        if (r == null) {
          return;
        }
        int pref = model.getBoundingBox().getBounds().width;
        double[] scales = new double[model.getPolytope().getDimensions()];
        for (int j = 0; j < scales.length; j++) {
          scales[j] = ((double)sz)/pref;
        }
          model.scale(scales);
      }});
  }
  
  @Override
  public Dimension getPreferredSize() {
//    Rectangle2D r = model.getBoundingBox();
//    if (r == null) {
//      return new Dimension(200,  200);
//    }
//    Rectangle box = r.getBounds();
    Rectangle box = new Rectangle(0, 0, 200, 200);
    Insets i = getInsets();
    return new Dimension((int) (box.getWidth() + i.left + i.right), (int) (box.getHeight() + i.top + i.bottom));
  }

  boolean fnuk;
  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = null;
    if (g instanceof Graphics2D) {
      g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
    Insets insets = getInsets();
    int w = getWidth() - insets.left - insets.right;
    int h = getHeight() - insets.top - insets.bottom;
    double size = w < h? w : h;
    g.translate(insets.left, insets.top);
    g.setClip(0, 0, w, h);
    //if (isOpaque()) {
      g.setColor(getBackground());
      g.fillRect(0, 0, w, h);
    //}
    g.setColor(getForeground());
    g.translate(w/2, h/2);
    if (model.getPolytope() == null || model.getPolytope().isEmpty()) {
      return;
    }
    Point mid = model.midpoint();
    Rectangle2D r = model.getBoundingBox();
    g.translate((int) -mid.getCoordinate(0), (int) -mid.getCoordinate(1));
    double sizeP = r.getWidth() < r.getHeight()? r.getWidth() : r.getHeight();
    double scale = 0.8 * size / sizeP;
    for (Edge edge: model.getPolytope().getEdges()) {
      Point p1 = edge.getVertex1().getCoordinates();
      Point p2 = edge.getVertex2().getCoordinates();
      double x1 = p1.get(0) * scale;
      double y1 = p1.get(1) * scale;
      double x2 = p2.get(0) * scale;
      double y2 = p2.get(1) * scale;
      if (g2 == null) {
        g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
      } else {
        g2.draw(new Line2D.Double(x1, y1, x2, y2));
      }
    }
//    if (fnuk) {
//      g.drawLine(-10, 0, 10, 0);
//      g.drawLine(0, -10, 0, 10);
//    }
//    fnuk = !fnuk;
  }
}
