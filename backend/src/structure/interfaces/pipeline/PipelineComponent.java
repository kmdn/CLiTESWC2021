package structure.interfaces.pipeline;

import java.util.Collection;

import structure.datatypes.Mention;

public interface PipelineComponent {

	public Collection<Collection<Mention>> execute(final AnnotationPipelineItem callItem,
			String text);

}
