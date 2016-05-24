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

public class HeuristicCheckSNLP_IF {
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
	public void noIfStatement() {
		String input = "If barks loudly";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		HeuristicCheck.checkForIfClause(graph.getNodes().toArray(new ParseNode[0]), list);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 0; i < nodes.length; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.INDEPENDENT_STATEMENT));
		}
	}

	@Test
	public void verbConjunction() {
		String input = "If you closed and wiped the floor go";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		HeuristicCheck.checkForIfClause(nodes, list);

		for (int i = 0; i < 5; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.IF_STATEMENT));
		}
		assertTrue(nodes[5].getAttributeValue("commandType") == null);
	}

	@Test
	public void doubleVerbConjunction() {
		String input = "If I leave go outside";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		HeuristicCheck.checkForIfClause(nodes, list);

		for (int i = 0; i < 3; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.IF_STATEMENT));
		}
		assertTrue(nodes[3].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
		assertTrue(nodes[4].getAttributeValue("commandType") == null);
	}

	@Test
	public void adjectivePhrase() {
		String input = "If the black dog barks loudly go";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		HeuristicCheck.checkForIfClause(nodes, list);

		for (int i = 0; i < 6; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.IF_STATEMENT));
		}
	}

	@Test
	public void preposition() {
		String input = "If the dog comes into the room prepare his dinner";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		HeuristicCheck.checkForIfClause(nodes, list);

		for (int i = 0; i < 6; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.IF_STATEMENT));
		}
		// trustSNLP=false assertTrue(nodes[6].getAttributeValue("commandType") == null); 
	}

	@Test
	public void particlePhrase() {
		String input = "If you turned off the lights";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		HeuristicCheck.checkForIfClause(nodes, list);

		for (int i = 0; i < 3; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.IF_STATEMENT));
		}
	}

	@Test
	public void toInfinitive() {
		String input = "If you are ready to help me come over";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		HeuristicCheck.checkForIfClause(nodes, list);

		for (int i = 0; i < 7; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.IF_STATEMENT));
		}
		assertTrue(nodes[8].getAttributeValue("commandType") == null);
	}

	@Test
	public void thenClause() {
		String input = "if the dog is barking give him some food";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		HeuristicCheck.checkForIfClause(nodes, list);

		assertTrue(nodes[5].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
	}

	@Test
	public void tripleVerbConjunction() {
		String input = "if the dog is barking or growling give him some food";
		createGraph(input);
		List<Keyword> list = new ArrayList<Keyword>();
		list.add(new Keyword(null, "if", 0, 0));
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		HeuristicCheck.checkForIfClause(nodes, list);

		for (int i = 0; i < 7; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.IF_STATEMENT));
		}
		assertTrue(nodes[7].getAttributeValue("commandType").equals(CommandType.THEN_STATEMENT));
		for (int i = 8; i < 10; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType") == null);
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
		for (INode node : graph.getNodes()) {
			node.setAttributeValue("commandType", null);
		}
	}
}
