package se.pp.forsberg.polytope.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;

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
  private double originalSize;

  public JPolytope(final PolytopeModel model) {
    this.model = model;
    model.addPolytopeListener(new PolytopeListener() {
      @Override
      public void polytopeChanged(PolytopeChangedEvent e) {
        repaint();
      }
    });
    originalSize = model.getBoundingBox().getWidth();
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
    Rectangle box = model.getBoundingBox().getBounds();
    Insets i = getInsets();
    return new Dimension((int) (box.getWidth() + i.left + i.right), (int) (box.getHeight() + i.top + i.bottom));
  }
  
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
    g.translate(insets.left, insets.top);
    g.setClip(0, 0, w, h);
    if (isOpaque()) {
      g.setColor(getBackground());
      g.fillRect(0, 0, w, h);
    }
    g.setColor(getForeground());
    g.translate(w/2, h/2);
    for (Edge edge: model.getPolytope().getEdges()) {
      Point p1 = edge.getVertex1().getCoordinates();
      Point p2 = edge.getVertex2().getCoordinates();
      if (g2 == null) {
        g.drawLine((int)p1.getCoordinate(0), (int)p1.getCoordinate(1), (int)p2.getCoordinate(0), (int)p2.getCoordinate(1));
      } else {
        g2.draw(new Line2D.Double(p1.getCoordinate(0), p1.getCoordinate(1), p2.getCoordinate(0), p2.getCoordinate(1)));
      }
    }
  }
}
