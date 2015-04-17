package sovotemon.qa;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


/**
 * Every 60 seconds QA page is scraped for answers and parsed. Context attribute "qamonitor"
 * is set to an instance of this. Servlets access through that.
 */
@WebListener
public class QAContextListener implements ServletContextListener {

    
    public static final int VERSION = 2;
    
    
    public static class Question {
        public final int number;
        public final String text;
        public final String html;
        Question (int n, String t, String h) { number = n; html = h; text = t; }
    }
    
    
    public static class Answer {
        public final int number; // 0 = intro
        public final String html;
        Answer (int n, String h) { number = n; html = h; }
    }
    
    
    public static class Response implements Comparable<Response> {
        public final int userId;
        public final URL answerUrl;
        public final String displayName;
        public final String timeText;
        public final List<Answer> answers = new ArrayList<Answer>();
        public final boolean missing;
        Response (int u, URL url, String d, String t) { userId = u; answerUrl = url; displayName = d; timeText = t; missing = false; }
        Response (String d) { userId = 0; answerUrl = null; displayName = d; timeText = null; missing = true; }
        
        public Answer getAnswer (int number) {
            for (Answer a : answers)
                if (a.number == number)
                    return a;
            return null;
        }
        
        public void addAnswer (Answer a) {
            if (getAnswer(a.number) != null)
                System.out.println("Duplicate answer " + a.number + " for user " + displayName);
            else
                answers.add(a);
        }
        
        boolean hasAllAnswers (List<Question> questions) {
            boolean hasall = true;
            for (Question q : questions) {
                if (getAnswer(q.number) == null) {
                    hasall = false;
                    System.out.println("Missing answer for " + q.number + " for user " + displayName);
                }
            }
            return hasall;
        }
        
        @Override public int compareTo (Response r) {
            if (r == null)
                return -1;
            else
                return displayName.compareToIgnoreCase(r.displayName);
        }
        
    }
    
    public static class QA {
        public final List<Question> questions;
        public final List<Response> responses;
        QA (List<Question> q, List<Response> r) {
            questions = q;
            responses = r;
        }
        public Question getQuestion (int number) {
            for (Question q : questions)
                if (q.number == number)
                    return q;
            return null;
        }
    }
    
    private static final String QA_PAGE = "http://meta.stackoverflow.com/questions/290096";
    
