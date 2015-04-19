<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" session="false"%>
<%@ page import="sovotemon.qa.*" %>
<%
QAContextListener monitor = (QAContextListener)getServletContext().getAttribute("qamonitor");
if (monitor == null)
    return;

QA qa = monitor.getQA();
if (qa == null) {
    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server recently restarted and is now initializing. Please try again in a few seconds.");
    return;
}
if (qa.topics == null || qa.responses == null)
    return;

String topicId = request.getParameter("q");
topicId = (topicId == null ? "" : topicId.trim());
if (topicId.isEmpty()) topicId = QA.Topic.INTRODUCTION_ID;

String prevId = qa.advanceId(topicId, -1);
String nextId = qa.advanceId(topicId, 1);

QA.Topic topic = qa.getTopic(topicId);
String title = (topic == null ? null : topic.title);
String qhtml = (topic == null ? null : topic.html);
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>SO 2015 Candidate Questionnaire Viewer</title>
<link rel="stylesheet" type="text/css" href="qa.css"/>
<style type="text/css">
/* http://stackoverflow.com/a/14393575/616460 */
html, body {
    overflow-x: hidden;
}
.commentbox {
    background: #fafafa;
    padding: 12px;
    border: 1px solid #ccc;
    width: 550px;
    position: fixed;
    /*top: 50px;*/
    bottom: 25px;
    right: -576px;
    opacity: 0;
    overflow-y: scroll;
}
</style>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
<script type="text/javascript">
function fixPage () {
    // header
    var height = $("#header").outerHeight();
    $("#fake-header").height(height);
    var anchorOffset = $("#header").outerHeight();
    // fix anchors for header
    $(".anchor").css({'height':anchorOffset,'margin-top':-anchorOffset});
    // adjust comment box top to not overlap nav bar
    $(".commentbox").css({'top':$("#navbar").outerHeight() + 25});
    // get rid of cellspacing on comment tables because i'm too lazy to do it on the back-end
    $(".commentbox>table").each(function(){$(this).attr('cellspacing','0')});
}
function areCommentsVisible (userId) {
    return $("#commentbox-"+userId).hasClass("commentbox-shown");
}
function setCommentsVisible (userId, show) {
    if (show != areCommentsVisible(userId)) {
        if (show)
            $("#commentbox-"+userId).animate({right:'25px',opacity:'1'},300).addClass("commentbox-shown");
        else
            $("#commentbox-"+userId).animate({right:'-576px',opacity:'0'},300).removeClass("commentbox-shown");
    }
}
function showOrHideComments (userId) {
    $(".commentbox").each(function () {
        var cbUserId = $(this).attr("data-userid");
        setCommentsVisible(cbUserId, (userId == cbUserId) && !areCommentsVisible(cbUserId))
    }); 
}
</script>
</head>
<body onload="fixPage();">
<div id="header">
    <div id="navbar" class="nav">
        <div class="nav-left"><a href="?q=<%=prevId%>">&larr; Previous</a></div>
        <div class="nav-right"><a href="?q=<%=nextId%>">Next &rarr;</a></div>
        <div class="nav-center">
            <%
            boolean first = true;
            for (QA.Topic t : qa.topics) {
               if (t.id.equalsIgnoreCase(topicId)) { 
               %><%=first?"":" | "%><span class="nav-current"><%= t.shortTitle %></span><%
               } else {
               %><%=first?"":" | "%><a href="?q=<%=t.id%>"><%=t.shortTitle%></a><%
               }
               first = false;
            }
            %>
        </div>
    </div>
    <div class="question">
        <h1 class="question-title"><%= title %></h1>
        <% if (qhtml != null) { %><blockquote class="question-text"><%= qhtml %></blockquote><% } %>
    </div>
</div>
<div id="fake-header"></div>
<div id="page">
    <%
    for (QA.Response r : qa.responses) {
        QA.Answer answer = r.getAnswer(topicId);
        String nonetext = (topicId.equals(QA.Topic.INTRODUCTION_ID) ? "No introduction, that's OK!" : "No response provided.");
    %>
    <div class="response<%= first ? " first" : "" %><%= r.missing ? " missing" : ""%>">
        <a class="anchor" id="<%= r.userId %>"></a>
        <div class="response-header">
            <h2 class="response-name"><%= r.displayName %></h2>
            <div class="response-nav">
            <!-- i'm disabling these again, they work but i think they distract from the purpose of this tool; just go read the user's answer. -->
            <!-- no i'm not, just kidding. -->
            <!-- psyyyyyche! -->
            <!-- no actually here they are -->
                <% if (answer != null) { %>
                [<a href="?q=<%=prevId%>#<%=r.userId%>">prev</a>]
                [<a href="?q=<%=nextId%>#<%=r.userId%>">next</a>]
                [<a href="javascript:showOrHideComments(<%=r.userId%>);">comments</a>]
                <% } %>
            <!-- how about this instead, just for link sharing, since i already have the anchors... -->
            <!-- no actually how about not.
                [<a href="?q=<%=topicId%>#<%=r.userId%>">permalink</a>]
                -->
            </div>
        </div>
        <% if (answer != null) { %>
            <div class="response-time">
               <% if (answer.answerUrl != null) { %><a href="<%=answer.answerUrl.toString()%>"><%=answer.answerTime%></a><% } %>
               <% if (answer.revisionUrl != null) { %> (<a href="<%=answer.revisionUrl.toString()%>"><%=answer.revisionTime%></a>)<% } %>
            </div>
            <% if (answer.html == null) { %>
            <blockquote class="response-text"><span class="response-empty"><%=nonetext %></span></blockquote>
            <% } else { %>
            <blockquote class="response-text"><%= answer == null ? ("<span class=\"response-empty\">"+nonetext+"</span>") : answer.html %></blockquote>
            <% } %>
        <% } else if (!r.missing) { %>
            <blockquote class="response-text"><span class="response-empty"><%=nonetext %></span></blockquote>
        <% } else { %>
            <blockquote class="response-text"><span class="response-missing">This user has not responded to the questionnaire yet.</span></blockquote>
        <% } %>
    </div>
    <% 
        first = false;
    } 
    %>
    <div id="appinfo">Author: <a target="_blank" href="http://stackoverflow.com/users/616460">Jason C</a> | Version: <%= QAContextListener.VERSION %> | <a class="aboutlink" href="qa-about.html">About</a> | Please do not share outside of SO posts (Twitter, etc.), thanks!</div>
</div>
<%  
for (QA.Response r : qa.responses) {
    QA.Answer answer = r.getAnswer(topicId); 
    if (answer != null && answer.commentsHtml != null) { 
%>
<div class="commentbox" id="commentbox-<%=r.userId%>" data-userid="<%=r.userId%>">
    <span class="commentbox-hide">[<a href="javascript:setCommentsVisible(<%=r.userId%>,false);">hide</a>]</span>
    <h2 class="commentbox-title"><%= r.displayName %>: <%=answer.sourceName %> Comments</h2>
    <%=answer.commentsHtml %>
</div>
<%
    }
}
%>
</body>
</html>