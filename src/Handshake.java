import javax.naming.ldap.HasControls;

public class Handshake {
    public byte[] handshakeMsg = new byte[32];

     public Handshake(Integer peerID){
        String msg = "P2PFILESHARINGPROJ" + "          " + Integer.toString(peerID);
        System.out.println(msg);
        handshakeMsg = msg.getBytes();
        for(int i=0;i<32;i++)
        {
            System.out.print(handshakeMsg[i] + " ");
        }
        System.out.println("");
     }
}
