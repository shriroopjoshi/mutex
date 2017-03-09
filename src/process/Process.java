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
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import messages.ReadyMessage;
import messages.ReadyReplyMessage;
import messages.ReplyMessage;
import messages.RequestMessage;
import utils.Commons;
import utils.Constants;

/**
 * An implementation of a Process
 * 
 * @author shriroop
 *
 */
public class Process implements Observer {

	ServerSocket server;
	HashMap<Integer, ProcessInfo> processes;
	int processNumber;
	Receiver receiver;
	Phases phase;
	Clock clock;
	ArrayList<Integer> deferSet;
	volatile boolean[] replySet;
	volatile boolean[] optimizationSet; // set for optimization

	volatile boolean isRequestingCS, isExecutingCS;
	int requestTimestamp;
	volatile int count, phaseCount;
	volatile boolean flag;

	// variables to collect data
	volatile int messagesSent, messagesReceived;
	long time;

	public Process() throws IOException {
		clock = new Clock();
		isRequestingCS = false;
		isExecutingCS = false;
		requestTimestamp = -1;
		messagesSent = 0;
		messagesReceived = 0;
		time = 0;
	}

	private void init() throws UnknownHostException, IOException {
		// Communicate with PZERO to know about all other processes.
		String address = InetAddress.getLocalHost().getHostAddress();
		Socket s = null;
		try {
			s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
		} catch (IOException ex) {
			Commons.log("PROCESS - 0 not accessible");
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

		replySet = new boolean[processes.keySet().size() + 1];
		optimizationSet = new boolean[processes.keySet().size() + 1];
		Commons.log("I am process #" + (processNumber + 1), processNumber);
	}

	public void startProcess() throws UnknownHostException, IOException, InterruptedException {
		init();

		// Start a receiver on a new thead, so that it can always listen to new
		// messages
		this.server = new ServerSocket(Constants.LISTENING_PORT);
		receiver = new Receiver(server);
		receiver.addObserver(Process.this);
		Thread t = new Thread(receiver);
		t.setName("ReceiverThread");
		t.start();

		// Start PHASE-I
		phaseOne();
	}

	private void phaseOne() throws UnknownHostException, IOException {
		Commons.log("---- Starting PHASE-I ----", processNumber);
		count = 0;
		phaseCount = Constants.PHASE_ONE_COUNT;
		phase = Phases.PHASE_ONE;
		requestCriticalSection();
	}

	private void phaseTwo() throws UnknownHostException, IOException {
		Commons.log("---- Starting PHASE-II ----", processNumber);
		count = 0;
		phaseCount = Constants.PHASE_TWO_COUNT;
		phase = Phases.PHASE_TWO;
		requestCriticalSection();
	}

	private void requestCriticalSection() throws UnknownHostException, IOException {
		Commons.log("Waiting...", processNumber);
		// busy-waiting before requesting critical section
		if (phase == Phases.PHASE_ONE) {
			int t = Commons.wait(5, 10);
			Commons.log("Waited for " + t + " units", processNumber);
		} else if (phase == Phases.PHASE_TWO) {
			int t;
			if (processNumber % 2 != 0)
				t = Commons.wait(45, 50);
			else
				t = Commons.wait(5, 10);
			Commons.log("Waited for " + t + " units", processNumber);
		}
		this.isRequestingCS = true;
		requestTimestamp = -1;

		// Initialize reply_set
		// This is helpful for Roucairol and Carvalho optimization
		replySet[processNumber] = true;
		optimizationSet[processNumber] = true;
		flag = true;
		int tick = clock.event();
		requestTimestamp = tick;
		time = new Date().getTime();
		RequestMessage rm = new RequestMessage(processNumber, tick);
		//sendToAll(rm.toString());
		for(int i = 0; i < optimizationSet.length; i++)
			if(!optimizationSet[i]) {
				Commons.log("Sending REQUEST to Process - " + (i + 1) + " at t = " + tick, processNumber);
				sendMessage(processes.get(i).pid, rm.toString());
			}
		tryCriticalSection();
	}

	/**
	 * An implementation of critical section
	 */
	private void criticalSection() {
		this.isRequestingCS = false;
		this.isExecutingCS = true;
		time = new Date().getTime() - time;
		Commons.log("Time required to enter critical section = " + time + " millisec", processNumber);
		Commons.log("\t\t\t\t* * *\n\t\t\tEntering CRITICAL SECTION at t = " + clock.peek() + "\n\t\t\tPhysical time: "
				+ new Date().toString() + "\n\t\t\t\t\t* * *", processNumber);
		Commons.wait(3);
		this.isExecutingCS = false;
		count++;
	}

	private void sendMessage(int pid, String message) throws IOException {
		Socket s = new Socket(processes.get(pid).host, Constants.LISTENING_PORT);
		Commons.writeToSocket(s, message);
		messagesSent += 1;
		s.close();
	}

	private void sendToAll(String message) throws IOException {
		for (int pid : processes.keySet()) {
			sendMessage(pid, message);
		}
	}

	/**
	 * Check for replies from all. If yes, enter Critical Section
	 */
	public void tryCriticalSection() {
		boolean allReplies = true;
		for (boolean b : replySet) {
			allReplies &= b;
		}
		if (allReplies) {
			if (flag) {
				String p = this.phase == Phases.PHASE_ONE ? "PHASE_ONE" : "PHASE_TWO";
				//System.out.println("\n Phase = " + p + " | count = " + this.count + "\n");
				Thread t = new Thread(new ProcessThread());
				t.setName("ProcessThread");
				t.start();
				flag = !flag;
			}
		}
	}

	/**
	 * Receiving point for the incoming messages This function implements the
	 * RECEIVE(Pi) rule of the algorithm
	 */
	@Override
	public synchronized void update(Observable o, Object arg) {
		int tick;
		try {
			messagesReceived += 1;
			if (arg instanceof ReplyMessage) {
				// Received a REPLY
				ReplyMessage msg = (ReplyMessage) arg;
				tick = clock.update(msg.getTimestamp());
				Commons.log("Received REPLY from PROCESS - " + (msg.getPid() + 1) + " at t = " + tick, processNumber);
				replySet[msg.getPid()] = true;
				optimizationSet[msg.getPid()] = true;
				tryCriticalSection();
			} else if (arg instanceof RequestMessage) {
				// Received a REQUEST
				RequestMessage msg = (RequestMessage) arg;
				tick = clock.update(msg.getTimestamp());
				Commons.log("Received REQUEST from PROCESS - " + (msg.getPid() + 1) + " at t = " + tick, processNumber);
				if (!this.isRequestingCS) {
					// Send a reply, as I am not requesting CS
					// System.out.println("[NOT REQ CS]");
					tick = clock.event();
					ReplyMessage rm = new ReplyMessage(processNumber, tick);
					Commons.log("Sending REPLY to PROCESS - " + (msg.getPid() + 1) + " at t = " + tick, processNumber);
					sendMessage(msg.getPid(), rm.toString());
					optimizationSet[msg.getPid()] = false;
				} else if (!this.isExecutingCS) {
					if (requestTimestamp > msg.getTimestamp()) {
						// I am requesting CS, send reply only if T(m) > T
						tick = clock.event();
						ReplyMessage rm = new ReplyMessage(processNumber, tick);
						Commons.log("Sending REPLY to PROCESS - " + (msg.getPid() + 1) + " at t = " + tick,
								processNumber);
						sendMessage(msg.getPid(), rm.toString());
						optimizationSet[msg.getPid()] = false;
					} else {
						// If both processes requested CS at same time, give
						// preference to process with lower PID
						if (requestTimestamp == msg.getTimestamp()) {
							if (processNumber < msg.getPid()) {
								addToDeferReplySet(msg.getPid());
							} else {
								tick = clock.event();
								ReplyMessage rm = new ReplyMessage(processNumber, tick);
								Commons.log("Sending REPLY to PROCESS - " + (msg.getPid() + 1) + " at t = " + tick,
										processNumber);
								sendMessage(msg.getPid(), rm.toString());
								optimizationSet[msg.getPid()] = false;
							}
						} else {
							// defer reply
							addToDeferReplySet(msg.getPid());
						}
					}
				} else {
					// defer reply. I am executing CS
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
		//System.out.println(Thread.currentThread().getName());
		Socket s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
		ReadyMessage rm = new ReadyMessage(InetAddress.getLocalHost().getHostAddress());
		Commons.writeToSocket(s, rm.toString());
		BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
		br.readLine();
		// Print statistics before exiting
		Commons.log("Messages sent: " + messagesSent / 2, processNumber);
		Commons.log("Messages received: " + messagesReceived / 2, processNumber);
		System.exit(0);
	}

	/**
	 * A thread which takes control after recceiving all replies. This is to
	 * help receive-thread function smoothly
	 * 
	 * @author shriroop
	 *
	 */
	class ProcessThread implements Runnable {
		@Override
		public void run() {
			criticalSection();
			try {
				sendDeferedReplies();
				if (count >= phaseCount) {
					if (phase == Phases.PHASE_ONE) {
						phaseTwo();
					} else {
						exit();
					}
				} else {
					requestCriticalSection();
				}
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
				Commons.log("Sending REPLY to PROCESS - " + (rm.getPid() + 1) + " at t = " + tick, processNumber);
				sendMessage(pid, rm.toString());
				optimizationSet[pid] = false;
			}
			deferSet = null;
		}
	}
}

enum Phases {
	PHASE_ONE, PHASE_TWO
}
