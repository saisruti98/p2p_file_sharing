import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import java.io.FileOutputStream;
import java.io.FileReader;

public class PeerProcess implements Constants
{
    public static int myPeerID;
    public static ServerSocket mySocket;
    public static Vector<PeerInfo> peerInfo = new Vector<PeerInfo>();
    public static Map<Integer, Socket> peerSocketMap = new HashMap<Integer, Socket>();
    public static Logger logger = Logger.getLogger("PeerLog");
    public static BitSet myBitMap;
    public static Map<Integer, BitSet> peerBitMap = new HashMap<Integer, BitSet>();
    public static List<Integer> peerIsInterested = new ArrayList<>();
    public static List<Integer> neighborList = new ArrayList<>();
    public static List<Integer> prevneighborList = new ArrayList<>();
    
    // Loaded from the config file
    public static int numPreferredNeighbors;
    public static int unchokingInterval;
    public static int optimisticUnchokingInterval;
    public static String fileName;
    public static int fileSize;
    public static int pieceSize;
    public static int totalPieces;

    public static boolean isInterested(int peerID){
        BitSet peerBitSet = peerBitMap.get(peerID);

        for(int i = 0; i < totalPieces; i++){
            if(myBitMap.get(i) == false && peerBitSet.get(i) == true){
                return true;
            }
        }
        
        return false;
    }

