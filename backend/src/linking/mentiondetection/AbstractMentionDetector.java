package linking.mentiondetection;

import java.util.Collection;

import com.google.common.collect.Lists;

import structure.datatypes.Mention;
import structure.interfaces.pipeline.AnnotationPipelineItem;
import structure.interfaces.pipeline.MentionDetector;

public abstract class AbstractMentionDetector implements MentionDetector {

	@Override
	public Collection<Collection<Mention>> execute(final AnnotationPipelineItem callItem,
			final String text) {
		// Takes first parameter
		final Collection<Mention> mentions = detect(text);
		final Collection<Collection<Mention>> retMentions = Lists.newArrayList();
		retMentions.add(mentions);
		return retMentions;
	}
}