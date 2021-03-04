package install.surfaceform.query;

import install.surfaceform.query.general.NP_HSFQuery;
import structure.config.kg.EnumModelType;
import structure.interfaces.Executable;

public class NP_HSFQueryExecutor implements Executable {

	@Override
	public void init() {

	}

	@Override
	public boolean reset() {
		return false;
	}

	@Override
	public <T> T exec(Object... o) throws Exception {
		final EnumModelType KG;
		EnumModelType KGhelper = EnumModelType.DEFAULT;
		if (o.length > 0) {
			for (Object ob : o) {
				if (ob instanceof EnumModelType) {
					KGhelper = (EnumModelType) ob;
					break;
				}
			}
		}
		KG = KGhelper;
		KGhelper = null;
		new NP_HSFQuery(KG).execQueries();
		return null;
	}

	@Override
	public boolean destroy() {
		return false;
	}
}
