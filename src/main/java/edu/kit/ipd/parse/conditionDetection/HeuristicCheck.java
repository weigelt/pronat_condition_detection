package edu.kit.ipd.parse.conditionDetection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * Static helperclass, which provides methods for checking heuristical
 * constraints.
 *
 * @author Vanessa Steurer
 */
public class HeuristicCheck {
	private static final Logger logger = LoggerFactory.getLogger(HeuristicCheck.class);
	private static HashSet<String> auxiliaryVerbList = new HashSet<String>(
			Arrays.asList(new String[] { "am", "is", "are", "was", "were", "has", "have", "had" }));

	private HeuristicCheck() {

	}

	public static void checkForIfClause(INode[] nodes, List<Keyword> ifKeywordsInText, boolean[] verfifiedByDA) {
		List<Keyword> heuristicFails = new ArrayList<Keyword>();
		for (Keyword keyword : ifKeywordsInText) {
			int begin = keyword.getKeywordEnd() + 1;
			int keywordIndex = ifKeywordsInText.indexOf(keyword);
			int nextStmt;
			if (keywordIndex + 1 < ifKeywordsInText.size()) {
				nextStmt = ifKeywordsInText.get(keywordIndex + 1).getKeywordBegin();
			} else {
				nextStmt = nodes.length;
			}
			logger.info("Check heuristic for detected if-clause number " + (keywordIndex + 1) + ".");

			// NOUN PHRASE: NP _or_ NOUN BLOCK: NP PP NP; NP CC NP;
			if (!SyntaxHelper.isNounPhrase(nodes, begin)) {
				setCommandType(nodes, keyword.getKeywordBegin(), nextStmt - 1, CommandType.INDEPENDENT_STATEMENT, verfifiedByDA);
				heuristicFails.add(keyword);
				logger.debug("IF-Heuristic-Fail: no nominal phrase (NP) after if-keyword.");
				continue;
			}

			int endOfNounBlock = begin;
			for (int i = begin; i < nextStmt; i++) {
				if (!(SyntaxHelper.isNounPhrase(nodes, i) || SyntaxHelper.isNounBlock(nodes, i, nextStmt))) {
					endOfNounBlock = i - 1;
					logger.debug("End of noun-block at position: " + endOfNounBlock);
					break;
				}
				endOfNounBlock = i;
			}

			// VERB PHRASE: VP _or_ VERB BLOCK: VP CC VP, VP 'not' VP, VP PP VP
			if (endOfNounBlock + 1 < nextStmt && !SyntaxHelper.isVerbPhrase(nodes, endOfNounBlock + 1)) {
				setCommandType(nodes, keyword.getKeywordBegin(), nextStmt - 1, CommandType.INDEPENDENT_STATEMENT, verfifiedByDA);
				heuristicFails.add(keyword);
				logger.debug("IF-Heristic-Fail: no verb phrase (VP) after ifclause keyword and noun.");
				continue;
			}

			int endOfVerbBlock = endOfNounBlock;
			int verbCounter = 0;
			boolean beginThenClause = false;
			for (int i = endOfVerbBlock + 1; i < nextStmt; i++) {
				if (!(SyntaxHelper.isVerbPhrase(nodes, i) || SyntaxHelper.isVerbBlock(nodes, i, nextStmt))) {
					endOfVerbBlock = i - 1;
					logger.debug("End of verb-block at position: " + endOfVerbBlock);
					break;
				}
				endOfVerbBlock = i;

				if (ConditionDetector.compensateSNLP) { // catch IOB-format-failures of the ASR
					if (SyntaxHelper.isVerbPhrase(nodes, i)) {
						verbCounter++; // if there are too many consecutive verbs, the last is chosen for then-stmt begin
						if (verbCounter == 2 && SyntaxHelper.isVerbPhrase(nodes, i - 1) // 2 consecutive verbs ("if .. playing, go")
								&& !auxiliaryVerbList.contains(nodes[i - 1].getAttributeValue("value").toString().toLowerCase())) {
							ConditionDetector.setNodeAttribute(nodes[i], i, "commandType", CommandType.THEN_STATEMENT, verfifiedByDA);
							beginThenClause = true;
							logger.debug("Found double verb phrase.");
							break;

						} else if (verbCounter == 3) { // 3 consecutive verbs ("if .. has playing, go")
							ConditionDetector.setNodeAttribute(nodes[i], i, "commandType", CommandType.THEN_STATEMENT, verfifiedByDA);
							beginThenClause = true;
							logger.debug("Found triple verb phrase.");
							break;
						}
					} else { // verbblock: the current word is a conjunction (CC)
						verbCounter = 1;
					}
				} else { // dont catch IOB-format-failures of the ASR
					if (!SyntaxHelper.isVerbBlock(nodes, i - 1, nextStmt)
							&& nodes[i].getAttributeValue("chunkIOB").toString().equals("B-VP")) {
						verbCounter++;
						if (verbCounter == 2) {
							ConditionDetector.setNodeAttribute(nodes[i], i, "commandType", CommandType.THEN_STATEMENT, verfifiedByDA);
							beginThenClause = true;
							break;
						}
					}
				}
			}

			if (beginThenClause) {
				logger.debug("End of spotted if-clause at position: " + (endOfVerbBlock - 1) + "\n");
				setCommandType(nodes, keyword.getKeywordBegin(), endOfVerbBlock - 1, CommandType.IF_STATEMENT, verfifiedByDA);
				continue;
			}

			// advanced heuristic: ADJP, ADVP, PRP, PP
			int endOfIfBlock = findPP_ADJP_ADVP_PRP(nodes, nextStmt, endOfVerbBlock);

			setCommandType(nodes, keyword.getKeywordBegin(), endOfIfBlock, CommandType.IF_STATEMENT, verfifiedByDA);
			logger.debug("End of spotted if-clause at position: " + endOfIfBlock + "\n");
		}

		for (Keyword hint : heuristicFails) { // when If-heuristic fails, no ifclause in input
			ifKeywordsInText.remove(hint);
		}
	}

