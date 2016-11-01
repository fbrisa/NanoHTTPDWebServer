package nanohttpdwebserver.controller;

import fi.iki.elonen.NanoHTTPD;
import nanohttpdwebserver.TwigWorker;
import java.io.File;
import java.util.Map;

public class WebServerControllerDefault2 extends WebServerController {
    @Override
    public NanoHTTPD.Response execMe(Map<String, String> headers, NanoHTTPD.IHTTPSession session, String uri,Map<String, Object> context) {
        
        context.put("websiteTitle", "My First Website 2");
        context.put("content", "My Interesting Content 2");
        
        return TwigWorker.serveFile(uri, headers, session, new File("www/index.html.twig"), null, context);
        
//        WebServerPlugin plugin = server.mimeTypeHandlers.get("text/html");
//        NanoHTTPD.Response response;
//        plugin.setServer(null,context);
//        response = plugin.serveFile(uri, headers, session, new File("www/index.html.twig"), "text/html");
//        return response;    
    }
}
