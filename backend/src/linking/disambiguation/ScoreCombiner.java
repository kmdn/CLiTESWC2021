package linking.disambiguation;

import java.util.function.BiFunction;

import structure.interfaces.Scorable;
import structure.interfaces.Weighable;
import structure.utils.Loggable;

/**
 * Class determining how scores stemming from various scorer instances are
 * combined into a single disambiguated score
 * 
 * @author Kristian Noullet
 *
 * @param <T> what type the scorers are working with
 */
public class ScoreCombiner<T extends Scorable> implements Loggable {
	public Number combine(final Number currScore, final Number nextScore, final Weighable<T> scorer,
			final T scorerParam) {
		// Add all types of scorers here with the appropriate weights
		//final Number score = scorer.computeScore(scorerParam);
		final Number weight = scorer.getWeight();
		final BiFunction<Number, T, Number> func = scorer.getScoreModulationFunction();
		final Number modulatedVal = func == null ? nextScore : func.apply(nextScore, scorerParam).doubleValue();
		return add(currScore, weight.doubleValue() * modulatedVal.doubleValue());
		// Generally not needed, but PR unfortunately can have some extremely high
		// values by comparison and as such requires some smoothing (e.g. through
		// sqrt())
//		if (scorer instanceof PageRankScorer) {
//			// Pretty much just sets the weight
//			final Double prScore = Numbers.PAGERANK_WEIGHT.val.doubleValue()
//					// Due to PR values varying highly, doing a square root of it to slightly
//					// smoothen it out
//					* Math.sqrt(scorer.computeScore(scorerParam).doubleValue());
//			return add(currScore, prScore);
//		} else if (scorer instanceof VicinityScorer) {
//			final Double vicScore = Numbers.VICINITY_WEIGHT.val.doubleValue()
//					* scorer.computeScore(scorerParam).doubleValue();
//			return add(currScore, vicScore);
//		} else {
//			return add(currScore, weight.doubleValue() * score.doubleValue());
//		}
	}

	/**
	 * Transforms both numbers to double and adds them together.<br>
	 * <b>If currScore is NULL, it is treated as 0.</b>
	 * 
	 * @param currScore
	 * @param score
	 * @return
	 */
	private Number add(Number currScore, Number score) {
		return currScore == null ? score.doubleValue() : currScore.doubleValue() + score.doubleValue();
	}
}
