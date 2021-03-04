package launcher.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import structure.config.constants.EnumConnection;
import structure.config.constants.FilePaths;
import structure.config.kg.EnumModelType;
import structure.utils.FileUtils;
import structure.utils.RDFUtils;
import structure.utils.Stopwatch;

public class LauncherTestQuery {

	public static void main(String[] args) {
		// Folder with queries to execute
		final String inFolderPath = "./crunchbase_queries/";
		final EnumModelType KG = EnumModelType.
		// MAG//
		// DBPEDIA_FULL//
				//WIKIDATA//
				CRUNCHBASE//
		;
		System.out.println("Testing query for: " + KG.name());
		Stopwatch.start(LauncherTestQuery.class.getName());
		// Dataset
		final Dataset dataset = TDBFactory.createDataset(FilePaths.DATASET.getPath(KG));
		System.out.println("Finished loading!");
		Stopwatch.endOutputStart(LauncherTestQuery.class.getName());
		// Model
		final Model model = dataset.getDefaultModel();
		try {
//		final String queryStr = "select distinct ?s (CONCAT(CONCAT(?fname, \" \"), ?lname) AS ?o) where {\r\n"
//				+ "?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontologycentral.com/2010/05/cb/vocab#Person> .\r\n"
//				+ "?s <http://ontologycentral.com/2010/05/cb/vocab#last_name> ?lname .\r\n"
//				+ "?s <http://ontologycentral.com/2010/05/cb/vocab#first_name> ?fname .\r\n" + "}";
			System.out.println("Executing query...");

			final File inFolder = new File(inFolderPath);
			if (!inFolder.isDirectory() || !inFolder.exists()) {
				throw new FileNotFoundException("Could not find directory ["+inFolderPath+"]...");
			}
			File[] files = inFolder.listFiles();
			PrintStream oldOut = System.out;
			for (File file : files) {
				final String queryStr = FileUtils.getContents(file);
				try (final PrintStream fos = new PrintStream(
						new FileOutputStream(new File(file.getName() + "_out"), false))) {
					System.setOut(fos);
				}
				execQuery(model, queryStr);

				Stopwatch.endOutputStart(LauncherTestQuery.class.getName());
			}
			System.setOut(oldOut);
			// getABC(model);
			// getObjectsFor(model, "http://dbpedia.org/resource/Smartphone");
			// getObjectsForSubjectOfSFQuery(model,
			// "http://dbpedia.org/resource/Smartphone");
//		Stopwatch.endOutputStart(LauncherTestQuery.class.getName());
			// getPredicates(model);
			// Stopwatch.endOutputStart(LauncherTestQuery.class.getName());
			// getTypes(model);
			// Stopwatch.endOutputStart(LauncherTestQuery.class.getName());
			// getPredicatesGroupCounts(model);
			// Stopwatch.endOutputStart(LauncherTestQuery.class.getName());
			// getRandom(model);
			// testVirtuoso();
			// getDBLPAuthors(model);
			// getSteveJobsConnections(model);
			// getCrunchbaseNews(model);
			// getPredicatesAndTypes(model);
		} catch (Exception e) {

		} finally {
			model.close();
			dataset.close();
		}

		System.out.println("Finished!");
		// "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 100"
	}

	private static Map<String, String> mapEntityToName = new HashMap<>();

