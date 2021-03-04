package launcher.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import dataset.EnumTSVCoNLL2011;
import linking.disambiguation.linkers.BabelfyLinker;
import linking.disambiguation.linkers.DBpediaSpotlightLinker;
import linking.disambiguation.linkers.OpenTapiocaLinker;
import structure.config.constants.Strings;
import structure.linker.Linker;

public class LauncherCoNLLTSVAnnotation {
	private static final String outDir = "C:/Users/wf7467/Desktop/Evaluation Datasets/CoNLL_2011_Orchestration";
	private static int counter = 0;
	private static BufferedWriter bwLog = null;

	public static final String tsvInPath = "C:\\Users\\wf7467\\Desktop\\Evaluation Datasets\\Datasets\\aida-yago2-dataset\\aida-yago2-dataset\\"//
			+ "AIDA-YAGO2-dataset.tsv";

	public static void main(String[] args) {
		final int startAt = 0;
		final int stopAt = 2000;
		final boolean cont = false;
		final File outLog = new File(outDir + "/" + "log.txt");
		try {
			if (!outLog.getParentFile().exists()) {
				// Make directories if parent directory does not exist
				outLog.getParentFile().mkdirs();
			}

			bwLog = new BufferedWriter(new FileWriter(outLog, true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		final int startDoc;
		final int stopDoc;
		if (cont) {
			startDoc = continueWhereLeftOff(outLog);
			stopDoc = 2_000;
		} else {
			startDoc = startAt;
			stopDoc = stopAt;
		}

		//
		System.out.println("Starting from: " + startDoc);
		// Linkers to use for annotation
		final List<Linker> linkers = Lists.newArrayList();
		linkers.add(new DBpediaSpotlightLinker());
		linkers.add(new BabelfyLinker());
		linkers.add(new OpenTapiocaLinker());

		// Input TSV file
		final File inFile = new File(tsvInPath);
		final Map<String, String> results = new HashMap<>();
		final List<String> inTexts = parseTSV(inFile, startDoc, stopDoc, results);

		// Now that we've collected the input texts, annotate them
		for (int docCounter = startDoc; docCounter < inTexts.size(); ++docCounter) {
			annotate(docCounter, inTexts.get(docCounter), linkers);
		}
	}

	
	public static String makeResultKey(final Integer docCounter, final Integer startOffset, final String surfaceForm)
	{
		return (docCounter==null?"no_doc":docCounter)+"_"+(startOffset==null?"no_offset":startOffset)+"_"+(surfaceForm==null?"no_sf":surfaceForm);
	}
	
	/**
	 * 
	 * @param inFile   TSV file with the contents
	 * @param startDoc which document to start at
	 * @param stopDoc  which document to end at
	 * @param results  where to put the results
	 * @return
	 */
	public static List<String> parseTSV(final File inFile, final int startDoc, final int stopDoc,
			Map<String, String> results) {
		// List w/ textual documents
		final List<String> ret = Lists.newArrayList();

		int docCounter = 0;

		// TSV Parsing
		final TsvParserSettings tsvSettings = new TsvParserSettings();
		final TsvParser parser = new TsvParser(tsvSettings);

		// parses all rows in one go.
		final List<String[]> rows = parser.parseAll(inFile);

		// -DOCSTART- (94 Australia)
		final String docStringKeyword = "-DOCSTART-";
		final String docStringDELIM = " ";
		final int cols = EnumTSVCoNLL2011.values().length;
		final StringBuilder sbInputText = new StringBuilder();
		final String DELIM = " ";
		// Currently populated w/ Wikipedia URLs but unused - meant to potentially check
		// later on what entities there are
		final Set<String> allEntities = new HashSet<>();

		// - Each document starts with a line: -DOCSTART- (<docid>)
		// - Each following line represents a single token, sentences are separated by
		// an empty line
		//
		// Lines with tabs are tokens the are part of a mention:
		// - column 1 is the token
		// - column 2 is either B (beginning of a mention) or I (continuation of a
		// mention)
		// - column 3 is the full mention used to find entity candidates
		// - column 4 is the corresponding YAGO2 entity (in YAGO encoding, i.e. unicode
		// characters are backslash encoded and spaces are replaced by underscores, see
		// also the tools on the YAGO2 website), OR --NME--, denoting that there is no
		// matching entity in YAGO2 for this particular mention, or that we are missing
		// the connection between the mention string and the YAGO2 entity.
		// - column 5 is the corresponding Wikipedia URL of the entity (added for
		// convenience when evaluating against a Wikipedia based method)
		// - column 6 is the corresponding Wikipedia ID of the entity (added for
		// convenience when evaluating against a Wikipedia based method - the ID refers
		// to the dump used for annotation, 2010-08-17)
		// - column 7 is the corresponding Freebase mid, if there is one (thanks to
		// Massimiliano Ciaramita from Google Zürich for creating the mapping and making
		// it available to us)
		String[] oldDocLineSplit = new String[] {};
		String oldDocID = oldDocLineSplit.length >= 2 ? oldDocLineSplit[1].replace("(", "") : "N/A";
		String oldDocDomain = oldDocLineSplit.length >= 3 ? oldDocLineSplit[2].replace(")", "") : "N/A";

		for (String[] row : rows) {
			final int rowLength = row.length;
			switch (rowLength) {
			case 0:
				// finish or empty line
				sbInputText.append(DELIM);
				continue;
			case 1:
				// Either only a string (no entity etc.) or "-DOCSTART- (ID DOMAIN)"
				// Process
				final String firstCol = row[0];

				if (firstCol.startsWith(docStringKeyword)) {
					// We found a new document!
					// So let's annotate what we've gathered thus far
					// It's a new sentence, so send the previous sentence out to the linkers
					final String[] newDocLineSplit = firstCol.split(docStringDELIM);
					final String newDocID = newDocLineSplit.length >= 2 ? newDocLineSplit[1].replace("(", "") : "N/A";
					final String newDocDomain = newDocLineSplit.length >= 3 ? newDocLineSplit[2].replace(")", "")
							: "N/A";

					if (sbInputText.length() > 0 && docCounter >= startDoc && docCounter <= stopDoc) {
						// System.out.println("Input document " + oldDocID + " - " + oldDocDomain);
						ret.add(sbInputText.toString());
					} else {
						// Skipping
					}
					oldDocLineSplit = newDocLineSplit;
					oldDocID = newDocID;
					oldDocDomain = newDocDomain;
					docCounter++;
					// Reset buffer due to new document
					sbInputText.setLength(0);
				} else {
					// It's a simple word without an associated entity, so just add it with a
					// delimiter for the next word
					sbInputText.append(row[0]);
					sbInputText.append(DELIM);
				}
				break;
			default:
				// It's a line with an entity!
				final String wikipediaURL = rowLength > 4 ? row[4] : null;
				final String wikipediaID = rowLength > 5 ? row[5] : null;
				final String freebaseMID = rowLength > 6 ? row[6] : null;
				
				final int startOffset = sbInputText.toString().length();
				final String resultKey = makeResultKey(docCounter, startOffset, row[0]);
				results.put(resultKey, wikipediaURL);
				sbInputText.append(row[0]);
				sbInputText.append(DELIM);
				// Add the wikipediaURL in case it is useful for evaluation later on
				allEntities.add(wikipediaURL);
				break;
			}
		}
		// Annotate the last one
		if (sbInputText.length() > 0 && docCounter >= startDoc && docCounter <= stopDoc) {
			ret.add(sbInputText.toString());
		}

		return ret;
	}

	private static int continueWhereLeftOff(final File outLog) {
		if (!outLog.exists()) {
			return 0;
		}

		try (final BufferedReader brIn = new BufferedReader(new FileReader(outLog))) {
			String line = null;
			String prevLine = line;
			while ((line = brIn.readLine()) != null) {
				prevLine = line;
			}

			// Read number from this line
			return Integer.parseInt(prevLine, 10);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return 0;
		}
	}

	private static void annotate(final int docCounter, final String input, final List<Linker> linkers) {
		counter++;
		System.out.println("Annotating(Doc#" + docCounter + " (#" + counter + " for this run)");
		System.out.println(input);
		for (Linker linker : linkers) {
			String annotatedText = null;
			try {
				// retries until it works or failed too often
				annotatedText = annotate(input, linker, 0);
			} catch (InterruptedException e) {
				System.err.println("ERROR - Thread.sleep issue");
				e.printStackTrace();
			}
			outputText(docCounter, annotatedText, linker);
		}

		// Wait a bit to avoid spamming them too much...
		try {
			final long sleepTime = 150l + new Random(System.currentTimeMillis()).nextInt(2_000);
			System.out.println("Sleeping between requests for: " + sleepTime);
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String annotate(String input, Linker linker, int attemptCounter) throws InterruptedException {
		String annotatedText = null;
		final int MAX_ATTEMPTS = 20;
		String errorMsg = "", smallErrorMsg = "";
		try {
			annotatedText = linker.annotate(input);
		} catch (Exception ioe) {
			annotatedText = null;
			final StringWriter errors = new StringWriter();
			ioe.printStackTrace(new PrintWriter(errors));
			errorMsg = errors.toString();
			smallErrorMsg = errorMsg.substring(0, Math.min(errorMsg.length(), 80));
			// System.out.println("ERROR MESSAGE: "+errorMsg);
			// IOException
		} finally {
			if (annotatedText == null) {
				if (attemptCounter < MAX_ATTEMPTS) {
					if (errorMsg.contains("response code: 414")) {
						// Text too long
						System.out.println("Error Code 414 - URL too long... -> Skipping to next");
					} else {
						System.err.println("ERROR MSG: " + smallErrorMsg + " (...)");
						System.out.println(
								"[" + linker.getClass().getName() + "] Failed attempt [" + attemptCounter + "]");
						// Let's retry...
						final long waitTime = 3_000l * (attemptCounter + 1);
						System.out.println("Waiting for [" + waitTime + "ms.]");
						Thread.sleep(waitTime);
						annotatedText = annotate(input, linker, attemptCounter + 1);
					}
				} else {
					System.err.println("Too many failed attempts... [" + attemptCounter + "]");
				}

			}
		}
		return annotatedText;

	}

	/**
	 * Outputs the annotatedtext to a file based on the document counter
	 * 
	 * @param docCounter
	 * @param annotatedText
	 * @param linker
	 */
	private static void outputText(int docCounter, String annotatedText, Linker linker) {
		final String extension;
		if (linker instanceof BabelfyLinker || linker instanceof DBpediaSpotlightLinker) {
			extension = "json";
		} else if (linker instanceof OpenTapiocaLinker) {
			extension = "nif";
		} else {
			extension = "unknown";
		}
		final String outPath = outDir + "/" + linker.getClass().getName().replace(".", "_") + "/"
				+ (annotatedText == null ? "_error_" : "") + docCounter + "." + extension;
		final File outFile = new File(outPath);
		if (!outFile.getParentFile().exists()) {
			// Make directories if parent directory does not exist
			outFile.getParentFile().mkdirs();
		}
		try (final BufferedWriter bwOut = new BufferedWriter(new FileWriter(outFile))) {
			if (annotatedText == null) {
				// No annotations, so don't write anything
				return;
			}
			bwOut.write(annotatedText);
			// Log which documents were annotated and output to a file
			if (bwLog != null) {
				try {
					bwLog.write("" + docCounter);
					bwLog.write(Strings.NEWLINE.val);
					bwLog.flush();
				} catch (IOException e1) {
					System.err.println("Error while logging");
					e1.printStackTrace();
				}
			}
		} catch (IOException e) {
			System.err.println("Exception w/ document #" + docCounter + " (File: " + outPath + ")");
			e.printStackTrace();
		}
	}

}
