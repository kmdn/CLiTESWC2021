package linking.disambiguation.scorers;

import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;

import linking.disambiguation.scorers.pagerank.PageRankLoader;
import structure.config.constants.FilePaths;
import structure.config.constants.Numbers;
import structure.config.kg.EnumModelType;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.Scorer;
import structure.utils.Stopwatch;

/**
 * Class handling apriori PageRank scores by simply loading the appropriate one
 * for a wanted PossibleAssignment instance
 * 
 * @author Kristian Noullet
 *
 */
public class PageRankScorer implements Scorer<PossibleAssignment> {
	private static final Logger logger = Logger.getLogger(PageRankScorer.class);
	private final EnumModelType KG;
	private int warnCounter = 0;
	private final PageRankLoader pagerankLoader;
	private final BiFunction<Number, PossibleAssignment, Number> func = new BiFunction<Number, PossibleAssignment, Number>() {

		@Override
		public Number apply(Number score, PossibleAssignment scorerParam) {
			// Pretty much just sets the weight
			return Math.sqrt(score.doubleValue());
		}
	};

	public PageRankScorer(final EnumModelType KG) throws IOException {
		this(KG, false);
	}

	public PageRankScorer(final EnumModelType KG, final boolean forceReload) throws IOException {
		this(KG, forceReload, new File(FilePaths.FILE_PAGERANK.getPath(KG)));
	}

	/**
	 * Loads PageRank only once unless forced through the forceReload param
	 * 
	 * @param forceReload
	 * @throws Exception
	 */
	public PageRankScorer(final EnumModelType KG, final boolean forceReload, final File pageRankFile)
			throws IOException {
		this.KG = KG;
		this.pagerankLoader = new PageRankLoader(KG);
		// Only load pagerank once (or it will takes ages for the same result)
		Stopwatch.start("pagerankloading");
		loadPageRank();
		Stopwatch.endOutput("pagerankloading");
	}

	public PageRankScorer(final EnumModelType KG, final PageRankLoader pagerankLoader) {
		this.KG = KG;
		this.pagerankLoader = pagerankLoader;
	}

	@Override
	public Number computeScore(PossibleAssignment param) {
		final String assignment = param.getAssignment();
		if (assignment != null) {
			final Number retNumber = this.pagerankLoader.getScore(assignment.toString());
			if (retNumber == null) {
				warnCounter++;
				if (warnCounter % 100_000 == 0) {
					logger.warn(
							warnCounter + " - No page rank value found for: Assignment(" + assignment.toString() + ")");
				}
			}
			return retNumber == null ? 0f : retNumber;
		} else {
			logger.error("Assignment is NULL");
		}
		return 0f;
	}

	/**
	 * Loads the pagerank map from the default location
	 * 
	 * @return
	 * @throws Exception
	 */
	public void loadPageRank() throws IOException {
		this.pagerankLoader.exec();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		return hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		//Means every instance will have the same hashCode value
		return getClass().getName().hashCode();
	}

	@Override
	public Number getWeight() {
		return Numbers.PAGERANK_WEIGHT.val;
	}

	@Override
	public BiFunction<Number, PossibleAssignment, Number> getScoreModulationFunction() {
		return func;
	}

}
