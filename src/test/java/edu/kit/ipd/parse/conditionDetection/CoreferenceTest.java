package edu.kit.ipd.parse.conditionDetection;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseArcType;
import edu.kit.ipd.parse.luna.graph.ParseNode;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;

public class CoreferenceTest {
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

		input = "robo go to the table if there are any dirty dishes grab the dirty plates and go to the dishwasher";

		try {
			actual = snlp.parse(input, null);
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Arrays.deepToString(actual));
		graph = snlp.createParseGraph(actual);
		dt.setGraph(graph);
	}

	@Test
	public void withoutCoref() {
		dt.exec();
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 7; i < nodes.length; i++) {
			assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.INDEPENDENT_STATEMENT));
		}
	}

	@Test
	public void withCorefAndPPNP() {
		// Add synthetic coref-arc
		INode corefBegin = null;
		INode corefEnd = null;
		for (INode node : graph.getNodes()) {
			if (node.getAttributeValue("value").toString().equalsIgnoreCase("plates")) {
				corefBegin = node;
			} else if (node.getAttributeValue("value").toString().equalsIgnoreCase("dishes")) {
				corefEnd = node;
			}
		}
		graph.createArc(corefEnd, corefBegin, new ParseArcType("coref"));

		dt.exec();
	}

}
