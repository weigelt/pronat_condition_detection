package edu.kit.ipd.parse.conditionDetection;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IArcType;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;

/**
 * This class concadinates the spotted statements and fills the ContainerClasses
 * with their belonging nodes.
 *
 * @author Vanessa Steurer
 */
public class StatementExtractor {
	private static final String COMMANDTYPELOCATION_ATTRIBUTE = "commandTypeLocation";
	private static final String COMMANDTYPE_ATTRIBUTE = "commandType";
	private static final String STATEMENT_ARC_TYPE = "statement";
	private static final Logger logger = LoggerFactory.getLogger(StatementExtractor.class);

	private StatementExtractor() {

	}

	public static List<ConditionContainer> concatIfWithThen(INode[] nodes, List<Keyword> ifHints, List<Keyword> thenHints) {
		setCmdtypeIfNoStmtFound(nodes, ifHints);
		List<ConditionContainer> spottedConditions = new ArrayList<ConditionContainer>();

		for (int i = 0; i < ifHints.size(); i++) {
			spottedConditions.add(new ConditionContainer(null, null, null));
			spottedConditions.get(i).setIfStmt(new IfStatement(new ArrayList<INode>()));
			int currStmtPos = ifHints.get(i).getKeywordBegin();
			int nextStmt;
			if (i + 1 < ifHints.size()) {
				nextStmt = ifHints.get(i + 1).getKeywordBegin();
			} else {
				nextStmt = nodes.length;
			}
			logger.info("Concat if with then block for detected clause number " + (i + 1) + ".");

			while (nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) != null // Skip If-Statement
					&& nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE).equals(CommandType.IF_STATEMENT)) {
				spottedConditions.get(i).getIfStmt().addNodeToNodeList(nodes[currStmtPos]);
				currStmtPos++;
				if (currStmtPos >= nodes.length) {
					currStmtPos--;
					break;
				}
			}
			int endOfIfStmt = currStmtPos;

			//TODO: here is a sloppy BUGFIX: 'thenHints.isEmpty() || thenHints.size() < i + 1'
			//sometimes the List "thenHints" is not as large as "ifHints"
			//in this case "thenHints.get(i)" returns an out of bonds as we iterate over ifHints
			//to fix that, we just return the so far spotted condition
			//BUT: if this case occurs, we have an if without a then
			// Maybe we want to spot the condition anyway and leave it to the dialog agent to ask for the 'then'
			if (thenHints.isEmpty() || thenHints.size() < i + 1) {
				return spottedConditions;
			}

