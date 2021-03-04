package structure.utils;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class NetUtils {
	private final static Predicate<String> urlPattern = Pattern.compile("^(https?://)?" + // protocol
			"((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|" + // domain name
			"((\\d{1,3}\\.){3}\\d{1,3}))" + // OR ip (v4) address
			"(:\\d+)?(/[-a-z\\d%_.~+]*)*" + // port and path
			"(\\?[;&a-z\\d%_.~+=-]*)?" + // query string
			"(\\#[-a-z\\d_]*)?$", 'i').asPredicate(); // fragment locater

	public static boolean isIRI(final String urlToTest) {
		boolean ret = urlPattern.test(urlToTest);
		// System.out.println("isIRI("+urlToTest+"): "+ret);
		return ret;
	}
}
