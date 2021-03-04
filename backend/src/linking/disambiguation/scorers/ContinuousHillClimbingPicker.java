package linking.disambiguation.scorers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import linking.disambiguation.scorers.pagerank.PageRankLoader;
import structure.config.constants.Comparators;
import structure.datatypes.Mention;
import structure.utils.MentionUtils;

/**
 * This cluster item picker serves to continuously call HillClimbingPicker,
 * while removing the weakest identified link in each iteration, until only 2
 * (including) remain
 * 
 * @author Kris
 *
 */
public class ContinuousHillClimbingPicker extends HillClimbingPicker {

	public ContinuousHillClimbingPicker(final BiFunction<Double, Double, Double> operation,
			final EntitySimilarityService similarityService, final PageRankLoader pagerankLoader) {
		super(operation, similarityService, pagerankLoader);
	}

	public ContinuousHillClimbingPicker(final EntitySimilarityService similarityService,
			final PageRankLoader pagerankLoader) {
		super(similarityService, pagerankLoader);
	}

	/**
	 * ContinuousChoices = overall choices <br>
	 * IterationChoices = choices for that specific iteration
	 * 
	 */
	@Override
	public List<String> combine() {
		super.prune = false;
		int iterationCounter = 0;
		final List<Mention> copyContext = Lists.newArrayList(this.context);
		System.out.println(getClass().getName()+" - Mentions["+copyContext+"]");
		// Sorts them for the sake of initialisation picking based on word order
		Collections.sort(copyContext, Comparators.mentionOffsetComparator);
		// Computing clusters outside, so we don't have to redo it every time
		final Map<String, List<String>> clusters = computeClusters(copyContext);
		// Remove entities that do not have an associated embedding
		// & cluster if they are left w/o entity as a result of it
		removeInvalidEmbeddings(clusters);

		final Map<String, List<MutablePair<String, Double>>> continuousChoices = new HashMap<>();
		while (copyContext.size() > 1 && clusters.size() > 1) {
			// Do the picking logic
			System.out.println("Displaying (valid) clusters: "+displayMap(clusters, 10));
			final Map<String, Pair<String, Double>> iterationChoices = super.pickItems(clusters);

			// If no item has been picked, there is no need to continue... -> jump out
			if (iterationChoices == null || iterationChoices.size() < 1) {
				break;
			}
			try {
				// Processes the choices and removes the worst 'cluster of candidates'
				processIterationResults(continuousChoices, iterationChoices, clusters, copyContext);
			} catch (IllegalArgumentException | NullPointerException exc) {
				System.err.println("###########################################");
				System.out.println("Clusters:" + displayMap(clusters));
				System.err.println("###########################################");
				System.out.println("Iteration Choices:" + displayMap(iterationChoices));
				System.err.println("###########################################");
				System.err.println("Copy context:" + copyContext);
				System.err.println("###########################################");
				System.err.println("Context: " + context);
				System.err.println("###########################################");
				throw exc;
			}

			System.out.println("Iteration(#" + iterationCounter++ + ") Choices:");
			System.out.println(displayMap(iterationChoices));
		}

		// Now just get the best one for each surface form
		final List<String> retList = Lists.newArrayList();
		for (Entry<String, List<MutablePair<String, Double>>> entrySurfaceForm : continuousChoices.entrySet()) {
			Double maxValue = Double.MIN_VALUE;
			Pair<String, Double> maxPair = null;
			String maxKey = null;

			for (MutablePair<String, Double> pair : entrySurfaceForm.getValue()) {
				if (pair.getValue() > maxValue) {
					maxPair = pair;
					maxValue = pair.getValue();
					maxKey = pair.getKey();
				}
			}
			if (maxKey != null) {
				retList.add(maxKey);
			}
		}

		getLogger().info("FINAL CHOICES[" + retList.size() + "]: " + retList);
		return retList;
	}

