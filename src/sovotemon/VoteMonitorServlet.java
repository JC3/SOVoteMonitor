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

	private static final int VERSION = 5; // if incremented, existing users will see a popup telling them to refresh.
	
	
	private MonitorContextListener monitor;

    
	@Override public void init(ServletConfig config) throws ServletException {
	    monitor = (MonitorContextListener)config.getServletContext().getAttribute("monitor");
	}

	
	@Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    String json;
	    
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
	        jsons.append("}");
	        json = jsons.toString();
	    } else {
	        // request is for vote data; note Arrays.toString() encloses in []'s.
	        json = String.format("{\"v\":%s,\"r\":%d}", Arrays.toString(monitor.getVotes()).replace(" ", ""), VERSION);
	    }
	    
	    response.setContentType("application/json");
        response.getOutputStream().write(json.getBytes("us-ascii"));
        
	}


	@Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    // whatever.
	    doGet(request, response);
	    
	}

}
