package structure.config.constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import structure.config.kg.EnumModelType;

/**
 * Enumeration containing all (constant) file locations the framework utilises
 * (with the exception of properties files due to dependency cycles), handling
 * folder structure generation (if not yet existant) based on defined knowledge
 * graphs (see {@link EnumModelType} for KG locations)
 * 
 * @author Kristian Noullet
 *
 */
public enum FilePaths {
	// Contains all constant paths
	// Attempting to keep a specific order
	//
	// ##################################
	// # MAIN
	// ##################################
	// ##################################
	// DIRECTORIES for src/main
	// ##################################
	DIR_RESOURCE("resources/"), //
	DIR_DATA(DIR_RESOURCE.path + "data/"), //
	DIR_DATASETS(DIR_DATA.path + "datasets/"), //
	DIR_MENTIONS(DIR_DATA.path + "mentions/"), //
	DIR_NEWS(DIR_DATA.path + "news/"), //
	DIR_NEWS_BODY(DIR_NEWS.path + "body/"), //
	DIR_NEWS_LINKS(DIR_NEWS.path + "links/"), //
	DIR_NEWS_FILTERED(DIR_NEWS.path + "filtered/"), //
	DIR_NEWS_FILTERED_BODY(DIR_NEWS_FILTERED.path + "body/"), //
	DIR_NEWS_FILTERED_LINKS(DIR_NEWS_FILTERED.path + "links/"), //

	DIR_LOGS(DIR_DATA.path + "logs/"), //
	DIR_TAGTOG_OUTPUT(DIR_DATA.path + "tagtog/"), //
	DIR_TAGTOG_ANALYSIS(DIR_DATA.path + "tagtog_analysis/"), //
	DIR_TAGTOG_OUTPUT_ANNJSON(DIR_TAGTOG_OUTPUT.path + "ann_json/"), //
	DIR_TAGTOG_OUTPUT_DOCJSON(DIR_TAGTOG_OUTPUT.path + "doc_json/"), //
	DIR_TAGTOG_OUTPUT_TEXT(DIR_TAGTOG_OUTPUT.path + "text/"), //
	DIR_TAGTOG_OUTPUT_RDF(DIR_TAGTOG_OUTPUT.path + "rdf/"), //
	DIR_TAGTOG_OUTPUT_RDF_ANNJSON(DIR_TAGTOG_OUTPUT_RDF.path + "ann_json/"), //
	DIR_TAGTOG_OUTPUT_RDF_DOCJSON(DIR_TAGTOG_OUTPUT_RDF.path + "doc_json/"), //
	DIR_TAGTOG_OUTPUT_RDF_TEXT(DIR_TAGTOG_OUTPUT_RDF.path + "text/"), //
	DIR_HOPS(FilePaths.DIR_DATA.path + "hops/"), //
	DIR_HOPS_GRAPH(FilePaths.DIR_HOPS.path + "graph/"), //
	DIR_HOPS_OUTPUT(DIR_HOPS.path + "output/"), //
	DIR_LOGS_HOPS(DIR_HOPS.path + "logs/"), //
	DIR_QUERY(DIR_DATA.path + "query/"), //
	DIR_QUERY_IN(DIR_QUERY.path + "in/"), //
	DIR_QUERY_OUT(DIR_QUERY.path + "out/"), //
	DIR_QUERY_IN_SURFACEFORM(DIR_QUERY_IN.path + "entity_surfaceform/"), //
	DIR_QUERY_OUT_SURFACEFORM(DIR_QUERY_IN_SURFACEFORM.path.replace(DIR_QUERY_IN.path, DIR_QUERY_OUT.path)), //
	DIR_QUERY_IN_HELPING_SURFACEFORM(DIR_QUERY_IN.path + "helping_surfaceform/"), //
	DIR_QUERY_OUT_HELPING_SURFACEFORM(
			DIR_QUERY_IN_HELPING_SURFACEFORM.path.replace(DIR_QUERY_IN.path, DIR_QUERY_OUT.path)), //
	DIR_QUERY_IN_NP_HELPING_SURFACEFORM(DIR_QUERY_IN.path + "np_helping_surfaceform/"), //
	DIR_QUERY_OUT_NP_HELPING_SURFACEFORM(
			DIR_QUERY_IN_NP_HELPING_SURFACEFORM.path.replace(DIR_QUERY_IN.path, DIR_QUERY_OUT.path)), //
	DIR_QUERY_IN_NP_URL_HELPING_SURFACEFORM(DIR_QUERY_IN.path + "np_helping_surfaceform_url/"), //
	DIR_QUERY_OUT_NP_URL_HELPING_SURFACEFORM(
			DIR_QUERY_IN_NP_URL_HELPING_SURFACEFORM.path.replace(DIR_QUERY_IN.path, DIR_QUERY_OUT.path)), //
	DIR_QUERY_IN_EXTENDED_GRAPH_HOPS(DIR_QUERY_IN.path + "hops/",
			"Folder containing all queries pertaining to so-called 'hops' for vicinity scoring."), //
	DIR_QUERY_OUT_EXTENDED_GRAPH_HOPS(DIR_QUERY_OUT.path + "hops/",
			"Folder containing outputs of queries pertaining to so-called 'hops' for vicinity scoring."), //
	DIR_EXTENDED_GRAPH(DIR_DATA.path + "extended_graph/"), //
	DIR_OUT_HSFURL(DIR_DATA.path + "url_contents/"), //
	DIR_BABELFY(DIR_DATA.path + "babelfy/", "Babelfy related files & folders within this folder"), //
	DIR_BABELFY_OUTPUT(DIR_BABELFY.path + "out/", "Babelfy outputs"), //
	DIR_BABELFY_INPUT(DIR_BABELFY.path + "in/", "Babelfy inputs (possible useful in the future)"), //
	// RDF2Vec Walks
	DIR_WALK_GENERATOR(DIR_DATA.path + "walks/", "Walk generation output directory"), //
	// SSP Embeddings output
	DIR_SSP(DIR_DATA.path + "ssp/"), //
	DIR_EMBEDDINGS_SSP_ENTITY_REPRESENTATION(DIR_SSP.path + "representations/",
			"(Potential Deprecated Logic) Directory for entities, each file being one entity's representation"), //

