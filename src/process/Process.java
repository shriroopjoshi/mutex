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
import java.util.Collections;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import messages.ReadyMessage;
import messages.ReadyReplyMessage;
import messages.ReplyMessage;
import messages.RequestMessage;
import utils.Commons;
import utils.Constants;

public class Process implements Observer {

	ServerSocket server;
	ArrayList<String> hosts;
	int processNumber;
	Clock clock;
	ServerSocket reqCS, cs;
	boolean[] deferReplies;
	boolean[] replies;

	static int requestTimestamp;
	int count = 0, phaseCount = 20;

	public Process() throws IOException {
		reqCS = null;
		cs = null;
		requestTimestamp = -1;
	}

	public void startProcess() throws UnknownHostException, IOException, InterruptedException {
		String address = InetAddress.getLocalHost().getHostAddress();
		// Commons.log("Starting process at " + address);
		Socket s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
		ReadyMessage rm = new ReadyMessage(address);
		PrintWriter pw = new PrintWriter(s.getOutputStream());
		// Commons.log(rm.toString());
		// Commons.log("Sending message to PROC-0 at " +
		// Constants.PROC_ZERO_HOST + ":" + Constants.PROC_ZERO_PORT);
		Commons.writeToSocket(s, rm.toString());
		pw.write(rm.toString());
		// Commons.log("Waiting for reply from PROC-0");
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		ReadyReplyMessage reply = ReadyReplyMessage.getObjectFromString(br.readLine());
		hosts = reply.getHosts();
		// Commons.log("Reply: " + reply);

		/*
		 * Initialization
		 */
		this.processNumber = hosts.indexOf(address) + 1;
		int ss = hosts.size();
		deferReplies = new boolean[ss];
		for (int i = 0; i < deferReplies.length; i++)
			deferReplies[i] = false;
		replies = new boolean[ss];
		for (int i = 0; i < replies.length; i++)
			replies[i] = false;
		Commons.log("Replies size = " + replies.length, processNumber);
		Commons.log("I am process #" + processNumber, processNumber);
		clock = new Clock();
		this.server = new ServerSocket(Constants.LISTENING_PORT);
		Receiver r = new Receiver(hosts, processNumber, clock, server);
		r.addObserver(Process.this);
		Thread t = new Thread(r);
		Commons.log("Started thread", processNumber);
		t.setName("ReceiverThread");
		// Commons.log("Starting background thread to receive messages",
		// processNumber);
		t.start();
		sleep(5, 10);
		phaseOne();
	}

	private void sleep(int min, int max) throws InterruptedException {
		int t = new Random().nextInt(max - min + 1) + 5;
		Commons.log("Sleeping for " + t + " units", processNumber);
		Thread.sleep(t * Constants.TIME_UNIT);
	}

	private void phaseOne() throws InterruptedException, UnknownHostException, IOException {
		// Commons.log("Starting [PHASE - 1]", processNumber);
		// for (int i = 0; i < 20; i++) {
		// random.nextInt(max - min + 1) + min

		Commons.log("Requesting critical section", processNumber);
		requestCriticalSection();
		// criticalSection();
		// }
	}

	private void criticalSection() throws InterruptedException, IOException {
		Commons.requestCS(reqCS);
		requestTimestamp = -1;
		{
			cs = Commons.executeCS(null);
			Commons.log("Entering CRITICAL SECTION\n\t\t" + new Date().toString(), processNumber);
			Thread.sleep(3 * Constants.TIME_UNIT);
			Commons.executeCS(cs);
		}
		sleep(5, 10);
		count++;
	}

	private void requestCriticalSection() throws UnknownHostException, IOException {
		if (count >= phaseCount) {
			// phaseTwo();
			exit();
		}
		Socket s;
		RequestMessage rm;
		int tick = clock.event();
		requestTimestamp = tick;
		Commons.log("Sending REQUESTMESSAGE at t = " + tick, processNumber);
		reqCS = Commons.requestCS(null);
		for (int i = 0; i < replies.length; i++)
			replies[i] = false;
		replies[processNumber - 1] = true;
		for (String host : hosts) {
			s = new Socket(host, Constants.LISTENING_PORT);
			rm = new RequestMessage(processNumber, tick);
			Commons.writeToSocket(s, rm.toString());
			s.close();
		}
	}

	private void exit() throws UnknownHostException, IOException {
		// TODO Auto-generated method stub
		Socket s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
		ReadyMessage rm = new ReadyMessage(InetAddress.getLocalHost().getHostAddress());
		Commons.writeToSocket(s, rm.toString());
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		System.exit(0);
	}

	public static int getRequestTimestamp() {
		return requestTimestamp;
	}

	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		Commons.log("MESSAGE = " + arg.toString(), processNumber);
		if (arg.toString().contains("SEND_REPLY")) {
			/*
			 * SEND_REPLY
			 */
			int index = Integer.parseInt(arg.toString().split(":")[1]) - 1;
			Commons.log("Sending reply message to PROCESS-" + index, processNumber);
			ReplyMessage rm = new ReplyMessage(processNumber, clock.peek());
			try {
				Socket s = new Socket(hosts.get(index), Constants.LISTENING_PORT);
				Commons.writeToSocket(s, rm.toString());
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (arg.toString().contains("DEFER_REPLY")) {
			int index = Integer.parseInt(arg.toString().split(":")[1]) - 1;
			deferReplies[index] = true;
			replies[index] = true;
			boolean flag = true;
			for (boolean b : replies) {
				flag &= b;
				Commons.log("FLAG = " + flag, processNumber);
			}
			if (flag) {
				try {
					criticalSection();
					sendDefferedReplies();
				} catch (InterruptedException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (arg.toString().contains("REPLY_FROM")) {
			int index = Integer.parseInt(arg.toString().split(":")[1]) - 1;
			replies[index] = true;
			boolean flag = true;
			for (boolean b : replies) {
				flag &= b;
				Commons.log("FLAG = " + flag, processNumber);
			}
			if (flag) {
				try {
					criticalSection();
					sendDefferedReplies();
				} catch (InterruptedException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void sendDefferedReplies() throws UnknownHostException, IOException {
		// TODO Auto-generated method stub
		Socket s;
		for (int i = 0; i < deferReplies.length; i++) {
			if (deferReplies[i]) {
				s = new Socket(hosts.get(i), Constants.LISTENING_PORT);
				ReplyMessage rm = new ReplyMessage(processNumber, clock.peek());
				Commons.writeToSocket(s, rm.toString());
				s.close();
			}
		}
	}
}
