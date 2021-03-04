package experiment;

import java.util.Arrays;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import linking.pipeline.combiner.IntersectCombiner;
import linking.pipeline.combiner.UnionCombiner;
import linking.pipeline.interfaces.Combiner;
import linking.pipeline.interfaces.Filter;
import linking.pipeline.interfaces.Splitter;
import linking.pipeline.interfaces.Translator;
import linking.pipeline.splitter.CopySplitter;
import structure.config.kg.EnumModelType;
import structure.interfaces.CandidateGeneratorDisambiguator;
import structure.interfaces.pipeline.AnnotationPipeline;
import structure.interfaces.pipeline.CandidateGenerator;
import structure.interfaces.pipeline.Disambiguator;
import structure.interfaces.pipeline.FullAnnotator;
import structure.interfaces.pipeline.MentionDetector;

public class AnnotationPipelineBuilder {

	private final JSONObject annotationPipelineConfig;
	private final EnumModelType knowledgeBase;
	private final Map<String, String> linkerClasses;

	public AnnotationPipelineBuilder(JSONObject annotationPipelineConfig,
			EnumModelType knowledgeBase, Map<String, String> linkerClasses) {
		this.annotationPipelineConfig = annotationPipelineConfig;
		this.knowledgeBase = knowledgeBase;
		this.linkerClasses = linkerClasses;
	}

	/**
	 * Read linker configurations from JSON and create a list annotation pipelines
	 * @param annotationPipelineConfig
	 * @return List of StandardLinkerConfig and CustomLinkerConfig objects
	 */
	public AnnotationPipeline buildAnnotationPipeline() {
		AnnotationPipeline annotationPipeline = new AnnotationPipeline(); 

        String pipelineType = (String) annotationPipelineConfig.get("pipelineType");

        if (pipelineType.equals("standard")) {
        	String linkerName = (String) annotationPipelineConfig.get("linker");
			FullAnnotator annotator = null;
			String linkerClassName = linkerClasses.get(linkerName);
			try {
				annotator = (FullAnnotator) Class.forName(linkerClassName)
						.getDeclaredConstructor(EnumModelType.class)
						.newInstance(knowledgeBase);
				annotationPipeline.addMD_CG_ED("md_cg_ed1", annotator);
			} catch (ClassCastException e) {
				//TODO exception handling
				System.err.println("Error: Annotator could not be initialized");
				e.printStackTrace();
			} catch (Exception e) {
				//TODO exception handling
				e.printStackTrace();
			}
        	
        } else if (pipelineType.equals("custom")) {
        	String mentionDetectorName = (String) annotationPipelineConfig.get("mentionDetector");
        	String candidateGeneratorDisambiguatorName = (String) annotationPipelineConfig
        			.get("candidateGeneratorDisambiguator");
			MentionDetector mentionDetector = null;
			CandidateGeneratorDisambiguator candidateGeneratorDisambiguator = null;
			String mentionDetectorClassName = linkerClasses.get(mentionDetectorName);
			String candidateGeneratorDisambiguatorClassName = linkerClasses.get(
					candidateGeneratorDisambiguatorName);

			try {
				mentionDetector = (MentionDetector) Class.forName(mentionDetectorClassName)
						.getDeclaredConstructor(EnumModelType.class)
						.newInstance(knowledgeBase);
				candidateGeneratorDisambiguator = (CandidateGeneratorDisambiguator) Class
						.forName(candidateGeneratorDisambiguatorClassName)
						.getDeclaredConstructor(EnumModelType.class)
						.newInstance(knowledgeBase);
			} catch (ClassCastException e) {
				//TODO exception handling
				System.err.println("Error: Annotator components could not be initialized");
				e.printStackTrace();
			} catch (Exception e) {
				//TODO exception handling
				e.printStackTrace();
			}

			annotationPipeline.addMD("md1", mentionDetector);
			annotationPipeline.addCG_ED("md_cg1", candidateGeneratorDisambiguator);
			annotationPipeline.addConnection("md1", "md_cg1");

        } else if (pipelineType.equals("complex")) {
			annotationPipeline = readComplexPipelineConfig(annotationPipelineConfig);

        } else {
        	System.err.println("Warning: Invalid linker type (" + pipelineType + "), skipping");
        }

		return annotationPipeline;
	}

