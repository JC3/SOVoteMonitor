package sovotemon.qa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextMatcher {
    
    
    private static Set<String> getWords (String text) {
        String[] wordlist = text.toLowerCase().split("\\W+");
        return new HashSet<String>(Arrays.asList(wordlist));
    }
    
    
    private static float getScore (String a, String b) {
        if (a == null || b == null) return 0.0f;
        Set<String> i = getWords(a);
        if (i.isEmpty()) return 0.0f;
        float osize = i.size();
        i.retainAll(getWords(b));
        return (float)i.size() / osize;        
    }
    
    
    public static class Result {
        final int index;
        final float score;
        Result (int n, float s) { index = n; score = s; }
    }
    

    public static Result findMatch (String t, List<String> ts, float minscore) {
        float maxscore = 0;
        int maxindex = -1;
        for (int n = 0; n < ts.size(); ++ n) {
            float score = getScore(t, ts.get(n));
            if (maxindex == -1 || score > maxscore) {
                maxindex = n;
                maxscore = score;
            }
        }
        return maxscore >= minscore ? new Result(maxindex, maxscore) : null;
    }
    
    
}
