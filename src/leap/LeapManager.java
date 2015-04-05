package leap;

import com.leapmotion.leap.*;

public class LeapManager {
	public Controller controller;
	public LeapListener listener;
	
	public LeapManager() {
		controller = new Controller();
		listener = new LeapListener();
		
		controller.addListener(listener);
	}
	
	public boolean close() {
		controller.removeListener(listener);
		return true;
	}
}
