package sovotemon;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/raw")
public class RawDataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;


	private MonitorContextListener monitor;

	   
    @Override public void init(ServletConfig config) throws ServletException {
        monitor = (MonitorContextListener)config.getServletContext().getAttribute("monitor");
    }
    
    	   
	@Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	    List<CandidateInfo> candidates = monitor.getSnapshot();
	    Collections.sort(candidates, new Comparator<CandidateInfo>() {
            @Override public int compare (CandidateInfo a, CandidateInfo b) {
                return b.voteCount - a.voteCount;
            }
	    });
	    
	    StringBuilder s = new StringBuilder();
	    for (CandidateInfo c : candidates)
	        s.append(String.format("\"%s\" %d\n", c.displayName, c.voteCount));
	    
        response.setContentType("text/plain");
        response.getOutputStream().write(s.toString().getBytes("us-ascii"));

	}


	@Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	    doGet(request, response);
	
	}

	
}
