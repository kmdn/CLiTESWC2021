package linking.disambiguation.scorers;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import linking.disambiguation.scorers.embedhelp.AbstractClusterItemPicker;
import linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import linking.disambiguation.scorers.pagerank.PageRankLoader;
import structure.config.constants.Comparators;
import structure.datatypes.Mention;
import structure.utils.Stopwatch;

/**
 * A cluster item picker instance, picking items based on a hill-climbing scheme
 * 
 * @author Kristian Noullet
 *
 */
public class HillClimbingPicker extends AbstractClusterItemPicker {

	private final Random r = new Random(System.currentTimeMillis());
	private final boolean CREATE_TABLE = false;
	private final boolean ENTITY_TABLE = true;

	protected boolean prune = false;

	// Instance variables
	public final int REPEAT;
	protected Collection<Mention> context;
	protected final EntitySimilarityService similarityService;
	protected final PageRankLoader pagerankLoader;
	public final int pagerankTopK;
	public final double pagerankMinThreshold;
	public final PICK_SELECTION initStrategy;
	public final double pruneThreshold;

	public HillClimbingPicker(final Map<String, List<Number>> entityEmbeddingsMap,
			final PageRankLoader pagerankLoader) {
		this(new EntitySimilarityService(entityEmbeddingsMap), pagerankLoader);
	}

	public HillClimbingPicker(final EntitySimilarityService similarityService, final PageRankLoader pagerankLoader) {
		this(DEFAULT_OPERATION, similarityService, pagerankLoader);
	}

	public HillClimbingPicker(final BiFunction<Double, Double, Double> combineOperation,
			final EntitySimilarityService similarityService, final PageRankLoader pagerankLoader) {
		this(combineOperation, similarityService, DEFAULT_REPEAT, pagerankLoader, DEFAULT_PR_TOP_K,
				DEFAULT_PR_MIN_THRESHOLD, DEFAULT_FIRST_CHOICE, DEFAULT_PRUNE_MIN_SCORE_RATIO);
	}

	public HillClimbingPicker(final BiFunction<Double, Double, Double> combineOperation,
			final EntitySimilarityService similarityService, final int repeat, final PageRankLoader pagerankLoader,
			final int pagerankTopK, final double pagerankMinThreshold, final PICK_SELECTION initStrategy,
			final double pruneThreshold) {
		super(combineOperation);
		this.similarityService = similarityService;
		// How many times to repeat hillclimbing
		this.REPEAT = Math.max(MIN_REPEAT, repeat);
		this.pagerankTopK = pagerankTopK;
		this.pagerankMinThreshold = pagerankMinThreshold;
		this.initStrategy = initStrategy;
		this.pagerankLoader = pagerankLoader;
		this.pruneThreshold = pruneThreshold;
	}

	@Override
	public void linkContext(Collection<Mention> context) {
		this.context = context;
	}

	@Override
	public void updateContext() {
		// None?
	}

	@Override
	public List<String> combine() {

		// --------------------------------------------------
		// Order list by natural occurring order of words
		// (enforces intuition of words close to each other being about similar topics)
		// --------------------------------------------------
		final List<Mention> contextList = Lists.newArrayList(context);
		// Sorts them for the sake of initialisation picking based on word order
		Collections.sort(contextList, Comparators.mentionOffsetComparator);
		// Compute clusters with the strings for simplicity of calls
		final Map<String, List<String>> clusters = computeClusters(contextList);

		removeInvalidEmbeddings(clusters);
		final Map<String, Pair<String, Double>> finalChoiceMap = pickItems(clusters);

		final List<String> retList = Lists.newArrayList();
		for (Pair<String, Double> pair : finalChoiceMap.values()) {
			retList.add(pair.getKey());
		}

		getLogger().info("FINAL CHOICES[" + retList.size() + "]: " + retList);
		return retList;
	}

