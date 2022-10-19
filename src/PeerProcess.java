import java.net.Socket;
import java.net.ServerSocket;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class PeerProcess 
{
    public static Vector<PeerInfo> peerInfo = new Vector<PeerInfo>();
    public static Logger logger = Logger.getLogger("PeerLog");

    public static void connectToPeers(String currPeerID, int currIndex)
	{
        Socket requestSocket; 
		
        try{
            for(int i = 0; i< currIndex; i++){
                // Get the information of peers that are already active(the list of peers are started in order)
                PeerInfo peer = peerInfo.get(i);
                // Make a TCP connection to that peer
			    requestSocket = new Socket(peer.peerAddress,Integer.parseInt(peer.peerPort));
                
                // Should be sent to a log file along with timestamp
			    logger.info("Peer " + currPeerID + "makes a connection to Peer " + peer.peerId);
            }
		}
		catch (IOException e) {
    			System.err.println("Connection to peer failed. Peer initialised?");
		} 
	}
    public static void main(String[] args) {
        String inputPeerID = args[0];
        System.out.println(inputPeerID);
        
        int index = 0;
        String st;
        String parentDir = new File (System.getProperty("user.dir")).getParent();

        try {
            FileHandler logFileHandler = new FileHandler(parentDir + "/log_peer_" + inputPeerID + ".log");
            SimpleFormatter formatter = new SimpleFormatter();  
            
            logger.addHandler(logFileHandler);
            logFileHandler.setFormatter(formatter);

            String peerConfigFile = parentDir + "/PeerInfo.cfg";
            BufferedReader in = new BufferedReader(new FileReader(peerConfigFile));

            
            while((st = in.readLine()) != null) {
                String[] value = st.split("\\s+");
                String currentPeerID = value[0];
                System.out.println(currentPeerID);
                
                if (!currentPeerID.equals(inputPeerID)){
                    peerInfo.addElement(new PeerInfo(value[0], value[1], value[2]));
                }else{
                    String port = value[2];

                    // Add handler for logging current node's logs
                   
                    // Create a socket at the port for TCP connections
                    ServerSocket listener = new ServerSocket(Integer.parseInt(port));
                    logger.info("Socket created\n");
                    // Establish connections with the other peers 
                    connectToPeers(currentPeerID, index);
                }

                index++;        
            }

            in.close();
        }
        catch (IOException e)
        {
            System.out.println("File not found");
        }
    }
}