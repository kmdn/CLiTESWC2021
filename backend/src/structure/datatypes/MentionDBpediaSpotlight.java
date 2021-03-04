package structure.datatypes;

import java.util.Collection;
import java.util.List;

import com.beust.jcommander.internal.Lists;

import structure.interfaces.FeatureStringable;

public class MentionDBpediaSpotlight extends Mention implements FeatureStringable {
	private Double scoreSecond;
	private String types;
	private Integer support;

	public MentionDBpediaSpotlight(Mention mention) {
		super(mention);
		// TODO Auto-generated constructor stub
	}

	public MentionDBpediaSpotlight(String word, Collection<PossibleAssignment> possibleAssignments, int offset,
			double detectionConfidence, String originalMention, String originalWithoutStopwords) {
		super(word, possibleAssignments, offset, detectionConfidence, originalMention, originalWithoutStopwords);
		// TODO Auto-generated constructor stub
	}

	public MentionDBpediaSpotlight(String word, PossibleAssignment assignment, int offset, double detectionConfidence,
			String originalMention, String originalWithoutStopwords) {
		super(word, assignment, offset, detectionConfidence, originalMention, originalWithoutStopwords);
		// TODO Auto-generated constructor stub
	}

	public MentionDBpediaSpotlight(String word, PossibleAssignment assignment, int offset) {
		super(word, assignment, offset);
		// TODO Auto-generated constructor stub
	}

	public Double getScoreSecond() {
		return scoreSecond;
	}

	public MentionDBpediaSpotlight setScoreSecond(Double scoreSecond) {
		this.scoreSecond = scoreSecond;
		return this;
	}

	public String getTypes() {
		return types;
	}

	public MentionDBpediaSpotlight setTypes(String types) {
		this.types = types;
		return this;
	}

	public Integer getSupport() {
		return support;
	}

	public MentionDBpediaSpotlight setSupport(Integer support) {
		this.support = support;
		return this;
	}

	@Override
	public List<String> toFeatureString() {
		final List<String> ret = Lists.newArrayList();
		ret.add("DBpedia");
		ret.add(getScoreSecond().toString());
		ret.add(getSupport().toString());
		ret.add(getTypes());
		return ret;
	}

}
