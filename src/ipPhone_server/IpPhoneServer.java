package ipPhone_server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

import ipPhone_util.*;

public class IpPhoneServer {
	private int port_TCP = 6584;// port_TCP for dialing
	private ServerSocket SS;// TCP connection for dialing
	private Socket socket;// TCP connection for dialing
	
	private byte[] buf;// buffer for sending data
	private int SIZE = 50 * 1024;// buffer size for UDP
	private int port_UDP = 7135;// port_UDP for dialing
	private DatagramPacket dpk;// UDP packet for transport data
	private DatagramSocket dsk;// UDP socket for receive packet only
	
	private ArrayList<ClientConnection> clients = new ArrayList<ClientConnection>();// clients who is in connecting
	private ArrayList<Message> messages = new ArrayList<Message>();// messages received from/ send to clients
	
	public IpPhoneServer() {
		this.initServer();
		this.startServer();
	}
	
	/** initialize the server **/
	private void initServer() {
		try {
			buf = new byte[SIZE];
			
			// Create a DatgramPacket to receive data
			dpk = new DatagramPacket(buf, buf.length);
			
			// Create a socket to listen at port_UDP
			dsk = new DatagramSocket(port_UDP, InetAddress.getByName("localhost"));
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/* start server, listen at port_TCP for client's connection
	 * start data broadcast thread
	 * start data receive thread
	 * loop to receive client's connection 
	 **/
	private void startServer() {
		Scanner sc = new Scanner(System.in);
		// listen on specified port
		try {
			System.out.println("please input server InetAddress:");
			String address_string = sc.nextLine();
			SS = new ServerSocket(port_TCP, 10, InetAddress.getByName (address_string));
			sc.close();
			
//			System.out.println("server started:" + SS.getInetAddress());
			System.out.println("server started:" + SS.getLocalSocketAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Server error " + e + "(port_TCP " + this.port_TCP + ")");
		}
		
		System.out.println("server is ready for client to connect");
		
		// create a BroadcastThread and start it
		new BroadcastThread().start();
		
		// create a data receiver thread and start it
		new DataReceiverThread().start();
		
		// accept all incoming connection
		while(true) {
			try {
				socket = SS.accept();
				
				// receive client information
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				ClientDataPacket CDP = (ClientDataPacket) in.readObject();
				
				//create a ClientConnection thread
				ClientConnection CC = new ClientConnection(socket, CDP.getAddress(), CDP.getClientID());
				CC.start();
				
				// client connect to server successfully, add it to listen queue
				this.addToClients(CC);
				System.out.println("new client(" + CC.getClientID() + ") " + CC.getInetAddress() + ":" + CC.getPort() + " on port_TCP " + port_TCP);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/** add the new connection to the list of connections **/
    private void addToClients(ClientConnection cc) {
        try {
            clients.add(cc); 
        } catch (Throwable t) {
            //mutex error, try again
            Utils.sleep(1);
            addToClients(cc);
        }
    }

    /** add a message to the messages queue **/
	private void addToMessagesQueue(Message m) {
		try {
			this.messages.add(m);
        } catch (Throwable t) {
            //mutex error, try again
            Utils.sleep(1);
            addToMessagesQueue(m);
        }
	}
	
	/** run the server **/
	public static void main(String[] args) {
		new IpPhoneServer();
	}
	
	/**
	 * receive data from clients
	 */
	private class DataReceiverThread extends Thread {
		public DataReceiverThread() {}
		
		@Override
		public void run() {
//			int count = 0;
			while(true) {
				try {
					// Receive the data in byte buffer.
    				dsk.receive(dpk);
    				
    				// change the type of receiving data into message class
    				Message m = (Message)DataTransfer.byteArrayToSerializableObject(dpk.getData());
    				if(m == null) continue;
    				m.setTimestamp(System.nanoTime() / 1000000L);

    				// test
//    				count++;
//    				System.out.println("server receive " + count + " data(" + dpk.getData().toString() + ") from " + m.getClientID());
    				
    				// add message to massages queue
    				addToMessagesQueue(m);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
     * broadcasts messages to each ClientConnection, and removes dead ones
     */
    private class BroadcastThread extends Thread {
    	public BroadcastThread() {}
    	
    	@Override
    	public void run() {
    		System.out.println("begin to broadcast data");
    		
    		while(true) {
    			// delete all dead client connections
    			ArrayList<ClientConnection> toRemove = new ArrayList<ClientConnection>();
	            for (ClientConnection cc : clients) {
	            	//connection is dead, need to be removed
	                if (!cc.isAlive()) {
	                   System.out.println("dead connection closed: " + cc.getInetAddress() + ":" + cc.getPort() + " on port " + port_TCP);
	                   toRemove.add(cc);
	                }
	            }
	            clients.removeAll(toRemove); //delete all dead connections
    			
    			// send data to other clients
    			if(messages.isEmpty()) {// no messages to send
	            	Utils.sleep(10); //avoid busy wait
                    continue;
	            }else {// we got some messages to send
	            	Message m = messages.get(0);
	            	for (ClientConnection cc : clients) { //broadcast the message
                        if (cc.getClientID() == m.getClientID())// need to judge the source of data, for not sending data back
                        	continue;
                        cc.addToQueue(m);
                    }
	            	messages.remove(m); //remove it from the broadcast queue
	            }
    		}
    	}
    }

}
