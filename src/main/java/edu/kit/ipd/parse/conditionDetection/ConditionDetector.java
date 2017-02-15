package edu.kit.ipd.parse.conditionDetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.kohsuke.MetaInfServices;

import edu.kit.ipd.parse.luna.agent.AbstractAgent;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.tools.ConfigManager;

/**
 * This is the 'main'-class which starts the condition-detection-algorithm. It
 * uses the configsetting which are provided in the configfile, searches for
 * conditional statements and saves the results in the graph by transforming it.
 *
 * @author Vanessa Steurer
 */
@MetaInfServices(AbstractAgent.class)
public class ConditionDetector extends AbstractAgent {
	public static final String CONDITION_NUMBER = "conditionNumber";
	public static final String ID = "condition_detector";
	private Synonyms synonyms;
	public static boolean useCoreference;
	public static boolean compensateSNLP;
	public static boolean showDoubtfulResults;
	public static boolean firstRun;

	@Override
	public void init() {
		firstRun = true;
		synonyms = new Synonyms();
		setId(ID);
	}

	@Override
	public void exec() {
		// Load synonyms and config-data
		synonyms.importSynonyms();
		setConfigs();

		// Readout nodes of the graph and add a commandType attribute to each of them
		INode[] nodes = toArrayKeepReference();
		for (INode node : nodes) {
			if (firstRun) {
				node.getType().addAttributeToType("String", "commandType");
				node.getType().addAttributeToType("int", CONDITION_NUMBER);
			}
			node.setAttributeValue("commandType", null);
			node.setAttributeValue(CONDITION_NUMBER, -1);
		}

		// Look for keywords and check heuristics
		List<ConditionContainer> spottedConditions = lookForConditionalClauses(nodes);

		// Transform the graph on the basis of the found commandTypes
		StatementExtractor.transformGraph(graph, nodes, spottedConditions);

		// Output of the conditionDetection
		toString(nodes);

		firstRun = false;
	}

	/**
	 * This method executes the pattern recognition of the conditional clauses.
	 *
	 * @param nodes
	 *            containing the input words
	 * @return condition spotted in the input
	 */
	private List<ConditionContainer> lookForConditionalClauses(INode[] nodes) {
		// If-Statement (Bedingung)
		List<Keyword> ifHints = KeywordScanner.searchIfKeywords(synonyms, nodes);
		HeuristicCheck.checkForIfClause(nodes, ifHints);

		// Then-Statement (Folgeanweisung)
		List<Keyword> thenHints = KeywordScanner.searchThenKeywords(synonyms, nodes, ifHints);
		HeuristicCheck.checkForThenClause(nodes, thenHints);
		List<ConditionContainer> spottedConditions = StatementExtractor.concatIfWithThen(nodes, ifHints, thenHints);

		// Else-Statement (Alternativ-Anweisung)
		List<Keyword> elseHints = KeywordScanner.searchElseKeywords(synonyms, nodes, ifHints);
		HeuristicCheck.checkForElseClause(nodes, elseHints);
		spottedConditions = StatementExtractor.concatThenWithElse(nodes, spottedConditions, thenHints, elseHints);

		return spottedConditions;
	}

	/**
	 * Prints a formatted output of the results from the pattern recognition of
	 * method lookForConditionalClauses().
	 *
	 * @param nodes
	 *            containing the input words
	 */
	private void toString(INode[] nodes) {
		String s = "\n output = ";
		for (int i = 0; i < nodes.length; i++) {
			s = s.concat(nodes[i].getAttributeValue("value").toString() + "|" + nodes[i].getAttributeValue("commandType").toString() + " ");
		}
		System.out.println(s + "\n");
	}

	/**
	 * This method loads the configparameters.
	 */
	public void setConfigs() {
		Properties props = ConfigManager.getConfiguration(ConditionDetector.class);
		useCoreference = props.getProperty("useCoreference").equalsIgnoreCase("TRUE");
		compensateSNLP = props.getProperty("compensateSNLP").equalsIgnoreCase("TRUE");
		showDoubtfulResults = props.getProperty("showDoubtfulResults").equalsIgnoreCase("TRUE");
	}

	/**
	 * This method stores the nodes with type "token" in an array and returns
	 * it.
	 *
	 * This project only uses the wordnodes (type = token) of the graph. It is
	 * given, that these wordnodes are saved consecutive in LinkedHashSet, each
	 * connected with an arc of type "relation". The returned array is no
	 * deepcopy. It saves the references from each node in graph. By every
	 * execution if this project, the array of this method has to be updated.
	 *
	 * @return nodesArray used in this project
	 */
	private INode[] toArrayKeepReference() {
		Set<? extends INode> nodesSet = graph.getNodes();
		List<INode> wordNodesList = new ArrayList<INode>();

		for (INode node : nodesSet) { // Find rootnode of the graph
			if (node.getType().getName().equalsIgnoreCase("token") && (int) node.getAttributeValue("position") == 0) {
				wordNodesList.add(node);
				break;
			}
		}

		if (wordNodesList.isEmpty()) {
			System.out.println("Cannot find root-node in graph.");
		}

		for (int i = 0; i < wordNodesList.size(); i++) { // Only save the wordnodes ("token") for conditionDetection
			INode currNode = wordNodesList.get(i); // Run trough the graph by starting at the rootnode and going over the arcs to the next node
			for (IArc arc : currNode.getOutgoingArcs()) {
				if (arc.getType().getName().equalsIgnoreCase("relation")) {
					currNode = arc.getTargetNode();
					wordNodesList.add(currNode);
				}
			}
		}

		INode[] nodesArray = new INode[wordNodesList.size()];
		for (int i = 0; i < wordNodesList.size(); i++) { // Create an array with the persisting references on the graph
			nodesArray[i] = wordNodesList.get(i);
		}

		return nodesArray;
	}

}
