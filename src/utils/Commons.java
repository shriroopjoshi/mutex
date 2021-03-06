package utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.Random;

/**
 * Contains all commonly used methods
 * @author shriroop
 *
 */
public class Commons {

	public static void log(String message) {
		System.out.println("[PROCESS]: " + message);
	}
	
	public static void log(String message, int pid) {
		System.out.println("[PROCESS - " + (pid + 1) + "]: " + message);
	}
	
    public static Properties loadProperties(String filename) {
        InputStream configs;
        Properties prop = new Properties();
        try {
            configs = new FileInputStream(filename);
            prop.load(configs);
        } catch (FileNotFoundException ex) {
            System.err.println("CONFIG file not found\n" + ex);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Unable to read CONFIG file");
            System.exit(1);
        }
        return prop;
    }
    
    public static void writeToSocket(Socket s, String message) throws IOException {
    	OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream(), "UTF-8");
        osw.append(message).append("\n");
        osw.flush();
    }
	
	public static int wait(int min, int max) {
		int t = new Random().nextInt(max - min + 1) + min;
		busywait(t);
		return t;
	}
	
	public static void wait(int timeunits) {
		busywait(timeunits);
	}
	
	private static void busywait(int t) {
		long now = System.currentTimeMillis();
		long waitTill = now + t * Constants.TIME_UNIT;
		while(now < waitTill) {
			now = System.currentTimeMillis();
		}
	}
	
	public static void printReplySet(boolean[] set) {
		System.out.print("ReplySet: [ ");
		for(boolean b: set)
			System.out.print(b + " ");
		System.out.println("]");
	}
}
