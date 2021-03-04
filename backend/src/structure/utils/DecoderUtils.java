package structure.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.semanticweb.yars.nx.util.NxUtil;

public class DecoderUtils {
	public static String combineSFormOffset(final String surface_form, int offset) {
		return combineSFormOffset(surface_form, Integer.toString(offset));
	}

	public static String combineSFormOffset(final String surface_form, String offset) {
		return surface_form + "__;__" + offset;
	}

	/**
	 * Takes a URL and adds &lt; and &gt; to beginning/end if required. If the
	 * string already has them, nothing changes.
	 * 
	 * @param url
	 * @return
	 */
	public static String urlToResourceURL(final String url) {
		String ret = url;
//		if (RDFNodeUtils.isBlankNode(url)) {
//			return ret;
//		}
		if (!url.startsWith("<")) {
			ret = "<" + ret;
		}
		if (!url.endsWith(">")) {
			ret += ">";
		}
		return ret;
	}

	/**
	 * From string to proper output (translates quotes etc.)
	 * 
	 * @param out
	 * @return
	 */
	public static String escapeForOutput(final String out) {
		return "\"" + NxUtil.escapeForMarkup(out) + "\"";
	}

	/**
	 * From potentially weird-looking output format to 'proper' string again
	 * 
	 * @param out
	 * @return
	 */
	public static String unescapeFromOutput(final String out) {
		return NxUtil.unescape(out, true);
	}
	
	public static String escapePercentage(final String textPercentageEncoded)
	{
		String decoded = null;
		try {
			decoded = URLDecoder.decode(textPercentageEncoded, "UTF8");
		} catch (UnsupportedEncodingException uee) {
			// #WeTried
			decoded = null;
		}
		return decoded;

	}

}