    public static void connectToPeers(String currPeerID, int currIndex)
	{
        try{
            for(int i = 0; i< currIndex; i++){
                // Get the information of peers that are already active(the list of peers are started in order)
                PeerInfo peer = peerInfo.get(i);
                // Make a TCP connection to that peer
			    final Socket peerSocket = new Socket(peer.peerAddress,Integer.parseInt(peer.peerPort));
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
                            // Add the peerID and its socket to the map
                            peerSocketMap.put(peer.peerId, peerSocket);
                
                            // Should be sent to a log file along with timestamp
			                logger.info("Peer " + currPeerID + " makes a connection to Peer " + peer.peerId);
                            
                            Message bitFieldMsg = new Message(BITFIELD, myBitMap.toByteArray());
                            out.write(bitFieldMsg.message);

                             // Receive the BITFIELD
                            int byteCount = input.available();
                             // Wait until there's a new message in the queue
                            while(byteCount==0){
                                 byteCount = input.available();
                            }
            
                            byte bitFieldBuffer[] = new byte[byteCount];
                            input.read(bitFieldBuffer);
                                        
                            Message recBitFieldMsg = new Message(bitFieldBuffer);
                            peerBitMap.put(peer.peerId, recBitFieldMsg.getBitSet(totalPieces));

                            // check if interested in the peer
                            Message interestedMsg;
                            if((myBitMap.cardinality() != totalPieces) && (isInterested(peer.peerId))){
                                interestedMsg =  new Message(INTERESTED, null); 
                            }else{
                                interestedMsg =  new Message(NOT_INTERESTED, null);
                            }

                            out.write(interestedMsg.message);

                            // Receive interested/not interested message
                            byteCount = input.available();
                            // Wait until there's a new message in the queue
                            while(byteCount==0){
                                byteCount = input.available();
                            }

                            byte interestedBuffer[] = new byte[byteCount];
                            input.read(interestedBuffer);
                            
                            Message recInterestedMsg = new Message(interestedBuffer);
                            
                            // Add it the map to keep track of PeerID's that are interested
                            if(recInterestedMsg.msgType == INTERESTED){
                                peerIsInterested.add(peer.peerId);
                            }  
                            
                            Thread listenerThread = new Thread(new Runnable(){
                                public void run(){
                                    listenForever(peerSocket, input, out);
                                }
                            });
                            listenerThread.start();
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
                    Boolean hasFile = value[3].equals("1");
                    // Create a socket at the port for TCP connections
                    mySocket = new ServerSocket(Integer.parseInt(port));
                    logger.info("Socket created\n");
                    // Set the bitmap
                    myBitMap.set(0, totalPieces, hasFile);

                    if(hasFile){
                        String parentDir = new File (System.getProperty("user.dir")).getParent();
                        Path srcFile =Paths.get(parentDir + "/" + fileName);
                        Path dstFile = Paths.get(parentDir + "/" + myPeerID + "/" + fileName);
                        Files.copy(srcFile,dstFile);
                        fileBreaker();
                    }

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

    public static class setNeighbours implements Runnable{
        public void run(){
            // check the downLoad rates and make a list of top n peers from interested list
            System.out.println("Get neighbors");
            Random random = new Random();  
            int interestedCount;
            Collections.copy(prevneighborList, neighborList);

            synchronized(this){
                interestedCount = peerIsInterested.size();
            }

            // Note that the iteration is done with + 1 to incorporate the random neighbor for now
            if(neighborList.size() == 0){
                for(int i = 0; i < numPreferredNeighbors + 1; i++){
                    neighborList.add(peerIsInterested.get(random.nextInt(interestedCount)));
                }
            }else{
                for(int i = 0; i < numPreferredNeighbors + 1; i++){
                    neighborList.set(i, peerIsInterested.get(random.nextInt(interestedCount)));
                }
            }
            
            for(int i = 0; i < numPreferredNeighbors + 1; i++){
                System.out.println("Cur [" + i + "]" +neighborList.get(i));
                // System.out.println("Prev [" + i + "]" +prevneighborList.get(i));
                // neighborList.set(0, 999);
                // System.out.println("Curx [" + i + "]" +neighborList.get(i));
                // System.out.println("Prevx [" + i + "]" +prevneighborList.get(i));
            }

            synchronized(this){
                System.out.println("Lists equal ?" + prevneighborList.equals(neighborList));
            if(!prevneighborList.equals(neighborList)){
                sendchokemsg(neighborList,prevneighborList);
                sendunchokemsg();
            }
            }
        }
    };

    public static void startSharing(){
        Thread sharingThread = new Thread(new Runnable(){
            public void run(){
                    synchronized(this){
                        while(peerIsInterested.size() == 0){
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    ScheduledExecutorService scheduler
                        = Executors.newScheduledThreadPool(1);
                    // Scheduling the neighbourFetch
                    scheduler.scheduleAtFixedRate(new setNeighbours(), 0, unchokingInterval, TimeUnit.SECONDS);
            }
        });
        sharingThread.start();
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
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte buffer[] = new byte[HS_MSG_LEN];
                            baos.write(buffer, 0 , input.read(buffer));

                            Handshake recHandshake = new Handshake(buffer);
                            Handshake msgToSend= new Handshake(myPeerID); 
                            out.write(msgToSend.handshakeMsg); 
                            
                            if(msgToSend.header.equals(recHandshake.header)){
                                int callerPeerID = recHandshake.peerID;
                                
                                synchronized(this) {
                                    peerSocketMap.put(callerPeerID, socket);
                                }
                                
                                logger.info("Peer " + myPeerID + " is connected from Peer " + callerPeerID);
                                
                                // Receive the BITFIELD
                                int byteCount = input.available();
                                // Wait until there's a new message in the queue
                                while(byteCount==0){
                                    byteCount = input.available();
                                }

                                byte bitFieldBuffer[] = new byte[byteCount];
                                input.read(bitFieldBuffer);
                                
                                Message recBitFieldMsg = new Message(bitFieldBuffer);
                                
                                synchronized(this) {
                                    // Add it the map to keep track of PeerID bitmaps
                                    peerBitMap.put(callerPeerID, recBitFieldMsg.getBitSet(totalPieces));
                                }

                                // Send the bitfield 
                                Message msg = new Message(BITFIELD, myBitMap.toByteArray());
                                out.write(msg.message);

                                // Receive interested/not interested message
                                byteCount = input.available();
                                // Wait until there's a new message in the queue
                                while(byteCount==0){
                                    byteCount = input.available();
                                }

                                byte interestedBuffer[] = new byte[byteCount];
                                input.read(interestedBuffer);
                                
                                Message recInterestedMsg = new Message(interestedBuffer);
                                
                                synchronized(this) {
                                    // Add it the map to keep track of PeerID's that are interested
                                    if(recInterestedMsg.msgType == INTERESTED){
                                        System.out.println("client interested!!");
                                        peerIsInterested.add(callerPeerID);
                                    }else if(recInterestedMsg.msgType == NOT_INTERESTED){
                                        System.out.println("client not interested!!");
                                    }
                                }

                                Message interestedMsg;
                                if((myBitMap.cardinality() != totalPieces) && (isInterested(callerPeerID))){
                                    interestedMsg =  new Message(INTERESTED, null); 
                                }else{
                                    interestedMsg =  new Message(NOT_INTERESTED, null);
                                }

                                out.write(interestedMsg.message);

                                listenForever(socket,input, out);
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
    public static void sendchokemsg(List<Integer> neighborList, List<Integer> prevneighborList){
        for(int i = 0; i < numPreferredNeighbors + 1; i ++)
        {
            if(!neighborList.contains(prevneighborList.get(i))){
                int peerID = prevneighborList.get(i);
                try {
                    Socket socket = peerSocketMap.get(peerID);
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    
                    Thread sendchokThread = new Thread(new Runnable() {
                        public void run()
                        {
                                try {
                                    Message chokeMsg =  new Message(CHOKE, null);
                                    out.write(chokeMsg.message);
                                    logger.info("choke message sent to " + peerID);
                                } catch (IOException e) {
                                    System.out.println("Failed while sending choke message");
                                }
                        }
                    });
                    sendchokThread.start();
    
                } catch (IOException e) {
                    System.out.println("Failed while sending choke message");
                } 
            }
        }
    }
    public static void sendunchokemsg()
    {
        while(neighborList.size() == 0){
        }
        for(int i = 0; i < numPreferredNeighbors + 1; i ++)
        {
            int peerID = neighborList.get(i);
            try {
                Socket socket = peerSocketMap.get(peerID);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                Thread sendunchokThread = new Thread(new Runnable() {
                    public void run()
                    {
                            try {
                                Message unchokeMsg =  new Message(UNCHOKE, null);
                                out.write(unchokeMsg.message);
                                logger.info("Unchoke message sent to " + peerID);
                            } catch (IOException e) {
                                System.out.println("Failed while sending unchoke message");
                            }
                    }
                });
                sendunchokThread.start();

            } catch (IOException e) {
                System.out.println("Failed while sending unchoke message");
            } 

        }
    } 
    public static void listenForever(Socket socket, DataInputStream input, DataOutputStream out){
        // infinite listen!!!!
        while(true){
            try{
                int byteCount = input.available();
                // Wait until there's a new message in the queue
                while(byteCount==0){
                    byteCount = input.available();
                }
                byte newBuffer[] = new byte[byteCount];
                input.read(newBuffer);
                
                Message recMsg = new Message(newBuffer);
                switch (recMsg.msgType) {
                    case CHOKE:
                        
                        break;
                
                    case UNCHOKE:
                        logger.info("Received Unchoke Message");
                        break;
                    
                    case INTERESTED:
                        
                        break;
                    
                    case NOT_INTERESTED:
                        
                        break;
                
                    case HAVE:
                        
                        break;
                    
                    case REQUEST:
                        
                        break;
                    
                    case PIECE:
                        
                        break;
                
                    default:
                        break;
                }
            } catch(IOException e){
                System.out.println("Problem while listening");
            }
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
			myBitMap = new BitSet(totalPieces);
		} catch (Exception e) {
			System.out.println("Error in reading Common.cfg");
		}
    }

    public static void fileBreaker(){
        try {
            String parentDir = new File (System.getProperty("user.dir")).getParent();
            String folderPath = parentDir + "/" + myPeerID;
			FileInputStream fileInput = new FileInputStream(folderPath  + "/" + fileName);
			byte buffer[] = new byte[pieceSize];
			int index = 0;
            new File(folderPath + "/pieces").mkdirs();

			while (true) {
				int i = fileInput.read(buffer, 0, pieceSize);

				if (i == -1) 	break;

				String outputFile = folderPath + "/pieces/"  +  index;
				FileOutputStream fileOutput = new FileOutputStream(outputFile);
			    fileOutput.write(buffer, 0, i);
				fileOutput.flush();
				fileOutput.close();
				index++;
			}
        
            fileInput.close();
		} catch (Exception e) {
            e.printStackTrace();
			System.out.println("Couldn't Split the file");
		}
    }

    public static void main(String[] args) {
        String inputPeerID = args[0];
        myPeerID = Integer.parseInt(inputPeerID);
        
        String parentDir = new File (System.getProperty("user.dir")).getParent();
        new File(parentDir + "/" + myPeerID).mkdirs();

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
            startSharing();
            listenForPeers();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("File/Class not found");
        }
    }
}