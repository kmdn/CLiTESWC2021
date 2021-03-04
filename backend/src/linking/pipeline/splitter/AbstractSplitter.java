package linking.pipeline.splitter;

import java.util.Collection;

import linking.pipeline.interfaces.Splitter;
import structure.datatypes.Mention;
import structure.interfaces.pipeline.AnnotationPipelineItem;

public abstract class AbstractSplitter implements Splitter {

	@Override
	public Collection<Collection<Mention>> execute(final AnnotationPipelineItem callItem, String text) {
		final Collection<AnnotationPipelineItem> dependencies = callItem.getDependencies();
		if (dependencies.size() == 1) {
			final Collection<Collection<Mention>> multiMentions = dependencies.iterator().next().getResults();
			if (multiMentions.size() != 1) {
				throw new RuntimeException(
						"Invalid number of arguments passed: multiMentions size[" + multiMentions.size() + "]");
			}

			final Collection<Mention> toSplit = multiMentions.iterator().next();
			final int copies = callItem.getTargets().size();
			return split(toSplit, copies);
		} else {
			throw new IllegalArgumentException("Invalid number of dependencies for this component...");
		}
	}

}
