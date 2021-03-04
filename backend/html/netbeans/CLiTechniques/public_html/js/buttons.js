$(document).ready(function() {
    var annotatorCounter = 0
    $("#addStandardAnnotator").on("click", function () {
        
                var newDiv = "(Adding) Annotator["+annotatorCounter+++"]</br>";
		$("#grapharea").append(newDiv);
                
		//$("#popup-windows").append(newDiv);

	});
});