package launcher.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.util.Strings;

import com.beust.jcommander.internal.Lists;

import linking.disambiguation.linkers.BabelfyLinker;
import linking.disambiguation.linkers.DBpediaSpotlightLinker;
import linking.disambiguation.linkers.OpenTapiocaLinker;
import linking.mentiondetection.exact.HashMapCaseInsensitive;
import structure.datatypes.Mention;
import structure.datatypes.MentionBabelfy;
import structure.datatypes.MentionDBpediaSpotlight;
import structure.datatypes.MentionOpenTapioca;
import structure.datatypes.PossibleAssignment;
import structure.interfaces.FeatureStringable;
import structure.linker.AbstractLinkerURL;
import structure.linker.Linker;
import structure.utils.LinkerUtils;

public class LauncherCoNLLBuildFeatures {

	private static final String outDir = "C:/Users/wf7467/Desktop/Evaluation Datasets/CoNLL_2011_Orchestration";

	private final static Map<String, String> classToWekaMappings = new HashMapCaseInsensitive<String>();

	private static final Set<String> toIgnore = new HashSet<>();

	private static final Set<String> classKeeper = new HashSet<>();

	private final static boolean OUTPUT_ONLY_SINGLE_CLASSES = true;

	public static void main(String[] args) {
		classToWekaMappings.put("class java.lang.Double", "numeric");
		classToWekaMappings.put("class java.lang.String", "string");
		classToWekaMappings.put("class java.lang.Integer", "integer");
		toIgnore.add("NIL");
		toIgnore.add("");
		toIgnore.add("-1337.0");
		toIgnore.add("-1337");
		for (Object o : LinkerUtils.defaultList) {
			toIgnore.add(o.toString());
		}

		run();
	}

