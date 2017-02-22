package process;

public class Clock {

	private int tick;
	private int d;
	
	public Clock() {
		this.tick = 0;
		this.d = 1;
	}
	
	public void setD(int d) {
		this.d = d;
	}
	
	public int event() {
		tick += d;
		return tick;
	}
}
