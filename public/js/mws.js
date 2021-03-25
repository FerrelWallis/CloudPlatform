$(document).ready(function() {

	/* Side dropdown menu */
	$("div#mws-navigation ul li a, div#mws-navigation ul li span")
	.bind('click', function(event) {
		if($(this).next('ul').size() !== 0) {
			$(this).next('ul').slideToggle('fast', function() {
				$(this).toggleClass('closed');
			});
			event.preventDefault();
		}
	});

	
	/* Table Row CSS Class */
	$("table.mws-table tbody tr:even").addClass("even");
	$("table.mws-table tbody tr:odd").addClass("odd");

});
