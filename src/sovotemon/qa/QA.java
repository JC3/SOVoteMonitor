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

    
    public final List<Question> questions;
    public final List<Response> responses;
    
    
    QA (List<Question> q, List<Response> r) {
        questions = q;
        responses = r;
    }
    
    
    /**
     * @param number Question number, starts at 1.
     * @return The Question, or null if no question with the number exists.
     */
    public Question getQuestion (int number) {
        for (Question q : questions)
            if (q.number == number)
                return q;
        return null;
    }

    
    /**
     * A single question in the questionnaire. Has a number (1-based), the original html for 
     * rendering, and the original raw text for matching.
     */
    public static class Question {
   
        public final int number;
        public final String text;
        public final String html;
        
        Question (int n, String t, String h) { 
            number = n; 
            html = h; 
            text = t; 
        }
    
    }    
    
    
    /**
     * A single user's answer to a question. Has an associated question number (0 for intro)
     * and the original html for rendering.
     */
    public static class Answer {
    
        public final int number; // 0 = intro
        public final String html;
        
        Answer (int n, String h) { 
            number = n; 
            html = h; 
        }
        
    }
 
    
    /**
     * A single user's response to the questionnaire. Contains some user and post info and a
     * list of answers to the questions.
     */
    public static class Response implements Comparable<Response> {
    
        public final int userId;
        public final URL answerUrl;
        public final String displayName;
        public final String timeText;
        public URL revisionUrl;
        public String editText;
        public final List<Answer> answers = new ArrayList<Answer>();
        public final boolean missing;
        
        Response (int u, URL url, String d, String t) { 
            userId = u; 
            answerUrl = url; 
            displayName = d; 
            timeText = t;
            missing = false; 
        }
        
        Response (String d) {
            userId = 0;
            answerUrl = null; 
            displayName = d;
            timeText = null;
            missing = true;
        }
        
        void setEdited (URL revisionUrl, String editText) {
            this.revisionUrl = revisionUrl;
            this.editText = editText;
        }
        
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
        
        void debugHasAllAnswers (List<Question> questions) {
            for (Question q : questions)
                if (getAnswer(q.number) == null)
                    System.out.println("Missing answer for " + q.number + " for user " + displayName);
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
