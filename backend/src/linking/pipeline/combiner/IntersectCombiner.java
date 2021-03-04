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
public class IntersectCombiner extends AbstractCombiner {
	final boolean ONLY_MD_MERGE = false;

	/**
	 * Merges the mentions and averages the assignment and detection scores.<br>
	 * Applies a logical INTERSECTION to the specific mode of execution:<br>
	 * MD: if more than 1 system mention it, we keep it<br>
	 * CG: If Candidate entity (=Possible Assignment) exists more than once, we keep
	 * it<br>
	 * ED: (Same as CG)<br>
	 * 
	 * 
	 * @param toMerge mentions for the same piece of text to merge
	 * @return
	 */
	public Collection<Mention> combine(final Collection<Collection<Mention>> multiItems) {
		final Collection<Mention> ret = Lists.newArrayList();
		final Map<String, List<Mention>> mapMentions = collectMentions(multiItems);

		// Intersect logic --> remove items that are only once in it
		mapMentions.entrySet().removeIf(e -> e.getValue().size() < 2);

		// Go through map merging mentions accordingly and adding to the return
		// collection
		if (ONLY_MD_MERGE) {
			// INTERSECT logic ONLY for mentions handled by removeIf,
			// so collect the wanted mentions and return them...
			for (Map.Entry<String, List<Mention>> e : mapMentions.entrySet()) {
				final Mention m = e.getValue().iterator().next();
				final Mention addMention = new Mention(m.getMention(), (PossibleAssignment) null, m.getOffset(),
						m.getDetectionConfidence(), m.getOriginalMention(), m.getOriginalWithoutStopwords());
				ret.add(addMention);
			}
			return ret;
		} else {
			// INTERSECT logic also for candidates...
			mapMentions.entrySet().forEach(e -> ret.add(intersectMerge(e.getValue())));
		}

		return ret;

	}

	private Mention intersectMerge(final Collection<Mention> toMerge) {
		// Just take the first mention as a means to extract template info from that
		// fits for all of them
		final Mention templateMention = toMerge.iterator().next();
		final Map<String, List<PossibleAssignment>> mapAssignments = collectAssignments(toMerge);

		// Intersect logic --> remove items that are only once in it
		mapAssignments.entrySet().removeIf(e -> e.getValue().size() < 2);

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
