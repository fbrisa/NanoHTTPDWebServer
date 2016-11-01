package nanohttpdwebserver;

import nanohttpdwebserver.controller.WebServerControllerDefault;
import java.io.File;
import nanohttpdwebserver.controller.WebServerControllerDefault2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 * @author francesco
 */
public class NanoHTTPDWebServer {

    private static Logger logger;
    
    private static boolean vivi=true;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        File configLog4jFile=new File("./config/log4j.xml");
        
        if (configLog4jFile.exists()) {
            System.setProperty("logback.configurationFile", configLog4jFile.getAbsolutePath());
        }
                
        logger = LoggerFactory.getLogger(NanoHTTPDWebServer.class);
        logger.info("Starting NanoHTTPDWebServer");
        
        
        
        
        NetworkTCPListener networkTCPListener=(new NetworkTCPListenerStarter()).startListeningOnPort();
        networkTCPListener.controllers.put("", new WebServerControllerDefault());        
        networkTCPListener.controllers.put("/otherPage", new WebServerControllerDefault2());        
        
        while (vivi) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) { }
        }
    }
}
