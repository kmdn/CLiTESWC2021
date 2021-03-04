package launcher;

import java.io.IOException;

import linking.disambiguation.linkers.AgnosLinker;
import structure.config.kg.EnumModelType;
import structure.linker.Linker;

public class LauncherLinking {

	public static void main(String[] args) {
		try {
			new LauncherLinking().run();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void run() throws Exception {
		final EnumModelType KG = EnumModelType.//
		// WIKIDATA//
		// DBPEDIA_FULL//
				DEFAULT//
		;
		final Linker linker = new AgnosLinker(KG);
		linker.init();
		final String input = "hello world";
		linker.annotateMentions(input);
	}
}