	/**
	 * Removes invalid (non-existing) embeddings from clusters and removes the
	 * clusters if they are left without entity
	 * 
	 * @param clusters
	 */
	protected void removeInvalidEmbeddings(Map<String, List<String>> clusters) {
//		if (true) {
//			return;
//		}
		// ------------------------------------------------
		// Remove non-existing embedding entities - Start
		final List<Map.Entry<String, List<String>>> toRemove = Lists.newArrayList();
		for (Map.Entry<String, List<String>> cluster : clusters.entrySet()) {
			// Removes non-existing values
			this.similarityService.ascertainSimilarityExistence(cluster.getValue());
			if (cluster.getValue().size() < 1) {
				toRemove.add(cluster);
			}
		}
		// If there are no entities left, remove the entire cluster
		for (Map.Entry<String, List<String>> cluster : toRemove) {
			clusters.remove(cluster.getKey());
		}
		// Remove non-existing embedding entities - End
		// ------------------------------------------------
	}

	/**
	 * Does all the computations and calls to get
	 * 
	 * @return
	 */
	protected Map<String, Pair<String, Double>> pickItems(final Map<String, List<String>> clusters) {
		// Choose an initial combination and improve on it at every step until it can no
		// longer be improved
		// Try making it with a chain-like minimal distance logic
		// "Get smallest for one of current chains"

		// Map to keep the final choices in our eye
		// Map<SurfaceForm, List<Pair<Entity, SimilaritySum>>>
		final Map<String, List<Pair<String, Double>>> disambiguationResultsMap = new HashMap<>();

		// Use clusterNames as the shuffling mechanism
		final List<String> clusterNames = Lists.newArrayList(clusters.keySet());
		// Collections.shuffle(clusterNames);

		// Pagerank stuff - limits the items to the top PR_TOP_K and PR_MIN_THRESHOLD
		final Map<String, String> topPRChoices = new HashMap<>();
		// Computes PageRank scores and filters out the ones that are too low either in
		// rank or score (based on parameters passed)
		final Map<String, List<String>> limitedClusters = computePRLimitedClusters(this.pagerankLoader, clusters,
				this.pagerankTopK, this.pagerankMinThreshold);
		final Iterator<String> itClusterNames = clusterNames.iterator();
		// Remove clusters that should not be disambiguated on
		while (itClusterNames.hasNext()) {
			final String clusterName = itClusterNames.next();
			final List<String> rankedScores = limitedClusters.get(clusterName);
			if (rankedScores == null || rankedScores.size() == 0
					|| (REMOVE_SINGLE_ASSIGNMENTS && rankedScores.size() == 1)) {
				// ALSO: Remove it from HillClimbing consideration when there's only one...
				// If the PR score is too low, make sure no more disambiguation is done on it
				// getLogger().info("Removing CLUSTER[" + clusterName + "]");
				// Removes in case it exists there... which it shouldn't
				topPRChoices.remove(clusterName);
				// Remove the cluster altogether
				itClusterNames.remove();
				clusters.remove(clusterName);
				continue;
			}
			// Overwrite clusters, so disambiguation is only done on top PR scores
			// rather than on all candidates
			clusters.put(clusterName, rankedScores);
			topPRChoices.put(clusterName, rankedScores.get(0));
		}

		println("Initialised choices - Start");
		displayClusterChoices(topPRChoices, "picker init");
		println("Initialised choices - End");

		// Execute hillclimbing multiple times
		for (int hillClimbExec = 0; hillClimbExec < REPEAT; ++hillClimbExec) {
			hillClimb(disambiguationResultsMap, clusters, Lists.newArrayList(clusterNames), topPRChoices);
		}

		// -------------------------------------------
		// HillClimbing Disambigation - END
		// -------------------------------------------

		// -------------------------
		// Group results
		// -------------------------
		final Map<String, Pair<String, Double>> finalChoiceMap = group(disambiguationResultsMap);

		// ---------------
		// Pruning
		// ---------------
		if (prune) {
			prune(finalChoiceMap);
		}

		return finalChoiceMap;
	}

	/**
	 * Displays the clusters and clusters they belong to
	 * 
	 * @param clusterChoices
	 */
	private void displayClusterChoices(Map<? extends Object, ? extends Object> clusterChoices, final String prefix) {
		for (Entry<? extends Object, ? extends Object> e : clusterChoices.entrySet()) {
			println("[" + prefix + "] - Cluster[" + e.getKey() + "]: Choice[" + e.getValue() + "]");
		}
	}

