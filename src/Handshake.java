import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class Handshake implements Serializable {
    public byte[] handshakeMsg = new byte[32];
    public int peerID;
    public String header;

     public Handshake(Integer peerID){
        this.peerID = peerID;
        this.header = "P2PFILESHARINGPROJ";
        String msg = header + "          " + Integer.toString(peerID);
        System.out.println(msg);
        handshakeMsg = msg.getBytes();
     }

     public Handshake(byte[] msg){
        this.handshakeMsg = msg;
        String s = new String(msg, StandardCharsets.UTF_8);
        System.out.println(s);
        String peer = s.substring(28);
        String header = s.substring(0,18);
        this.peerID =  Integer.parseInt(peer);
        this.header = header;
     }
}
