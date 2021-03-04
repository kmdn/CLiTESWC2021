var pipelineConfigDefault = {
	"id" : 1,
	"pipelineType" : "complex",
	"components" : {
			"md": [
				{id: "MD1", value: "Babelfy"},
				{id: "MD2", value: "DBpediaSpotlight"},
				{id: "MD3", value: "Babelfy"}
			],
			"cg": [],
			"cg_ed": [
				{id: "CG_ED1", value: "Babelfy"}
			],
			"ed": [],
			"combiner": [
				{id: "CO1", value: "Union"}
			],
			"splitter": []
	},
	"connections": [
		{source: "MD1", target: "CO1"},
		{source: "MD2", target: "CO1"},
		{source: "MD3", target: "CO1"},
		{source: "CO1", target: "CG_ED1"}
	],
	"startComponents": [
		"MD1",
		"MD2",
		"MD3"
	],
	"endComponents": [
		"CG_ED1"
	]
};

var pipelineConfigExampleB = {
		"id" : 1,
		"pipelineType" : "complex",
		"components" : {
				"md": [
					{id: "MD1", value: "Babelfy"}
				],
				"cg": [
					{id: "CG1", value: "DBpediaSpotlight"}
				],
				"cg_ed": [],
				"ed": [
					{id: "ED1", value: "Babelfy"}
				],
				"combiner": [
					{id: "TR1", value: "WIKIDATA_TO_DBPEDIA"}
				],
				"splitter": []
		},
		"connections": [
			{source: "MD1", target: "CG1"},
			{source: "CG1", target: "ED1"},
			{source: "ED1", target: "TR1"}
		],
		"startComponents": [
			"MD1"
		],
		"endComponents": [
			"TR1"
		]
	};

var pipelineConfigExampleA = {
		"id" : 1,
		"pipelineType" : "complex",
		"components" : {
				"md": [
					{id: "MD1", value: "Babelfy"},
					{id: "MD2", value: "DBpediaSpotlight"},
					{id: "MD3", value: "AIDA"}
				],
				"cg": [
					{id: "CG1", value: "Babelfy"}
				],
				"cg_ed": [],
				"ed": [
					{id: "ED1", value: "AIDA"}
				],
				"combiner": [
					{id: "CO1", value: "Union"}
				],
				"splitter": [
					{id: "SP1", value: 3}
				]
		},
		"connections": [
			{source: "SP1", target: "MD1"},
			{source: "SP1", target: "MD2"},
			{source: "SP1", target: "MD3"},
			{source: "MD1", target: "CO1"},
			{source: "MD2", target: "CO1"},
			{source: "MD3", target: "CO1"},
			{source: "CO1", target: "CG1"},
			{source: "CG1", target: "ED1"}
		],
		"startComponents": [
			"SP1"
		],
		"endComponents": [
			"ED1"
		]
	};


/**
 * Maps the names of the node types (e.g. mention detector, splitter) to their ID
 * abbreviation (e.g. MD, SP)
 */
var typeNameToTypeIdMap = {
		"md" : "MD",
		"cg" : "CG",
		"ed" : "ED",
		"cg_ed" : "CG_ED",
		"combiner" : "CO",
		"splitter" : "SP",
		"translator" : "TR",
		"filter" : "FI"
};

var typeIdToTypeNameMap = {
		"MD" : "md",
		"CG" : "cg",
		"ED" : "ed",
		"CG_ED" : "cg_ed",
		"CO" : "combiner",
		"SP" : "splitter",
		"TR" : "translator",
		"FI" : "filter"
};

var typeNameToTypeStringMap = {
		"md" : "Mention Detector",
		"cg" : "Candidate Generator",
		"cg_ed" : "Candidate Generator Disambiguator (combined)",
		"ed" : "Entity Disambiguator",
		"combiner" : "Combiner",
		"splitter" : "Splitter",
		"translator" : "Translator",
		"filter" : "Filter"
};

var linkingComponentTypes = [
	"md",
	"cg",
	"cg_ed",
	"ed"
];

var interComponentProcessorTypes = [
	"combiner",
	"splitter"
];

/**
 * Defines colors for the component types
 */
var typeToColorMap = {
		"start": "black",
		"end" : "black",
		"md" : "#9ccc65",
		"cg" : "#9ccc65",
		"ed" : "#9ccc65",
		"cg_ed" : "#9ccc65",
		"splitter" : "#fbc02d",
		"combiner" : "#fbc02d",
		"translator" : "grey",
		"filter" : "grey",
		"default" : "yellow"
};

/**
 * Cytoscape layout configuration
 */
var cyLayoutConfig = {
		name: 'klay',
		klay: {
			spacing: 50
		},
		nodeDimensionsIncludeLabels: true,
		padding: 20
		//padding: 40
	};



