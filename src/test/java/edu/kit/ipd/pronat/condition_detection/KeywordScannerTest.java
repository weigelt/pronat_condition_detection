package edu.kit.ipd.pronat.condition_detection;

import java.util.ArrayList;
import java.util.List;

import edu.kit.ipd.pronat.condition_detection.model.CommandType;
import edu.kit.ipd.pronat.condition_detection.model.Keyword;
import edu.kit.ipd.pronat.condition_detection.model.Synonyms;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;
import edu.kit.ipd.parse.luna.graph.ParseGraph;
import edu.kit.ipd.parse.luna.graph.ParseNode;

import static org.junit.Assert.*;

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
		wordType.addAttributeToType(CommandType.class.getName(), "commandType");
	}

	@Test
	public void ifKeywordScan_2wordSynonym() {
		String[] words = { "in", "case", "you" };
		List<Keyword> expKeywords = new ArrayList<>();
		Keyword inCaseKW = new Keyword(CommandType.IF_STATEMENT, "[in, case]", 0, 1);
		expKeywords.add(inCaseKW);
		fillNodes(words.length, words);
		List<Keyword> actual = KeywordScanner.searchIfKeywords(syn, nodes);
		assertKeywordEquals(expKeywords, actual);
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
		List<Keyword> expKeywords = new ArrayList<>();
		Keyword ifKW = new Keyword(CommandType.IF_STATEMENT, "[if]", 0, 0);
		Keyword inCaseKW = new Keyword(CommandType.IF_STATEMENT, "[in, case]", 3, 4);
		expKeywords.add(ifKW);
		expKeywords.add(inCaseKW);
		fillNodes(words.length, words);
		List<Keyword> actual = KeywordScanner.searchIfKeywords(syn, nodes);
		assertEquals(2, actual.size());
		assertKeywordEquals(expKeywords, actual);
	}

	private void assertKeywordEquals(List<Keyword> expected, List<Keyword> actual) {
		for (int i = 0; i < expected.size(); i++) {
			Keyword currEx = expected.get(i);
			Keyword currAc = actual.get(i);
			assertEquals(currEx.getCmdtype().name(), currAc.getCmdtype().name());
			assertEquals(currEx.getKeywordBegin(), currAc.getKeywordBegin());
			assertEquals(currEx.getKeywordEnd(), currAc.getKeywordEnd());
			assertEquals(currEx.getKeyword(), currAc.getKeyword());
		}
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
