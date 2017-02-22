package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Random;

import messages.ReadyMessage;
import messages.ReadyReplyMessage;
import messages.ReplyMessage;
import messages.RequestMessage;
import utils.Commons;
import utils.Constants;

public class Process {

	ServerSocket server;
	ArrayList<String> hosts;
	int processNumber;
	Clock clock;
	PriorityQueue<ReplyMessage> queue;

	public Process() throws IOException {
		// server = new ServerSocket(Constants.LISTENING_PORT);
	}

	public void start() throws UnknownHostException, IOException, InterruptedException {
		String address = InetAddress.getLocalHost().getHostAddress();
		Commons.log("Starting process at " + address);
		Socket s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
		ReadyMessage rm = new ReadyMessage(address);
		PrintWriter pw = new PrintWriter(s.getOutputStream());
		Commons.log(rm.toString());
		Commons.log("Sending message to PROC-0 at " + Constants.PROC_ZERO_HOST + ":" + Constants.PROC_ZERO_PORT);
		Commons.writeToSocket(s, rm.toString());
		pw.write(rm.toString());
		ServerSocket tempServer = new ServerSocket(Constants.LISTENING_PORT);
		Commons.log("Waiting for reply from PROC-0");
		Socket socket = tempServer.accept();
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		ReadyReplyMessage reply = ReadyReplyMessage.getObjectFromString(br.readLine());
		hosts = reply.getHosts();
		Commons.log("Reply: " + reply);
		tempServer.close();
		Commons.log("I am process #" + processNumber, processNumber);

		/*
		 * Initialization
		 */
		this.processNumber = hosts.indexOf(address) + 1;
		this.server = new ServerSocket(Constants.LISTENING_PORT);
		clock = new Clock();
		new Receiver().start();
		queue = new PriorityQueue<>();
		phaseOne();
	}

	private void phaseOne() throws InterruptedException, UnknownHostException, IOException {
		for (int i = 0; i < 20; i++) {
			// random.nextInt(max - min + 1) + min
			Thread.sleep((new Random().nextInt(6) + 5) * Constants.TIME_UNIT);
			requestCriticalSection();
			int counter = 0;
			while (counter < hosts.size()) {
				synchronized (queue) {
					for (ReplyMessage rm : queue) {
						counter++;
					}
				}
				Thread.sleep(Constants.TIME_UNIT);
			}
			emptyQueue();
			criticalSection();
		}

	}

	private void criticalSection() throws InterruptedException {
		// TODO Auto-generated method stub
		Commons.log("Entering CRITICAL SECTION\n\t\t" + new Date().toString(), processNumber);
		Thread.sleep(3 * Constants.TIME_UNIT);

	}

	private void emptyQueue() {
		// TODO Auto-generated method stub

	}

	private void requestCriticalSection() throws UnknownHostException, IOException {
		Socket s;
		RequestMessage rm;
		for (String host : hosts) {
			s = new Socket(host, Constants.LISTENING_PORT);
			rm = new RequestMessage(processNumber, clock.event());
			Commons.writeToSocket(s, rm.toString());
		}
	}

	void addToQueue() {

	}
}
