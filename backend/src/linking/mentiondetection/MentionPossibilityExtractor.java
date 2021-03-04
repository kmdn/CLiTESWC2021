package linking.mentiondetection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.util.NxUtil;

import structure.config.constants.Strings;
import structure.config.kg.EnumModelType;
import structure.utils.Loggable;
import structure.utils.TextUtils;

/**
 * This class extracts literals and maps them as Map<Literal, Set<Source>> The
 * point is to be able to give the MentionDetector an input map to match against
 * text occurrences.
 * 
 * @author Kwizzer
 *
 */
public class MentionPossibilityExtractor implements Loggable {
	private final int DEFAULT_MIN_LENGTH_THRESHOLD = 1;// 0 pretty much means 'no threshold'
	private final int DEFAULT_MAX_LENGTH_THRESHOLD = 50;// 0 pretty much means 'no threshold'
	private int lengthMinThreshold = DEFAULT_MIN_LENGTH_THRESHOLD;
	private int lengthMaxThreshold = DEFAULT_MAX_LENGTH_THRESHOLD;
	private final Set<String> blackList;
	private final HashMap<String, Collection<String>> mentionPossibilities = new HashMap<>();
	private final String delim = Strings.ENTITY_SURFACE_FORM_LINKING_DELIM.val;

	public MentionPossibilityExtractor(final EnumModelType KG) {
		Set<String> stopwords = null;
		try {
			stopwords = new StopwordsLoader(KG).getStopwords();
		} catch (IOException e) {
			stopwords = null;
		}
		if (stopwords == null) {
			this.blackList = new HashSet<String>();
		} else {
			this.blackList = stopwords;
		}
	}

	public MentionPossibilityExtractor(final StopwordsLoader stopwordsLoader) throws IOException {
		this(stopwordsLoader.getStopwords());
	}

	public MentionPossibilityExtractor(final Set<String> stopwords) {
		this.blackList = stopwords;
	}

	public void populateBlacklist(final File inFile) throws IOException {
		try (BufferedReader brIn = Files.newBufferedReader(Paths.get(inFile.getPath()))) {
			String line = null;
			while ((line = brIn.readLine()) != null) {
				blackList.add(line);
			}
		}
	}

	public void dumpBlacklist(final File outFile) throws IOException {
		try (BufferedWriter bwOut = Files.newBufferedWriter(Paths.get(outFile.getPath()), StandardOpenOption.WRITE)) {
			for (String word : blackList) {
				bwOut.write(word);
				bwOut.newLine();
			}
		}
	}

	public void blacklist(final Collection<String> stopwords) {
		this.blackList.addAll(stopwords);
	}

	public void blacklist(final String word) {
		this.blackList.add(word);
	}

	/**
	 * Let's look at the entity file and get everything that we can link back to
	 * them
	 * 
	 * @param inFile
	 * @return
	 * @throws IOException
	 */
	public Map<String, Collection<String>> addPossibilities(final File inFile) throws IOException {
		try (final BufferedReader brIn = new BufferedReader(new FileReader(inFile), 8192 * 100)) {
			processFile(brIn);
		}
		return mentionPossibilities;
	}

	/**
	 * General processing for a file, choose whether it's a specific file for
	 * linking or simply a N3
	 * 
	 * @param brIn
	 * @throws IOException
	 */
	private void processFile(BufferedReader brIn) throws IOException {
		processFileEntitySurfaceFormLinking(brIn);
	}

	/**
	 * Specific file having entities and their respective surface forms (usually
	 * output by querying a KG)
	 * 
	 * @param brIn
	 * @throws IOException
	 */
	private void processFileEntitySurfaceFormLinking(final BufferedReader brIn) throws IOException {
		boolean nxparsing = false;
		if (!nxparsing) {
			plainStringParsing(brIn);
		} else {
			nxparsing(brIn);
		}

	}

	private void plainStringParsing(BufferedReader brIn) throws IOException {
		String line = null;
		final boolean ONLY_STRING = true;
		if (ONLY_STRING) {
			// Assumes that it's just plain strings and no RDF fanciness
			final boolean ignoreLowerCaseSpacedWords = true;
			while ((line = brIn.readLine()) != null) {
				final String[] tokens = line.split(delim);
				if (tokens.length == 2) {
					addPossibility(mentionPossibilities, tokens[1], tokens[0]);
					addSpacedPossibilities(mentionPossibilities, tokens[1], tokens[0], ignoreLowerCaseSpacedWords);
				} else if (tokens.length == 3) {
					addPossibility(mentionPossibilities, tokens[2], tokens[0]);
					addSpacedPossibilities(mentionPossibilities, tokens[2], tokens[0], ignoreLowerCaseSpacedWords);
				} else if (tokens.length != 0) {
					getLogger().error("Invalid line...: " + line);
				}
			}

		} else {
			// Makes RDF Nodes out of the strings
			while ((line = brIn.readLine()) != null) {
				final String[] tokens = line.split(delim);
				if (tokens.length == 2) {
					mentionPossibility(tokens[0], "<link>", tokens[1]);
					// addPossibility(mentionPossibilities, tokens[1], tokens[0]);
				} else if (tokens.length == 3) {
					// addPossibility(mentionPossibilities, tokens[2], tokens[0]);
					mentionPossibility(tokens[0], tokens[1], tokens[2]);
				} else if (tokens.length != 0) {
					getLogger().error("Invalid line...: " + line);
				}
			}
		}
	}

