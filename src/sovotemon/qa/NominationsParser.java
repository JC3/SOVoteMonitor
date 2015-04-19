package sovotemon.qa;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


public class NominationsParser {

    
    public static void mergeIntoQA (QA qa, URL url) {

        Document doc;
        
        try {
            doc = Jsoup.parse(url, 10000);
        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("^ while parsing nomination page " + url);
            return;
        }
        
        // find nomination posts; nominations tab tr's don't have post ids so look for divs instead
        for (Element div : doc.getElementsByTag("div")) {

            try {
                                
                if (!div.id().startsWith("post-"))
                    continue;
    
                Element info = div.getElementsByClass("user-info").first();
                if (info == null)
                    continue;
                
                String postId = div.id();
                URL postUrl = new URL(url, "#" + postId);
                // todo: /a/ instead of anchors for post links
                
                Element time = info.getElementsByClass("user-action-time").first();
                String postTime = (time == null ? "post link" : time.text()); // would be weird to be null, but put in something usable just in case
                
                URL revisionUrl = null;
                for (Element menu : div.getElementsByClass("post-menu")) {
                    for (Element link : menu.getElementsByTag("a")) {
                        if (link.hasAttr("href") && link.text().trim().equalsIgnoreCase("history")) {
                            revisionUrl = new URL(url, link.attr("href"));
                            break;
                        }
                    }
                    if (revisionUrl != null)
                        break;
                }
                // ps not using post link from menu, since it doesn't go to nominations tab.
    
                Element post = div.getElementsByClass("post-text").first();
                if (post == null)
                    continue;
                
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
                
                QA.Response response = qa.getResponse(userId);
                if (response == null)
                    continue; // this person isn't in the election; don't bother continuing

                Element comments = div.getElementsByClass("comments").first();
                try {
                    comments = ScrapeUtils.queryCommentsIfNeeded(url, comments, Integer.parseInt(postId.split("-")[1]), true);
                } catch (Exception x) {
                    System.err.println("when getting nomination comments: " + x.getClass().getSimpleName() + ": " + x.getMessage());
                    comments = null;
                }
                
                // hr's are annoying; also remove post-primary qa links that were edited in
                for (Element e : post.children()) {
                    if ("hr".equals(e.tagName())) {
                        e.remove();
                        continue;
                    }
                    // if it contains the qa link (todo: support for different forms in future), remove it
                    Element link = e.getElementsByTag("a").first();
                    if (link != null && link.text().toLowerCase().contains("answers to your questions")) {
                        e.remove();
                        continue;
                    }
                }
                
                // convert relative links to absolute (lots of tag links)
                for (Element e : post.getElementsByAttribute("href"))
                    e.attr("href", new URL(url, e.attr("href")).toString());
                if (comments != null)
                    for (Element e : comments.getElementsByAttribute("href")) // todo: get rid of this, ScrapeUtils does it for the query already
                        e.attr("href", new URL(url, e.attr("href")).toString());
                
                System.out.println("QA; nomination post found for " + userName + " " + userId);
                QA.Answer answer = response.addAnswer(new QA.Answer(QA.Topic.NOMINATION_ID, post.html(), postUrl, postTime, "Nomination"));
                answer.setEdited(revisionUrl, revisionUrl == null ? null : "revision history");
                answer.setComments(comments == null ? null : comments.outerHtml());
    
            } catch (Exception x) {
                
                x.printStackTrace();
                System.err.println("warning: " + x.getClass() + ": " + x.getMessage());
                continue;
                
            }
                
        }
        
        
    }
    
    
}
