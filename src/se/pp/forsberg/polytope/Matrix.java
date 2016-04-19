package se.pp.forsberg.polytope;

/**
 * Inifinitely large square matrix for use by generalized affine transform
 * @author k287750
 *
 */
public class Matrix {
  private int nominalSize = -1;
  private double[][] m = new double[0][];
  private double EPSILON = 0.0000000001;
  
//  public Matrix(double[][] dat) {
//    this.data = dat;
//    this.nrows = dat.length;
//    this.ncols = dat[0].length; 
//  }
  
  public Matrix() {
  }
  public Matrix(int nominalSize) {
    this.nominalSize = nominalSize;
  }
  private int size() {
    return m.length;
  }
  int nominalSize() {
    return (nominalSize < 0)? m.length: nominalSize;
  }
  /**
   * Get value at (y,x). Values outside data matrix will be defaulted to identity
   * @param y
   * @param x
   * @return
   */
  public double get(int y, int x) {
    if (y < m.length && m[y] != null && x < m[y].length) {
      return m[y][x];
    }
    return getDefault(y, x);
  }
  private double getDefault(int y, int x) {
    return (x == y)? 1 : 0;
  }
  /**
   * Set value at (y,x). Data matrix will automatically be grown if needed.
   * @param y
   * @param x
   * @param d
   */
  public void set(int y, int x, double d) {
    if (Math.abs(get(y, x) - d) < EPSILON) {
      return;
    }
    if (y >= m.length) {
      double[][] m2 = new double[y+1][];
      System.arraycopy(m, 0, m2, 0, m.length);
      m = m2;
    }
    if (m[y] == null || x >= m[y].length ) {
      double r[] = new double[x+1];
      if (y < r.length) {
        r[y] = 1;
      }
      if (m[y] != null) {
        System.arraycopy(m[y], 0, r, 0, m[y].length);
      }
      m[y] = r;
    }
    m[y][x] = d;
  }
  private void normalize() {
    int rows = 0;
    for (int y = 0; y < m.length; y++) {
      int columns = 0;
      if (m[y] != null) {
        for (int x = 0; x < m[y].length; x++) {
          if (Math.abs(m[y][x] - getDefault(y, x)) > EPSILON) {
            columns = x+1;
            rows = y+1;
          }
        }
      }
      if (columns == 0) {
        m[y] = null;
      } else if (columns < m[y].length) {
        double r[] = new double[columns];
        System.arraycopy(m[y], 0, r, 0, r.length);
        m[y] = r;
      }
    }
    if (rows < m.length) {
      double[][] m2 = new double[rows][];
      System.arraycopy(m, 0, m2, 0, m2.length);
    }
  }
//  public double[][] getData() {
//    return data;
//  }
  
//  public Matrix transposition() {
//    Matrix result = realClone();
//    result.transpose();
//    return result;
//  }
//  public void transpose() {
//    for (int y = size()-1; y >= 0; y--) {
//      for (int x = size()-1; x >= 0; x--) {
//        if (x != y) {
//          double d = get(y, x);
//          set(y, x, get(x, y));
//          set(x, y, d);
//        }
//      }
//    }
//  }

//  public double determinant() {
//    if (nominalSize() == 2) {
//      return (get(0, 0) * get(1, 1)) - (get(0, 1) * get(1, 0));
//    }
//    double sum = 0.0;
//    for (int i = 0; i < nominalSize(); i++) {
//      sum += changeSign(i) * get(0, i) * createSubMatrix(0, i).determinant();
//    }
//    return sum;
//  }
//
//
//  public Matrix createSubMatrix(int excludingRow, int excludingColumn) {
//    Matrix result = new Matrix(nominalSize() - 1);
//    for (int y = nominalSize() - 1; y > excludingRow; y--) {
//      for (int x = nominalSize() - 1; x > excludingColumn; x--) {
//        result.set(y-1, x-1, get(y, x));
//      }
//      for (int x = excludingColumn - 1; x >= 0; x--) {
//        result.set(y-1, x, get(y, x));
//      }
//    }
//    for (int y = excludingRow - 1; y >= 0; y--) {
//      for (int x = nominalSize() - 1; x > excludingColumn; x--) {
//        result.set(y, x-1, get(y, x));
//      }
//      for (int x = excludingColumn - 1; x >= 0; x--) {
//        result.set(y, x, get(y, x));
//      }
//    }
//    return result;
//  }
//
//  public Matrix cofactor() {
//    Matrix result = new Matrix(nominalSize());
//    for (int y = nominalSize() - 1; y >= 0; y--) {
//      for (int x = nominalSize() - 1; x >= 0; x--) {
//        result.set(y, x, changeSign(x+y) * createSubMatrix(y, x).determinant());
//      }
//    }
//    return result;
//  }
//  
//  private static int changeSign(int i) {
//    return ((i & 1) == 0)? -1 : 1;
//  }

