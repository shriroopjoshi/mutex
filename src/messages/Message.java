package messages;

import com.google.gson.GsonBuilder;

public abstract class Message {

	@Override
	public String toString() {
		return new GsonBuilder().create().toJson(this);
	}
}
