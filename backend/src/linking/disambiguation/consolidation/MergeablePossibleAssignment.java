package linking.disambiguation.consolidation;

import structure.datatypes.PossibleAssignment;

public class MergeablePossibleAssignment extends PossibleAssignment {

	public MergeablePossibleAssignment(String assignment//, String mentionToken
			) {
		super(assignment//, mentionToken
				);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PossibleAssignment || obj instanceof MergeablePossibleAssignment) {
			final PossibleAssignment ass = ((PossibleAssignment) obj);
			return getAssignment().equals(ass.getAssignment()) //&& getMentionToken().equals(ass.getMentionToken())
					;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return 23 * (getAssignment().hashCode() //+ getMentionToken().hashCode()
				);
	}

}
