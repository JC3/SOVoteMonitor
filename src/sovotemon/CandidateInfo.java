package sovotemon;


/**
 * Information about a candidate.
 */
public class CandidateInfo implements Comparable<CandidateInfo>, Cloneable {

    final String postId;
    final int userId;
    final String displayName;
    
    volatile int voteCount;         // read/write thread, inside of lock
    volatile boolean active;        // read/write thread, inside of lock
    volatile int voteCountPending;  // write thread, outside of lock
    volatile boolean activePending; // write thread, outside of lock
    
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
    
    /**
     * Clone will be identical except for pending data.
     */
    @Override public CandidateInfo clone () {
        CandidateInfo copy = new CandidateInfo(postId, userId, displayName);
        copy.voteCount = voteCount;
        copy.active = active;
        return copy;
    }
 
    /**
     * Commit pending data. This facilitates multi-threading. See MonitorContextListener.
     * @return True if data changed, false if not.
     */
    boolean commitPending () {
        boolean changed = (voteCount != voteCountPending) || (active != activePending);
        voteCount = voteCountPending;
        active = activePending;
        return changed;
    }
    
}