	// ##################################
	// FILES for src/main
	// ##################################
	FILE_STOPWORDS(DIR_DATA.path + "stopwords.txt"), //

	FILE_CRUNCHBASE_ENTITIES(DIR_DATA.path + "crunchbase_entities.nt"), //
	FILE_CRUNCHBASE_ENTITIES_TYPED_LITERAL_STRING(DIR_DATA.path + "crunchbase_entities_typed_literal_string.nt"), //
	FILE_CRUNCHBASE_ENTITIES_NOUN_PHRASES(DIR_DATA.path + "crunchbase_sf_noun_phrases.nt"), //
	FILE_CRUNCHBASE_ENTITIES_NOUN_PHRASES_LINKED(DIR_DATA.path + "crunchbase_sf_noun_phrases_linked.nt"), //
	FILE_NEWS_URLS_CONTENT_UNSORTED(DIR_DATA.path + "news_urls_content_unsorted.nt"), //
	FILE_NEWS_URLS_CONTENT_SORTED(DIR_DATA.path + "news_urls_content_sorted.nt"), //
	FILE_TAGTOG_SAMPLE_OUTPUT(DIR_DATA.path + "tagtog_sample.json"), //
	FILE_DUMP_CRUNCHBASE(DIR_DATA.path + "crunchbase-dump-201510.nt"), //
	FILE_CB_NEWS_URL(DIR_DATA.path + "cb_news.txt"), //

