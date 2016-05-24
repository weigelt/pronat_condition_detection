package edu.kit.ipd.parse.conditionDetection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.kit.ipd.parse.luna.graph.ParseGraph;
import edu.kit.ipd.parse.luna.graph.ParseNode;
import edu.kit.ipd.parse.luna.graph.ParseNodeType;

public class KeywordScannerTest {
	KeywordScanner scanner;
	Synonyms syn;
	ParseGraph graph;
	ParseNodeType wordType;
	ParseNode[] nodes;

	@Before
	public void setUpGraph() throws Exception {
		syn = new Synonyms();
		syn.importSynonyms();
		graph = new ParseGraph();
		wordType = new ParseNodeType("token");
		wordType.addAttributeToType("String", "value");
		wordType.addAttributeToType("String", "commandType");
	}

	@After
	public void afterTest() {
		System.out.println(" ");
	}

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

	@Test
	public void ifKeywordScan_multipleKeywords() {
		String[] words = { "if", "you", "go", "in", "case", "the", "tree" };
		fillNodes(words.length, words);
		List<Keyword> list = KeywordScanner.searchIfKeywords(syn, nodes);

		assertEquals(list.size(), 2);
	}

	private void fillNodes(int input, String[] s) {
		nodes = new ParseNode[input];
		for (int i = 0; i < input; i++) {
			ParseNode node = graph.createNode(wordType);
			nodes[i] = node;
			node.setAttributeValue("value", s[i]);
		}
	}

	private void fillNodes2(int input, String[] s, int ifStmtLength) {
		nodes = new ParseNode[input];
		for (int i = 0; i < input; i++) {
			ParseNode node = graph.createNode(wordType);
			node.setAttributeValue("value", s[i]);
			nodes[i] = node;
			if (i < ifStmtLength) {
				nodes[i].setAttributeValue("commandType", CommandType.IF_STATEMENT);
			}
		}
	}
}
