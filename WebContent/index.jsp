<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" session="false"%>
<%
String stylename;
if ("so".equals(request.getParameter("style")))
    stylename = "so";
else
    stylename = "plain";
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>SO 2015 Election Vote Monitor</title>
<link rel="stylesheet" type="text/css" href="<%= stylename %>.css"/>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
<script src="jquery.formatDateTime.min.js"></script>
<script src="jquery.storageapi.min.js"></script> 
<script type="text/javascript">
function setLocal (k, v) {
	try {
		$.localStorage.set(k, v);
	} catch (e) {
		console.log("setLocal " + k + " failed: " + e);
	}
}

function getLocal (k) {
	try {
		return $.localStorage.get(k);
	} catch (e) {
		console.log("getLocal " + k + " failed: " + e);
		return null;
	}
}

function localSupported () {
	try {
		return window.localStorage ? true : false;
	} catch (e) {
		console.log("localSupported failed: " + e);
		return false;
	}
}

var previous = null;
var saved = getLocal("saved");
var version = 0;
var resetTime = new Date(getLocal("resetTime"));
var serial = 0;
var intervalId = null;
var ready = false;

function addZero (v) { 
	return (v<10 ? '0' : '') + v; 
}

function updateVoteCounts (v, status) {
	$("#debug-status").text(status);
	if (status == "success") {
	    $("#debug-serial").text(v.s);
	    $("#debug-version").text(v.r);
	    serial = v.s;
	    if (v.r != version)
	        $("#update").show();
	    if (previous == null)
	        previous = v.v;
	    if (saved == null || saved.length != v.v.length /* covers case when local storage candidate list is definitely different */) {
	        saved = v.v;
	        resetTime = new Date();
	        setLocal("saved", saved);
	        setLocal("resetTime", resetTime.getTime());
	    }	
        $("#last-reset").text($.formatDateTime("yy-mm-dd hh:ii:ss", resetTime));
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
	} else {
		// data not modified, but we need to zero out the changed columns
		for (var n = 0; n < previous.length; ++ n) { // in a reasonably sane universe, previous will have been set already
            $("#change-" + n).text("0");
            $("#change-" + n).removeClass();
            $("#change-" + n).addClass("zero");			
		}
	}
    // time
    var now = new Date();
    $("#last-updated").text($.formatDateTime("yy-mm-dd hh:ii:ss", now));
    // reset time difference
    var diff = Math.floor((now.getTime() - resetTime.getTime()) / 1000);
    var ds = diff % 60; diff = Math.floor(diff / 60);
    var dm = diff % 60; diff = Math.floor(diff / 60);
    var dh = diff;
    $("#reset-time").text(dh + ":" + addZero(dm) + ":" + addZero(ds));
    // update status
    $("#debug-status").text(status);
}

function update () {
	//$.getJSON("votes", function (v) {
    // no more shorthand, need more options for supporting 304's -- or maybe we don't, but whatever. i'm confused. [15-apr-2015]
    $.ajax({
    	dataType: "json",
    	url: "votes",
    	data: { "s": serial },
    	success: updateVoteCounts
    });
}

function reset () {
    saved = null;
    serial = 0; // super duper hack; force requery on next update so accum column is cleared even if not modified. getting REALLY lazy.
}

function toggleDebug () {
	$(".debug").toggle();
}

