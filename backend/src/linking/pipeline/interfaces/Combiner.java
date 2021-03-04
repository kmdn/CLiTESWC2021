package linking.pipeline.interfaces;

import java.util.Collection;

import structure.datatypes.Mention;

public interface Combiner extends Subcomponent {
	public abstract Collection<Mention> combine(final Collection<Collection<Mention>> multiItems);
}
