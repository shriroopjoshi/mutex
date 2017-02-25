package messages;

import com.google.gson.GsonBuilder;

/**
 * Abstract class which serves as a base for all messages
 * @author shriroop
 *
 */
public abstract class Message {

	@Override
	public String toString() {
		return new GsonBuilder().create().toJson(this);
	}
}
