package touchlesscooking1;

import leap.*;
import leap.LeapManager.LEAP_EVENT;

public class LeapHandler implements LeapListener {

	TouchlessCooking1 scene;
	
	public LeapHandler(TouchlessCooking1 scene) {
		this.scene = scene;
	}
	
	@Override
	public void eventFired(LEAP_EVENT event) {
		scene.updateLeapEvent(event);
		System.out.println("registered event: " + event);
	}

}
