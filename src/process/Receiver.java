package process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import messages.Message;
import messages.ReplyMessage;
import messages.RequestMessage;

public class Receiver extends Process implements Runnable {

	Receiver() throws IOException {

	}

	@Override
	public void run() {
		Socket socket;
		while (true) {
			try {
				socket = server.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				Message message = ReplyMessage.getObjectFromString(br.readLine());
				if (message instanceof ReplyMessage) {
					synchronized (queue) {
						queue.add((ReplyMessage) message);
					}
				}
				if (message instanceof RequestMessage) {
					// TODO: DO something here
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
