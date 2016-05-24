package edu.kit.ipd.parse.conditionDetection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseNode;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;
import edu.kit.ipd.parse.shallownlp.WordPosType;

public class ConditionDetectorTest {
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
	}

	@Test
	public void executeProject() {
		input = "Go to the fridge open the fridge if they are fresh oranges take the oranges and the water if there are no fresh oranges take the orange juice and the water close the fridge and take them to the table";
		String[] words = { "put" };
		String[] pos = { "VB" };
		WordPosType w = new WordPosType(words, pos);
		try {
			actual = snlp.parse(input, w);
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Arrays.deepToString(actual));
		graph = snlp.createParseGraph(actual);
		dt.setGraph(graph);
		dt.exec();

		//showArcs();
	}

	/**
	 * Shows the created arcs for the result of the
	 * conditionDetection-Algorithm.
	 */
	private void showArcs() {
		INode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		String s = "cmdtype = ";
		for (int i = 0; i < nodes.length; i++) {
			String loc = "loc";
			for (IArc arc : nodes[i].getOutgoingArcs()) {
				if (arc.getType().getName().equals("statement")) {
					loc = arc.getAttributeValue("commandTypeLocation").toString();
				}
			}
			s = s.concat(nodes[i].getAttributeValue("value").toString() + "|" + nodes[i].getAttributeValue("commandType").toString() + "--"
					+ loc + "->");
		}
		System.out.println(s);
	}

	@Test
	public void executeProjectTwice() {
		input = "if I leave and the dog in the kitchen is hungry go and bring in the mail";

		try {
			actual = snlp.parse(input, null);
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Arrays.deepToString(actual));
		graph = snlp.createParseGraph(actual);
		dt.setGraph(graph);
		System.out.println("Run first time");
		dt.exec();

		System.out.println("\nRun second time");
		dt.exec();
	}

}
