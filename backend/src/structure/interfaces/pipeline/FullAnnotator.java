package structure.interfaces.pipeline;

import java.util.Collection;

import structure.datatypes.Mention;

public interface FullAnnotator extends PipelineComponent {

	public Collection<Mention> generateDisambiguate(String inputText, Collection<Mention> mentions);

}
