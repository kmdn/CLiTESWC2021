package structure.datatypes;

import java.util.List;

import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Meaning;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.ScoredMarking;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.gerbil.transfer.nif.data.ScoredAnnotation;
import org.aksw.gerbil.transfer.nif.data.ScoredNamedEntity;

import com.beust.jcommander.internal.Lists;

import structure.interfaces.FeatureStringable;

public class MentionOpenTapioca extends MentionMarking implements FeatureStringable {

	public MentionOpenTapioca(String inText, Marking marking) {
		super(inText, marking);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String inText, Meaning meaning) {
		super(inText, meaning);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String inText, MeaningSpan meaningSpan) {
		super(inText, meaningSpan);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String inText, NamedEntity namedEntity) {
		super(inText, namedEntity);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String word, PossibleAssignment assignment, int offset, double detectionConfidence,
			String originalMention, String originalWithoutStopwords) {
		super(word, assignment, offset, detectionConfidence, originalMention, originalWithoutStopwords);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String word, PossibleAssignment assignment, int offset) {
		super(word, assignment, offset);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String inText, ScoredAnnotation annotation) {
		super(inText, annotation);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String inText, ScoredMarking marking) {
		super(inText, marking);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String inText, ScoredNamedEntity scoredNamedEntity) {
		super(inText, scoredNamedEntity);
		// TODO Auto-generated constructor stub
	}

	public MentionOpenTapioca(String inText, Span span) {
		super(inText, span);
		// TODO Auto-generated constructor stub
	}

	public static MentionOpenTapioca create(final String input, final Marking m) {
		if (m instanceof NamedEntity) {
			return new MentionOpenTapioca(input, (NamedEntity) m);
		}
		return new MentionOpenTapioca(input, m);
	}

	@Override
	public List<String> toFeatureString() {
		final List<String> ret = Lists.newArrayList();
		ret.add("OpenTapioca");
		final PossibleAssignment ass = getAssignment();
		ret.add(ass == null || ass.getAssignment() == null ? "NO_ASS" : ass.getAssignment());
		ret.add(ass == null || ass.getScore() == null ? "-1.0" : String.valueOf(ass.getScore().doubleValue()));
		return ret;
	}

}
