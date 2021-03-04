package install;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;

import com.google.common.collect.Lists;

import structure.config.constants.FilePaths;
import structure.config.kg.EnumModelType;
import structure.utils.Loggable;

/**
 * Load a specified knowledge base into a Jena dataset that we can query
 * henceforth
 * 
 * @author Kris
 *
 */
public class LauncherSetupTDB implements Loggable {
	private static List<String> abortedList = Lists.newArrayList();

	public static void main(String[] args) {
		// new LauncherSetupTDB().exec();

		// Load the knowledge graph into the appropriate dataset
		final EnumModelType KG = EnumModelType.
		// MINI_MAG
		// DBLP
		// DBPEDIA
		// CRUNCHBASE2
		// MAG
				CRUNCHBASE
		// DEFAULT
		// DBPEDIA_FULL
		// WIKIDATA//
		;
		System.out.println("Setting up TDB for KG[" + KG.name() + "]");
		final String KGpath =
				// WIKIDATA
				//"/vol2/wikidata/dumps/20190213/wikidata-20190213-truthy-BETA_all_URI-obj.nt"//
				
			"/vol2/cb/crunchbase-201806/dumps/crunchbase-dump-2018-06_sanitized.nt"
		;
		// final String KGpath = FilePaths.FILE_KNOWLEDGE_GRAPH.getPath(KG);//
		// "";
		// "/vol2/cb/crunchbase-201510/dumps/crunchbase-dump-201510.nt";//CB2015
		// "/vol2/cb/crunchbase-201806/dumps/crunchbase-dump-2018-06_sanitized.nt";//
		// CB2018
		// "./cb2018-06/crunchbase-dump-2018-06.nt";//NORMALIZED_CB2
		// "/home/faerberm/inRDF-URI-as-obj/";// MAG
		// "/vol2/kris/PaperReferences_o.nt";// MAG PaperReferences_o.nt
		// "/vol2/dblp/dumps/dblp_2018-11-02_unique.nt";//DBLP
		// "./dblp_kg/dblp_2018-11-02_unique.nt";//NORMALIZED_DBLP
		// "/vol1/mag/data/2018-07-19/MAGFieldsOfStudyKG/MAGFieldsOfStudyKG.nt";//Mini-MAG
		// "./crunchbase-dump-2018-06_normalized.nt";// normalized CB2
		// "./dblp_2018-11-02_unique_normalized.nt";// normalized DBLP
		// "/vol1/data_faerberm/kris/data_dbpedia_extracted";// DBpedia
		// "/home/noulletk/prog/bmw/dbpedia/resources/data/datasets/extracted/";//
		// DBpedia
		// "/home/noulletk/prog/bmw/input_dbpedia/";// DBpedia
		// Handle appropriately both for input file (just load it)
		// and input directory (get all files within it, aka. ignore subdirectories)
		// "/vol2/wikidata/dumps/20190213/wikidata-20190213-truthy-BETA_all_URI-obj.nt";
		final File inFile = new File(KGpath);
		final List<String> inFiles = Lists.newArrayList();
		if (inFile.isDirectory()) {
			// Just takes files from the first level, does NOT go deeper if a directory is
			// contained within specified directory
			for (File f : inFile.listFiles()) {
				if (f.isFile()) {
					inFiles.add(f.getAbsolutePath());
				}
			}
		} else {
			inFiles.add(inFile.getAbsolutePath());
		}
		// Execute the loading part...
		for (String kgInPath : inFiles) {
			System.out.println("Source(" + (inFiles.indexOf(kgInPath) + 1) + "/" + inFiles.size() + "): " + kgInPath);
			new LauncherSetupTDB().exec(KG, kgInPath);
			System.out.println("Aborted (" + abortedList.size() + "): " + abortedList);
		}

		System.out.println("Aborted files(" + abortedList.size() + "): " + abortedList);
		// Set up for other

	}

	/**
	 * Loads all KGs from FilePaths.FILE_EXTENDED_GRAPH into their respective
	 * datasets
	 * 
	 */
	private void exec() {
		// Choose for which KG to load it into the TDB
		// final EnumModelType KG = EnumModelType.MAG;
		for (EnumModelType KG : EnumModelType.values()) {
			final String KGpath = FilePaths.FILE_KNOWLEDGE_GRAPH.getPath(KG);
			// Read a line to make sure it is not an empty file we are trying to load
			try (BufferedReader br = new BufferedReader(new FileReader(KGpath))) {
				// Read first line to check whether it's an empty file!
				if (br.readLine() == null) {
					// Skip this file if it's empty
					getLogger().info("Skipping " + KG.name() + " due to empty file.");
					continue;
				} else {
					// Process file if it's not empty
					getLogger().info("Loading " + KG.name());
					exec(KG, KGpath);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads a single KG into the appropriate dataset
	 * 
	 * @param KG     which graph it corresponds to
	 * @param KGpath where to load it from
	 */
	public void exec(EnumModelType KG, final String KGpath) {
		final String datasetPath = FilePaths.DATASET.getPath(KG);
		// Non-empty file
		final Dataset dataset = TDBFactory.createDataset(datasetPath);
		dataset.begin(ReadWrite.READ);
		// Get model inside the transaction
		Model model = dataset.getDefaultModel();
		dataset.end();

		// Now load it all into the Model
		dataset.begin(ReadWrite.WRITE);
		model = dataset.getDefaultModel();
		try {
			TDBLoader.loadModel(model, KGpath, true);
			// model.commit();
			dataset.commit();
		} catch (Exception e) {
			System.out.println("Aborted: " + KGpath);
			abortedList.add(KGpath);
			// model.abort();
			dataset.abort();
		} finally {
			dataset.end();
		}

	}
}
