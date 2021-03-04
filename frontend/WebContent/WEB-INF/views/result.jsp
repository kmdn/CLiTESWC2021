<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Agnos Experiment Result</title>

<!-- jQuery -->
<link href="https://code.jquery.com/ui/1.10.4/themes/ui-lightness/jquery-ui.css" rel="stylesheet">
<script src="https://code.jquery.com/jquery-1.10.2.js"></script>
<script src="https://code.jquery.com/ui/1.10.4/jquery-ui.js"></script>

<!-- Change JQueryUI plugin names to fix name collision with Bootstrap -->
<!-- https://www.ryadel.com/en/using-jquery-ui-bootstrap-togheter-web-page/ -->
<script type="text/javascript">
$.widget.bridge('uitooltip', $.ui.tooltip);
$.widget.bridge('uibutton', $.ui.button);
</script>

<!-- Bootstrap -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js"></script>
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css">
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js"></script>

<link rel="stylesheet" href="css/style.css">
<script src="js/script.js"></script>

</head>
<body class="container">

	<nav class="navbar navbar-default" role="navigation">
		<div class="container-fluid">
		</div>
	</nav>

	<h1>Result</h1>
	<div id="result"></div>
	<div class="row">
		<div class="col-md-8 col-md-offset-2">
			<hr>
		</div>
	</div>
	<div class="row">
		<div class="col-md-8">
			<p>Download result of all experiments as JSON:</p>
			<button class="btn btn-primary json-download" type="button">Download</button>
		</div>
	</div>

	<script>
		$(function() {
			showResult();
		});
	</script>

</body>
</html>
