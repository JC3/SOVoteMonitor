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
String qtext = (qa.getQuestion(number) == null ? null : qa.getQuestion(number).text);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>SO 2015 Candidate Questionnaire Viewer</title>
<link rel="stylesheet" type="text/css" href="qa.css"/>
</head>
<body>
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
    <% if (qtext != null) { %><blockquote class="question-text"><%= qtext %></blockquote><% } %>
</div>
<%
boolean first = true;
for (QAContextListener.Response r : qa.responses) {
    QAContextListener.Answer answer = r.getAnswer(number);
    String nonetext = (number == 0 ? "No introduction, that's OK!" : "No response provided.");
%>
<div class="response<%= first ? " first" : "" %>">
    <a name="<%= r.userId %>"></a>
    <div class="response-header">
	    <h2 class="response-name"><%= r.displayName %></h2>
	    <div class="response-nav">
	        <% if (!r.missing) { %>
	        [<a href="?q=<%=prevnumber%>#<%=r.userId%>">prev</a>]
	        [<a href="?q=<%=nextnumber%>#<%=r.userId%>">next</a>]
	        <% } %>
	    </div>
	</div>
    <% if (r.missing) { %>
    <blockquote class="response-text"><span class="response-missing">This user has not responded to the questionnaire yet.</span></blockquote>
    <% } else { %>
    <div class="response-time"><a href="<%= r.answerUrl == null ? "" : r.answerUrl.toString() %>"><%= r.timeText == null ? "" : r.timeText %></a></div>
    <% if (qtext != null) { %><blockquote class="question-text"><%=number %>. <%= qtext %></blockquote><% } %>
    <blockquote class="response-text"><%= answer == null ? ("<span class=\"response-empty\">"+nonetext+"</span>") : answer.html %></blockquote>
    <% } %>
</div>
<% 
    first = false;
} 
%>
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
<div id="appinfo">Author: <a target="_blank" href="http://stackoverflow.com/users/616460">Jason C</a> | Version: <%= QAContextListener.VERSION %> | <a href="index.jsp">Live Vote Monitor!</a> | Please do not share outside of SO posts (Twitter, etc.), thanks!</div>
</body>
</html>