  public void invert() {
    int n = nominalSize();
    double x[][] = new double[n][n];
    double b[][] = new double[n][n];
    int index[] = new int[n];

    for (int i=0; i<n; ++i) {
      b[i][i] = 1;
    }
    // Transform the matrix into an upper triangle
    gaussian(index);
    // Update the matrix b[i][j] with the ratios stored
    for (int i=0; i<n-1; ++i)
      for (int j=i+1; j<n; ++j)
        for (int k=0; k<n; ++k)
          b[index[j]][k] -= get(index[j], i) * b[index[i]][k];
    // Perform backward substitutions
    for (int i=0; i<n; ++i) 
    {
      x[n-1][i] = b[index[n-1]][i]/get(index[n-1], n-1);
      for (int j=n-2; j>=0; --j) 
      {
        x[j][i] = b[index[j]][i];
        for (int k=j+1; k<n; ++k) 
        {
          x[j][i] -= get(index[j], k) * x[k][i];
        }
        x[j][i] /= get(index[j], j);
      }
    }
    m = x;
    normalize();
  }
  // Method to carry out the partial-pivoting Gaussian
  // elimination.  Here index[] stores pivoting order.
  public void gaussian(int index[]) {
    int n = index.length;
    double c[] = new double[n];
    // Initialize the index
    for (int i = 0; i < n; ++i)
      index[i] = i;
    // Find the rescaling factors, one from each row
    for (int i = 0; i < n; ++i) {
      double c1 = 0;
      for (int j = 0; j < n; ++j) {
        double c0 = Math.abs(get(i, j));
        if (c0 > c1)
          c1 = c0;
      }
      c[i] = c1;
    }
    // Search the pivoting element from each column
    int k = 0;
    for (int j = 0; j < n - 1; ++j) {
      double pi1 = 0;
      for (int i = j; i < n; ++i) {
        double pi0 = Math.abs(get(index[i], j));
        pi0 /= c[index[i]];
        if (pi0 > pi1) {
          pi1 = pi0;
          k = i;
        }
      }
      // Interchange rows according to the pivoting order
      int itmp = index[j];
      index[j] = index[k];
      index[k] = itmp;
      for (int i = j + 1; i < n; ++i)

      {

        double pj = get(index[i], j) / get(index[j], j);

        // Record pivoting ratios below the diagonal

        set(index[i], j, pj);

        // Modify other elements accordingly
        for (int l = j + 1; l < n; ++l)
          set(index[i], l, get(index[i], l) - pj * get(index[j], l));
      }
    }
  }

  
  
  public Matrix inverse() {
//    return cofactor().transposition().multiple(1.0 / determinant());
    Matrix result = realClone();
    result.invert();
    return result;
  }
//  public void invert() {
//    Matrix result = inverse();
//    m = result.m;
//  }

//  private Matrix multiple(double d) {
//    Matrix result = realClone();
//    result.multiply(d);
//    return result;
//  }
//  private void multiply(double d) {
//    for (int y = 0; y < m.length; y++) {
//      if (m[y] != null) {
//        for (int x = 0; x < m[y].length; x++) {
//          set(y, x, get(y, x) * d);
//        }
//      }
//    }
//  }
  public void concatenate(Matrix m) {
    int s = (int) Math.max(size(), m.size());
    Matrix me = realClone();
    // [this] = [this] x [t]
    for (int y = s-1; y >= 0; y--) {
      for (int x = s-1; x >= 0; x--) {
        double d = 0;
        for (int i = 0; i < s; i++) {
          d += me.get(y, i) * m.get(i, x);
        }
        set(y, x, d);
      }
    }
    normalize();
  }
  
  @Override
  protected Object clone() throws CloneNotSupportedException {
    return realClone();
  }
  Matrix realClone() {
    Matrix result = new Matrix();
    result.m = new double[m.length][];
    for (int i = 0; i < m.length; i++) {
      if (m[i] != null) {
        result.m[i] = new double[m[i].length]; 
        System.arraycopy(m[i], 0, result.m[i], 0, m[i].length);
      }
    }
    return result;
  }
  
  @Override
  public String toString() {
    int rows = 2, columns = 2;
    if (m.length > rows) {
      rows = m.length;
    }
    for (int y = 0; y < rows && y < m.length; y++) {
      if (m[y] != null && m[y].length > columns) {
        columns = m[y].length;
      }
    }
    int size = (int) Math.max(rows, columns);
    int widths[] = new int[size];
    String[][] data = new String[size][];
    for (int y = 0; y < size; y++) {
      data[y] = new String[size];
      for (int x = 0; x < size; x++) {
        data[y][x] = String.format("%.3f", get(y, x));
        if (data[y][x].length() > widths[x]) {
          widths[x] = data[y][x].length();
        }
      }
    }
    StringBuilder stringBuilder = new StringBuilder();
    for (int y = 0; y < size; y++) {
      stringBuilder.append('[');
      for (int x = 0; x < size; x++) {
        if (x > 0) {
          stringBuilder.append(' ');
        }
        stringBuilder.append(String.format("%" + widths[x] + "s", data[y][x]));
      }
      stringBuilder.append("]\n");
    }
    
    return stringBuilder.toString();
  }
}


