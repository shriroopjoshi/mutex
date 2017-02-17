package utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Commons {

	public static void log(String message) {
		System.out.println("[PROCESS]: " + message);
	}
	
	public static void log(String message, int pid) {
		System.out.println("[PROCESS - " + pid + "]: " + message);
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
}