	private static void run() {
		final List<Linker> linkers = Lists.newArrayList();
		final List<String> missingDocs = Lists.newArrayList();

		linkers.add(new DBpediaSpotlightLinker());
		linkers.add(new BabelfyLinker());
		linkers.add(new OpenTapiocaLinker());

		final int startDoc = 1, stopDoc = 2000;
		final File tsvInFile = new File(LauncherCoNLLTSVAnnotation.tsvInPath);

		if (!tsvInFile.exists()) {
			System.err.println(
					"FATAL - Could not find TSV input file (required for input texts to BabelfyLinker.textToMentions(...) and possibly others) at location: "
							+ tsvInFile.getAbsolutePath());
			return;
		}

		final Map<String, String> results = new HashMap<>();
		final List<String> inTexts = LauncherCoNLLTSVAnnotation.parseTSV(tsvInFile, startDoc, stopDoc, results);

		final Comparator<Mention> mentionOffsetComparator = new Comparator<Mention>() {
			@Override
			public int compare(Mention o1, Mention o2) {
				if (o1.getOffset() != o2.getOffset()) {
					// Order by start offset if they are not equal
					return o1.getOffset() - o2.getOffset();
				} else {
					// If they have the same start offset, order by end offset
					// first come the small words
					return o1.getMention().length() - o2.getMention().length();
				}
			}
		};

		final File outVectorFile = new File(outDir + "/" + "outVector.txt");
		final List<String> featureTypes = Lists.newArrayList();
		try (final BufferedWriter bw = new BufferedWriter(new FileWriter(outVectorFile))) {
			final int endCond = Math.min(stopDoc, inTexts.size() + startDoc);
			for (int docCounter = startDoc; docCounter < endCond; ++docCounter) {
				System.out.println("Document#" + docCounter);
				// -startDoc in case the first one actually represents one that is actually a
				// lot further
				final String inText = inTexts.get(docCounter - startDoc);
				System.out.println("Input text #" + docCounter + " (list item#" + (docCounter - startDoc) + "):"
						+ inText.substring(0, Math.min(inText.length(), 50)) + " (...)");
				final TreeMap<String, List<Mention>> linkerMentionMap = new TreeMap<>();
				for (int i = 0; i < linkers.size(); ++i) {
					final Linker linker = linkers.get(i);
					// A list of mentions per linker, sort them by offset + length of mention
					final String annotatedText = getContents(linker, docCounter);
					if (annotatedText != null && annotatedText.length() > 0) {
						// Read in file
						if (linker instanceof AbstractLinkerURL) {
							// Transform the read text to a collection of mentions
							// Read it in, apply text to w/e to their specific mentions
							final AbstractLinkerURL urlLinker = ((AbstractLinkerURL) linker);
							urlLinker.setText(inText);
							final Collection<Mention> mentions = urlLinker.textToMentions(annotatedText);
							final List<Mention> mentionList = Lists.newArrayList(mentions);
							// Sort mentions by offsets for the merge-logic
							Collections.sort(mentionList, mentionOffsetComparator);
							linkerMentionMap.put(linker.getClass().getName(), mentionList);
						} else {
							System.err.println(
									"Not an AbstractLinkerURL instance... (cannot apply specific textToMentions() transformation)");
						}
					} else {

						System.err.println("No text to process found for [doc#" + docCounter + "] an linker["
								+ linker.getClass() + "]");
						missingDocs.add("doc[" + docCounter + "]: " + linker.getClass());
					}
				}
				// Map is populated with mentions for this document, now merge them together
				final TreeSet<String> keys = new TreeSet<>(linkerMentionMap.keySet());
				// Map with iterator for linker-based mention progression
				final TreeMap<String, Iterator<Mention>> iteratorMap = new TreeMap<>();
				for (String key : keys) {
					final ListIterator<Mention> iterator = linkerMentionMap.get(key).listIterator();
					iteratorMap.put(key, iterator);
				}

				final Map<String, Mention> currentElements = new TreeMap<>();
				// final Map<String, Integer> linkerVectorSizes = new HashMap<>();
				while (iteratorMap.size() > 0) {
					int minStartOffset = Integer.MAX_VALUE;
					int minEndOffset = Integer.MAX_VALUE;
					final Set<String> minimumMentionKeys = new HashSet<>();
					// Left to right to empty iteratorMap if one has reached the end or initialise
					// the currTopElements if they haven't yet
					for (Iterator<Map.Entry<String, Iterator<Mention>>> it = iteratorMap.entrySet().iterator(); it
							.hasNext();) {
						final Map.Entry<String, Iterator<Mention>> entry = it.next();
						final Iterator<Mention> itMentions = entry.getValue();

						final Mention linkerCurrentMention = currentElements.get(entry.getKey());

						if (!itMentions.hasNext()) // && linkerMention == null)
						{
							// 0. "Nothing new" to do here, so skip to next linker
							// 1. LinkerMention should never be null unless there really isn't anything in
							// it... so just skip to next
							// 2. Reached end for one linker's mentions --> remove its iterator
							it.remove();
							continue;
						} else if (linkerCurrentMention == null) {
							// no currently top element for this map
							// -> we're at the beginning, so populate it
							final Mention mention = itMentions.next();
							currentElements.put(entry.getKey(), mention);

							// initialise how many entries should be added for this linker's vector
//							if (mention instanceof FeatureStringable) {
//								linkerVectorSizes.put(entry.getKey(),
//										LinkerUtils.toFeatureString(((FeatureStringable) mention)).size());
//							}

							// then go to next
						}

						// Define the minimum mention(s) for this iteration to be processed
						if (linkerCurrentMention != null) {
							// Finds minimum
							if (minStartOffset > linkerCurrentMention.getOffset()) {
								// Current minimum is STRICTLY greater, so we have found a new minimum!
								// --> adjust list of minimums (delete existing minimums and set current offset
								// as minimal)
								minStartOffset = linkerCurrentMention.getOffset();
								minEndOffset = linkerCurrentMention.getOffset()
										+ linkerCurrentMention.getMention().length();
								minimumMentionKeys.clear();
								minimumMentionKeys.add(entry.getKey());
							} else if (minStartOffset == linkerCurrentMention.getOffset()) {
								// now we need to check the end offset
								final int mentionEndOffset = linkerCurrentMention.getOffset()
										+ linkerCurrentMention.getMention().length();
								if (minEndOffset == mentionEndOffset) {
									// Add it to list so we can merge afterwards (if there is no smaller minimum
									// afterwards)
									// Add it to list and don't change minimum b/c it doesn't change
									minimumMentionKeys.add(entry.getKey());
								} else if (mentionEndOffset < minEndOffset) {
									// current minimum is larger than we are - SO WE ARE BETTER! LET'S ADD OURSELVES
									// AND OVERWRITE MINIMUM
									// will be processed at a later iteration
									minEndOffset = mentionEndOffset;
									minimumMentionKeys.clear();
									minimumMentionKeys.add(entry.getKey());
									continue;
								} else // if (mentionEndOffset > minEndOffset)
								{
									// This strongly depends on the comparator's logic for the sorting, but either
									// first take small ones - which is the case now - or first the longer ones
									// current minimum is smaller than we are, so we skip / ignore
									continue;
								}
							}
						}
					}
					// currTopElements are initialised and iteratorMap only contains iterators which
					// still have elements

					// Now go through currTopElements, find the "earliest" elements and advance
					// those by one

					// Use or update the current top mentions
					// Use aka. "Merge":
					// --> always "advance" the one that is lagging behind
					// If both have same offset and same SF length --> go to next to see if you can
					// find one that would fit
					// The ones w/ the same start offset and end offsets --> merge and remove from
					// currTopElements map
					// Then add next element to currTopElements map
					final StringBuilder sbFeatures = new StringBuilder();
					boolean addedMentionToOutputStuff = false;
					final List<Object> combinedFeatures = Lists.newArrayList();
					final List<FeatureStringable> aggregateFeatureRequests = Lists.newArrayList();
					Integer startOffset = null;
					String surfaceForm = null;

					for (Map.Entry<String, Iterator<Mention>> entry : iteratorMap.entrySet()) {
						if (minimumMentionKeys.contains(entry.getKey())) {
							// current entry's pointer is at a minimum right now
							// so let's get it and get its features
							final Mention mention = currentElements.get(entry.getKey());
							if (!addedMentionToOutputStuff) {
								// Just add general mention stuff to the beginning of the vector
								final List<Object> mentionFeatures = LinkerUtils.getMentionFeatures(docCounter, inText,
										mention);
								combinedFeatures.addAll(mentionFeatures);
								surfaceForm = mention.getOriginalMention();
								startOffset = mention.getOffset();
								addedMentionToOutputStuff = true;
							}

							// and let's add features for this linker
							if (mention instanceof FeatureStringable) {
								final FeatureStringable featureStringable = (FeatureStringable) mention;
								// Write out the feature string
								// features.addAll(LinkerUtils.toFeatures(featureStringable));//
								// featureStringable.toFeatureString();
								aggregateFeatureRequests.add(featureStringable);
							} else {
								System.err.println("Mention[" + mention + "] is not a FeatureStringable");
								// create a new feature string for mentions
								// this allows for recovery of feature vector (dimensions) when sth has gone
								// wrong in terms of mention creation
								// features.addAll(LinkerUtils.toFeatures(null));
								aggregateFeatureRequests.add(null);
							}

							// and move the pointer forward
							currentElements.put(entry.getKey(), entry.getValue().next());
						} else {
							aggregateFeatureRequests.add(null);
						}
					}

					// Add wanted entities
//					System.out.println(
//							"Doc Counter[" + docCounter + "] offset[" + startOffset + "] sf[" + surfaceForm + "]");
					final String resultKey = LauncherCoNLLTSVAnnotation.makeResultKey(docCounter, startOffset,
							surfaceForm);
//					System.out.println("Result key: " + resultKey);
					final String res = results.get(resultKey);
					final String wantedEntity = res == null ? "N/A" : res;
//					if (res != null)
//					{
//						System.out.println("Match for entity! "+res);
//					}

					// Fill it up till it's full for all linkers
					for (int i = aggregateFeatureRequests.size(); i < keys.size(); ++i) {
						aggregateFeatureRequests.add(null);
					}

					if (aggregateFeatureRequests != null && aggregateFeatureRequests.size() > 0) {
						boolean allNull = (aggregateFeatureRequests.get(0) == null);

						for (int i = 1; i < aggregateFeatureRequests.size(); ++i) {
							final Object o = aggregateFeatureRequests.get(i);
							allNull &= (o == null);
						}

						if (!allNull) {
							if (!addedMentionToOutputStuff) {
								throw new RuntimeException("Haven't added mention stuff to the beginning");
							}
							// Removes mention stuff from beginning... TODO: REMOVE THIS LINE FOR THEM TO BE
							// OUTPUT
							// combinedFeatures.clear();
							final String processedWantedEntity = wantedEntity.replace("http://en.wikipedia.org/wiki/",
									"");
							final TreeSet<String> correctLinkers = new TreeSet<>();
							;
							for (FeatureStringable fs : aggregateFeatureRequests) {
								if (fs == null) {
									continue;
								} else if (fs instanceof MentionBabelfy) {
									final MentionBabelfy mention = (MentionBabelfy) fs;
									final boolean hasEntity = checkIfHasCorrectEntity(processedWantedEntity, mention);
									if (hasEntity) {
										final String wantedLinker = linkerClassToWekaClass(BabelfyLinker.class);
										correctLinkers.add(wantedLinker);
										classKeeper.add(wantedLinker);
										if (OUTPUT_ONLY_SINGLE_CLASSES) {
											break;
										}
									}
								} else if (fs instanceof MentionOpenTapioca) {
									final MentionOpenTapioca mention = (MentionOpenTapioca) fs;
									final boolean hasEntity = checkIfHasCorrectEntity(processedWantedEntity, mention);
									if (hasEntity) {
										final String wantedLinker = linkerClassToWekaClass(OpenTapiocaLinker.class);
										correctLinkers.add(wantedLinker);
										classKeeper.add(wantedLinker);
										if (OUTPUT_ONLY_SINGLE_CLASSES) {
											break;
										}
									}

								} else if (fs instanceof MentionDBpediaSpotlight) {
									final MentionDBpediaSpotlight mention = (MentionDBpediaSpotlight) fs;
									final boolean hasEntity = checkIfHasCorrectEntity(processedWantedEntity, mention);
									if (hasEntity) {
										final String wantedLinker = linkerClassToWekaClass(
												DBpediaSpotlightLinker.class);
										correctLinkers.add(wantedLinker);
										classKeeper.add(wantedLinker);
										if (OUTPUT_ONLY_SINGLE_CLASSES) {
											break;
										}
									}
								} else {
									System.err.println("No such mention[" + fs.getClass() + "] yet accepted...");
								}
							}

							final StringBuilder sbToPredict = new StringBuilder();
							for (String correctLinker : correctLinkers) {
								sbToPredict.append(correctLinker);
							}
							if (correctLinkers.size() == 0) {
								sbToPredict.append("NONE");
							}

							List<Object> features = LinkerUtils.toFeatures(aggregateFeatureRequests);
							if (sbToPredict.length() == 0) {
								throw new RuntimeException("ERROR - Empty prediction...");
							}
							features.add(sbToPredict.toString());
							combinedFeatures.addAll(features);
							// combinedFeatures.add(sbToPredict.toString());
							sbFeatures.append(LinkerUtils.featuresToStr(combinedFeatures));
							checkConsistency(docCounter, combinedFeatures, featureTypes);
							String strFeatures = sbFeatures.toString();
							// Remove last comma if there is one
							if (strFeatures.endsWith(",")) {
								// remove last character
								strFeatures = strFeatures.substring(0, strFeatures.length() - 1);
							}
							bw.write(strFeatures);
							bw.write(Strings.LINE_SEPARATOR);
							bw.flush();
						}

					} else {
						combinedFeatures.clear();
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Read the outVectorFile, process the lines and output

		// If the token is not NIL --> output it in the form of: INDEX VALUE

		final String outPathFeatures = "orchestration_features.arff";
		final File outFileFeatures = new File(outDir + "/" + outPathFeatures);
		readVectorFileOutputARFF(linkers, featureTypes, outVectorFile, outFileFeatures);

		// Output missing documents
		System.out.println("Missing documents[" + missingDocs.size() + "]:");
		for (String doc : missingDocs) {
			System.out.println(doc);
		}
	}

	private static boolean checkIfHasCorrectEntity(final String wantedEntity, Mention mention) {
		if (mention.getAssignment() != null && sameEntity(mention.getAssignment().getAssignment(), wantedEntity)) {
			return true;
		}
		for (PossibleAssignment poss : mention.getPossibleAssignments()) {
			if (sameEntity(poss.getAssignment(), wantedEntity)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * TODO: Check for sameAs links here
	 * 
	 * @param assignment
	 * @param processedWantedEntity
	 * @return
	 */
	private static boolean sameEntity(String assignment, String processedWantedEntity) {
		if (assignment.equalsIgnoreCase(processedWantedEntity)) {
			return true;
		}
		if (assignment.contains(processedWantedEntity)) {
			return true;
		}
		return false;
	}

	private static void readVectorFileOutputARFF(final List<Linker> linkers, final List<String> featureTypes,
			File outVectorFile, File outFileFeatures) {
		try (final BufferedReader br = new BufferedReader(new FileReader(outVectorFile));
				final BufferedWriter bwFeatures = new BufferedWriter(new FileWriter(outFileFeatures))) {
			// Write the preamble w/ the feature types etc
			System.out.println("Types of features:");
			int typeCounter = 1;
			
			final String relation = "@RELATION agnos.orchestration";
			System.out.println(relation);
			bwFeatures.write(relation);
			bwFeatures.newLine();
			bwFeatures.newLine();

			
			for (String type : featureTypes) {
				final String attribute = "@ATTRIBUTE feature-" + typeCounter++ + "\t" + translateToWekaType(type);
				System.out.println(attribute);
				bwFeatures.write(attribute);
				bwFeatures.newLine();
			}
			final String classLabelStart = "@ATTRIBUTE CLASS_LABEL\t{NONE,";
			System.out.print(classLabelStart);
			bwFeatures.write(classLabelStart);

			// Output the class for supervised learning
			if (OUTPUT_ONLY_SINGLE_CLASSES) {
				final String firstLinkerStr = linkerClassToWekaClass(linkers.get(0).getClass());
				System.out.print(firstLinkerStr);
				bwFeatures.write(firstLinkerStr);
				for (int i = 1; i < linkers.size(); ++i) {
					final Linker linker = linkers.get(i);
					final String toOutput = "," + linkerClassToWekaClass(linker.getClass());
					bwFeatures.write(toOutput);
					System.out.print(toOutput);
				}
			} else {
				// Outputs all found class-combinations
				Iterator<String> classIterator = classKeeper.iterator();
				if (classIterator.hasNext()) {
					final String firstLinkerStr = classIterator.next();
					System.out.print(firstLinkerStr);
					bwFeatures.write(firstLinkerStr);
				}

				while (classIterator.hasNext()) {
					String clazz = classIterator.next();
					final String toOutput = "," + clazz;
					bwFeatures.write(toOutput);
					System.out.print(toOutput);
				}
			}

			final String classLabelEnd = "}";
			System.out.println(classLabelEnd);
			bwFeatures.write(classLabelEnd);
			bwFeatures.newLine();
			bwFeatures.newLine();
			bwFeatures.write("@DATA");
			bwFeatures.newLine();
			bwFeatures.newLine();

			// Now read through the vector, process them (remove NIL entries) and output as
			// "INDEX VALUE,"
			String line = null;
			while ((line = br.readLine()) != null) {
				bwFeatures.write(processVectorLine(line));
				bwFeatures.newLine();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String linkerClassToWekaClass(Class clazz) {
		final String[] dotSplit = clazz.getName().split("\\.");
		return dotSplit[dotSplit.length - 1];
	}

	private static String processVectorLine(String line) {
		final String[] commaTokens = line.split(",");

		final StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		for (int i = 0; i < commaTokens.length; ++i) {
			final String token = commaTokens[i];
			if (!toIgnore.contains(token)) {
				if (!first) {
					sb.append(",");
				}
				sb.append(i + " " + token);
				first = false;
			}
		}
		sb.append("}");
		return sb.toString();
	}

	private static String translateToWekaType(String type) {
		final String ret = classToWekaMappings.get(type);
		if (ret == null) {
			throw new RuntimeException("ERROR - No Type defined for '" + type + "'");
		}
		return ret;
	}

	/**
	 * Checks whether lines are consistent, throws a RuntimeException otherwise
	 * 
	 * @param combinedFeatures
	 * @param featureTypes
	 */
	private static void checkConsistency(final int docCounter, List<Object> combinedFeatures,
			List<String> featureTypes) {
		if (featureTypes.size() == 0) {
			// fill it
			for (Object feature : combinedFeatures) {
				final String featureType = // feature + "," +
						feature.getClass().toString();
				featureTypes.add(featureType);
			}
		} else {
			// else check that it is consistent
			if (combinedFeatures.size() != featureTypes.size()) {
				throw new RuntimeException("[Doc#" + docCounter + "] Inconsistent feature sizes! (Expected["
						+ featureTypes.size() + "], Found[" + combinedFeatures.size() + "]):" + Strings.LINE_SEPARATOR
						+ combinedFeatures);
			}
			for (int i = 0; i < combinedFeatures.size(); ++i) {
//				final String featureTypeInfo = featureTypes.get(i);
//				final String featureType = featureTypeInfo.split(",")[1];
				final String featureType = featureTypes.get(i);
				if (!featureType.equals(combinedFeatures.get(i).getClass().toString())) {
					throw new RuntimeException("[Doc#" + docCounter + "/Sizes(" + combinedFeatures.size()
							+ ")] Inconsistent feature types at index[" + i + "]! (Expected[" + featureType
							+ "], Found[" + combinedFeatures.get(i).getClass().toString() + ", "
							+ combinedFeatures.get(i) + "])" + Strings.LINE_SEPARATOR + "Combined Features:"
							+ Strings.LINE_SEPARATOR + combinedFeatures);
				}
			}
		}
	}

	private static String getContents(final Linker linker, final int docCounter) {
		// Read results of appropriate files
		final String extension;
		if (linker instanceof BabelfyLinker || linker instanceof DBpediaSpotlightLinker) {
			extension = "json";
		} else if (linker instanceof OpenTapiocaLinker) {
			extension = "nif";
		} else {
			extension = "unknown";
		}
		final String inPath = outDir + "/" + linker.getClass().getName().replace(".", "_") + "/" + docCounter + "."
				+ extension;
		final File inFile = new File(inPath);
		// System.out.println("Infile: " + inFile.getAbsolutePath());
		String annotatedText = null;
		if (inFile.exists()) {
			try (final BufferedReader br = new BufferedReader(new FileReader(inFile))) {
				final List<String> lines = org.apache.commons.io.IOUtils.readLines(br);
				final StringBuilder sbAnnotate = new StringBuilder();
				for (String line : lines) {
					sbAnnotate.append(line);
				}
				annotatedText = sbAnnotate.toString();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return annotatedText;
	}

}
