package edu.kit.ipd.parse.conditionDetection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IArcType;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;
import edu.kit.ipd.parse.luna.graph.Pair;
import edu.kit.ipd.parse.luna.graph.ParseNode;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;

public class CoreferenceTest {
	ConditionDetector dt;
	Token[] actual;
	ShallowNLP snlp;
	String input;
	IGraph graph;

	//	@Before
	//	public void setUp() {
	//		snlp = new ShallowNLP();
	//		snlp.init();
	//		dt = new ConditionDetector();
	//		dt.init();
	//
	//		input = "robo go to the table if there are any dirty dishes grab the dirty plates and go to the dishwasher";
	//
	//		try {
	//			actual = snlp.parse(input, null);
	//		} catch (IOException | URISyntaxException | InterruptedException e) {
	//			e.printStackTrace();
	//		}
	//		System.out.println(Arrays.deepToString(actual));
	//		graph = snlp.createParseGraph(actual);
	//		dt.setGraph(graph);
	//	}

	@Before
	public void setUp() {
		snlp = new ShallowNLP();
		snlp.init();
		dt = new ConditionDetector();
		dt.init();

		input = "if I leave do the laundry iron it go to the wardrobe and put it in the wardrobe";
		//input = "Robo go to the table if there are any dirty dishes got the dirty dishes and go to the dishwasher open the dishwasher and put the dirty dishes into the dishwasher the dishwasher and returned to the table shower and clean this up the clean dishes and go to the keyboard open the card and put the clean dishes into the car";
		try {
			actual = snlp.parse(input, null);
		} catch (IOException | URISyntaxException | InterruptedException e) {
			e.printStackTrace();
		}
		graph = snlp.createParseGraph(actual);
		dt.setGraph(graph);
	}

	//@Ignore
	@Test
	public void withoutCoref() {
		dt.exec();
		ParseNode[] nodes = graph.getNodes().toArray(new ParseNode[0]);
		for (int i = 8; i < nodes.length; i++) {
			//			System.out.println(
			//					"word: " + nodes[i].getAttributeValue("value") + " " + "command: " + nodes[i].getAttributeValue("commandType"));
			Assert.assertEquals(CommandType.INDEPENDENT_STATEMENT, nodes[i].getAttributeValue("commandType"));
			//assertTrue(nodes[i].getAttributeValue("commandType").equals(CommandType.INDEPENDENT_STATEMENT));
		}
	}

	@Test
	public void withCorefAndPPNP() {
		// Add synthetic coref-arc
		INode corefBegin = null;
		INode corefEnd = null;
		INodeType eNT = graph.createNodeType("contextEntity");
		IArcType ref = graph.createArcType("reference");
		IArcType coref = graph.createArcType("contextRelation");
		coref.addAttributeToType("double", "confidence");
		coref.addAttributeToType("String", "typeOfRelation");

		List<Pair<Integer, Integer>> corefs = new ArrayList<>();
		corefs.add(new Pair<Integer, Integer>(14, 7));
		//		corefs.add(new Pair<Integer, Integer>(14, 10));
		//		corefs.add(new Pair<Integer, Integer>(22, 19));
		//		corefs.add(new Pair<Integer, Integer>(27, 14));
		//		corefs.add(new Pair<Integer, Integer>(30, 22));
		//		corefs.add(new Pair<Integer, Integer>(32, 30));
		//		corefs.add(new Pair<Integer, Integer>(37, 4));
		//		corefs.add(new Pair<Integer, Integer>(45, 41));
		//		corefs.add(new Pair<Integer, Integer>(53, 50));
		//		corefs.add(new Pair<Integer, Integer>(58, 45));
		//		corefs.add(new Pair<Integer, Integer>(61, 53));

		for (Pair<Integer, Integer> pair : corefs) {
			for (INode node : graph.getNodes()) {
				if (node.getType().equals(graph.getNodeType("token"))) {
					if ((Integer) node.getAttributeValue("position") == pair.getLeft().intValue()) {
						corefBegin = node;
					} else if ((Integer) node.getAttributeValue("position") == pair.getRight().intValue()) {
						corefEnd = node;
					}
				}
			}
			INode begin = graph.createNode(eNT);
			INode end = graph.createNode(eNT);
			IArc refArc = graph.createArc(begin, corefBegin, ref);
			IArc refArc2 = graph.createArc(end, corefEnd, ref);

			IArc arc = graph.createArc(begin, end, coref);
			arc.setAttributeValue("confidence", 1.0d);
			arc.setAttributeValue("typeOfRelation", "referentRelation");
		}

		dt.exec();
		ParseNode[] nodes = dt.getGraph().getNodes().toArray(new ParseNode[0]);
		for (int i = 3; i < nodes.length; i++) {
			if (nodes[i].getType().getName().equals("token")) {
				//				System.out.println(
				//						"word: " + nodes[i].getAttributeValue("value") + " " + "command: " + nodes[i].getAttributeValue("commandType"));
				Assert.assertEquals(CommandType.THEN_STATEMENT, nodes[i].getAttributeValue("commandType"));
			}
		}
	}

}