	public final Map<String, Pair<String, Double>> group(
			Map<String, List<Pair<String, Double>>> disambiguationResultsMap) {
		// Now that it has been executed as many times as we want it to, we should group
		// the similar entities and possibly do something with the similaritySum values
		// (e.g. disambiguate over them?), OTHERWISE: just take the number of times they
		// each appeared and take the most-appearing one

		// <Key,Value>: <SurfaceForm, ChosenEntity>
		final Map<String, Pair<String, Double>> finalChoiceMap = new HashMap<>();
		// <Key,Value>: <EntityIRI, CosineSimSum>
		final Map<String, Double> groupMap = new HashMap<>();
		for (Map.Entry<String, List<Pair<String, Double>>> e : disambiguationResultsMap.entrySet()) {
			groupMap.clear();
			final String surfaceForm = e.getKey();
			// Group all pairs for this specific surface form
			combineForSF(groupMap, e.getValue());

			// displayAllResultsMap(allResultsMap);
			// displayScoredChoices(surfaceForm, groupMap);

			// Pairs have been summed up together, so let's rank them
			Double prevSim = Double.MIN_VALUE;
			for (Entry<String, Double> pairEntry : groupMap.entrySet()) {
				// For this surface form, choose the best candidate
				if (pairEntry.getValue() > prevSim) {
					finalChoiceMap.put(surfaceForm, new ImmutablePair<>(pairEntry.getKey(), pairEntry.getValue()));
					prevSim = pairEntry.getValue();
				}
			}
		}
		return finalChoiceMap;
	}

	/**
	 * Combines pairs from toCombinePairs as defined in the operation through
	 * {@link #applyOperation(Double, Double)}, storing the result in groupMap
	 * 
	 * @param groupMap       map storing the results
	 * @param toCombinePairs pairs which are to be combined if they have a surface
	 *                       form in common
	 */
	protected void combineForSF(final Map<String, Double> groupMap, List<Pair<String, Double>> toCombinePairs) {
		for (Pair<String, Double> pair : toCombinePairs) {
			Double existingPairValue = groupMap.get(pair.getLeft());
			if (existingPairValue == null) {
				existingPairValue = 0D;
			}
			// key = entity; value = combined score
			groupMap.put(pair.getLeft(), applyOperation(existingPairValue, pair.getRight()));
		}
	}

	/**
	 * Prunes entries from given map for which the Pair's Double (right) element is
	 * less than the minimum score threshold
	 * 
	 * @param choiceMap
	 */
	private void prune(final Map<String, Pair<String, Double>> choiceMap) {
		final Iterator<Entry<String, Pair<String, Double>>> finalChoicesIterator = choiceMap.entrySet().iterator();
		final Double MIN_SCORE = computeMinScore();
		getLogger().info("Min score THRESHOLD:" + MIN_SCORE);
		getLogger().info("PRUNING/ABSTAINING PROCEDURE - START");
		while (finalChoicesIterator.hasNext()) {
			final Entry<String, Pair<String, Double>> entry = finalChoicesIterator.next();
			// getLogger().info("Entry:" + entry.getKey() + " - " + entry.getValue());
			getLogger().info("PRUNING: Min_SCORE(" + MIN_SCORE + "): KEY-SF:" + entry.getKey() + ", Entity/Score:"
					+ entry.getValue());
			if (entry.getValue().getRight() < MIN_SCORE) {
				// getLogger().info("[" + MIN_SCORE + "] ->Pruning:" + entry.getKey() + " - " +
				// entry.getValue());
				finalChoicesIterator.remove();
				getLogger().info("-> PRUNING: REMOVED");
			} else {
				getLogger().info("-> PRUNING: KEEPING");
			}
		}
		// getLogger().info("PRUNING/ABSTAINING PROCEDURE - END");

	}

