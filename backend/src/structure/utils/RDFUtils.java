package structure.utils;

import java.io.InputStream;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.adapters.RDFReaderRIOT;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class RDFUtils {
	final static Logger logger = LogManager.getLogger(RDFUtils.class);

	private static Model parseModel(final InputStream is, final Model model) {
		final RDFReaderRIOT rdfReader = new RDFReaderRIOT("NT");
		rdfReader.read(model, is, "");
		return model;
	}

	private static Model getDefaultModel() {
		Model model = ModelFactory.createDefaultModel();
		// model.setNsPrefixes(NIFTransferPrefixMapping.getInstance());
		return model;
	}

	public static ResultSet execQuery(Model model, String queryStr) {
		//System.out.println("Executing");
		//System.out.println(queryStr);
		final Query query = QueryFactory.create(queryStr);
		// Execute the query and obtain results
		final QueryExecution qe = QueryExecutionFactory.create(query, model);
		final ResultSet results = qe.execSelect();
		return results;
	}

}
