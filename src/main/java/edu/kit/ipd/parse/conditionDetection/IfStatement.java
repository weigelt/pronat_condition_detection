package edu.kit.ipd.parse.conditionDetection;

import java.util.ArrayList;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * This class represents an if-clause.
 * 
 * @author Vanessa Steurer
 *
 */
public class IfStatement extends Statement {
	private int begin;
	private int end;

	public IfStatement(ArrayList<INode> arrayList) {
		this.cmdtype = CommandType.IF_STATEMENT;
		this.nodeList = arrayList;
		this.begin = Integer.MAX_VALUE;
		this.end = Integer.MIN_VALUE;
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
