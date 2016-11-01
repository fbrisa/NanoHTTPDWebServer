package nanohttpdwebserver.controller;

import fi.iki.elonen.NanoHTTPD;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Paul S. Hawke (paul.hawke@gmail.com)
*         On: 9/14/13 at 8:09 AM
*/
public abstract class WebServerController {
    //Map<String, Object> execMe(Map<String, String> headers, IHTTPSession session, String uri,Map<String, Object> context);

    protected static final Logger LOGGER = LoggerFactory.getLogger(WebServerController.class);
    
    /**
     *
     * @param headers
     * @param session
     * @param uri
     * @param context
     * @return
     */
    abstract public NanoHTTPD.Response execMe(Map<String, String> headers, IHTTPSession session, String uri,Map<String, Object> context);
}