function setup() {
	$('#experimentType').multiselect();
	$('#matching').multiselect();
	$('#datasets').multiselect();
	$('#knowledgeBases').multiselect();
	
	var linkerIndex = 1;
	
	var pipelineConfigs = {};
	var pipelineCyGraphs = {};

	$("#addStandardLinker").on("click", function () {
		var newRow = $('<div class="standard-linker card col-md-12 text-center" linker-id="' + linkerIndex + '">');
		var cols = '';

		cols += '<div class="card-body">';
			cols += '<h5 class="card-title">' + linkerIndex + ': Standard Linker</h5>';
			cols += '<div class="card-text form-group row">';
				cols += '<div class="col-md-12">';
					cols += '<select class="dropdown-linker" name="linker">';
					cols += '</select>';
				cols += '</div>';
			cols += '</div>';
			cols += '<div class="form-group">';
				cols += '<button class="btn btn-danger my-remove" type="button">Remove</button>'
			cols += '</div>';
		cols += '</div>';

		newRow.append(cols);
		$("#linker").append(newRow);
		
		fillLinkerDropdown(newRow, ".dropdown-linker");

		$(".my-remove").on("click", function (event) {
			$(this).closest(".standard-linker").remove();
		});

		linkerIndex += 1;
	});

	$("#addSimplePipeline").on("click", function () {
		var newRow = $('<div class="custom-linker card col-md-12 text-center" linker-id="' + linkerIndex + '">');
		var cols = '';

		cols += '<div class="card-body">';
			cols += '<h5 class="card-title">' + linkerIndex + ': Simple Pipeline</h5>';
			cols += '<div class="card-text form-group row">';
				cols += '<label class="col-md-4 control-label" for="linker-md">Mention Detection</label>';
				cols += '<div class="col-md-8">';
					cols += '<select class="dropdown-linker-md">';
					cols += '</select>';
				cols += '</div>';
			cols += '</div>';
			cols += '<div class="form-group row">';
				cols += '<label class="col-md-4 control-label" for="linker-cg-ed">Candidate Generation + Entity Disambiguation</label>';
				cols += '<div class="col-md-8">';
					cols += '<select class="dropdown-linker-cg-ed">';
					cols += '</select>';
				cols += '</div>';
			cols += '</div>';
			cols += '<div class="form-group">';
				cols += '<button class="btn btn-danger my-remove" type="button">Remove</button>'
			cols += '</div>';
		cols += '</div>';

		newRow.append(cols);
		$("#linker").append(newRow);

		fillLinkerDropdown(newRow, ".dropdown-linker-md", "MD");
		fillLinkerDropdown(newRow, ".dropdown-linker-cg-ed", "CG_ED");

		$(".my-remove").on("click", function (event) {
			$(this).closest(".custom-linker").remove();
		});
		
		linkerIndex += 1;
	});

	$("#addComplexPipeline").on("click", function () {
		var newRow = $('<div class="complex-pipeline card col-md-12 text-center" linker-id="' + linkerIndex + '">');
		var cols = buildHtmlComplexPipeline(linkerIndex);
		newRow.append(cols);
		$("#linker").append(newRow);

		// var pipelineConfig = Object.assign({}, pipelineConfigDefault); // shallow copy
		var pipelineConfig = $.extend(true, {}, pipelineConfigDefault); // deep copy
		pipelineConfig.id = linkerIndex;
		
		// Cytoscape
		var pipelineGraph = generatePipelineGraph(pipelineConfig);
		var divId = "cy" + linkerIndex;
		var cy = cytoscape({
			container: document.getElementById(divId),
			elements: pipelineGraph,
			layout: cyLayoutConfig,
			style: [{
				selector: 'node',
				style: {
					'background-color': function(ele){ return getComponentColor(ele.data('type')) },
					'label': 'data(label)',
					'text-wrap': 'wrap',
					'text-valign': 'bottom',
					'text-margin-y': '5px',
				}
			},
			{
				selector: 'edge',
				style: {
					'curve-style': 'bezier',
					'target-arrow-shape': 'triangle'
				}
			},
			{
				selector: 'node:selected',
				style: {
					'background-color': "red",
				}
			}]
		});
		
		cy.on('tap', function(event){
			if (event.target === cy){
				// ignore tap on background
				hideComponentDetailsView(pipelineConfig.id);
			} else if (event.target.isEdge()) {
				// ignore tap on edge
				hideComponentDetailsView(pipelineConfig.id);
			} else {
				var componentId = event.target.data('id');
				var componentType = event.target.data('type');
				if (componentId == "start" || componentId == "end") {
					// ignore tap on start or end node
					hideComponentDetailsView(pipelineConfig.id);
				} else {
					showComponentDetails(pipelineConfig, cy, componentId, componentType);
					showComponentDetailsView(pipelineConfig.id);
				}
			}
		});
		
		fillAddComponentDropdown(pipelineConfig, cy);
		
		pipelineConfigs[linkerIndex] = pipelineConfig;
		pipelineCyGraphs[linkerIndex] = cy;

		$(".my-remove").on("click", function (event) {
			$(this).closest(".complex-pipeline").remove();
		});

		linkerIndex += 1;
	});
	


	
	
	
	$('#submit').click( function() {
		taskType = $('#experimentType option:selected').val();
		matching = $('#matching option:selected').val();
		
		var linkerConfigs = [];
		
		// add standard linkers
		$('.standard-linker').each(function(index, linker) {
			var linkerId = parseInt($(linker).attr('linker-id'));
			var linkerName = $(linker).find('select.dropdown-linker option:selected').val();
			linkerConfig = {
					'id' : linkerId,
					'pipelineType' : 'standard',
					'linker' : linkerName};
			linkerConfigs.push(linkerConfig);
		});
		
		// add linker pipeline
		$('.custom-linker').each(function(index, linker) {
			var linkerId = parseInt($(linker).attr('linker-id'));
			var mentionDetector= $(linker).find('select.dropdown-linker-md option:selected').val();
			var candidateGeneratorDisambiguator = $(this).find('select.dropdown-linker-cg-ed option:selected').val();
			linkerConfig = {
					'id' : linkerId,
					'pipelineType' : 'custom',
					'mentionDetector' : mentionDetector,
					'candidateGeneratorDisambiguator' : candidateGeneratorDisambiguator};
			linkerConfigs.push(linkerConfig);
		});

		// add complex pipeline
//		$('.complex-pipeline').each(function(index, pipeline) {
//			var pipelineConfig3 = generatePipelineConfigJson(pipelineConfig); // TODO
//			pipelineConfig3['id'] = pipelineConfig["id"];
//			pipelineConfig3['pipelineType'] = pipelineConfig["pipelineType"];
//			linkerConfigs.push(pipelineConfig3);
//		});
		$.each(pipelineConfigs, function(index, pipelineConfig) {
			var linkerConfig = generatePipelineConfigJson(pipelineConfig);
			linkerConfigs.push(linkerConfig);
		});

		datasets = []
		$('#datasets option:selected').each(function() {
			datasets.push($(this).val());
		});
		
		inputText = $('#inputText').val();
		
		knowledgeBases = []
		$('#knowledgeBases option:selected').each(function() {
			knowledgeBases.push($(this).val());
		});
		
		data = {};
		data.taskType = taskType;
		data.matching = matching;
		data.linkerConfigs = linkerConfigs;
		data.datasets = datasets;
		data.inputText = inputText;
		data.knowledgeBases = knowledgeBases;
		
		$.ajax({
			type : "POST",
			url: "execute",
			data : { 'experimentData' : JSON.stringify(data) },
			success : function(data) {
				//document.getElementById("result").innerHTML = JSON.stringify(data, undefined, 2);
				//console.log(data);
				var win = window.open("result", '_blank');
			}
		});

	});
	
}


