package linking.disambiguation.scorers.embedhelp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.beust.jcommander.internal.Lists;

import linking.disambiguation.scorers.pagerank.PageRankLoader;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.datatypes.pr.PageRankScore;
import structure.interfaces.ContextBase;
import structure.utils.Loggable;

/**
 * Class handling the selection (=picking) of items (or entities) from within
 * defined clusters (=entities grouped by surface forms)
 * 
 * @author Kristian Noullet
 *
 */
public interface ClusterItemPicker extends ContextBase<Mention>, Loggable {
	public enum PICK_SELECTION {
		TOP_PAGERANK, RANDOM, OPTIMAL_CALC
	}

	public static final int DEFAULT_PR_TOP_K = 5_000_000;// 30;// 50;// 30;// 0;// 100;
	public static final double DEFAULT_PR_MIN_THRESHOLD = 0d;// 1d;// 0.16d;// 0.16d;// 1d;// 0.1d;
	public static final int DEFAULT_REPEAT = 50;// was 200 before, but due to long texts...
	public static final double DEFAULT_PRUNE_MIN_SCORE_RATIO = 0.40;
	public static final boolean allowSelfConnection = true;
	// Whether to remove assignments when there is only one possibility (due to high
	// likelihood of distortion)
	public static final boolean REMOVE_SINGLE_ASSIGNMENTS = false;
	// 0.16d due to MANY rarely-referenced 0.15d endpoints existing
	public static final int MIN_REPEAT = 500;

	public static final PICK_SELECTION DEFAULT_FIRST_CHOICE = PICK_SELECTION//
			// .TOP_PAGERANK
			.RANDOM//
	;
	public final static BiFunction<Double, Double, Double> DEFAULT_OPERATION = CombineOperation.OCCURRENCE.combineOperation;

	public List<String> combine();

	public double getPickerWeight();

	/**
	 * Prints the experiment setup
	 */
	public void printExperimentSetup();

	/**
	 * 
	 * @return which combination operation is used for this picker instance
	 */
	public BiFunction<Double, Double, Double> getCombinationOperation();

	default Map<String, Map<String, Number>> computeInitScoreMap(final Collection<Mention> context,
			final Number initValue) {
		final Map<String, Map<String, Number>> retMap = new HashMap<>();
		for (Mention mention : context) {
			if (mention == null || mention.getMention() == null) {
				continue;
			}

			final String surfaceForm = mention.getMention();
			Map<String, Number> sfMap = null;
			if ((sfMap = retMap.get(surfaceForm)) == null) {
				retMap.put(surfaceForm, sfMap);
				sfMap = new HashMap<>();
			}
			for (PossibleAssignment assignment : mention.getPossibleAssignments()) {
				sfMap.put(assignment.getAssignment(), initValue);
			}
		}
		return retMap;
	}

	/**
	 * Computes a map of clusters (key = surface form; value = list of entities for
	 * the surface form)
	 * 
	 * @param context
	 * @return
	 */
	default Map<String, List<String>> computeClusters(final Collection<Mention> context) {
		final Map<String, List<String>> clusterMap = new HashMap<>();

		List<String> putList = Lists.newArrayList();
		final Set<String> multipleOccurrences = new HashSet<>();
		int collisionCounter = 0;
		for (Mention m : context) {
			if (m == null || m.getMention() == null) {
				continue;
			}
			final List<String> absent = clusterMap.putIfAbsent(m.getMention(), putList);
			if (absent == null) {
				// Everything OK
				final List<String> cluster = clusterMap.get(m.getMention());
				for (PossibleAssignment ass : m.getPossibleAssignments()) {
					cluster.add(ass.getAssignment().toString());
				}
				// Prepare the putList for the next one
				putList = Lists.newArrayList();
			} else {
				multipleOccurrences.add(m.getMention());
				collisionCounter++;
				// getLogger().warn("Cluster already contained wanted mention (doubled word in
				// input?)");
			}
		}
		// getLogger().warn("Multiple SF occurrences - Collisions(" + collisionCounter +
		// ") for SF("+ multipleOccurrences.size() + "): " + multipleOccurrences);
		return clusterMap;
	}

