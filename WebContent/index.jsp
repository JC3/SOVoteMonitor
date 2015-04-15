<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>SO 2015 Election Vote Monitor</title>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
<script src="jquery.formatDateTime.min.js"></script>
<script type="text/javascript">
var previous = null;
var saved = null;
var version = 0;
var resetTime = null;

function addZero (v) { 
	return (v<10 ? '0' : '') + v; 
}

function update () {
	$.getJSON("votes", function (v) {
		if (v.r != version)
			$("#update").show();
		if (previous == null)
			previous = v.v;
		if (saved == null) {
			saved = v.v;
		    resetTime = new Date();
		    $("#last-reset").text($.formatDateTime("yy-mm-dd hh:ii:ss", resetTime));
		}
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
        $($('#live > tbody > tr').detach().sort(function(a, b) { return ($(b).find('.votecount').text() | 0) - ($(a).find('.votecount').text() | 0) })).appendTo('#live')
        // update ranks oh god i dont know what im doing
        var rank = 1;
        $(".rank").each(function () {
        	$(this).text(rank ++);
        	$(this).parent().removeClass();
        	if (rank == 12)
        		$(this).parent().addClass("cutoff");
        });
        var last = 0;
        $(".gap").each(function () {
        	var curr = $(this).siblings(".votecount").text(); 
            $(this).text((last == 0) ? '' : (last - curr));
        	last = curr;
        });
        // time
        var now = new Date();
        $("#last-updated").text($.formatDateTime("yy-mm-dd hh:ii:ss", now));
        // reset time difference
        var diff = Math.floor((now.getTime() - resetTime.getTime()) / 1000);
        var ds = diff % 60; diff = Math.floor(diff / 60);
        var dm = diff % 60; diff = Math.floor(diff / 60);
        var dh = diff;
        $("#reset-time").text(dh + ":" + addZero(dm) + ":" + addZero(ds));
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
	    	               '<td class="gap">...</td>' +
                           '<td id="change-' + index + '">...</td>' +
                           '<td id="accum-' + index + '">...</td>' +
	    	               '</tr>');
	});
}

function setup () {
	$.getJSON("votes?t=c", function (c) {
		version = c.r;
		$("#version-number").text(version);
	    build(c.c);
	    update();
	    setInterval(update, 5000);
	});
}
</script>
<style type="text/css">
#live th { font-weight: bold; text-align: right; padding-right: 2ex; }
#live th.votehead { font-weight: bold; text-align: right; padding-right: 1ex; }
#live td { text-align: right; border-top: 1px solid #c0c0c0; padding-right: 2ex; }
#live td.votecount { padding-right: 1ex; }
#live td.gap { color: #909090; font-size: small; }
#live tr.cutoff td { border-top: 2px solid red; }
#live td.up { background: #a0ffa0; border-right: 2px solid white; }
#live td.zero { background: #f0f0f0; border-right: 2px solid white; }
#live td.down { background: #ffa0a0; border-right: 2px solid white; }
#halp { margin-top: 2ex; font-weight: bold; }
#update { display: none; font-weight: bold; background: red; color: white; width: 100%; font-size: larger; text-align: center; margin-bottom: 1ex; }
#live { float: left; }
#info { float: left; margin-left: 3ex; margin-top: 3ex; }
#wrapper { overflow: hidden; }
.key { white-space: nowrap; font-weight: bold; }
.value { white-space: nowrap; margin-left: 1ex; text-align: right; }
#disclaimer { color: red; margin-top: 3ex; }
#appinfo { color: #909090; margin-top: 0.5ex; font-size: small; }
</style>
</head>
<body onload="setup();">
<div id="update">An update has been made! Please refresh the page!</div>
<div id="wrapper">
	<table id="live" cellspacing="0">
	<thead><tr><th>Rank</th><th>User</th><th class="votehead">Votes</th><th>Next</th><th>Change</th><th>Accum.</tr></thead>
	<tbody id="votes"></tbody>
	<tfoot><tr><td/><td/><td/><td/><td/><td><a href="javascript:reset();">Reset</a></td></tr></tfoot>
	</table>
	<div id="info">
		<table>
	    <tr><td class="key">Last Updated:</td><td class="value" id="last-updated"></td></tr>
	    <tr><td class="key">Last Reset:</td><td class="value" id="last-reset"></td></tr>
	    <tr><td class="key">Time Since Reset:</td><td class="value" id="reset-time"></td></tr>
	    </table>
	    <div id="disclaimer">Please do not share this link in public (Twitter, SO posts, etc.), my server can only handle so much! Thanks!</div>
	</div>
</div>
<div id="halp">Counts refreshed every 5 seconds. 'Next' column shows gap to next rank up. 'Change' column shows change since last refresh. 'Accum' column shows total change since page load. Press 'Reset' at the bottom of the table to reset the 'Accum' column's start point.</div>
<hr>
<div id="appinfo">Author: <a href="http://stackoverflow.com/users/616460">Jason C</a> | Version: <span id="version-number"></span></div>
</body>
</html>