	private static void getSteveJobsConnections(Model model) {
		Map<String, Integer> mapEntityNeighbours = new HashMap<>();
		System.out.println("################################################");
		System.out.println("# Steve Jobs - Grabbing 2nd degree connections #");
		System.out.println("################################################");
		String queryStr = "select distinct ?s ?p1 ?o1 ?p2 ?o2 where { \n" //
				+ " ?s  ?p1 ?o1 . \n" //
				+ " ?o1 ?p2 ?o2 . \n"//
				+ " FILTER(?s = <http://dbpedia.org/resource/Steve_Jobs>)" + " }"//
																					// + " LIMIT 100"
		;
		final String from = "steveJobs";

		ResultSet results = RDFUtils.execQuery(model, queryStr);
		while (results.hasNext()) {
			final QuerySolution qs = results.next();
			final String obj1 = qs.get("o1").toString();
			mapEntityNeighbours.put(obj1, mapEntityNeighbours.getOrDefault(obj1, 0) + 1);
		}

		System.out.println("############################################");
		System.out.println("# Steve Jobs #1                            #");
		System.out.println("############################################");
		queryStr = "select distinct ?s ?p1 ?o1 where { \n" //
				+ " ?s ?p1 ?o1 . \n" //
				+ " FILTER(?s = <http://dbpedia.org/resource/Steve_Jobs>)" + " }"//
																					// + " LIMIT 100"
		;
		System.out.println("%Single step");
		System.out.println();
		int level = 1;
		// Output Steve Jobs node
		System.out.println("\\node[rblock](steveJobs){dbr:Steve\\_Jobs};");

		results = RDFUtils.execQuery(model, queryStr);
		boolean firstInLevel = true;
		int nodeCounter = 0;
		final int maxNodes = 20;
		String prevNode = "";

		while (results.hasNext()) {
			final QuerySolution qs = results.next();
			final String edge = "";// qs.get("p1").toString();
			final String to = qs.get("o1").toString();
			if (!mapEntityNeighbours.containsKey(to)) {
				continue;
			}
			if (nodeCounter++ > maxNodes) {
				// ignore
				continue;
			} else {

				if (firstInLevel) {
					printNode(from + ".east", to, edge, from, DIRECTION.RIGHT, 25, "",
							mapEntityNeighbours.getOrDefault(to, 0));
					firstInLevel = false;
				} else {
					final String dirFrom = mapEntityToName.get(prevNode);
					if (dirFrom != null) {
						printNode(from + ".east", to, edge, dirFrom, DIRECTION.DOWN, 5, "",
								mapEntityNeighbours.getOrDefault(to, 0));
					}
				}
				prevNode = to;
			}
		}
		// Now print a node with the counter value
		if (nodeCounter > maxNodes) {
			printNode(from + ".east", "\\vdots (" + nodeCounter + ")", "\\ldots", mapEntityToName.get(prevNode),
					DIRECTION.DOWN, 5, "", 0);// , "draw=none,");
		}

		System.out.println("---------------------------------------------");

	}

	public enum DIRECTION {
		UP("above"), DOWN("below"), LEFT("left"), RIGHT("right"), //
		UP_LEFT(UP.direction + " " + LEFT.direction), //
		UP_RIGHT(UP.direction + " " + RIGHT.direction), //
		DOWN_LEFT(DOWN.direction + " " + LEFT.direction), //
		DOWN_RIGHT(DOWN.direction + " " + RIGHT.direction),//
		//
		;

		public String direction;

		DIRECTION(final String direction) {
			this.direction = direction;
		}
	};

	private static void printNode(String from, String to, String edge, String dirFrom, DIRECTION dir,
			final int distance, final String additionalTikzArgument, final int connectTo) {
		final String dirStr;

		if (edge.contains("sameAs") || edge.contains("date")) {
			// Ignore all sameAs links as they are relatively boring...
			return;
		}

		final String edgeLabelPos = "auto";// "left";//"auto";
		final String toName = createName(to);
		final String positioning = dir.direction + "=" + distance + "pt of " + dirFrom;
		System.out.println("\\node[rblock" //
				+ "," + positioning //
				+ additionalTikzArgument + "](" + toName + "){" //
				+ shorten(to) + "};")//
		;
		System.out.println("\\draw[edge] (" + from //
		// + ".east"//
				+ ") to node [" + edgeLabelPos + "] {" + shorten(edge) + "} (" + toName //
				+ ".west"//
				+ ");");

		if (connectTo > 0) {
			printNode(toName, "(" + connectTo + ")", "\\ldots", toName, DIRECTION.RIGHT, 30,
					", minimum width={width(\"(123456789)\")+2pt}", 0);
		}
	}

	private static String shorten(String to) {
		int maxLength = 15;
		to = to.replace("http://dbpedia.org/resource/", "dbr:");
		to = to.replace("http://dbpedia.org/property/", "dbp:");
		to = to.replace("http://dbpedia.org/ontology/", "dbo:");
		to = to.replace("http://en.wikipedia.org/wiki/", "wiki:");
		to = to.replace("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");
		to = to.replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:");
		to = to.replace("http://xmlns.com/foaf/0.1/", "foaf:");
		to = to.replace("http://www.w3.org/2002/07/owl#", "owl:");
		to = to.replace("http://www.w3.org/2001/XMLSchema#", "xsd:");
		to = to.replace("http://www.w3.org/2004/02/skos/core#", "skos:");
		to = to.replace("http://purl.org/linguistics/gold/", "gold:");
		to = to.replace("http://purl.org/dc/terms/", "purl:");
		to = to.replace("http://www.w3.org/ns/prov#", "prov:");

		to = to.replace("_", "\\_");
		// to = to.replace("^", "\\^");
		to = to.replace("#", "\\#");
		// to = to.replace("", "");

		if (to.length() > maxLength) {
			to = to.substring(0, maxLength) + "...";
		}
		return to;
	}

