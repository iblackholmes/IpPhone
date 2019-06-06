package ipPhone_server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import ipPhone_util.ClientDataPacket;
import ipPhone_util.DataTransfer;
import ipPhone_util.Message;
import ipPhone_util.Utils;


/**
 * this thread manages a connection with a client. it does a lot of stuff: 
 * -receive messages from the server thread 
 * -add messages to a send queue
 * -send messages from queue to the client (or throw them away if too old)
 *
 * @author dosse
 * @author iblackholmes
 */
public class ClientConnection extends Thread {
	//unique id of this client, generated in the constructor
    private int clientID; 
    
    // UDP connection to send response to client
    private DatagramPacket dpk;
    private DatagramSocket dsk;
    
    // data buffer for sending
    private byte[] buf;
    private int bufSize = 50 * 1024;
    
    // socket connecting between client and server
    private Socket socket;
    
    // Internet message of client
    private InetAddress address;
    
    //queue of messages to be sent to the client
    private ArrayList<Message> toSend = new ArrayList<Message>();
	
	public ClientConnection(Socket s, InetAddress address, int clientID) {
		this.socket= s;
		this.address = address;
		this.clientID = clientID;
        
        this.buf = new byte[this.bufSize];
        try {
			dpk = new DatagramPacket(buf, buf.length, new InetSocketAddress(this.address, this.clientID));
			dsk = new DatagramSocket();
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("initialze client connection fail");
		}
	}
	
	// get methods
	// returns this client'socket ip address
	public InetAddress getInetAddress() {
        return this.address;
    }

	// returns this client's UDP port
    public int getPort() {
        return this.clientID;
    }

    // return this client's unique id
    public int getClientID() {
        return clientID;
    }
    
    // add a message to send to the client
    public void addToQueue(Message m) {
        try {
            toSend.add(m);
        } catch (Throwable t) {
            //mutex error, ignore because the server must be as fast as possible
        }
    }
    
    @Override
    public void run() {
    	new Thread() {
			@Override
			public void run() {
				try {
					System.out.println("ready for cc closed");
					// receive client information
					ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
					ClientDataPacket CDP = (ClientDataPacket) in.readObject();
					
					System.out.println("receive data in cc");
					if(CDP.getConnectStatus()==false)
						socket.close();
				}catch(Exception e) {
					try {
						socket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}.start();
        while(true) {
        	if(socket.isClosed())
        		break;
            try {
            	//we got something to send to the client
                if (!toSend.isEmpty()) {
                    Message message = toSend.get(0);
                    
                    //judge whether the message is too old or of an unknown type
                    if (message.getTimestamp() + message.getTtl() < System.nanoTime() / 1000000L) {
                        System.out.println("dropping packet from " + message.getClientID() + " to " + clientID);
                        continue;
                    }
                    
                    //send the message
                    dpk.setData(DataTransfer.serializableObjectToByteArray(message));
                    dsk.send(dpk);
                    
                    //and remove it from the queue
                    toSend.remove(message);
                } else {
                    Utils.sleep(10); //avoid busy wait
                }
            }catch (Exception e) {
                //mutex error, try again
            	e.printStackTrace();
                continue;
            }
        }

    }
}
