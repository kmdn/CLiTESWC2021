package linking.disambiguation.consolidation;

import structure.datatypes.Mention;

public interface Mergeable<M> {
	public Mention merge(M toMerge);
}
