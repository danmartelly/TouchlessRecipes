package leap;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import com.leapmotion.leap.*;
import com.sun.org.apache.bcel.internal.generic.INSTANCEOF;

public class LeapManager {
	public Controller controller;
	public LeapListener listener;
	protected boolean isConnected = false;
	
	// zoom variables
	protected boolean isZooming1Hand = false;
	protected Vector zoomGrabRef;
	protected float zoomGrabSensitivity = .01F;
	protected boolean isZooming2Hands = false;
	protected float zoomHandDistanceRef = -1F;
	public float zoomMultiplier = 1F;
	
	public LeapManager() {
		controller = new Controller();
		controller.enableGesture(Gesture.Type.TYPE_SWIPE);
		listener = new LeapListener(this);
		
		controller.addListener(listener);
	}
	
	public void toggleConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}
	
	public void processCurrentFrame() {
		Frame frame = controller.frame();
		processFrameForSwipe(frame);
		processFrameForZooming(frame);
	}
	
	protected void processFrameForSwipe(Frame frame) {
		GestureList gestures = frame.gestures();
		//System.out.println("gestures" + gestures.count());
		for (int i = 0; i < gestures.count(); i++) {
			Gesture gest = gestures.get(0);
			if (gest.type() == Gesture.Type.TYPE_SWIPE) {
				System.out.println("Swiping");
			}
		}
	}
	
	protected void processFrameForZooming(Frame frame) {
		HandList hands = frame.hands();
		// zooming using 1 hand
		if (hands.count() == 1) {
			isZooming2Hands = false;
			Hand hand = hands.get(0);
			if (hand.grabStrength() > .7) {
				if (!isZooming1Hand) {
					isZooming1Hand = true;
					zoomGrabRef = hand.palmPosition();
					zoomMultiplier = 1F;
				} else {
					zoomMultiplier = Math.abs(hand.palmPosition().getZ() - zoomGrabRef.getZ())*zoomGrabSensitivity;
				}
			} else {
				isZooming1Hand = false;
			}
			//System.out.println("grip: " + hand.grabStrength() + " pinch: " + hand.pinchStrength() + " zoom: " + zoomMultiplier);
		}
		// zooming using 2 hands
		else if (hands.count() == 2) {
			isZooming1Hand = false;
			Hand hand1 = hands.get(0);
			Hand hand2 = hands.get(1);
			float distanceBetweenHands = hand1.palmPosition().distanceTo(hand2.palmPosition());
			if (!isZooming2Hands) { // wasn't zooming before
				zoomHandDistanceRef = distanceBetweenHands;
			}
			else {
				zoomMultiplier = distanceBetweenHands/zoomHandDistanceRef;
				//System.out.println(zoomMultiplier);
			}
			isZooming2Hands = true;
		}
		else {
			isZooming1Hand = false;
			isZooming2Hands = false;
		}
	}
	
	public boolean close() {
		controller.removeListener(listener);
		return true;
	}
}
