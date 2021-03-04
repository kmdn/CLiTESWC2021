package linking.pipeline.splitter;

import java.util.Collection;

import com.google.common.collect.Lists;

import structure.datatypes.Mention;

public class CopySplitter extends AbstractSplitter {

	public Collection<Collection<Mention>> split(Collection<Mention> mentionsToSplit) {
		return split(mentionsToSplit, 2);
	}

	public Collection<Collection<Mention>> split(Collection<Mention> mentionsToSplit, final int copies) {
		return split(mentionsToSplit, copies, null);
	}

	/**
	 * Splits passed mentions into copies
	 * 
	 * @param mentionsToSplit
	 * @param copies          how many copies to manufacture
	 * @param params          parameters which may be utilised for splitting
	 * @return split mentions
	 */
	public Collection<Collection<Mention>> split(Collection<Mention> mentionsToSplit, final int copies,
			final String[] params) {
		final Collection<Collection<Mention>> ret = Lists.newArrayList();
		if (params != null && params.length != 0) {
			// process parameters ...
		} else if (copies > 0) {
			for (int i = 0; i < copies; ++i) {
				// Copy the input and add to output
				final Collection<Mention> copy = Lists.newArrayList();
				for (Mention m : mentionsToSplit) {
					copy.add(new Mention(m));
				}
				ret.add(copy);
			}
		}
		return ret;
	}
}
