package edu.kit.ipd.parse.conditionDetection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * Static helperclass, which provides methods for scanning text on keywords of
 * if, then and else-clauses.
 * 
 * @author Vanessa Steurer
 */
public class KeywordScanner {
	private static final String COMMAND_TYPE = "commandType";
	private static final String POS_CC = "CC";
	private static final String POS = "pos";
	private static final String CHUNK_IOB = "chunkIOB";
	private static final Logger logger = LoggerFactory.getLogger(KeywordScanner.class);
	private static final HashSet<String> auxiliaryVerbList = new HashSet<String>(
			Arrays.asList(new String[] { "am", "is", "are", "was", "were", "has", "have", "had" }));
	private static final HashSet<String> elseKeywordBlacklist = new HashSet<String>(
			Arrays.asList(new String[] { "somebody", "somewhere", "someone", "anybody", "anywhere", "anyone" }));

	private KeywordScanner() {

	}

	// TODO Why are there three scanner methods  shouldn't they be all the same (with the list of synonyms as an additional parameter?
	// See #3
	public static List<Keyword> searchIfKeywords(Synonyms synonyms, INode[] nodes) {
		List<KeyPhrase> ifSynonymList = synonyms.getIfSynonyms();
		List<Keyword> ifKeywordsInText = new ArrayList<Keyword>();

		for (int i = 0; i < nodes.length; i++) { // Search for if-keywords in input
			boolean isKeyword = false;

			for (int j = 0; j < ifSynonymList.size(); j++) { // Compare words with existing synonyms
				int counter = 0;
				if (isKeyword) {
					break;
				}

				for (int k = 0; k < ifSynonymList.get(j).size(); k++) { // Consider synonyms consisting of more than one word
					if ((i - 1) >= 0) {
						if (ifSynonymList.get(j).get(0).equalsIgnoreCase("if")
								&& nodes[i].getAttributeValue("value").toString().equalsIgnoreCase("if")
								&& (nodes[i - 1].getAttributeValue("value").toString().equalsIgnoreCase("only")
										|| nodes[i - 1].getAttributeValue("value").toString().equalsIgnoreCase("else"))) {
							break;
						}
					}

					if (hasNext(i, nodes.length)) {
						if (ifSynonymList.get(j).get(0).equalsIgnoreCase("if")
								&& nodes[i].getAttributeValue("value").toString().equalsIgnoreCase("if")
								&& (nodes[i + 1].getAttributeValue("value").toString().equalsIgnoreCase("not")
										|| nodes[i + 1].getAttributeValue("value").toString().equalsIgnoreCase("so"))) {
							logger.debug("Found and skip then-statement keywords 'if so' or 'if not'.");
							break;
						}
					}

					if (hasNext(k + i, nodes.length)) {
						if (ifSynonymList.get(j).get(k).equals(nodes[i + k].getAttributeValue("value").toString().toLowerCase())) {
							counter++;
							if (counter == ifSynonymList.get(j).size()) {
								Keyword hint = new Keyword(CommandType.IF_STATEMENT, ifSynonymList.get(j).toString(), i, i + k);
								ifKeywordsInText.add(hint);
								isKeyword = true;
								break;
							}
						} else {
							break; // Mismatch: look for next synonym
						}
					}
				}
			}
		}

		if (ifKeywordsInText.isEmpty()) {
			logger.debug("Couldn't find condition-clauses in input.");
		} else {
			logger.debug("Found if-keywords {}", Arrays.toString(ifKeywordsInText.toArray(new Keyword[] {})));
		}

		return ifKeywordsInText;
	}

	public static List<Keyword> searchThenKeywords(Synonyms synonyms, INode[] nodes, List<Keyword> ifHints) {
		List<KeyPhrase> thenSynonymList = synonyms.getThenSynonyms();
		List<Keyword> thenKeywordsInText = new ArrayList<Keyword>();

		for (Keyword keyword : ifHints) {
			boolean isKeyword = false;
			int currStmt = keyword.getKeywordEnd() + 1;
			int keywordIndex = ifHints.indexOf(keyword);
			int nextStmt;
			if (keywordIndex + 1 < ifHints.size()) {
				nextStmt = ifHints.get(keywordIndex + 1).getKeywordBegin();
			} else {
				nextStmt = nodes.length;
			}
			logger.info("Search for then-keywords in detected if-clause number {}", keywordIndex + 1);

			currStmt = skipStatement(nodes, currStmt, CommandType.IF_STATEMENT); // Skip if-statement	
			logger.debug("Search then-keywords between position {} and {}", currStmt, (nextStmt - 1));

			int firstDetectedVerb = currStmt;
			boolean foundVerb = false;
			for (int i = currStmt; i < nextStmt; i++) { // Look for first verb
				if (nodes[i].getAttributeValue(CHUNK_IOB).toString().toUpperCase().contains("-VP")
						&& !nodes[i - 1].getAttributeValue(CHUNK_IOB).toString().toUpperCase().contains("-PP")
						&& !nodes[i - 1].getAttributeValue(POS).toString().equalsIgnoreCase(POS_CC)
						&& !auxiliaryVerbList.contains(nodes[i - 1].getAttributeValue("value").toString().toLowerCase())) {
					firstDetectedVerb = i;
					foundVerb = true;
					break;
				}
			}

			if (nodes[currStmt].getAttributeValue(COMMAND_TYPE) == null) { // Search for then-keywords in input
				for (int i = currStmt; i < firstDetectedVerb; i++) { // FIRST: Search between end of if-stmt to next verb	
					if (isKeyword) {
						break;
					}

					isKeyword = compareInputWithSyn(nodes, thenKeywordsInText, thenSynonymList, CommandType.THEN_STATEMENT, isKeyword, i);
				}

				if (!isKeyword) {
					for (int i = currStmt; i < nextStmt; i++) { // SECOND if first failed: Search between end of if-stmt to next if-stmt
						if (isKeyword) {
							break;
						}

						isKeyword = compareInputWithSyn(nodes, thenKeywordsInText, thenSynonymList, CommandType.THEN_STATEMENT, isKeyword,
								i);
					}

					if (!isKeyword) {
						Keyword hint = new Keyword(null, "DUMMY_KEYWORD", firstDetectedVerb, firstDetectedVerb); // No keyword found
						if (!foundVerb) {
							hint.setInvalid();
						}
						thenKeywordsInText.add(hint);
						logger.debug("No then-Keyword found in current if-Statement.");
					}
				}

				// Then-stmt beginns with verb (more than 3 consecutive verbs found in if-heuristic)
			} else if (nodes[currStmt].getAttributeValue(COMMAND_TYPE).equals(CommandType.THEN_STATEMENT)) {
				Keyword hint = new Keyword(CommandType.THEN_STATEMENT, "DUMMY_KEYWORD", currStmt, currStmt);
				thenKeywordsInText.add(hint);
				logger.debug("No then-Keyword found in current if-Statement.");
			}
		}

		return thenKeywordsInText;
	}