	// NPComplete's required tagger file
	FILE_NPCOMPLETE_ENGLISH_TAGGER("./lib/english-left3words-distsim.tagger"), //
	FILE_MENTIONS_BLACKLIST(DIR_MENTIONS.path + "blacklist.txt"), //
	FILE_LSH_DOCUMENT_VECTORS_SPARSE(DIR_MENTIONS.path + "document_vectors_sparse_entries.txt"), //
	FILE_LSH_HASHES(DIR_MENTIONS.path + "hashes.txt"), //
	FILE_PAGERANK(DIR_DATA.path + "pagerank.nt"), //
	FILE_PAGERANK_ADAPTED(DIR_DATA.path + "pagerank_adapted.nt"), //
	// Query-related files
	// FILE_QUERY_INPUT_ORGANIZATION_NAME(DIR_QUERY_IN_SURFACEFORM.path +
	// "literal_organization_name.txt"), //
	// FILE_QUERY_INPUT_ORGANIZATION_ALSOKNOWNAS(DIR_QUERY_IN_SURFACEFORM.path +
	// "literal_organization_also_known_as.txt"), //
	// FILE_QUERY_INPUT_PERSON_ALSOKNOWNAS(DIR_QUERY_IN_SURFACEFORM.path +
	// "literal_person_also_known_as.txt"), //
	// FILE_QUERY_INPUT_PERSON_FIRSTNAME_LASTNAME(DIR_QUERY_IN_SURFACEFORM.path +
	// "literal_person_firstName_lastName.txt"), //
	// FILE_QUERY_INPUT_PERSON_LASTNAME(DIR_QUERY_IN_SURFACEFORM.path +
	// "literal_person_lastName.txt"), //
	// FILE_QUERY_INPUT_PRODUCT_ALSOKNOWNAS(DIR_QUERY_IN_SURFACEFORM.path +
	// "literal_product_also_known_as.txt"), //
	// FILE_QUERY_INPUT_PRODUCT_NAME(DIR_QUERY_IN_SURFACEFORM.path +
	// "literal_product_name.txt"), //
	// Extended RDF graph file
	// extended_graph.nt
	// bmw_graph.nt
	// FILE_EXTENDED_GRAPH(DIR_DATA.path + "rdf_nodocs.nt"), //
	FILE_EXTENDED_GRAPH(DIR_DATA.path + "MAGFieldsOfStudyKG.nt"), //
	// FILE_EXTENDED_GRAPH(DIR_DATA.path + "rdf.nt"), //
	FILE_OUT_HSFURL_MAPPING(DIR_DATA.path + "url_mapping_log.txt",
			"Log file keeping track where each website is stored locally so we can easily retrieve the appropriate URL's contents!"), //

	FILE_TXT_ENTITIES(DIR_DATA.path + "entities.txt", "TXT File containing all entities"), //
	FILE_NT_ENTITIES(DIR_DATA.path + "entities.nt", "NT File containing all entities"), //

	// "Corrected" RDF Graph files
	FILE_KNOWLEDGE_GRAPH(DIR_DATA.path + "kg.nt"), //
	FILE_GRAPH_RDF(DIR_DATA.path + "rdf.ttl"), //
	FILE_GRAPH_RDF_TYPES(DIR_DATA.path + "types.ttl"), //
	// Extended TXT graph file
	FILE_EXTENDED_GRAPH_TEXT(DIR_DATA.path + "extended_graph_text.txt"), //
	// Hops-related files
	FILE_HOPS_GRAPH_DUMP_PATH_IDS(DIR_HOPS_GRAPH.path + "path_ids.txt"), //
	FILE_HOPS_GRAPH_DUMP_EDGE_IDS(DIR_HOPS_GRAPH.path + "edge_ids.txt"), //
	FILE_HOPS_GRAPH_DUMP(DIR_HOPS_GRAPH.path + "graph.txt"), //
	FILE_HOPS_SOURCE_NODES(DIR_HOPS.path + "source_nodes.txt",
			"Input file for path building. File containing all nodes from which we should hop"), //
	FILE_HOPS_SOURCE_NODES_NOT_FOUND(DIR_HOPS.path + "source_nodes_missing.txt",
			"Output file for path building. File to which will be output if items from within the FILE_HOPS_SOURCE_NODES file were not found in the graph"), //
	FILE_HOPS_OUTPUT_PATHS_TEMPLATE(DIR_HOPS_OUTPUT.path + "paths", false), //
	FILE_HOPS_OUTPUT_EDGES_TEMPLATE(DIR_HOPS_OUTPUT.path + "edges", false), //
	FILE_HOPS_OUTPUT_DIRECTIONS_TEMPLATE(DIR_HOPS_OUTPUT.path + "directions", false), //
	FILE_TAGTOG_DEFAULT_OUTPUT(DIR_TAGTOG_OUTPUT.path + "tagtog_output.txt"), //
	// FILE_BABELFY_DEFAULT_OUTPUT(DIR_BABELFY_OUTPUT.path +
	// "babelfy_default_output.json"), //
	FILE_ENTITY_SURFACEFORM_LINKING(DIR_DATA.path + "links_surfaceForms.txt"//
	// + "_shetland"
	), //

