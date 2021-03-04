package structure.datatypes;

import java.util.Collection;
import java.util.List;

import com.beust.jcommander.internal.Lists;

import structure.interfaces.FeatureStringable;

public class MentionBabelfy extends Mention implements FeatureStringable {
	private String babelfyBabelSynsetID;
	private Double babelfyGlobalScore;
	private String babelfySource;

	public MentionBabelfy(Mention mention) {
		super(mention);
		// TODO Auto-generated constructor stub
	}

	public MentionBabelfy(String word, Collection<PossibleAssignment> possibleAssignments, int offset,
			double detectionConfidence, String originalMention, String originalWithoutStopwords) {
		super(word, possibleAssignments, offset, detectionConfidence, originalMention, originalWithoutStopwords);
	}

	public MentionBabelfy(String word, PossibleAssignment assignment, int offset, double detectionConfidence,
			String originalMention, String originalWithoutStopwords) {
		super(word, assignment, offset, detectionConfidence, originalMention, originalWithoutStopwords);
	}

	public MentionBabelfy(String word, PossibleAssignment assignment, int offset) {
		super(word, assignment, offset);
	}

	public String getBabelfyBabelSynsetID() {
		return babelfyBabelSynsetID;
	}

	public MentionBabelfy setBabelfyBabelSynsetID(String babelfyBabelSynsetID) {
		this.babelfyBabelSynsetID = babelfyBabelSynsetID;
		return this;
	}

	public Double getBabelfyGlobalScore() {
		return babelfyGlobalScore;
	}

	public MentionBabelfy setBabelfyGlobalScore(Double babelfyGlobalScore) {
		this.babelfyGlobalScore = babelfyGlobalScore;
		return this;
	}

	public String getBabelfySource() {
		return babelfySource;
	}

	public MentionBabelfy setBabelfySource(String babelfySource) {
		this.babelfySource = babelfySource;
		return this;
	}

	@Override
	public List<String> toFeatureString() {
		final List<String> ret = Lists.newArrayList();
		ret.add("Babelfy");
		ret.add(getBabelfyBabelSynsetID());
		ret.add(getBabelfyGlobalScore().toString());
		ret.add(getBabelfySource());
		return ret;
	}

}
