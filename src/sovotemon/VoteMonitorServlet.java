package sovotemon;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet provides ajax endpoints. Serves candidate info when request parameter t is 'c',
 * servers vote count array otherwise.
 */
@WebServlet("/votes")
public class VoteMonitorServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final int VERSION = 14; // if incremented, existing users will see a popup telling them to refresh.
	
	
	private MonitorContextListener monitor;

    
	@Override public void init(ServletConfig config) throws ServletException {
	    monitor = (MonitorContextListener)config.getServletContext().getAttribute("monitor");
	}

	
	@Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    String json = null;
	    
	    // after this if block, if json == null this signifies data not modified
	    if ("c".equals(request.getParameter("t"))) {
	        // request is for candidate info
	        StringBuilder jsons = new StringBuilder("{\"c\":[");
	        for (int n = 0; n < monitor.getCandidateCount(); ++ n) {
	            CandidateInfo ci = monitor.getCandidateInfo(n);
	            if (n > 0) jsons.append(",");
	            jsons.append("{\"n\":\"");
	            jsons.append(ci.displayName);
	            jsons.append("\",\"i\":");
	            jsons.append(Integer.toString(ci.userId));
	            jsons.append("}");
	        }
	        jsons.append("],\"r\":");
	        jsons.append(VERSION);
	        jsons.append(",\"e\":");
	        jsons.append((monitor.getPrimaryEndDate() == null) ? "null" : monitor.getPrimaryEndDate().getTime());
	        jsons.append("}");
	        json = jsons.toString();
	    } else {
	        // request is for vote data 
	        String sstr = request.getParameter("s");
	        int clientSerial = 0;
	        if (sstr != null)
	            try { clientSerial = Integer.parseInt(sstr); } catch (NumberFormatException x) { /* ignore */ }
	        MonitorContextListener.Votes votes = monitor.getVotes(clientSerial);
	        if (votes != null) {
	            StringBuilder jsons = new StringBuilder("{");
	            // note Arrays.toString() encloses in []'s.
	            jsons.append(String.format("\"v\":%s,", Arrays.toString(votes.votes).replace(" ", "")));
	            jsons.append(String.format("\"w\":%s,", Arrays.toString(getInactiveIndices(votes.active)).replace(" ", "")));
	            jsons.append(String.format("\"r\":%d,", VERSION));
	            jsons.append(String.format("\"s\":%d}", votes.serial));
	            json = jsons.toString();
	        }
	    }

	    if (json != null) {
    	    response.setContentType("application/json");
            response.getOutputStream().write(json.getBytes("us-ascii"));
	    } else {
	        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
	    }
        
	}

	
	private static int[] getInactiveIndices (boolean[] active) {
	    int indices[] = new int[active.length], n = 0;
	    for (int j = 0; j < active.length; ++ j)
	        if (!active[j])
	            indices[n ++] = j;
	    return Arrays.copyOf(indices, n);
	}
	

	@Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    // whatever.
	    doGet(request, response);
	    
	}

}
