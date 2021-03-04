package structure.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.QuerySolution;

public class QuerySolutionIterator implements Iterator<String> {
	private final List<String> varNames = Lists.newArrayList();
	private int index = 0;
	private final int varNamesSize;

	public QuerySolutionIterator(QuerySolution result) {
		final Iterator<String> it = result.varNames();
		while (it.hasNext())
		{
			varNames.add(it.next());
		}
		//result.varNames().forEachRemaining(s -> varNames.add(s));
		Collections.sort(varNames, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				// Idea: sX -> pX -> oX
				// negative -> o1 is smaller than o2

				// Putting non-predicate/-object variables first
				if (!o1.startsWith("s") && !o1.startsWith("p") && !o1.startsWith("o")) {
					// it's not a predicate nor an object, so it's likely the entity
					//return 1;
					return o1.compareTo(o2);
				}

				if (!o2.startsWith("s") && !o2.startsWith("p") && !o2.startsWith("o")) {
					// it's not a predicate nor an object, so it's likely the entity
					//return -1;
					return o1.compareTo(o2);
				}

				// both of them are either s, p or o
				int commonIndex = 0;
				for (; commonIndex < Math.max(o1.length(), o2.length()); ++commonIndex) {
					if (o1.charAt(commonIndex) != o2.charAt(commonIndex)) {
						break;
					}
				}

				if (commonIndex != 0) {
					// if (o1.charAt(0) == o2.charAt(0)) {
					// starts with same, so compare just the numbers
					// return o1.substring(1).compareTo(o2.substring(1));
					return o1.substring(commonIndex).compareTo(o2.substring(commonIndex));
				}

				// One is smaller than the other
				if (o1.length() != o2.length()) {
					// Shorter one should be first
					return o1.length() - o2.length();
				}

				// If they're both the first one with just length 1
				if (o1.length() == 1 && o2.length() == 1) {
					// We want p before o
					return o2.compareTo(o1);
				}

				final String numbers1 = o1.substring(1), numbers2 = o2.substring(1);
				int numberComparison = numbers1.compareTo(numbers2);
				if (numberComparison == 0) {
					// Means that we're in the case of
					// p123 vs o123 so we invert the natural order
					return o2.compareTo(o1);
				} else {
					return numberComparison;
				}
			}
		});
		this.varNamesSize = varNames.size();
	}

	@Override
	public boolean hasNext() {
		return index < varNamesSize;
	}

	@Override
	public String next() {
		return varNames.get(index++);
	}

}
