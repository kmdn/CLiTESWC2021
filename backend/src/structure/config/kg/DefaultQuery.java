package structure.config.kg;

/**
 * Constant (SPARQL-based) query definitions used by the EnumModelType
 * enumeration for specific knowledge graphs to retrieve entities
 * 
 * @author Kristian Noullet
 *
 */
public enum DefaultQuery {
//	DBPEDIA(""), //
//	FREEBASE(""), //
//	CRUNCHBASE(""), //
	// Note that the entity variable HAS to be ?s for RDF2Vec
	ALL_LITERALS("SELECT DISTINCT ?s ?o WHERE { ?s ?p ?o . FILTER(isLiteral(?o))}"), //
	ALL_LABELS("SELECT DISTINCT ?s ?o WHERE { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?o }"), //
	;
	public final String query;

	DefaultQuery(final String entityQuery) {
		this.query = entityQuery;
	}
}
