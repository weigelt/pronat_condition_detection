package edu.kit.ipd.parse.conditionDetection;

import java.util.List;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * This class represents an else-clause.
 * 
 * @author Vanessa Steurer
 *
 */
public class ElseStatement extends Statement {
	private int begin;
	private int end;

	public ElseStatement(List<INode> nodeList) {
		this.cmdtype = CommandType.ELSE_STATEMENT;
		this.nodeList = nodeList;
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
