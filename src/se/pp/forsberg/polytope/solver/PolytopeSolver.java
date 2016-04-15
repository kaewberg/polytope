package se.pp.forsberg.polytope.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.pp.forsberg.polytope.Polytope;

public class PolytopeSolver {
	
	private Map<Integer, List<Polytope>> solutions = new HashMap<Integer, List<Polytope>>();
	
	public static void main(String[] arguments) {
		new PolytopeSolver().solve();
	}

	private void solve() {
		if (!continuePrevious()) {
			add2D();
			//solve(3);
		}
	}

	private boolean continuePrevious() {
		return false;
	}

	private void add2D() {
		List<Polytope> flat = new ArrayList<Polytope>();
		Polytope p = getPolygon(3);
		p.setName("Triangle");
		p.setId(0);
		flat.add(p);
		p = getPolygon(4);
		p.setName("Square");
		p.setId(1);
		flat.add(p);
		p = getPolygon(5);
		p.setName("Pentagon");
		p.setId(2);
		flat.add(p);
		p = getPolygon(6);
		p.setName("Hexagon");
		p.setId(3);
		flat.add(p);
		p = getPolygon(8);
		p.setName("Octagon");
		p.setId(4);
		flat.add(p);
		p = getPolygon(10);
		p.setName("Dekagon");
		p.setId(5);
		flat.add(p);
		
		solutions.put(2, flat);
	}

	private Polytope getPolygon(int i) {
		// Isoceles triangle sides x base 1
		// top angle 2pi/i
		// Right triangle hypotenuse x base 0,5 
		// Angles pi/i pi/2 pi-pi/i-pi/2=pi/2-pi/i
		// cos(pi-pi/i)h = 0,5
		// 0,5/sin(pi/i) = x/sin(PI/2)
		// x = 0,5sin(pi/i)
		Polytope p = Polytope.getEmpty(2);
		double d = 0.5 * Math.sin(Math.PI/2);
		for (int n = 0; n < i-1; n++) {
			double v = n * 2*Math.PI/i;
			p.add(Polytope.get(Math.cos(v)*d, Math.sin(v)*d));
		}
		p.close();
		return p;
	}
}
