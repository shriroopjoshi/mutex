package app;

import java.io.IOException;
import java.util.Properties;

import process.PZero;
import process.Process;
import utils.Commons;
import utils.Constants;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		if(args.length != 1) {
			System.err.println("USAGE");
			System.exit(1);
		}
		if(args[0].equalsIgnoreCase("pzero")) {
			PZero pzero = new PZero();
			pzero.start();
		} else {
			Commons.log("Started");
	        Commons.log("Reading configurations");
	        String configFile = "resources/config.properties";
	        Properties configs = Commons.loadProperties(configFile);
	        Constants.LISTENING_PORT = Integer.parseInt(configs.getProperty("process.port"));
	        Constants.PROC_ZERO_PORT = Integer.parseInt(configs.getProperty("processzero.port"));
	        Constants.PROC_ZERO_HOST = configs.getProperty("processzero.host");
	        Constants.TIME_UNIT = Integer.parseInt(configs.getProperty("process.timeunit"));
	        Process process = new Process();
	        process.start();
		}
	}

}
