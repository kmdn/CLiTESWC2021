package install.surfaceform.query.general;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

//import virtuoso.jena.driver.VirtGraph;
//import virtuoso.jena.driver.VirtuosoQueryExecution;
//import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import structure.config.constants.EnumConnection;
import structure.config.constants.FilePaths;
import structure.config.kg.EnumModelType;
import structure.utils.FileUtils;
import structure.utils.QuerySolutionIterator;

public abstract class LiteralEntityQuery {
	protected final EnumModelType KG;
	final boolean removeWhitespace;

	/**
	 * LiteralEntityQuery running on the KG's appropriate RDF database system
	 * (either VirtGraph or Jena model), reading input queries from KG-appropriate
	 * sub-filetree and outputting them accordingly
	 * 
	 * @param KG used knowledge graph
	 */
	protected LiteralEntityQuery(EnumModelType KG) {
		this(KG, true);
	}

	protected LiteralEntityQuery(EnumModelType KG, final boolean removeWhitespace) {
		this.KG = KG;
		this.removeWhitespace = removeWhitespace;
	}

	public void execQueries() throws IOException {

		try (final BufferedWriter bwAlternate = initAlternateChannelWriter()) {
			for (File f : new File(getQueryInputDir()).listFiles()) {
				if (f.isDirectory())
					continue;
				// Load query/-ies
				final String queryStr = FileUtils.getContents(f);
				List<BufferedWriter> writers = Lists.newArrayList();
				try (final BufferedWriter bwQuery = new BufferedWriter(
						new FileWriter(getQueryOutDir() + "/" + f.getName()))) {
					// Query the dataset and (1) output query outputs and (2) output alternate
					// channel data (e.g. linking)
					writers.add(bwQuery);
					if (bwAlternate != null) {
						writers.add(bwAlternate);
					}

					// Determines whether to run on a Jena Model or a Virtuoso VirtGraph
					if (this.KG.useVirtuoso()) {
						// Virtuoso VirtGraph execution
						final EnumConnection conn = this.KG.virtuosoConn;
						throw new IllegalArgumentException("Virtuoso VirtGraph currently not supported.");

						// final VirtGraph virtGraph = new VirtGraph(this.KG.virtuosoGraphname,
						// conn.baseURL,
						// new String(conn.userAcc.getBytesUsername()),
						// new String(conn.userAcc.getBytesPassword()));
						// execSelectQuery(queryStr, virtGraph, writers);

					} else {
						// Execution on the appropriate Apache Jena Model
						final Dataset dataset = TDBFactory.createDataset(FilePaths.DATASET.getPath(KG));
						final Model model = dataset.getDefaultModel();
						execSelectQuery(queryStr, model, writers);
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param result  Result to be output
	 * @param writers Writers[0]: General results output; Writer[1..n]: Specialised
	 *                alternate channel outputs
	 * @throws IOException
	 */
	protected void processResultLine(QuerySolution result, List<BufferedWriter> writers) throws IOException {
		final Iterator<String> itVars = new QuerySolutionIterator(result);
		while (itVars.hasNext()) {
			final String varName = itVars.next();
			String value = result.get(varName).toString();
			if (removeWhitespace) {
				// Remove all whitespace characters and replace them by single white space
//				final int initLength = value.length();
//				final String preVal = value;
				value = value.replaceAll("\\p{Space}", " ").replace("  ", " ");
//				final int diffLength = (initLength - value.length());
//				if (!preVal.equals(value)) {
//					System.out.println("Changed:");
//					System.out.println(preVal);
//					System.out.println(value);
//					System.out.println("------------------");
//				}
//				if (diffLength > 0) {
//					System.out.println("Spaces removed (" + diffLength + "): " + value);
//				}
			}
			// Output to query results
			outputMainChannel(varName, value, itVars.hasNext(), writers.get(0));
			if (writers.size() > 1) {
				// Output to query linking when appropriate
				outputAlternateChannels(varName, value, itVars.hasNext(), writers.subList(1, writers.size()));
			}
		}
	}

	/**
	 * Executes passed query on specified model, writing returned triples out
	 * appropriately (as specified by parameters)
	 * 
	 * @param queryStr query to be executed
	 * 
	 * @param model    model on which to execute passed query
	 * 
	 * @param writers  Writers outputting query results to appropriate files
	 * @throws IOException
	 */
	public void execSelectQuery(final String queryStr, Model model, final List<BufferedWriter> writers)
			throws IOException {
		final Query query = QueryFactory.create(queryStr);
		// Execute the query and obtain results
		final QueryExecution qe = QueryExecutionFactory.create(query, model);
		selectProcessResults(qe, writers);
	}

	/**
	 * 
	 * Equivalent of {@link #execSelectQuery(String, Model, List)} but for
	 * Virtuoso's VirtGraph
	 * 
	 * @param queryStr  query to be executed
	 * 
	 * @param virtGraph virtuoso graph on which to execute passed query
	 * 
	 * @param writers   Writers outputting query results to appropriate files
	 * @throws IOException
	 */
	/*
	 * public void execSelectQuery(final String queryStr, VirtGraph virtGraph, final
	 * List<BufferedWriter> writers) throws IOException { final
	 * VirtuosoQueryExecution qe = VirtuosoQueryExecutionFactory.create(queryStr,
	 * virtGraph); selectProcessResults(qe, writers); }
	 */

	/**
	 * Added as a central processing node-point for both Jena Model and Virtuoso's
	 * VirtGraph ...
	 * 
	 * @param qe
	 * @param writers
	 * @throws IOException
	 */
	private void selectProcessResults(final QueryExecution qe, final List<BufferedWriter> writers) throws IOException {
		final ResultSet results = qe.execSelect();

		// Iterate through returned triples
		while (results.hasNext()) {
			final QuerySolution result = results.next();
			processResultLine(result, writers);
		}
		qe.close();
	}

	protected abstract void outputMainChannel(String varName, String value, boolean hasNext, BufferedWriter writer)
			throws IOException;

	protected abstract void outputAlternateChannels(String varName, String value, boolean hasNext,
			List<BufferedWriter> writers) throws IOException;

	protected abstract BufferedWriter initAlternateChannelWriter() throws IOException;

	protected abstract String getQueryInputDir();

	protected abstract String getQueryOutDir();

}
