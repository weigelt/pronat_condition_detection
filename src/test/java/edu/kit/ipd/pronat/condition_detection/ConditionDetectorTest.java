package edu.kit.ipd.pronat.condition_detection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import edu.kit.ipd.pronat.prepipedatamodel.token.Token;
import edu.kit.ipd.pronat.shallow_nlp.ShallowNLP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.ParseNode;

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
		input = "armar please get me some vodka from the fridge if there are fresh oranges please bring them otherwise you have to give me the orange juice";
		try {
			actual = snlp.parse(input, null);
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
		//		System.out.println(Arrays.deepToString(actual));
		graph = snlp.createParseGraph(actual);
		dt.setGraph(graph);
		dt.exec();
		System.out.println(dt.getGraph().toString());

		//showArcs();
	}

	/**
	 * Shows the created arcs for the result of the conditionDetection-Algorithm.
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
		//		System.out.println(Arrays.deepToString(actual));
		graph = snlp.createParseGraph(actual);
		dt.setGraph(graph);
		//		System.out.println("Run first time");
		dt.exec();
		IGraph pg_first = graph.clone();
		//		System.out.println(graph);
		//		System.out.println("\nRun second time");
		dt.exec();
		//				System.out.println(pg_first);
		//		System.out.println(graph);
		Assert.assertEquals(pg_first, graph);
		//		System.out.println(pg_first);
	}

	/**
	 * Tests whether the conditionNumbers are set correctly.
	 *
	 * @author Tobias Hey
	 */
	@Test
	public void conditionNumberTest() {
		input = "Go to the fridge open the fridge if there are fresh oranges take the oranges and the water if there are no fresh oranges take the orange juice and the water";
		try {
			actual = snlp.parse(input, null);
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
		//		System.out.println(Arrays.deepToString(actual));
		graph = snlp.createParseGraph(actual);
		dt.setGraph(graph);
		dt.exec();
		List<INode> tokens = dt.getGraph().getNodesOfType(graph.getNodeType("token"));
		int[] expected = new int[tokens.size()];
		for (int i = 0; i < expected.length; i++) {
			if (i < 7) {
				expected[i] = -1;
			} else if (i < 18) {
				expected[i] = 0;
			} else if (i < 31) {
				expected[i] = 1;
			}
		}
		int[] actual = new int[tokens.size()];
		for (INode node : tokens) {
			actual[(int) node.getAttributeValue("position")] = (int) node.getAttributeValue(ConditionDetector.CONDITION_NUMBER);
		}
		Assert.assertArrayEquals(expected, actual);
	}

}
