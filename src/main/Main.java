package main;
import java.io.IOException;

import leap.LeapListener;
import leap.LeapManager;
import leap.LeapManager.LEAP_EVENT;
import tts.TTS;


public class Main {
	
	public class TestClass implements LeapListener {

		LeapManager l;
		public TestClass(LeapManager l) {
			this.l = l;
		}
		
		@Override
		public void eventFired(LEAP_EVENT event) {
			System.out.println("event fired: " + event + "zoom: " + l.getZoomMultiplier());
		}
		
	}
	
	public static void main(String[] args) {
		LeapManager leapManager = new LeapManager();
		TTS tts = new TTS();
		
		Main rand = new Main();
		leapManager.setTimerMode(true);
		
		//TestClass test = rand.new TestClass(leapManager);
		//leapManager.addListener(test);
		
		//tts.say("Welcome to Touch less Recipes.");
		//tts.say("Press any key to quit.");
		
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		tts.close();
		leapManager.close();
	}
}
