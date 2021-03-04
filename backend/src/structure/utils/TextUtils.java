package structure.utils;

import java.io.IOException;

import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.EnglishStemmer;

public class TextUtils {

	private static final char arrow_start = '<';
	private static final char arrow_end = '>';
	private static final char doubleQuote = '"';
	private static final char atSign = '@';

	/**
	 * Stems given input by applying the following delimiters: ".,;: "
	 * 
	 * @param input input to tokenize and stem
	 * @return tokenized version of input
	 * @throws IOException
	 */
	public static String stem(String input) {
		return stem(input, "[\\p{Punct}\\p{Space}]");
	}

	/**
	 * Stems given input (split by delimiters) tokens
	 * 
	 * @param input      to stem
	 * @param regexDelim delimiters by which to tokenize
	 * @return stemmed (concatenated) version of input
	 * @throws IOException
	 */
	public static String stem(String input, final String regexDelim) {
		final SnowballProgram stemmer = new EnglishStemmer();
		final StringBuilder sbRet = new StringBuilder();
		final String[] tokens = input.split(regexDelim);

		if (tokens.length > 0) {
			stemmer.setCurrent(tokens[0]);
			stemmer.stem();
			sbRet.append(stemmer.getCurrent());
		}
		for (int i = 1; i < tokens.length; ++i) {
			stemmer.setCurrent(tokens[i]);
			stemmer.stem();
			final String stem = stemmer.getCurrent();
			sbRet.append(" ");
			sbRet.append(stem);
		}

		return sbRet.toString();
	}

	public static String smallText(String text) {
		final int length = 50;
		return smallText(text, length);
	}

	public static String smallText(String text, int length) {
		final StringBuilder sb = new StringBuilder(text.substring(0, Math.min(text.length(), length)));
		if (text.length() > length) {
			sb.append("[...] (" + text.length() + ")");
		}
		return sb.toString();
	}

	/**
	 * Stip arrows signs if they are in the beginning and the end of the entity
	 * 
	 * @param entity that may or may not be surrounded by &lt; and &gt; signs
	 * @return input in case there are no &lt; and &gt; signs surrounding it,
	 *         otherwise the string without the first character and without the last
	 *         one (in comparison to the input string)
	 */
	public static String stripArrowSigns(final String line) {
		final int lastOffset = line.length() - 1;
		if (line.charAt(0) == arrow_start && line.charAt(lastOffset) == arrow_end) {
			return line.substring(1, lastOffset);
		} else {
			return line;
		}
	}

	/**
	 * Strips quotes and language.</br>
	 * 1. call {@link #stripLangTag(String)} 2. call
	 * {@link #stripSurroundingQuotes(String)}
	 * 
	 * @param line line with potentially a language tag and quotes preceding it
	 * @return string of line without quotes and language tag
	 */
	public static String stripQuotesAndLang(final String line) {
		return stripLangTag(stripSurroundingQuotes(line));
	}

	/**
	 * Removes quotes if they are in the beginning and in the end
	 * 
	 * @param line
	 * @return
	 */
	public static String stripSurroundingQuotes(final String line) {
		String processedLine = line;
		final int lastOffset = processedLine.length() - 1;
		/*
		 * (lastAtSign - (lastDoubleQuote = line.lastIndexOf(doubleQuote))) == 1:
		 * meaning that the last @ sign is one character after the last " character
		 */
		/*
		 * lastOffset - lastAtSign + 1 < 4: Meaning that only a language definition of
		 * length less than 4 is allowed
		 */

		if (processedLine.charAt(0) == doubleQuote && processedLine.charAt(lastOffset) == doubleQuote) {
			return processedLine.substring(1, lastOffset);
		} else {
			return processedLine;
		}
	}

	/**
	 * Strips @&lt;lang&gt; sign at the end of string (assuming it is preceded by
	 * double quotes)
	 * 
	 * @param line
	 * @return
	 */
	public static String stripLangTag(String line) {
		final int langLength = 8;
		String processedLine = line;
		final int lastOffset = processedLine.length() - 1;
		final int lastAtSign;
		final int lastDoubleQuote;
		if ((lastAtSign = processedLine.lastIndexOf(atSign)) != -1// there is an at sign!
				&& (lastAtSign - (lastDoubleQuote = processedLine.lastIndexOf(doubleQuote))) == 1// they appear
																									// consecutively,
																									// aka. "@, e.g.
																									// "@en
				&& (lastDoubleQuote != -1)// there is a double quote!
				&& lastOffset - lastAtSign + 1 < langLength)// after the @ sign, there are less than langLength
															// characters left, example for a FAILING example would be:
															// "abc"@123456
		{
			// Remove language tag
			processedLine = processedLine.substring(0, lastAtSign);
		}
		return processedLine;
	}

}
