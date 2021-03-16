package edu.kit.ipd.pronat.condition_detection;

import java.util.LinkedList;
import java.util.List;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * Helperclass for the syntax functions.
 * 
 * @author Sebastian Weigelt
 * @author Vanessa Steurer
 *
 */
public class SyntaxHelper {
	public static List<String> adverbBlacklist = new LinkedList<String>();

	private SyntaxHelper() {

	}

	public static boolean isNotPhrase(INode[] nodes, int i) {
		return nodes[i].getAttributeValue("value").toString().equalsIgnoreCase("not");
	}

	public static boolean isNounPhrase(INode[] nodes, int i) {
		return nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase().contains("-NP")
				&& !nodes[i].getAttributeValue("pos").toString().equalsIgnoreCase("CC");
	}

	public static boolean isNounBlock(INode[] nodes, int i, int nextStmt) {
		boolean nounConjunction = false;
		if (i + 1 < nextStmt) {
			String currentChunk = nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase();
			String currentPos = nodes[i].getAttributeValue("pos").toString();
			nounConjunction = isNounPhrase(nodes, i + 1)
					&& (currentPos.equalsIgnoreCase("CC") || currentChunk.contains("PP") || isNotPhrase(nodes, i));
		}

		return nounConjunction;
	}

	public static boolean isVerbPhrase(INode[] nodes, int i) {
		return nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase().contains("-VP")
				&& !nodes[i].getAttributeValue("value").toString().equalsIgnoreCase("to") && !isNotPhrase(nodes, i)
				&& !nodes[i].getAttributeValue("pos").toString().equalsIgnoreCase("CC");
	}

	public static boolean isVerbBlock(INode[] nodes, int i, int nextStmt) {
		boolean verbConjunction = false;
		if (i + 1 < nextStmt) {
			String currentWord = nodes[i].getAttributeValue("value").toString();
			String currentChunk = nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase();
			String currentPos = nodes[i].getAttributeValue("pos").toString();
			verbConjunction = isVerbPhrase(nodes, i + 1)
					&& (isNotPhrase(nodes, i) || (currentChunk.contains("PP") && !currentWord.equalsIgnoreCase("in"))
							|| currentWord.equalsIgnoreCase("to") || currentPos.equalsIgnoreCase("CC"));
		}

		return verbConjunction;
	}

	public static boolean isAdjectivePhrase(INode[] nodes, int i) {
		return nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase().contains("ADJP");
	}

	public static boolean isAdjectiveBlock(INode[] nodes, int i, int nextStmt) {
		boolean adjectiveConjunction = false;
		if (i + 1 < nextStmt) {
			String currentPos = nodes[i].getAttributeValue("pos").toString();
			adjectiveConjunction = isAdjectivePhrase(nodes, i + 1) && currentPos.equalsIgnoreCase("CC");
		}

		return adjectiveConjunction;
	}

	public static boolean isAdverbPhrase(INode[] nodes, int i) {
		return nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase().contains("ADVP");
	}

	public static boolean isAdverbBlock(INode[] nodes, int i, int nextStmt) {
		boolean adverbConjunction = false;
		if (i + 1 < nextStmt) {
			String currentPos = nodes[i].getAttributeValue("pos").toString();
			adverbConjunction = isAdverbPhrase(nodes, i + 1) && currentPos.equalsIgnoreCase("CC");
		}

		return adverbConjunction;
	}

	public static boolean isParticlePhrase(INode[] nodes, int i) {
		return nodes[i].getAttributeValue("chunkIOB").toString().toUpperCase().contains("PRT");
	}

	public static void setAdverbBlacklist(List<KeyPhrase> keyPhrases) {
		for (KeyPhrase keyPhrase : keyPhrases) {
			// TODO @vanessa: Warum ist das so? Warum nicht "not" bei "if not"
			// Warum ist die adverbBlacklist keine List<List<String>> bzw. List<KeyPhrase>?
			adverbBlacklist.add(keyPhrase.get(0));
		}
	}
}
