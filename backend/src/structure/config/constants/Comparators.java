package structure.config.constants;

import java.util.Comparator;

import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Span;
import org.apache.commons.lang3.tuple.Pair;

import structure.datatypes.Mention;

/**
 * Class containing static comparator instances for various types
 * 
 * @author Kristian Noullet
 *
 */
public class Comparators {
	// Mentions comparator based on their offsets, with additional logic on the
	// start offset
	public static final Comparator<Mention> mentionOffsetComp = new Comparator<Mention>() {
		@Override
		public int compare(Mention o1, Mention o2) {
			// Made so it accepts the smallest match as the used one
			final int diffLength = (o1.getOriginalMention().length() - o2.getOriginalMention().length());
			return (o1.getOffset() == o2.getOffset()) ? diffLength : ((o1.getOffset() > o2.getOffset()) ? 1 : -1);
		}
	};

	// Markings comparators based on their offsets
	public static final Comparator<Marking> markingsOffsetComp = new Comparator<Marking>() {

		@Override
		public int compare(Marking o1, Marking o2) {
			Span spanLeft = null, spanRight = null;
			if (o1 instanceof Span) {
				spanLeft = (Span) o1;
			}

			if (o2 instanceof Span) {
				spanRight = (Span) o2;
			}

			if (spanLeft == null || spanRight == null) {
				return 0;
			}

			return spanLeft.getStartPosition() - spanRight.getStartPosition();
		}
	};

	// Pair comparator based on the right element
	public static final Comparator<Pair<? extends Comparable, ? extends Comparable>> pairRightComparator = new Comparator<Pair<? extends Comparable, ? extends Comparable>>() {

		@Override
		public int compare(Pair<? extends Comparable, ? extends Comparable> o1,
				Pair<? extends Comparable, ? extends Comparable> o2) {
			return o1.getRight().compareTo(o2.getRight());
		}

	};

	// Pair comparator based on the left element
	public static final Comparator<Pair<? extends Comparable, ? extends Comparable>> pairLeftComparator = new Comparator<Pair<? extends Comparable, ? extends Comparable>>() {

		@Override
		public int compare(Pair<? extends Comparable, ? extends Comparable> o1,
				Pair<? extends Comparable, ? extends Comparable> o2) {
			return o1.getLeft().compareTo(o2.getLeft());
		}

	};

	// Simple offset-based comparator for mentions
	public static final Comparator<Mention> mentionOffsetComparator = new Comparator<Mention>() {
		@Override
		public int compare(Mention o1, Mention o2) {
			return o1.getOffset() - o2.getOffset();
		}
	};

}
