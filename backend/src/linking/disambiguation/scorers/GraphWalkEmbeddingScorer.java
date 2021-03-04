package linking.disambiguation.scorers;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import linking.disambiguation.scorers.embedhelp.ClusterItemPicker;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.PostScorer;
import structure.utils.Loggable;
import structure.utils.LoggerWrapper;

/**
 * A wrapper class for embeddings-based scorers by instantiating it with a
 * cluster item picker which does most of the heavy lifting
 * 
 * @author Kristian Noullet
 *
 */
public class GraphWalkEmbeddingScorer implements PostScorer<PossibleAssignment, Mention>, Loggable {
	private boolean hasChanged = true;
	private final Set<String> bestCombination = new HashSet<>();
	private final String changedLock = "hasChangedLock";
	private final ClusterItemPicker clusterHelper;

	public GraphWalkEmbeddingScorer(final ClusterItemPicker entityPicker) {
		this.clusterHelper = entityPicker;
		this.clusterHelper.printExperimentSetup();
	}

	private static LoggerWrapper log() {
		return Loggable.getLogger(GraphWalkEmbeddingScorer.class.getName());
	}

	/**
	 * Loads embeddings from a raw file
	 * 
	 * @param inPathRaw
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Map<String, List<Number>> rawload(final String inPathRaw)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		Map<String, List<Number>> helperMap = null;
		try {
			try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(inPathRaw)))) {
				helperMap = (Map<String, List<Number>>) ois.readObject();
			}
		} catch (EOFException eof) {
			getLogger().error("Exception!", eof);
		}
		return helperMap;
		// finally {
		// getLogger().info("HelperMap: " + helperMap);
		// this.entityEmbeddingsMap = helperMap;
		// }
	}

	@Override
	public Number computeScore(PossibleAssignment assignment) {
		synchronized (changedLock) {
			if (hasChanged) {
				recomputeOptimum();
			}
		}

		if (bestCombination.contains(assignment.toString())) {
			// This assignment is in the final wanted combination!
			return 1;
		} else {
			// This assignment is NOT in the final wanted combination.
			return 0;
		}
	}

	/**
	 * Recomputes the best combination of entities to minimize distance
	 */
	private synchronized void recomputeOptimum() {
		try {
			bestCombination.clear();
			bestCombination.addAll(clusterHelper.combine());
			// getLogger().info("Recomputeoptimum combo: ");
			// getLogger().info(bestCombination);
		} catch (Exception exc) {
//			getLogger().error(exc);
//			getLogger().error(exc.getMessage());
//			getLogger().error(exc.getCause());
//			getLogger().error(exc.getLocalizedMessage());
			getLogger().error(exc.getMessage(), exc);
		}
		hasChanged = false;
	}

	@Override
	public Number getWeight() {
		return this.clusterHelper.getPickerWeight();
		// return 20f;
	}

	@Override
	public void linkContext(Collection<Mention> context) {
		clusterHelper.linkContext(context);
	}

	@Override
	public void updateContext() {
		synchronized (changedLock) {
			hasChanged = true;
			clusterHelper.updateContext();
		}
	}

	@Override
	public BiFunction<Number, PossibleAssignment, Number> getScoreModulationFunction() {
		return null;
	}

}
