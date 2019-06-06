package ipPhone_util;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * 
 * @author iblackholmes
 * @description this class is used to send data about client class
 */
public class ClientDataPacket implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int clientID;
	private InetAddress address;
	private boolean connectStatus;//true for beginning connection, false for ending connection
	
	public ClientDataPacket() {}
	public ClientDataPacket(InetSocketAddress ISA, boolean connectStatus) {
		this.clientID = ISA.getPort();
		this.address = ISA.getAddress();
		this.connectStatus = connectStatus;
	}
	
	public void setClientID(int clientID) {
		this.clientID = clientID;
	}
	public int getClientID() {
		return this.clientID;
	}
	
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	public InetAddress getAddress() {
		return this.address;
	}
	
	public void setConnectStatus(boolean connectStatus) {
		this.connectStatus = connectStatus;
	}
	public boolean getConnectStatus() {
		return this.connectStatus;
	}

}
