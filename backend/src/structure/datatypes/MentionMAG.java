package structure.datatypes;

import java.util.List;

import com.beust.jcommander.internal.Lists;

import structure.interfaces.FeatureStringable;

public class MentionMAG extends Mention implements FeatureStringable {

	public MentionMAG(String word, PossibleAssignment possAss, int offset,
			Number defaultScore, String originalMention, String originalWithoutStopwords) {
		super(word, possAss, offset, defaultScore.doubleValue(), originalMention,
				originalWithoutStopwords);
	}

	@Override
	public List<String> toFeatureString() {
		// TODO
		final List<String> ret = Lists.newArrayList();
		ret.add("MAG");
		return ret;
	}
	

}
