import java.net.*;
import java.io.*;

public class PeerProcess 
{
    public static String[] prevHosts =new String[6];
    public static String[] prevPorts =new String[6];

    public static void connectToPeers(int numPeers)
	{
        Socket requestSocket; 
        System.out.println(numPeers);
		try{
            for(int i = 0; i< numPeers; i++){
			    //create a socket to connect to the server
                System.out.println(prevHosts[i] + prevPorts[i]);
			    requestSocket = new Socket(prevHosts[i], Integer.parseInt(prevPorts[i]));
			    System.out.println("Connected to" + prevHosts[i] + " port: " + prevPorts[i]);
            }
		}
		catch (IOException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		} 
	}
    public static void main(String[] args) {
        String inputPeerID = args[0];
        System.out.println(inputPeerID);
        // Start a socket.
        Integer i = 0;
        String st;
        try {
            BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
            while((st = in.readLine()) != null) {
                
                String[] tokens = st.split("\\s+");
                String currentPeerID = tokens[0];
                System.out.println(currentPeerID);
                
                if (!currentPeerID.equals(inputPeerID)){
                    prevPorts[i] = tokens[2];
                    prevHosts[i] = tokens[1];
                    System.out.println("iyguygu");
                }else{
                    ServerSocket listener = new ServerSocket(Integer.parseInt(tokens[2]));
                    System.out.println("Socket created\n");
                    connectToPeers(i);
                    break;
                }
                i++;        
            }
        }
        catch (IOException e)
        {
            System.out.println("File not found");
        }
    }
}