	private Map<String, List<Pair<String, Double>>> hillClimb(
			final Map<String, List<Pair<String, Double>>> disambiguationResultsMap,
			final Map<String, List<String>> clusters, final List<String> clusterNames,
			final Map<String, String> topPRClusterMap) {
		// Adds to clusterNames with the initial order
		// contextList.stream().forEach(mention ->
		// clusterNames.add(mention.getMention()));

		// Start with the written ordering (for initial bias), then shuffle

		// ##############################
		// Initial path choice - START
		// ##############################
		Stopwatch.start(getClass().getName());
		// First initialise by getting the optimal similarity
		final Map<String, Pair<String, Double>> chosenClusterEntityMap = new HashMap<>(clusters.size());
		Double similaritySum = 0D;
		if (clusterNames.size() > 1) {
			final String finalTarget;

			switch (this.initStrategy) {
			case RANDOM:
				if (true) {
					// IF clause for IDE scope pickup
					Collections.shuffle(clusterNames);
					final String fromClusterName = clusterNames.get(0);
					final String toClusterName = clusterNames.get(1);
					final List<String> fromEntities = clusters.get(fromClusterName);
					final List<String> toEntities = clusters.get(toClusterName);
					final int firstItemIndex = r.nextInt(fromEntities.size());
					final int secondItemIndex = r.nextInt(toEntities.size());
					final String sourceEntity = fromEntities.get(firstItemIndex);
					final String targetEntity = toEntities.get(secondItemIndex);
					chosenClusterEntityMap.put(fromClusterName,
							new ImmutablePair<String, Double>(sourceEntity, Double.MIN_VALUE));
					chosenClusterEntityMap.put(toClusterName, new ImmutablePair<String, Double>(targetEntity,
							this.similarityService.similarity(sourceEntity, targetEntity).doubleValue()));
					similaritySum += chooseClosestToFirst(clusters, clusterNames, chosenClusterEntityMap);
				}
				break;
			case OPTIMAL_CALC:
				if (true) {
					// IF clause for IDE scope pickup
					// First CHECK ALL THE BEST connections between the first two, then just check
					// best target
					chooseOptimalStart(clusterNames.get(0), clusterNames.get(1), chosenClusterEntityMap, clusters);
					similaritySum += chooseClosestToFirst(clusters, clusterNames, chosenClusterEntityMap);
				}
				break;
			case TOP_PAGERANK:
				if (true) {
					// IF clause for IDE scope pickup
					// Add the choices and their scores
					for (int i = 0; i < clusterNames.size() - 1; ++i) {
						final String fromClusterName = clusterNames.get(i);
						final String toClusterName = clusterNames.get(i + 1);
						final String fromClusterEntity = topPRClusterMap.get(fromClusterName);
						final String toClusterEntity = topPRClusterMap.get(toClusterName);
						chosenClusterEntityMap.put(fromClusterName,
								new ImmutablePair<String, Double>(fromClusterEntity, Double.MIN_VALUE));
						chosenClusterEntityMap.put(toClusterName, new ImmutablePair<String, Double>(toClusterEntity,
								this.similarityService.similarity(fromClusterEntity, toClusterEntity).doubleValue()));
					}
					// Modify the first one (with a first-last connection)
					// Now from last to first and update the 1st's score with the similarity
					final String firstClusterName = clusterNames.get(0);
					final String firstClusterEntity = chosenClusterEntityMap.get(firstClusterName).getLeft();
					final String lastClusterName = clusterNames.get(clusterNames.size() - 1);
					final String lastClusterEntity = chosenClusterEntityMap.get(lastClusterName).getLeft();
					chosenClusterEntityMap.put(firstClusterName, new ImmutablePair<String, Double>(firstClusterEntity,
							this.similarityService.similarity(lastClusterEntity, firstClusterEntity).doubleValue()));
					// new ImmutablePair<String, Double>(firstClusterEntity, Double.MIN_VALUE));
				}
				break;
			}

		}
		// Logger.getLogger(getClass().getName()).info("Initial path choice done in " +
		// Stopwatch.endDiffStart(getClass().getName()) + " ms!");
		// ##############################
		// Initial path choice - END
		// ##############################

		println("########################");
		println("Initialised path - Start");
		displayClusterChoices(chosenClusterEntityMap, "init hillClimb");
		println("Initialised path - End");
		println("########################");

		// ##############################
		// Randomized best-choice - START
		// ##############################
		// final int iterations = 20;
		final int iterations = Math.min(20, (int) (Math.sqrt(clusterNames.size())));
		Set<String> previousChoices = null;
		Set<String> currentChoices = null;
		final int maxIterations = 200;
		int iterCounter = 0;
		println("%Started");
		do {
			previousChoices = currentChoices;
			for (int i = 0; i < iterations; ++i) {
				// Shuffle the cluster names randomly
				Collections.shuffle(clusterNames);
				// This is the hillclimbing part of it which takes the best local choice in
				// order to maximize the reward of the overall similarity sum
				if (CREATE_TABLE) {
					println("%Shuffled clusters");
					println("\\hline");
					println("%Table header");
					final String lastCluster = clusterNames.get(clusterNames.size() - 1);
					// print("From/To");
					for (String clustername : clusterNames) {
						print(bolden(clustername));
						print(" & ");
					}
//					print(" & ");
					print(bolden("Similarity"));
					print(" & ");
					print(bolden("Sum Score"));

					println(" \\\\ \\hline");
					println("%Initialised entities");
					for (String clustername : clusterNames) {
						final String shortEntity = shortenDBpResource(
								chosenClusterEntityMap.get(clustername).getLeft());
						print(shortEntity);
						print(" & ");
					}
					// print(" & ");
					print("/");// N/A sim score
					print(" & ");
					print("/");// N/A overall score
					println(" \\\\ \\hline");
				}
				iterativelyPickLocalBest(clusters, chosenClusterEntityMap, clusterNames);
			}
			currentChoices = mapToValueSet(chosenClusterEntityMap);
			// getLogger().info("CHOICES AFTER ITERATIONS: " + currentChoices);
			iterCounter += iterations;
			if (iterCounter > maxIterations) {
				getLogger().warn("Been iterating(" + iterCounter + ") for a while now...");
				getLogger().warn("Previous choices:" + previousChoices);
				getLogger().warn("Current choices:" + currentChoices);
				break;
			}
		} while (!equals(previousChoices, currentChoices));

		// ##############################
		// Randomized best-choice - END
		// ##############################

		// Add this execution's results to the all-encompassing results map for further
		// analysis
		for (Map.Entry<String, Pair<String, Double>> e : chosenClusterEntityMap.entrySet()) {
			// Entities aggregated over executions for this particular SF
			List<Pair<String, Double>> sfEntities;
			if ((sfEntities = disambiguationResultsMap.get(e.getKey())) == null) {
				sfEntities = Lists.newArrayList();
				disambiguationResultsMap.put(e.getKey(), sfEntities);
			}
			sfEntities.add(e.getValue());
		}

		println("Stabilized Maximum - Start");
		displayClusterChoices(chosenClusterEntityMap, "stabilised results");
		println("Stabilized Maximum - End");
		println("########################################");

		return disambiguationResultsMap;
	}

