import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class Handshake implements Serializable, Constants {
    public byte[] handshakeMsg = new byte[HS_MSG_LEN];
    public int peerID;
    public String header;

     public Handshake(Integer peerID){
        this.peerID = peerID;
        this.header = "P2PFILESHARINGPROJ";
        String msg = header + "          " + Integer.toString(peerID);
        handshakeMsg = msg.getBytes();
     }

     public Handshake(byte[] msg){
        this.handshakeMsg = msg;
        String s = new String(msg, StandardCharsets.UTF_8);
        String peer = s.substring(HS_HEADER_LEN + HS_ZEROBITS_LEN);
        String header = s.substring(0, HS_HEADER_LEN);
        this.peerID =  Integer.parseInt(peer);
        this.header = header;
     }
}