	/**
	 * Parses the two-columns file with NxParser and a bit of circumventing by
	 * adding an unused token (so it still looks like triples to the NxParser)
	 * 
	 * @param brIn
	 */
	private void nxparsing(BufferedReader brIn) {
		final Iterator<String> bufferIterator = new Iterator<String>() {
			final StringBuilder ret = new StringBuilder();
			final StringBuilder tokenBuilder = new StringBuilder();
			String[] currLine = new String[] { "" };
			final String dummyPredicate = "<link>";
			// predicate position reosurce is missing
			final int missingResourceIndex = 1;

			@Override
			public boolean hasNext() {
				return currLine != null;
			}

			@Override
			public String next() {
				if (hasNext()) {
					ret.setLength(0);
					tokenBuilder.setLength(0);

					String line;
					try {
						line = brIn.readLine();
					} catch (IOException e) {
						line = null;
					}
					if (line != null) {
						currLine = line.split(delim);
					} else {
						currLine = null;
						return null;
					}

					for (int i = 0; i < currLine.length; ++i) {
						if (i != currLine.length - 1) {
							// For any token that's not the last
							final String token = currLine[i];
							final boolean isLiteral = token.contains("^^http://") || token.contains("^^<http://");
							if (!token.startsWith("<") && !isLiteral) {
								// Does not start w/ < and is not a literal
								tokenBuilder.append("<");
							}
							// add the actual token
							tokenBuilder.append(token);

							if (!token.endsWith(">") && !isLiteral) {
								tokenBuilder.append(">");
							}
							tokenBuilder.append(" ");
						} else {
							// For the last one (should be a literal)
							final String token = currLine[i];
							final int literalTypeIndex = token.indexOf("^^http://");
							final int literalTypeLtIndex = token.indexOf("^^<http://");
							final int atEnglishLanguage = token.indexOf("@en");
							// String is missing starting quotes
							if (!token.startsWith("\"")) {
								tokenBuilder.append("\"");
							}

							if (!token.endsWith("\"")) {
								// String is missing ending quote
								if (atEnglishLanguage != -1) {
									// Put the quote before the @en
									tokenBuilder.append(token.substring(0, atEnglishLanguage));
									tokenBuilder.append("\"");
									tokenBuilder.append(token.substring(atEnglishLanguage, token.length()));
								} else if (literalTypeIndex != -1) {
									// Put the quote before the string definition of ^^http:// etc
									// but add < and > around the string definition
									tokenBuilder.append(token.substring(0, literalTypeIndex));
									tokenBuilder.append("\"^^<");
									tokenBuilder.append(token.substring(literalTypeIndex + 2, token.length()));
									tokenBuilder.append(">");
								} else if (literalTypeLtIndex != -1) {
									// Put the quote before the string definition of ^^<http:// etc
									tokenBuilder.append(token.substring(0, literalTypeLtIndex));
									tokenBuilder.append("\"");
									tokenBuilder.append(token.substring(literalTypeLtIndex, token.length()));
								} else {
									// Put the quote at the end of line simply
									tokenBuilder.append(token);
									tokenBuilder.append("\"");
								}
							} else {
								// String has an ending quote already, so no need to add one
								tokenBuilder.append(token);
							}
							tokenBuilder.append(" ");
						}
						if (i == missingResourceIndex) {
							// You're at the position you need to introduce a new resource into, so add it
							// to the actual return value already
							ret.append(dummyPredicate);
							ret.append(" ");
						}
						ret.append(tokenBuilder.toString());
						tokenBuilder.setLength(0);
					}
					ret.append(".");
					return ret.toString();
				} else {
					// Should be handled by the above logic and hasNext() is resolved as currLine !=
					// null anyway, but just in case logic somehow changes it up
					currLine = null;
					return null;
				}
			}
		};
		// Making use of NxParser to remove String definition etc
		NxParser parser = new NxParser(new ArrayList<String>() {
			@Override
			public Iterator<String> iterator() {
				return bufferIterator;
			}
		});

		final StringBuilder sb = new StringBuilder();
		while (parser.hasNext()) {
			Node[] triple = parser.next();
			if (triple.length == 3) {
				mentionPossibility(triple[0], triple[1], triple[2]);
			} else {
				sb.setLength(0);
				for (Node n : triple) {
					sb.append(n.toString());
				}
				getLogger().warn("Weird triple length(" + triple.length + "): " + sb.toString());
			}
		}

	}

	private void addSpacedPossibilities(HashMap<String, Collection<String>> mentionPossibilities, String words,
			String source) {
		addSpacedPossibilities(mentionPossibilities, words, source, false);
	}