function build (c) {
	$.each(c, function (index, user) {
	    $("#votes").append('<tr>' +
	    	               '<td class="rank"></td>' +
	    		           '<td><a href="http://stackoverflow.com/users/' + user.i + '">' + user.n +'</a></td>' +
	    	               '<td class="votecount" id="votes-' + index + '">...</td>' +
	    	               '<td class="gap">...</td>' +
                           '<td id="change-' + index + '">...</td>' +
                           '<td class="divider"/>' + 
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
        ready = true;
        $("#interval").val(getLocal("interval") === null ? "5000" : getLocal("interval"));
        changeInterval();
	});
	$("#local-storage").text(localSupported() ? "Supported" : "Not Supported");
	$("#refreshnote").toggle(localSupported());
}

function changeInterval () {
	if (!ready) {
		console.log("warning: user was quick on the interval select; ignored, not ready yet");
		return;
	}
	var interval = $("#interval").val();
	if (intervalId !== null)
	    clearInterval(intervalId);
    intervalId = setInterval(update, interval);
    setLocal("interval", interval);
}
</script>
</head>
<body onload="setup();">
<div id="update">An update has been made! Please refresh the page!</div>
<div id="content">
<div id="wrapper">
	<table id="live" cellspacing="0">
	<thead><tr><th>Rank</th><th>User</th><th class="votehead">Votes</th><th>Next</th><th>Change</th><th class="divider"/><th>Accum.</tr></thead>
	<tbody id="votes"></tbody>
	<tfoot><tr><td/><td/><td/><td/><td/><td class="divider"/><td class="actioncell"><a href="javascript:reset();">Reset</a></td></tr></tfoot>
	</table>
	<div id="info">
		<table>
        <tr><td class="key">Last Updated:</td><td class="value" id="last-updated"></td></tr>
	    <tr><td class="key">Last Reset:</td><td class="value" id="last-reset"></td></tr>
	    <tr><td class="key">Time Since Reset:</td><td class="value" id="reset-time"></td></tr>
	    <tr><td class="key">Update Interval:</td><td class="value">
		    <select id="interval" onchange="changeInterval();">
		    <option value="5000">5 seconds</option>
		    <option value="30000">30 seconds</option>
		    <option value="300000">5 minutes</option>
		    <option value="1800000">30 minutes</option>
		    </select>
	    </td></tr>
        <tr class="debug"><td class="key">Update Status:</td><td class="value" id="debug-status"></td></tr>        
        <tr class="debug"><td class="key">Data Version:</td><td class="value" id="debug-serial"></td></tr>        
        <tr class="debug"><td class="key">Server Version:</td><td class="value" id="debug-version"></td></tr>
        <tr class="debug"><td class="key">Local Storage:</td><td class="value" id="local-storage"></td></tr>
	    </table>
        <div id="refreshnote">The "Accum" column will be saved across page refreshes.</div>
        <div id="disclaimer">Please do not share this link outside of SO posts (Twitter, other forums, etc.). Thanks!</div>
	    <div id="links">
		    Useful election links:
		    <ul>
	        <li><a target="_blank" href="http://meta.stackoverflow.com/questions/290096">Candidate Questionnaire Responses</a></li>
	        <li><a target="_blank" href="http://stackoverflow.com/election/6?tab=nomination&all=true">Candidate Nomination Discussions</a> (scroll down for comments)</li>
	        <li><a target="_blank" href="http://elections.stackexchange.com/#stackoverflow">Candidate Data Summary</a></li>
	        <li><a target="_blank" href="http://meta.stackoverflow.com/questions/289995">Candidate Activity Profiles</a></li>
	        <li><a target="_blank" href="http://stackoverflow.com/election/6">Election Page</a> (vote here)</li>
	        <li><a target="_blank" href="http://meta.stackexchange.com/questions/135360">There's an election going on. What's happening and how does it work?</a></li>
	        <li><a target="_blank" href="http://meta.stackexchange.com/questions/77541">How are moderator election votes counted, in plain English?</a></li>        
		    </ul>
	    </div>
	</div>
</div>
<div id="halp">Counts refreshed every 5 seconds by default; interval can be changed by using dropdown above. 'Next' column shows gap to next rank up. 'Change' column shows change since last refresh. 'Accum' column shows total change since page load. Press 'Reset' at the bottom of the table to reset the 'Accum' column's start point; 'Accum' values saved across page refresh.</div>
<hr>
<div id="appinfo">Author: <a target="_blank" href="http://stackoverflow.com/users/616460">Jason C</a> | Version: <span id="version-number"></span> | <a href="javascript:toggleDebug();">Show Debug Info</a> | <a target="_blank" href="http://meta.stackoverflow.com/questions/290346">Vote Monitor Meta Page</a></div>
</div>
</body>
</html>