package ipPhone_util;

import java.io.Serializable;
import javax.sound.sampled.AudioFormat;

/**
 * some sound
 * @author dosse, iblackholms
 */
public class SoundPacket implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static AudioFormat defaultFormat=new AudioFormat(11025f, 8, 1, true, true); //11.025khz, 8bit, mono, signed, big endian (changes nothing in 8 bit) ~8kb/s
    public static int defaultDataLenght=1200; //send 1200 samples/packet by default
    private byte[] data; //actual data. if null, comfort noise will be played
    private int clientID;// client ID
    
    public SoundPacket(byte[] data, int clientID) {
        this.data = data;
        this.clientID = clientID;
    }
    public SoundPacket(byte[] data) {
    	this.data = data;
    }

    public byte[] getData() {
        return data;
    }
    
    public int getClientID() {
    	return this.clientID;
    }
}
