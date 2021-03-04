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

/**
 * Details on markings
 * https://github.com/dice-group/gerbil/wiki/Document-Markings-in-gerbil.nif.transfer
 * 
 * Marking: toString/clone ScoredMarking: getConfidence, setConfidence
 * 
 * @author wf7467
 *
 */
public class MentionMarking extends Mention {
	private final static double defaultConfidence = 1.0d;
	private final static double defaultScore = 1.0d;

	public MentionMarking(String word, PossibleAssignment assignment, int offset, double detectionConfidence,
			String originalMention, String originalWithoutStopwords) {
		super(word, assignment, offset, detectionConfidence, originalMention, originalWithoutStopwords);
	}

	public MentionMarking(String word, PossibleAssignment assignment, int offset) {
		super(word, assignment, offset);
	}

	public MentionMarking(final String inText, final Marking marking) {
		this(marking.toString(), null, -1, defaultConfidence, marking.toString(), marking.toString());
	}

	public MentionMarking(final String inText, final ScoredMarking marking) {
		// String word, PossibleAssignment assignment, int offset, double
		// detectionConfidence, String originalMention, String originalWithoutStopwords
		this(marking.toString(), null, -1, marking.getConfidence(), marking.toString(), marking.toString());
	}

	public MentionMarking(final String inText, final ScoredAnnotation annotation) {
		// String word, PossibleAssignment assignment, int offset, double
		// detectionConfidence, String originalMention, String originalWithoutStopwords
		this(annotation.toString(), new PossibleAssignment(annotation.getUri() // ,annotation.toString()
		), -1, annotation.getConfidence(), annotation.toString(), annotation.toString());
		updatePossibleAssignments(transformURIs2Assignment("", annotation));
	}

	public MentionMarking(final String inText, final NamedEntity namedEntity) {
		// String word, PossibleAssignment assignment, int offset, double
		// detectionConfidence, String originalMention, String originalWithoutStopwords
		this(inText.substring(namedEntity.getStartPosition(), namedEntity.getStartPosition() + namedEntity.getLength()),
				new PossibleAssignment(namedEntity.getUri()
				// ,inText.substring(namedEntity.getStartPosition(),
				// namedEntity.getStartPosition() + namedEntity.getLength())
				), namedEntity.getStartPosition(), defaultConfidence,
				inText.substring(namedEntity.getStartPosition(),
						namedEntity.getStartPosition() + namedEntity.getLength()),
				inText.substring(namedEntity.getStartPosition(),
						namedEntity.getStartPosition() + namedEntity.getLength()));
		// Add all URIs
		final String mention = inText.substring(namedEntity.getStartPosition(),
				namedEntity.getStartPosition() + namedEntity.getLength());
		updatePossibleAssignments(transformURIs2Assignment(mention, namedEntity));
	}

	public MentionMarking(final String inText, final ScoredNamedEntity scoredNamedEntity) {
		// String word, PossibleAssignment assignment, int offset, double
		// detectionConfidence, String originalMention, String originalWithoutStopwords
		this(inText.substring(scoredNamedEntity.getStartPosition(),
				scoredNamedEntity.getStartPosition() + scoredNamedEntity.getLength()),
				new PossibleAssignment(scoredNamedEntity.getUri()
				// ,inText.substring(scoredNamedEntity.getStartPosition(),scoredNamedEntity.getStartPosition()
				// + scoredNamedEntity.getLength())
				), scoredNamedEntity.getStartPosition(), scoredNamedEntity.getConfidence(),
				inText.substring(scoredNamedEntity.getStartPosition(),
						scoredNamedEntity.getStartPosition() + scoredNamedEntity.getLength()),
				inText.substring(scoredNamedEntity.getStartPosition(),
						scoredNamedEntity.getStartPosition() + scoredNamedEntity.getLength()));
		// Add all URIs
		final String mention = inText.substring(scoredNamedEntity.getStartPosition(),
				scoredNamedEntity.getStartPosition() + scoredNamedEntity.getLength());
		updatePossibleAssignments(transformURIs2Assignment(mention, scoredNamedEntity));
	}

	public MentionMarking(final String inText, final Span span) {
		// String word, PossibleAssignment assignment, int offset, double
		// detectionConfidence, String originalMention, String originalWithoutStopwords
		this(inText.substring(span.getStartPosition(), span.getStartPosition() + span.getLength()), null,
				span.getStartPosition(), defaultConfidence,
				inText.substring(span.getStartPosition(), span.getStartPosition() + span.getLength()),
				inText.substring(span.getStartPosition(), span.getStartPosition() + span.getLength()));
	}

	public MentionMarking(final String inText, final Meaning meaning) {
		// String word, PossibleAssignment assignment, int offset, double
		// detectionConfidence, String originalMention, String originalWithoutStopwords
		this("", new PossibleAssignment(meaning.getUri()// ,""
		), -1, defaultConfidence, "", "");
		updatePossibleAssignments(transformURIs2Assignment("", meaning));
	}

	public MentionMarking(final String inText, final MeaningSpan meaningSpan) {
		// String word, PossibleAssignment assignment, int offset, double
		// detectionConfidence, String originalMention, String originalWithoutStopwords
		this(inText.substring(meaningSpan.getStartPosition(), meaningSpan.getStartPosition() + meaningSpan.getLength()),
				null, meaningSpan.getStartPosition(), defaultConfidence,
				inText.substring(meaningSpan.getStartPosition(),
						meaningSpan.getStartPosition() + meaningSpan.getLength()),
				inText.substring(meaningSpan.getStartPosition(),
						meaningSpan.getStartPosition() + meaningSpan.getLength()));
		final String mention = inText.substring(meaningSpan.getStartPosition(),
				meaningSpan.getStartPosition() + meaningSpan.getLength());
		updatePossibleAssignments(transformURIs2Assignment(mention, meaningSpan));
	}

	/**
	 * Transforms a list of URIs into a list of possible assignments
	 * 
	 * @param mention text
	 * @param meaning where to get URIs from
	 * @return list of possible assignments
	 */
	private List<PossibleAssignment> transformURIs2Assignment(final String mention, final Meaning meaning) {
		List<PossibleAssignment> possibleAssignments = Lists.newArrayList();
		for (String uri : meaning.getUris()) {
			final PossibleAssignment possAss = new PossibleAssignment(uri// , mention
			);
			possAss.setScore(defaultScore);
			possibleAssignments.add(possAss);
		}
		return possibleAssignments;
	}

	/**
	 * Creates a MentionMarking with the appropriate type of Marking
	 * 
	 * @param input input text
	 * @param m     marking from this passed text
	 * @return new MentionMarking instance
	 */
	public static MentionMarking create(final String input, final Marking m) {
		if (m instanceof NamedEntity) {
			return new MentionMarking(input, (NamedEntity) m);
		}
		return new MentionMarking(input, m);
	}

}