	/**
	 * Splits the passed 'word' (line or some such) by white space characters in
	 * hopes of helping to add surface forms that can be detected.<br>
	 * Note: This helps in cases in which a surface form might be defined as "Brad
	 * Pitt", but there not existing separate "Brad" and "Pitt" surface forms (e.g.
	 * for incomplete graphs not adding :firstName / :lastName additionally to just
	 * :name)
	 * 
	 * @param mentionPossibilities map containing surface forms along with their
	 *                             respective possible entities
	 * @param words                words/line
	 * @param source               entity which points towards this surface form
	 */
	private void addSpacedPossibilities(HashMap<String, Collection<String>> mentionPossibilities, String words,
			String source, final boolean ignoreLowercase) {
		// Add pattern Matcher for proper processing
		// final String[] multiWordTokens = words.split("\\p{Space}");
		final String[] multiWordTokens = InputProcessor.processToSingleWords(words);
		if (multiWordTokens.length > 1) {
			for (String word : multiWordTokens) {
				final String firstLetter = String.valueOf(word.charAt(0));
				if (ignoreLowercase && firstLetter.equals(firstLetter.toLowerCase())) {
					continue;
				}
				addPossibility(mentionPossibilities, word, source);
			}
		}
	}

	/**
	 * NTriples type of input file
	 * 
	 * @param brIn
	 * @throws IOException
	 */
	private void processFileNTriples(Reader brIn) throws IOException {
		final NxParser parser = new NxParser(brIn);
		int lineCounter = 0;
		final PrintStream syserr = System.err;
		System.setErr(new PrintStream(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				// Ignore
			}
		}));
		try {
			while (parser.hasNext()) {
				// Process the entity line
				final Node[] triple;
				try {
					triple = parser.next();
				} catch (StringIndexOutOfBoundsException sioobe) {
					continue;
				}
				mentionPossibility(triple[0], triple[1], triple[2]);
				lineCounter++;
				if (lineCounter % 10_000 == 0) {
					System.out.println("Processed lines: " + lineCounter);
				}
			}
		} catch (Exception e) {
		} finally {
			System.setErr(syserr);
		}
	}

	/**
	 * Populates the map one entry at a time
	 * 
	 * @param map    map to be populated
	 * @param word   word to be added to the map
	 * @param entity what the word belongs to
	 */
	private void addPossibility(final Map<String, Collection<String>> map, String word, String entity) {
		// word = Normalizer.normalize(word,
		// Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
		// .toLowerCase();
		// source = source;// .toLowerCase();
		if (!passesRequirements(entity, word))
			return;
		final String cleanedWord = TextUtils.stripQuotesAndLang(word.trim()).trim();

		Collection<String> s;
		if ((s = map.get(cleanedWord)) == null) {
			s = new HashSet<String>();
			map.put(cleanedWord, s);
		}
		s.add(TextUtils.stripArrowSigns(entity.trim()).trim());
	}

	/**
	 * Checks whether it passes threshold and blacklist requirements
	 * 
	 * @param word the word that should be added as a possibly found mention
	 * @return whether it passes threshold and blacklist requirements
	 */
	private boolean passesRequirements(String entity, String word) {
		boolean ret = ((word != null) && (word.length() > lengthMinThreshold) && (word.length() < lengthMaxThreshold)
				&& (blackList != null && (!blackList.contains(word))));
		// Remove all kinds of lists... they're a pain
		ret &= !entity.contains("List");
		ret &= !entity.contains("Category:");
		return ret;
	}

	public void setMinLenThreshold(final int minLen) {
		this.lengthMinThreshold = minLen;
	}

	/**
	 * Add the possibility if it fits what we want
	 */
	public void mentionPossibility(String s, String p, String o) {
		try {
			int endIndex = o.indexOf("^^http://");
			final String cleanedO;
			if (endIndex == -1) {
				cleanedO = NxUtil.escapeForMarkup(o);
			} else {
				cleanedO = NxUtil.escapeForMarkup(o.substring(0, endIndex));
			}
			mentionPossibility(new Resource(s, true), new Resource(p, true), new Literal(cleanedO));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("s:" + s);
			System.out.println("p:" + p);
			System.out.println("o:" + o);
		}
		// if (RDFNodeUtils.isTypedLiteral(o)) {
		// addPossibility(mentionPossibilities,
		// RDFNodeUtils.stripLiteralQuotesAndType(o), s);
		// }
	}

	public void mentionPossibility(Node s, Node p, Node o) {
		// <http://www.w3.org/1999/02/22-rdf-syntax-ns#label>
		if (o instanceof org.semanticweb.yars.nx.Literal) {
//			final boolean addPoss;
//			if (p instanceof org.semanticweb.yars.nx.Resource
//			// && ((org.semanticweb.yars.nx.Resource) p).toString().equals(labelURI)
//			) {
			addPossibility(mentionPossibilities, o.toString(), s.toString());
//			}
		}
	}
}
