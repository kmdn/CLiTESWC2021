# Agnos_mini
Agnos is a KG-agnostic entity linking framework, allowing for ease of extension and deployment.
The ease of extension refers both to various knowledge graphs, as well as alternative methods for mention detection, candidate generation, entity disambiguation, as well as pruning.
<br>
<h1>Quick Start Guide</h1>
<ol start="0">
<li>Clone Repository</li>
<li>
	Run install/InstallFiletree.java - as the name implies, it simply creates the file tree to make it easier to place required files.
</li>
<li>
	Add your desired RDF KG as a .NT file to the execution environment's directory under "<i>./default/resources/data/kg.nt</i>"
</li>
<li>
	Run <i>install.LauncherInstallation.java</i> - it (1) loads the placed KG into a RDF Store, (2) computes PageRank on it and (3) extracts mentions (by default with a SPARQL query querying for all rdfs:label elements).
</li>
<li>
	Setup complete!
	<!-- <br>
	Run Launcher.java to run a simple entity linking pipeline.<br>
	Mention detection and candidate generation may now be performed!<br>
	For disambiguation, the default scoring mechanisms of PageRank and VicinityScorerDirectedSparseGraph are now ready to be used. 
	-->
</li>
</ol> 

<h1>Running Agnos</h1>
Post configuration, you may run Agnos by executing <i>launcher.LauncherLinking</i>
<br>It takes a string input, applies exact case-insensitive mention detection on it, followed by candidate generation and default disambiguation behaviour. 
<br>Results are output to the console.
<br>We also provide <i>launcher.LauncherLinkingSample</i> - an easily modifiable sample on how the annotation code process looks like.


<h1>Full Startup Guide</h1>
<ol start="0">
<li>Clone Repository</li>
<li>
	Add wanted Knowledge Graph (KG) as a new enum item to <i>structure/config/kg/EnumModelType</i>
	<br>e.g. MY_KNOWLEDGE_GRAPH("./my_kg/") - henceforth we will denote the chosen KG's root path as $KG$.
	<br><b>Note</b>: This will allow 
	<ol>
		<li>
			A user/developer to specify particular configurations for a specific KG  (e.g. surface forms for mention detection, underlying caching structures etc.)
		</li>
		<li>
			Agnos to create the file tree as required by the system in the defined location, in this case under the execution's current directory in a $KG$ folder.
		</li>
		<li>
			KG isolation in order to avoid unexpected interactions on the user-side.
		</li>
	</ol>
</li>
<li>
	Run <i>install/BuildFiletree</i> - as the name implies, it simply creates the file tree to make it easier to place files appropriately
</li>
<li>
	Load KG into an RDF Store by defining the location of your RDF-based KG within <i>install.LauncherSetupTDB:KGpath</i> and running it for your defined KG (in <i>install.LauncherSetupTDB:KG</i>).
	Note: 
	<br>
	<ol>
		<li>
			If you define an input folder, all including files will be added to the Jena TDB.
		</li>
		<li>
			If you already have an existing Apache Jena(-compatible) RDF Store, simply put it into $KG$/resources/data/datasets/graph.dataset .
		</li>
	</ol>