function showComponentDetails(pipelineConfig, cy, componentId, componentType) {
	var component = pipelineConfig["components"][componentType].find(x => x.id == componentId);
	
	var detailsView = getComponentDetailsViewDiv(pipelineConfig.id);
	detailsView.find("h5").text(typeNameToTypeStringMap[componentType]);
	detailsView.find("p").text(componentId);
	
	// show dropdowns for value, dependency and target selection
	fillComponentValueDropdown(pipelineConfig, cy, componentId, componentType, component.value);
	fillComponentDependenciesDropdown(pipelineConfig, cy, componentId);
	fillComponentTargetsDropdown(pipelineConfig, cy, componentId);
	
	// update delete button
	detailsView.find(".btn-delete-component").off("click").click(function() {
		deleteComponent(pipelineConfig, cy, componentId);
	});
}

function getComponentDependencies(pipelineConfig, componentId) {
	var dependencies = [];
	$.each(pipelineConfig["connections"], function(index, connection) {
		if (connection.target == componentId) {
			dependencies.push(connection.source);
		}
	});
	if (pipelineConfig["startComponents"].indexOf(componentId) > -1) {
		dependencies.push("start");
	}
	return dependencies;
}

function getComponentTargets(pipelineConfig, componentId) {
	var targets = [];
	$.each(pipelineConfig["connections"], function(index, connection) {
		if (connection.source == componentId) {
			targets.push(connection.target);
		}
	});
	if (pipelineConfig["endComponents"].indexOf(componentId) > -1) {
		targets.push("end");
	}
	return targets;
}

