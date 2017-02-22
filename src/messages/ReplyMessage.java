package messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ReplyMessage extends Message {

	int pid;
	
	public ReplyMessage(int pid) {
		this.pid = pid;
	}
	
	public int getPid() {
		return pid;
	}
	
	public static ReplyMessage getObjectFromString(String object) {
		Gson gson = new GsonBuilder().setLenient().create();
		return gson.fromJson(object, ReplyMessage.class);
	}
}
