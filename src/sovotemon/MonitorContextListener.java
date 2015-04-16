package sovotemon;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.jsoup.nodes.Element;


/**
 * Candidate list is queried on context init then task is scheduled to periodically update
 * vote counts. Context attribute "monitor" is set to an instance of this. Servlets access
 * through that.
 */
@WebListener
public class MonitorContextListener implements ServletContextListener {

    private static final String ELECTIONS_PAGE = "http://stackoverflow.com/election/6?tab=primary";
    
    private ScheduledExecutorService executor;    
   
    // the collections are not modified after initial creation
    private final Map<String,CandidateInfo> candidatesByPost = new HashMap<String,CandidateInfo>();
    private final List<CandidateInfo> candidatesSorted = new ArrayList<CandidateInfo>();
    private volatile int updateSerial = 1; // start at 1; clients use 0 for initial value
    
    // lock is for CandidateInfo#voteCount and updateSerial access
    private final ReadWriteLock candidatesLock = new ReentrantReadWriteLock();

    
    /**
     * Query elections page for candidate info and initialize collections.
     */
    private void updateCandidates () throws Exception {

        URL url = new URL(ELECTIONS_PAGE);
        
        for (Element tr : Jsoup.parse(url, 10000).getElementsByTag("tr")) {

            try {
                                
                if (!tr.id().startsWith("post-"))
                    continue;
    
                Element info = tr.getElementsByClass("user-info").first();
                if (info == null)
                    continue;
                
                String postId = tr.id();
    
                Element details = info.getElementsByClass("user-details").first();
                if (details == null)
                    continue;
    
                Element profileLink = details.getElementsByTag("a").first();
                if (profileLink == null)
                    continue;
                
                URL profileUrl = new URL(url, profileLink.attr("href"));
                String userName = profileLink.ownText().trim();
                int userId;
    
                Matcher idMatcher = Pattern.compile(".*/users/([0-9]+)(?:/.*)*").matcher(profileUrl.toString());
                if (idMatcher.matches())
                    userId = Integer.parseInt(idMatcher.group(1));
                else
                    continue;
                
                CandidateInfo ci = new CandidateInfo(postId, userId, userName);
                candidatesByPost.put(postId, ci);
                candidatesSorted.add(ci);
    
            } catch (Exception x) {
                
                System.err.println("warning: " + x.getClass() + ": " + x.getMessage());
                continue;
                
            }
                
        }
        
        Collections.sort(candidatesSorted);
        for (CandidateInfo ci : candidatesSorted)
            System.out.println(ci);
        
    }
    
    
    /**
     * Query election page for current votes. Called periodically from executor thread.
     */
    private void updateVotes () {
        
        try {
    
            URL url = new URL(ELECTIONS_PAGE);
 
            // only voteCountPending set here so we don't have to hold the lock while parsing
            for (Element tr : Jsoup.parse(url, 10000).getElementsByTag("tr")) {
    
                try {
                                    
                    if (!tr.id().startsWith("post-"))
                        continue;
        
                    Element vp = tr.getElementsByClass("vote-count-post").first();
                    
                    String postId = tr.id();
                    int votes = Integer.parseInt(vp.ownText());
                    
                    CandidateInfo ci = candidatesByPost.get(postId);
                    if (ci != null)
                        ci.voteCountPending = votes;
        
                } catch (Exception x) {
                    
                    System.err.println("warning: " + x.getClass() + ": " + x.getMessage());
                    continue;
                    
                }
                
            }
            
        } catch (Exception x) {

            x.printStackTrace();
            System.err.println("update failed: " + x.getClass() + ": " + x.getMessage());

        }

        // now copy to voteCount while locked
        try {
            candidatesLock.writeLock().lock();
            boolean changed = false;
            for (CandidateInfo ci : candidatesSorted) {
                if (ci.voteCount != ci.voteCountPending) {
                    changed = true;
                    ci.voteCount = ci.voteCountPending;
                }
            }
            if (changed)
                ++ updateSerial;
        } finally {
            candidatesLock.writeLock().unlock();
        }

    }
    

    public static class Votes {
        final int[] votes;
        final long serial;
        Votes (int[] votes, long serial) { this.votes = votes; this.serial = serial; }
    }
    
    
    /**
     * Get last updated vote counts.
     * @param updateSerial Serial number to facilitate 304 responses. Pass -1 to guarantee non-null return.
     * @return Array, index corresponds to index in candidatesSorted, or null if currentSerial same as updateSerial.
     */
    public Votes getVotes (int currentSerial) {

        Votes votes = null;
        
        try {
            candidatesLock.readLock().lock();
            if (currentSerial != updateSerial) {
                int[] values = new int[candidatesSorted.size()];
                for (int n = 0; n < candidatesSorted.size(); ++ n)
                    values[n] = candidatesSorted.get(n).voteCount;
                votes = new Votes(values, updateSerial);
            }
        } finally {
            candidatesLock.readLock().unlock();
        }
        
        return votes;
        
    }
    
    
    /**
     * Get a copy of current data. 
     */
    public List<CandidateInfo> getSnapshot () {
        
        List<CandidateInfo> snapshot = new ArrayList<CandidateInfo>();
        
        try {
            candidatesLock.readLock().lock();
            for (int n = 0; n < candidatesSorted.size(); ++ n)
                snapshot.add(candidatesSorted.get(n).clone());
        } finally {
            candidatesLock.readLock().unlock();
        }
        
        return snapshot;

    }
    

    @Override public void contextInitialized (ServletContextEvent event) {
        
        System.out.println("Context initializing...");
        
        try {
            updateCandidates();
        } catch (Exception x) {
            x.printStackTrace();
        }
        
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new Runnable(){@Override public void run(){updateVotes();}}, 0, 3, TimeUnit.SECONDS);
        event.getServletContext().setAttribute("monitor", this);
        
    }

 
    @Override public void contextDestroyed (ServletContextEvent event) {
        
        executor.shutdownNow();
        
    }

    
    public int getCandidateCount () {
        
        return candidatesSorted.size();
        
    }
    
    
    public CandidateInfo getCandidateInfo (int n) {
        
        return candidatesSorted.get(n);
        
    }
    
    
}