	// Walk Generator Embeddings files

	FILE_GRAPH_WALK_OUTPUT(DIR_WALK_GENERATOR.path + "walk.txt"), //
	// Just use if you have to output walks somewhere completely different (e.g. due
	// to space restrictions)
	// FILE_GRAPH_WALK_OUTPUT("/mnt/maclusext/" + "walk.txt", true,
	// EnumModelType.NONE), //
	FILE_GRAPH_WALK_OUTPUT_SENTENCES(DIR_WALK_GENERATOR.path + "walk_sentences.txt"), //
	FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS(DIR_WALK_GENERATOR.path
			// + "DBpediaVecotrs200_20Shuffle.txt"//
			// + "graphwalk_entity_embeddings.txt_new"//
			+ "graphwalk_entity_embeddings.txt"//
			, "Proper output for computed entity embeddings"), //
	FILE_EMBEDDINGS_GRAPH_WALK_ENTITY_EMBEDDINGS_RAWMAP(DIR_WALK_GENERATOR.path + "graphwalk_entity_embeddings_map.raw",
			"RAW dump of the used hashmap (should be faster for loading rather than using line-by-line logic)"), //

	// Walk Generator general files
	FILE_GRAPH_WALK_BLACKLIST_PREDICATE(DIR_WALK_GENERATOR.path + "blacklist_predicates.txt"), //
	FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_HUMAN(DIR_WALK_GENERATOR.path + "walk_predicate_mapping.txt"), //
	FILE_GRAPH_WALK_ID_MAPPING_PREDICATE_MACHINE(DIR_WALK_GENERATOR.path + "walk_predicate_mapping.raw"), //
	FILE_GRAPH_WALK_ID_MAPPING_ENTITY_HUMAN(DIR_WALK_GENERATOR.path + "walk_entity_mapping.txt"), //
	FILE_GRAPH_WALK_ID_MAPPING_ENTITY_MACHINE(DIR_WALK_GENERATOR.path + "walk_entity_mapping.raw"), //
	// Walk generator entity log file
	FILE_GRAPH_WALK_LOG_ENTITY(DIR_WALK_GENERATOR.path + "processed_entities.log"), //

	// SSP Embeddings files
	FILE_EMBEDDINGS_SSP_TEXTDATA_SORTED(DIR_SSP.path + "ssp_file_textdata_sorted.txt",
			"Mapping of files and entities for SSP entity representations"), //
	FILE_EMBEDDINGS_SSP_SENTENCES(DIR_SSP.path + "ssp_sentences.txt"), //
	FILE_EMBEDDINGS_SSP_ENTITY_EMBEDDINGS(DIR_SSP.path + "ssp_entity_embeddings.txt",
			"Proper output for computed entity embeddings"), //
	FILE_EMBEDDINGS_SSP_ENTITY_EMBEDDINGS_RAWMAP(DIR_SSP.path + "ssp_entity_embeddings_map.raw",
			"RAW dump of the used hashmap (should be faster for loading rather than using line-by-line logic)"), //
	FILE_SSP_QUERY_OUT_COMBINED_OUTPUT(DIR_QUERY_OUT.path + "query_output_combined.txt"), //
	FILE_DATASET_NORMALIZATION_MAPPING_SUBJECT(DIR_DATASETS.path + "normalized_subjects.txt"), //
	FILE_DATASET_NORMALIZATION_MAPPING_PREDICATE(DIR_DATASETS.path + "normalized_predicates.txt"), //
	FILE_DATASET_NORMALIZATION_MAPPING_OBJECT(DIR_DATASETS.path + "normalized_objects.txt"), //

	// ##################################
	// LOG FILES
	// ##################################
	LOG_FILE_WEB_CRAWLING(DIR_LOGS.path + "webcrawler.txt"), //
	LOG_FILE_ERROR_WEB_CRAWLING(DIR_LOGS.path + "webcrawler_error.txt"), //
	LOG_FILE_HOPS_FILES_CREATED(DIR_LOGS_HOPS.path + "created_pathfiles.txt"), //
	LOG_FILE_TAGTOG_ANALYSIS(DIR_TAGTOG_OUTPUT.path + "tagtog_analysis.txt"), //
	LOG_FILE_CRAWL_CB_ERROR((DIR_LOGS.path + "cb_news_error.txt")), //
	LOG_FILE_CRAWL_CB_PROGRESS((DIR_LOGS.path + "cb_news_progress.txt")), //
	LOG_FILE_CRAWL_CB_IGNORE((DIR_LOGS.path + "cb_news_ignore.txt")), //

