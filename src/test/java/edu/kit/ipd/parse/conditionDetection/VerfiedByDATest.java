package edu.kit.ipd.parse.conditionDetection;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseNode;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;

public class VerfiedByDATest {

	ConditionDetector dt;
	Token[] actual;
	ShallowNLP snlp;
	String input;
	IGraph graph;

	@Before
	public void setUp() {
		snlp = new ShallowNLP();
		snlp.init();
		dt = new ConditionDetector();
		dt.init();

		input = "if I leave do the laundry iron it go to the wardrobe and put it in the wardrobe";

		try {
			actual = snlp.parse(input, null);
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
		graph = snlp.createParseGraph(actual);
		dt.setGraph(graph);
	}

	@Test
	public void simpleDATest() {
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		mockDA(nodes);
		dt.exec();
		for (int i = 3; i < nodes.length; i++) {
			System.out.println(
					"word: " + nodes[i].getAttributeValue("value") + " " + "command: " + nodes[i].getAttributeValue("commandType"));
			Assert.assertEquals(CommandType.THEN_STATEMENT, nodes[i].getAttributeValue("commandType"));
			//assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.INDEPENDENT_STATEMENT));
		}
	}

	private void mockDA(ParseNode[] nodes) {
		for (int i = 3; i < nodes.length; i++) {
			INode node = nodes[i];
			if (!node.getType().containsAttribute("commandTypeVerified", "boolean")) {
				node.getType().addAttributeToType("boolean", "commandTypeVerified");
			}
			if (!node.getType().containsAttribute("commandType", "String")) {
				node.getType().addAttributeToType("String", "commandType");
			}
			node.setAttributeValue("commandType", CommandType.THEN_STATEMENT);
			node.setAttributeValue("commandTypeVerified", true);
		}
	}
}
