package structure.datatypes;

import org.apache.log4j.Logger;

import structure.interfaces.Scorable;
import structure.utils.Loggable;

/**
 * A possible output candidate for entity linking - it can be scored, based on
 * the score it can be compared to another possible assignment with (assumingly)
 * the same mention
 * 
 * @author Kristian Noullet
 *
 */
public class PossibleAssignment implements Scorable, Comparable<PossibleAssignment>, Loggable {
	private static Logger logger = Logger.getLogger(PossibleAssignment.class);
	private static final float defaultScore = -1f;
	private Number score = Float.valueOf(0f);
	private final String assignment;
	// private final String mentionToken;
	private boolean computedScore = false;
	private boolean warned = false;

	public static PossibleAssignment createNew(final String assignment// , final String mentionToken
	) {
		// return new PossibleAssignment(new Resource(assignment, false).toN3(),
		// mentionToken);
		return new PossibleAssignment(assignment// , mentionToken
		);
	}

	public PossibleAssignment(final String assignment// , final String mentionToken
	) {
		this(assignment, // mentionToken,
				defaultScore);
	}

	public PossibleAssignment(final String assignment// , final String mentionToken
			, final Number score) {
		this.assignment = assignment;
		// this.mentionToken = mentionToken;
		this.score = score;
		if (this.score.floatValue() != defaultScore) {
			this.computedScore = true;
		}
	}

	public PossibleAssignment(final PossibleAssignment toCopy) {
		this.assignment = toCopy.assignment;
		this.score = toCopy.score;
		// Likely not really used but myah
		this.computedScore = toCopy.computedScore;
		this.warned = toCopy.warned;
	}

	@Override
	public int compareTo(final PossibleAssignment o) {
		return Double.compare(this.score.doubleValue(), o.score.doubleValue());
	}

	public String getAssignment() {
		return assignment;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PossibleAssignment) {
			@SuppressWarnings("rawtypes")
			final PossibleAssignment ass = ((PossibleAssignment) obj);
			return this.score.equals(ass.score) && this.assignment.equals(ass.assignment)
			// && this.mentionToken.equals(ass.mentionToken)
			;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return 23 * (this.assignment.hashCode() + this.score.hashCode() // + this.mentionToken.hashCode()
		);
	}

	@Override
	public String toString() {
		return getAssignment() + " / " + this.score;
	}

	public Number getScore() {
		if (!computedScore && !warned) {
			// Warns only once per possible assignment
			logger.warn("Score has not yet been computed.");
			warned = true;
		}
		return this.score;
	}

	// public String getMentionToken() {
	// return mentionToken;
	// }

	public void setScore(final Number score) {
		this.score = score;
		computedScore = true;
	}
}
