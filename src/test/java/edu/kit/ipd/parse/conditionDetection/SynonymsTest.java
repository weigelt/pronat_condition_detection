package edu.kit.ipd.parse.conditionDetection;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import edu.kit.ipd.parse.luna.tools.ConfigManager;

/**
 * This Testclass tests the correct import of the commandpattern-synonyms of
 * If/Then/Else.
 */
public class SynonymsTest {

	@Test
	public void testDefaultValues() {
		Properties synConfig = ConfigManager.getConfiguration(Synonyms.class);
		String ifProps = synConfig.getProperty(Synonyms.PROPS_IF_SYN);
		String thenProps = synConfig.getProperty(Synonyms.PROPS_IF_SYN);
		String elseProps = synConfig.getProperty(Synonyms.PROPS_IF_SYN);

		synConfig.setProperty(Synonyms.PROPS_IF_SYN, "");
		synConfig.setProperty(Synonyms.PROPS_THEN_SYN, "");
		synConfig.setProperty(Synonyms.PROPS_ELSE_SYN, "");

		Synonyms s = new Synonyms();
		s.importSynonyms();

		Assert.assertArrayEquals(new String[] { "if" }, s.getIfSynonyms().get(0).toArray(new String[] {}));
		Assert.assertArrayEquals(new String[] { "then" }, s.getThenSynonyms().get(0).toArray(new String[] {}));
		Assert.assertArrayEquals(new String[] { "else" }, s.getElseSynonyms().get(0).toArray(new String[] {}));

		synConfig.setProperty(Synonyms.PROPS_IF_SYN, ifProps);
		synConfig.setProperty(Synonyms.PROPS_THEN_SYN, thenProps);
		synConfig.setProperty(Synonyms.PROPS_THEN_SYN, elseProps);
	}

	@Test
	public void importIfSynonyms() {
		Synonyms synonym = new Synonyms();
		synonym.importSynonyms();
		for (int i = 0; i < synonym.getIfSynonyms().size(); i++) {
			System.out.println(synonym.getIfSynonyms().get(i));
		}
	}
}
