package install;

import org.apache.log4j.Logger;

import install.surfaceform.query.HSFQueryExecutor;
import install.surfaceform.query.NP_HSFQueryExecutor;
import install.surfaceform.query.NP_URLHSFQueryExecutor;
import install.surfaceform.query.SFQueryExecutor;
import structure.Pipeline;
import structure.config.kg.EnumModelType;

/**
 * Executes all queries from defined folders (for SF, HSF, NP HSF, NP URL HSF)
 * and saves the output to the respective output folders for the specific type
 * 
 * @author Kristian Noullet
 *
 */
public class LauncherExecuteQueries {

	public static void main(String[] args) {
		Pipeline pipeline = new Pipeline();
		final EnumModelType KG = EnumModelType.DEFAULT;//DBPEDIA_FULL;
		final String execMsg = "Executing queries for KG(" + KG.name() + ") - (SF, HSF, NP_HSF, NP_URLHSF)";
		Logger.getLogger(LauncherExecuteQueries.class)
				.info(execMsg);
		System.out.println(execMsg);
		pipeline.queue(new SFQueryExecutor(), KG);
		pipeline.queue(new HSFQueryExecutor(), KG);
		pipeline.queue(new NP_HSFQueryExecutor(), KG);
		pipeline.queue(new NP_URLHSFQueryExecutor(), KG);
		try {
			pipeline.exec();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
