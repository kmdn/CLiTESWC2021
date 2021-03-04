package linking.disambiguation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.PostScorer;
import structure.interfaces.Scorer;

public abstract class AbstractMultiDisambiguator extends AbstractDisambiguator implements MultiDisambiguator {
	private final HashSet<Mention> context = new HashSet<>();

	private final Set<Scorer<PossibleAssignment>> scorers = new HashSet<>();
	private final Set<PostScorer<PossibleAssignment, Mention>> postScorers = new HashSet<>();
	// Determines how everything is scored!
	private final ScoreCombiner<PossibleAssignment> combiner = new ScoreCombiner<PossibleAssignment>();

	public AbstractMultiDisambiguator() {
		for (PostScorer postScorer : getPostScorers()) {
			// Links a context object which will be updated when necessary through
			// updateContext(Collection<Mention<N>>)
			postScorer.linkContext(context);
		}

	}

	/**
	 * Adds a scorer for disambiguation
	 * 
	 * @param scorer
	 */
	public void addScorer(final Scorer<PossibleAssignment> scorer) {
		scorers.add(scorer);
	}

	public void addPostScorer(final PostScorer<PossibleAssignment, Mention> scorer) {
		postScorers.add(scorer);
	}

	public Set<Scorer<PossibleAssignment>> getScorers() {
		return scorers;
	}

	public Set<PostScorer<PossibleAssignment, Mention>> getPostScorers() {
		return postScorers;
	}

	public ScoreCombiner<PossibleAssignment> getScoreCombiner() {
		return combiner;
	}

	/**
	 * Updates the context for post-scorers (required for proper functioning)
	 * 
	 * @param mentions
	 */
	public void updatePostContext(Collection<Mention> mentions) {
		clearContext();
		this.context.addAll(mentions);
		for (PostScorer postScorer : getPostScorers()) {
			postScorer.updateContext();
		}
	}

	protected void clearContext() {
		this.context.clear();
	}
}
