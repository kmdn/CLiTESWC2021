package linking.disambiguation;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;

import linking.disambiguation.scorers.ContinuousHillClimbingPicker;
import linking.disambiguation.scorers.GraphWalkEmbeddingScorer;
import linking.disambiguation.scorers.PageRankScorer;
import linking.disambiguation.scorers.VicinityScorerDirectedSparseGraph;
import linking.disambiguation.scorers.embedhelp.CombineOperation;
import linking.disambiguation.scorers.embedhelp.EntitySimilarityService;
import linking.disambiguation.scorers.pagerank.PageRankLoader;
import structure.config.constants.EnumEmbeddingMode;
import structure.config.constants.FilePaths;
import structure.config.constants.Numbers;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.PostScorer;
import structure.interfaces.Scorer;
import structure.utils.EmbeddingsUtils;
import structure.utils.Loggable;
import structure.utils.Stopwatch;

public class DisambiguatorAgnos extends AbstractMultiDisambiguator implements MultiDisambiguator, Loggable {
	private final long sleeptime = 100l;
	private final boolean IGNORE_DOUBLED_MENTIONS = true;
	private final EntitySimilarityService similarityService;

	/**
	 * Default setting constructor for a defined knowledge graph
	 * 
	 * @param KG
	 * @throws IOException
	 */
	public DisambiguatorAgnos(final EnumModelType KG) throws IOException {
		this(KG, (Set<String>) null);
	}

	public DisambiguatorAgnos(final EnumModelType KG, final Set<String> wantedEntities) throws IOException {
		this(KG, EnumEmbeddingMode.DEFAULT, wantedEntities);
	}

	public DisambiguatorAgnos(final EnumModelType KG, final EnumEmbeddingMode embeddingMode) throws IOException {
		this(KG, embeddingMode, null);
	}

	public DisambiguatorAgnos(final EnumModelType KG, final EnumEmbeddingMode embeddingMode,
			final Set<String> wantedEntities) throws IOException {
		final CombineOperation combineOperation = CombineOperation.MAX_SIM;

		// Pre-Scoring
		// Add PR Scoring and get PRLoader for other scoring mechanisms
		final PageRankLoader pagerankLoader = setupPageRankScoringAddScorer(KG);

		// Post-scoring
		// PossibleAssignment.addPostScorer(new VicinityScorer());

//		int displayCounter = 0;
//		for (Entry<String, List<Number>> e : entityEmbeddingsMap.entrySet()) {
//			System.out.println(e.getKey());
//			displayCounter++;
//			if (displayCounter > 50) {
//				System.out.println("printed 50");
//				break;
//			}
//		}

		System.out.println("Added VicinityScorerDirectedSparseGraph");
		addPostScorer(new VicinityScorerDirectedSparseGraph(KG));

		final boolean doEmbeddings = true;
		if (doEmbeddings) {
			System.out.println("Added GraphWalkEmbeddingScorer " + "[" + KG.name() + "]");
			this.similarityService = setupSimilarityService(KG, embeddingMode, wantedEntities);
			addPostScorer(new GraphWalkEmbeddingScorer(new ContinuousHillClimbingPicker(
					combineOperation.combineOperation, similarityService, pagerankLoader)));
		} else {
			this.similarityService = null;
		}
	}

	private EntitySimilarityService setupSimilarityService(EnumModelType KG, final EnumEmbeddingMode embeddingMode,
			final Set<String> wantedEntities) throws IOException {
		final Map<String, List<Number>> entityEmbeddingsMap;
		if (embeddingMode == EnumEmbeddingMode.LOCAL) {
			entityEmbeddingsMap = EmbeddingsUtils.humanload(
					FilePaths.FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN.getPath(KG),
					FilePaths.FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS.getPath(KG), wantedEntities);
			return new EntitySimilarityService(entityEmbeddingsMap);
		} else {
			return new EntitySimilarityService();
		}
	}

	/**
	 * Adds PR scorer and loads the PR scores
	 */
	private PageRankLoader setupPageRankScoringAddScorer(final EnumModelType KG) throws IOException {
		// How to load pagerank
		final String pagerankWatch = "pagerank";
		Stopwatch.start(pagerankWatch);
		final PageRankLoader pagerankLoader = new PageRankLoader(KG);
		// Loads the pagerank from file
		pagerankLoader.exec();
		Stopwatch.endOutput(pagerankWatch);

		// Pre-scoring
		addScorer(new PageRankScorer(KG, pagerankLoader));
		return pagerankLoader;
	}

