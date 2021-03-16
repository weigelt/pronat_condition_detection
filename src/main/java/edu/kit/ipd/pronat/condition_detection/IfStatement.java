package edu.kit.ipd.pronat.condition_detection;

import java.util.ArrayList;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * This class represents an if-clause.
 * 
 * @author Sebastian Weigelt
 * @author Vanessa Steurer
 *
 */
public class IfStatement extends Statement {
	private int begin;
	private int end;

	public IfStatement(ArrayList<INode> arrayList) {
		cmdtype = CommandType.IF_STATEMENT;
		nodeList = arrayList;
		begin = Integer.MAX_VALUE;
		end = Integer.MIN_VALUE;
	}

	public void addNodeToNodeList(INode node) {
		getNodeList().add(node);
		if (begin > (int) node.getAttributeValue("position")) {
			begin = (int) node.getAttributeValue("position");
		}

		if (end < (int) node.getAttributeValue("position")) {
			end = (int) node.getAttributeValue("position");
		}
	}

	public int getBegin() {
		return begin;
	}

	public int getEnd() {
		return end;
	}
}