	public static void checkForThenClause(INode[] nodes, List<Keyword> thenKeywordsInText, boolean[] verfifiedByDA) {
		for (Keyword keyword : thenKeywordsInText) {
			int currStmt = keyword.getKeywordEnd() + 1;
			int nextStmt = searchNextIfStmt(nodes, currStmt);
			int keywordIndex = thenKeywordsInText.indexOf(keyword);
			logger.info("Check heuristic for detected then-clause number " + (keywordIndex + 1) + ".");

			if (keyword.isDummy() && keyword.getCmdtype() == null) { // if no then-Keyword is found, start then-heuristic check at first verb
				currStmt -= 1;
				if (keyword.isInvalid()) {
					setCommandType(nodes, currStmt, nextStmt - 1, CommandType.INDEPENDENT_STATEMENT, verfifiedByDA);
					logger.debug("No then-keyword and no verb found in this conditional clause.\n");
					continue;
				}
				logger.debug("Set first spotted verb (" + nodes[currStmt].getAttributeValue("value").toString()
						+ ") as beginning of then-clause.");

			} else if (keyword.isDummy() && keyword.getCmdtype().equals(CommandType.THEN_STATEMENT)) { // verb is already set as start of then-clause
				currStmt -= 1;
				logger.debug("Double/Triple verbcomposition found. Then-clause begins with verb ("
						+ nodes[currStmt].getAttributeValue("value").toString() + ").");

			} else if (!keyword.isDummy() && currStmt == nodes.length) { // if the found then-keyword is the last word in the input, set invalid (e.g. please)
				keyword.setKeyword("DUMMY_KEYWORD");
				keyword.setInvalid();
				continue;
			}

			// VERB PHRASE: VP _oder_ VERB BLOCK: VP CC VP, VP 'not' VP, VP PP VP
			if (!SyntaxHelper.isVerbPhrase(nodes, currStmt)) {
				int endOfThenBlock = checkForAlternativeWordOrder(nodes, currStmt, nextStmt);
				if (endOfThenBlock > currStmt) {
					setCommandType(nodes, currStmt - 1, endOfThenBlock, CommandType.THEN_STATEMENT, verfifiedByDA);
					keyword.setKeyword(nodes[currStmt - 1].getAttributeValue("value").toString());
					logger.debug("End of spotted then-clause with alternative word-order at position: " + endOfThenBlock + "\n");
					continue;
				}
				setCommandType(nodes, keyword.getKeywordBegin(), currStmt, CommandType.INDEPENDENT_STATEMENT, verfifiedByDA);
				keyword.setKeyword("DUMMY_KEYWORD");
				logger.debug("THEN-Heristic-Fail: no verb phrase (VP) or nominal phrase (NP) after then-keyword.");
				continue;
			}

			int endOfVerbBlock = currStmt;
			for (int i = currStmt; i < nextStmt; i++) {
				if (!(SyntaxHelper.isVerbPhrase(nodes, i) || SyntaxHelper.isVerbBlock(nodes, i, nextStmt))) {
					endOfVerbBlock = i - 1;
					logger.debug("End of verb-block at position: " + endOfVerbBlock);
					break;
				}
				endOfVerbBlock = i;
			}

			// NOUN PHRASE: NP _or_ NOUN BLOCK: NP PP NP; NP CC NP;
			if (endOfVerbBlock + 1 < nextStmt && !(SyntaxHelper.isNounPhrase(nodes, endOfVerbBlock + 1)
					|| SyntaxHelper.isNounBlock(nodes, endOfVerbBlock + 1, nextStmt)
					|| SyntaxHelper.isAdverbPhrase(nodes, endOfVerbBlock + 1)
					|| SyntaxHelper.isParticlePhrase(nodes, endOfVerbBlock + 1))) {
				// check alternative wordorder Then+NP+VP (Aussage statt Anweisung)
				int wordBeforeVerb = currStmt - 1;
				if (SyntaxHelper.isNounPhrase(nodes, wordBeforeVerb) && nodes[wordBeforeVerb].getAttributeValue("commandType") == null) {
					int endOfThenBlock = checkForAlternativeWordOrder(nodes, wordBeforeVerb, nextStmt);
					if (endOfThenBlock > currStmt) {
						while (nodes[wordBeforeVerb].getAttributeValue("commandType") == null
								&& SyntaxHelper.isNounPhrase(nodes, wordBeforeVerb)) {
							ConditionDetector.setNodeAttribute(nodes[wordBeforeVerb], wordBeforeVerb, "commandType",
									CommandType.THEN_STATEMENT, verfifiedByDA);
							wordBeforeVerb--;
						}
						setCommandType(nodes, wordBeforeVerb + 1, endOfThenBlock, CommandType.THEN_STATEMENT, verfifiedByDA);
						keyword.setKeyword(nodes[currStmt - 1].getAttributeValue("value").toString());
						keyword.setKeywordBegin(currStmt - 1);
						keyword.setKeywordEnd(currStmt - 1);
						logger.debug("End of spotted then-clause with alternative word-order at position: " + endOfThenBlock + "\n");
						continue;
					}
				}
			}

			int endOfNounBlock = endOfVerbBlock;
			for (int i = endOfNounBlock + 1; i < nextStmt; i++) {
				if (!(SyntaxHelper.isNounPhrase(nodes, i) || SyntaxHelper.isNounBlock(nodes, i, nextStmt))) {
					endOfNounBlock = i - 1;
					logger.debug("End of noun-block at position: " + endOfNounBlock);
					break;
				}
				endOfNounBlock = i;
			}

			// advanced heuristic: ADJP, ADVP, PRP, PP
			int endOfThenBlock = findPP_ADJP_ADVP_PRP(nodes, nextStmt, endOfNounBlock);
			endOfThenBlock = searchEndOfCurrChunk(nodes, nextStmt, endOfThenBlock, endOfThenBlock + 1);

			if (keyword.isDummy()) {
				keyword.setKeyword(nodes[currStmt].getAttributeValue("value").toString());
			}

			setCommandType(nodes, keyword.getKeywordBegin(), endOfThenBlock, CommandType.THEN_STATEMENT, verfifiedByDA);
			logger.debug("End of spotted then-clause at position: " + endOfThenBlock + "\n");
		}
	}