	private String bolden(String shortEntity) {
		return "\\textbf{" + shortEntity + "}";
	}

	private String shortenDBpResource(String string) {
		String ret = string;
		ret = ret.replace("http://dbpedia.org/resource/", "dbr:");
		ret = ret.replace("Thee_Silver_Mt._Zion_Memorial_Orchestra", "Mem_Orchestra");
		ret = ret.replace("Battle_of_Britain_Memorial_Flight", "Mem_Battle");
		ret = ret.replace("Ponce,_Puerto_Rico", "Ponce_Puerto");
		ret = ret.replace("Late_of_the_Pier", "Late_Pier");
		ret = ret.replace("Bill_Walsh_(American_football_coach)", "Bill_Walsh");
		ret = ret.replace("Wood_Memorial_Stakes", "Mem_Wood");
		ret = ret.replace("Lucas_Oil_Late_Model_Dirt_Series", "Lucas_Oil");
		ret = ret.replace("Stanford_Graduate_School_of_Business", "Stanford_Business");
		ret = ret.replace("Late_Night_with_Jimmy_Fallon", "Late_Night");
		ret = ret.replace("", "");
		ret = ret.replace("_", "\\_");
		return ret;
	}

	/**
	 * Made a method out of it since the minimal score should be dependent on the
	 * operation applied (on whether it is based on summed SIMILARITIES or on
	 * OCCURRENCE)
	 * 
	 * @return minimum score threshold
	 */
	private Double computeMinScore() {
		return ((double) REPEAT) * this.pruneThreshold;
	}

	private void displayAllResultsMap(Map<String, List<Pair<String, Double>>> allResultsMap) {
		getLogger().info("ALL RESULTS MAP - START");
		for (Entry<String, List<Pair<String, Double>>> e : allResultsMap.entrySet()) {
			getLogger().info("[" + e.getKey() + "] " + e.getValue());
		}
		getLogger().info("ALL RESULTS MAP - END");
	}

