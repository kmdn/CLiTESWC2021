package structure.config.kg;

import structure.config.constants.EnumConnection;
import structure.config.constants.Strings;

/**
 * Supported Knowledge Graphs for our framework
 * 
 * @author Kristian Noullet
 *
 */
public enum EnumModelType {
	// NONE("", null), //
	// DBPEDIA(Strings.ROOTPATH.val + "dbpedia/", EntityQuery.DEFAULT), //
	DBPEDIA_FULL(Strings.ROOTPATH.val + "dbpedia_full/", EntityQuery.DEFAULT, EnumConnection.SHETLAND_VIRTUOSO, false,
			"http://dbpedia.org"), //

	// FREEBASE(Strings.ROOTPATH.val + "freebase/", EntityQuery.DEFAULT), //

	CRUNCHBASE(Strings.ROOTPATH.val + "crunchbase2018/", EntityQuery.CRUNCHBASE2), //

	// CRUNCHBASE2(Strings.ROOTPATH.val + "crunchbase2015/",
	// EntityQuery.CRUNCHBASE2), //
	// MINI_MAG(Strings.ROOTPATH.val + "mini_mag/", EntityQuery.MAG), //
	// MAG(Strings.ROOTPATH.val + "mag/", EntityQuery.MAG), //
	// DBLP(Strings.ROOTPATH.val + "dblp/", EntityQuery.DBLP), //
	WIKIDATA(Strings.ROOTPATH.val + "wikidata/", EntityQuery.WIKIDATA), //
	WORDNET(Strings.ROOTPATH.val + "wordnet/", EntityQuery.DEFAULT), //
	DEFAULT(Strings.ROOTPATH.val + "default/", EntityQuery.DEFAULT) //
	;

	public final String root;
	public final EntityQuery query;

	public final EnumConnection virtuosoConn;
	private final boolean useVirtuoso;
	public final String virtuosoGraphname;

	EnumModelType(final String folderName, final EntityQuery query) {
		this(folderName, query, null);
	}

	EnumModelType(final String folderName, final EntityQuery query, EnumConnection virtuosoConn) {
		this(folderName, query, virtuosoConn, virtuosoConn != null, null);
	}

	EnumModelType(final String folderName, final EntityQuery query, EnumConnection virtuosoConn,
			final boolean useVirtuoso, final String graphName) {
		this.root = folderName;
		this.query = query;
		this.virtuosoConn = virtuosoConn;
		this.useVirtuoso = useVirtuoso;
		this.virtuosoGraphname = graphName;
	}

	/**
	 * Whether to use Virtuoso for this KG
	 * 
	 * @return
	 */
	public boolean useVirtuoso() {
		return this.useVirtuoso && this.virtuosoConn != null;
	}

	@Override
	public String toString() {
		return name();
	}

	public String findableName() {
		return name();
	}

}
