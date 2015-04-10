package leap;
import com.leapmotion.leap.*;

public class LeapFrameListener extends Listener{
	LeapManager manager;
	
	public LeapFrameListener(LeapManager manager) {
		this.manager = manager;
	}
	
	public void onConnect(Controller controller) {
		manager.toggleConnected(true);
		System.out.println("Connected");
	}
	
	public void onDisconnect(Controller controller) {
		manager.toggleConnected(false);
		System.out.println("Disconnected");
	}
	
	public void onFrame(Controller controller) {
		manager.processCurrentFrame();
	}
}