	private void displayScoredChoices(final String sf, final Map groupMap) {
		final Set<Entry> entrySet = groupMap.entrySet();
		getLogger().info("displayScoredChoices - START");
		for (Map.Entry e : entrySet) {
			getLogger().info("GROUP[" + sf + "] " + e.getKey() + " -> " + e.getValue());
		}
		getLogger().info("displayScoredChoices - END");
	}

	/**
	 * Takes the left part of the value's pair and adds it to a "value set" which it
	 * then returns
	 * 
	 * @param chosenClusterEntityMap map containing the pairs to process
	 * @return values (aka. left part of pair) combined as described
	 */
	private Set<String> mapToValueSet(Map<String, Pair<String, Double>> chosenClusterEntityMap) {
		final Set<String> valueSet = new HashSet<String>();
		// for (Map.Entry<String, Pair<String, Double>> e :
		// chosenClusterEntityMap.entrySet()) {
		// valueSet.add(e.getValue().getLeft());
		// }

		chosenClusterEntityMap.values().stream().forEach(pair -> valueSet.add(pair.getLeft()));
		return valueSet;
	}

	/**
	 * Whether since the previous result
	 * 
	 * @param previousChoices
	 * @param chosenClusterEntityMap
	 * @return
	 */
	private boolean equals(Set<String> previousChoices, Set<String> currentChoices) {
		if (previousChoices == null || currentChoices == null) {
			return (previousChoices == null) && (currentChoices == null);
		}
		if (previousChoices.size() != currentChoices.size()) {
			return false;
		}
		return previousChoices.containsAll(currentChoices);
	}

