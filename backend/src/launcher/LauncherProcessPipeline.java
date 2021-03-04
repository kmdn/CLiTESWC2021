package launcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;

import linking.candidategeneration.CandidateGeneratorMap;
import linking.disambiguation.linkers.BabelfyLinker;
import linking.mentiondetection.InputProcessor;
import linking.mentiondetection.exact.MentionDetectorMap;
import linking.pipeline.combiner.UnionCombiner;
import structure.datatypes.Mention;
import structure.interfaces.pipeline.AnnotationPipeline;
import structure.interfaces.pipeline.AnnotationPipelineItem;
import structure.interfaces.pipeline.Disambiguator;
import structure.interfaces.pipeline.MentionDetector;

public class LauncherProcessPipeline {

	public static void main(final String[] args) {
		new LauncherProcessPipeline().run();
	}

	public void run() {
		// Map: ID, MD instance
		final String text = "Toby is the best Spiderman by far";
//		final Map<String, PipelineQueueItem> collMD = new HashMap<>();
//		// Fill collMD with the appropriate instance of MD
//
//		final Map<String, CandidateGenerator> collCG = new HashMap<>();
//		final Map<String, Disambiguator> collED = new HashMap<>();
//		final Map<String, PipelineQueueItem> components = new HashMap<>();
//		final Map<String, Collection<Mention>> mapMentions = new HashMap<>();
//		// Start with MD detection of text
//		final Stack<PipelineQueueItem> processStack = new Stack<PipelineQueueItem>();
//
//		for (Entry<String, PipelineQueueItem> e : collMD.entrySet()) {
//			// final Collection<Mention> mentions = ((MentionDetector)
//			// (e.getValue().getComponent())).detect(text);
//			// final Collection<String> targets = e.getValue().getTargets();
//			// mapMentions.put(e.getKey(), mentions);
//			processStack.add(e.getValue());
//		}
//
//		while (processStack.size() > 0) {
//			final PipelineQueueItem component = processStack.pop();
//			final Collection<PipelineQueueItem> dependencies = component.getDependencies();
//			final Collection<PipelineQueueItem> items = Lists.newArrayList();
//			for (PipelineQueueItem dependency : dependencies) {
//				if (!dependency.isFinished()) {
//					items.add(dependency);
//				}
//			}
//			if (items.size() > 0) {
//				// re-add this pipeline item to the stack before the others, so it is executed
//				// after them
//				processStack.add(component);
//				for (PipelineQueueItem dependency : items) {
//					processStack.add(dependency);
//				}
//			} else {
//				// all dependencies are ready!
//				// final Collection<Mention> mentions = execute(component, mapMentions);
//				// add mention results so they can be taken by the next step
//			}
//		}

		// Root for dependency resolution
		// final AnnotationPipelineItem results = new AnnotationPipelineItem("results",
		// null);

		final Map<String, Collection<String>> mention1Map = new HashMap<>();
		final Map<String, Collection<String>> mention2Map = new HashMap<>();
		final Map<String, Collection<String>> mention3Map = new HashMap<>();
		final Map<String, Collection<String>> candidateMap = new HashMap<>();
		mention1Map.put("toby", Arrays.asList(new String[] { ":Toby_MacGuire!", ":Toby McTobison" }));
		mention2Map.put("spiderman", Arrays.asList(new String[] { ":Spiderman_3", ":Spiderpig" }));
		mention2Map.put("toby", Arrays.asList(new String[] { ":M2_Toby" }));
		mention3Map.put("spiderman", Arrays.asList(new String[] { ":Spiderpig" }));

		for (Map.Entry<String, Collection<String>> e : mention1Map.entrySet()) {
			candidateMap.put(e.getKey(), e.getValue());
		}

		for (Map.Entry<String, Collection<String>> e : mention2Map.entrySet()) {
			candidateMap.put(e.getKey(), e.getValue());
		}

		for (Map.Entry<String, Collection<String>> e : mention3Map.entrySet()) {
			candidateMap.put(e.getKey(), e.getValue());
		}

		final MentionDetector mentionDetect1 = new MentionDetectorMap(mention1Map,
				new InputProcessor(Lists.newArrayList()));
		final MentionDetector mentionDetect2 = new MentionDetectorMap(mention2Map,
				new InputProcessor(Lists.newArrayList()));
		final MentionDetector mentionDetect3 = new MentionDetectorMap(mention3Map,
				new InputProcessor(Lists.newArrayList()));

		final Disambiguator ed = new Disambiguator() {

			@Override
			public Collection<Collection<Mention>> execute(AnnotationPipelineItem callItem, String text) {
				Collection<AnnotationPipelineItem> dependencies = callItem.getDependencies();
				if (dependencies.size() == 1) {
					final AnnotationPipelineItem prev = dependencies.iterator().next();
					final Collection<Collection<Mention>> retMentions = Lists.newArrayList();
					final Collection<Mention> mentions = prev.getResults().iterator().next();
					try {
						disambiguate(text, mentions);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					retMentions.add(mentions);
					return retMentions;
				} else {
					return null;
				}
			}

			@Override
			public Collection<Mention> disambiguate(String inputText, Collection<Mention> mentions) throws InterruptedException {
				return new BabelfyLinker().generateDisambiguate(null, mentions);
			}
		};

		final AnnotationPipeline pipeline = new AnnotationPipeline();
		pipeline.addMD("MD1", mentionDetect1);
		pipeline.addMD("MD2", mentionDetect2);
		pipeline.addCombiner("CO1", new UnionCombiner());
		pipeline.addCG("CG1", new CandidateGeneratorMap(candidateMap));
		pipeline.addMD("MD3", mentionDetect3);
		// pipeline.addItem("SP1", new CopySplitter());
		pipeline.addED("ED1", ed);
		pipeline.addED("ED2", ed);

		pipeline.addConnection("MD1", "CO1");
		pipeline.addConnection("MD2", "CO1");
		pipeline.addConnection("CO1", "CG1");
		pipeline.addConnection("CG1", "ED1");
		pipeline.addConnection("MD1", "ED2");

		annotate(pipeline, text);
		annotate(pipeline, "Steve jobs is a dude");
		annotate(pipeline, text);

	}

	private void annotate(final AnnotationPipeline pipeline, final String text) {
		pipeline.execute(text);
		System.out.println("Results");
		System.out.println(pipeline.getResults(text));
		pipeline.reset();

	}

}
