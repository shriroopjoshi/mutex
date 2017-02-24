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
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import messages.ReadyMessage;
import messages.ReadyReplyMessage;
import messages.ReplyMessage;
import messages.RequestMessage;
import utils.Commons;
import utils.Constants;

public class Process implements Observer {

	ServerSocket server;
	HashMap<Integer, ProcessInfo> processes;
	int processNumber;
	Receiver receiver;
	Phases phase;
	Clock clock;
	ArrayList<Integer> deferSet;
	boolean[] replySet;

	boolean isRequestingCS, isExecutingCS;
	int requestTimestamp;
	int count, phaseCount;

	public Process() throws IOException {
		clock = new Clock();
		isRequestingCS = false;
		isExecutingCS = false;
		requestTimestamp = -1;
	}

	private void init() throws UnknownHostException, IOException {
		// Communicate with PZERO to know about all other processes.
		String address = InetAddress.getLocalHost().getHostAddress();
		Socket s = null;
		try {
			s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
		} catch (IOException ex) {
			System.out.println("PROCESS - 0 not accessible");
			System.exit(1);
		}
		ReadyMessage rm = new ReadyMessage(address);
		PrintWriter pw = new PrintWriter(s.getOutputStream());
		Commons.writeToSocket(s, rm.toString());
		pw.write(rm.toString());
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		ReadyReplyMessage reply = ReadyReplyMessage.getObjectFromString(br.readLine());

		// Store all information about other processes
		ArrayList<String> hosts = reply.getHosts();
		processNumber = hosts.indexOf(address);
		processes = new HashMap<>();
		for (String host : hosts) {
			int pid = hosts.indexOf(host);
			if (pid != processNumber) {
				ProcessInfo p = new ProcessInfo(pid, host);
				processes.put(pid, p);
			}
		}
		Commons.log("I am process #" + (processNumber + 1), processNumber);
	}

	public void startProcess() throws UnknownHostException, IOException, InterruptedException {
		init();

		this.server = new ServerSocket(Constants.LISTENING_PORT);
		receiver = new Receiver(server);
		receiver.addObserver(Process.this);
		Thread t = new Thread(receiver);
		t.setName("ReceiverThread");
		t.start();

		Commons.log("Starting PHASE-I", processNumber);
		phaseOne();
	}

	private void phaseOne() throws InterruptedException, UnknownHostException, IOException {
		count = 0;
		phaseCount = Constants.PHASE_ONE_COUNT;
		phase = Phases.PHASE_ONE;
		requestCriticalSection();
	}
	

	private void requestCriticalSection() throws UnknownHostException, IOException {
		// Check if the given number of critical sections are completed
		if (count >= phaseCount) {
			if (phase == Phases.PHASE_TWO)
				exit();
			else {
				exit();
				
				//phaseTwo();
			}
		}
		// busy-waiting before requesting critical section
		if (phase == Phases.PHASE_ONE) {
			int t = Commons.wait(5, 10);
			Commons.log("Waited for " + t + " units", processNumber);
		} else if (phase == Phases.PHASE_TWO) {
			int t = Commons.wait(45, 50);
			Commons.log("Waited for " + t + " units", processNumber);
		}
		this.isRequestingCS = true;
		requestTimestamp = -1;
		replySet = new boolean[processes.keySet().size() + 1];
		replySet[processNumber] = true;

		int tick = clock.event();
		requestTimestamp = tick;
		Commons.log("Sending REQUEST at t = " + tick, processNumber);
		RequestMessage rm = new RequestMessage(processNumber, tick);
		sendToAll(rm.toString());
	}

	private void criticalSection() {
		this.isRequestingCS = false;
		this.isExecutingCS = true;
		Commons.log("\t\t\t\t* * *\n\t\t\tEntering CRITICAL SECTION at t = " + clock.peek() + "\n\t\t\t\t\t* * *", processNumber);
		Commons.wait(3);
		this.isExecutingCS = false;
		count++;
	}

	private void sendMessage(int pid, String message) throws IOException {
		Socket s = new Socket(processes.get(pid).host, Constants.LISTENING_PORT);
		Commons.writeToSocket(s, message);
		s.close();
	}

	private void sendToAll(String message) throws IOException {
		for (int pid : processes.keySet()) {
			sendMessage(pid, message);
		}
	}

	public void tryCriticalSection() {
		boolean allReplies = true;
		for (boolean b : replySet) {
			allReplies &= b;
		}
		if (allReplies) {
			Thread t = new Thread(new ProcessThread());
			t.setName("ProcessThread");
			t.start();
		}
	}

	@Override
	public synchronized void update(Observable o, Object arg) {
		int tick;
		try {
			if (arg instanceof ReplyMessage) {
				ReplyMessage msg = (ReplyMessage) arg;
				tick = clock.update(msg.getTimestamp());
				Commons.log("Received REPLY from PROCESS - " + msg.getPid() + " at t = " + tick, processNumber);
				replySet[msg.getPid()] = true;
				tryCriticalSection();
			} else if (arg instanceof RequestMessage) {
				RequestMessage msg = (RequestMessage) arg;
				tick = clock.update(msg.getTimestamp());
				Commons.log("Received REQUEST from PROCESS - " + msg.getPid() + " at t = " + tick, processNumber);
				if (!this.isRequestingCS) {
					ReplyMessage rm = new ReplyMessage(processNumber, tick);
					Commons.log("Sending REPLY to PROCESS - " + rm.getPid() + " at t = " + tick, processNumber);
					sendMessage(msg.getPid(), rm.toString());
				} else if (!this.isExecutingCS) {
					if (requestTimestamp > tick) {
						ReplyMessage rm = new ReplyMessage(processNumber, tick);
						Commons.log("Sending REPLY to PROCESS - " + rm.getPid() + " at t = " + tick,
								processNumber);
						sendMessage(msg.getPid(), rm.toString());
					} else {
						addToDeferReplySet(msg.getPid());
					}
				} else {
					addToDeferReplySet(msg.getPid());
				}
			} else {
				Commons.log("[FATAL] Known reply received", processNumber);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void addToDeferReplySet(int pid) {
		if (deferSet == null) {
			deferSet = new ArrayList<>();
		}
		deferSet.add(pid);
	}

	private void exit() throws UnknownHostException, IOException {
		Commons.log("Exiting now", processNumber);
		Socket s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
		ReadyMessage rm = new ReadyMessage(InetAddress.getLocalHost().getHostAddress());
		Commons.writeToSocket(s, rm.toString());
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		br.readLine();
		System.exit(0);
	}

	class ProcessThread implements Runnable {

		@Override
		public void run() {
			criticalSection();
			try {
				sendDeferedReplies();
				requestCriticalSection();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	public void sendDeferedReplies() throws IOException {
		if (deferSet != null) {
			int tick = clock.event();
			for (Integer pid : deferSet) {
				ReplyMessage rm = new ReplyMessage(processNumber, tick);
				Commons.log("Sending REPLY to PROCESS - " + rm.getPid() + " at t = " + tick, processNumber);
				sendMessage(pid, rm.toString());
			}
			deferSet = null;
		}
	}
}

enum Phases {
	PHASE_ONE, PHASE_TWO
}
