package edu.kit.ipd.parse.conditionDetection;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseNode;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;

public class HeuristicCheckSNLP_THEN {
	private ShallowNLP snlp;
	private IGraph graph;
	private Token[] actual;

	@BeforeClass
	public static void init() {
		ConditionDetector cd = new ConditionDetector();
		cd.init();
		cd.setConfigs();
	}

	@Test
	public void indpStatement() {
		String input = "If it barks loud desk then desk";
		createGraph(input);
		List<Keyword> thenHints = new ArrayList<Keyword>();
		thenHints.add(new Keyword(CommandType.THEN_STATEMENT, "then", 5, 5));
		HeuristicCheck.checkForThenClause(graph.getNodes().toArray(new ParseNode[0]), thenHints);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 5; i < nodes.length; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.INDEPENDENT_STATEMENT));
		}
	}

	@Test
	public void searchFirstVerb() {
		String input = "If you closed the door and wiped the floor go";
		createGraph(input);
		List<Keyword> thenHints = new ArrayList<Keyword>();
		thenHints.add(new Keyword(null, "DUMMY_KEYWORD", 9, 9));
		HeuristicCheck.checkForThenClause(graph.getNodes().toArray(new ParseNode[0]), thenHints);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		assertTrue(nodes[9].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
	}

	@Test
	public void verbConjunction() {
		String input = "then wash the dishes and bring the lunch";
		createGraph(input);
		List<Keyword> thenHints = new ArrayList<Keyword>();
		thenHints.add(new Keyword(CommandType.THEN_STATEMENT, "then", 0, 0));
		HeuristicCheck.checkForThenClause(graph.getNodes().toArray(new ParseNode[0]), thenHints);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 0; i < 5; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
		}
	}

	@Test
	public void verbParticlePhrase() {
		String input = "if the door is open come over here";
		createGraph(input);
		List<Keyword> thenHints = new ArrayList<Keyword>();
		thenHints.add(new Keyword(CommandType.THEN_STATEMENT, "DUMMY_KEYWORD", 5, 5));
		HeuristicCheck.checkForThenClause(graph.getNodes().toArray(new ParseNode[0]), thenHints);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		assertTrue(nodes[5].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
		assertTrue(nodes[6].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
	}

	@Test
	public void nomenParticlePhrase() {
		String input = "if the door is open can you tidy it up";
		createGraph(input);
		List<Keyword> thenHints = new ArrayList<Keyword>();
		thenHints.add(new Keyword(CommandType.THEN_STATEMENT, "tidy up", 5, 6));
		HeuristicCheck.checkForThenClause(graph.getNodes().toArray(new ParseNode[0]), thenHints);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 5; i < nodes.length; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
		}
	}

	@Test
	public void NP_PP_NP() {
		String input = "So wash the dishes on the table";
		createGraph(input);
		List<Keyword> thenHints = new ArrayList<Keyword>();
		thenHints.add(new Keyword(null, "DUMMY_KEYWORD", 1, 1));
		HeuristicCheck.checkForThenClause(graph.getNodes().toArray(new ParseNode[0]), thenHints);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 1; i < nodes.length; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
		}
	}

	@Test
	public void NP_PP_VP() {
		String input = "So bring me the coffee to go";
		createGraph(input);
		List<Keyword> thenHints = new ArrayList<Keyword>();
		thenHints.add(new Keyword(null, "DUMMY_KEYWORD", 1, 1));
		HeuristicCheck.checkForThenClause(graph.getNodes().toArray(new ParseNode[0]), thenHints);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 1; i < nodes.length; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
		}
	}

	private void createGraph(String input) {
		snlp = new ShallowNLP();
		snlp.init();
		try {
			actual = snlp.parse(input, null);
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Arrays.deepToString(actual));
		graph = snlp.createParseGraph(actual);
		graph.getNodes().iterator().next().getType().addAttributeToType("String", "commandType");
		;
		for (INode node : graph.getNodes()) {
			node.setAttributeValue("commandType", null);
		}
	}
}