/**
 * Return possible component values (i.e. linkers for MD, CG, CG_ED, ED, and various values for the
 * inter-component processors (ICP))
 */
function getPossibleComponentValues(pipelineConfig, componentType, callback) {
	var possibleValues = [];
	if (isLinkingComponent(componentType)) {
		getLinkers(componentType, callback);
	} else if (isInterComponentProcessor(componentType)) {
		getICPs(componentType, callback);
	}
	return possibleValues;
}


function getLinkers(taskType, callback) {
	var url = "linkers"
	if (typeof taskType !== undefined) {
		url += "?taskType=" + taskType;
	}
	$.ajax({
		type : "GET",
		url: url,
		success : function(data) {
			callback(data);
		},
		error : function() {}
	});
}

function getICPs(icpType, callback) {
	var url = "icps"
	if (typeof icpType !== undefined) {
		url += "?icpType=" + icpType;
	}
	$.ajax({
		type : "GET",
		url: url,
		success : function(data) {
			callback(data);
		},
		error : function() {}
	});
}


/**
 * Return possible component dependencies for dropdown selection
 */ 
function getPossibleComponentDependencies(pipelineConfig, componentId) {
	var possibleDependencies = [];
	possibleDependencies.push("start");
	// TODO filter to valid ones
	$.each(pipelineConfig["components"], function(key, componentList) {
		$.each(componentList, function(index, component) {
			if (component.id != componentId) {
				possibleDependencies.push(component.id);
			}
		});
	});
	return possibleDependencies;
}

/**
 * Return possible component targets for dropdown selection
 */ 
function getPossibleComponentTargets(pipelineConfig, componentId) {
	var possibleTargets =[];
	// TODO filter to valid ones
	$.each(pipelineConfig["components"], function(key, componentList) {
		$.each(componentList, function(index, component) {
			if (component.id != componentId) {
				possibleTargets.push(component.id);
			}
		});
	});
	possibleTargets.push("end");
	return possibleTargets;
}

/**
 * Generates the HTML of the dropdown menu for adding new components to a complex pipeline
 */
function fillAddComponentDropdown(pipelineConfig, cy) {
	var pipelineDiv = getPipelineDiv(pipelineConfig.id);
	var dropdown = pipelineDiv.find(".dropdown-add-component > div");
	dropdown.empty();
	
	// components
	$.each(linkingComponentTypes, function(i, nodeType) {
		dropdown.append($("<a></a>").attr("class", "dropdown-item").attr("href", "#")
				.attr("data-type", nodeType).text(typeNameToTypeStringMap[nodeType]));
	});
	
	dropdown.append($("<div></div>").attr("class", "dropdown-divider"));
	
	// inter-component processors
	$.each(interComponentProcessorTypes, function(i, nodeType) {
		dropdown.append($("<a></a>").attr("class", "dropdown-item").attr("href", "#")
				.attr("data-type", nodeType).text(typeNameToTypeStringMap[nodeType]));
	});
	
	// event handler
	var dropdownElements = pipelineDiv.find(".dropdown-add-component > div > a");
	dropdownElements.off("click");
	dropdownElements.on("click", function() {
		var componentType = $(this).data("type");
		var componentId = getFreeComponentId(pipelineConfig, componentType);
		var componentValue = getComponentDefault(componentType);
		addComponent(pipelineConfig, cy, componentId, componentType, componentValue);
	});
}

/**
 * Generates the HTML of the dropdown menu for choosing the value of a component of a complex
 * pipeline
 */
function fillComponentValueDropdown(pipelineConfig, cy, componentId, componentType, componentValue) {
	getPossibleComponentValues(pipelineConfig, componentType, function(possibleComponentValues) {
		var dropdown = getPipelineDiv(pipelineConfig.id).find(".dropdown-component-value");
		dropdown.empty();
		$.each(possibleComponentValues, function(index, entry) {
			var option = $("<option></option>").attr("value", entry).text(entry);
			dropdown.append(option);
		});
		dropdown.val(componentValue);
		dropdown.multiselect('rebuild');
		dropdown.off("change"); // remove old event handler
		dropdown.change(function() {
			var value = $(this).val();
			setComponentValue(pipelineConfig, cy, componentId, componentType, value);
		});
	});
}

/**
 * Fill the component dependency multiselect dropdown with possible dependencies and select the
 * currently active ones
 */
