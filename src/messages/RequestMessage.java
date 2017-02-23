package messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RequestMessage extends Message {

	int pid;
	int timestamp;
	String name;
	
	public RequestMessage(int pid, int timestamp) {
		this.pid = pid;
		this.timestamp = timestamp;
		this.name = "RequestMessage";
	}
	
	public int getPid() {
		return pid;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public static RequestMessage getObjectFromString(String object) {
		Gson gson = new GsonBuilder().setLenient().create();
		return gson.fromJson(object, RequestMessage.class);
	}
}
