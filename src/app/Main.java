package app;

import java.io.IOException;
import java.util.Properties;

import process.PZero;
import process.Process;
import utils.Commons;
import utils.Constants;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("0")) {
				PZero pzero = new PZero();
				pzero.start();
			} else {
				System.out.println("USAGE: ./run.sh [0]");
			}
		} else {
			if (args.length == 0) {
				Commons.log("Started");
				Commons.log("Reading configurations");
				String configFile = "resources/config.properties";
				Properties configs = Commons.loadProperties(configFile);
				Constants.LISTENING_PORT = Integer.parseInt(configs.getProperty("process.port"));
				Constants.PROC_ZERO_PORT = Integer.parseInt(configs.getProperty("processzero.port"));
				Constants.PHASE_ONE_COUNT = Integer.parseInt(configs.getProperty("process.phaseone.count", "20"));
				Constants.PHASE_TWO_COUNT = Integer.parseInt(configs.getProperty("process.phasetwo.count", "20"));
				Constants.PROC_ZERO_HOST = configs.getProperty("processzero.host");
				Constants.TIME_UNIT = Integer.parseInt(configs.getProperty("process.timeunit"));
				Process process = new Process();
				process.startProcess();
			} else {
				System.out.println("USAGE: ./run.sh [0]");
			}
		}
	}

}
