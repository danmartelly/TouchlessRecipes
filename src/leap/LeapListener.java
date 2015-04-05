package leap;
import com.leapmotion.leap.*;

public class LeapListener extends Listener{
	public void onConnect(Controller controller) {
		System.out.println("Connected");
	}
	
	public void onFrame(Controller controller) {
		System.out.println("Frame available");
	}
}
