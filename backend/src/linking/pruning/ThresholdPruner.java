package linking.pruning;

import java.util.Collection;
import java.util.List;

import com.beust.jcommander.internal.Lists;

import structure.datatypes.Mention;

public class ThresholdPruner implements MentionPruner {
	private final double threshold;

	public ThresholdPruner(final double threshold) {
		// Do a pruner that works
		// 1. with the min threshold of the found score = THRESHOLDING
		// 2. "how much of final score is from interconnections of top-scored entities?"
		// if a big part -> keep it,
		// otherwise if score is just from 'background' entities, it means that our
		// front-row place is
		// not very deserved
		// 3. if we are the only one, remove based on threshold
		// 4. for 2. add also a check that the next-best entities have possibilities of
		// 'climbing' up
		this.threshold = threshold;
	}

	@Override
	public List<Mention> prune(Collection<Mention> mentions) {
		final List<Mention> retMentions = Lists.newArrayList();
//		double summedWeight = 0d;
//		for (Scorer<PossibleAssignment> scorer : Disambiguator.getScorers()) {
//			summedWeight += scorer.getWeight().doubleValue();
//		}
//
//		final double minThreshold = threshold * summedWeight;
		final double minThreshold = threshold;
		for (Mention mention : mentions) {
			if (mention.getAssignment().getScore().doubleValue() >= minThreshold) {
				retMentions.add(mention);
			}
		}
		return retMentions;
	}
}