	private static int nameCounter = 0;

	private static String createName(String to) {
		String name;
		if ((name = mapEntityToName.get(to)) == null) {
			name = "" + nameCounter++;
			mapEntityToName.put(to, name);
		}
		return name;
	}

	private static void testVirtuoso() {
		final boolean LOCAL = true;
		if (LOCAL) {
			final String graphName = "http://dbpedia.org";
			final EnumConnection connShetland = EnumConnection.SHETLAND_VIRTUOSO;
			final String url = connShetland.baseURL;
			// final String url = "jdbc:virtuoso://localhost:1112";
			// "http://shetland.informatik.uni-freiburg.de:8890/sparql";//
			// virtuoso.jdbc4.VirtuosoException:
			// Wrong port number
			final String queryStr =
//					"select ?s ?p ?o where { ?s ?p ?o } LIMIT 100";
//					"select distinct ?s ?p ?o where { ?s ?p ?o "
//							+ ". FILTER( "//
//							+ "isLiteral(?o) "//
//							+ " ) "//
//							+ ". FILTER( "//
//							+ "STRLEN(?o) > 0 "//
//							+ " ) "//
//							+ " } limit 100";
//					"select distinct ?p where { ?s ?p ?o . FILTER(isLiteral(?o)) )} limit 1000";
					// "select distinct ?bPred where { \n" + "?aSubj ?bPred ?cObj . \n" + "} LIMIT
					// 100";
					// "SELECT DISTINCT ?s ?o WHERE { ?s
					// <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>
					// <http://www.w3.org/2002/07/owl#Thing> . ?s <http://xmlns.com/foaf/0.1/name>
					// ?o }"
					"SELECT DISTINCT ?p STR(?obj) AS ?o WHERE { <http://dbpedia.org/resource/Tiger_Woods> ?p ?obj . FILTER( isLiteral(?obj) ) . FILTER( STRLEN(STR(?obj)) > 1 ) }";
			throw new IllegalArgumentException("VirtGraph not supported at this moment.");
//			final VirtGraph virtGraph = new VirtGraph(graphName, url,
//					new String(connShetland.userAcc.getBytesUsername()),
//					new String(connShetland.userAcc.getBytesPassword()));
//			execQuery(virtGraph, queryStr);
		} else {
//			final String queryStr = "select distinct ?bPred where { \n" + "?aSubj ?bPred ?cObj . \n" + "} LIMIT 100";
//
//			try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", queryStr)) {
//				// Set the DBpedia specific timeout.
//				((QueryEngineHTTP) qexec).addParam("timeout", "10000");
//
//				// Execute.
//				ResultSet rs = qexec.execSelect();
//				while (rs.hasNext()) {
//					System.out.println(rs.next().toString());
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.err.println("============================================");
//				System.err.println(queryStr);
//				System.err.println("============================================");
//			}
		}
	}

//	private static void execQuery(VirtGraph virtGraph, String queryStr) {
//		System.out.println("Executing: " + queryStr);
//		final VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(queryStr, virtGraph);
//		final ResultSet results = vqe.execSelect();
//		displayQuery(results);
//	}

