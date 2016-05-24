package edu.kit.ipd.parse.conditionDetection;

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
 * @author Vanessa Steurer
 *
 */
public class Synonyms {
	private static final Logger logger = LoggerFactory.getLogger(Synonyms.class);
	private List<List<String>> ifSynonymList;
	private List<List<String>> thenSynonymList;
	private List<List<String>> elseSynonymList;

	public Synonyms() {
		ifSynonymList = new ArrayList<List<String>>();
		thenSynonymList = new ArrayList<List<String>>();
		elseSynonymList = new ArrayList<List<String>>();
	}

	public void importSynonyms() {
		Properties props = ConfigManager.getConfiguration(Synonyms.class);
		String ifSyn = props.getProperty("IF_SYN");
		String thenSyn = props.getProperty("THEN_SYN");
		String elseSyn = props.getProperty("ELSE_SYN");

		splitSynonymInput(ifSyn, ifSynonymList);
		splitSynonymInput(thenSyn, thenSynonymList);
		splitSynonymInput(elseSyn, elseSynonymList);
		SyntaxHelper.setAdverbBlacklist(elseSynonymList);

		if (ifSynonymList.isEmpty()) {
			List<String> keywordIf = new ArrayList<String>();
			keywordIf.add("if");
			ifSynonymList.add(keywordIf);
			logger.debug("No if-synonym found. Added default keyword 'if'.");
		}

		if (thenSynonymList.isEmpty()) {
			List<String> keywordThen = new ArrayList<String>();
			keywordThen.add("then");
			thenSynonymList.add(keywordThen);
			logger.debug("No then-synonym found. Added default keyword 'then'.");
		}

		if (elseSynonymList.isEmpty()) {
			List<String> keywordElse = new ArrayList<String>();
			keywordElse.add("else");
			elseSynonymList.add(keywordElse);
			logger.debug("No else-synonym found. Added default keyword 'else'.");
		}
	}

	private List<List<String>> splitSynonymInput(String synonymInputString, List<List<String>> list) {
		List<String> statementSynonyms = new ArrayList<String>();
		if (synonymInputString != null && !synonymInputString.isEmpty()) {
			statementSynonyms = Arrays.asList(synonymInputString.split(";"));
		}

		for (String synonym : statementSynonyms) {
			List<String> currStatementSynonym = new ArrayList<String>();
			currStatementSynonym = Arrays.asList(synonym.split(" "));
			list.add(currStatementSynonym);
		}

		return list;
	}

	public List<List<String>> getIfSynonyms() {
		return ifSynonymList;
	}

	public List<List<String>> getThenSynonyms() {
		return thenSynonymList;
	}

	public List<List<String>> getElseSynonyms() {
		return elseSynonymList;
	}

}
