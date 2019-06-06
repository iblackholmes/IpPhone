package ipPhone_client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import ipPhone_util.*;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * reads data from microphone and sends it to the server
 *
 * @author dosse, iblackholmes
 */
public class MicThread extends Thread {

    public static double amplification = 1.0;
    private TargetDataLine mic;
    private IpPhoneClient client;

    public MicThread() throws LineUnavailableException {
        //open microphone line, an exception is thrown in case of error
        AudioFormat af = SoundPacket.defaultFormat;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
        mic = (TargetDataLine) (AudioSystem.getLine(info));
        mic.open(af);
        mic.start();
    }
    public MicThread(IpPhoneClient client) throws LineUnavailableException {
        this.client = client;
    	
    	//open microphone line, an exception is thrown in case of error
        AudioFormat af = SoundPacket.defaultFormat;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
        mic = (TargetDataLine) (AudioSystem.getLine(info));
        mic.open(af);
        mic.start();
    }

    @Override
    public void run() {
    	System.out.println("client(" + client.getClientID() + ") microphone start");
        while(true) {
            if (mic.available() >= SoundPacket.defaultDataLenght) { //we got enough data to send            	
                byte[] buff = new byte[SoundPacket.defaultDataLenght];
                while (mic.available() >= SoundPacket.defaultDataLenght) { //flush old data from mic to reduce lag, and read most recent data
                    mic.read(buff, 0, buff.length); //read from microphone
                }
                try {
                    //this part is used to decide whether to send or not the packet. if volume is too low, an empty packet will be sent and the remote client will play some discomfort noise
                    long tot = 0;
                    for (int i = 0; i < buff.length; i++) {
                        buff[i] *= amplification;
                        tot += Math.abs(buff[i]);
                    }
                    tot *= 2.5;
                    tot /= buff.length;
                    
                    //create and send packet
                    Message message = null;
                    if (tot == 0) {//send empty packet
                        message = new Message(client.getClientID(), new SoundPacket(null));
                    } else { //send data
                        //compress the sound packet with GZIP
                        ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
                        GZIPOutputStream GOS = new GZIPOutputStream(BAOS);
                        GOS.write(buff);
                        GOS.flush();
                        GOS.close();
                        BAOS.flush();
                        BAOS.close();
                        message = new Message(client.getClientID(), new SoundPacket(BAOS.toByteArray()));  //create message for server, will generate chId and timestamp from this computer's IP and this socket's port 
                    }
                    // add message to queue
                    client.addToMssageQueue(message);
//                    System.out.println("successfully gen data");
                } catch (IOException ex) { //connection error
                	System.out.println("microphone get sound data error");
                	break;
                }
            } else {
                Utils.sleep(10); //sleep to avoid busy wait
            }
        }
    }

    public static void main(String[] args) {
    	try {
			new MicThread().start();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}