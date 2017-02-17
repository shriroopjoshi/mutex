package messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ReadyMessage extends Message {

	public String host;

	public ReadyMessage(String host) {
		super();
		this.host = host;
	}

	public String getHost() {
		return host;
	}
	
	public static ReadyMessage getObjectFromString(String object) {
		Gson gson = new GsonBuilder().setLenient().create();
		return gson.fromJson(object, ReadyMessage.class);
	}
	
}
