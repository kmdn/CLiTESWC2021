package install.surfaceform.query.general;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import structure.config.constants.FilePaths;
import structure.config.constants.Strings;
import structure.config.kg.EnumModelType;

public class SFQuery extends LiteralEntityQuery {
	private static final String newline = Strings.NEWLINE.val;
	/*
	 * @param entityVarName entity's query variable name
	 * 
	 * @param surfaceFormVarName surface form's query variable name
	 * 
	 * @param delimLinking delimiter for value of each entry (for outputting
	 * Entity/SF linking data)
	 * 
	 * @param delimQueryResults delimiter for value of each query result entry
	 */
	private static final String delimLinking = Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val;
	private static final String delimQueryResults = Strings.QUERY_RESULT_DELIMITER.val;
	private static final List<String> entityVarNames = Arrays
			.asList(new String[] { "entity", "s", "subject", "a", "asubject", "asubj", "a_subject", "sub", "subj" });
	private static final List<String> surfaceFormVarNames = Arrays
			.asList(new String[] { "lit", "o", "object", "b", "bobject", "bobj", "b_object", "ob", "obj" });

	public SFQuery(EnumModelType KG) {
		super(KG);
	}

	@Override
	protected BufferedWriter initAlternateChannelWriter() throws IOException {
		return new BufferedWriter(new FileWriter(FilePaths.FILE_ENTITY_SURFACEFORM_LINKING.getPath(KG), false));
	}

	@Override
	protected String getQueryInputDir() {
		return FilePaths.DIR_QUERY_IN_SURFACEFORM.getPath(KG);
	}

	@Override
	protected String getQueryOutDir() {
		return FilePaths.DIR_QUERY_OUT_SURFACEFORM.getPath(KG);
	}

	@Override
	protected void outputMainChannel(String varName, String value, boolean hasNext, BufferedWriter writer)
			throws IOException {
		final String dynamicDelimQueryResults = (hasNext ? delimQueryResults : newline);
		writer.write(value + dynamicDelimQueryResults);
	}

	@Override
	protected void outputAlternateChannels(String varName, String value, boolean hasNext, List<BufferedWriter> writers)
			throws IOException {
		varName = varName.toLowerCase();
//		if ((entityVarNames.contains(varName)) || surfaceFormVarNames.contains(varName)) {
//			final String dynamicDelimLinking = ((hasNext && entityVarNames.contains(varName)) ? delimLinking : newline);
//			writers.get(0).write(value + dynamicDelimLinking);
//		}

		final String dynamicDelimLinking = (hasNext ? delimLinking : newline);
		writers.get(0).write(value + dynamicDelimLinking);

	}
}
