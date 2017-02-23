package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;

import messages.ReplyMessage;
import messages.RequestMessage;
import utils.Commons;

public class Receiver extends Observable implements Runnable {

	ArrayList<Boolean> permission;
	private ServerSocket server;
	private int processNumber;
	private Clock clock;

	public Receiver(ArrayList<String> hosts, int processNumber, Clock clock, ServerSocket server) throws IOException {
		this.server = server;
		this.processNumber = processNumber;
		this.clock = clock;
		permission = new ArrayList<>(hosts.size());
		for (int i = 0; i < permission.size(); i++)
			permission.set(i, false);
	}

	@Override
	public void run() {
		Socket socket;
		Commons.log("/RECEIVER: Receiving messages", processNumber);
		while (true) {
			try {
				socket = server.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String rawMessage = br.readLine();
				Commons.log("/RECEIVER: message received: " + rawMessage, processNumber);
				if (rawMessage.contains("RequestMessage")) {
					RequestMessage msg = RequestMessage.getObjectFromString(rawMessage);
					int tick = clock.event();
					Commons.log("/RECEIVER: clock value: " + tick, processNumber);
					clock.update(msg.getTimestamp());
					if(!Commons.isProcessRequestingCS() && !Commons.isProcessExecutingCS()) {
						/*
						 * SEND REPLY
						 */
						setChanged();
						notifyObservers("SEND_REPLY_TO:" + msg.getPid());
					} else if(Commons.isProcessRequestingCS()) {
						if(Process.getRequestTimestamp() > clock.peek()) {
							/*
							 * SEND REPLY
							 */
							setChanged();
							notifyObservers("SEND_REPLY_TO:" + msg.getPid());
						}
					} else {
						/*
						 * DEFER REPLY
						 */
						setChanged();
						notifyObservers("DEFER_REPLY_TO:" + msg.getPid());
					}
					Commons.log("/RECEIVER: REQUESTMESSAGE from [PROCESS - " + msg.getPid() + "] at t =" + clock.peek(),
							processNumber);
				} else if (rawMessage.contains("ReplyMessage")) {
					ReplyMessage msg = ReplyMessage.getObjectFromString(rawMessage);
					Commons.log("/RECEIVER: REPLYMESSAGE from [PROCESS - " + msg.getPid() + "] at t" + clock.peek(),
							processNumber);
					permission.set(msg.getPid() - 1, true);
					clock.update(msg.getTimestamp());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