	/**
	 * Hill-Climbing part. Maximizes over summed similarity towards a specified item
	 * (and whether it increases or decreases thanks to it)
	 * 
	 * @param clusters
	 * @param chosenClusterEntityMap
	 * @param clusterNames
	 */
	private void iterativelyPickLocalBest(Map<String, List<String>> clusters,
			Map<String, Pair<String, Double>> chosenClusterEntityMap, List<String> clusterNames) {
		// Now pick the best one between current and next
		for (int clusterNameIndex = 1; clusterNameIndex < clusterNames.size(); ++clusterNameIndex) {
			final String currClusterName = clusterNames.get(clusterNameIndex - 1);
			final String nextClusterName = clusterNames.get(clusterNameIndex);

			final Pair<String, Double> bestNext = this.similarityService.topSimilarity(
					chosenClusterEntityMap.get(currClusterName).getLeft(), clusters.get(nextClusterName),
					allowSelfConnection);
			if (bestNext == null) {
				// If there is no connection other than possibly oneself (depending on
				// allowSelfConnection), skip it
				continue;
			}
			// If this increases the similarity sum, take it
			// Otherwise just go to the next one
			final Pair<String, Double> previousChoice = chosenClusterEntityMap.remove(nextClusterName);
			final Double newSimilaritySum = getSimilaritySumToEntity(chosenClusterEntityMap, bestNext.getLeft());
			final Double oldSimilaritySum = getSimilaritySumToEntity(chosenClusterEntityMap, previousChoice.getLeft());
//			log.info("NEW 'CANDIDATE' TOP - From[" + currClusterName + "]-To[" + nextClusterName + "]: BEST["
//					+ bestNext.getLeft() + ", " + bestNext.getRight() + "] - NEW SIM: " + newSimilaritySum);
//			log.info("'CURRENT' TOP: " + previousChoice.getLeft() + ", " + previousChoice.getRight()
//					+ " - CURRENT SIM: " + oldSimilaritySum);
			// Intuition: If I like my choice more than anyone dislikes it, it is worth it!
			// So if the new similarity sum is higher, we'll take it
			final Pair<String, Double> chosenEntity;
			final Number chosenSimValue;

			if (newSimilaritySum >= oldSimilaritySum) {
//				log.info("CHOICE DONE: NEW SIMILARITY SUM");
				chosenClusterEntityMap.put(nextClusterName, bestNext);
				chosenEntity = bestNext;
				chosenSimValue = newSimilaritySum;
			} else {
//				log.info("CHOICE DONE: CURRENT SIMILARITY SUM");
				// Update the value if initialization didn't take care of it
				final Pair<String, Double> oldChoiceUpdatedSimilarity = new ImmutablePair<>(previousChoice.getLeft(),
						this.similarityService.similarity(chosenClusterEntityMap.get(currClusterName).getLeft(), //
								previousChoice.getKey()//
						).doubleValue()//
				);
				// Has to be added again for the likely-updated similarity (as other entities
				// might have changed since this was last set)
				// chosenClusterEntityMap.put(nextClusterName, previousChoice);
				chosenClusterEntityMap.put(nextClusterName, oldChoiceUpdatedSimilarity);
				chosenEntity = oldChoiceUpdatedSimilarity;
				chosenSimValue = oldSimilaritySum;
			}

			if (!CREATE_TABLE) {
				final boolean switched = (newSimilaritySum >= oldSimilaritySum);
				final String fromEntity = chosenClusterEntityMap.get(currClusterName).getLeft();
				println("------------------------------------------------------------");
				println("FROM[" + currClusterName + "] -> TO[" + nextClusterName + "]");
				println("From Entity: " + fromEntity);
				println("[" + (switched ? "switchedFrom" : "keeping") + "] Score[" + oldSimilaritySum + "] "
						+ (switched ? "Switched from:" : "Keeping old:") + previousChoice);
				println("[" + (switched ? "switchedTo" : "ignored") + "] Score[" + newSimilaritySum + "] "
						+ (switched ? "Switched   to:" : "'Ignoring' :") + bestNext);
			} else if (CREATE_TABLE) {
				if (ENTITY_TABLE) {
					final DecimalFormat format = new DecimalFormat("##.000000");
					// print(currClusterName + "\t->\t" + nextClusterName //
//							+ " (Sum Old:" + format.format(oldSimilaritySum) //
//							+ "; New:" + format.format(newSimilaritySum) //
//							+ ", Sim Old:" + format.format(previousChoice.getValue()) //
//							+ "; New:" + format.format(bestNext.getValue()) //
//							+ ")" //
//					);
					for (int i = 0; i < clusterNameIndex; ++i) {
						// Spacing for table
						print(" & ");
					}

					print(shortenDBpResource(chosenEntity.getKey()));
					for (int i = clusterNameIndex; i < clusterNames.size() - 1; ++i) {
						// Spacing for table
						print(" & ");
					}
					print(" & ");
					print(format.format(chosenEntity.getValue()));// similarity score of choice
					// println("%" + chosenEntity.toString());
					// println("%" + chosenClusterEntityMap.get(currClusterName) + "->"+
					// chosenClusterEntityMap.get(nextClusterName));
					print(" & ");
					print(format.format(chosenSimValue));// entire path's score
					print("\\\\ ");
					println("\\hline");
				}
			}

		}

	}

	private void println(String str) {
		print(str + System.getProperty("line.separator"));
	}

	private void print(String str) {
		// System.out.print(str);
	}

	/**
	 * Chooses optimal starting position AND optimal 2nd item (updating the second's
	 * similarity) to maximize similarity between first and second item
	 * 
	 * @param fromCluster
	 * @param toCluster
	 * @param chosenClusterEntityMap
	 * @param clusters
	 * @return
	 */
	private String chooseOptimalStart(final String fromCluster, final String toCluster,
			final Map<String, Pair<String, Double>> chosenClusterEntityMap, final Map<String, List<String>> clusters) {
		Double similaritySum = 0D;
		String fromClusterName = fromCluster;
		String toClusterName = toCluster;
		final List<String> fromEntities = clusters.get(fromClusterName);
		final List<String> toEntities = clusters.get(toClusterName);
		Pair<String, Double> bestTarget = null;
		String bestFromEntity = null;
		double currBestSim = Double.MIN_VALUE;

		Stopwatch.start("topSimilarity");
		for (String fromEntity : fromEntities) {
			final Pair<String, Double> entitySimPair = this.similarityService.topSimilarity(fromEntity, toEntities,
					allowSelfConnection);
			if (entitySimPair == null) {
				continue;
			}
			final Double entitySim = entitySimPair.getRight();
			if (entitySim > currBestSim) {
				currBestSim = entitySim;
				bestFromEntity = fromEntity;
				bestTarget = entitySimPair;
//				Logger.getLogger(getClass().getName()).info("[" + Stopwatch.endDiffStart("topSimilarity") + " ms] "
//						+ "FROM(" + bestFromEntity + ") - BEST TARGET(" + bestTarget + ") ");
			}
		}
		// The target of the last cluster's entity will be the first one
		final String finalTarget = bestFromEntity;

//		Logger.getLogger(getClass().getName())
//				.info("Finished (hardcore topsimilarity - FROM(" + fromEntities.size() + ")xTO(" + toEntities.size()
//						+ ")) doing the two first entities in " + Stopwatch.endDiff(getClass().getName()) + " ms!");

		// Put choice into the map for tracking
		// Note: the 'target' of a similarity KEEPS THE SIMILARITY
		// Hence: Putting null/smallest value for SOURCE's similarity
		chosenClusterEntityMap.put(fromClusterName,
				new ImmutablePair<String, Double>(bestFromEntity, Double.MIN_VALUE));

		// Target entity gets the computed similarity
		chosenClusterEntityMap.put(toClusterName, bestTarget);
		similaritySum += bestTarget.getRight();

		// Now iterate through the rest to choose one each
		Stopwatch.endDiffStart("topSimilarity");

		return finalTarget;
	}

