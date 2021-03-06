package sovotemon.qa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ScrapeUtils {

    
    public static Element queryCommentsTable (URL siteUrl, int postId) throws IOException {
        
        URL commentUrl = new URL(siteUrl, "/posts/" + postId + "/comments");
        System.out.println("querying comments from " + commentUrl);
        
        URLConnection conn = commentUrl.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        StringBuilder fragment = new StringBuilder("<table><tbody>");
        
        String line;
        while ((line = in.readLine()) != null)
            fragment.append(line).append("\n");
        
        in.close();
        
        fragment.append("</tbody></table>");
        
        Document doc = Jsoup.parseBodyFragment(fragment.toString(), siteUrl.toString());
        return doc.getElementsByTag("table").first();
        
    }

    
    public static Element queryCommentsIfNeeded (URL url, Element commentDiv, int postId, boolean clean) {
    
        Element comments = null;
        
        if (commentDiv != null) {
            // if there are more comments we have to load them explicitly, otherwise we can just use the ones here.
            Element showLink = commentDiv.parent().getElementsByClass("js-show-link").first();
            if (showLink == null || showLink.hasClass("dno")) {
                // no more comments, no need for another query
                System.out.println("all comments loaded for " + postId);
                comments = commentDiv.getElementsByTag("table").first();
            } else {
                System.out.println("loading additional comments for " + postId);
                try {
                    Thread.sleep(2000);
                    comments = queryCommentsTable(url, postId);
                } catch (Exception x) {
                    System.err.println("when loading comments: " + x.getClass().getSimpleName() + ": " + x.getMessage());
                }
            }
        }
        
        if (clean)
            cleanSOComments(comments);
        
        return comments;
        
    }

    
//    private static class Counter {
//        int value;
//    }
    
    
    public static void cleanSOComments (Element comments) {
        
        // generate a report for debugging
//        Map<String,Counter> attrCounts = new TreeMap<String,Counter>();
//        for (Element e : comments.getAllElements()) {
//            for (Attribute a : e.attributes()) {
//                Counter c = attrCounts.get(a.getKey());
//                if (c == null) {
//                    c = new Counter();
//                    attrCounts.put(a.getKey(), c);
//                }
//                c.value ++;
//            }
//        }
//        System.out.println("=== comment attributes ===");
//        for (Map.Entry<String,Counter> e : attrCounts.entrySet()) {
//            System.out.println(e.getKey() + " => " + e.getValue().value);
//        }     
        
        for (Element e : comments.getAllElements()) {
            e.removeAttr("dir");
            e.removeAttr("id");
            e.removeAttr("rel");
            e.removeAttr("title");
            for (String data : e.attributes().dataset().keySet())
                e.removeAttr(data);
        }
        
        
    }
    
    
}
