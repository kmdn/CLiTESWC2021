package linking.disambiguation.scorers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.Triple;
import org.aksw.agdistis.util.TripleIndex;
import org.aksw.agdistis.util.TripleIndexContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;

public class MAG {

}

class BreadthFirstSearch {
	private final TripleIndex index;
	private final String algo;

	public BreadthFirstSearch(final TripleIndex index, final String algo) {
		this.index = index;
		this.algo = algo;
	}


	public void run(final int maxDepth, final DirectedSparseGraph<Node, String> graph, final String edgeType, final String nodeType)
			throws UnsupportedEncodingException, IOException {
		final Queue<Node> q = new LinkedList<Node>();
		for (final Node node : graph.getVertices()) {
			q.add(node);
		}
		while (!q.isEmpty()) {
			final Node currentNode = q.poll();
			final int level = currentNode.getLevel();
			if (level < maxDepth) {
				List<Triple> outgoingNodes = null;
				outgoingNodes = index.search(currentNode.getCandidateURI(), null, null);
				if (outgoingNodes == null) {
					continue;
				}
				for (final Triple targetNode : outgoingNodes) {
					if (targetNode.getPredicate() == null && targetNode.getObject() == null) {
						continue;
					}
					if (targetNode.getPredicate().startsWith(edgeType) && targetNode.getObject().startsWith(nodeType)) {
						final int levelNow = level + 1;
						final Node Node = new Node(targetNode.getObject(), 0, levelNow, algo);
						q.add(Node);
						graph.addEdge(graph.getEdgeCount() + ";" + targetNode.getPredicate(), currentNode, Node);
					}
				}
			}
		}
	}
}



class HITS {
	private HashSet<String> restrictedEdges;

