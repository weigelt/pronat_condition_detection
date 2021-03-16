package edu.kit.ipd.pronat.condition_detection;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.kit.ipd.pronat.prepipedatamodel.token.Token;
import edu.kit.ipd.pronat.shallow_nlp.ShallowNLP;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseNode;

public class HeuristicCheckSNLP_ELSE {
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
	public void noElseStatement() {
		String input = "If the meal is hot warm it up go downstairs and bring me the wine";
		createGraph(input);
		List<Keyword> elseHints = new ArrayList<Keyword>();
		elseHints.add(new Keyword(null, "DUMMY_KEYWORD", 7, 7));
		HeuristicCheck.checkForElseClause(graph.getNodes().toArray(new ParseNode[0]), elseHints, new boolean[graph.getNodes().size()]);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 0; i < nodes.length; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType") == null);
		}
	}

	@Ignore
	@Test
	public void indpStatement() {
		String input = "If the dog barks he is angry else he wags his tail";
		createGraph(input);
		List<Keyword> elseHints = new ArrayList<Keyword>();
		elseHints.add(new Keyword(CommandType.ELSE_STATEMENT, "else", 7, 7));
		HeuristicCheck.checkForElseClause(graph.getNodes().toArray(new ParseNode[0]), elseHints, new boolean[graph.getNodes().size()]);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 7; i < nodes.length; i++) {
			System.out.println(
					"word: " + nodes[i].getAttributeValue("value") + " " + "command: " + nodes[i].getAttributeValue("commandType"));
			Assert.assertEquals(CommandType.INDEPENDENT_STATEMENT, nodes[i].getAttributeValue("commandType"));
			//assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.INDEPENDENT_STATEMENT));
		}
	}

	@Test
	public void NPandVP() {
		String input = "else wash the dirty dishes and store the food in the fridge";
		createGraph(input);
		List<Keyword> elseHints = new ArrayList<Keyword>();
		elseHints.add(new Keyword(CommandType.ELSE_STATEMENT, "else", 0, 0));
		HeuristicCheck.checkForElseClause(graph.getNodes().toArray(new ParseNode[0]), elseHints, new boolean[graph.getNodes().size()]);

		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 0; i < 7; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.ELSE_STATEMENT));
		}
		// trustSNLP=false	assertTrue(nodes[8].getAttributeValue("commandType").equals(CommandType.ELSE_STATEMENT));
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
		graph.getNodes().iterator().next().getType().addAttributeToType(CommandType.class.getName(), "commandType");

		for (INode node : graph.getNodes()) {
			node.setAttributeValue("commandType", null);
		}
	}
}
