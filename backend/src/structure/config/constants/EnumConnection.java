package structure.config.constants;

/**
 * Enumeration handling connections to different servers
 * @author Kristian Noullet
 *
 */
public enum EnumConnection {
	// Connecting for downloading / uploading annotation files
	//TAGTOG(EnumUserAccounts.TAGTOG_CONNECTION, "https", "www.tagtog.net", "/-api/documents/v1"), //
	//TAGTOG_TEST(EnumUserAccounts.TAGTOG_CONNECTION_TEST, "https", "www.tagtog.net", "/-api/documents/v1"), //
	// KIT SPARQL Endpoint
	KIT(null, "http", "km.aifb.kit.edu", "/services/crunchbase-sparql"), //
	// TUCO/CELEBES SPARQL Endpoint
	CELEBES(null, "http", "10.8.150.43:8890", "/sparql"), //
	// BABELFY has its own API, so no need for a separate connection
	SHETLAND_VIRTUOSO(EnumUserAccounts.VIRTUOSO_DBA, "", "jdbc:virtuoso://localhost:1112"),
	//Seeland and Shetland have the same accounts
	SEELAND_VIRTUOSO(EnumUserAccounts.VIRTUOSO_DBA, "", "jdbc:virtuoso://seeland.informatik.privat:1112");

	public final EnumUserAccounts userAcc;
	public final String protocol;
	public final String baseURL;
	public final String urlSuffix;
	public final String[] misc;

	private EnumConnection(EnumUserAccounts userAcc, String... connDetails) {
		this.userAcc = userAcc;
		int argC = 0;
		this.protocol = connDetails != null && connDetails.length > argC ? connDetails[argC] : null;
		argC++;
		this.baseURL = connDetails != null && connDetails.length > argC ? connDetails[argC] : null;
		argC++;
		this.urlSuffix = connDetails != null && connDetails.length > argC ? connDetails[argC] : null;
		argC++;
		this.misc = connDetails != null && connDetails.length > argC ? new String[connDetails.length - argC] : null;
		if (this.misc != null) {
			for (int i = argC; i < connDetails.length; ++i) {
				this.misc[i - argC] = connDetails[argC];
			}
		}

	}
}
