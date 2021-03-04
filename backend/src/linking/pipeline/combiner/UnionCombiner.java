package linking.pipeline.combiner;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;

/**
 * Does a union on elements passed, adding more and more.</br>
 * May be used to combine results from multiple: MD: mentions --> add new
 * mentions CG: candidate entities --> add new candidates (if ED: dis. entities
 * --> add
 * 
 * @author wf7467
 *
 */
public class UnionCombiner extends AbstractCombiner {

	public Collection<Mention> combine(final Collection<Collection<Mention>> multiItems) {
		final Collection<Mention> ret = Lists.newArrayList();
		final Map<String, List<Mention>> mapMentions = collectMentions(multiItems);
		// Go through map merging mentions accordingly and adding to the return
		// collection
		mapMentions.entrySet().forEach(e -> ret.add(additiveMerge(e.getValue())));

		return ret;

	}

	/**
	 * Merges the mentions and averages the assignment and detection scores
	 * 
	 * @param toMerge mentions for the same piece of text to merge
	 * @return
	 */
	private Mention additiveMerge(final Collection<Mention> toMerge) {
		// Just take the first mention as a means to extract template info from that
		// fits for all of them
		final Mention templateMention = toMerge.iterator().next();
		final Map<String, List<PossibleAssignment>> mapAssignments = collectAssignments(toMerge);

		// Merge assignments together
		final List<PossibleAssignment> mergedAssignments = Lists.newArrayList();
		for (final Entry<String, List<PossibleAssignment>> e : mapAssignments.entrySet()) {
			final List<PossibleAssignment> assignments = e.getValue();

			// Get the first's entity (should be the same for them all)
			final String entity = assignments.get(0).getAssignment();

			// Create a new possible assignment
			final PossibleAssignment retAss = new PossibleAssignment(entity, 0.0f);

			// Sum it
			for (PossibleAssignment ass : assignments) {
				retAss.setScore(retAss.getScore().doubleValue() + ass.getScore().doubleValue());
			}
			// Average it
			retAss.setScore(retAss.getScore().doubleValue() / ((double) (assignments.size())));
			mergedAssignments.add(retAss);
		}

		// Create a new mention
		final Mention retMention = new Mention(templateMention.getMention(), mergedAssignments,
				templateMention.getOffset(), templateMention.getDetectionConfidence(),
				templateMention.getOriginalMention(), templateMention.getOriginalWithoutStopwords());
		return retMention;
	}
}