	/**
	 * 
	 * this methods runs hits on a graph and returns the most authoritative
	 * sources, due to HITS is very simple, it will be implemented by iterating
	 * two formulas can be written as matrix multiplication but this would be
	 * quite ineffective
	 * 
	 * @param k
	 * 
	 * @param Graph
	 * @return
	 * @throws InterruptedException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void runHits(final Graph g, final int k) throws InterruptedException {
		// restrict Graph
		if (restrictedEdges != null) {
			final HashSet<Object> toBeRemoved = new HashSet<Object>();
			for (final Object edge : g.getEdges()) {
				final String edgeString = (String) edge;
				for (final String restrict : restrictedEdges) {
					if (edgeString.contains(restrict)) {
						toBeRemoved.add(edge);
					}
				}
			}
			// because of concurrent modification exception
			for (final Object edge : toBeRemoved) {
				g.removeEdge(edge);
			}
		}
		// x - authority weight
		// y - hub weight
		Node n;
		for (int iter = 0; iter < k; iter++) {
			for (final Object o : g.getVertices()) {
				n = (Node) o;
				double x = 0;
				for (final Object inc : g.getPredecessors(n)) {
					x += ((Node) inc).getHubWeight();
				}
				double y = 0;
				for (final Object inc : g.getSuccessors(n)) {
					y += ((Node) inc).getAuthorityWeight();
				}
				n.setUnnormalizedAuthorityWeight(x * n.getAuthorityWeightForCalculation());
				n.setUnnormalizedHubWeight(y * n.getHubWeightForCalculation());
			}
			// build normalization
			double sumX = 0;
			double sumY = 0;
			for (final Object o : g.getVertices()) {
				n = (Node) o;
				sumX += n.getUnnormalizedAuthorityWeight();
				sumY += n.getUnnormalizedHubWeight();
			}
			for (final Object o : g.getVertices()) {
				n = (Node) o;
				n.setAuthorityWeight(n.getUnnormalizedAuthorityWeight() / sumX);
				n.setHubWeight(n.getUnnormalizedHubWeight() / sumY);
			}
		}
	}

	public void restrictEdges(final HashSet<String> restrictedEdges) {
		this.restrictedEdges = restrictedEdges;

	}
}



class PageRank {
	/**
	 * @param args
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void runPr(final Graph g, final int maxIteration, final double threshold) {

		final int numNodes = g.getVertices().size();
		int numIteration = 0;
		Node n;
		Node m;
		Object[] successors;
		Collection succ;

		final double w = 0.85; // standard weight
		double pr = (double) 1 / numNodes; // temporary storage for pagerank
		double randomWalker;
		double distance;
		double sumPr;

		// initialize PR set every value to 1/numNodes
		for (final Object o : g.getVertices()) {
			n = (Node) o;
			n.setPageRank(pr);
			n.setPageRankNew(0);
		}

		// Update Pagerank while distance between graphs is higher than
		// threshold
		// and numIteration < maxIteration
		// but at least once
		do {
			randomWalker = 0;
			// iterate over every node
			for (final Object o : g.getVertices()) {
				n = (Node) o;

				// if n has outgoing edges, spread the weight
				succ = g.getSuccessors(n);
				if (!succ.isEmpty()) {

					// weight to be spread is current weight divided by outgoing
					// edges
					pr = n.getPageRank() / succ.size();

					successors = succ.toArray();
					for (int i = 0; i < succ.size(); i++) {
						m = (Node) successors[i];
						// add to pageRank new!
						m.setPageRankNew(m.getPageRankNew() + pr);
					}
				} else {
					randomWalker += n.getPageRank() / numNodes;
				}
			}
			// distribute randomWalker
			for (final Object o : g.getVertices()) {
				n = (Node) o;
				n.setPageRankNew((w * (n.getPageRankNew() + randomWalker)) + ((1 - w) / numNodes));
			}
			distance = computeDistance(g);

			// update Graph and get sum of Values
			sumPr = 0;
			for (final Object o : g.getVertices()) {
				n = (Node) o;
				n.setPageRank(n.getPageRankNew());
				n.setPageRankNew(0);
				sumPr += n.getPageRank();
			}

			for (final Object o : g.getVertices()) {
				n = (Node) o;
				n.setPageRank(n.getPageRank() / sumPr);

			}
			// inkrement iteration
			numIteration += 1;

		} while ((distance > threshold) && numIteration < maxIteration);

	}

	@SuppressWarnings("rawtypes")
	private double computeDistance(final Graph g) {
		Node n;
		double distance = 0;
		for (final Object o : g.getVertices()) {
			n = (Node) o;
			distance += Math.abs(n.getPageRank() - n.getPageRankNew());
		}
		return distance;
	}

}



class DomainWhiteLister {
	private final TripleIndex index;
	HashSet<String> whiteList = new HashSet<String>();

	public DomainWhiteLister(final TripleIndex index) throws IOException {
		final Properties prop = new Properties();
		final InputStream input = DomainWhiteLister.class.getResourceAsStream("/config/agdistis.properties");
		prop.load(input);
		final String envWhiteList = System.getenv("AGDISTIS_WHITELIST");
		final String file = envWhiteList != null ? envWhiteList : prop.getProperty("whiteList");

		loadWhiteDomains(file);

		this.index = index;
	}

	private void loadWhiteDomains(final String file) throws IOException {
		final BufferedReader br = new BufferedReader(
				new InputStreamReader(DomainWhiteLister.class.getResourceAsStream(file)));
		while (br.ready()) {
			final String line = br.readLine();
			whiteList.add(line);
		}
		br.close();
	}

	public boolean fitsIntoDomain(final String candidateURL) {
		final List<Triple> tmp = index.search(candidateURL, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null);
		if (tmp.isEmpty())
			return true;
		for (final Triple triple : tmp) {
			if (!triple.getObject().contains("wordnet") && !triple.getObject().contains("wikicategory"))
				if (whiteList.contains(triple.getObject())) {
					return true;
				}
		}
		return false;
	}
}



class CandidateUtil {
    private TripleIndex index;
    private TripleIndexContext indexByContext;
    private String nodeType;
    private DomainWhiteLister domainWhiteLister;

    private static Logger log = LoggerFactory.getLogger(CandidateUtil.class);
    
	public TripleIndex getIndex() {
		return index;
	}
	public TripleIndexContext getIndexContext() {
		return indexByContext;
    }
    public void setNodeType(final String nodeType) {
		this.nodeType = nodeType;
    }
    public void setIndex(final TripleIndex index) {
		try {
			this.index = index;
			this.domainWhiteLister = new DomainWhiteLister(index);
		} catch (final IOException e) {
			log.error("Could not set new index in Candidate Util due to DomainWhiteLister");
			e.printStackTrace();
		}

	}
}



class CandidatesScore implements Comparable<CandidatesScore> {

	private int startPos;
	private String uri;
	private double score; // Don't use double type for financial information.

	public int getStart() {
		return startPos;
	}

	public void setStart(final int start) {
		this.startPos = start;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(final String uri) {
		this.uri = uri;
	}

	public double getScore() {
		return score;
	}

	public void setScore(final double score) {
		this.score = score;
	}

	@Override
	public int compareTo(final CandidatesScore o) {
		return new Double(o.getScore()).compareTo(score);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Candidates [StartPosition=");
		builder.append(startPos);
		builder.append(", Uri=");
		builder.append(uri);
		builder.append(", Score=");
		builder.append(score);
		builder.append("]");
		return builder.toString();
	}
	
}



class NEDAlgo_HITS {
	private final Logger log = LoggerFactory.getLogger(NEDAlgo_HITS.class);
	private String edgeType;
	private String nodeType;
	private final CandidateUtil cu;
	private TripleIndex index;
	private TripleIndexContext indexByContext;
	// needed for the experiment about which properties increase accuracy
	private double threshholdTrigram;
	private int maxDepth;
	private Boolean heuristicExpansionOn;
	private final String algorithm;
	private final boolean context;

	public NEDAlgo_HITS() throws IOException {
		final Properties prop = new Properties();
		final InputStream input = NEDAlgo_HITS.class.getResourceAsStream("/config/agdistis.properties");
		prop.load(input);

		final String envNodeType = System.getenv("AGDISTIS_NODE_TYPE");
		final String nodeType = envNodeType != null ? envNodeType : prop.getProperty("nodeType");
		final String envEdgeType = System.getenv("AGDISTIS_EDGE_TYPE");
		final String edgeType = envEdgeType != null ? envEdgeType : prop.getProperty("edgeType");
		final String envThresholdTrigram = System.getenv("AGDISTIS_THRESHHOLD_TRIGRAM");
		final double threshholdTrigram = Double.valueOf(envThresholdTrigram != null ? envThresholdTrigram : prop.getProperty("threshholdTrigram"));
		final String envMaxDepth = System.getenv("AGDISTIS_MAX_DEPTH");
		final int maxDepth = Integer.valueOf(envMaxDepth != null ? envMaxDepth : prop.getProperty("maxDepth"));
		final String envHeuristicExpansion = System.getenv("AGDISTIS_HEURISTIC_EXPANSION_ON");
		this.heuristicExpansionOn = Boolean.valueOf(envHeuristicExpansion != null ? envHeuristicExpansion : prop.getProperty("heuristicExpansionOn"));
		final String envAlgorithm = System.getenv("AGDISTIS_ALGORITHM");
		this.algorithm = envAlgorithm != null ? envAlgorithm : prop.getProperty("algorithm");
		this.nodeType = nodeType;
		this.edgeType = edgeType;
		this.threshholdTrigram = threshholdTrigram;
		this.maxDepth = maxDepth;
		this.cu = new CandidateUtil();
		this.index = cu.getIndex();
		final String envContext = System.getenv("AGDISTIS_CONTEXT");
		this.context = Boolean.valueOf(envContext != null ? envContext : prop.getProperty("context"));
		if (context == true) { // in case the index by context exist
			this.indexByContext = cu.getIndexContext();
		}
	}

	public void run(final Document document, final Map<NamedEntityInText, List<CandidatesScore>> candidatesPerNE, final DirectedSparseGraph<Node, String> graph) {
		try {
			final NamedEntitiesInText namedEntities = document.getNamedEntitiesInText();
			//DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

			// 0) insert candidates into Text
			//log.debug("\tinsert candidates");
			//cu.insertCandidatesIntoText(graph, document, threshholdTrigram, heuristicExpansionOn);

			// 1) let spread activation/ breadth first search run
			log.info("\tGraph size before BFS: " + graph.getVertexCount());
			final BreadthFirstSearch bfs = new BreadthFirstSearch(index, algorithm);
			bfs.run(maxDepth, graph, edgeType, nodeType);
			log.info("\tGraph size after BFS: " + graph.getVertexCount());

			if (algorithm.equals("hits")) {
				// 2.1) let HITS run
				log.info("\trun HITS");
				final HITS h = new HITS();
				h.runHits(graph, 20);
			} else if (algorithm.equals("pagerank")) {
				// 2.2) let Pagerank run
				log.info("\trun PageRank");
				final PageRank pr = new PageRank();
				pr.runPr(graph, 50, 0.1);
			}

			// 3) store the candidate with the highest hub, highest authority
			// ratio
			// manipulate which value to use directly in node.compareTo
			log.debug("\torder results");
			final ArrayList<Node> orderedList = new ArrayList<Node>();
			orderedList.addAll(graph.getVertices());
			Collections.sort(orderedList);
			for (final NamedEntityInText entity : namedEntities) {
				for (int i = 0; i < orderedList.size(); i++) {
					final Node m = orderedList.get(i);
					// there can be one node (candidate) for two labels
					if (m.containsId(entity.getStartPos())) {
						entity.setNamedEntity(m.getCandidateURI());
						break;
					}

				}
			}
			// To get all candidates along with their scores
			if (candidatesPerNE != null) {
				for (final NamedEntityInText entity : namedEntities) {
					final List<CandidatesScore> listCandidates = new ArrayList<>();
					for (int i = 0; i < orderedList.size(); i++) {
						final Node m = orderedList.get(i);

						// there can be one node (candidate) for two labels
						if (m.containsId(entity.getStartPos())) {

							final CandidatesScore candidates = new CandidatesScore();
							candidates.setStart(entity.getStartPos());
							candidates.setUri(m.getCandidateURI());
							candidates.setScore(m.getAuthorityWeight());
							listCandidates.add(candidates);
						}

					}
					candidatesPerNE.put(entity, listCandidates);
				}
			}

		} catch (final Exception e) {
			log.error("AGDISTIS cannot be run on this document.", e);
		}
	}

	public void close() throws IOException {
		index.close();
		if (context == true) {
		indexByContext.close();
		}
	}

	public void setThreshholdTrigram(final double threshholdTrigram) {
		this.threshholdTrigram = threshholdTrigram;
	}

	public void setMaxDepth(final int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public void setHeuristicExpansionOn(final Boolean value) {
		this.heuristicExpansionOn = value;
	}
	public String getEdgeType() {
		return edgeType;
	}

	public void setEdgeType(final String edgeType) {
		this.edgeType = edgeType;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(final String nodeType) {
		this.nodeType = nodeType;
		this.cu.setNodeType(nodeType);
	}

	public void setIndex(final TripleIndex index) {
		this.index = index;
		this.cu.setIndex(index);
	}

}