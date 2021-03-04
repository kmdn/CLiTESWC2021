<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html ng-app="pipelineConfig">
<head>
<meta charset="UTF-8">
<title>CLiT</title>

<!-- jQuery -->
<script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>

<!-- Bootstrap -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js"></script>
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css">
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js"></script>
<link rel="stylesheet" href="css/bootstrap-multiselect.css">
<script src="js/bootstrap-multiselect.js"></script>

<!-- Cytoscape -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.17.1/cytoscape.min.js"></script>
<script src="https://unpkg.com/klayjs@0.4.1/klay.js"></script>
<script src="js/cytoscape-klay.js"></script>

<link rel="stylesheet" href="css/style.css">
<script src="js/script.js"></script>
</head>

<body class="container">

	<nav class="navbar navbar-default" role="navigation">
		<div class="container-fluid">
		</div>
	</nav>

	<h1>CLiT Experiment Configuration</h1>

	<form>
		<fieldset>

			<legend>New Experiment</legend>
			
			<div class="form-group row">
				<label class="col-md-4 control-label" for="inputText">Input text</label>
				<div class="col-md-8">
					<textarea class="form-control" id="inputText">Napoleon was the emperor of the First French Empire.</textarea>
				</div>
			</div>

			<!--
			<div class="form-group row">
				<label class="col-md-4 control-label" for="datasets">Dataset</label>
				<div class="col-md-4">
					<select id="datasets" multiple="multiple" disabled>
						<option value="ACE2004">ACE2004</option>
						<option value="AIDA/CoNLL-Complete">AIDA/CoNLL-Complete</option>
						<option value="AIDA/CoNLL-Test A">AIDA/CoNLL-Test A</option>
						<option value="AIDA/CoNLL-Test B">AIDA/CoNLL-Test B</option>
						<option value="AIDA/CoNLL-Training">AIDA/CoNLL-Training</option>
						<option value="AQUAINT">AQUAINT</option>
						<option value="DBpediaSpotlight">DBpediaSpotlight</option>
						<option value="Derczynski IPM NEL">Derczynski IPM NEL</option>
						<option value="N3-RSS-500">N3-RSS-500</option>
						<option value="N3-Reuters-128">N3-Reuters-128</option>
						<option value="OKE 2018 Task 1 training dataset">OKE 2018 Task 1 training dataset</option>
						<option value="OKE 2018 Task 2 training dataset">OKE 2018 Task 2 training dataset</option>
						<option value="OKE 2018 Task 4 training dataset">OKE 2018 Task 4 training dataset</option>
					</select>
				</div>
			</div>
			-->
			
			<!-- 
			<div class="form-group row">
				<label class="col-md-4 control-label" for="type">Experiment Type</label>
				<div class="col-md-4">
					<select id="experimentType">
						<option value="A2KB">A2KB</option>
						<option value="C2KB">C2KB</option>
						<option value="D2KB">D2KB</option>
						<option value="ERec">Entity Recognition</option>
						<option value="ETyping">Entity Typing</option>
						<option value="OKE_Task1">OKE Challenge 2015 - Task 1</option>
						<option value="OKE_Task2">OKE Challenge 2015 - Task 2</option>
						<option value="RT2KB">RT2KB</option>
						<option value="RE">RE</option>
						<option value="OKE2018Task4">OKE2018Task4</option>
					</select>
				</div>
			</div>
			
			<div class="form-group row">
				<label class="col-md-4 control-label" for="matching">Matching</label>
				<div class="col-md-4">
					<select id="matching">
						<option value="WEAK_ANNOTATION_MATCH" selected="selected">Weak annotation match</option>
						<option value="STRONG_ANNOTATION_MATCH">Strong annotation match</option>
					</select>
				</div>
			</div>
			-->

			<div class="row">
				<div class="col-md-12 col-md-offset-2">
					<hr>
				</div>
			</div>

			<div class="form-group row">
				<label class="col-md-12 control-label" for="linker">Linker</label>
			</div>
			<div class="form-group">
				<div id="linker"></div>

				<button id="addStandardLinker" class="btn btn-primary" type="button">Add Standard Linker</button>
				<button id="addSimplePipeline" class="btn btn-primary" type="button">Add Linker Pipeline</button>
				<button id="addComplexPipeline" class="btn btn-primary" type="button">Add Complex Linker Pipeline</button>
			</div>

			<!--
			<div class="row">
				<div class="col-md-8 col-md-offset-2">
					<hr>
				</div>
			</div>

			<div class="form-group row">
				<label class="col-md-4 control-label" for="knowledge-base">Knowledge Base</label>
				<div class="col-md-4">
					<select id="knowledgeBases" multiple="multiple">
						<option value="Wikipedia">Wikipedia</option>
						<option value="Wikidata">Wikidata</option>
						<option value="DBpedia">DBpedia</option>
						<option value="YAGO">YAGO</option>
					</select>
				</div>
			</div>
			-->

			<div class="row">
				<div class="col-md-12 col-md-offset-2">
					<hr>
				</div>
			</div>

			<div class="form-group">
				<label class="col-md-4 control-label" for="submit"></label>
				<div id="submitField" class="col-md-8">
					<input type="button" id="submit" name="singlebutton" class="btn btn-primary" value="Run Experiment">
				</div>
			</div>
		</fieldset>
	</form>
	
	<pre id="result"></pre>
	
	<script type="text/javascript">
		$(document).ready(function() {
			setup();
		});
	</script>
	

</body>
</html>

