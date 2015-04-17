<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" session="false"%>
<%
String stylename;
if ("so".equals(request.getParameter("style")))
    stylename = "so";
else if ("astro".equals(request.getParameter("style")))
    stylename = "astro";
else
    stylename = "plain";
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>SO 2015 Election Vote Monitor</title>
<link rel="stylesheet" type="text/css" href="<%= stylename %>.css"/>
<style type="text/css">
#reset-pending { display: none; }
#refreshnote { display: none; }
#update { display: none; }
.debug { display: none; }
</style>
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
var enddate = null;
var electionEnded = false;

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
	        $("#reset-link").show();
	        $("#reset-pending").hide();
	    }	
        $("#last-reset").text($.formatDateTime("yy-mm-dd hh:ii:ss", resetTime));
        // withdraw status
        var withdrawn = new Array();
        for (var n = 0; n < v.v.length; ++ n)
        	withdrawn[n] = false;
        if (v.w != null) {
            $(".entry").removeClass("withdrawn");
            $.each(v.w, function (index, id) {
                withdrawn[id] = true;
                $("#entry-" + id).addClass("withdrawn");
            });
        }   
        // vote counts
	    $.each(v.v, function (index, votes) {
	        $("#votes-" + index).text(votes);
	        if (previous != null) {
	            var delta = votes - previous[index];
                $("#change-" + index).removeClass();
                if (!withdrawn[index]) {
	                $("#change-" + index).text((delta > 0 ? '+' : '') + delta);
	                $("#change-" + index).addClass((delta > 0 ? 'up' : (delta < 0 ? 'down' : 'zero')));
                } else {
                    $("#change-" + index).text("-");                	
                }
	        }
	        if (saved != null) {
                $("#accum-" + index).removeClass();
                if (!withdrawn[index]) {
                    var delta = votes - saved[index];
  	                $("#accum-" + index).text((delta > 0 ? '+' : '') + delta);
	                $("#accum-" + index).addClass((delta > 0 ? 'up' : (delta < 0 ? 'down' : 'zero')));
                } else {
                	$("#accum-" + index).text("-");
                }
	        }
	    });
	    previous = v.v;
	    // i copied this from meagar's so chat message
	    $($('#live > tbody > tr').detach().sort(function(a, b) { return ($(b).find('.votecount').text() | 0) - ($(a).find('.votecount').text() | 0) })).appendTo('#live')
	    // update ranks oh god i dont know what im doing
	    var rank = 1;
	    $(".rank").each(function () {
	    	if ($(this).parent().hasClass("withdrawn")) {
	    		$(this).text("out");
	    	} else {
		        $(this).text(rank ++);
		        $(this).parent().removeClass("cutoff");
		        if (rank == 12)
		            $(this).parent().addClass("cutoff");
	    	}
	    });
	    var last = 0;
	    $(".gap").each(function () {
	    	if ($(this).parent().hasClass("withdrawn")) {
	    		$(this).text("");
	    	} else {
	            var curr = $(this).siblings(".votecount").text(); 
	            $(this).text((last == 0) ? '' : (last - curr));
	            last = curr;
	    	}
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
    // stop updating after election ends; do it here so we get at least one update in (todo: improve logic)
    if (electionEnded) {
    	console.log("primaries are over; stopping update queries.");
    	clearInterval(intervalId);
    	intervalId = null;
    }    
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

function updateCountdown () {
	if (enddate !== null) {
	    var diff = Math.floor((enddate - new Date().getTime()) / 1000);
	    if (diff < 0) {
	    	$("#time-left").text("Primaries ended! Congrats to all!");
	    	electionEnded = true; // next update will be our last
	    } else {
		    var ds = diff % 60; diff = Math.floor(diff / 60);
		    var dm = diff % 60; diff = Math.floor(diff / 60);
		    var dh = diff % 24; diff = Math.floor(diff / 24);
		    var dd = diff;
		    $("#time-left").text(dd + ":" + addZero(dh) + ":" + addZero(dm) + ":" + addZero(ds));
	    }
	}
}
	
function reset () {
    saved = null;
    serial = 0; // super duper hack; force requery on next update so accum column is cleared (and reset button reshown) even if not modified. getting REALLY lazy.
    $("#reset-link").hide();
    $("#reset-pending").show();
}

function toggleDebug () {
	$(".debug").toggle();
}

function build (c) {
	$.each(c, function (index, user) {
	    $("#votes").append('<tr class="entry" id="entry-' + index + '">' +
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
		enddate = new Date(c.e);
		version = c.r;
		$("#version-number").text(version);
	    build(c.c);
	    update();
        ready = true;
        $("#interval").val(getLocal("interval") === null ? "5000" : getLocal("interval"));
        changeInterval();
        setInterval(updateCountdown, 1000);
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
	if (electionEnded) {
		console.log("not changing interval; primaries are over.");
	} else {
	    if (intervalId !== null)
	        clearInterval(intervalId);
	    intervalId = setInterval(update, interval);
	}
    setLocal("interval", interval);
}
</script>
</head>
<body onload="setup();">
<div id="update">An update has been made! Please refresh the page!</div>
<div id="wrapper">
	<table id="live" cellspacing="0">
	<thead><tr><th>Rank</th><th>User</th><th class="votehead">Votes</th><th>Next</th><th>Change</th><th class="divider"/><th>Accum.</tr></thead>
	<tbody id="votes"></tbody>
	<tfoot><tr><td/><td/><td/><td/><td/><td class="divider"/><td class="actioncell"><span id="reset-link"><a href="javascript:reset();">Reset</a></span><span id="reset-pending">Wait...</span></td></tr></tfoot>
	</table>
	<div id="info">
		<table>
		<tr><td class="key">Election Countdown:</td><td class="value" id="time-left"></td></tr>
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
        <tr class="debug"><td class="key">Data Serial:</td><td class="value" id="debug-serial"></td></tr>        
        <tr class="debug"><td class="key">Server Version:</td><td class="value" id="debug-version"></td></tr>
        <tr class="debug"><td class="key">Local Storage:</td><td class="value" id="local-storage"></td></tr>
	    </table>
        <div id="refreshnote">The "Accum" column will be saved across page refreshes.</div>
        <div id="disclaimer">Please do not share this link outside of SO posts (Twitter, other forums, etc.). Thanks!</div>
	    <div id="links">
		    Useful election links:
		    <ul>
	        <li><a  href="http://meta.stackoverflow.com/questions/290096">Candidate Questionnaire Responses</a> (<a  href="qa.jsp">alternate view</a>)</li>
	        <li><a  href="http://stackoverflow.com/election/6?tab=nomination&all=true">Candidate Nomination Discussions</a> (scroll down for comments)</li>
	        <li><a  href="http://elections.stackexchange.com/#stackoverflow">Candidate Data Summary</a></li>
	        <li><a  href="http://meta.stackoverflow.com/questions/289995">Candidate Activity Profiles</a></li>
	        <li><a  href="http://stackoverflow.com/election/6">Election Page</a> (vote here)</li>
	        <li><a  href="http://meta.stackexchange.com/questions/135360">There's an election going on. What's happening and how does it work?</a></li>
	        <li><a  href="http://meta.stackexchange.com/questions/77541">How are moderator election votes counted, in plain English?</a></li>        
		    </ul>
		    Themes: <a href="?style=plain">Default</a> | <a href="?style=so">SO</a> | <a href="?style=astro">Astro</a>
	    </div>
	</div>
</div>
<div id="halp">Counts refreshed every 5 seconds by default; interval can be changed by using dropdown above. 'Next' column shows gap to next rank up. 'Change' column shows change since last refresh. 'Accum' column shows total change since page load. Press 'Reset' at the bottom of the table to reset the 'Accum' column's start point.</div>
<hr>
<div id="appinfo">Author: <a  href="http://stackoverflow.com/users/616460">Jason C</a> | Version: <span id="version-number"></span> | <a href="javascript:toggleDebug();">Show Debug Info</a> | <a  href="http://meta.stackoverflow.com/questions/290346">Vote Monitor Meta Page</a></div>
</body>
</html>