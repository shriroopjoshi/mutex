package messages;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ReadyReplyMessage extends Message {

	ArrayList<String> hosts;
	
	public ReadyReplyMessage(ArrayList<String> hosts) {
		this.hosts = hosts;
	}
	
	public ArrayList<String> getHosts() {
		return hosts;
	}
	
	
	public static ReadyReplyMessage getObjectFromString(String object) {
		Gson gson = new GsonBuilder().setLenient().create();
		return gson.fromJson(object, ReadyReplyMessage.class);
	}
}
