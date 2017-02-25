package messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Reply message sent by nodes to indicate approval for other node to enter
 * critical section
 * @author shriroop
 *
 */
public class ReplyMessage extends Message {

	int pid;
	int timestamp;
	String name;
	
	public ReplyMessage(int pid, int timestamp) {
		this.pid = pid;
		this.timestamp = timestamp;
		this.name = "ReplyMessage";
	}
	
	public int getPid() {
		return pid;
	}
	
	public int getTimestamp() {
		return timestamp;
	}
	
	public static ReplyMessage getObjectFromString(String object) {
		Gson gson = new GsonBuilder().setLenient().create();
		return gson.fromJson(object, ReplyMessage.class);
	}
}
