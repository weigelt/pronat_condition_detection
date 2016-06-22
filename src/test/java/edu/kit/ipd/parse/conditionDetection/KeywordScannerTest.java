package edu.kit.ipd.parse.conditionDetection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;
import edu.kit.ipd.parse.luna.graph.ParseGraph;
import edu.kit.ipd.parse.luna.graph.ParseNode;

public class KeywordScannerTest {
	private static final String TOKEN_NODE_TYPE = "token";
	KeywordScanner scanner;
	Synonyms syn;
	IGraph graph;
	INodeType wordType;
	INode[] nodes;

	@Before
	public void setUpGraph() throws Exception {
		syn = new Synonyms();
		syn.importSynonyms();
		graph = new ParseGraph();
		if (graph.hasNodeType(TOKEN_NODE_TYPE)) {
			wordType = graph.getNodeType(TOKEN_NODE_TYPE);
		} else {
			wordType = graph.createNodeType(TOKEN_NODE_TYPE);
		}
		wordType.addAttributeToType("String", "value");
		wordType.addAttributeToType("String", "commandType");
	}

	@Ignore("Test has no assertion - so we can skip it")
	@Test
	public void ifKeywordScan_2wordSynonym() {
		String[] words = { "in", "case", "you" };
		fillNodes(words.length, words);
		List<Keyword> list = KeywordScanner.searchIfKeywords(syn, nodes);
	}

	@Test
	public void ifKeywordScan_SkipIfso() {
		String[] words = { "if", "so", "go" };
		fillNodes(words.length, words);
		List<Keyword> list = KeywordScanner.searchIfKeywords(syn, nodes);

		assertTrue(list.isEmpty());
	}

	// Fixme - see #4
	@Test
	public void ifKeywordScan_multipleKeywords() {
		String[] words = { "if", "you", "go", "in", "case", "the", "tree" };
		fillNodes(words.length, words);
		List<Keyword> list = KeywordScanner.searchIfKeywords(syn, nodes);

		assertEquals(2, list.size());
	}

	private void fillNodes(int input, String[] s) {
		nodes = new ParseNode[input];
		for (int i = 0; i < input; i++) {
			INode node = graph.createNode(wordType);
			nodes[i] = node;
			node.setAttributeValue("value", s[i]);
		}
	}
}