function fillComponentDependenciesDropdown(pipelineConfig, cy, componentId) {
	var componentDependencies = getComponentDependencies(pipelineConfig, componentId);
	var possibleComponentDependencies = getPossibleComponentDependencies(pipelineConfig, componentId);
	var dropdown = getPipelineDiv(pipelineConfig.id).find(".dropdown-component-dependencies");
	dropdown.empty();
	$.each(possibleComponentDependencies, function(index, entry) {
		var option = $("<option></option>").attr("value", entry).text(entry);
		if (componentDependencies.indexOf(entry) > -1) {
			option.prop("selected", "selected");
		}
		dropdown.append(option);
	});
	dropdown.multiselect('rebuild');
	dropdown.off("change"); // remove old event handler
	dropdown.change(function() {
		var value = $(this).val();
		setComponentDependencies(pipelineConfig, cy, componentId, value);
	});
}

/**
 * Fill the component target multiselect dropdown with possible targets and select the
 * currently active ones
 */
function fillComponentTargetsDropdown(pipelineConfig, cy, componentId) {
	var componentTargets = getComponentTargets(pipelineConfig, componentId);
	var possibleComponentTargets = getPossibleComponentTargets(pipelineConfig, componentId);
	var dropdown = getPipelineDiv(pipelineConfig.id).find(".dropdown-component-targets");
	dropdown.empty();
	$.each(possibleComponentTargets, function(index, entry) {
		var option = $("<option></option>").attr("value", entry).text(entry);
		if (componentTargets.indexOf(entry) > -1) {
			option.prop("selected", "selected");
		}
		dropdown.append(option);
	});
	dropdown.multiselect('rebuild');
	dropdown.off("change"); // remove old event handler
	dropdown.change(function() {
		var value = $(this).val();
		setComponentTargets(pipelineConfig, cy, componentId, value);
	});
}


/**
 * Add a new component
 */
function addComponent(pipelineConfig, cy, componentId, componentType, componentValue) {
	var component = {id: componentId, value: componentValue, new: true};
	pipelineConfig["components"][componentType].push(component);
	console.log("Added node " + componentId);
	updatePipelineGraph(cy, pipelineConfig);
}

/**
 * Update the value of a component
 */
function setComponentValue(pipelineConfig, cy, componentId, componentType, componentValue) {
	var component = pipelineConfig["components"][componentType].find(x => x.id === componentId);
	component.value = componentValue;
	updatePipelineGraph(cy, pipelineConfig);
}

/**
 * Update the dependencies of a component
 */
function setComponentDependencies(pipelineConfig, cy, componentId, componentDependencies) {
	// remove property "new"
	var componentType = getComponentTypeById(componentId);
	delete pipelineConfig["components"][componentType].find(x => x.id == componentId).new;
	
	// delete all existing connections
	pipelineConfig["connections"] = pipelineConfig["connections"].filter(function(obj) {
		return obj.target != componentId;
	});
	
	// delete from start components
	pipelineConfig["startComponents"] = pipelineConfig["startComponents"].filter(function(obj) {
		return obj != componentId;
	});
	
	// add all new
	$.each(componentDependencies, function(index, componentDependency) {
		if (componentDependency === "start") {
			pipelineConfig["startComponents"].push(componentId);
		} else {
			var connection = {source: componentDependency, target: componentId};
			pipelineConfig["connections"].push(connection);
		}
	});
	
	updatePipelineGraph(cy, pipelineConfig);
	updatePipelineGraphLayout(cy);
}

/**
 * Update the dependencies of a component
 */
function setComponentTargets(pipelineConfig, cy, componentId, componentTargets) {
	// delete all existing connections
	pipelineConfig["connections"] = pipelineConfig["connections"].filter(function(obj) {
		return obj.source != componentId;
	});
	
	// delete from end components
	pipelineConfig["endComponents"] = pipelineConfig["endComponents"].filter(function(obj) {
		return obj != componentId;
	});
	
	// add all new
	$.each(componentTargets, function(index, componentTarget) {
		if (componentTarget === "end") {
			pipelineConfig["endComponents"].push(componentId);
		} else {
			var connection = {source: componentId, target: componentTarget};
			pipelineConfig["connections"].push(connection);
		}
	});
	
	updatePipelineGraph(cy, pipelineConfig);
	updatePipelineGraphLayout(cy);
}

/**
 * Delete pipeline component and update Cytoscape graph
 */
function deleteComponent(pipelineConfig, cy, componentId) {
	var componentType = getComponentTypeById(componentId);
	
	// delete component
	var componentList = pipelineConfig["components"][componentType];
	pipelineConfig["components"][componentType] = componentList.filter(function(obj) {
		return obj.id != componentId;
	});
	
	// delete edges
	var connectionList = pipelineConfig["connections"];
	pipelineConfig["connections"] = connectionList.filter(function(obj) {
		return obj.source != componentId && obj.target !== componentId;
	});
	
	// delete from start and end components list
	$.each(["startComponents", "endComponents"], function(index, listName) {
		var componentsList = pipelineConfig[listName];
		pipelineConfig[listName] = componentsList.filter(function(obj) {
			return obj != componentId;
		});
	});

	console.log("Deleted node " + componentId);
	updatePipelineGraph(cy, pipelineConfig);
	hideComponentDetailsView(pipelineConfig.id);
}


