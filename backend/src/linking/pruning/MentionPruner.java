package linking.pruning;

import java.util.Collection;
import java.util.List;

import structure.datatypes.Mention;

public interface MentionPruner {
	public List<Mention> prune(final Collection<Mention> mentions);
}
