package process;

/**
 * A POJO for keeping process information
 * @author shriroop
 *
 */
public class ProcessInfo {

	public ProcessInfo(int pid, String host) {
		this.pid = pid;
		this.host = host;
	}
	
	int pid;
	String host;
}
