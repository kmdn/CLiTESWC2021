package linking.candidategeneration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;

import structure.datatypes.Mention;
import structure.datatypes.PossibleAssignment;
import structure.utils.Loggable;

/**
 * A simple dictionary/lookup-table-like implementation of candidate generation
 * 
 * @author Kristian Noullet
 *
 */
public class CandidateGeneratorMap extends AbstractCandidateGenerator implements Loggable {
	private final Map<String, Collection<String>> linking;
	private final boolean throwException = true;

	public CandidateGeneratorMap(Map<String, Collection<String>> linking) {
		this.linking = linking;
	}

	/**
	 * Generates all possible assignments for given mention
	 * 
	 * @param mention Mention for which to generate candidates!
	 * @return set of possible assignments
	 */
	@Override
	public List<PossibleAssignment> generate(Mention mention) {
		final Collection<String> possibleEntities = this.linking.get(mention.getMention());
		if (possibleEntities == null) {
			getLogger().error("Could not find any such mention(" + mention.getMention() + ") o.o");
			if (throwException) {
				throw new RuntimeException("Could not find a mention(" + mention.getMention()
						+ ") although it apparently was detected...");
			} else {
				return null;
			}
		}
		final List<PossibleAssignment> ret = Lists.newArrayList();
		for (String entity : possibleEntities) {
			ret.add(PossibleAssignment.createNew(entity// , mention.getMention()
			));
		}

		return ret;
	}

	@Override
	public void generate(Collection<Mention> mentions) {
		for (Mention m : mentions) {
			// Update possible assignments
			m.updatePossibleAssignments(generate(m));
		}
	}
}
