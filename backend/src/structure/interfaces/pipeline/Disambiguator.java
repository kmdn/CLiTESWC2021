package structure.interfaces.pipeline;

import java.util.Collection;

import structure.datatypes.Mention;

public interface Disambiguator extends PipelineComponent {
	public Collection<Mention> disambiguate(final String disambiguate, final Collection<Mention> mentions) throws InterruptedException;
}