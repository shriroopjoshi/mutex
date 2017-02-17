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

import messages.ReadyMessage;
import messages.ReadyReplyMessage;
import utils.Commons;
import utils.Constants;

public class Process {

	ServerSocket server;	
	ArrayList<String> hosts;
	
	public Process() throws IOException {
		server = new ServerSocket(Constants.LISTENING_PORT);
	}
	
    @SuppressWarnings("resource")
	public void start() throws UnknownHostException, IOException {
        String address = InetAddress.getLocalHost().getHostAddress();
        Commons.log("Starting process at " + address);
        Socket s = new Socket(Constants.PROC_ZERO_HOST, Constants.PROC_ZERO_PORT);
        ReadyMessage rm = new ReadyMessage(address);
        PrintWriter pw = new PrintWriter(s.getOutputStream());
        System.out.println(rm + "\n");
        Commons.log("Sending message to PROC-0 at " + Constants.PROC_ZERO_HOST);
        pw.write(rm + "\n");
        ServerSocket tempServer = new ServerSocket(Constants.LISTENING_PORT);
        Commons.log("Waiting for reply from PROC-0");
        Socket socket = tempServer.accept();
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ReadyReplyMessage reply = ReadyReplyMessage.getObjectFromString(br.readLine());
        hosts = reply.getHosts();
        Commons.log("Reply: " + reply);
        tempServer.close();
        //new Receiver(server).start();
    }
}