	/**
	 * Once the first one has been chosen, continues by taking the most-similar
	 * next-neighbour (does NOT check if the overall similarity is maximized)
	 * 
	 * @param clusters
	 * @param clusterNames
	 * @param chosenClusterEntityMap
	 * @return
	 */
	private Double chooseClosestToFirst(final Map<String, List<String>> clusters, final List<String> clusterNames,
			final Map<String, Pair<String, Double>> chosenClusterEntityMap) {
		Double similaritySum = 0D;
		String fromClusterName, toClusterName;
		for (int i = 1; i < clusterNames.size() - 1; ++i) {
			fromClusterName = clusterNames.get(i);
			toClusterName = clusterNames.get(i + 1);
			final Pair<String, Double> entitySimPair = this.similarityService.topSimilarity(
					chosenClusterEntityMap.get(fromClusterName).getLeft(), clusters.get(toClusterName),
					allowSelfConnection);
			if (entitySimPair != null) {
				chosenClusterEntityMap.put(toClusterName, entitySimPair);
				similaritySum += entitySimPair.getRight();
			}
		}

		// # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
		// Then just get the similarity from the last one to the first one
		// # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

		// SOURCE of the "last-to-first" connection
		final String lastClusterName = clusterNames.get(clusterNames.size() - 1);
		// TARGET of the "last-to-first" connection
		final String firstClusterName = clusterNames.get(0);
		final String firstEntity = chosenClusterEntityMap.get(firstClusterName).getLeft();
		// The entities for the last and first were already chosen, but we need to see
		// how similar they actually are
		final Double lastFirstSimilarity = this.similarityService.similarity(
				// last entity
				chosenClusterEntityMap.get(lastClusterName).getLeft(),
				// first entity
				firstEntity).doubleValue();
		final Pair<String, Double> finalConnection = new ImmutablePair<String, Double>(firstEntity,
				lastFirstSimilarity);
		similaritySum += lastFirstSimilarity;
		chosenClusterEntityMap.put(firstClusterName, finalConnection);

		return similaritySum;
	}

	private Double getSimilaritySumToEntity(Map<String, Pair<String, Double>> chosenClusterEntityMap,
			final String sourceEntity) {
		Double sum = 0D;
		for (Map.Entry<String, Pair<String, Double>> e : chosenClusterEntityMap.entrySet()) {
			if (!sourceEntity.equals(e.getValue().getKey())) {
				sum += this.similarityService.similarity(sourceEntity, e.getValue().getKey()).doubleValue();
			}
		}
		return sum;
	}

	@Override
	public double getPickerWeight() {
		return 20d;
	}

	public String getExperimentSetupString() {
		return "Experiment Setup: INIT_STRATEGY[" + this.initStrategy.name() + "], ITERATIONS[" + this.REPEAT
				+ "], PR_MIN_THRESHOLD[" + this.pagerankMinThreshold + "], PR_TOP_K[" + this.pagerankTopK + "]";
	}

	@Override
	public void printExperimentSetup() {
		getLogger().info(getExperimentSetupString());
	}

}
