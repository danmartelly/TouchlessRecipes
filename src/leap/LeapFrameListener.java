package leap;
import com.leapmotion.leap.*;

public class LeapFrameListener extends Listener{
	LeapManager manager;
	
	public LeapFrameListener(LeapManager manager) {
		this.manager = manager;
	}
	
	public void onConnect(Controller controller) {
		manager.toggleConnected(true);
		System.out.println("Leap connected");
	}
	
	public void onDisconnect(Controller controller) {
		manager.toggleConnected(false);
		System.out.println("Leap disconnected");
	}
	
	public void onFrame(Controller controller) {
		manager.processCurrentFrame();
	}
}
