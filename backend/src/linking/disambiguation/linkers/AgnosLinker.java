package linking.disambiguation.linkers;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

import linking.candidategeneration.CandidateGeneratorMap;
import linking.disambiguation.DisambiguatorAgnos;
import linking.mentiondetection.InputProcessor;
import linking.mentiondetection.exact.MentionDetectorMap;
import structure.config.constants.EnumEmbeddingMode;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.linker.AbstractLinkerLocal;
import structure.utils.DetectionUtils;
import structure.utils.Loggable;
import structure.utils.MentionUtils;
import structure.utils.Stopwatch;

public class AgnosLinker extends AbstractLinkerLocal implements Loggable {

	public AgnosLinker(final EnumModelType KG) {
		super(KG);
	}

	@Override
	public boolean init() {
		try {
			System.out.println("Loading structures...");
			final Map<String, Collection<String>> surfaceFormLinks = getMentions(KG);
			// Initialize Mention Detection w/ possible mentions
			setMentionDetection(new MentionDetectorMap(surfaceFormLinks, new InputProcessor(null)));
			// Initialize Candidate Generation w/ surface forms and candidates
			setCandidateGeneration(new CandidateGeneratorMap(surfaceFormLinks));
			// Initialize Disambiguator w/ according algorithms
			setDisambiguator(new DisambiguatorAgnos(this.KG, EnumEmbeddingMode.LOCAL));
			System.out.println("Finished loading structures - starting process");
			return true;
		} catch (RuntimeException | IOException re) {
			getLogger().error("Failed initializing structures");
			return false;
		}
	}

	@Override
	public String annotate(String input) {
		throw new RuntimeException("Use #annotateMentions(String) instead");
	}

	@Override
	public Collection<Mention> annotateMentions(String input) {
		try {
			System.out.println("Computing for :" + KG.name());
			Stopwatch.start(getClass().getName());
			final Collection<Mention> mentions = md.detect(input);
			System.out.println("Finished MD - starting CG");
			cg.generate(mentions);
			System.out.println("Finished CG - starting Disambiguation");
			d.disambiguate(input, mentions);
			System.out.println("Finished Disambiguation - starting displaying...");
			System.out.println("Total Process Duration:" + Stopwatch.endDiffStart(getClass().getName()) + " ms.");
			MentionUtils.displayMentions(mentions);
			return mentions;
		} catch (InterruptedException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}

	private Map<String, Collection<String>> getMentions(final EnumModelType KG) throws IOException {
		// final MentionPossibilityLoader mpl = new MentionPossibilityLoader(KG);
		final Map<String, Collection<String>> tmpMap;
		// mpl.exec(new File(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG)));
		tmpMap = DetectionUtils.loadSurfaceForms(KG, null);
		return DetectionUtils.makeCaseInsensitive(tmpMap);
	}

	@Override
	public String getKG() {
		return this.KG.name();
	}

	@Override
	public Number getWeight() {
		return 1.0f;
	}

	@Override
	public BiFunction<Number, Mention, Number> getScoreModulationFunction() {
		return null;
	}
}
