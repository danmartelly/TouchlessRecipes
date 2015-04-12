package leap;

import com.sun.javafx.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import com.leapmotion.leap.*;
import com.sun.javafx.scene.traversal.Direction;
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
	protected float zoomGrabSensitivity = .005F;
	protected boolean isZooming2Hands = false;
	protected float zoomHandDistanceRef = -1F;
	protected float zoomMultiplier = 1F;
	
	// turn page variables
	protected boolean isSwiping = false;
	protected int currentGestID = -1;
	
	//other
	protected Point2D cursorPosition;
	protected LEAP_STATE currentState = LEAP_STATE.NONE;
	protected boolean isConnected = false;
	public Controller controller;
	public LeapFrameListener leapListener;
	protected List<LeapListener> leapListeners;
	
	public LeapManager() {
		controller = new Controller();
		controller.enableGesture(Gesture.Type.TYPE_SWIPE);
		leapListener = new LeapFrameListener(this);
		
		leapListeners = new ArrayList<LeapListener>();
		
		controller.addListener(leapListener);
	}
	
	public LEAP_STATE getCurrentState() {
		return currentState;
	}
	
	public float getCurrentZoomMultiplier() {
		return zoomMultiplier;
	}
	
	public void addListener(LeapListener listener) {
		leapListeners.add(listener);
	}
	
	public void removeListener(LEAP_EVENT event, LeapListener listener) {
		leapListeners.remove(listener);
	}
	
	protected void fireEvent(LEAP_EVENT event) {
		for (int i = 0; i < leapListeners.size(); i++) {
			leapListeners.get(i).eventFired(event);
		}
	}
	
	public Point2D getCursorPosition() {
		return cursorPosition;
	}
	
	
	public void toggleConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}
	
	public void processCurrentFrame() {
		Frame frame = controller.frame();
		processFrameForCursor(frame);
		processFrameForSwipe(frame);
		processFrameForZooming(frame);
	}
	
	protected void processFrameForCursor(Frame frame) {
		HandList hands = frame.hands();
		if (hands.count() == 1) {
			Vector pos;
			Hand hand = hands.get(0);
			FingerList fingers = hand.fingers();
			if (fingers.get(1).isExtended()) { // pointer finger extended
				Finger fing = fingers.get(1);
				pos = fing.tipPosition();
			} else if (hand.pointables().extended().count() > 0) {
				Pointable p = hand.pointables().extended().get(0);
				pos = p.tipPosition();
			} else {
				pos = hand.palmPosition();
			}
			cursorPosition = new Point2D(pos.getX(), pos.getY());
		} else {
			cursorPosition = null;
		}
	}
	
	protected void processFrameForSwipe(Frame frame) {
		if (getCurrentState() == LEAP_STATE.NONE) {
			GestureList gestures = frame.gestures();
			for (int i = 0; i < gestures.count(); i++) {
				Gesture gest = gestures.get(i);
				if (gest.type() == Gesture.Type.TYPE_SWIPE) {
					isSwiping = true;
					currentGestID = gest.id();
					SwipeGesture swipeGest = new SwipeGesture(gest);
					Vector dir = swipeGest.direction();
					if (dir.getX() > .5) {
						currentState = LEAP_STATE.IS_TURNING_NEXT;
						fireEvent(LEAP_EVENT.START_NEXT_PAGE);
					} else if (dir.getX() < -.5) {
						currentState = LEAP_STATE.IS_TURNING_PREV;
						fireEvent(LEAP_EVENT.START_PREV_PAGE);
					}
				}
			}
		} else if (isSwiping) {
			boolean foundCurrentGesture = false;
			GestureList gestures = frame.gestures();
			for (int i = 0; i < gestures.count(); i++) {
				Gesture gest = gestures.get(i);
				if (gest.id() == currentGestID) {
					foundCurrentGesture = true;
				}
			}
			if (!foundCurrentGesture) {
				isSwiping = false;
				LEAP_STATE prevState = getCurrentState();
				currentState = LEAP_STATE.NONE;
				if (prevState == LEAP_STATE.IS_TURNING_NEXT) {
					fireEvent(LEAP_EVENT.END_NEXT_PAGE);
				} else {
					fireEvent(LEAP_EVENT.END_PREV_PAGE);
				}
			}
		}
	}
	
	protected void processFrameForZooming(Frame frame) {
		HandList hands = frame.hands();
		if (getCurrentState() == LEAP_STATE.IS_ZOOMING) {
			if (isZooming1Hand) {
				// zooming using 1 hand
				if (hands.count() == 1) {
					Hand hand = hands.get(0);
					zoomMultiplier = (float) Math.max(0.3, 1. - (zoomGrabRef.getZ() - hand.palmPosition().getZ())*zoomGrabSensitivity);
					if (hand.grabStrength() < .7) {
						isZooming1Hand = false;
						currentState = LEAP_STATE.NONE;
						fireEvent(LEAP_EVENT.STOP_ZOOM);
					}
				} else {
					currentState = LEAP_STATE.NONE;
					fireEvent(LEAP_EVENT.STOP_ZOOM);
				}
				
			} else if (isZooming2Hands) {
				if (hands.count() == 2) {
					Hand hand1 = hands.get(0);
					Hand hand2 = hands.get(1);
					float distanceBetweenHands = hand1.palmPosition().distanceTo(hand2.palmPosition());
					zoomMultiplier = distanceBetweenHands/zoomHandDistanceRef;
				} else {
					currentState = LEAP_STATE.NONE;
					fireEvent(LEAP_EVENT.STOP_ZOOM);
				}
			} else { // this case shouldn't hit
				currentState = LEAP_STATE.NONE;
			}
		} else {
			isZooming1Hand = false;
			isZooming2Hands = false;
			zoomMultiplier = 1F;
			// zooming using 1 hand
			if (hands.count() == 1) {
				Hand hand = hands.get(0);
				if (hand.grabStrength() > .7) {
					isZooming1Hand = true;
					zoomGrabRef = hand.palmPosition();
					
				}
			}
			// zooming using 2 hands
			else if (hands.count() == 2) {
				isZooming2Hands = true;
				Hand hand1 = hands.get(0);
				Hand hand2 = hands.get(1);
				float distanceBetweenHands = hand1.palmPosition().distanceTo(hand2.palmPosition());
				zoomHandDistanceRef = distanceBetweenHands;
			}
			
			if (isZooming1Hand || isZooming2Hands) {
				currentState = LEAP_STATE.IS_ZOOMING;
				fireEvent(LEAP_EVENT.START_ZOOM);
			}
		}
	}
	
	public boolean close() {
		controller.removeListener(leapListener);
		return true;
	}
}
