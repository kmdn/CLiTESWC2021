$(document).ready(function() {
	$('#type').multiselect();
	$('#matching').multiselect();
	$('#dataset').multiselect();
	$('#knowledge-base').multiselect();

	$("#addStandardAnnotator").on("click", function () {
		var newRow = $('<div class="standardAnnotator card col-md-8 text-center">');
		var cols = '';

		cols += '<div class="card-body">';
			cols += '<h5 class="card-title">Standard Annotator</h5>';
			cols += '<div class="card-text form-group row">';
				cols += '<div class="col-md-12">';
					cols += '<select class="annotator" multiple="multiple">';
						cols += '<option value="AIDA">AIDA</option>';
						cols += '<option value="Babelfy">Babelfy</option>';
						cols += '<option value="DBpedia Spotlight" selected="selected">DBpedia Spotlight</option>';
						cols += '<option value="Dexter">Dexter</option>';
						cols += '<option value="Entityclassifier.eu NER">Entityclassifier.eu NER</option>';
						cols += '<option value="FOX">FOX</option>';
						cols += '<option value="FRED">FRED</option>';
						cols += '<option value="FREME NER">FREME NER</option>';
						cols += '<option value="Kea">Kea</option>';
						cols += '<option value="NERD-ML">NERD-ML</option>';
						cols += '<option value="OpenTapioca">OpenTapioca</option>';
						cols += '<option value="TagMe 2">TagMe 2</option>';
						cols += '<option value="WAT">WAT</option>';
						cols += '<option value="xLisa-NER">xLisa-NER</option>';
						cols += '<option value="xLisa-NGRAM">xLisa-NGRAM</option>';
					cols += '</select>';
				cols += '</div>';
			cols += '</div>';
			cols += '<div class="form-group">';
				cols += '<button class="btn btn-danger my-remove" type="button">Remove</button>'
			cols += '</div>';
		cols += '</div>';

		newRow.append(cols);
		$("#annotators").append(newRow);

		$('.annotator').multiselect();

		$(".my-remove").on("click", function (event) {
			$(this).closest(".standardAnnotator").remove();
		});

	});

	$("#addCustomAnnotator").on("click", function () {
		var newRow = $('<div class="customAnnotator card col-md-8 text-center">');
		var cols = '';

		cols += '<div class="card-body">';
			cols += '<h5 class="card-title">Custom Annotator</h5>';
			cols += '<div class="card-text form-group row">';
				cols += '<label class="col-md-4 control-label" for="annotator-md">Mention Detection</label>';
				cols += '<div class="col-md-4">';
					cols += '<select class="annotator-md">';
						cols += '<option value="AIDA">AIDA</option>';
						cols += '<option value="Babelfy">Babelfy</option>';
					cols += '</select>';
				cols += '</div>';
			cols += '</div>';
			cols += '<div class="form-group row">';
				cols += '<label class="col-md-4 control-label" for="annotator-cg">Candidate Generation</label>';
				cols += '<div class="col-md-4">';
					cols += '<select class="annotator-cg">';
						cols += '<option value="AIDA">AIDA</option>';
						cols += '<option value="Babelfy">Babelfy</option>';
					cols += '</select>';
				cols += '</div>';
			cols += '</div>';
			cols += '<div class="form-group row">';
				cols += '<label class="col-md-4 control-label" for="annotator-ed">Entity Disambiguation</label>';
				cols += '<div class="col-md-4">';
					cols += '<select class="annotator-ed">';
						cols += '<option value="AIDA">AIDA</option>';
						cols += '<option value="Babelfy">Babelfy</option>';
					cols += '</select>';
				cols += '</div>';
			cols += '</div>';
			cols += '<div class="form-group">';
				cols += '<button class="btn btn-danger my-remove" type="button">Remove</button>'
			cols += '</div>';
		cols += '</div>';

		newRow.append(cols);
		$("#annotators").append(newRow);

		$('.annotator-md').multiselect();
		$('.annotator-cg').multiselect();
		$('.annotator-ed').multiselect();

		$(".my-remove").on("click", function (event) {
			$(this).closest(".customAnnotator").remove();
		});
	});

});

