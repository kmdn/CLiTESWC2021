package linking.candidategeneration;

import java.util.Collection;

import com.google.common.collect.Lists;

import structure.datatypes.Mention;
import structure.interfaces.pipeline.AnnotationPipelineItem;
import structure.interfaces.pipeline.CandidateGenerator;

public abstract class AbstractCandidateGenerator implements CandidateGenerator {
	@Override
	public Collection<Collection<Mention>> execute(AnnotationPipelineItem callItem, String text) {
		final Collection<AnnotationPipelineItem> dependencies = callItem.getDependencies();
		if (dependencies.size() == 1) {
			final Collection<Collection<Mention>> multiMentions = dependencies.iterator().next().getResults();
			if (multiMentions.size() != 1) {
				throw new RuntimeException(
						"Invalid number of arguments passed: multiMentions size[" + multiMentions.size() + "]");
			}

			final Collection<Mention> mentions = multiMentions.iterator().next();
			generate(mentions);
			final Collection<Collection<Mention>> retMentions = Lists.newArrayList();
			retMentions.add(mentions);
			return retMentions;
		} else {
			throw new IllegalArgumentException("Invalid number of dependencies for this component...");
		}

	}
}
