package leap;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import com.leapmotion.leap.*;
import com.sun.org.apache.bcel.internal.generic.INSTANCEOF;

/**
 * This class listens to the leap hardware to determine when to process the frame data.
 * It processes the data, changes its state and fires LEAP_EVENTs accordingly.
 * @author martelly
 *
 */
public class LeapManager {
	public enum LEAP_STATE {
		NONE,
		IS_ZOOMING,
		IS_TURNING_NEXT,
		IS_TURNING_PREV
	};
	
	public enum LEAP_EVENT {
		START_ZOOM,
		STOP_ZOOM,
		START_NEXT_PAGE,
		END_NEXT_PAGE,
		START_PREV_PAGE,
		END_PREV_PAGE
	};
	
	// zoom variables
	protected boolean isZooming1Hand = false;
	protected Vector zoomGrabRef;
	protected float zoomGrabSensitivity = .01F;
	protected boolean isZooming2Hands = false;
	protected float zoomHandDistanceRef = -1F;
	protected float zoomMultiplier = 1F;
	
	// turn page variables
	
	//other
	protected LEAP_STATE currentState = LEAP_STATE.NONE;
	protected boolean isConnected = false;
	public Controller controller;
	public LeapFrameListener leapListener;
	protected HashMap<LEAP_EVENT, List<LeapListener>> LeapListeners;
	
	public LeapManager() {
		controller = new Controller();
		controller.enableGesture(Gesture.Type.TYPE_SWIPE);
		leapListener = new LeapFrameListener(this);
		
		LeapListeners = new HashMap<LEAP_EVENT, List<LeapListener>>();
		
		controller.addListener(leapListener);
	}
	
	public LEAP_STATE getCurrentState() {
		return currentState;
	}
	
	public float getCurrentZoomMultiplier() {
		return zoomMultiplier;
	}
	
	public void addListener(LEAP_EVENT event, LeapListener listener) {
		if (LeapListeners.containsKey(event)) {
			LeapListeners.get(event).add(listener);
		} else {
			List<LeapListener> newList = new ArrayList<LeapListener>();
			newList.add(listener);
			LeapListeners.put(event, newList);
		}
	}
	
	public void removeListener(LEAP_EVENT event, LeapListener listener) {
		List<LeapListener> list = LeapListeners.get(event);
		if (list != null) {
			list.remove(listener);
		}
	}
	
	protected void fireEvent(LEAP_EVENT event) {
		List<LeapListener> list = LeapListeners.get(event);
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				LeapListener listener = list.get(0);
				listener.eventFired(event);
			}
		}
	}
	
	public Point cursorPosition() {
		return new Point(0,0);
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
					fireEvent(LEAP_EVENT.START_ZOOM);
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
		controller.removeListener(leapListener);
		return true;
	}
}