/**
 * Update the Cytoscape canvas and redraw the graph completely from global variable
 * <tt>pipelineConfig</tt>
 */
function updatePipelineGraph(cy, pipelineConfig) {
	var pipelineGraph = generatePipelineGraph(pipelineConfig);
	cy.json({ elements: pipelineGraph});
}

/**
 * Updates the layout of the Cytoscape graph
 */
function updatePipelineGraphLayout(cy) {
	cy.layout(cyLayoutConfig).run();
}

/** 
 * Generates the Cytoscape pipeline graph from the pipelineConfig
 */
function generatePipelineGraph(pipelineConfig) {
	
	var pipelineGraph = [];
	var edgeId = 1;
	
	// add start and end node
	pipelineGraph.push({data: {id: "start", type: "start", label: "START"}});
	pipelineGraph.push({data: {id: "end", type: "end", label: "END"}});
	
	// read pipeline config and add nodes to the graph
	$.each(pipelineConfig["components"], function(key, componentList) {
		$.each(componentList, function(index, component) {
			if (component.value == null) {
				var label = component.id + "\n<none>";
			} else {
				var label = component.id + "\n" + component.value;
			}
			var node = {data: {id: component.id, type: key, label: label}};
//			if (component.new) {
//				node.data.position = {x: 120, y: 120};
//			}
			pipelineGraph.push(node);
		});
	});
	
	// read pipeline config and add edges to the graph
	$.each(pipelineConfig["connections"], function(index, connection) {
		var edge = {data: {id: 'e' + edgeId, source: connection.source, target: connection.target}};
		pipelineGraph.push(edge);
		edgeId++;
	});
	
	// add connections to start node
	$.each(pipelineConfig["startComponents"], function(index, componentId) {
		var edge = {data : {id : 'e' + edgeId, source: "start", target: componentId}};
		pipelineGraph.push(edge);
		edgeId++;
	});
	
	// add connections to end node
	$.each(pipelineConfig["endComponents"], function(index, componentId) {
		var edge = {data : {id : 'e' + edgeId, source: componentId, target: "end"}};
		pipelineGraph.push(edge);
		edgeId++;
	});
	
	return pipelineGraph;
}

function generatePipelineConfigJson(pipelineConfig) {
	var pipelineConfig2 = {};
	// add ID and type
	//var linkerId = parseInt($(linker).attr('linker-id'));
	pipelineConfig2["id"] = pipelineConfig["id"];
	pipelineConfig2["pipelineType"] = pipelineConfig["pipelineType"];
	// add components
	$.each(Object.keys(typeNameToTypeStringMap), function(indexComponentType, componentType) {
		if (componentType in pipelineConfig["components"]) {
			pipelineConfig2[componentType] = [];
			$.each(pipelineConfig["components"][componentType], function(indexComponent, component) {
				var newComponent = {};
				newComponent[component["id"]] = component["value"];
				pipelineConfig2[componentType].push(newComponent);
			});
		}
	});
	// add connections
	pipelineConfig2["connections"] = [];
	$.each(pipelineConfig["connections"], function(indexConnection, connection) {
		var newConnection = {};
		newConnection[connection["source"]] = connection["target"];
		pipelineConfig2["connections"].push(newConnection);
	});
	return pipelineConfig2;
}




/**
 * Returns the next free ID for a given node type
**/
function getFreeComponentId(pipelineConfig, componentType) {
	var components = pipelineConfig["components"][componentType];
	var typePrefix = typeNameToTypeIdMap[componentType];
//	var nodes = cy.$('node[id ^= "' + typePrefix + '"]');
	var lastId = 0;
	if (components.length > 0) {
		$.each(components, function(index, component) {
			//var idNr = parseInt(component.id.substring(2)); // assuming ID format XXnn, e.g. MD1, CG24, ED8
			var idNr = parseInt(component.id.substring(component.id.search(/\d/), component.id.length));
			if (idNr > lastId) {
				lastId = idNr;
			}
		});
	}
	var freeIdNr = lastId + 1;
	var freeId = typePrefix + freeIdNr.toString();
	return freeId;
}

/**
 * Returns a default value for a given component type
 */
function getComponentDefault(componentType) {
//	return typeIdToDefaultValueMap[componentType];
	return null;
}

/**
 * Return Cytoscape node color for a given component type
 */
function getComponentColor(type) {
	var color = typeToColorMap[type];
	if (color == null) {
		console.log("No color defined for type '" + type + "'");
		color = typeToColorMap["default"];
	}
	return color;
}

