package sovotemon.qa;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Results of the candidate questionnaire. Contains a list of questions, and a list of responses.
 * I've taken some liberties here and foregone protections like unmodifiable lists and getters and
 * such, because I don't care. Suck it, Java nerds.
 */
public class QA {
    
    public final List<Topic> topics;
    public final List<Response> responses;
     
    
    QA (List<Topic> t, List<Response> r) {
        topics = t;
        responses = r;
    }
    
    
    /**
     * Things are getting weird.
     * @return Empty string if ID not found; never returns null.
     */
    public String advanceId (String id, int by) {
        int index = -1;
        for (index = 0; index < topics.size(); ++ index)
            if (topics.get(index).id.equalsIgnoreCase(id.trim()))
                break;
        if (index == topics.size())
            return "";
        else
            return topics.get((((index + by) % topics.size()) + topics.size()) % topics.size()).id;
    }
    
    
    /**
     * @param id Topic ID (questions are "1" thru whatever, also intro, nomination, etc.)
     * @return The Topic, or null if no question with the number exists.
     */
    public Topic getTopic (String id) {
        for (Topic t : topics)
            if (t.id.equalsIgnoreCase(id.trim()))
                return t;
        return null;
    }
    
    
    /**
     * @param userId User ID.
     * @return Response for this user, null if not found.
     */
    public Response getResponse (int userId) {
        for (Response r : responses)
            if (r.userId == userId)
                return r;
        return null;
    }

    
    /**
     * A single topic; this can be either a question in the questionnaire (in which case the ID
     * is the question #) or some other source of information (like an intro or nomination post).
     * If applicable, contains the original html for rendering, and the original raw text for
     * matching.
     */
    public static class Topic {
        
        public static final String INTRODUCTION_ID = "i";
        public static final String NOMINATION_ID = "n";
        
        public static Topic createIntroductionTopic () {
            return new Topic(INTRODUCTION_ID, "Introductions", "Intros", null, null);
        }
        
        public static Topic createNominationTopic () {
            return new Topic(NOMINATION_ID, "Nomination Posts", "Nominations", null, null);
        }
   
        public final String id;
        public final String title;
        public final String shortTitle;
        public final String text;
        public final String html;
        
        Topic (String id, String title, String shortTitle, String text, String html) {
            this.id = id.trim();
            this.title = title;
            this.shortTitle = shortTitle;
            this.text = text;
            this.html = html;
        }
    
    }    
    
    
    /**
     * A single user's answer to a topic. Has an associated topic ID and the original html for
     * rendering.
     */
    public static class Answer {
    
        public final String topicId;
        public final String html;
        public final URL answerUrl;
        public final String answerTime;
        public URL revisionUrl;
        public String revisionTime;
        public String commentsHtml;
        public final String sourceName;
        
        Answer (String topicId, String html, URL answerUrl, String answerTime, String sourceName) {
            this.topicId = topicId;
            this.html = html;
            this.answerUrl = answerUrl;
            this.answerTime = answerTime;
            this.sourceName = sourceName;
        }
        
        void setEdited (URL revisionUrl, String editText) {
            this.revisionUrl = revisionUrl;
            this.revisionTime = editText;
        }
        
        void setComments (String html) {
            this.commentsHtml = html;
        }
        
    }
 
    
    /**
     * A single user's response to the questionnaire (and other topics). Contains some user and
     * post info and a list of answers to the topics.
     */
    public static class Response implements Comparable<Response> {
    
        public final int userId;
        public final String displayName;
        public final List<Answer> answers = new ArrayList<Answer>();
        public final boolean missing;
        
        Response (int userId, String displayName, boolean missing) { 
            this.userId = userId;
            this.displayName = displayName;
            this.missing = missing;
        }
                
        public Answer getAnswer (String topicId) {
            for (Answer a : answers)
                if (a.topicId.equalsIgnoreCase(topicId.trim()))
                    return a;
            return null;
        }
        
        public Answer addAnswer (Answer a) {
            if (getAnswer(a.topicId) != null)
                System.out.println("Duplicate answer " + a.topicId + " for user " + displayName);
            else
                answers.add(a);
            return a;
        }
        
        void debugHasAllAnswers (List<Topic> topics) {
            for (Topic t : topics)
                if (getAnswer(t.id) == null)
                    System.out.println("Missing answer for " + t.id + " for user " + displayName);
        }
    
        /**
         * Case-insensitive user display name compare.
         */
        @Override public int compareTo (Response r) {
            if (r == null)
                return -1;
            else
                return displayName.compareToIgnoreCase(r.displayName);
        }
        
    }    
    
}