	/**
	 * Limit the number of items within each cluster to a specified amount
	 * (PR_TOP_K) along with a hard minimum threshold<br>
	 * Note that both are hard limits for exclusion from cluster and do not show a
	 * preference in relation to each other
	 * 
	 * @param prLoader         PageRankLoader instance to load the pagerank values -
	 *                         if not yet loaded within the wrapper
	 * @param clusters         clusters as computed in
	 *                         {@link #computeClusters(Collection)}
	 * @param PR_TOP_K         how many items there are at most
	 * @param PR_MIN_THRESHOLD what the minimum PR value for something should be
	 * @return the filtered clusters based on the input parameters
	 */
	default Map<String, List<String>> computePRLimitedClusters(final PageRankLoader prLoader,
			final Map<String, List<String>> clusters, final int PR_TOP_K, final double PR_MIN_THRESHOLD) {
		final Map<String, List<String>> copyClusters = new HashMap<>();
		for (final String clusterName : clusters.keySet()) {
			final List<String> entities = clusters.get(clusterName);
			// log.info("SF[" + clusterName + "] - Entities[" + entities + "]");

			List<PageRankScore> rankedScores = prLoader.makeOrPopulateList(entities);
			if (PR_TOP_K > 0) {
				rankedScores = prLoader.getTopK(entities, PR_TOP_K);
			}

			if (PR_MIN_THRESHOLD > 0) {
				prLoader.cutOff(entities, PR_MIN_THRESHOLD);
			}

			// final List<AssignmentScore> rankedScores = prLoader.cutOff(entities,
			// PR_MIN_THRESHOLD);

			final List<String> limitedEntities = Lists.newArrayList();
			// Compute the list to disambiguate from
			// rankedScores.stream().forEach(item -> limitedEntities.add(item.assignment));
			for (PageRankScore item : rankedScores) {
				limitedEntities.add(item.assignment);
			}

			// Overwrite clusters, so disambiguation is only done on top PR scores
			copyClusters.put(clusterName, limitedEntities);
		}
		return copyClusters;
	}

	default Map<String, Map<String, Number>> computePRLimitedScoreClusters(final PageRankLoader prLoader,
			final Map<String, List<String>> clusters, final int PR_TOP_K, final double PR_MIN_THRESHOLD,
			final Number initVal) {
		final Map<String, Map<String, Number>> copyClusters = new HashMap<>();
		for (final String clusterName : clusters.keySet()) {
			final List<String> entities = clusters.get(clusterName);
			// log.info("SF[" + clusterName + "] - Entities[" + entities + "]");

			List<PageRankScore> rankedScores = prLoader.makeOrPopulateList(entities);
			if (PR_TOP_K > 0) {
				rankedScores = prLoader.getTopK(entities, PR_TOP_K);
			}

			if (PR_MIN_THRESHOLD > 0) {
				prLoader.cutOff(entities, PR_MIN_THRESHOLD);
			}

			// final List<AssignmentScore> rankedScores = prLoader.cutOff(entities,
			// PR_MIN_THRESHOLD);

			final Map<String, Number> limitedEntities = new HashMap<>();
			// Compute the list to disambiguate from
			// rankedScores.stream().forEach(item -> limitedEntities.add(item.assignment));
			for (PageRankScore item : rankedScores) {
				limitedEntities.put(item.assignment, initVal);
			}

			// Overwrite clusters, so disambiguation is only done on top PR scores
			copyClusters.put(clusterName, limitedEntities);
		}
		return copyClusters;
	}

	/**
	 * An occurrence-based combination operation incrementing the value (by 1)
	 * 
	 * @param previousValue     old value
	 * @param pairSimilaritySum how similar the considered entities are
	 * @return new value
	 */
	public static Double occurrenceOperation(Double previousValue, Double pairSimilaritySum) {
		return previousValue + 1;
	}

	/**
	 * A similarity-based combination operation increasing the value by the
	 * similarity
	 * 
	 * @param previousValue     old value
	 * @param pairSimilaritySum how similar the considered entities are
	 * @return new value
	 */
	public static Double similarityOperation(Double previousValue, Double pairSimilaritySum) {
		return previousValue + pairSimilaritySum;
	}

	/**
	 * A similarity-based combination operation increasing the value by the
	 * <b>squared</b> similarity
	 * 
	 * @param previousValue     old value
	 * @param pairSimilaritySum how similar the considered entities are
	 * @return new value
	 */
	public static Double similaritySquaredOperation(Double previousValue, Double pairSimilaritySum) {
		return previousValue + Math.pow(pairSimilaritySum, 2f);
	}

	/**
	 * A maximum similarity-based combination operation simply taking the highest of
	 * the two values (meaning the highest similarity value should win out)
	 * 
	 * @param previousValue     old value
	 * @param pairSimilaritySum how similar the considered entities are
	 * @return new value
	 */
	public static Double maxedOperation(Double previousValue, Double pairSimilaritySum) {
		return Math.max(previousValue, pairSimilaritySum);
	}

}
