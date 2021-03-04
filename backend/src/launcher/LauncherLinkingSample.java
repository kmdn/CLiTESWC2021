package launcher;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import linking.candidategeneration.CandidateGeneratorMap;
import linking.disambiguation.DisambiguatorAgnos;
import linking.mentiondetection.InputProcessor;
import linking.mentiondetection.exact.MentionDetectorMap;
import structure.config.kg.EnumModelType;
import structure.datatypes.Mention;
import structure.interfaces.pipeline.CandidateGenerator;
import structure.interfaces.pipeline.MentionDetector;
import structure.utils.MentionUtils;

public class LauncherLinkingSample {

	public static void main(String[] args) {
		try {
			new LauncherLinkingSample().run();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void run() throws InterruptedException, IOException {
		final EnumModelType KG = EnumModelType.DEFAULT;//DBPEDIA_FULL;
		final String input = "";
		final HashMap<String, Collection<String>> surfaceFormLinks = new HashMap<>();
		final MentionDetector md = new MentionDetectorMap(surfaceFormLinks, new InputProcessor(null));
		final CandidateGenerator cg = new CandidateGeneratorMap(surfaceFormLinks);
		final DisambiguatorAgnos d = new DisambiguatorAgnos(KG);
		final Collection<Mention> mentions = md.detect(input);
		cg.generate(mentions);
		d.disambiguate(input, mentions);
		MentionUtils.displayMentions(mentions);
	}
}
