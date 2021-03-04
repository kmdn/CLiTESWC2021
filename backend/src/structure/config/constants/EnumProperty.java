package structure.config.constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Enumeration for loading properties from predefined constant files
 * @author Kristian Noullet
 *
 */
public enum EnumProperty {

	// ##################################
	// # User Data (i.e. basic client authentication)
	// ##################################
	//AUTHENTICATE_TAGTOG(Strings.ROOTPATH.val + "config/tagtog.properties"), //
	//AUTHENTICATE_TAGTOG_TESTING(Strings.ROOTPATH.val + "config/tagtog_testing.properties"), //
	//AUTHENTICATE_BABELFY(Strings.ROOTPATH.val + "config/babelfy.properties"), //
	AUTHENTICATE_VIRTUOSO(Strings.ROOTPATH.val + "config/virtuoso.properties"),//
	;

	private final String path;

	EnumProperty(final String path) {
		this.path = path;
	}

	/**
	 * Queries this property file for the specified keyword
	 * 
	 * @param keyword
	 * @return
	 */
	public char[] get(final String keyword) {
		try (InputStream is = new FileInputStream(new File(this.path))) {
			final Properties prop = new Properties();
			prop.load(is);
			// Long call-chain for fail-fast behaviour
			char[] ret = prop.getProperty(keyword).toCharArray();
			// A bit overkill, but as prop.clear just sets the Entry<?,?> values to null, it
			// is still up to the GC, so rather explicitly overwrite it
			prop.setProperty(keyword, "");
			prop.clear();
			return ret;
		} catch (IOException e) {
			// Likely a FNFE from a sub-optimally set-up project environment
			e.printStackTrace();
		} catch (NullPointerException npe) {
			throw new IllegalArgumentException("Invalid value requested. (" + keyword + ") for " + this.path);
		}
		// Shouldn't be reached as it either defaults to the try-block or should throw a
		// NPE if no valid value is found
		return null;
	}

}
