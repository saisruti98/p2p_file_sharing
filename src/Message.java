import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class Message implements Serializable {
    public byte[] message;
    public int msgLen;
    public int msgType;
    public byte[] msgPayLoad;

    Message(int msgLen, int msgType, byte[] msgPayLoad){
        this.msgLen= msgLen;
        this.msgType= msgType;
        int payLoadLen = msgPayLoad.length;
        this.msgPayLoad=new byte[payLoadLen];
        this.msgPayLoad= msgPayLoad;

        message = new byte[5 * 8 + payLoadLen];
        byte[] y = ByteBuffer.allocate(4).putInt(msgLen).array();
        
        for(int i=0; i<4; i++){
            message[i] = y[i];
        }
        
        message[4] = ByteBuffer.allocate(1).putInt(msgType).array()[0];

        for(int i = 0;i < payLoadLen; i++){
            message[5 + i] = msgPayLoad[i];
        }
    }
}