	private void processIterationResults(Map<String, List<MutablePair<String, Double>>> continuousChoices,
			Map<String, Pair<String, Double>> iterationChoices, Map<String, List<String>> clusters,
			List<Mention> copyContext) {
		// Go through our choices and see which ones to cut away for the next iteration
		for (Map.Entry<String, Pair<String, Double>> iterationChoice : iterationChoices.entrySet()) {
			final String key = iterationChoice.getKey();
			List<MutablePair<String, Double>> continuousPairs = continuousChoices.get(key);
			if (continuousPairs == null) {
				continuousPairs = Lists.newArrayList();
				continuousChoices.put(key, continuousPairs);
			}

			boolean found = false;
			final Pair<String, Double> iterationChoicePair = iterationChoice.getValue();
			for (MutablePair<String, Double> continuousPair : continuousPairs) {
				if (continuousPair.getKey().equals(iterationChoicePair.getKey())) {
					// Same entity = 'Collision' - so modify/update score accordingly
					found = true;
					// It's the same pair, so let's combine them!
					final Double currentValue = continuousPair.getValue();
					final Double newValue = computeNewValue(this.context.size() - clusters.size(), currentValue,
							iterationChoicePair.getValue());
					continuousPair.setValue(newValue);
				}
			}

			if (!found) {
				// TODO: Check if logic really holds as rn I'm not sure whether there really is
				// exactly one pair here if it doesn't exist yet
				//
				// Not a collision, so just add it
				continuousPairs.add(new MutablePair<String, Double>(iterationChoicePair.getLeft(),
						initVal(iterationChoicePair.getRight())));
			}
		}

		Double minValue = Double.MAX_VALUE;
		Pair<String, Double> minPair = null;
		String minKey = null;
		// Find the entity-score pair for the worst surface form
		for (Map.Entry<String, Pair<String, Double>> e : iterationChoices.entrySet()) {
			final Pair<String, Double> currentPair = e.getValue();
			final Double currentValue = currentPair.getRight();
			if (currentValue <= minValue) {
				minKey = e.getKey();
				minPair = currentPair;
				minValue = currentValue;
			}
		}

		// Remove surface form with worst result (as it likely is noise)
		clusters.remove(minKey);
		MentionUtils.removeStringMention(minKey, copyContext);

	}

	private <T> String displayMap(Map<String, T> map) {
		final int MAX_ITEMS = 10;
		return displayMap(map, MAX_ITEMS);
	}

	private <T> String displayMap(Map<String, T> map, final int MAX_ITEMS) {
		final StringBuilder retSB = new StringBuilder();
		final StringBuilder sbSub = new StringBuilder();
		final String NEWLINE = System.getProperty("line.separator");
		for (Map.Entry<String, T> e : map.entrySet()) {
			final T val = e.getValue();
			// Reset the SB for the value item(s)
			sbSub.setLength(0);
			if (val instanceof Iterable) {
				final StringBuilder sbSubSub = new StringBuilder();
				final Iterator valIt = ((Iterable) val).iterator();
				int iterCounter = 0;
				if (!valIt.hasNext()) {
					// Do nothing if there's nothing following...
				} else {
					Object o = valIt.next();
					sbSubSub.append(o.toString());
					iterCounter++;

					while (valIt.hasNext()) {
						o = valIt.next();
						iterCounter++;
						sbSubSub.append("\t;\t");
						sbSubSub.append(o.toString());
						if (iterCounter > MAX_ITEMS) {
							break;
						}
					}
					sbSub.append(sbSubSub.toString());
				}
				// val == null ? "<NULL>": val.subList(0, Math.min(val.size(), MAX_ITEMS + 1));
			} else {
				sbSub.append(val.toString());
			}
			retSB.append("Key[" + e.getKey() + "] " + sbSub.toString());
			retSB.append(NEWLINE);
		}
		return retSB.toString();
	}

	private Double initVal(Double right) {
		// return right;
		return 1D;
	}

	/**
	 * Computes the new value based on the iteration that we are part of, as well as
	 * the previously existing value and the new value
	 * 
	 * @param iterationNumber
	 * @param previousValue
	 * @param currentValue
	 * @return
	 */
	private Double computeNewValue(int iterationNumber, Double previousValue, Double currentValue) {
		return previousValue + iterationNumber * currentValue;
		// return previousValue + currentValue;
		// return previousValue + 1;
	}

}
