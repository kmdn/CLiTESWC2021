package linking.disambiguation.consolidation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;

public class MergeableMention extends Mention {
	private Map<PossibleAssignment, MergeablePossibleAssignment> possibleAssignments = new HashMap<>();

	public MergeableMention(String word, PossibleAssignment assignment, int offset) {
		super(word, assignment, offset);
	}

	public MergeableMention(String word, PossibleAssignment assignment, int offset, double detectionConfidence,
			String originalMention, String originalWithoutStopwords) {
		super(word, assignment, offset, detectionConfidence, originalMention, originalWithoutStopwords);
	}

	/**
	 * Copies a given mention (replacing its equals(Object) method for set storage
	 * adaptation), creating copies of assignments as well
	 * 
	 * @param m mention to copy
	 * @return a copy of passed mention and possible assignments each w/ a modified
	 *         equals(Object) method
	 */
	public MergeableMention(Mention m) {
		this(m.getMention(), m.getAssignment(), m.getOffset(), m.getDetectionConfidence(), m.getOriginalMention(),
				m.getOriginalWithoutStopwords());
		updatePossibleAssignments(m.getPossibleAssignments());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mention || obj instanceof MergeableMention) {
			final Mention mentionObj = (Mention) obj;
			return getMention().equals(mentionObj.getMention()) && (getOffset() == mentionObj.getOffset())
					&& (((getAssignment() == null && mentionObj.getAssignment() == null)
							|| (getAssignment() == mentionObj.getAssignment()))
							&& getAssignment().equals(mentionObj.getAssignment()));
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		// If all constituents are null, make it a weird sum, so there is no collision
		// with anything else
		return ((this.getMention() == null) ? 4 : (this.getMention().hashCode())) + getOffset();
	}

	@Override
	public void updatePossibleAssignments(Collection<PossibleAssignment> possibleAssignments) {
		for (PossibleAssignment possAss : possibleAssignments) {
			final MergeablePossibleAssignment copyAssignment = new MergeablePossibleAssignment(possAss.getAssignment()
					//,possAss.getMentionToken()
					);
			copyAssignment.setScore(possAss.getScore());
			this.possibleAssignments.put(copyAssignment, copyAssignment);
		}
	}

	@Override
	public Collection<PossibleAssignment> getPossibleAssignments() {
		return this.possibleAssignments.keySet();
	}

	public PossibleAssignment findAssignment(final PossibleAssignment assignment) {
		return this.possibleAssignments.get(assignment);
	}

}
