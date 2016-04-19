package se.pp.forsberg.polytope.solver;

import static java.lang.Math.*;

// "Exact angles" using symbolic notation
public abstract class Angle {
  
  public abstract double getAngle();
  
  public static class RationalPi extends Angle {
    private final int nominator, denominator;
    public RationalPi(int nominator, int denominator) {
      if (denominator < 0) {
        denominator = -denominator;
        nominator = -nominator;
      }
      // normalize to -PI - PI
      while (nominator < -denominator) {
        nominator += 2*denominator;
      }
      while (nominator > denominator) {
        nominator -= 2*denominator;
      }
      int gcd = gcd(nominator, denominator);
      nominator /= gcd;
      denominator /= gcd;
      this.nominator = nominator;
      this.denominator = denominator;
    }
    private int gcd(int a, int b) {
      // \gcd(a,0) = a
      // \gcd(a,b) = \gcd(b, a \,\mathrm{mod}\, b),
      if (b == 0) {
        return a;
      }
      return gcd(b, a % b);
    }
    @Override
    public double getAngle() {
      return nominator * PI / denominator;
    }
    @Override
    public String toString() {
      if (nominator == 0) {
        return "0";
      }
      if (denominator == 1) {
        if (nominator == 1) {
          return "PI";
        }
        if (nominator == -1) {
          return "-PI";
        }
        return nominator + "PI";
      }
      if (nominator == 1) {
        return "PI/" + denominator;
      }
      return nominator + "PI/" + denominator;
    }
  }
  
  
  
  public static class TrinaryAngle extends Angle {
    public interface Value {
      double getValue(double v1, double v2, double v3);
    }
    private final Value value;
    private final String description;
    private final Angle v1, v2, v3;
    public TrinaryAngle(Angle v1, Angle v2, Angle v3, Value value, String description) {
      this.value = value;
      this.description = description;
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
    }
    @Override
    public double getAngle() {
      return value.getValue(v1.getAngle(), v2.getAngle(), v3.getAngle());
    }
    @Override
    public String toString() {
      return String.format(description, v1, v2, v3);
    }
  }
}