	public static void checkForElseClause(INode[] nodes, List<Keyword> elseKeywordsInText, boolean[] verfiedByDA) {
		for (Keyword keyword : elseKeywordsInText) {
			int currStmt = keyword.getKeywordEnd() + 1;
			int nextStmt = searchNextIfStmt(nodes, currStmt);
			int keywordIndex = elseKeywordsInText.indexOf(keyword);

			if (!keyword.isDummy()) { // wenn else-Keyword gefunden wurde, prüfe Heuristik
				logger.info("Check heuristic for detected else-clause number " + (keywordIndex + 1) + ".");

				// VERB PHRASE: VP _or_ VERB BLOCK: VP CC VP, VP 'not' VP, VP PP VP
				if (!SyntaxHelper.isVerbPhrase(nodes, currStmt)) {
					if (!keyword.isDummy()) {
						int endOfElseBlock = checkForAlternativeWordOrder(nodes, currStmt, nextStmt);
						if (endOfElseBlock > currStmt) {
							setCommandType(nodes, keyword.getKeywordBegin(), endOfElseBlock, CommandType.ELSE_STATEMENT, verfiedByDA);
							logger.debug("End of spotted else-clause with alternative word-order at position: " + endOfElseBlock + "\n");
							continue;
						}
					}
					setCommandType(nodes, keyword.getKeywordBegin(), nextStmt - 1, CommandType.INDEPENDENT_STATEMENT, verfiedByDA);
					keyword.setKeyword("DUMMY_KEYWORD");
					logger.debug("ELSE-Heristic-Fail: no verb phrase (VP) after else-clause-keyword.");
					continue;
				}

				int endOfVerbBlock = currStmt;
				for (int i = currStmt; i < nextStmt; i++) {
					if (!(SyntaxHelper.isVerbPhrase(nodes, i) || SyntaxHelper.isVerbBlock(nodes, i, nextStmt))) {
						endOfVerbBlock = i - 1;
						logger.debug("End of verb-block at position: " + endOfVerbBlock);
						break;
					}
					endOfVerbBlock = i;
				}

				int endOfNounBlock = endOfVerbBlock;
				for (int i = endOfNounBlock + 1; i < nextStmt; i++) {
					if (!(SyntaxHelper.isNounPhrase(nodes, i) || SyntaxHelper.isNounBlock(nodes, i, nextStmt))) {
						endOfNounBlock = i - 1;
						logger.debug("End of noun-block at position: " + endOfNounBlock);
						break;
					}
					endOfNounBlock = i;
				}

				// advanced heuristic: ADJP, ADVP, PRP, PP
				int endOfElseBlock = findPP_ADJP_ADVP_PRP(nodes, nextStmt, endOfNounBlock);
				endOfElseBlock = searchEndOfCurrChunk(nodes, nextStmt, endOfElseBlock, endOfElseBlock + 1);

				setCommandType(nodes, keyword.getKeywordBegin(), endOfElseBlock, CommandType.ELSE_STATEMENT, verfiedByDA);
				logger.debug("End of spotted else-clause at position: " + endOfElseBlock + "\n");
			}
		}
	}

