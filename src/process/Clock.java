package process;

/**
 * Implementation of Lamport's logical clock
 * @author shriroop
 *
 */
public class Clock {

	private int tick;
	private int d;
	
	public Clock() {
		this.tick = 0;
		this.d = 1;
	}
	
	public synchronized void setD(int d) {
		this.d = d;
	}
	
	public synchronized int event() {
		tick += d;
		return tick;
	}
	
	public synchronized int update(int tick) {
		this.tick += d;
		if(this.tick < tick + d)
			this.tick = tick + d;
		return this.tick;
	}
	
	public int peek() {
		return tick;
	}
}
