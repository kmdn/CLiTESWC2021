package linking.disambiguation.scorers.pagerank;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import structure.config.constants.FilePaths;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.datatypes.pr.PageRankScore;
import structure.interfaces.Executable;
import structure.utils.DecoderUtils;
import structure.utils.Stopwatch;

/**
 * Class handling loading of PageRank scores for specific KGs
 * 
 * @author Kristian Noullet
 *
 */
public class PageRankLoader implements Executable {
	private final EnumModelType KG;
	public Map<String, Number> pagerankScores = null;
	public static Set<String> setPRNotFound = new HashSet<>();

	public PageRankLoader(final EnumModelType KG) {
		this.KG = KG;
	}

	@Override
	public void init() {
		// Nothing to do here
	}

	@Override
	public Map<String, Number> exec() throws IOException {
		return exec(null);
	}

	/**
	 * Ignores input arguments and just loads values from the file specified in
	 * FilePaths.FILE_PAGERANK in the tree for the specific KG
	 */
	@Override
	public Map<String, Number> exec(Object... o) throws IOException {
		// Load PR from file
		getLogger().debug("Loading PageRank");
		final String watchName = this.getClass().getName();
		Stopwatch.start(watchName);
		this.pagerankScores = readIn(new File(FilePaths.FILE_PAGERANK.getPath(this.KG)));
		getLogger().debug("Finished loading! ("+Stopwatch.endDiff(watchName)+")");
		return this.pagerankScores;
	}

	@Override
	public boolean destroy() {
		this.pagerankScores.clear();
		this.pagerankScores = null;
		return true;
	}

	/**
	 * Returns the score if the pagerank was loaded
	 * 
	 * @param entity entity for which a score exists
	 * @return score associated to the entity
	 */
	public Number getScore(final String entity) {
		synchronized (this.pagerankScores) {
			if (this.pagerankScores == null || this.pagerankScores.size() == 0) {
				try {
					exec();
				} catch (IOException e) {
					getLogger().error(
							"IOException while loading PageRank scores from file (KG = " + this.KG.name() + ")", e);
				}
			}
		}
		final String entityURL = processKey(entity);
		Number ret = this.pagerankScores.get(entityURL);
		if (ret == null) {
			final String decoded = DecoderUtils.escapePercentage(entityURL);
			if (decoded != null && decoded.length() > 0) {
				ret = this.pagerankScores.get(decoded);
			}
		}
		return ret;
	}

	/**
	 * Takes all the possible assignments and checks for their PR scores and ranks
	 * them in a list (descendingly) and takes the topK defined
	 * 
	 * @param mention
	 * @param topK
	 * @return
	 */
	public List<PageRankScore> getTopK(final Mention mention, final int topK) {
		return getTopK(mention.getPossibleAssignments(), topK);
	}

	public List<PageRankScore> getTopK(final Mention mention, final int topK, final double minThreshold) {
		final List<PageRankScore> topKList = getTopK(mention, topK);
		int cutOffIndex = -1;
		for (int i = 0; i < topKList.size(); ++i) {
			final PageRankScore assScore = topKList.get(i);
			if (assScore.score.doubleValue() < minThreshold) {
				cutOffIndex = i;
				break;
			}
		}
		if (cutOffIndex == 0) {
			return null;
		}
		return topKList.subList(0, cutOffIndex);
	}

	/**
	 * Cuts off the passed (sorted) list if the score goes under the passed
	 * threshold<br>
	 * Returns NULL rather than an empty list if the first one already is below
	 * threshold
	 * 
	 * @param scores
	 * @param minThreshold
	 * @return
	 */
	public <T extends Comparable<T>> List<PageRankScore> cutOff(final Collection<T> scores,
			final double minThreshold) {
		int cutOffIndex = -1;
		int counter = 0;
		List<PageRankScore> assignmentScores = makeOrPopulateList(scores);
		Collections.sort(assignmentScores, Comparator.reverseOrder());
		for (PageRankScore assScore : assignmentScores) {
			if (assScore.score.doubleValue() < minThreshold) {
				cutOffIndex = counter;
				break;
			}
			counter++;
		}
		if (cutOffIndex == -1) {
			// Means none of them was too small, so take them all!
			// cutOffIndex = scores.size();
			// getLogger().info("ALL scores are good 'enough':" + scores);
			return assignmentScores;
		}

		if (cutOffIndex == 0) {
			// getLogger().info("NULL - Too small Scores list:" + scores);
			return Lists.newArrayList();
		}
		// getLogger().info("LIMITED scores [0," + cutOffIndex + "]:" + scores);
		final List<PageRankScore> retList = assignmentScores.subList(0, cutOffIndex);
		return retList;
	}

	public <T extends Comparable<T>> List<PageRankScore> getTopK(final Collection<T> assignments, final int topK) {
		final List<PageRankScore> assignmentScores = makeOrPopulateList(assignments);
		if (assignmentScores.size() == 0) {
			return assignmentScores;
		}
		Collections.sort(assignmentScores, Comparator.reverseOrder());
		return assignmentScores.subList(0, Math.min(assignmentScores.size(), topK));
	}

	public <T extends Comparable<T>> List<PageRankScore> makeOrPopulateList(Collection<T> assignments) {
		final List<PageRankScore> assignmentScores = Lists.newArrayList();
		for (T possAss : assignments) {
			if (possAss instanceof PageRankScore) {
				assignmentScores.add((PageRankScore) possAss);
			} else {
				Number foundScore = getScore(possAss.toString());
				if (foundScore == null) {
					// getLogger().error("[" + possAss.toString() + "] No PR score.");
					setPRNotFound.add(possAss.toString());
					foundScore = 0d;
				}

				final PageRankScore assignmentScore = new PageRankScore().assignment(possAss.toString())
						.score(foundScore);
				assignmentScores.add(assignmentScore);
			}
		}
		return assignmentScores;
	}

	/**
	 * Reads pagerank from a proper pagerank RDF file where the source is the node
	 * for which the object is the pagerank value of e.g. <a> <:PRValue> "50.23"
	 * 
	 * @param inFile
	 * @return
	 */
	public static Map<String, Number> readIn(final File inFile) {
		final Map<String, Number> map = new HashMap<String, Number>();
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			final NxParser nxparser = new NxParser(brIn);
			while (nxparser.hasNext()) {
				final Node[] nodes = nxparser.next();
				try {
					final String key = processKey(nodes[0].toString());
					Number val;
					if ((val = map.get(key)) == null) {
						val = 0f;
					}
					// Sums up PR values if there's multiple variations of the same entity, e.g.
					// uppercase and lowercase
					map.put(key, val.floatValue() + Float.valueOf(nodes[2].toString()));
				} catch (ArrayIndexOutOfBoundsException aiooe) {
					getLog().error("Error appeared with: " + Arrays.toString(nodes));
					throw aiooe;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	private static String processKey(final String key) {
		return key.toLowerCase();
	}

	public static Logger getLog() {
		return org.apache.log4j.Logger.getLogger(PageRankLoader.class.getName());
	}
}
