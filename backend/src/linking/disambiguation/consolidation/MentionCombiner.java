package linking.disambiguation.consolidation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.beust.jcommander.internal.Lists;

import structure.datatypes.Mention;

public class MentionCombiner {
	private final BiFunction<List<Mention>, List<Mention>, List<Mention>> combinationFunction;

	MentionCombiner(final BiFunction<List<Mention>, List<Mention>, List<Mention>> combinationFunction) {
		this.combinationFunction = combinationFunction;
	}

	/**
	 * Returns </br>
	 * Note: It always creates a copy of the list
	 * @param mentions
	 * @return
	 */
	public List<Mention> combine(final List<List<Mention>> mentions) {
		if (mentions.size() <= 0) {
			return new ArrayList<Mention>();
		}

		if (mentions.size() == 1) {
			return Lists.newArrayList(mentions.get(0));
		}

		List<Mention> retMentions = Lists.newArrayList(mentions.get(0));
		for (int i = 1; i < mentions.size(); ++i) {
			retMentions = this.combinationFunction.apply(retMentions, mentions.get(i));
		}
		return retMentions;
	}

	public List<Mention> combine(final List<Mention>... multiMentions) {
		final List<List<Mention>> tmpMentions = Lists.newArrayList();
		for (List<Mention> mentions : multiMentions) {
			tmpMentions.add(mentions);
		}
		return combine(tmpMentions);
	}

}
