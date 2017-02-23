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
	ArrayList<Boolean> deferReplies;

	static int requestTimestamp;

	public Process() throws IOException {
		reqCS = null;
		cs = null;
		requestTimestamp = -1;
	}

	public void startProcess() throws UnknownHostException, IOException, InterruptedException {
		String address = InetAddress.getLocalHost().getHostAddress();
		Commons.log("Starting process at " + address);
		Socket s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
		ReadyMessage rm = new ReadyMessage(address);
		PrintWriter pw = new PrintWriter(s.getOutputStream());
		Commons.log(rm.toString());
		Commons.log("Sending message to PROC-0 at " + Constants.PROC_ZERO_HOST + ":" + Constants.PROC_ZERO_PORT);
		Commons.writeToSocket(s, rm.toString());
		pw.write(rm.toString());
		Commons.log("Waiting for reply from PROC-0");
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		ReadyReplyMessage reply = ReadyReplyMessage.getObjectFromString(br.readLine());
		hosts = reply.getHosts();
		Commons.log("Reply: " + reply);

		/*
		 * Initialization
		 */
		this.processNumber = hosts.indexOf(address) + 1;
		deferReplies = new ArrayList<>(hosts.size());
		for(int i = 0; i < deferReplies.size(); i++)
			deferReplies.set(i, false);
		Commons.log("I am process #" + processNumber, processNumber);
		clock = new Clock();
		this.server = new ServerSocket(Constants.LISTENING_PORT);
		Receiver r = new Receiver(hosts, processNumber, clock, server);
		r.addObserver(Process.this);
		Thread t = new Thread(r);
		Commons.log("Started thread", processNumber);
		t.setName("ReceiverThread");
		Commons.log("Starting background thread to receive messages", processNumber);
		t.start();
		phaseOne();
	}

	private void phaseOne() throws InterruptedException, UnknownHostException, IOException {
		Commons.log("Starting [PHASE - 1]", processNumber);
		for (int i = 0; i < 20; i++) {
			// random.nextInt(max - min + 1) + min
			int t = new Random().nextInt(6) + 5;
			Commons.log("Sleeping for " + t + " units", processNumber);
			Thread.sleep(t * Constants.TIME_UNIT);
			Commons.log("Requesting critical section", processNumber);
			requestCriticalSection();
			//criticalSection();
		}
	}

	private void criticalSection() throws InterruptedException, IOException {
		Commons.requestCS(reqCS);
		requestTimestamp = -1;
		cs = Commons.executeCS(null);
		Commons.log("Entering CRITICAL SECTION\n\t\t" + new Date().toString(), processNumber);
		Thread.sleep(3 * Constants.TIME_UNIT);
		Commons.executeCS(cs);
	}

	private void requestCriticalSection() throws UnknownHostException, IOException {
		Socket s;
		RequestMessage rm;
		int tick = clock.event();
		requestTimestamp = tick;
		Commons.log("Sending REQUESTMESSAGE at t = " + tick, processNumber);
		reqCS = Commons.requestCS(null);
		for (String host : hosts) {
			s = new Socket(host, Constants.LISTENING_PORT);
			rm = new RequestMessage(processNumber, tick);
			Commons.writeToSocket(s, rm.toString());
			s.close();
		}
	}
	
	public static int getRequestTimestamp() {
		return requestTimestamp;
	}

	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		if(arg.toString().contains("SEND_REPLY")) {
			/*
			 * SEND_REPLY
			 */
			int index = Integer.parseInt(arg.toString().split(":")[1]) - 1;
			ReplyMessage rm = new ReplyMessage(processNumber, clock.peek());
			try {
				Socket s = new Socket(hosts.get(index), Constants.LISTENING_PORT);
				Commons.writeToSocket(s, rm.toString());
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else if(arg.toString().contains("DEFER_REPLY")) {
			int index = Integer.parseInt(arg.toString().split(":")[1]) - 1;
			deferReplies.set(index, true);
			try {
				criticalSection();
			} catch (InterruptedException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