	// ##################################
	// DATASETS
	// ##################################
	DATASET_SAMPLE(DIR_DATASETS.path + "sample.dataset", false), //
	DATASET_CRUNCHBASE(DIR_DATASETS.path + "crunchbase.dataset", false), //
	DATASET(DIR_DATASETS.path + "graph.dataset", false), //

	// ##################################
	// # TEST
	// ##################################
	// ##################################
	// DIRECTORIES for src/test
	// ##################################
	DIR_TEST_RDF_PAGERANK(DIR_DATA.path + "rdfpagerank_test/"), //
	DIR_TEST_RDF_PAGERANK_IN(DIR_TEST_RDF_PAGERANK.path + "in/"), //
	DIR_TEST_RDF_PAGERANK_OUT(DIR_TEST_RDF_PAGERANK.path + "out/"), //
	// DIR_TEST_BABELFY(DIR_BABELFY.path + "test/", "Babelfy Test directory"), //
	// DIR_TEST_BABELFY_OUTPUT(DIR_TEST_BABELFY.path + "out/", "Babelfy Test output
	// directory"), //
	// DIR_TEST_BABELFY_INPUT(DIR_TEST_BABELFY.path + "in/", "Babelfy Test input
	// directory"), //
	DIR_TEST_TAGTOG(DIR_TAGTOG_OUTPUT.path + "test/", "TAGTOG Test directory"), //
	DIR_TEST_TAGTOG_TEXT(DIR_TEST_TAGTOG.path + "text/", "TAGTOG Test directory"), //
	DIR_TEST_TAGTOG_DOCJSON(DIR_TEST_TAGTOG.path + "doc_json/", "TAGTOG Test directory"), //
	DIR_TEST_TAGTOG_ANNJSON(DIR_TEST_TAGTOG.path + "ann_json/", "TAGTOG Test directory"), //
	DIR_TEST_TAGTOG_TEXT_OUTPUT(DIR_TEST_TAGTOG_TEXT.path + "out/", "TAGTOG Test output directory"), //
	DIR_TEST_TAGTOG_TEXT_INPUT(DIR_TEST_TAGTOG_TEXT.path + "in/", "TAGTOG Test input directory"), //
	DIR_TEST_TAGTOG_ANNJSON_OUTPUT(DIR_TEST_TAGTOG_ANNJSON.path + "out/", "TAGTOG Test output directory"), //
	DIR_TEST_TAGTOG_ANNJSON_INPUT(DIR_TEST_TAGTOG_ANNJSON.path + "in/", "TAGTOG Test input directory"), //
	DIR_TEST_TAGTOG_DOCJSON_OUTPUT(DIR_TEST_TAGTOG_DOCJSON.path + "out/", "TAGTOG Test output directory"), //
	DIR_TEST_TAGTOG_DOCJSON_INPUT(DIR_TEST_TAGTOG_DOCJSON.path + "in/", "TAGTOG Test input directory"), //
	DIR_TEST_GRAPH(DIR_HOPS_GRAPH.path + "test/"), //

