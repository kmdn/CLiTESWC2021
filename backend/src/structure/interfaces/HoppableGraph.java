package structure.interfaces;

import java.util.Collection;

import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public interface HoppableGraph<V, E> {
	public boolean addEdge(E e1, Pair<? extends V> pair, EdgeType et);

	public E findEdge(V v1, V v2);

	public Collection<E> findEdgeSet(V v1, V v2);

	public Collection<E> getInEdges(V v);

	public Collection<E> getOutEdges(V v);

	public Collection<V> getPredecessors(V v);

	public Collection<V> getSuccessors(V v);

	public Pair<V> getEndpoints(E e);

	public V getSource(E e);

	public V getDest(E e);

	public boolean isSource(V v, E e);

	public boolean isDest(V v, E e);

	public Collection<E> getEdges();

	public Collection<V> getVertices();

	public boolean containsVertex(V v);

	public boolean containsEdge(E e);

	public int getEdgeCount();

	public int getVertexCount();

	public Collection<V> getNeighbors(V v);

	public Collection<E> getIncidentEdges(V v);

	public boolean addVertex(V v);

	public boolean removeVertex(V v);

	public boolean removeEdge(E e);
}