	/**
	 * Transform JSON into Pipeline object
	 * @param linkerConfig
	 * @param knowledgeBase
	 * @return
	 */
	private AnnotationPipeline readComplexPipelineConfig(JSONObject linkerConfigJson) {

		AnnotationPipeline pipeline = new AnnotationPipeline();

		for (String pipelineItemStr : Arrays.asList("md", "cg", "ed", "cg_ed", "combiner", "splitter",
				"translator", "filter", "interactions", "connections")) {
			JSONArray itemArray = (JSONArray) linkerConfigJson.get(pipelineItemStr);

			if (itemArray == null || itemArray.size() == 0) {
				System.out.println("Info: No component of type '" + pipelineItemStr + "' specified");
			} else {
				for (Object itemObj : itemArray) {
					if (itemObj instanceof JSONObject) {
						JSONObject itemJson = (JSONObject) itemObj;
						pipeline = readComplexPipelineConfigObject(knowledgeBase, pipeline, pipelineItemStr, itemJson);
					}
				}
			}
		}
		return pipeline;
	}

	/**
	 * Translates an entry from the complex pipeline config into an pipeline item and adds it to the pipeline
	 * @param knowledgeBase
	 * @param pipeline
	 * @param pipelineItemStr
	 * @param itemJson
	 * @return 
	 */
	private AnnotationPipeline readComplexPipelineConfigObject(EnumModelType knowledgeBase,
			final AnnotationPipeline pipeline, String pipelineItemStr, JSONObject itemJson) {
		// Read ID and name of the component
		//TODO nicer way than doing this in a loop with a break?
		String keyStr = null;
		Object value = null;
		for (Object key : itemJson.keySet()) {
			keyStr = (String) key;
			value = itemJson.get(keyStr); // TODO was tun für interactions? (JSONArray != String)
			break;
		}

		if (value != null) {
			// MD, CG, ED
			if (Arrays.asList("md", "cg", "ed", "cg_ed").contains(pipelineItemStr)) {
				String valueStr = (String) value;
				String className = linkerClasses.get(valueStr);
				try {
					switch (pipelineItemStr) {
					case "md":
						MentionDetector mentionDetector = (MentionDetector) Class.forName(className)
								.getDeclaredConstructor(EnumModelType.class)
								.newInstance(knowledgeBase);
						pipeline.addMD(keyStr, mentionDetector);
						System.out.println("Info: Added mention detector '" + valueStr + "' with ID '" + keyStr + "'");
						break;
					case "cg":
						CandidateGenerator candidateGenerator = (CandidateGenerator) Class.forName(className)
								.getDeclaredConstructor(EnumModelType.class)
								.newInstance(knowledgeBase);
						pipeline.addCG(keyStr, candidateGenerator);
						System.out.println("Info: Added candidate generator '" + valueStr + "' with ID '" + keyStr + "'");
						break;
					case "ed":
						Disambiguator disambiguator = (Disambiguator) Class.forName(className)
								.getDeclaredConstructor(EnumModelType.class)
								.newInstance(knowledgeBase);
						pipeline.addED(keyStr, disambiguator);
						System.out.println("Info: Added entity disambiguator '" + valueStr + "' with ID '" + keyStr + "'");
						break;
					case "cg_ed":
						CandidateGeneratorDisambiguator candidateGeneratorDisambiguator = (CandidateGeneratorDisambiguator) Class.forName(className)
								.getDeclaredConstructor(EnumModelType.class)
								.newInstance(knowledgeBase);
						pipeline.addCG_ED(keyStr, candidateGeneratorDisambiguator);
						System.out.println("Info: Added candidate generator disambiguator '" + valueStr + "' with ID '" + keyStr + "'");
						break;
					}
				} catch (ClassCastException e) {
					//TODO exception handling
					System.err.println("Error: " + valueStr + " cannot be used as " + pipelineItemStr);
				} catch (Exception e) {
					//TODO exception handling
					e.printStackTrace();
				}
			}
			// Inter-component processors
			else if (Arrays.asList("combiner", "splitter", "translator", "filter").contains(pipelineItemStr)) {
				String valueStr = (String) value;
				try {
					// TODO also use reflection here?
					switch (pipelineItemStr) {
					case "combiner":
						Combiner combiner = null;
						if ("union".equalsIgnoreCase(valueStr)) {
							combiner = new UnionCombiner();
						} else if ("intersection".equalsIgnoreCase(valueStr)) {
							combiner = new IntersectCombiner();
						} else {
							// TODO exception handling
							System.err.println("Warning: Skipping invalid combiner type with key '" + keyStr + "'");
						}
						pipeline.addCombiner(keyStr, combiner);
						System.out.println("Info: Added combiner '" + valueStr + "' with ID '" + keyStr + "'");
						break;
					case "splitter":
						Splitter splitter = null;
						if ("copy".equalsIgnoreCase(valueStr)) {
							splitter = new CopySplitter();
						} else {
							// TODO exception handling
							System.err.println("Warning: Skipping invalid splitter type with key '" + keyStr + "'");
						}
						pipeline.addSplitter(keyStr, splitter);
						System.out.println("Info: Added splitter '" + valueStr + "' with ID '" + keyStr + "'");
						break;
					case "translator":
						// TODO implement back-end
						System.err.println("Warning: Skipping translator, not implemented yet");
						Translator translator = null;
//										if ("WD_TO_DBP".equalsIgnoreCase(className)) {
//											
//										} else {
//											// TODO exception handling
//											System.out.println("Warning: Skipping invalid translator type with key '" + keyStr + "'");
//										}
						pipeline.addTranslator(keyStr, translator);
//										System.out.println("Info: Added translator '" + valueStr + "' with ID '" + keyStr + "'");
						break;
					case "filter":
						// TODO implement back-end
						System.err.println("Warning: Skipping filter, not implemented yet");
						Filter filter = null;
//										if ("TYPE_PERSON".equalsIgnoreCase(className)) {
//											
//										} else {
//											// TODO exception handling
//											System.out.println("Warning: Skipping invalid filter type with key '" + keyStr + "'");
//										}
						pipeline.addFilter(keyStr, filter);
//										System.out.println("Info: Added filter '" + valueStr + "' with ID '" + keyStr + "'");
						break;
					}
				} catch (ClassCastException e) {
					//TODO exception handling
					System.err.println("Error: '" + valueStr + "' cannot be used as " + pipelineItemStr);
				} catch (Exception e) {
					//TODO exception handling
					e.printStackTrace();
				}
			}
			// Interactions and connections
			else if (Arrays.asList("interactions", "connections").contains(pipelineItemStr)) {
				try {
					// TODO also use reflection here?
					switch (pipelineItemStr) {
					case "interactions":
						System.out.println("Info: Skipping interactions (please use connections only)");
//						JSONArray targetArray = (JSONArray) value;
//						for (Object targetObj : targetArray) {
//							String targetStr = (String) targetObj;
//							pipeline.addConnection(keyStr, targetStr);
//							System.out.println("Info: Added interaction " + keyStr + " -> " + targetStr);
//						}
						break;
					case "connections":
						String targetStr = (String) value;
						pipeline.addConnection(keyStr, targetStr);
						System.out.println("Info: Added connection " + keyStr + " -> " + targetStr);
						break;
					}
				} catch (ClassCastException e) {
					//TODO exception handling
					System.err.println("Error: '" + value.toString() + "' cannot be used as " + pipelineItemStr);
				} catch (Exception e) {
					//TODO exception handling
					e.printStackTrace();
				}
			}
			else {
				// TODO exception handling
				System.err.println("Warning: Ignored invalid entry '" + value.toString() + "' in pipeline config");
			}
		} else {
			//TODO exception handling
		}
		return pipeline;
	}

}
