package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;

import messages.ReadyMessage;
import messages.ReadyReplyMessage;
import utils.Commons;
import utils.Constants;

public class PZero {

	ServerSocket server;
	ArrayList<String> hosts;
	
	public PZero() throws IOException {
		Commons.log("Reading configurations", 0);
		String configFile = "resources/config.properties";
        Properties configs = Commons.loadProperties(configFile);
        //Constants.LISTENING_PORT = Integer.parseInt(configs.getProperty("process.port"));
        Constants.PROC_ZERO_PORT = Integer.parseInt(configs.getProperty("processzero.port"));
        Constants.NUM_PROC = Integer.parseInt(configs.getProperty("number.process"));
        Constants.PROC_ZERO_HOST = configs.getProperty("processzero.host");
        server = new ServerSocket(Constants.PROC_ZERO_PORT);
        hosts = new ArrayList<>(Constants.NUM_PROC);
	}
	
	public void start() throws IOException {
		Commons.log("Starting process", 0);
		int counter = 0;
		while (counter < Constants.NUM_PROC) {
            Socket socket = server.accept();
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ReadyMessage msg = ReadyMessage.getObjectFromString(br.readLine());
            Commons.log("Process started at " + msg.getHost(), 0);
            Commons.log("Message: ", 0);
            hosts.add(msg.getHost());
            counter++;
        }
		Commons.log("Sending replies", 0);
		ReadyReplyMessage rrm = new ReadyReplyMessage(hosts);
		for (String host: hosts) {
			Socket s = new Socket(host, Constants.LISTENING_PORT);
			Commons.writeToSocket(s, rrm.toString());
		}
		Commons.log("Exiting now", 0);
	}
}
