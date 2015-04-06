import java.io.IOException;

import leap.LeapManager;
import tts.TTS;


public class Main {
	public static void main(String[] args) {
		LeapManager leapManager = new LeapManager();
		TTS tts = new TTS();
		
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
