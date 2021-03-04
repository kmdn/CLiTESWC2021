package structure.config.kg;

/**
 * Constant (SPARQL-based) query definitions used by the EnumModelType
 * enumeration for specific knowledge graphs to retrieve entities
 * 
 * @author Kristian Noullet
 *
 */
public enum EntityQuery {
//	DBPEDIA(""), //
//	FREEBASE(""), //
//	CRUNCHBASE(""), //
	// Note that the entity variable HAS to be ?s for RDF2Vec
	MAG("SELECT DISTINCT ?s WHERE { ?s <http://xmlns.com/foaf/0.1/name> ?o }"), //
	DBLP("select distinct ?s where { \n"
			+ "?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Agent> . \n"
			+ "?s <http://www.w3.org/2000/01/rdf-schema#label> ?b . \n"
			// + "?author <http://xmlns.com/foaf/0.1/name> ?b . \n"

			+ "}"), //
	CRUNCHBASE2("SELECT DISTINCT ?s WHERE { \r\n" //
			+ "  ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o \r\n"//
			+ ". FILTER( ?o IN (\r\n" //
			+ "<http://ontologycentral.com/2010/05/cb/vocab#Acquisition>, \r\n"//
			+ "<http://ontologycentral.com/2010/05/cb/vocab#Organization>, \r\n"//
			+ "<http://ontologycentral.com/2010/05/cb/vocab#Person>, \r\n"//
			+ "<http://ontologycentral.com/2010/05/cb/vocab#News>, \r\n"//
			+ "<http://ontologycentral.com/2010/05/cb/vocab#Job>, \r\n"//
			+ "<http://ontologycentral.com/2010/05/cb/vocab#Location> \r\n"//
			+ "				)\r\n" //
			+ "		)\r\n"//
			+ "}"), //
	WIKIDATA("SELECT DISTINCT ?s WHERE "//
			+ "{ ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://wikiba.se/ontology#Item> }"//
	), //
	DEFAULT("Select ?s Where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Thing>}") //
	;
	public final String query;

	EntityQuery(final String entityQuery) {
		this.query = entityQuery;
	}
}