    private ScheduledExecutorService executor;    
   
    
    private volatile List<Question> questions;
    private volatile List<Response> responses;
    private final ReadWriteLock qaLock = new ReentrantReadWriteLock();

    
    public QA getQA () {
        try {
            qaLock.readLock().lock();
            return new QA(questions, responses);
        } finally {
            qaLock.readLock().unlock();
        }
    }
    
    
    private static class ResponseInfo {
        final Element answer;
        final Element user;
        final Element time;
        ResponseInfo (Element a, Element u, Element t) { answer = a; user = u; time = t; }
    }
    
    
    private static boolean containsResponse (List<Response> responses, String name) {
        for (Response r : responses)
            if (name.equalsIgnoreCase(r.displayName))
                return true;
        return false;
    }
    
    
    private void updateQA () throws Exception {

        // hard coded list of candidate names to build a list of people that 
        // haven't responded yet...  todo: not hard-coded
        List<String> allNames = new ArrayList<String>();
        allNames.add("Martijn Pieters");
        allNames.add("meagar");
        allNames.add("Jon Clements");
        allNames.add("Matt");
        allNames.add("Second Rikudo");
        allNames.add("deceze");
        allNames.add("Raghav Sood");
        allNames.add("Paresh Mayani");
        allNames.add("Jeremy Banks");
        allNames.add("Ed Cottrell");
        /*allNames.add("Jason C");
        allNames.add("Undo");
        allNames.add("slugster");
        allNames.add("Qantas 94 Heavy");
        allNames.add("Andy");
        allNames.add("Sergey K.");
        allNames.add("vcsjones");
        allNames.add("rekire");
        allNames.add("Thomas Owens");
        allNames.add("Moshe");
        allNames.add("Michael Irigoyen");
        allNames.add("codeMagic");
        allNames.add("hichris123");
        allNames.add("Idan Adar");
        allNames.add("Mooseman");
        allNames.add("AstroCB");
        allNames.add("Unihedro");
        allNames.add("Shree");
        allNames.add("Hemang");*/
        // done with that
        
        Element questionElement = null;
        List<ResponseInfo> responseElements = new ArrayList<ResponseInfo>();

        for (int page = 1; page <= 2; ++ page) { // there will not be more than 2 pages of answers
            if (page > 1)
                Thread.sleep(2000); // avoid throttle
            URL url = new URL(QA_PAGE + "?tab=oldest&page=" + page);
            Document doc = Jsoup.parse(url, 10000);
            if (questionElement == null) {
                questionElement = doc.getElementById("question");
                if (questionElement != null)
                    questionElement = questionElement.getElementsByClass("post-text").first();
            }
            for (Element answerDiv : doc.getElementsByClass("answer")) {
                Element answer = answerDiv.getElementsByClass("post-text").first();
                Element info = answerDiv.getElementsByClass("reputation-score").first();
                if (info != null) info = info.parent();
                if (info != null) info = info.parent();
                if (info != null) {
                    Element user = info.getElementsByClass("user-details").first();
                    Element time = info.getElementsByClass("user-action-time").first();
                    if (answer != null && user != null) // let time be null, silly parse error to fail completely on
                        responseElements.add(new ResponseInfo(answer, user, time));
                }
            }
        }
        
        if (questionElement == null || responseElements.isEmpty())
            throw new Exception("No question/answers found.");
        
        List<Question> questions = parseQuestions(questionElement);
        for (Question q : questions)
            System.out.println("QA " + q.number + " => " + q.text);
      
        System.out.println("Response element sets: " + responseElements.size());
        List<Response> responses = new ArrayList<Response>();
        for (ResponseInfo info : responseElements)
            responses.add(parseResponse(questions, info));
        
        for (String name : allNames)
            if (!containsResponse(responses, name))
                responses.add(new Response(name));
        
        Collections.sort(responses);
        
        try {
            qaLock.writeLock().lock();
            this.questions = questions;
            this.responses = responses;
        } finally {
            qaLock.writeLock().unlock();
        }
        
    }
    
    
    private static List<Question> parseQuestions (Element e) {
        
        for (Element blockquote : e.getElementsByTag("blockquote")) {
            Element ol = blockquote.getElementsByTag("ol").first();
            if (ol != null) {
                List<Question> questions = new ArrayList<Question>();
                int number = 1;
                for (Element li : ol.getElementsByTag("li")) {
                    Question q = new Question(number ++, li.text(), li.html());
                    questions.add(q);
                }
                return questions;
            }
        }
        
        return Collections.emptyList();
        
    }
    
    
    private static int parseUserId (String path) {
        
        // everybody's favorite regex
        Matcher idMatcher = Pattern.compile(".*/users/([0-9]+)(?:/.*)*").matcher(path);
        if (idMatcher.matches())
            return Integer.parseInt(idMatcher.group(1));
        else
            return -1;

    }
    
    
    private static Response parseResponse (List<Question> questions, ResponseInfo info) {
        
        // user info
        
        Element userlink = info.user.getElementsByAttribute("href").first();
        if (userlink == null)
            return null;
       
        String displayName = userlink.ownText().trim();
        int userId = parseUserId(userlink.attr("href"));
        String timeText = (info.time == null ? "" : info.time.text().trim());
        URL answerUrl = null;
        
        for (Element div = info.user.parent(); div != null && answerUrl == null; div = div.parent())
            if (div.hasAttr("data-answerid"))
                try {
                    answerUrl = new URL(QA_PAGE + "#" + div.attr("data-answerid"));
                } catch (MalformedURLException x) {
                }

        Response response;
        if (!displayName.isEmpty() && userId > 0) {
            response = new Response(userId, answerUrl, displayName, timeText);
        } else {
            return null;
        }
        
        System.out.println("RESPONSE " + response.userId + " [" + response.displayName + "]: " + response.timeText);
        
        // answers

        List<String> qstrings = new ArrayList<String>();
        for (Question q : questions)
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
                        response.addAnswer(new Answer(number, currentBlock.toString()));
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
            response.addAnswer(new Answer(number, currentBlock.toString()));

        response.hasAllAnswers(questions);
        
        return response;
        
    }
    
    
    @Override public void contextInitialized (ServletContextEvent event) {
        
        System.out.println("QA monitor initializing...");
        
        executor = Executors.newScheduledThreadPool(1);
        
        executor.scheduleAtFixedRate(new Runnable(){
            @Override public void run(){
                try {
                    updateQA();
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        }, 0, 60, TimeUnit.SECONDS);
  
        event.getServletContext().setAttribute("qamonitor", this);
        
    }

 
    @Override public void contextDestroyed (ServletContextEvent event) {
        
        executor.shutdownNow();
        
    }
    
    
}