	private static int searchNextIfStmt(INode[] nodes, int currStmt) {
		int nextStmt = currStmt;
		for (int i = currStmt; i < nodes.length; i++) {
			if (nodes[i].getAttributeValue("commandType") == null
					|| !nodes[i].getAttributeValue("commandType").equals(CommandType.IF_STATEMENT)) {
				nextStmt++;
			} else {
				return nextStmt;
			}
		}

		return nodes.length;
	}

	/**
	 * This method checks the heuristic of a then-statement for
	 * declaration-wordorder (not the command-wordorder)
	 *
	 * @param nodes
	 *            of the graph
	 * @param currStmt
	 *            is the first node where to check the heuristic. Mostly the one
	 *            after the keyword/first verb
	 * @param nextStmt
	 *            in the input
	 * @return end position of spotted statement
	 */
	private static int checkForAlternativeWordOrder(INode[] nodes, int currStmt, int nextStmt) {
		// NOUN PHRASE _or_ NOUN BLOCK: NP PP NP; NP CC NP;
		int endOfBlock = currStmt;
		if (!SyntaxHelper.isNounPhrase(nodes, currStmt)) {
			return currStmt;
		}

		for (int i = currStmt; i < nextStmt; i++) {
			if (!(SyntaxHelper.isNounPhrase(nodes, i) || SyntaxHelper.isNounBlock(nodes, i, nextStmt))) {
				endOfBlock = i - 1;
				logger.debug("alternative word-order: End of noun-block at position: " + endOfBlock);
				break;
			}
			endOfBlock = i;
		}

		// VERB PHRASE: VP _or_ VERB BLOCK: VP CC VP, VP 'not' VP, VP PP VP
		int nextWord = endOfBlock + 1;
		if (nextWord < nextStmt && !SyntaxHelper.isVerbPhrase(nodes, nextWord)) {
			return currStmt;
		}

		for (int i = nextWord; i < nextStmt; i++) {
			if (!(SyntaxHelper.isVerbPhrase(nodes, i) || SyntaxHelper.isVerbBlock(nodes, i, nextStmt))) {
				endOfBlock = i - 1;
				logger.debug("alternative word-order: End of verb-block at position: " + endOfBlock);
				break;
			}
			endOfBlock = i;
		}

		// NOUN PHRASE: NP _or_ NOUN BLOCK: NP CC NP, NP 'not' NP, NP PP NP
		for (int i = endOfBlock + 1; i < nextStmt; i++) {
			if (!(SyntaxHelper.isNounPhrase(nodes, i) || SyntaxHelper.isNounBlock(nodes, i, nextStmt))) {
				endOfBlock = i - 1;
				logger.debug("alternative word-order: End of noun-block at position: " + endOfBlock);
				break;
			}
			endOfBlock = i;
		}

		// advanced heuristic: ADJP, ADVP, PRP, PP
		endOfBlock = findPP_ADJP_ADVP_PRP(nodes, nextStmt, endOfBlock);

		return endOfBlock;
	}