	public Collection<Mention> disambiguate(final String input, final Collection<Mention> mentions) throws InterruptedException {
		return disambiguate(input, mentions, IGNORE_DOUBLED_MENTIONS);
	}

	/**
	 * Disambiguate mentions and find the best possible assignment for each
	 * 
	 * @param mentions              list of mentions
	 * @param removeDoubledMentions
	 * @return 
	 * @throws InterruptedException
	 */
	public Collection<Mention> disambiguate(final String input, final Collection<Mention> mentions, final boolean removeDoubledMentions)
			throws InterruptedException {
		// Update contexts
		updatePostContext(mentions);

		// Start sending mentions to scorers
		if (removeDoubledMentions) {
			// In order to avoid disambiguating multiple times for the same mention word, we
			// split our mentions up and then just copy results from the ones that were
			// computed
			final Map<String, Collection<Mention>> mentionMap = new HashMap<>();
			// Split up the mentions by their keys
			for (final Mention mention : mentions) {
				Collection<Mention> val;
				if ((val = mentionMap.get(mention.getMention())) == null) {
					val = Lists.newArrayList();
					mentionMap.put(mention.getMention(), val);
				}
				val.add(mention);
			}
			for (final Map.Entry<String, Collection<Mention>> e : mentionMap.entrySet()) {
				// Just score the first one within the lists of doubled mentions
				final Collection<Mention> sameWordMentions = e.getValue();
				final Mention mention = sameWordMentions.iterator().next();// .get(0);
				score(mention);
				// Assign the top-scored possible assignment to the mention
				mention.assignBest();
				// Copy into the other mentions
				// for (int i = 1; i < e.getValue().size(); ++i) {
				final Iterator<Mention> itSameWordMentions = sameWordMentions.iterator();
				while (itSameWordMentions.hasNext()) {
					// Skip the first one as it's just time lost...
					final Mention sameWordMention = itSameWordMentions.next();
					sameWordMention.copyResults(mention);
				}
			}
		} else {
			for (Mention mention : mentions) {
				// Compute the score for this mention
				score(mention);
				// Assign the best possible assignment to this mention
				// Best assignment can be retrieved from mention object directly
				mention.assignBest();
			}
		}
		clearContext();
		return mentions;
	}

	/**
	 * Calls the scorer appropriately and returns a list of the assignments
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	private Collection<PossibleAssignment> score(final Mention mention) throws InterruptedException {
		// Now score all of the assignments based on their own characteristics
		// and on the contextual ones
		Collection<PossibleAssignment> possAssignments = mention.getPossibleAssignments();
		final int assSize = possAssignments.size();
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(Numbers.SCORER_THREAD_AMT.val.intValue());
		final AtomicInteger doneCounter = new AtomicInteger(0);
		for (PossibleAssignment assgnmt : possAssignments) {
			// Multi thread here
			final Future<Integer> future = executor.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					final Number score = computeScore(assgnmt);
					assgnmt.setScore(score);
					return doneCounter.incrementAndGet();
				}
			});
		}
		executor.shutdown();
		long sleepCounter = 0l;
		do {
			// No need for await termination as this is pretty much it already...
			Thread.sleep(sleeptime);
			sleepCounter += sleeptime;
			if ((sleepCounter > 5_000) && ((sleepCounter % 5000) <= sleeptime)) {
				getLogger().debug(
						"Score Computation - In progress [" + doneCounter.get() + " / " + assSize + "] documents.");
			}
		} while (!executor.isTerminated());
		final boolean terminated = executor.awaitTermination(10L, TimeUnit.MINUTES);
		if (!terminated) {
			throw new RuntimeException("Could not compute score in time.");
		}
		return mention.getPossibleAssignments();
	}

	/**
	 * Computes the score for this possible assignment
	 */
	private Number computeScore(final PossibleAssignment assgnmt) {
		Number currScore = null;
		// Goes through all the scorers that have been defined and combines them in the
		// wanted manner
		// Pre-scoring step
		for (Scorer<PossibleAssignment> scorer : getScorers()) {
			final Number nextScore = scorer.computeScore(assgnmt);
			currScore = getScoreCombiner().combine(currScore, nextScore, scorer, assgnmt);
		}
		// Post-scoring step
		for (PostScorer<PossibleAssignment, Mention> scorer : getPostScorers()) {
			final Number nextScore = scorer.computeScore(assgnmt);
			currScore = getScoreCombiner().combine(currScore, nextScore, scorer, assgnmt);
		}
		return currScore;
	}
}