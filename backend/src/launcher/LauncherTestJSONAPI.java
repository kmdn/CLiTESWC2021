package launcher;

import org.json.JSONObject;

import api.JSONAPIAnnotator;

public class LauncherTestJSONAPI {

	public static void main(String[] args) {
		final JSONAPIAnnotator annotator = new JSONAPIAnnotator();// EnumModelType.DBPEDIA_FULL);
		annotator.init();
		final String input = ", input: 'steve jobs and joan baez were a couple'";
		final String inString = "{kg: 'wd', mentiondetection: true" + input + ", topk: true }";
		final JSONObject jsonObj = new JSONObject(inString);

		System.out.println(annotator.annotateDocument(jsonObj));
	}
}
