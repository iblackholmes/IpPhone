package ipPhone_util;

public class Utils {
    public static void sleep(int ms){
        try {Thread.sleep(ms);} catch (InterruptedException ex) {}
    }
}
