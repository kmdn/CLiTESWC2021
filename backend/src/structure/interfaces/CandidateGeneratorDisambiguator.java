package structure.interfaces;

import java.util.Collection;

import structure.datatypes.Mention;
import structure.interfaces.pipeline.PipelineComponent;

public interface CandidateGeneratorDisambiguator extends PipelineComponent {

	public boolean init();

	public Collection<Mention> generateDisambiguate(String inputText, Collection<Mention> mentions);
}
