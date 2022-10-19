public class PeerInfo {
	public Integer peerId;
	public String peerAddress;
	public String peerPort;
	
	public PeerInfo(Integer pId, String pAddress, String pPort) {
		peerId = pId;
		peerAddress = pAddress;
		peerPort = pPort;
	}
}