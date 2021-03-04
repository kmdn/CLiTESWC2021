package structure.interfaces;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

/**
 * Graph implementation through use of DirectedSparseGraph<V, E> and according
 * definition of HoppableGraph for generalization of other Graph instance
 * 
 * @author wf7467
 *
 * @param <V>
 * @param <E>
 */
public class DirectedHoppableSparseGraph<V, E> extends DirectedSparseGraph<V, E> implements HoppableGraph<V, E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2945374408827888627L;

}