/**
 * Get the type of an component by it's id (MD1 is type "md", SP3 is type "splitter")
 */
function getComponentTypeById(componentId) {
	return typeIdToTypeNameMap[componentId.substring(0, componentId.search(/\d/))];
}

/**
 * Returns the pipeline HTML div of a given complex pipeline
 */
function getPipelineDiv(pipelineId) {
	return $("div[linker-id='" + pipelineId + "']");
}

/**
 * Returns the details view HTML div element of a given complex pipeline
 */
function getComponentDetailsViewDiv(pipelineId) {
	return getPipelineDiv(pipelineId).find("#detailsView");
}

/**
 * Show the component details view
 */
function showComponentDetailsView(pipelineId) {
	getComponentDetailsViewDiv(pipelineId).show();
}

/**
 * Hide the component details view
 */
function hideComponentDetailsView(pipelineId) {
	getComponentDetailsViewDiv(pipelineId).hide();
}

/**
 * Check if <tt>type</tt> is a linking component
 */
function isLinkingComponent(type) {
	return linkingComponentTypes.indexOf(type) > -1;
}

/**
 * Check if <tt>type</tt> is a inter-component processor
 */
function isInterComponentProcessor(type) {
	return interComponentProcessorTypes.indexOf(type) > -1;
}



function fillLinkerDropdown(linkerBox, htmlClass, taskType) {
	getLinkers(taskType, function(linkers) {
		var dropdown = $(linkerBox).find(htmlClass);
		dropdown.empty();
		$.each(linkers, function(i, linker) {
			dropdown.append($("<option></option>")
					.attr("value", linker).text(linker));
		});
		dropdown.multiselect();
	});
}









// Results


function getLastResult() {
	$.ajax({
		type : "GET",
		url: "lastresult",
		success : function(data) {
			//document.getElementById("json").innerHTML = JSON.stringify(data, undefined, 2);
			//console.log(data);
			buildResultHtml(data);
		}
	});
}

function showResult() {
	getLastResult();
}

function buildResultTitleHtml(experimentTaskResult) {
	var html = $("<h5>").addClass("card-title");
	
	var annotationPipelineConfig = experimentTaskResult["annotationPipelineConfig"];
	var pipelineType = annotationPipelineConfig["pipelineType"];
	var linkerId = experimentTaskResult["taskId"];

	if (pipelineType == "standard") {
		var linkerName = annotationPipelineConfig["linker"];
		html.append(linkerId + ": " + linkerName);
	} else if (pipelineType == "custom") {
		var mentionDetector = annotationPipelineConfig["mentionDetector"];
		var candidateGeneratorDisambiguator = annotationPipelineConfig["candidateGeneratorDisambiguator"];
		html.append(linkerId + ": " + mentionDetector + " (MD) + " + candidateGeneratorDisambiguator + " (CG+ED)");
	} else {
		html.append(linkerId + ": Complex Pipeline");
	}
	
	return html;
}

function buildResultHtml(data) {
	var experimentToMentionMap = {}; // contains the spanToMentionMaps for different experiments required for the tooltips
	var experimentId = 0;
	
	data.forEach(function(experimentResult) {
		var experimentHtml = $("<div>").addClass("card").addClass("col-md-8");
		var cardBody = $("<div>").addClass("card-body");
		
		var doc = experimentResult["document"];
		var text = doc["text"];
		var mentions = doc["mentions"];
		var spanId = 0;
		var spanToMentionMap = {}; // e.g. {0: ["url0", "url1"], 1: ["url1", "url2"], 2: ["url3"], 3: []};
		var openSpans = [];

		var cardText = $("<div>").addClass("card-text");
		var annotatedTextHtml = "<p>";
		// split by each character
		$.each(text.split(""), function(index, character) {
			// check for each mention
			$.each(mentions, function(i, mention) {
				var originalMention = mention["originalMention"];
				var mentionStartPos = mention["offset"];
				var mentionEndPos = mentionStartPos + originalMention.length;
				var mentionUrl = mention["possibleAssignments"][0]["assignment"];
				// does the mention start here?
				if (mentionStartPos == index) {
					// use of data-bracket possibly for URL, but would be text only, as the CSS-after-selector
					// can't contain HTML, only text
					annotatedTextHtml += '<span class="mention" data-bracket="" span-id="' + spanId +
							'" experiment-id="' + experimentId + '">';
					openSpans.push(spanId);
					spanToMentionMap[spanId] = mention;
					spanId += 1;
				}
				// does the mention end here?
				if (mentionEndPos == index) {
					annotatedTextHtml += '</span>';
					delete openSpans[spanId];
				}
			});
			annotatedTextHtml += character;
		});
		annotatedTextHtml += "</p>";

		experimentToMentionMap[experimentId] = spanToMentionMap;
		experimentId++;
		
		cardText.append(annotatedTextHtml);
		var cardTitle = buildResultTitleHtml(experimentResult)
		cardBody.append(cardTitle);
		cardBody.append(cardText);
		experimentHtml.append(cardBody);		
		$("#result").append(experimentHtml);
	});

	// initialize tooltips
	$(document).uitooltip({
		items: ".mention",
		show: null, // show immediately
		hide: {
			effect: ""
		},
		// make tooltips selectable/clickable (https://stackoverflow.com/a/15014759)
		close: function(event, ui) {
			ui.tooltip.hover(
					function () {
						$(this).stop(true).fadeTo(400, 1); 
						//.fadeIn("slow"); // doesn't work because of stop()
					},
					function () {
						$(this).fadeOut("400", function(){ $(this).remove(); })
					}
			);
		},
		content: function() {
			var spanId = $(this).attr("span-id");
			var experimentId = $(this).attr("experiment-id");
			var mention = experimentToMentionMap[experimentId][spanId];
			var html = $("<div>");
			//TODO Why don't they have an assignment?
			//var url = mention["assignment"]
			var url = mention["possibleAssignments"][0]["assignment"];
			html.append("<p><a href='" + url + "'>" + url + "</p>");
			return html;
		}
	});

	$("button.json-download").on("click", function(e) {
		$.ajax({
			type : "GET",
			url: "jsonresult",
			success : function(data) {
				var win = window.open("jsonresult", '_blank');
			}
		});
	});
}



