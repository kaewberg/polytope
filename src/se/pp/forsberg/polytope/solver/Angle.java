package se.pp.forsberg.polytope.solver;

import static java.lang.Math.*;

import java.util.Map;

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
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof RationalPi)) {
        return false;
      }
      RationalPi other = (RationalPi) obj;
      return nominator == other.nominator && denominator == other.denominator;
    }
  }
  
  public static class RationalAcos extends Angle {
    private final int multiplier, nominator, denominator;
    public RationalAcos(int multiplier, int nominator, int denominator) {
      this.multiplier = multiplier; 
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
      return multiplier*acos(nominator / denominator);
    }
    @Override
    public String toString() {
      return ((multiplier == 1)? "" : (multiplier + "*")) + "acos(" + nominator + "/" + denominator + ")";
    }
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof RationalAcos)) {
        return false;
      }
      RationalAcos other = (RationalAcos) obj;
      return multiplier == other.multiplier && nominator == other.nominator && denominator == other.denominator;
    }
  }
  
  public static class TrinaryAngle extends Angle {
    public interface Value {
      double getValue(double v1, double v2, double v3);
    }
    final Value value;
    final String description;
    final Angle v1, v2, v3;
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
    @Override
    public String toString(Map<Angle, String> angleNames) {
      String s1 = angleNames.containsKey(v1)? angleNames.get(v1) : v1.toString(angleNames);
      String s2 = angleNames.containsKey(v2)? angleNames.get(v2) : v2.toString(angleNames);
      String s3 = angleNames.containsKey(v3)? angleNames.get(v3) : v3.toString(angleNames);

      return String.format(description, s1, s2, s3);
    }
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof TrinaryAngle)) {
        return false;
      }
      TrinaryAngle other = (TrinaryAngle) obj;
      return v1.equals(other.v1) && v2.equals(other.v2) && v3.equals(other.v3) && value.equals(other.value);
    }
    @Override
    public int hashCode() {
      return v1.hashCode() ^ v2.hashCode() ^ v3.hashCode() ^ value.hashCode();
    }
  }

  public static class QuintaryAngle extends Angle {
    public interface Value {
      double getValue(double v1, double v2, double v3, double v4, double v5);
    }
    final Value value;
    final String description;
    final Angle v1, v2, v3, v4, v5;
    public QuintaryAngle(Angle v1, Angle v2, Angle v3, Angle v4, Angle v5, Value value, String description) {
      this.value = value;
      this.description = description;
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;;
      this.v4 = v4;;
      this.v5 = v5;
    }
    @Override
    public double getAngle() {
      return value.getValue(v1.getAngle(), v2.getAngle(), v3.getAngle(), v4.getAngle(), v5.getAngle());
    }
    @Override
    public String toString() {
      return String.format(description, v1, v2, v3, v4, v5);
    }
    @Override
    public String toString(Map<Angle, String> angleNames) {
      String s1 = angleNames.containsKey(v1)? angleNames.get(v1) : v1.toString(angleNames);
      String s2 = angleNames.containsKey(v2)? angleNames.get(v2) : v2.toString(angleNames);
      String s3 = angleNames.containsKey(v3)? angleNames.get(v3) : v3.toString(angleNames);
      String s4 = angleNames.containsKey(v4)? angleNames.get(v4) : v4.toString(angleNames);
      String s5 = angleNames.containsKey(v5)? angleNames.get(v5) : v5.toString(angleNames);

      return String.format(description, s1, s2, s3, s4, s5);
    }
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof QuintaryAngle)) {
        return false;
      }
      QuintaryAngle other = (QuintaryAngle) obj;
      return v1.equals(other.v1) && v2.equals(other.v2) && v3.equals(other.v3) && v4.equals(other.v4) && v5.equals(other.v5) && value.equals(other.value);
    }
    @Override
    public int hashCode() {
      return v1.hashCode() ^ v2.hashCode() ^ v3.hashCode() ^ v4.hashCode() ^ v5.hashCode() ^ value.hashCode();
    }
  }
  public static class Unknown extends Angle {
    @Override
    public double getAngle() {
      throw new IllegalArgumentException("Attempt to get value of unknowm angle");
    }
    @Override
    public String toString() {
      return "<Unknown>";
    }
  }

  
  /**
   * To string using symbolic names of already defined angles
   * @param angleNames
   * @return
   */
  public String toString(Map<Angle, String> angleNames) {
    return toString();
  }
}
