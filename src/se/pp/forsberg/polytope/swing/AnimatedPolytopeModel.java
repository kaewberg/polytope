package se.pp.forsberg.polytope.swing;

import se.pp.forsberg.polytope.AffineTransform;
import se.pp.forsberg.polytope.Polytope;

public class AnimatedPolytopeModel extends BasicPolytopeModel {

  private long INTERVAL = 2000;
  private int MAX_FRAMERATE = 100;
  
  public interface Transformer<ReturnType> {
    public void transform(Polytope polytope, double t);
    public ReturnType result(Polytope polytope);
  }
  public abstract class VoidTransformer implements Transformer<Object> {
    public Object result(Polytope polytope) {
      finalTransform(polytope);
      return null;
    }
    abstract void finalTransform(Polytope polytope);
  }
  public abstract class AffineTransformer extends VoidTransformer {
    public abstract AffineTransform transform(double t);
    public void transform(Polytope polytope, double t) {
      polytope.transform(transform(t));
    }
    public void finalTransform(Polytope polytope) {
      polytope.transform(transform(1.0));
    }
  }
  
  private class TemporalInterpolator<ReturnType> {
    private long time0 = System.currentTimeMillis();
    Transformer<ReturnType> transformer;
    public TemporalInterpolator(Transformer<ReturnType> transformer) {
      this.transformer = transformer;
    }
    public ReturnType interpolate() {
      Polytope original = getPolytope();
      Polytope copy = original.realClone();
      AnimatedPolytopeModel.this.polytoppe = copy;
      double t = 0;
      long time = System.currentTimeMillis();
      while (time - time0 < INTERVAL) {
        int dt = (int) (time- time0);
        if (dt < 1000 / MAX_FRAMERATE) {
          try {
            Thread.sleep(1000 / MAX_FRAMERATE - dt);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          dt = 1000 / MAX_FRAMERATE;
        }
        t = ((double) dt) / INTERVAL;
        transformer.transform(copy, t);
        firePolytopeChanged();
        time = System.currentTimeMillis();
      }
      AnimatedPolytopeModel.this.polytoppe = original;
      ReturnType result = transformer.result(original);
      firePolytopeChanged();
      return result;
    }
  }
  private class VoidTemporalInterpolator extends TemporalInterpolator<Object> {
    public VoidTemporalInterpolator(VoidTransformer transformer) {
      super(transformer);
    }
  }
  @Override
  public void translate(final double... distances) {
    new VoidTemporalInterpolator(new AffineTransformer(){
      @Override public AffineTransform transform(double t) {
        double[] partialDistances = new double[distances.length];
        for (int i = 0; i < distances.length; i++) {
          partialDistances[i] = distances[i] * t;
        }
        return AffineTransform.getTranslateInstance(partialDistances);
      }}).interpolate();
  }
  @Override
  public void scale(final double... scales) {
    new VoidTemporalInterpolator(new AffineTransformer(){
      @Override public AffineTransform transform(double t) {
        double[] partialScales = new double[scales.length];
        for (int i = 0; i < scales.length; i++) {
          partialScales[i] = scales[i] * t;
        }
        return AffineTransform.getScaleInstance(partialScales);
      }}).interpolate();
  }
  @Override
  public void rotate(final int dimension1, final int dimension2, final double v) {
    new VoidTemporalInterpolator(new AffineTransformer(){
      @Override public AffineTransform transform(double t) {
        return AffineTransform.getRotateInstance(dimension1, dimension2, v*t);
      }}).interpolate();
  }
  @Override
  public Polytope copyAndRotate(final Polytope facetToCopy, final Polytope ridgeToRotateAbout, final double angle) {
    final Polytope facet = facetToCopy.cloneExcept(ridgeToRotateAbout);
    
    return new TemporalInterpolator<Polytope>(new Transformer<Polytope>() {
      boolean first = true;
      @Override public void transform(Polytope polytope, double t) {
        if (first) {
          polytope.add(facet);
        } else {
          first = false;
        }
        facet.rotate(ridgeToRotateAbout, angle*t);
        //System.out.println("result " + t + "\n" + polytope + "\n" + facet);
      }
      @Override
      public Polytope result(Polytope polytope) {
        return polytope.copyAndRotate(facetToCopy, ridgeToRotateAbout, angle);
      }
    }).interpolate();
  }
}