	// ##################################
	// FILES for src/test
	// ##################################
	TEST_FILE_OUT_NEWS_TXT(DIR_DATA.path + "out_#news.txt"), //
	TEST_FILE_NEWS_URLS_IN(DIR_DATA.path + "news_urls_test.nt"), //
	TEST_FILE_PAGERANKRDF_EXAMPLE1_IN(DIR_TEST_RDF_PAGERANK_IN.path + "example1.nt"), //
	TEST_FILE_PAGERANKRDF_EXAMPLE2_IN(DIR_TEST_RDF_PAGERANK_IN.path + "example2.nt"), //
	TEST_FILE_PAGERANKRDF_EXAMPLE3_IN(DIR_TEST_RDF_PAGERANK_IN.path + "data_example.nt"), //
	TEST_FILE_PAGERANKRDF_EXAMPLE1_OUT(DIR_TEST_RDF_PAGERANK_OUT.path + "example1.nt"), //
	TEST_FILE_PAGERANKRDF_EXAMPLE2_OUT(DIR_TEST_RDF_PAGERANK_OUT.path + "example2.nt"), //
	TEST_FILE_PAGERANKRDF_EXAMPLE3_OUT(DIR_TEST_RDF_PAGERANK_OUT.path + "data_example.nt"), //
	TEST_FILE_SURFACE_FORM_EXTRACTOR_OUT_FILE(DIR_DATA.path + "out_surface_forms_extracted.txt"), //
	TEST_FILE_ENTITIES_NOUNPHRASE_SURFACEFORMS_SMALL(DIR_DATA.path + "entities_np_sf_small_test.txt"), //
	TEST_FILE_SPARQL_VIRTUOSO_EXAMPLE("resources/data/query_test/sparql_example.txt"), //
	TEST_FILE_SPARQL_VIRTUOSO_EXAMPLE_LONG("resources/data/query_test/long_query.txt"), //
	TEST_FILE_GRAPH_DUMP_PATH_IDS(DIR_TEST_GRAPH.path + "path_ids.txt"), //
	TEST_FILE_GRAPH_DUMP_EDGE_IDS(DIR_TEST_GRAPH.path + "edge_ids.txt"), //
	TEST_FILE_GRAPH_DUMP(DIR_TEST_GRAPH.path + "graph.txt"), //

	// ##################################
	// DIRECTORY for TRAINING documents
	// ##################################
	TRAINING_FILES(DIR_DATA.path + "TESTING/"), //

	;
	protected final String path;
	private final String val;

	FilePaths(final String path) {
		this(path, true, EnumModelType.DEFAULT);
	}

	FilePaths(final String path, final EnumModelType KG) {
		this(path, true, KG);
	}

	FilePaths(final String path, final boolean initFile) {
		this(path, "", initFile, EnumModelType.DEFAULT);
	}

	FilePaths(final String path, final boolean initFile, final EnumModelType KG) {
		this(path, "", initFile, KG);
	}

	FilePaths(final String path, final String desc) {
		this(path, desc, EnumModelType.DEFAULT);
	}

	FilePaths(final String path, final String desc, final EnumModelType KG) {
		this(path, desc, true, KG);
	}

	FilePaths(final String path, final String desc, final boolean initFile, final EnumModelType KG) {
		if (KG != null) {
			switch (KG) {
			case DEFAULT:
				// Initialise for every KG if it's default one
				for (EnumModelType initKG : EnumModelType.values()) {
					init((initKG.root.endsWith("/") ? initKG.root : initKG.root + "/") + path, initFile);
				}
				break;
			default:
				init((KG.root.endsWith("/") ? KG.root : KG.root + "/") + path, initFile);
				break;
			}
		} else {
			init(path, initFile);
		}
		this.path = path;
		this.val = path;
	}

	public String getPath(EnumModelType KG) {
		return KG.root + this.path;
	}

	private void init(final String path, final boolean initFile) {
		if (initFile) {
			final File file = new File(path);
			try {
				if (!file.exists()) {
					final File parentFile = file.getParentFile();
					boolean parentCreatedSuccessfully = true;
					if (parentFile != null && !parentFile.exists()) {
						parentCreatedSuccessfully = parentFile.mkdirs();
					}

					if (parentCreatedSuccessfully) {
						if (path.endsWith("/")) {
							final boolean created = file.mkdir();
							// System.out.println("Created(" + created + "): " + file.getAbsolutePath());
						} else {
							final boolean created = file.createNewFile();
							// System.out.println("Created(" + created + "): " + file.getAbsolutePath());
						}
					}
				}
			} catch (IOException ioe) {
				// Ignore, just don't do anything
			}
		}
	}

	/**
	 * Loads value from given file in which data is stored line-wise as:<br>
	 * key=value
	 * 
	 * @param key key that a value is stored under
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String load(final String key) throws FileNotFoundException, IOException {
		final String compositeKey = key + "=";
		try (BufferedReader brIn = new BufferedReader(new FileReader(this.path))) {
			boolean found = false;
			String line = null;
			while ((line = brIn.readLine()) != null) {
				if (line.startsWith(compositeKey)) {
					return line.substring(compositeKey.length());
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return path;
	}
}
