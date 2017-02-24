package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;

import messages.Message;
import messages.ReplyMessage;
import messages.RequestMessage;

public class Receiver extends Observable implements Runnable {

	private ServerSocket server;

	public Receiver(ServerSocket server) throws IOException {
		this.server = server;
	}

	@Override
	public void run() {
		Socket socket;
		while (true) {
			try {
				socket = server.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String rawMessage = br.readLine();
				if (rawMessage.contains("RequestMessage")) {
					RequestMessage msg = RequestMessage.getObjectFromString(rawMessage);
					notify(msg);
				} else if (rawMessage.contains("ReplyMessage")) {
					ReplyMessage msg = ReplyMessage.getObjectFromString(rawMessage);
					notify(msg);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void notify(Message message) {
		setChanged();
		notifyObservers(message);
	}

}
