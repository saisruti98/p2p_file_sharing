import java.net.Socket;
import java.net.ServerSocket;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

public class PeerProcess 
{
    public static int myPeerID;
    public static ServerSocket mySocket;
    public static Vector<PeerInfo> peerInfo = new Vector<PeerInfo>();
    public static Map<Integer, Socket> peerSocketMap = new HashMap<Integer, Socket>();
    public static Logger logger = Logger.getLogger("PeerLog");
    public static BitSet myBitMap;
    
    // Loaded from the config file
    public static int numPreferredNeighbors;
    public static int unchokingInterval;
    public static int optimisticUnchokingInterval;
    public static String fileName;
    public static int fileSize;
    public static int pieceSize;
    public static int totalPieces;

    
    public static void connectToPeers(String currPeerID, int currIndex)
	{
        Socket peerSocket; 
		
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
                            // Add the peerID and its socket to the map
                            peerSocketMap.put(peer.peerId, peerSocket);
                
                            // Should be sent to a log file along with timestamp
			                logger.info("Peer " + currPeerID + " makes a connection to Peer " + peer.peerId);
                        }else{
                            System.out.println("Wrong peerid");
                        }
                    }else{
                        System.out.println("Wrong header");
                    }
                }
            }
		}
		catch (IOException e) {
            e.printStackTrace();
    			System.err.println("Connection to peer failed. Peer initialised?");
		} 
	}

    public static void createPeerSocket(String peerConfigFile){
        int index = 0;
        String st;
        BufferedReader in;
        
        try {
            in = new BufferedReader(new FileReader(peerConfigFile));    
            while((st = in.readLine()) != null) {
                String[] value = st.split("\\s+");
                String currentPeerID = value[0];
                
                if (!currentPeerID.equals(Integer.toString(myPeerID))){
                    peerInfo.addElement(new PeerInfo(Integer.parseInt(value[0]), value[1], value[2]));
                }else{
                    String port = value[2];
                    // Create a socket at the port for TCP connections
                    mySocket = new ServerSocket(Integer.parseInt(port));
                    logger.info("Socket created\n");

                    // Set the bitmap
                    myBitMap.set(0, totalPieces, value[3].equals("1"));

                    // Establish connections with the other peers 
                    connectToPeers(currentPeerID, index);
                }

                index++;        
            }

            in.close();
        } catch (IOException e) {
            System.out.println("File not found");
        }
    }

    public static void listenForPeers(){
        try {
            while(true){
                Socket socket = mySocket.accept();
                DataInputStream input = new DataInputStream(socket.getInputStream()); 
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                Thread listenThread = new Thread(new Runnable(){
                    public void run()
                    {
                        try {
                            while(true){
                                Handshake recHandshake = null;

                                while (recHandshake == null){ 
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    byte buffer[] = new byte[32];
                                    baos.write(buffer, 0 , input.read(buffer));

                                    recHandshake = new Handshake(buffer);
                                    Handshake msgToSend= new Handshake(myPeerID); 
                                    out.write(msgToSend.handshakeMsg); 
                                    
                                    if(msgToSend.header.equals(recHandshake.header)){
                                        int callerPeerID = recHandshake.peerID;
                                        synchronized(this) {
                                            peerSocketMap.put(callerPeerID, socket);
                                        }
                                        logger.info("Peer " + myPeerID + " is connected from Peer " + callerPeerID);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Failed while peer connection");
                        }
                    }
                });
                listenThread.start();
            }
        } catch (IOException e) {
            System.out.println("Failed while peer connection");
        }
    }

    public static void loadConfig(String commonConfigFile){
		InputStream input;
        Properties prop = new Properties();

		try {
			input = new FileInputStream(commonConfigFile);
			prop.load(input);
			numPreferredNeighbors = Integer.parseInt(prop.getProperty("NumberOfPreferredNeighbors"));
			unchokingInterval = Integer.parseInt(prop.getProperty("UnchokingInterval"));
			optimisticUnchokingInterval = Integer.parseInt(prop.getProperty("OptimisticUnchokingInterval"));
			fileName = prop.getProperty("FileName");
			fileSize = Integer.parseInt(prop.getProperty("FileSize"));
			pieceSize = Integer.parseInt(prop.getProperty("PieceSize"));

			totalPieces = (int) java.lang.Math.ceil((double)fileSize/(double)pieceSize);     
            System.out.println("Total pieces " + totalPieces);   
			myBitMap = new BitSet(totalPieces);
		} catch (Exception e) {
			System.out.println("Error in reading Common.cfg");
		}
    }

    public static void main(String[] args) {
        String inputPeerID = args[0];
        myPeerID = Integer.parseInt(inputPeerID);
        
        String parentDir = new File (System.getProperty("user.dir")).getParent();

        try {
            // Add handler for logging current node's logs
            FileHandler logFileHandler = new FileHandler(parentDir + "/log_peer_" + inputPeerID + ".log");
            SimpleFormatter formatter = new SimpleFormatter();  
            
            logger.addHandler(logFileHandler);
            logFileHandler.setFormatter(formatter);

            String peerConfigFile = parentDir + "/PeerInfo.cfg";
            String commonConfigFile = parentDir + "/Common.cfg";
            
            loadConfig(commonConfigFile);
            createPeerSocket(peerConfigFile);
            listenForPeers();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("File/Class not found");
        }
    }
}