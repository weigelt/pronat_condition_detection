package edu.kit.ipd.parse.conditionDetection;

import java.io.File;

import org.junit.Test;

import com.darkprograms.speech.recognizer.GoogleResponse;
import com.darkprograms.speech.recognizer.Recognizer;

public class GoogleSpeechApiTest {

	@Test
	public void speechApi() {
		// erzeugt ein neues Recognizer Objekt. Das zweite Argument ist der API-key. 100 Anfragen pro Tag frei.
		Recognizer recognizer = new Recognizer(Recognizer.Languages.ENGLISH_US, "AIzaSyAeC0EwDNsdjFUHZqnAd2CuxsRSN32Wips");
		try {
			int maxNumOfResponses = 1;
			//Argumente: Deine Flac-Datei als File, Anzahl der Antworten (es funktioniert nur 1) und die Samplerate (8000Hz)
			GoogleResponse response = recognizer.getRecognizedDataForFlac(
					new File("C:\\Users\\Nico\\Documents\\vannyUni\\BA\\steurer_ba\\Evaluation\\audios\\8000\\scene5_18.flac"),
					maxNumOfResponses, (int) 8000);
			System.out.println("Google Response: " + response.getResponse());
			// häufig ist die erste Alternative null, deshalb sollte man auch noch die anderen anschauen
			System.out.println("Other Possible responses are: ");
			for (String s1 : response.getOtherPossibleResponses()) {
				System.out.println("\t" + s1);
			}
			System.out.println("------------------------------------------------------------------------------------------");
		} catch (Exception ex) {
			System.out.println("ERROR: Google cannot be contacted");
			ex.printStackTrace();
		}
	}

}
