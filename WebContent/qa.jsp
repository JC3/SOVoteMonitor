<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" session="false"%>
<%@ page import="sovotemon.qa.QAContextListener" %>
<%
QAContextListener monitor = (QAContextListener)getServletContext().getAttribute("qamonitor");
if (monitor == null)
    return;

QAContextListener.QA qa = monitor.getQA();
if (qa.questions == null || qa.responses == null)
    return;

int number = 0;
try {
    number = Integer.parseInt(request.getParameter("q"));
} catch (Exception x) {
}

int prevnumber = (number + qa.questions.size()) % (qa.questions.size() + 1);
int nextnumber = (number + 1) % (qa.questions.size() + 1);

String title = (number == 0 ? "Introductions" : ("Question #" + number));
String qhtml = (qa.getQuestion(number) == null ? null : qa.getQuestion(number).html);
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>SO 2015 Candidate Questionnaire Viewer</title>
<link rel="stylesheet" type="text/css" href="qa.css"/>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
<script type="text/javascript">
function fixHeader () {
	var height = $("#header").height();
	$("#fake-header").height(height);
	console.log(height);
}
function toggleMissing () {
    $(".missing").toggle();
    if ($(".missing").is(":visible")) {
    	$("#hideshow").text("Hide Missing Responses");
    } else {
    	$("#hideshow").text("Show Missing Responses");
    }
}
</script>
</head>
<body onload="fixHeader();">
<div id="header">
	<div class="nav">
	    <div class="nav-left"><a href="?q=<%=prevnumber%>">&larr; Previous</a></div>
	    <div class="nav-right"><a href="?q=<%=nextnumber%>">Next &rarr;</a></div>
	    <div class="nav-center">
			<% if (number == 0) { %><span class="nav-current">Intros</span><% } else { %><a href="?q=0">Intros</a><% } %>
			<%
			for (QAContextListener.Question q : qa.questions) { 
			   if (number == q.number) { 
			   %> | <span class="nav-current">#<%= q.number %></span><%
			   } else {
			   %> | <a href="?q=<%=q.number%>">#<%=q.number%></a><%
			   }
			}
			%>
		</div>
	</div>
	<div class="question">
	    <h1 class="question-title"><%= title %></h1>
	    <% if (qhtml != null) { %><blockquote class="question-text"><%= qhtml %></blockquote><% } %>
	</div>
	<div class="misc">
	    <a href="javascript:toggleMissing();" id="hideshow">Hide Missing Responses</a>
	</div>
</div>
<div id="fake-header"></div>
<div id="page">
	<%
	boolean first = false; // not used right now
	for (QAContextListener.Response r : qa.responses) {
	    QAContextListener.Answer answer = r.getAnswer(number);
	    String nonetext = (number == 0 ? "No introduction, that's OK!" : "No response provided.");
	%>
	<div class="response<%= first ? " first" : "" %><%= r.missing ? " missing" : ""%>">
	    <a name="<%= r.userId %>"></a>
	    <div class="response-header">
		    <h2 class="response-name"><%= r.displayName %></h2>
		    <!-- disabled until i can figure out how to make these work with dynamically sized header
		    <div class="response-nav">
		        <% if (!r.missing) { %>
		        [<a href="?q=<%=prevnumber%>#<%=r.userId%>">prev</a>]
		        [<a href="?q=<%=nextnumber%>#<%=r.userId%>">next</a>]
		        <% } %>
		    </div>
		    -->
		</div>
	    <% if (r.missing) { %>
	    <blockquote class="response-text"><span class="response-missing">This user has not responded to the questionnaire yet.</span></blockquote>
	    <% } else { %>
	    <div class="response-time"><a href="<%= r.answerUrl == null ? "" : r.answerUrl.toString() %>"><%= r.timeText == null ? "" : r.timeText %></a></div>
	    <blockquote class="response-text"><%= answer == null ? ("<span class=\"response-empty\">"+nonetext+"</span>") : answer.html %></blockquote>
	    <% } %>
	</div>
	<% 
	    first = false;
	} 
	%>
    <div id="appinfo">Author: <a target="_blank" href="http://stackoverflow.com/users/616460">Jason C</a> | Version: <%= QAContextListener.VERSION %> | <a href="index.jsp">Live Vote Monitor!</a> | Please do not share outside of SO posts (Twitter, etc.), thanks!</div>
</div>
</body>
</html>