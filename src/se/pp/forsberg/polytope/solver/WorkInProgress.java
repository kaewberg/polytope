//package se.pp.forsberg.polytope.solver;
//
//import java.util.HashSet;
//import java.util.IdentityHashMap;
//import java.util.Map;
//import java.util.Set;
//
///**
// * An unsolved polytope being folded. Contains extra data aiding the solution
// * process, and important parts of the algorithm.
// * 
// * @author Johan
// */
//
//public class WorkInProgress extends Polytope {
//	boolean completed = false;
//
//	public WorkInProgress(int n) {
//		super(n);
//	}
//
//	public boolean isCompleted() {
//		return completed;
//	}
//
//	public boolean add(Polytope facet) {
//		if (facet.n != n - 1) {
//			throw new IllegalArgumentException("n-polytope must consist of n-1 polytopes");
//		}
//		facets.add(facet);
//		if (!solve()) {
//			facets.remove(facet);
//			return false;
//		}
//		return true;
//	}
//
//	/**
//	 * Attempt to solve for angles based on current situation. Sets the
//	 * "completed" property if the result is valid and closes the polytope.
//	 * 
//	 * @return false if current situtation is impossible
//	 */
//	public boolean solve() {
//		if (facets.isEmpty()) {
//			return true;
//		}
//		boolean solved = false;
//		for (Polytope ridge: getRidges()) {
//			// Already solved?
//			if (angles.containsKey(ridge)) {
//				continue;
//			}
//			if (!solve(ridge)) {
//				return false;
//			}
//		}
//		if (solved) {
//			if (!finalValidation()) {
//				return false;
//			}
//			completed = true;
//		}
//		return true;
//	}
//
//	/**
//	 * All angles have been solved, but if misused the result may be disconnected
//	 * 
//	 * @return Is this a closed, connected polytope?
//	 */
//	private boolean finalValidation() {
//		// if 2 facets it must have been folded flat
//		if (facets.size() < 3) {
//			return false;
//		}
//		// Each ridge must be connected to exactly two facets, and the angle between them solved
//		Map<Polytope, Set<Polytope>> ridgeToFacetMap = getRidgeToFacetMap();
//		for (Polytope ridge: ridgeToFacetMap.keySet()) {
//			if (ridgeToFacetMap.get(ridge).size() != 2 || !angles.containsKey(ridge)) {
//				return false;
//			}
//		}
//		// Pick a random facet and find all facets recursively reachable from that. The result must be all facets.
//		Set<Polytope> reachableFacets = new HashSet<Polytope>();
//		collectConnectedFacets(facets.iterator().next(), reachableFacets);
//		if (!reachableFacets.equals(facets)) {
//			return false;
//		}
//		return true;
//	}
//
//	/**
//	 * Attempt to find the angle of this facet
//	 * @param ridge
//	 * @return
//	 */
//	private boolean solve(Polytope ridge) {
//		Set<Polytope> facets = getFacets(p -> p.facets.contains(ridge));
//	}
//
//
//}