	private static void getRandom(Model model) {
		System.out.println("############################################");
		System.out.println("# Random                                   #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?a ?b ?c where { \n" + " ?a ?b ?c . \n" + " }" + " ORDER BY RAND()"
				+ " LIMIT 100";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getDBLPAuthors(Model model) {
		System.out.println("############################################");
		System.out.println("# Authors                                  #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?author ?b where { \n"
				+ "?author <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Agent> . \n"
				// + "?author <http://www.w3.org/2000/01/rdf-schema#label> ?b . \n"
				+ "?author <http://xmlns.com/foaf/0.1/name> ?b . \n"

				+ "}" + " LIMIT 100";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getCrunchbaseNews(Model model) {
		System.out.println("############################################");
		System.out.println("# News                                  #");
		System.out.println("############################################");
		final String queryStr = "SELECT ?s ?url WHERE {\n"
				+ "?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://ontologycentral.com/2010/05/cb/vocab#News> .\n"
				+ "?s <http://ontologycentral.com/2010/05/cb/vocab#url> ?url ." + "}\n" + " LIMIT 100";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getTypes(Model model) {
		System.out.println("############################################");
		System.out.println("# Types                                    #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?aType1 where { \n"
				+ "?aSubj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?aType1 . \n" + "}";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getPredicates(Model model) {
		System.out.println("############################################");
		System.out.println("# Predicates                               #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?bPred where { \n" + "?aSubj ?bPred ?cObj . \n" + "}";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getObjectsFor(Model model, final String subj) {
		System.out.println("############################################");
		System.out.println("# Objects For                              #");
		System.out.println("############################################");
		final StringBuilder sbSubj = new StringBuilder();
		if (!subj.startsWith("<")) {
			sbSubj.append("<");
		}
		sbSubj.append(subj);
		if (!subj.endsWith(">")) {
			sbSubj.append(">");
		}
		final String queryStr = "select distinct ?cObj where { \n" + sbSubj.toString() + " ?bPred ?cObj . \n" + "}";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getPredicatesGroupCounts(Model model) {
		System.out.println("############################################");
		System.out.println("# Predicate Counts                         #");
		System.out.println("############################################");
		final String queryStr = "select ?bPred (COUNT(?bPred) AS ?CT) where { \n" + "?aSubj ?bPred ?cObj . \n"
				+ "} GROUP BY ?bPred";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getABC(Model model) {
		final String queryStr = "select distinct ?a ?b ?c where { ?a ?b ?c } LIMIT 100";
		execQuery(model, queryStr);
	}

	private static void getPredicatesAndTypes(final Model model) {
		System.out.println("############################################");
		System.out.println("# Predicates linking different types       #");
		System.out.println("############################################");
		final String queryStr = "select distinct ?aType1 ?bPred ?cType2 where { \n"
				+ "?aSubj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?aType1 . \n"
				+ "?cObj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?cType2 . \n" + "?aSubj ?bPred ?cObj . \n"
				+ "}";
		execQuery(model, queryStr);
		System.out.println("---------------------------------------------");
	}

	private static void getObjectsForSubjectOfSFQuery(Model model, String subj) {
		System.out.println("############################################");
		System.out.println("# getObjectsForSubjectOfSFQuery            #");
		System.out.println("############################################");
		final StringBuilder sbSubj = new StringBuilder();
		if (!subj.startsWith("<")) {
			sbSubj.append("<");
		}
		sbSubj.append(subj);
		if (!subj.endsWith(">")) {
			sbSubj.append(">");
		}
		final String sfQuery = "SELECT DISTINCT ?s (STR(?obj) AS ?o) WHERE { \r\n"
				+ " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?oType .\r\n" + " ?s ?p ?obj .\r\n" //
				+ " FILTER( ?p IN (	<http://xmlns.com/foaf/0.1/givenName>, \r\n"
				+ "					<http://dbpedia.org/property/name>, \r\n"
				+ "					<http://xmlns.com/foaf/0.1/name>, \r\n"
				+ "					<http://xmlns.com/foaf/0.1/surname>, \r\n"
				+ "					<http://dbpedia.org/property/birthName>, \r\n"
				+ "					<http://www.w3.org/2000/01/rdf-schema#label>,\r\n"
				+ "					<http://xmlns.com/foaf/0.1/nick>,\r\n"
				+ "					<http://dbpedia.org/property/q>,\r\n"
				+ "					<http://dbpedia.org/property/n>\r\n" //
				+ "				) \r\n" //
				+ "		) .\r\n"//
				+ " FILTER( isLiteral(?obj) ) .\r\n" //
				+ " FILTER( STRLEN( STR(?obj) ) > 1 ) .\r\n"//
				+ " FILTER( STRLEN( STR(?obj) ) < 8000 ) .\r\n" //
				+ " FILTER( ?oType IN (<http://www.w3.org/2002/07/owl#Thing>, <http://www.w3.org/2004/02/skos/core#Concept>) ) .\r\n"//
				+ " FILTER( ?s = " + sbSubj.toString() + ")" //
				+ "}"//
		;
		execQuery(model, sfQuery);
		System.out.println("---------------------------------------------");

	}

	private static void execQuery(Model model, String queryStr) {
		System.out.println("Executing");
		System.out.println(queryStr);
		final ResultSet results = RDFUtils.execQuery(model, queryStr);
		displayQuery(results);
	}

	private static void displayQuery(ResultSet results) {
		while (results.hasNext()) {
			final QuerySolution qs = results.next();
			// Iterator<String> it = new de.dwslab.petar.walks.QuerySolutionIterator(qs);
			Iterator<String> it = qs.varNames();
			while (it.hasNext()) {
				final String varName = it.next();
				System.out.print(varName + ":" + qs.get(varName) + " ");
			}
			System.out.println();
		}
	}
}
