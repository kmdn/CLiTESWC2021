package structure.config.constants;

/**
 * Constants used for RDF-related tasks
 * @author Kristian Noullet
 *
 */
public enum RDFConstants {
	RDF_BMW("http://bmw-kg.de/"),
	RDF_CLASS(RDF_BMW.val + "class/"),
	RDF_PROPERTY(RDF_BMW.val + "property/"),
	RDF_ENTITY(RDF_BMW.val + "entity/"),

	
	XSD_STRING("^^xsd:string"),
	XSD_DATETIME("^^xsd:dateTime"),
	XSD_LANGUAGE("^^xsd:language"),
	XSD_INTEGER("^^xsd:integer"),
	XSD_DECIMAL("^^xsd:decimal"),
	
	
	//Predicates
	PRED_CREATOR("sioc:hasCreator"),
	PRED_TYPE("rdf:type"),
	PRED_TEXT("sioc:content"),
	PRED_DATETAGGED("dcterms:created"),
	PRED_LANGUAGE("dcterms:language"),
	PRED_HASENTITY(RDF_PROPERTY.val + "hasEntityAnnotation"),
	PRED_BEGININDEX("nif:beginIndex"),
	PRED_ENDINDEX("nif:endIndex"),
	PRED_ENTITYOCC("nif:entityOccurrenceProv"),
	PRED_ANCHOR("nif:anchorOf"),
	PRED_ENTITYTYPE("rdf:entityType"),
	PRED_STATE(RDF_PROPERTY.val + "state"),
	PRED_LABEL("rdf:label"),
	PRED_BMW_ENTITYTYPE("bmw:entityType"),
	
	//Objects
	OBJ_CREATOR(RDF_BMW.val + "user/"),
	OBJ_DOCTYPE(OBJ_CREATOR.val + "customer_feedback"),
	OBJ_ENTITY(RDF_CLASS.val + "EntityAnnotation"),


	
	//Prefixes
	PREFIX("@prefix "),
	PREFIX_XSD(PREFIX.val + "xsd: <http://www.w3.org/2001/XMLSchema#> . \n"),
	PREFIX_DCTERMS(PREFIX.val + "dcterms: <http://purl.org/dc/terms/> . \n"),
	PREFIX_RDF(PREFIX.val + "rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \n"),
	PREFIX_NIF(PREFIX.val + " nif: <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#> . \n"),
	PREFIX_SIOC(PREFIX.val + "sioc: <http://rdfs.org/sioc/ns#> . \n"),

	// RDF predicates for SPARQL queries
	// Common predicates
	SPARQL_PRED_TYPE("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
	// From 'types.ttl' file
	SPARQL_PRED_ENTITY_TYPE("http://www.bmw-kg.de/entity/entityType"),
	SPARQL_PRED_LABEL("http://www.w3.org/1999/02/22-rdf-syntax-ns#label"),
	// From 'rdf.ttl' file
	// Document predicates
	SPARQL_PRED_HAS_CREATOR("http://rdfs.org/sioc/ns#hasCreator"),
	SPARQL_PRED_CONTENT("http://rdfs.org/sioc/ns#content"),
	SPARQL_PRED_CREATED("http://purl.org/dc/terms/created"),
	SPARQL_PRED_LANGUAGE("http://purl.org/dc/terms/language"),
	SPARQL_PRED_HAS_ENTITY_ANNOTATION("http://bmw-kg.de/property/hasEntityAnnotation"),
	// Entity predicates
	SPARQL_PRED_ANCHOR_OF("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#anchorOf"),
	SPARQL_PRED_BEGIN_INDEX("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#beginIndex"),
	SPARQL_PRED_END_INDEX("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#endIndex"),
	SPARQL_PRED_LINKED_TO("http://www.w3.org/1999/02/22-rdf-syntax-ns#linkedTo"),
	SPARQL_PRED_ENTITY_OCCURENCE_PROV("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#entityOccurrenceProv"),
	SPARQL_PRED_STATE("http://bmw-kg.de/property/state"),
	;

	public final String val;
	public final String desc;
	
	private RDFConstants(final String value) {
		this(value, "");
	}

	private RDFConstants(String value, String desc) {
		this.val = value;
		this.desc = desc;
	}
	
	


}