	public static List<Keyword> searchElseKeywords(Synonyms synonyms, INode[] nodes, List<Keyword> ifHints) {
		List<KeyPhrase> elseSynonymList = synonyms.getElseSynonyms();
		List<Keyword> elseKeywordsInText = new ArrayList<Keyword>();

		for (Keyword keyword : ifHints) {
			boolean isKeyword = false;
			int currStmt = keyword.getKeywordEnd() + 1;
			int keywordIndex = ifHints.indexOf(keyword);
			int nextStmt;
			if (keywordIndex + 1 < ifHints.size()) {
				nextStmt = ifHints.get(keywordIndex + 1).getKeywordBegin();
			} else {
				nextStmt = nodes.length;
			}
			logger.info("Search for else-keywords in detected if-clause number {}", keywordIndex + 1);

			currStmt = skipStatement(nodes, currStmt, CommandType.IF_STATEMENT); // Skip IF-Statement
			currStmt = skipStatement(nodes, currStmt, CommandType.THEN_STATEMENT); // Skip THEN-Statement (in case one exists)
			currStmt = skipStatement(nodes, currStmt, CommandType.INDEPENDENT_STATEMENT); // Skip INDP-Statement (in case one exists)
			logger.debug("Search else-keywords between position {} and {}", currStmt, nextStmt);

			for (int i = currStmt; i < nextStmt; i++) { // Search for else-keywords in input
				if (isKeyword) {
					break;
				}

				isKeyword = compareInputWithSyn(nodes, elseKeywordsInText, elseSynonymList, CommandType.ELSE_STATEMENT, isKeyword, i);
			}

			if (!isKeyword) {
				Keyword hint = new Keyword(null, "DUMMY_KEYWORD", currStmt, currStmt); // no else-stmt found
				elseKeywordsInText.add(hint);
				logger.debug("No else-keyword found in current if-Statement -> no else-Statement.");
			}
		}

		return elseKeywordsInText;
	}

	private static boolean hasNext(int currentWord, int numberOfWords) {
		return (currentWord + 1) < numberOfWords;
	}

	private static int skipStatement(INode[] nodes, int currStmt, CommandType commandType) { // first word after statement
		while (nodes[currStmt].getAttributeValue(COMMAND_TYPE) != null
				&& nodes[currStmt].getAttributeValue(COMMAND_TYPE).equals(commandType)) {
			currStmt++;
			if (currStmt >= nodes.length) {
				return currStmt - 1;
			}
		}

		return currStmt;
	}

	private static boolean compareInputWithSyn(INode[] nodes, List<Keyword> keywordsInText, List<KeyPhrase> thenSynonymList,
			CommandType cmdType, boolean isKeyword, int i) {
		for (int j = 0; j < thenSynonymList.size(); j++) { // Compare words with existing synonyms

			int counter = 0;
			for (int k = 0; k < thenSynonymList.get(j).size(); k++) { // Consider synonyms consisting of more than one word
				if (hasNext(i, nodes.length)) {
					if (thenSynonymList.get(j).get(0).equalsIgnoreCase("else")
							&& nodes[i].getAttributeValue("value").toString().equalsIgnoreCase("else")
							&& (nodes[i + 1].getAttributeValue("value").toString().equalsIgnoreCase("if")
									|| elseKeywordBlacklist.contains(nodes[i - 1].getAttributeValue("value").toString().toLowerCase()))) {

						logger.debug("Skip false else-keywords 'somebody else', 'anybody else', 'else if' etc.");
						break;
					}
				}

				if (thenSynonymList.get(j).get(k).equalsIgnoreCase(nodes[i + k].getAttributeValue("value").toString())) {
					counter++;
					if (counter == thenSynonymList.get(j).size()) {
						Keyword hint = new Keyword(cmdType, thenSynonymList.get(j).toString(), i, i + k);
						keywordsInText.add(hint);
						logger.debug("Found key phrase: {}", thenSynonymList.get(j).toString());
						return true;
					}

				} else {
					break; // Mismatch: check next synonym
				}
			}
		}

		return false;
	}

	private static String tooString(List<Keyword> keywords) {
		String keywordsFound = "";
		for (Keyword keyword : keywords) {
			keywordsFound = keywordsFound.concat(keyword.getKeyword() + ",");
		}

		return keywordsFound;
	}
}