<li> Semi-OPTIONAL
	<ol>
		<li>
			Put SPARQL Queries to be executed on loaded KG for surface form extraction into appropriate folders.
			<br>If you already have a file containing surface forms and their related resources, 
			<br>please put it in $KG$/resources/data/links_surfaceForms.txt (the filepath may be changed in <i>structure.config.FilePaths:FILE_ENTITY_SURFACEFORM_LINKING</i>.
			<br>The line-wise split delimiter may be defined under <i>structure.config.Strings:ENTITY_SURFACE_FORM_LINKING_DELIM</i>, where the resource is in first position and the defined literal in second.
		</li>
		<li>
			Define <i>install.LauncherExecuteQueries:KG</i> with the defined KG and run it. The program will extract appropriate surface forms from your defined KG, outputting them appropriately for the system to process.
		</li>
	</ol>
</li>
<li>
	Setup complete!
	Simple mention detection and candidate generation may now be performed!
	As for disambiguation, depending on which scoring scheme one would like to use, a file containing PageRank scores or embeddings may have to be defined.
	For RDF PageRank computation, we provide code under <i>install.PageRankComputer</i> which may then be loaded by disambiguation algorithms using a <i>PageRankLoader</i> from the generated $KG$/resources/data/pagerank.nt file.
</li>
</ol> 

<br>
<br><h1>API</h1>
<br>Code for NIF-format-based queries (<i>api.NIFAPIAnnotator</i>) as well as calls through JSON (<i>api.JSONAPIAnnotator</i>) are provided mainly for API-usage.
<br>A very basic API front-end page may be downloaded from <a href="https://km.aifb.kit.edu/sites/agnos-demo/">Agnos</a> and used locally.

<h1>Mention Detection</h1>
<br>Out-of-the-box Agnos provides users with 2 main mention detection mechanisms:
<ul>
	<li><i><a href="https://git.scc.kit.edu/wf7467/agnos_mini/-/tree/master/src/linking/mentiondetection/exact/MentionDetectorMap.java">linking.mentiondetection.exact.MentionDetectorMap</a></i> (Exact matching)</li>
	<li><i><a href="https://git.scc.kit.edu/wf7467/agnos_mini/-/tree/master/src/linking/mentiondetection/fuzzy/MentionDetectorLSH.java">linking.mentiondetection.fuzzy.MentionDetectorLSH</a></i> (Fuzzy matching)</li>
</ul>
<br>Former performs mention detection by checking whether a possible input is contained within a passed map instance.
<br>Latter utilizes locality-sensitive hashing techniques (MinHash), allowing detection with a user-defined grade of fuzziness.
<br>Please note that <i>linking.mentiondetection.fuzzy.MentionDetectorLSH</i> requires (surface form) structures to be computed prior to linking in order to allow for highly-scalable performance.

<h2>Custom Mention Detection</h2>
<br>Mention detection standards are enforced through structure.interfaces.MentionDetector
<br>It enforces easy-to-implement detection for the ease-of-processing of the text annotation pipeline.
<br>As such, any custom mention detection technique should simply implement it in order to warrant compliance with other steps.
<br>Therewith, e.g. consolidation of Agnos' mention detection through POS methods is relatively trivial.

<h1>Candidate Generation</h1>
<br>Agnos mainly utilizes a single candidate generation mechanism: dictionary look-up.
<br>It is implemented within <i>linking.candidategeneration.CandidateGeneratorMap</i> and can be used with a defined mapping.
<br>Custom candidate generation may be performed through implementation of the <i>structure.interfaces.CandidateGenerator</i> interface.

<h1>Disambiguation</h1>
<br>Agnos allows for simple extension of its disambiguation repertoire.
<br>Among others, through use of its <i>structure.interfaces.Scorer</i> and <i>structure.interfaces.PostScorer</i> interfaces.
<br>The difference between the two is that <i>structure.interfaces.Scorer</i> is assumed to be a so-called apriori scoring mechanism (meaning single candidate scores are independant of other candidates), whereas <i>structure.interfaces.PostScorer</i> instances attribute different scores to candidate entities, depending on other candidate entities they are detected with, therewith allowing for the notion of "context" to play a role.
<br>An example of a <i>structure.interfaces.Scorer</i> instance would be our PageRankScorer.
<br>For <i>structure.interfaces.PostScorer</i> instances, we provide <i>linking.disambiguation.scorers.GraphWalkEmbeddingScorer</i> and <i>VicinityScorerDirectedSparseGraph</i>, among others.
<br>Defining which scoring mechanisms may be used for disambiguation is configurable through the defined <i>linking.disambiguation.Disambiguator</i> instance by calling the <i>addScorer(...)</i> and <i>addPostScorer(...)</i> methods, respectively.
<br>How single scorers' scores are combined may be defined within their own implementation which is then applied through our consolidation mechanism <i>linking.disambiguation.ScoreCombiner</i>.