package ipPhone_client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import ipPhone_util.*;

/**
 * this thread plays the sound coming from one of the users that are connected
 * on the server. each user has its own AudioChannel.
 *
 * @author dosse, iblackholmes
 */
public class AudioChannel extends Thread {

    private ArrayList<Message> queue = new ArrayList<Message>(); //queue of messages to be played
    private int lastSoundPacketLen = SoundPacket.defaultDataLenght;
    private long lastPacketTime = System.nanoTime();

    public boolean canKill() { //returns true if it's been a long time since last received packet
        if (System.nanoTime() - lastPacketTime > 5000000000L) {
            return true; //5 seconds with no data
        } else {
            return false;
        }
    }

    public void closeAndKill() {
        if (speaker != null) {
            speaker.close();
        }
        this.interrupt();
    }

    public AudioChannel() {}

    public void addToQueue(Message m) { //adds a message to the play queue
        queue.add(m);
    }
    private SourceDataLine speaker = null; //speaker

    @Override
    public void run() {
        try {
            //open channel to sound card
            AudioFormat af = SoundPacket.defaultFormat;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(af);
            speaker.start();
            //sound card ready
            for (;;) { //this infinite cycle checks for new packets to be played in the queue, and plays them. to avoid busy wait, a sleep(10) is executed at the beginning of each iteration
                if (queue.isEmpty()) { //nothing to play, wait
                    Utils.sleep(10);
                    continue;
                } else {//we got something to play
                    lastPacketTime = System.nanoTime();
                    Message in = queue.get(0);
                    queue.remove(in);
                    if (in.getData() instanceof SoundPacket) { //it's a sound packet, send it to sound card
                        SoundPacket m = (SoundPacket) (in.getData());
                        if (m.getData() == null) {//sender skipped a packet, play comfort noise
                            byte[] noise = new byte[lastSoundPacketLen];
                            for (int i = 0; i < noise.length; i++) {
                                noise[i] = (byte) ((Math.random() * 3) - 1);
                            }
                            speaker.write(noise, 0, noise.length);
                        } else {
                            //decompress data
                            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(m.getData()));
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            for (;;) {
                                int b = gis.read();
                                if (b == -1) {
                                    break;
                                } else {
                                    baos.write((byte) b);
                                }
                            }
                            //play de-compressed data
                            byte[] toPlay=baos.toByteArray();
                            speaker.write(toPlay, 0, toPlay.length);
                            lastSoundPacketLen = m.getData().length;
                        }
                    } else { //not a sound packet, trash
                        continue; //invalid message
                    }
                }
            }
        } catch (Exception e) { //sound card error or connection error, stop
            System.out.println("receiverThread error: " + e.toString());
            if (speaker != null) {
                speaker.close();
            }
            this.interrupt();
        }
    }
}
