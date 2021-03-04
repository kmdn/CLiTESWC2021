package launcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import linking.disambiguation.consolidation.SumConsolidator;
import linking.disambiguation.linkers.BabelfyLinker;
import linking.disambiguation.linkers.DBpediaSpotlightLinker;
import linking.disambiguation.linkers.OpenTapiocaLinker;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.linker.Linker;
import structure.utils.MentionUtils;

public class LauncherConsolidateLinking {

	public static void main(String[] args) {
		final String input = "Steve Jobs and Joan Baez are famous people";
		try {
			consolidateTest();
			// singleOpenTapioca(input);
			// singleDBpedia();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void singleOpenTapioca(final String input) throws Exception {
		final Linker linker = new OpenTapiocaLinker();
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");
		// System.out.println("Result:" + linker.annotate(input));
		final Collection<Mention> ret = linker.annotateMentions(input);

		MentionUtils.displayMentions(ret);
		System.out.println("Res: " + ret);
	}

	private static void singleDBpedia() throws IOException {
		final DBpediaSpotlightLinker linker = new DBpediaSpotlightLinker();
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");
		final Collection<Mention> ret = linker.annotateMentions("Steve Jobs and Joan Baez are famous people");

		MentionUtils.displayMentions(ret);
		System.out.println("Res: " + ret);
	}

	private static void consolidateTest() throws Exception {
		final String input = "Steve Jobs and Joan Baez are famous people";

		final List<Linker> linkers = new ArrayList<>();
		final DBpediaSpotlightLinker linker1 = new DBpediaSpotlightLinker();
		linker1.confidence(0.0f);
//		final Linker linker1 = new OpenTapiocaLinker();
		final Linker linker2 = new OpenTapiocaLinker();
		final Linker linker3 = new BabelfyLinker(EnumModelType.DBPEDIA_FULL);
		
		//linkers.add(linker1);
		linkers.add(linker2);
//		linkers.add(linker3);
		// final String ret = linker.annotate("Steve Jobs and Joan Baez are famous
		// people");

		final boolean ALL_OR_KG = true;
		// final Collection<Mention> ret = linker.annotateMentions(input);

		final SumConsolidator consolidator = new SumConsolidator(linkers.toArray(new Linker[] {}));
		Map<Linker, Collection<Mention>> linkerResults;
		try {
			linkerResults = consolidator.executeLinkers(input);

			final boolean output = true;

			if (output) {
				// Output annotations for each linker
				for (Entry<Linker, Collection<Mention>> e : linkerResults.entrySet()) {
					System.out.print("Linker[" + e.getKey().getClass() + "]:");
					System.out.println(e.getValue());
				}
				// results one by one
				System.out.println("Linker Count:" + linkerResults.size());
			}
			// results in map
			// System.out.println("Linker results:" + linkerResults);

			// Merge annotations by KG
			final Map<String, Collection<Mention>> results;

			if (ALL_OR_KG) {
				final Map<String, Collection<Mention>> tmpResults = new HashMap<>();
				final Collection<Mention> tmp = consolidator.mergeAll(linkerResults);
				tmpResults.put("", tmp);
				results = tmpResults;
			} else {
				results = consolidator.mergeByKG(linkerResults);
			}

			// Merged results
			System.out.println("Results: " + results);

			// Display consolidated results
			for (Entry<String, Collection<Mention>> e : results.entrySet()) {
				final Collection<Mention> ret = e.getValue();
				MentionUtils.displayMentions(ret);
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} finally {
		}
	}
}
