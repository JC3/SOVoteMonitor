package sovotemon;


public class CandidateInfo implements Comparable<CandidateInfo> {

    final String postId;
    final int userId;
    final String displayName;
    
    volatile int voteCount;         // read/write thread, inside of lock
    volatile int voteCountPending;  // write thread, outside of lock
    
    CandidateInfo (String postId, int userId, String displayName) { 
        this.postId = postId;
        this.userId = userId;
        this.displayName = displayName; 
    }
    
    @Override public int compareTo (CandidateInfo o) {
        return userId - o.userId;
    }
    
    @Override public String toString () {
        return String.format("CandidateInfo: %s %s %s", postId, userId, displayName);
    }
    
}