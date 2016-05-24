package edu.kit.ipd.parse.conditionDetection;

import org.junit.Test;

/**
 * This Testclass tests the correct import of the commandpattern-synonyms of
 * If/Then/Else.
 */
public class SynonymsTest {

	@Test
	public void importIfSynonyms() {
		Synonyms synonym = new Synonyms();
		synonym.importSynonyms();
		for (int i = 0; i < synonym.getIfSynonyms().size(); i++) {
			System.out.println(synonym.getIfSynonyms().get(i));
		}
	}
}
