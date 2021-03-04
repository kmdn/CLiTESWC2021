package install.surfaceform.query.general;

import structure.config.constants.FilePaths;
import structure.config.kg.EnumModelType;

public class NP_URL_HSFQuery extends HSFQuery {
	public NP_URL_HSFQuery(EnumModelType KG) {
		super(KG);
	}

	@Override
	protected String getQueryInputDir() {
		return FilePaths.DIR_QUERY_IN_NP_URL_HELPING_SURFACEFORM.getPath(KG);
	}

	@Override
	protected String getQueryOutDir() {
		return FilePaths.DIR_QUERY_OUT_NP_URL_HELPING_SURFACEFORM.getPath(KG);
	}
}