	private static int findPP_ADJP_ADVP_PRP(INode[] nodes, int nextStmt, int endOfBlock) {
		int nextWord = endOfBlock + 1;

		if (nextWord < nextStmt && SyntaxHelper.isNotPhrase(nodes, nextWord)) {
			endOfBlock += 1;
			nextWord = endOfBlock + 1;
			logger.debug("Found negation.");
		}

		if (nextWord < nextStmt && SyntaxHelper.isAdjectivePhrase(nodes, nextWord)) {
			endOfBlock += 1;
			nextWord = endOfBlock + 1;
			logger.debug("Found adjective.");
			if (nextWord + 1 < nextStmt && SyntaxHelper.isAdjectiveBlock(nodes, nextWord, nextStmt)) {
				endOfBlock += 2;
				nextWord = endOfBlock + 1;
				logger.debug("End of adjective-phrase at position: " + endOfBlock);
			}

		} else if (nextWord < nextStmt && SyntaxHelper.isAdverbPhrase(nodes, nextWord)
				&& !SyntaxHelper.adverbBlacklist.contains(nodes[nextWord].getAttributeValue("value"))) {
			endOfBlock += 1;
			nextWord = endOfBlock + 1;
			logger.debug("Found adverb.");
			if (nextWord + 1 < nextStmt && SyntaxHelper.isAdverbBlock(nodes, nextWord, nextStmt)) {
				endOfBlock += 2;
				nextWord = endOfBlock + 1;
				logger.debug("End of adverb-phrase at position: " + endOfBlock);
			}

		} else if (nextWord < nextStmt && SyntaxHelper.isParticlePhrase(nodes, nextWord)) {
			endOfBlock += 1;
			nextWord = endOfBlock + 1;
			logger.debug("Found particle-phrase.");
		}

		if (SyntaxHelper.isVerbBlock(nodes, nextWord, nextStmt)) {
			endOfBlock += 2;
			logger.debug(
					"Found konjunctive/prepositional verb-phrase with '" + nodes[nextWord].getAttributeValue("value").toString() + "'.");
			nextWord = endOfBlock + 1;
			if (nextWord < nextStmt && SyntaxHelper.isNounPhrase(nodes, nextWord)) {
				endOfBlock += 1;
				nextWord = endOfBlock + 1;
				logger.debug("End of additional noun-block at position: " + endOfBlock);
			}

		} else if (SyntaxHelper.isNounBlock(nodes, nextWord, nextStmt)) {
			endOfBlock += 2;
			logger.debug(
					"Found konjunctive/prepositional noun-phrase with '" + nodes[nextWord].getAttributeValue("value").toString() + "'.");
			int conjunction = nextWord;
			nextWord = endOfBlock + 1;
			endOfBlock = searchEndOfCurrChunk(nodes, nextStmt, endOfBlock, nextWord);
			nextWord = endOfBlock + 1;

			if (nextWord < nextStmt && SyntaxHelper.isNounBlock(nodes, nextWord, nextStmt)) { // "if I leave and the dog in the kitchen..."
				endOfBlock += 2;
				nextWord = endOfBlock + 1;
				endOfBlock = searchEndOfCurrChunk(nodes, nextStmt, endOfBlock, nextWord);
				nextWord = endOfBlock + 1;
				logger.debug("Found additional noun-block.");
			}

			if (nextWord < nextStmt && SyntaxHelper.isVerbPhrase(nodes, nextWord) // "if I leave AND mom says ..." (2 consecutive if-clauses)
					&& nodes[conjunction].getAttributeValue("pos").toString().equalsIgnoreCase("CC")) {
				endOfBlock += 1;
				nextWord = endOfBlock + 1;
				logger.debug("Found additional verb-phrase.");
			}
		}

		return endOfBlock;
	}

	private static int searchEndOfCurrChunk(INode[] nodes, int nextStmt, int endOfIfBlock, int nextWord) {
		for (int i = nextWord; i < nextStmt; i++) {
			if (nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase().startsWith("I-")) {
				endOfIfBlock++;
			} else {
				break;
			}
		}

		return endOfIfBlock;
	}

	private static void setCommandType(INode[] nodes, int begin, int end, CommandType cmd, boolean[] verifiedByDA) {
		String s = "cmdtype: ";
		for (int i = begin; i <= end; i++) {
			ConditionDetector.setNodeAttribute(nodes[i], i, "commandType", cmd, verifiedByDA);
			s = s.concat(
					nodes[i].getAttributeValue("value").toString() + "|" + nodes[i].getAttributeValue("commandType").toString() + ", ");
		}
		logger.debug(s);
	}
}