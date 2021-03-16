package edu.kit.ipd.pronat.condition_detection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.tools.ConfigManager;

/**
 * This class provides all valid synonyms of the commandpattern-keywords.
 * Synonyms are lists of String-lists, because synonyms can consist of more than
 * one word.
 *
 * @author Sebastian Weigelt
 * @author Vanessa Steurer
 *
 */
public class Synonyms {
	private static final String DEFAULT_KEYWORD_ELSE = "else";
	private static final String DEFAULT_KEYWORD_THEN = "then";
	private static final String DEFAULT_KEYWORD_IF = "if";

	/** Key for loading else synonyms */
	static final String PROPS_ELSE_SYN = "ELSE_SYN";
	/** Key for loading then synonyms */
	static final String PROPS_THEN_SYN = "THEN_SYN";
	/** Key for loading if synonyms */
	static final String PROPS_IF_SYN = "IF_SYN";
	/** Delimiter for key words and key phrases in properties file */
	private static final String PROPS_STRING = ",";

	private List<KeyPhrase> ifSynonymList;
	private List<KeyPhrase> thenSynonymList;
	private List<KeyPhrase> elseSynonymList;

	private static final Logger logger = LoggerFactory.getLogger(Synonyms.class);

	public Synonyms() {
		ifSynonymList = new ArrayList<KeyPhrase>();
		thenSynonymList = new ArrayList<KeyPhrase>();
		elseSynonymList = new ArrayList<KeyPhrase>();
	}

	public void importSynonyms() {
		logger.trace("Loading synonyms for condition detection from configuration");

		Properties props = ConfigManager.getConfiguration(Synonyms.class);
		String ifSyn = props.getProperty(PROPS_IF_SYN, DEFAULT_KEYWORD_IF).trim();
		ifSyn = ifSyn.isEmpty() ? DEFAULT_KEYWORD_IF : ifSyn;

		String thenSyn = props.getProperty(PROPS_THEN_SYN, DEFAULT_KEYWORD_THEN).trim();
		thenSyn = thenSyn.isEmpty() ? DEFAULT_KEYWORD_THEN : thenSyn;

		String elseSyn = props.getProperty(PROPS_ELSE_SYN, DEFAULT_KEYWORD_ELSE).trim();
		elseSyn = elseSyn.isEmpty() ? DEFAULT_KEYWORD_ELSE : elseSyn;

		splitSynonymInput(ifSyn, ifSynonymList);
		splitSynonymInput(thenSyn, thenSynonymList);
		splitSynonymInput(elseSyn, elseSynonymList);
		SyntaxHelper.setAdverbBlacklist(elseSynonymList);
	}

	private List<KeyPhrase> splitSynonymInput(String synonymInputString, List<KeyPhrase> list) {
		List<String> statementSynonyms = new ArrayList<String>();
		if (synonymInputString != null && !synonymInputString.isEmpty()) {
			statementSynonyms = Arrays.asList(synonymInputString.split(PROPS_STRING));
		}

		for (String synonym : statementSynonyms) {
			KeyPhrase currStatementSynonym = new KeyPhrase();
			for (String s : synonym.trim().split(" ")) {
				currStatementSynonym.add(s);
			}
			list.add(currStatementSynonym);
		}

		return list;
	}

	public List<KeyPhrase> getIfSynonyms() {
		return ifSynonymList;
	}

	public List<KeyPhrase> getThenSynonyms() {
		return thenSynonymList;
	}

	public List<KeyPhrase> getElseSynonyms() {
		return elseSynonymList;
	}
}
