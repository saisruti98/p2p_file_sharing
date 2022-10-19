import java.net.Socket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;

public class PeerProcess 
{
    public static int myPeerID;
    public static ServerSocket mySocket;
    public static Vector<PeerInfo> peerInfo = new Vector<PeerInfo>();
    public static Map<Integer, Socket> peerSocketMap = new HashMap<Integer, Socket>();
    public static Logger logger = Logger.getLogger("PeerLog");

    public static void connectToPeers(String currPeerID, int currIndex)
	{
        Socket peerSocket; 
		
        
        // Unsure if the connections stay intact after program exits the control from this snippet! Have to figure out!!! Maybe store the socket info to map?

        try{
            for(int i = 0; i< currIndex; i++){
                // Get the information of peers that are already active(the list of peers are started in order)
                PeerInfo peer = peerInfo.get(i);
                // Make a TCP connection to that peer
			    peerSocket = new Socket(peer.peerAddress,Integer.parseInt(peer.peerPort));
                // Send current peerID to the other peer
                Handshake msg = new Handshake(Integer.parseInt(currPeerID));
                //ByteArrayOutputStream bos = new ByteArrayOutputStream();    
                DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream()); 
                out.write(msg.handshakeMsg); 

                Handshake recHandshake = null;

                while (recHandshake == null){
                    DataInputStream input = new DataInputStream(peerSocket.getInputStream()); 
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte buffer[] = new byte[32];
                    baos.write(buffer, 0 , input.read(buffer));
                    recHandshake = new Handshake(buffer);
                    
                    if(recHandshake.header.equals(msg.header)){
                        if(recHandshake.peerID == peer.peerId){
                            System.out.println("Yayyyyy");
                            // YAYyyyyyyy
                        }else{
                            System.out.println("Wrong peerid");
                        }
                    }else{
                        System.out.println("Wrong header");
                    }
                }
                
                // Add the peerID and its socket to the map
                peerSocketMap.put(peer.peerId, peerSocket);
                
                // Should be sent to a log file along with timestamp
			    logger.info("Peer " + currPeerID + " makes a connection to Peer " + peer.peerId);
            }
		}
		catch (IOException e) {
            e.printStackTrace();
    			System.err.println("Connection to peer failed. Peer initialised?");
		} 
	}
    public static void main(String[] args) {
        String inputPeerID = args[0];
        myPeerID = Integer.parseInt(inputPeerID);
        
        int index = 0;
        String st;
        String parentDir = new File (System.getProperty("user.dir")).getParent();

        try {
            // Add handler for logging current node's logs
            FileHandler logFileHandler = new FileHandler(parentDir + "/log_peer_" + inputPeerID + ".log");
            SimpleFormatter formatter = new SimpleFormatter();  
            
            logger.addHandler(logFileHandler);
            logFileHandler.setFormatter(formatter);

            String peerConfigFile = parentDir + "/PeerInfo.cfg";
            BufferedReader in = new BufferedReader(new FileReader(peerConfigFile));

            
            while((st = in.readLine()) != null) {
                String[] value = st.split("\\s+");
                String currentPeerID = value[0];
                
                if (!currentPeerID.equals(inputPeerID)){
                    peerInfo.addElement(new PeerInfo(Integer.parseInt(value[0]), value[1], value[2]));
                }else{
                    String port = value[2];
                    // Create a socket at the port for TCP connections
                    mySocket = new ServerSocket(Integer.parseInt(port));
                    logger.info("Socket created\n");
                    // Establish connections with the other peers 
                    connectToPeers(currentPeerID, index);
                }

                index++;        
            }

            in.close();

            // Keep the socket open for other peers to connect. Receives the peerID of the connecting node
            while(true){
                Socket socket = mySocket.accept();
                DataInputStream input = new DataInputStream(socket.getInputStream()); 
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte buffer[] = new byte[32];
                baos.write(buffer, 0 , input.read(buffer));

                Handshake recMsg = new Handshake(buffer);
                int callerPeerID = recMsg.peerID;
                peerSocketMap.put(callerPeerID, socket);

                Handshake msg= new Handshake(myPeerID);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream()); 
                out.write(msg.handshakeMsg); 
                
                logger.info("Peer " + inputPeerID + " is connected from Peer " + callerPeerID);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("File/Class not found");
        }
    }
}