package org.braincopy.ws;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/sl")
public class TLEServlet extends HttpServlet {
    
  private static final long serialVersionUID = 1L;

  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
        
    PrintWriter out = res.getWriter();
    out.println("<html><body>");
    out.println("<h1>Hello Servlet !!</h1>");
    out.println("</body></html>");
  }
}