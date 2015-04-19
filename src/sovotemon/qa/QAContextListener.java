package sovotemon.qa;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


/**
 * Every 60 seconds QA page is scraped for answers and parsed. Context attribute "qamonitor"
 * is set to an instance of this. Servlets access through that.
 */
@WebListener
public class QAContextListener implements ServletContextListener {

    
    public static final int VERSION = 6;
    
    private static final String QA_PAGE = "http://meta.stackoverflow.com/questions/290096";
    private static final String NOMINATIONS_PAGE = "http://stackoverflow.com/election/6?tab=nomination&all=true";
    
    private ScheduledExecutorService executor;    
   
    
    private volatile QA qa;
    private final ReadWriteLock qaLock = new ReentrantReadWriteLock();

    
    public QA getQA () {
        try {
            qaLock.readLock().lock();
            return qa;
        } finally {
            qaLock.readLock().unlock();
        }
    }
    
    
    private void updateQA () throws Exception {

        // hard coded list of candidate names to build a list of people that 
        // haven't responded yet...  todo: not hard-coded
        Map<Integer,String> expected = new HashMap<Integer,String>();
        expected.put(100297, "Martijn Pieters");
        expected.put(229044, "meagar");
        expected.put(1252759, "Jon Clements");
        expected.put(444991, "Matt");
        expected.put(871050, "Second Rikudo");
        expected.put(476, "deceze");
        expected.put(1069068, "Raghav Sood");
        expected.put(379693, "Paresh Mayani");
        expected.put(1114, "Jeremy Banks");
        expected.put(2057919, "Ed Cottrell");
        // done with that
        
        QA qa = QAParser.parseQA(new URL(QA_PAGE), expected);
        Thread.sleep(2000); // more spacing for page query to prevent throttling
        NominationsParser.mergeIntoQA(qa, new URL(NOMINATIONS_PAGE));
        
        try {
            qaLock.writeLock().lock();
            this.qa = qa;
        } finally {
            qaLock.writeLock().unlock();
        }
        
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
