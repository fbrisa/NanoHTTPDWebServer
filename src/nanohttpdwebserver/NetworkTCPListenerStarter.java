package nanohttpdwebserver;

import java.io.IOException;


/**
 *
 * @author francesco
 */
public class NetworkTCPListenerStarter  implements Runnable {
    NetworkTCPListener nuovoListener;
    
    
    public NetworkTCPListener startListeningOnPort(int port) {
        nuovoListener=new NetworkTCPListener(port);
        
        Thread threadListener=new Thread(this);
        threadListener.start();
        
        return nuovoListener;
    }
    
    public NetworkTCPListener startListeningOnPort() {
        return startListeningOnPort(8777);
    }

    @Override
    public void run() {
        try {
            nuovoListener.start();
        } catch(IOException e){
          System.err.println("Error in server : "+e);
        }
    }        
}
