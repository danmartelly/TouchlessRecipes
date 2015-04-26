package leap;

import com.sun.javafx.geom.Point2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.leapmotion.leap.*;

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
		IS_TURNING_PREV,
		IS_ROTATING
	};
	
	public enum LEAP_EVENT {
		START_ZOOM,
		END_ZOOM,
		START_NEXT_PAGE,
		END_NEXT_PAGE,
		START_PREV_PAGE,
		END_PREV_PAGE,
		START_ROTATION,
		END_ROTATION
	};
	
	// zoom variables
	protected boolean isZooming1Hand = false;
	protected Vector zoomGrabRef;
	protected float zoomGrabSensitivity = .005F;
	protected boolean isZooming2Hands = false;
	protected float zoomHandDistanceRef = -1F;
	protected float zoomMultiplier = 1F;
	protected float prevZoomMultiplier;
	
	// turn page variables
	protected boolean isSwiping = false;
	protected HashMap<Integer, List<Vector>> pointableVelocityLists; // id is the int
	protected List<Integer> pointableVelocityIdList;
	protected int swipeCurrentFrameCount = 0;
	protected int frameCountBeforeSwipeEnd = 30;
	protected int velocitiesToTrack = 15;
	protected float minSwipeVelocity = 600;
	protected int currentGestID = -1;
	
	// rotation variables
	protected boolean isRotating = false;
	protected Frame rotationRefFrame = null;
	protected float relativeRotation = 0F;
	protected Hand rotationRefHand;
	
	//other
	protected Point2D cursorPosition;
	protected final float cursorXOrigin = -190;
	protected final float cursorYOrigin = 200;
	protected final float cursorSensitvity = 2.5f;
	protected LEAP_STATE currentState = LEAP_STATE.NONE;
	protected boolean isConnected = false;
	public Controller controller;
	public LeapFrameListener leapListener;
	protected List<LeapListener> leapListeners;
	protected boolean timerMode = false;
	
	public LeapManager() {
		controller = new Controller();
		//controller.enableGesture(Gesture.Type.TYPE_SWIPE);
		pointableVelocityLists = new HashMap<Integer, List<Vector>>();
		pointableVelocityIdList = new ArrayList<Integer>();
		leapListener = new LeapFrameListener(this);
		
		leapListeners = new ArrayList<LeapListener>();
		
		controller.addListener(leapListener);
	}
	
	public boolean isTimerMode() {
		return timerMode;
	}
	
	public void setTimerMode(boolean isTimer) {
		timerMode = isTimer;
	}
	
	public LEAP_STATE getCurrentState() {
		return currentState;
	}
	
	public float getZoomMultiplier() {
		return zoomMultiplier;
	}
	
	public float getRotation() {
		return relativeRotation;
	}
	
	public void addListener(LeapListener listener) {
		leapListeners.add(listener);
	}
	
	public void removeListener(LeapListener listener) {
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
		processFrameForRotation(frame);
		processFrameForSwipe(frame);
		processFrameForZooming(frame);
	}
	
	protected void processFrameForRotation(Frame frame) {
		if (!isTimerMode()) {
			if (getCurrentState() == LEAP_STATE.IS_ROTATING) {
				currentState = LEAP_STATE.NONE;
				fireEvent(LEAP_EVENT.END_ROTATION);
			}
			return;
		}
		
		HandList hands = frame.hands();
		if (getCurrentState() == LEAP_STATE.IS_ROTATING) {
			if (hands.count() >= 1) {
				float newRotation = 0.0F;
				for (int i = 0; i < hands.count(); i++) {
					Hand hand = hands.get(i);
					if (hand.id() == rotationRefHand.id() && hand.grabStrength() > .7) {
						newRotation = hand.rotationAngle(rotationRefFrame, Vector.zAxis());
					}
				}
				if (newRotation == 0.0) {
					currentState = LEAP_STATE.NONE;
					fireEvent(LEAP_EVENT.END_ROTATION);
				} else {
					relativeRotation = newRotation;
					System.out.println("relative rotation: " + relativeRotation);
				}
			}
		} else {
			for (int i = 0; i < hands.count(); i++) {
				if (hands.get(i).grabStrength() > .7) {
					rotationRefFrame = frame;
					rotationRefHand = hands.get(i);
					currentState = LEAP_STATE.IS_ROTATING;
					relativeRotation = 0F;
					fireEvent(LEAP_EVENT.START_ROTATION);
					return;
				}
			}
		}
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
			float x = cursorXOrigin + (pos.getX() - cursorXOrigin)*cursorSensitvity;
			float y = cursorYOrigin + (cursorYOrigin - pos.getY())*cursorSensitvity;
			cursorPosition = new Point2D(x, y);
		} else {
			cursorPosition = null;
		}
	}
	
	protected void processFrameForSwipe(Frame frame) {
		if (isTimerMode()) {
			isSwiping = false;
			if (getCurrentState() == LEAP_STATE.IS_TURNING_NEXT) {
				currentState = LEAP_STATE.NONE;
				fireEvent(LEAP_EVENT.END_NEXT_PAGE);
			} else if (getCurrentState() == LEAP_STATE.IS_TURNING_PREV) {
				currentState = LEAP_STATE.NONE;
				fireEvent(LEAP_EVENT.END_PREV_PAGE);
			}
			return;
		}
		
		// update velocities and include new ids
		HandList hands = frame.hands();
		List<Integer> allPointIds = new ArrayList<Integer>(); 
		for (int i = 0; i < hands.count(); i++) {
			Hand hand = hands.get(i);
			PointableList points = hand.pointables();
			for (int j = 0; j < points.count(); j++) {
				Pointable p = points.get(j);
				allPointIds.add(p.id());
				if (pointableVelocityLists.containsKey(p.id())) {
					List<Vector> velList = pointableVelocityLists.get(p.id());
					if (velList.size() > velocitiesToTrack)
						velList.remove(0);
					velList.add(p.tipVelocity());
				} else {
					pointableVelocityIdList.add(p.id());
					List<Vector> newList = new LinkedList<Vector>();
					newList.add(p.tipVelocity());
					pointableVelocityLists.put(p.id(), newList);
				}
			}
		}
		
		// remove old ids
		for (int i = 0; i < pointableVelocityIdList.size(); i++) {
			int id = pointableVelocityIdList.get(i);
			if (!allPointIds.contains(id)) {
				pointableVelocityLists.remove(id);
				pointableVelocityIdList.remove(i);
			}
		}
		
		if (getCurrentState() != LEAP_STATE.NONE &&
			getCurrentState() != LEAP_STATE.IS_TURNING_NEXT &&
			getCurrentState() != LEAP_STATE.IS_TURNING_PREV) {
			isSwiping = false;
			return;
		}
		
		if (isSwiping) {
			swipeCurrentFrameCount++;
			if (swipeCurrentFrameCount > frameCountBeforeSwipeEnd) {
				if (getCurrentState() == LEAP_STATE.IS_TURNING_NEXT) {
					currentState = LEAP_STATE.NONE;
					fireEvent(LEAP_EVENT.END_NEXT_PAGE);
					isSwiping = false;
				} else if (getCurrentState() == LEAP_STATE.IS_TURNING_PREV) {
					currentState = LEAP_STATE.NONE;
					fireEvent(LEAP_EVENT.END_PREV_PAGE);
					isSwiping = false;
				}
			}
			return;
		}
		
		// see if any velocities are above a threshold
		for (int i = 0; i < pointableVelocityIdList.size(); i++) {
			int id = pointableVelocityIdList.get(i);
			List<Vector> velList = pointableVelocityLists.get(id);
			if (velList.size() >= velocitiesToTrack) {
				Vector sum = new Vector();
				for (int j = 0; j < velList.size(); j++) {
					sum = sum.plus(velList.get(j));
				}
				sum = sum.times((float) (1./velList.size()));
				Vector normalized = sum.normalized();
				if (!isSwiping) {
					if (sum.magnitude() > minSwipeVelocity) {
						if (normalized.getX() < -.5) {
							isSwiping = true;
							currentState = LEAP_STATE.IS_TURNING_NEXT;
							fireEvent(LEAP_EVENT.START_NEXT_PAGE);
							clearSwipeData();
							return;
						} else if (normalized.getX() > .5) {
							isSwiping = true;
							currentState = LEAP_STATE.IS_TURNING_PREV;
							fireEvent(LEAP_EVENT.START_PREV_PAGE);
							clearSwipeData();
							return;
						}
					}
				}
			}
		}
	}
	
	protected void clearSwipeData() {
		pointableVelocityLists.clear();
		pointableVelocityIdList.clear();
		swipeCurrentFrameCount = 0;
	}
	
	protected void processFrameForZooming(Frame frame) {
		if (isTimerMode()) {
			if (getCurrentState() == LEAP_STATE.IS_ZOOMING) {
				currentState = LEAP_STATE.NONE;
				fireEvent(LEAP_EVENT.END_ZOOM);
			}
			return;
		}
		
		HandList hands = frame.hands();
		if (getCurrentState() == LEAP_STATE.IS_ZOOMING) {
			if (isZooming1Hand) {
				// zooming using 1 hand
				if (hands.count() == 1) {
					Hand hand = hands.get(0);
					zoomMultiplier = prevZoomMultiplier * (float) Math.max(0.3, 1. - (zoomGrabRef.getZ() - hand.palmPosition().getZ())*zoomGrabSensitivity);
					if (hand.grabStrength() < .7 || hand.pointables().extended().count() > 0) {
						isZooming1Hand = false;
						currentState = LEAP_STATE.NONE;
						fireEvent(LEAP_EVENT.END_ZOOM);
					}
				} else {
					currentState = LEAP_STATE.NONE;
					fireEvent(LEAP_EVENT.END_ZOOM);
				}
				
			} else if (isZooming2Hands) {
				if (hands.count() == 2) {
					Vector pos1 = hands.get(0).palmPosition();
					Vector pos2 = hands.get(1).palmPosition();
					pos1.setZ(0);
					pos2.setZ(0);
					float distanceBetweenHands = pos1.distanceTo(pos2);
					zoomMultiplier = prevZoomMultiplier * distanceBetweenHands/zoomHandDistanceRef;
				} else {
					currentState = LEAP_STATE.NONE;
					fireEvent(LEAP_EVENT.END_ZOOM);
				}
			} else { // this case shouldn't hit
				currentState = LEAP_STATE.NONE;
			}
		} else {
			isZooming1Hand = false;
			isZooming2Hands = false;
			// zooming using 1 hand
			if (hands.count() == 1) {
				Hand hand = hands.get(0);
				if (hand.grabStrength() > .7 && hand.pointables().extended().count() == 0) {
					isZooming1Hand = true;
					zoomGrabRef = hand.palmPosition();
					prevZoomMultiplier = zoomMultiplier;
				}
			}
			// zooming using 2 hands
			else if (hands.count() == 2) {
				isZooming2Hands = true;
				Hand hand1 = hands.get(0);
				Hand hand2 = hands.get(1);
				float distanceBetweenHands = hand1.palmPosition().distanceTo(hand2.palmPosition());
				zoomHandDistanceRef = distanceBetweenHands;
				prevZoomMultiplier = zoomMultiplier;
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
