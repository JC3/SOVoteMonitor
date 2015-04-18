package sovotemon.qa;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class QAParser {
    
    
    /**
     * Parse a QA from a questionnaire post.
     * @param qaUrl URL, must not contain a query string due to developer apathy.
     * @param expectedResponses If not null, a map of user ids -> display names of expected users, for
     *        determining which responses are missing.
     * @return A fully parsed and probably accurate QA.
     * @throws Exception randomly.
     */
    public static QA parseQA (URL qaUrl, Map<Integer,String> expectedResponses) throws Exception {
        
        Element questionElement = null;
        List<ResponseElements> responseElements = new ArrayList<ResponseElements>();

        for (int page = 1; page <= 2; ++ page) { // just check 2 pages of answers (30 candidates or 10 candidates max).
         
            if (page > 1)
                Thread.sleep(2000); // avoid throttle
            URL url = new URL(qaUrl + "?tab=oldest&page=" + page);
            Document doc = Jsoup.parse(url, 10000);
            
            // get the question post element the first time we see it (doesn't matter what page)
            if (questionElement == null) {
                questionElement = doc.getElementById("question");
                if (questionElement != null)
                    questionElement = questionElement.getElementsByClass("post-text").first();
            }
            
            // get all the answer post elements and user info boxes
            for (Element answerDiv : doc.getElementsByClass("answer")) {
                Element answer = answerDiv.getElementsByClass("post-text").first();
                // there are one or two user-info's per post: [editor] original.
                // original code (that didn't care about edits) looked for the one with a reputation-score
                // child, but this breaks when the answer is edited by somebody other than the op (and the
                // editors rep is displayed). so, need to be more proper about this:
                Elements userinfos = answerDiv.getElementsByClass("user-info");
                if (!userinfos.isEmpty()) {
                    if (userinfos.size() > 2) 
                        System.out.println("YO! more than 2 user-infos!! FIX ME!");
                    // edit info
                    Element editor = (userinfos.size() == 1) ? null : userinfos.first();
                    Element edit = (editor == null) ? null : editor.getElementsByClass("user-action-time").first();
                    // poster info
                    Element poster = userinfos.last();
                    Element user = poster.getElementsByClass("user-details").first();
                    Element time = poster.getElementsByClass("user-action-time").first();
                    // might be good now
                    if (answer != null && user != null) // let time be null, silly parse error to fail completely on
                        responseElements.add(new ResponseElements(answer, user, time, edit));
                }
            }
            
        }
        
        if (questionElement == null)
            throw new Exception("No question post found.");
        if (responseElements.isEmpty())
            throw new Exception("No answers found.");
        
        List<QA.Question> questions = parseQuestions(questionElement);
        for (QA.Question q : questions)
            System.out.println("QA " + q.number + " => " + q.text);
      
        System.out.println("Response element sets: " + responseElements.size());
        List<QA.Response> responses = new ArrayList<QA.Response>();
        for (ResponseElements info : responseElements)
            responses.add(parseResponse(qaUrl, questions, info));

        if (expectedResponses != null) {
            for (Map.Entry<Integer,String> expected : expectedResponses.entrySet())
                if (!containsResponse(responses, expected.getKey())) {
                    responses.add(new QA.Response(expected.getValue()));
                    System.out.println("RESPONSE " + expected.getKey() + " [" + expected.getValue() + "]: missing");
                }
        }

        Collections.sort(responses);

        return new QA(questions, responses);
        
    }
    
    
    private static boolean containsResponse (List<QA.Response> responses, int userId) {
        for (QA.Response r : responses)
            if (r.userId == userId)
                return true;
        return false;
    }
    
    
    /**
     * Parse a list of QA questions from the questionnaire post's question->post-text element.
     * For now, looks for an ol inside a blockquote and treats each li as a question.
     * @param e The post-text element from the questionnaire post.
     * @return A list of Questions, or an empty list if none found.
     * @todo Expand to support possibly different formats on other sites (e.g. ul).
     */
    private static List<QA.Question> parseQuestions (Element e) {
        
        for (Element blockquote : e.getElementsByTag("blockquote")) {
            Element ol = blockquote.getElementsByTag("ol").first();
            if (ol != null) {
                List<QA.Question> questions = new ArrayList<QA.Question>();
                int number = 1;
                for (Element li : ol.getElementsByTag("li")) {
                    QA.Question q = new QA.Question(number ++, li.text(), li.html());
                    questions.add(q);
                }
                return questions;
            }
        }
        
        return Collections.emptyList();
        
    }
    
    
    /**
     * Extract user ID from a user profile link using an over-zealous regex that everybody
     * makes fun of.
     * @param path A link URL, e.g. "/user/1234" or "http://stackoverflow.com/user/1234/wherever".
     * @return A user ID, e.g. 1234, or -1 if it couldn't be parsed.
     */
    private static int parseUserId (String path) {
        
        // everybody's favorite regex
        Matcher idMatcher = Pattern.compile(".*/users/([0-9]+)(?:/.*)*").matcher(path);
        if (idMatcher.matches())
            return Integer.parseInt(idMatcher.group(1));
        else
            return -1;

    }
    
    
    /**
     * Parse an entire response from a set of page elements.
     * @param qaUrl QA URL for building links.
     * @param questions List of questions, so we can match response quotes with questions.
     * @param info Set of relevant page elements.
     * @return A response, or null if there was an error.
     */
    private static QA.Response parseResponse (URL qaUrl, List<QA.Question> questions, ResponseElements info) {
        
        // -------- user info
        
        Element userlink = info.user.getElementsByAttribute("href").first();
        if (userlink == null)
            return null;
       
        // basic user info
        
        String displayName = userlink.ownText().trim();
        int userId = parseUserId(userlink.attr("href"));
        
        // answer time and url
        
        String timeText = (info.time == null ? "" : info.time.text().trim());
        URL answerUrl = null;
        
        for (Element div = info.user.parent(); div != null && answerUrl == null; div = div.parent())
            if (div.hasAttr("data-answerid"))
                try {
                    answerUrl = new URL(qaUrl + "#" + div.attr("data-answerid"));
                } catch (MalformedURLException x) {
                }

        // edit time and revision url
        
        Element revisionLink = (info.edit == null ? null : info.edit.getElementsByAttribute("href").first());
        String editText = null;
        URL revisionUrl = null;
        if (revisionLink != null) {
            try {
                editText = (revisionLink == null ? null : revisionLink.text().trim());
                revisionUrl = (revisionLink == null ? null : new URL(qaUrl, revisionLink.attr("href")));
            } catch (MalformedURLException x) {
                editText = null;
                revisionUrl = null;
            }
        }
        
        // -------- done with that
        
        QA.Response response;
        if (!displayName.isEmpty() && userId > 0) {
            response = new QA.Response(userId, answerUrl, displayName, timeText);
            response.setEdited(revisionUrl, editText);
        } else {
            return null;
        }
        
        System.out.println("RESPONSE " + response.userId + " [" + response.displayName + "]: " + response.timeText + ", " + response.editText);
        
        // ------- answers

        List<String> qstrings = new ArrayList<String>();
        for (QA.Question q : questions)
            qstrings.add(q.text);
        
        StringBuilder currentBlock = new StringBuilder();
        int number = 0;

        for (Element element : info.answer.children()) {
             
            boolean consumed = false;
            
            if (element.tagName().equals("blockquote")) {
                TextMatcher.Result match = TextMatcher.findMatch(element.text(), qstrings, 0.8f);                
                if (match != null) {
                    //System.out.println(questions.get(match.index).number + " " + match.score);
                    //System.out.println("  Q => " + questions.get(match.index).text);
                    //System.out.println("  B => " + element.text());
                    if (currentBlock.length() > 0)
                        response.addAnswer(new QA.Answer(number, currentBlock.toString()));
                    currentBlock = new StringBuilder();
                    number = questions.get(match.index).number;
                    consumed = true;
                }
            }
            
            if (!consumed) {
                boolean skip = false;                
                if (currentBlock.length() == 0 && (element.tagName().equals("h1") || element.tagName().equals("h2"))) // skip the "whoever's answers" header.
                    skip = true;
                if (currentBlock.length() == 0 && element.text().toLowerCase().startsWith(response.displayName.toLowerCase() + "'s answers")) // codeMagic just used bold
                    skip = true;
                if (element.tagName().equals("hr"))
                    skip = true;
                if (!skip)
                    currentBlock.append(element.outerHtml());
            }
            
        }

        if (currentBlock.length() > 0)
            response.addAnswer(new QA.Answer(number, currentBlock.toString()));

        response.debugHasAllAnswers(questions);
        
        return response;
        
    }

    
    /**
     * A collection of important DOM nodes, patiently waiting to be parsed into a response.
     */
    private static class ResponseElements {
        
        final Element answer;
        final Element user;
        final Element time;
        final Element edit;
        
        ResponseElements (Element a, Element u, Element t, Element e) { 
            answer = a; 
            user = u; 
            time = t; 
            edit = e;
        }
        
    }
    

}
