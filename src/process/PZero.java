package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import messages.ReadyMessage;
import messages.ReadyReplyMessage;
import utils.Commons;
import utils.Constants;

/**
 * Implementation of PROC-0.
 * It send ACK to all nodes when they are ready, and ACK to exit after completion.
 * @author shriroop
 *
 */
public class PZero {

	ServerSocket server;
	HashMap<String, Boolean> hosts;

	public PZero() throws IOException {
		Commons.log("Reading configurations", -1);
		String configFile = "resources/config.properties";
		Properties configs = Commons.loadProperties(configFile);
		Constants.PROC_ZERO_PORT = Integer.parseInt(configs.getProperty("processzero.port"));
		Constants.NUM_PROC = Integer.parseInt(configs.getProperty("number.process"));
		Constants.PROC_ZERO_HOST = configs.getProperty("processzero.host");
		server = new ServerSocket(Constants.PROC_ZERO_PORT);
		hosts = new HashMap<>(Constants.NUM_PROC);
	}

	public void start() throws IOException {
		/*
		 * Wait till all processes are ready
		 */
		Commons.log("Starting process", -1);
		int counter = 0;
		ArrayList<Socket> sockets = new ArrayList<>();
		while (counter < Constants.NUM_PROC) {
			Socket socket = server.accept();
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			ReadyMessage msg = ReadyMessage.getObjectFromString(br.readLine());
			Commons.log("Process started at " + msg.getHost(), -1);
			hosts.put(msg.getHost(), true);
			sockets.add(socket);
			counter++;
		}
		/*
		 * Send the IP address of all nodes to every node as ACK
		 */
		ArrayList<String> h = new ArrayList<>(hosts.keySet());
		ReadyReplyMessage rrm = new ReadyReplyMessage(h);
		for (Socket socket : sockets) {
			Commons.writeToSocket(socket, rrm.toString());
		}
		
		/*
		 * Wait till all process complete execution
		 */
		Commons.log("Waiting for exit messages", -1);
		counter = 0;
		sockets = new ArrayList<>();
		while (allCompleted(hosts)) {
			Socket socket = server.accept();
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			ReadyMessage msg = ReadyMessage.getObjectFromString(br.readLine());
			if(hosts.get(msg.getHost())) {
				Commons.log("Process terminated at " + msg.getHost(), -1);
				sockets.add(socket);
				hosts.put(msg.getHost(), false);
			}
			counter++;
		}
		counter = 0;
		/*
		 * Send ACK to all so that everyone can exit
		 */
		for (Socket socket : sockets) {
			Commons.writeToSocket(socket, "EXIT");
		}

		Commons.log("Exiting now", -1);
	}

	private boolean allCompleted(HashMap<String, Boolean> hosts) {
		// TODO Auto-generated method stub
		boolean incomplete = false;
		for(String host: hosts.keySet()) {
			incomplete = incomplete | hosts.get(host);
		}
		return incomplete;
	}

}
