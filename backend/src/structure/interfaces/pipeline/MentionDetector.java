package structure.interfaces.pipeline;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import structure.datatypes.Mention;
import structure.utils.MentionUtils;

public interface MentionDetector extends PipelineComponent {
	public boolean init() throws Exception;

	public Collection<Mention> detect(final String input);

	public Collection<Mention> detect(String input, String source);

	/**
	 * Makes a copy of the passed mentions, iterates through them and upon finding a
	 * colliding mentions, it adds the longer original mention to a set to be
	 * removed
	 * 
	 * @param mentions
	 * @return
	 */
	default List<Mention> removeLongerCollisions(Collection<Mention> mentions, final boolean removeStartOffset,
			final boolean removeEndOffset) {
		final List<Mention> copyMentions = Lists.newArrayList(mentions);
		final Set<String> toRemove = new HashSet<>();
		for (int i = 0; i < copyMentions.size(); ++i) {
			final Mention outerMention = copyMentions.get(i);
			for (int j = i + 1; j < copyMentions.size(); ++j) {
				final Mention innerMention = copyMentions.get(j);
				final boolean sameSF = outerMention.getMention().equals(innerMention.getMention());
				final boolean sameStartOffset = (outerMention.getOffset() == innerMention.getOffset());
				final boolean sameEndOffset = //
						(outerMention.getOffset() + outerMention.getOriginalMention().length()) //
								== //
								(innerMention.getOffset() + innerMention.getOriginalMention().length())//
				;
				// i!=j should always be the case, but added it in case the logic changes
				if (i != j && sameSF && !outerMention.getOriginalMention().equals(innerMention.getOriginalMention())) {
					// Same linked mention, but not the same original text

					if (false //
							|| (removeStartOffset && sameStartOffset) || (removeEndOffset && sameEndOffset)) {
						// starts/ends on the same spot
						if (outerMention.getOriginalMention().length() > innerMention.getOriginalMention().length()) {
							// Remove the longer one
							toRemove.add(outerMention.getOriginalMention());
						} else {
							toRemove.add(innerMention.getOriginalMention());
						}
					}
				}
			}
		}

		// After having gathered the right information, get to it!
		MentionUtils.removeStringMentionOriginal(toRemove, copyMentions);
		return copyMentions;
	}

}
