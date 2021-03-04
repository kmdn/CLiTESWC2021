package launcher.debug;

import org.json.JSONObject;

import api.JSONAPIAnnotator;
import structure.config.kg.EnumModelType;

public class LauncherJSONTest {

	public static void main(String[] args) {
		new LauncherJSONTest().run();
	}

	private void run() {
		final JSONObject jsonObj = new JSONObject(
				"{\"topk\":false,\"input\":\"\",\"kg\":\"DBP\",\"fuzzy\":false,\"mentiondetection\":false}");
		final EnumModelType KG = EnumModelType.DEFAULT;//DBPEDIA_FULL;
		final JSONAPIAnnotator annotator = new JSONAPIAnnotator(KG);
		annotator.init();

		String ret = annotator.annotateDocument(jsonObj);
		System.out.println(ret);
	}

}