function buildHtmlComplexPipeline(linkerIndex) {
//	var cols = $([
//		'<div class="card-body">',
//		'	<h5 class="card-title">' + linkerIndex + ': Complex Pipeline</h5>',
//		//'	<div id="cy"></div>',
//		'	<div class="card-text form-group row">',
//		'		<label class="col-md-4 control-label" for="drowdown-linker-md">Mention Detection</label>',
//		'		<div class="col-md-8">',
//		'			<select class="dropdown-linker-md" multiple="multiple">',
//		'			</select>',
//		'		</div>',
//		'	</div>',
//		'	<div class="form-group row">',
//		'		<label class="col-md-4 control-label" for="dropdown-ic-processor"></label>',
//		'		<div class="col-md-4">',
//		'			<select class="dropdown-ic-processor">',
//		'			</select>',
//		'		</div>',
//		'	</div>',
//		'	<div class="form-group row">',
//		'		<label class="col-md-4 control-label" for="dropdown-linker-cg-ed">Candidate',
//		'			Generation + Entity Disambiguation</label>',
//		'		<div class="col-md-8">',
//		'			<select class="dropdown-linker-cg-ed" multiple="multiple">',
//		'			</select>',
//		'		</div>',
//		'	</div>',
//		'	<div class="form-group">',
//		'		<button class="btn btn-danger my-remove" type="button">Remove</button>',
//		'	</div>',
//		'</div>',
//	].join("\n"));
	
	var cols = $([
		'<div class="card-body">',
		'	<h5 class="card-title">' + linkerIndex + ': Complex Pipeline</h5>',
		'	<div class="row">',
		'		<div id="graphView" class="col-8">',
		'			<div id="cy' + linkerIndex + '" style="height: 400px; text-align: left"></div>',
		'		</div>',
		'		<div class="col-4">',
		'			<div id="detailsView" style="display: none; text-align: left">',
		'				<h5>Component</h5>',
		'				<div class="form-group">',
		'					<p>ID</p>',
		'				</div>',
		'				<div class="form-group">',
		'					<select class="dropdown-component-value"></select><br>',
		'				</div>',
		'				<div class="form-group">',
		'					<label>Dependencies</label>',
		'					<select class="dropdown-component-dependencies" multiple="multiple"></select><br>',
		'				</div>',
		'				<div class="form-group">',
		'					<label>Targets</label>',
		'					<select class="dropdown-component-targets" multiple="multiple"></select>',
		'				</div>',
		'				<div class="form-group" style="text-align: center">',
		'					<input type="button" class="btn btn-danger btn-delete-component" value="Delete">',
		'				</div>',
		'				<hr>',
		'			</div>',
		'			<div class="dropdown dropdown-add-component">',
		'				<button class="btn dropdown-toggle" type="button"',
		'					id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true"',
		'					aria-expanded="false">Add component</button>',
		'				<div class="dropdown-menu" aria-labelledby="dropdownMenuButton"></div>',
		'			</div>',
		'		</div>',
		'	</div>',
		'	<div class="form-group">',
		'		<button class="btn btn-danger my-remove" type="button">Remove</button>',
		'	</div>',
		'</div>'
		].join("\n"));
	
	return cols;
}

