package edu.kit.ipd.parse.conditionDetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.agent.AbstractAgent;
import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IArcType;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseGraph;
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
	private static final String CONDITION_TYPE_ATTRIBUTE = "commandType";
	private static final String VERIFIED_BY_DA_SUFFIX = "Verified";
	public static final String ID = "condition_detector";
	private Synonyms synonyms;
	public static boolean useCoreference;
	public static boolean compensateSNLP;
	public static boolean showDoubtfulResults;
	public static boolean firstRun;

	private static final Logger logger = LoggerFactory.getLogger(ConditionDetector.class);

	public ConditionDetector() {
		setId(ID);
	}

	@Override
	public void init() {
		firstRun = true;
		synonyms = new Synonyms();
	}

	@Override
	public void exec() {
		// Load synonyms and config-data
		synonyms.importSynonyms();
		setConfigs();

		// Readout nodes of the graph and add a commandType attribute to each of them
		INode[] nodes = null;
		try {
			nodes = toArrayKeepReference(graph);
		} catch (MissingDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean[] verifiedConditions = new boolean[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			INode node = nodes[i];
			if (!node.getType().containsAttribute(CONDITION_TYPE_ATTRIBUTE, "String")) {
				node.getType().addAttributeToType("String", CONDITION_TYPE_ATTRIBUTE);
			}
			if (!node.getType().containsAttribute(CONDITION_NUMBER, "int")) {
				node.getType().addAttributeToType("int", CONDITION_NUMBER);
			}
			if (node.getType().containsAttribute(CONDITION_TYPE_ATTRIBUTE + VERIFIED_BY_DA_SUFFIX, "boolean")) {
				if (node.getAttributeValue(CONDITION_TYPE_ATTRIBUTE + VERIFIED_BY_DA_SUFFIX) != null
						&& (boolean) node.getAttributeValue(CONDITION_TYPE_ATTRIBUTE + VERIFIED_BY_DA_SUFFIX)) {
					verifiedConditions[i] = true;
				}
			} else {
				node.setAttributeValue("commandType", null);
				node.setAttributeValue(CONDITION_NUMBER, -1);
			}
		}

		// Look for keywords and check heuristics
		List<ConditionContainer> spottedConditions = lookForConditionalClauses(nodes, graph, verifiedConditions);

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
	 * @param verifiedConditions
	 * @param verifiedConditions
	 * @return condition spotted in the input
	 */
	private List<ConditionContainer> lookForConditionalClauses(INode[] nodes, IGraph graph, boolean[] verifiedConditions) {
		// If-Statement (Bedingung)
		List<Keyword> ifHints = KeywordScanner.searchIfKeywords(synonyms, nodes);
		HeuristicCheck.checkForIfClause(nodes, ifHints, verifiedConditions);

		// Then-Statement (Folgeanweisung)
		List<Keyword> thenHints = KeywordScanner.searchThenKeywords(synonyms, nodes, ifHints);
		HeuristicCheck.checkForThenClause(nodes, thenHints, verifiedConditions);
		List<ConditionContainer> spottedConditions = StatementExtractor.concatIfWithThen(nodes, ifHints, thenHints, verifiedConditions);

		// Else-Statement (Alternativ-Anweisung)
		List<Keyword> elseHints = KeywordScanner.searchElseKeywords(synonyms, nodes, ifHints);
		HeuristicCheck.checkForElseClause(nodes, elseHints, verifiedConditions);
		spottedConditions = StatementExtractor.concatThenWithElse(nodes, spottedConditions, thenHints, elseHints, graph,
				verifiedConditions);

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
	 * This method stores the nodes with type "token" in an array and returns it.
	 *
	 * This project only uses the wordnodes (type = token) of the graph. It is
	 * given, that these wordnodes are saved consecutive in LinkedHashSet, each
	 * connected with an arc of type "relation". The returned array is no deepcopy.
	 * It saves the references from each node in graph. By every execution if this
	 * project, the array of this method has to be updated.
	 *
	 * @return nodesArray used in this project
	 */

	public static final INode[] toArrayKeepReference(IGraph graph) throws MissingDataException {
		List<INode> wordNodesList = new ArrayList<INode>();
		IArcType nextArcType;
		if ((nextArcType = graph.getArcType("relation")) != null) {
			if (graph instanceof ParseGraph) {
				ParseGraph pGraph = (ParseGraph) graph;
				INode current = pGraph.getFirstUtteranceNode();
				List<? extends IArc> outgoingNextArcs = current.getOutgoingArcsOfType(nextArcType);
				boolean hasNext = !outgoingNextArcs.isEmpty();
				wordNodesList.add(current);
				while (hasNext) {
					//assume that only one NEXT arc exists
					if (outgoingNextArcs.size() == 1) {
						current = outgoingNextArcs.toArray(new IArc[outgoingNextArcs.size()])[0].getTargetNode();
						wordNodesList.add(current);
						outgoingNextArcs = current.getOutgoingArcsOfType(nextArcType);
						hasNext = !outgoingNextArcs.isEmpty();
					} else {
						logger.error("Nodes have more than one NEXT Arc");
						throw new IllegalArgumentException("Nodes have more than one NEXT Arc");
					}
				}
			} else {
				logger.error("Graph is no ParseGraph!");
				throw new MissingDataException("Graph is no ParseGraph!");
			}
		} else {
			logger.error("Next Arctype does not exist!");
			throw new MissingDataException("Next Arctype does not exist!");
		}
		return wordNodesList.toArray(new INode[wordNodesList.size()]);
	}

	static void setNodeAttribute(INode node, int nodeIndex, String attrName, CommandType statement, boolean[] verifiedByDA) {
		if (!verifiedByDA[nodeIndex]) {
			node.setAttributeValue(attrName, statement);
		}
	}

}
