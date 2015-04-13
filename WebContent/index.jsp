<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>SO 2015 Election Vote Monitor</title>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
<script type="text/javascript">
var previous = null;
var saved = null;

function update () {
	$.getJSON("votes", function (v) {
		if (previous == null)
			previous = v.v;
		if (saved == null)
			saved = v.v;
		$.each(v.v, function (index, votes) {
			$("#votes-" + index).text(votes);
            if (previous != null) {
                var delta = votes - previous[index];
                $("#change-" + index).text((delta > 0 ? '+' : '') + delta);
                $("#change-" + index).removeClass();
                $("#change-" + index).addClass((delta > 0 ? 'up' : (delta < 0 ? 'down' : 'zero')));
            }
            if (saved != null) {
                var delta = votes - saved[index];
                $("#accum-" + index).text((delta > 0 ? '+' : '') + delta);
                $("#accum-" + index).removeClass();
                $("#accum-" + index).addClass((delta > 0 ? 'up' : (delta < 0 ? 'down' : 'zero')));
            }
		});
		previous = v.v;
		// i copied this from meagar's so chat message
        $($('table:first > tbody > tr').detach().sort(function(a, b) { return ($(b).find('.votecount').text() | 0) - ($(a).find('.votecount').text() | 0) })).appendTo('table:first')
        // update ranks oh god i dont know what im doing
        var rank = 1;
        $(".rank").each(function () {
        	$(this).text(rank ++);
        	$(this).parent().removeClass();
        	if (rank == 12)
        		$(this).parent().addClass("cutoff");
        });      
	});
}

function reset () {
    saved = null;
}

function build (c) {
	$.each(c, function (index, user) {
	    $("#votes").append('<tr>' +
	    	               '<td class="rank"></td>' +
	    		           '<td><a href="http://stackoverflow.com/users/' + user.i + '">' + user.n +'</a></td>' +
	    	               '<td class="votecount" id="votes-' + index + '">...</td>' +
                           '<td id="change-' + index + '">...</td>' +
                           '<td id="accum-' + index + '">...</td>' +
	    	               '</tr>');
	});
}

function setup () {
	$.getJSON("votes?t=c", function (c) {
	    build(c.c);
	    update();
	    setInterval(update, 5000);
	});
}
</script>
<style type="text/css">
th { font-weight: bold; text-align: right; padding-right: 2ex; }
td { text-align: right; border-top: 1px solid #c0c0c0; padding-right: 2ex; }
tr.cutoff td { border-top: 2px solid red; }
td.up { background: #a0ffa0; border-right: 2px solid white; }
td.zero { background: #f0f0f0; border-right: 2px solid white; }
td.down { background: #ffa0a0; border-right: 2px solid white; }
#halp { margin-top: 2ex; font-weight: bold; }
</style>
</head>
<body onload="setup();">
<table cellspacing="0">
<thead><tr><th>Rank</th><th>User</th><th>Votes</th><th>Change</th><th>Accum.</tr></thead>
<tbody id="votes"></tbody>
<tfoot><tr><td/><td/><td/><td/><td><a href="javascript:reset();">Reset</a></td></tr></tfoot>
</table>
<div id="halp">Counts refreshed every 5 seconds. 'Change' column shows change since last refresh. 'Accum' column shows total change since page load. Press 'Reset' at the bottom of the table to reset the 'Accum' column's start point.</div>
</body>
</html>