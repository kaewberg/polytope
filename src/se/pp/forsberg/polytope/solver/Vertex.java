package se.pp.forsberg.polytope.solver;

public class Vertex extends Polytope {
	public Vertex() {
		super(0);
	}
	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}
