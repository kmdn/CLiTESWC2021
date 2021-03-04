package install.surfaceform.query.general;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import structure.config.constants.FilePaths;
import structure.config.constants.Strings;
import structure.config.kg.EnumModelType;

public class HSFQuery extends LiteralEntityQuery {
	private static final String newline = Strings.NEWLINE.val;
	/*
	 * @param delimQueryResults delimiter for value of each query result entry
	 */
	private static final String delimQueryResults = Strings.QUERY_RESULT_DELIMITER.val;

	public HSFQuery(EnumModelType KG) {
		super(KG);
	}

	@Override
	protected BufferedWriter initAlternateChannelWriter() throws IOException {
		return null;
	}

	@Override
	protected String getQueryInputDir() {
		return FilePaths.DIR_QUERY_IN_HELPING_SURFACEFORM.getPath(KG);
	}

	@Override
	protected String getQueryOutDir() {
		return FilePaths.DIR_QUERY_OUT_HELPING_SURFACEFORM.getPath(KG);
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
		// Do nothing
	}
}
