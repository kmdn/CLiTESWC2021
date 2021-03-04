package linking.pipeline.interfaces;

import java.util.Collection;

import structure.datatypes.Mention;

public interface Splitter extends Subcomponent {
	public abstract Collection<Collection<Mention>> split(Collection<Mention> mentionsToSplit);

	public abstract Collection<Collection<Mention>> split(Collection<Mention> mentionsToSplit, final int copies);

	/**
	 * Splits passed mentions into various copies, allowing for further processing.
	 * We recommend using a Filter or Translator subcomponent after a splitter if
	 * more complex operations for a specific component should be executed
	 * 
	 * @param mentionsToSplit
	 * @param copies          how many copies to manufacture
	 * @param params          parameters which may be utilised for
	 * @return split mentions
	 */
	public abstract Collection<Collection<Mention>> split(Collection<Mention> mentionsToSplit, final int copies,
			final String[] params);
}
