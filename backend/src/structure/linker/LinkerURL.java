package structure.linker;

public interface LinkerURL extends Linker {
	static final String https = "https";
	static final String http = "http";

	/**
	 * Sets to HTTP Scheme
	 * 
	 * @return
	 */
	public LinkerURL http();

	/**
	 * Sets to HTTPS Scheme
	 * 
	 * @return
	 */
	public LinkerURL https();

	/**
	 * Sets the base URL
	 * 
	 * @param url
	 * @return
	 */
	public LinkerURL url(final String url);

	/**
	 * 
	 * @param suffix suffix to set for the URL
	 * @return
	 */
	public LinkerURL suffix(final String suffix);

	/**
	 * Set the timeout for this URL request. May be ignored by some linker while it
	 * may be required by others
	 * 
	 * @param timeout
	 * @return
	 */
	public LinkerURL timeout(final int timeout);

	/**
	 * Set the port number to connect to
	 * @param port to connect to
	 * @return chaining pattern
	 */
	public LinkerURL port(final int port);
	
	/**
	 * Sets specific parameter with given value
	 * 
	 * @param paramName  parameter name
	 * @param paramValue value of the parameter
	 * @return
	 */
	public LinkerURL setParam(final String paramName, final String paramValue);

}