			// IF + INDP
			spottedConditions.get(i).setThenStmt(new ThenStatement(new ArrayList<INode>()));
			if (thenHints.get(i).isDummy()) {
				checkForFalseThenKeyword(nodes, spottedConditions, thenHints, i, endOfIfStmt, nextStmt); // updates spottedConditions

				while (currStmtPos < nodes.length && nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) == null) {
					nodes[currStmtPos].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.INDEPENDENT_STATEMENT);
					spottedConditions.get(i).getThenStmt().addNodeToNodeList(nodes[currStmtPos]);
					spottedConditions.get(i).getThenStmt().setINDP();
					currStmtPos++;
				}
				if (currStmtPos > endOfIfStmt) {
					logger.debug(
							"Found section with missing commandtype. Set INDP-Stmt from " + endOfIfStmt + " to " + (currStmtPos - 1) + ".");
				}

				// IF + THEN
			} else {
				while (currStmtPos < nodes.length && nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) == null) {
					nodes[currStmtPos].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.IF_STATEMENT);
					spottedConditions.get(i).getIfStmt().addNodeToNodeList(nodes[currStmtPos]);
					currStmtPos++;
				}
				if (currStmtPos > endOfIfStmt) {
					logger.debug(
							"Found section with missing commandtype. Set IF-Stmt from " + endOfIfStmt + " to " + (currStmtPos - 1) + ".");
				}
			}
		}

		return spottedConditions;
	}

	public static List<ConditionContainer> concatThenWithElse(INode[] nodes, List<ConditionContainer> spottedConditions,
			List<Keyword> thenHints, List<Keyword> elseHints) {
		for (int i = 0; i < thenHints.size(); i++) {
			int currStmtPos = thenHints.get(i).getKeywordBegin();
			int endOfStmt;
			logger.info("Concat then with else block for detected clause number " + (i + 1) + ".");

			if (thenHints.get(i).isDummy()) { // Skip INDP-Stmt
				currStmtPos = skipThenOrIndpStmt(nodes, spottedConditions, i, currStmtPos, CommandType.INDEPENDENT_STATEMENT);
				spottedConditions.get(i).getThenStmt().setINDP();
			} else { // Skip THEN-Stmt
				currStmtPos = skipThenOrIndpStmt(nodes, spottedConditions, i, currStmtPos, CommandType.THEN_STATEMENT);
			}
			endOfStmt = currStmtPos;

			if (elseHints.get(i).isDummy()) {
				// IF + INDP, no ELSE
				if (thenHints.get(i).isDummy()) {
					while (currStmtPos < nodes.length && nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) == null) {
						nodes[currStmtPos].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.INDEPENDENT_STATEMENT);
						spottedConditions.get(i).getThenStmt().addNodeToNodeList(nodes[currStmtPos]);
						spottedConditions.get(i).getThenStmt().setINDP();
						currStmtPos++;
					}
					if (currStmtPos > endOfStmt) {
						logger.debug("Found section with missing commandtype. Set INDP-Stmt from " + endOfStmt + " to " + (currStmtPos - 1)
								+ ".\n");
					}

					// IF + THEN, no ELSE
				} else {
					if (ConditionDetector.useCoreference) {
						int corefSearchBegin = currStmtPos;
						currStmtPos = checkForCoreference(nodes, spottedConditions, i, corefSearchBegin);

						if (currStmtPos > corefSearchBegin) {
							for (int j = corefSearchBegin; j <= currStmtPos; j++) {
								nodes[j].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.THEN_STATEMENT);
								spottedConditions.get(i).getThenStmt().addNodeToNodeList(nodes[j]);
							}
							currStmtPos++;
						}
					}

					while (currStmtPos < nodes.length && nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) == null) {
						nodes[currStmtPos].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.INDEPENDENT_STATEMENT);
						currStmtPos++;
					}
				}

			} else {
				// IF + INDP + ELSE
				if (thenHints.get(i).isDummy()) {
					while (currStmtPos < nodes.length && nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) == null) {
						nodes[currStmtPos].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.INDEPENDENT_STATEMENT);
						spottedConditions.get(i).getThenStmt().addNodeToNodeList(nodes[currStmtPos]);
						spottedConditions.get(i).getThenStmt().setINDP();
						currStmtPos++;
					}
					if (currStmtPos > endOfStmt) {
						logger.debug("Found section with missing commandtype. Set INDP-Stmt from " + endOfStmt + " to " + (currStmtPos - 1)
								+ ".\n");
					}

					// IF + THEN + ELSE
				} else {
					while (currStmtPos < nodes.length && nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) == null) {
						nodes[currStmtPos].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.THEN_STATEMENT);
						spottedConditions.get(i).getThenStmt().addNodeToNodeList(nodes[currStmtPos]);
						currStmtPos++;
					}
					if (currStmtPos > endOfStmt) {
						logger.debug("Found section with missing commandtype. Set THEN-Stmt from " + endOfStmt + " to " + (currStmtPos - 1)
								+ ".\n");
					}
				}
				determineEndOfElse(nodes, spottedConditions, elseHints); // updates spottedConditions
			}

			spottedConditions.get(i).setConditionBegin();
			spottedConditions.get(i).setConditionEnd();
		}

		return spottedConditions;
	}

	private static int skipThenOrIndpStmt(INode[] nodes, List<ConditionContainer> spottedConditions, int i, int currStmt,
			CommandType cmdtype) {
		while (nodes[currStmt].getAttributeValue(COMMANDTYPE_ATTRIBUTE) != null // Skip IF,THEN or INDP-Statement
				&& nodes[currStmt].getAttributeValue(COMMANDTYPE_ATTRIBUTE).equals(cmdtype)) {
			spottedConditions.get(i).getThenStmt().addNodeToNodeList(nodes[currStmt]);
			currStmt++;
			if (currStmt >= nodes.length) {
				currStmt--;
				break;
			}
		}
		return currStmt;
	}

	/**
	 * This method looks again for then-Statements, considering the
	 * null-sections after the if-Statement.
	 *
	 * First check: The heuristic could have been failed and settet INDP,
	 * because the chosen keyword is invalid. Therefore this method checks, if
	 * the vP+NP heuristic passes, when it starts checking after directly after
	 * if clause.
	 *
	 * @param nodes
	 *            of the graph
	 * @param spottedConditions
	 *            in the input text
	 * @param thenHints
	 *            in the input text
	 * @param i
	 *            : number of the spotted conditions
	 * @param currStmtPos
	 *            : first word after if-statement
	 */
	private static void checkForFalseThenKeyword(INode[] nodes, List<ConditionContainer> spottedConditions, List<Keyword> thenHints, int i,
			int endOfIfStmt, int nextStmt) { // If then-keyword has been chosen wrongly, check then-heuristic VP + NP
		int lookForVerb = endOfIfStmt;
		boolean foundVerb = false; // If no verbs are found up to the next statement, break

		for (int j = endOfIfStmt; j < nextStmt; j++) {
			if (SyntaxHelper.isVerbPhrase(nodes, j)) {
				lookForVerb = j;
				foundVerb = true;
				break;
			}
		}

		if (!foundVerb) {
			return;
		}

		//verb phrase: VP _or_ verb block: VP CC VP, VP 'not' VP, VP PP VP
		int endOfThen = lookForVerb;
		for (int k = lookForVerb + 1; k < nextStmt; k++) {
			if (!(SyntaxHelper.isVerbPhrase(nodes, k) || SyntaxHelper.isVerbBlock(nodes, k, nextStmt))) {
				endOfThen = k - 1;
				logger.debug("End of verb-block at position: " + endOfThen);
				break;
			}
			endOfThen = k;
		}

		//noun phrase: NP _or_ noun block: NP NP; NP PP NP; NP CC NP;
		if (endOfThen + 1 < nextStmt
				&& (SyntaxHelper.isNounPhrase(nodes, endOfThen + 1) || SyntaxHelper.isNounBlock(nodes, endOfThen + 1, nextStmt)
						|| SyntaxHelper.isAdverbPhrase(nodes, endOfThen + 1) || SyntaxHelper.isParticlePhrase(nodes, endOfThen + 1))) {
			endOfThen += 1;
			endOfThen = searchEndOfCurrChunk(nodes, nextStmt, endOfThen + 1, endOfThen);
			logger.debug("End of noun-block at position: " + endOfThen);
		} else {
			endOfThen = endOfIfStmt;
		}

		// Set nodes with cmdtype=null BEFORE this block to IF_STMT zu, overwrite nodes with cmdtype=INDP to cmdtype=THEN
		for (int k = endOfIfStmt; k < lookForVerb; k++) {
			nodes[k].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.IF_STATEMENT);
			spottedConditions.get(i).getIfStmt().addNodeToNodeList(nodes[k]);
		}

		for (int k = lookForVerb; k < endOfThen; k++) {
			nodes[k].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.THEN_STATEMENT);
		}
		int indp = lookForVerb;
		while (nodes[indp].getAttributeValue(COMMANDTYPE_ATTRIBUTE) != null
				&& nodes[indp].getAttributeValue(COMMANDTYPE_ATTRIBUTE).equals(CommandType.INDEPENDENT_STATEMENT)) {
			nodes[indp].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.THEN_STATEMENT);
			indp++;
			if (indp >= nodes.length) {
				indp--;
				break;
			}
		}
		thenHints.get(i).setKeyword(nodes[lookForVerb].getAttributeValue("value").toString());
		thenHints.get(i).setKeywordBegin(lookForVerb);
		thenHints.get(i).setKeywordEnd(lookForVerb);
		logger.debug("Found then-clause by not considering a spotted keyword from position" + lookForVerb + " to " + endOfThen + ".\n");
	}

	private static void determineEndOfElse(INode[] nodes, List<ConditionContainer> spottedConditions, List<Keyword> elseHints) {
		for (int i = 0; i < elseHints.size(); i++) {
			if (!elseHints.get(i).isDummy()) {
				spottedConditions.get(i).setElseStmt(new ElseStatement(new ArrayList<INode>()));
				int currStmtPos = elseHints.get(i).getKeywordBegin();

				while (nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) != null // Skip ELSE-Statement
						&& nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE).equals(CommandType.ELSE_STATEMENT)) {
					spottedConditions.get(i).getElseStmt().addNodeToNodeList(nodes[currStmtPos]);
					currStmtPos++;
					if (currStmtPos >= nodes.length) {
						currStmtPos--;
						break;
					}
				}

				if (ConditionDetector.useCoreference) {
					int corefSearchBegin = currStmtPos;
					currStmtPos = checkForCoreference(nodes, spottedConditions, i, currStmtPos);

					if (currStmtPos > corefSearchBegin) {
						for (int j = corefSearchBegin; j <= currStmtPos; j++) {
							nodes[j].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.ELSE_STATEMENT);
							spottedConditions.get(i).getElseStmt().addNodeToNodeList(nodes[j]);
						}
						currStmtPos++;
					}
				}

				while (currStmtPos < nodes.length && nodes[currStmtPos].getAttributeValue(COMMANDTYPE_ATTRIBUTE) == null) {
					nodes[currStmtPos].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.INDEPENDENT_STATEMENT);
					currStmtPos++;
				}
			}
		}
	}

	private static int checkForCoreference(INode[] nodes, List<ConditionContainer> spottedConditions, int i, int corefSearchIndex) {
		int nextStmt = corefSearchIndex;
		boolean foundCoref = false;

		while (nextStmt < nodes.length && (nodes[nextStmt].getAttributeValue(COMMANDTYPE_ATTRIBUTE) == null // Look for next statement
				|| nodes[nextStmt].getAttributeValue(COMMANDTYPE_ATTRIBUTE).equals(CommandType.INDEPENDENT_STATEMENT))) {
			nextStmt++;
		}

		for (int j = corefSearchIndex; j < nextStmt; j++) {
			for (IArc arc : nodes[j].getOutgoingArcs()) {
				if (arc.getType().getName().equalsIgnoreCase("coref")) { // Coref-arc to another part of the THEN- or ELSE-stmt
					INode target = arc.getTargetNode();
					if (spottedConditions.get(i).getThenStmt().getNodeList().contains(target) || spottedConditions.get(i).hasElseStmt()
							&& spottedConditions.get(i).getElseStmt().getNodeList().contains(target)) {
						corefSearchIndex = j;
						foundCoref = true;
						logger.debug("Used coreference to spot the end of then-clause at position: " + corefSearchIndex + "\n");
					}
				}
			}
		}

		if (foundCoref) {
			int endOfBlock = searchEndOfCurrChunk(nodes, nextStmt, corefSearchIndex + 1, corefSearchIndex);
			int nextWord = endOfBlock + 1;
			if (nextWord < nextStmt && SyntaxHelper.isNounBlock(nodes, nextWord, nextStmt)) {
				logger.debug("Found konjunctive/prepositional noun-phrase with '" + nodes[endOfBlock].getAttributeValue("value").toString()
						+ "'.");
				endOfBlock += 2;
				nextWord = endOfBlock + 1;

			} else if (nextWord < nextStmt && SyntaxHelper.isVerbBlock(nodes, nextWord, nextStmt)) {
				logger.debug("Found konjunctive/prepositional noun-phrase with '" + nodes[endOfBlock].getAttributeValue("value").toString()
						+ "'.");
				endOfBlock += 2;
				nextWord = endOfBlock + 1;
			}
			endOfBlock = searchEndOfCurrChunk(nodes, nextStmt, nextWord, endOfBlock);

			return endOfBlock;
		}

		return corefSearchIndex;
	}

	private static int searchEndOfCurrChunk(INode[] nodes, int nextStmt, int nextWord, int endOfBlock) {
		for (int i = nextWord; i < nextStmt; i++) {
			if (nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase().startsWith("I-")) {
				endOfBlock++;
			} else {
				break;
			}
		}

		return endOfBlock;
	}

	/**
	 * If no statements are found in the input text, the cmdtype-attribute of
	 * every node has to be set to INDP-Stmt. Otherwise sets INDP-Stmt to nodes
	 * till first spotted-If-Stmt/Keyword.
	 *
	 * @param nodes
	 *            of the graph
	 * @param ifHints
	 *            which are spotted in the graph
	 */
	private static void setCmdtypeIfNoStmtFound(INode[] nodes, List<Keyword> ifHints) {
		if (ifHints.isEmpty()) { // If no if-clauses found set cmdtype=INDP to all nodes of the input
			for (int i = 0; i < nodes.length; i++) {
				nodes[i].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.INDEPENDENT_STATEMENT);
			}
			logger.debug("Set commandtype INDP-Statement to complete text.");

		} else { // If there are nodes before the first if-stmt with cmdtype=null, set cmdtype=INDP
			int firstIfStmt = ifHints.get(0).getKeywordBegin();
			for (int i = 0; i < firstIfStmt; i++) {
				nodes[i].setAttributeValue(COMMANDTYPE_ATTRIBUTE, CommandType.INDEPENDENT_STATEMENT);
			}
		}
	}

	/**
	 * Connects the modified nodes with the new arctype "statement" according to
	 * the spotted conditions.
	 *
	 * Each arc holds two attributes: first attribute: commandType. Gives the
	 * cmdtype of the node which the arcs points to second abttribute:
	 * commandTypeLocation. Gives the location of the node in the
	 * conditionStatement (begin, mid, end).
	 *
	 * @param graph
	 *            which is given, results has to be saved in it
	 * @param nodes
	 *            of the graph
	 * @param spottedConditions
	 *            of the input text
	 */
	public static void transformGraph(IGraph graph, INode[] nodes, List<ConditionContainer> spottedConditions) {
		IArcType arcType;
		if (ConditionDetector.firstRun) { // Add arctype "statement"
			if (graph.hasArcType(STATEMENT_ARC_TYPE)) {
				arcType = graph.getArcType(STATEMENT_ARC_TYPE);
			} else {
				arcType = graph.createArcType(STATEMENT_ARC_TYPE);
			}
			arcType.addAttributeToType("String", COMMANDTYPE_ATTRIBUTE);
			arcType.addAttributeToType("String", COMMANDTYPELOCATION_ATTRIBUTE);
			logger.info("Transform graph according to the spotted conditions.");

		} else { // Add arctype "statement". Delete arcs of conditionDetection-run before!!
			List<IArc> arcsToDelete = new ArrayList<IArc>();
			for (IArc arc : graph.getArcs()) {
				if (arc.getType().getName().equalsIgnoreCase(STATEMENT_ARC_TYPE)) {
					arcsToDelete.add(arc);
				}
			}
			for (int i = 0; i < arcsToDelete.size(); i++) {
				graph.deleteArc(arcsToDelete.get(i));
			}
			if (graph.hasArcType(STATEMENT_ARC_TYPE)) {
				arcType = graph.getArcType(STATEMENT_ARC_TYPE);
			} else {
				arcType = graph.createArcType(STATEMENT_ARC_TYPE);
			}
			arcType.addAttributeToType("String", COMMANDTYPE_ATTRIBUTE);
			arcType.addAttributeToType("String", COMMANDTYPELOCATION_ATTRIBUTE);
			logger.info("Transform graph according to the spotted conditions.");
		}

		if (spottedConditions.isEmpty()) { // Add INDP-arcs, if no if-clauses found
			createStmtArcs(graph, nodes, arcType, 0, nodes.length - 1, "indp");
			logger.debug("Set commandtype of arcs and nodes to INDP");
			return;
		}

		int endCondBefore = 0;
		for (int i = 0; i < spottedConditions.size(); i++) {
			if (i != 0) {
				endCondBefore = spottedConditions.get(i - 1).getConditionEnd() + 1;
			}
			ConditionContainer condition = spottedConditions.get(i);

			int indpEnd = condition.getIfStmt().getBegin() - 1; // Add arcs between INDP-Stmt and before if-clause, if existent
			int ifBegin = condition.getIfStmt().getBegin();
			if (indpEnd > 0) { // Add arc from INDP to IF
				createStmtArcs(graph, nodes, arcType, endCondBefore, indpEnd, "indp");
				IArc arcToIf = graph.createArc(nodes[indpEnd], nodes[ifBegin], arcType);
				createStmtConnectionArcs(arcToIf, "if");
			}
			int ifEnd = condition.getIfStmt().getEnd();
			createStmtArcs(graph, nodes, arcType, ifBegin, ifEnd, "if"); // Add arcs between IF-Stmts
			setConditionNumber(condition, i);

			if (condition.hasThenStmt()) { // If THEN exists, add arcs
				int thenBegin = condition.getThenStmt().getBegin();

				IArc arcIfToThen = graph.createArc(nodes[ifEnd], nodes[thenBegin], arcType);
				if (!ConditionDetector.showDoubtfulResults && condition.getThenStmt().isINDP()) {
					createStmtConnectionArcs(arcIfToThen, "indp");
				} else {
					createStmtConnectionArcs(arcIfToThen, "then"); // Add arc from IF to THEN
				}

				int thenEnd = condition.getThenStmt().getEnd(); // Add arcs between THEN-Stmts
				if (!ConditionDetector.showDoubtfulResults && condition.getThenStmt().isINDP()) {
					createStmtArcs(graph, nodes, arcType, thenBegin, thenEnd, "indp");
				} else {
					createStmtArcs(graph, nodes, arcType, thenBegin, thenEnd, "then");
				}

				if (condition.hasElseStmt()) { // If ELSE exists, add arcs
					int elseBegin = condition.getElseStmt().getBegin();
					IArc arcIfToElse = graph.createArc(nodes[ifEnd], nodes[elseBegin], arcType);
					createStmtConnectionArcs(arcIfToElse, "else"); // Add arc between IF to ELSE
					int elseEnd = condition.getElseStmt().getEnd(); // Add arcs between ELSE-Stmts
					createStmtArcs(graph, nodes, arcType, elseBegin, elseEnd, "else");

					int indpBegin = condition.getElseStmt().getEnd() + 1;
					if (indpBegin < nodes.length) {
						IArc arcThenToAfterStmt = graph.createArc(nodes[thenEnd], nodes[indpBegin], arcType);
						createStmtConnectionArcs(arcThenToAfterStmt, "indp");// Add arcs from THEN to INDP (skips else)
						IArc arcElseToAfterStmt = graph.createArc(nodes[elseEnd], nodes[indpBegin], arcType);
						createStmtConnectionArcs(arcElseToAfterStmt, "indp");// Add arcs from ELSE to INDP
					}

				} else { // If no else exists, add arcs between THEN to INDP
					int indpBegin = condition.getThenStmt().getEnd() + 1;
					if (indpBegin < nodes.length) {
						IArc arcThenToAfterStmt = graph.createArc(nodes[thenEnd], nodes[indpBegin], arcType);
						createStmtConnectionArcs(arcThenToAfterStmt, "indp");
					}
				}

			} else { // If no THEN & ELSE exists, add arcs between IF to INDP
				int indpBegin = ifEnd + 1;
				if (indpBegin < nodes.length) {
					IArc arcIfToIndp = graph.createArc(nodes[ifEnd], nodes[indpBegin], arcType);
					createStmtConnectionArcs(arcIfToIndp, "indp");
				}
			}

			if (i + 1 == spottedConditions.size()) {
				createStmtArcs(graph, nodes, arcType, condition.getConditionEnd() + 1, nodes.length - 1, "indp");
			}
		}
		logger.debug("Set commandtype of arcs and nodes to the commandtype of the spotted statement.");
	}

	/**
	 * Sets conditionNumber for the specified condition
	 *
	 * @author Tobias Hey
	 *
	 * @param condition
	 *            The condition to set the number for
	 * @param conditionNumber
	 *            The number to set
	 */
	private static void setConditionNumber(ConditionContainer condition, int conditionNumber) {
		for (INode node : condition.getIfStmt().getNodeList()) {
			node.setAttributeValue(ConditionDetector.CONDITION_NUMBER, conditionNumber);
		}
		if (condition.hasThenStmt()) {
			for (INode node : condition.getThenStmt().getNodeList()) {
				node.setAttributeValue(ConditionDetector.CONDITION_NUMBER, conditionNumber);
			}
		}
		if (condition.hasElseStmt()) {
			for (INode node : condition.getElseStmt().getNodeList()) {
				node.setAttributeValue(ConditionDetector.CONDITION_NUMBER, conditionNumber);
			}
		}

	}

	private static void createStmtArcs(IGraph graph, INode[] nodes, IArcType arcType, int beginStmt, int endStmt, String stmt) {

		for (int j = beginStmt; j < endStmt; j++) {
			IArc arc = graph.createArc(nodes[j], nodes[j + 1], arcType);
			arc.setAttributeValue(COMMANDTYPE_ATTRIBUTE, stmt);

			if (j + 1 != endStmt) {
				arc.setAttributeValue(COMMANDTYPELOCATION_ATTRIBUTE, "mid");
			} else {
				arc.setAttributeValue(COMMANDTYPELOCATION_ATTRIBUTE, "end");
			}
		}
	}

	private static void createStmtConnectionArcs(IArc arc, String stmt) {
		arc.setAttributeValue(COMMANDTYPE_ATTRIBUTE, stmt);
		arc.setAttributeValue(COMMANDTYPELOCATION_ATTRIBUTE, "begin");
	}

}
