package se.pp.forsberg.polytope.solver;

public class Edge extends Polytope {
	private double length;
	
	public Edge(Vertex v1, Vertex v2) {
		this(v1, v2, 1);
	}
	public Edge(Vertex v1, Vertex v2, double length) {
		super(1);
		facets.add(v1);
		facets.add(v2);
		this.length = length;
	}
	
	public double getLength() {
		return length;
	}
}
