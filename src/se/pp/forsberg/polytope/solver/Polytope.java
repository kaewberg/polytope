package se.pp.forsberg.polytope.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A canonical polytope is disconnected from coordinates except for the edge
 * lengths and the bicameral angles.
 * 
 * @author Johan
 *
 */
public class Polytope {
	int n;
	Set<Polytope> facets = new HashSet<Polytope>();
	Map<Polytope, Double> angles = new HashMap<Polytope, Double>();

	public Polytope(int n) {
		this.n = n;
	}

	public Set<Polytope> getRidges() {
		return getFirstComponents(p -> p.n == n - 2);
	}

	protected Map<Polytope, Set<Polytope>> getRidgeToFacetMap() {
		Map<Polytope, Set<Polytope>> result = new IdentityHashMap<Polytope, Set<Polytope>>();
		for (Polytope facet: facets) {
			for (Polytope ridge: facet.facets) {
				Set<Polytope> facetsByRidge = result.get(ridge);
				if (facetsByRidge == null) {
					facetsByRidge = new HashSet<Polytope>();
					result.put(ridge, facetsByRidge);
				}
				facetsByRidge.add(facet);
			}
		}
		return result;
	}

	/**
	 * Create a set of all facets reachable from the specified facet by moving to
	 * neighbors along the ridges.
	 * 
	 * @param facet
	 *          Starting face
	 * @param ridgeToFacetMap
	 *          Map of ridge to neighboring facets
	 * @param reachableFacets
	 *          Return value, all facets (recursively) reachable from facet.
	 */
	protected void collectConnectedFacets(Polytope facet, Set<Polytope> reachableFacets) {
		if (reachableFacets.contains(facet)) {
			return;
		}
		reachableFacets.add(facet);
		for (Polytope ridge: facet.facets) {
			for (Polytope neighboringFacet: getRidgeToFacetMap().get(ridge)) {
				collectConnectedFacets(neighboringFacet, reachableFacets);
			}
		}
	}

	private Set<Polytope> getFirstComponents(Predicate<Polytope> predicate) {
		Set<Polytope> result = new HashSet<Polytope>();
		collectFirstComponents(predicate, result);
		return result;
	}

	private void collectFirstComponents(Predicate<Polytope> predicate, Set<Polytope> result) {
		if (predicate.test(this)) {
			result.add(this);
		} else
			for (Polytope facet: facets) {
				facet.collectFirstComponents(predicate, result);
			}
	}
	
	protected Set<Polytope> getFacets(Predicate<Polytope> predicate) {
		Set<Polytope> result = new HashSet<Polytope>();
		for (Polytope facet: facets) {
			if (predicate.test(facet)) {
				result.add(facet);
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof Polytope)) {
			return false;
		}
		Polytope other = (Polytope) object;
		return n == other.n && facets.equals(other.facets);
	}

	@Override
	public int hashCode() {
		return n ^ facets.hashCode();
	}
}
