package edu.kit.ipd.parse.conditionDetection;

import java.util.List;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * This class represents a then-clause.
 * 
 * @author Vanessa Steurer
 *
 */
public class ThenStatement extends Statement {
	private int begin;
	private int end;
	private boolean isINDPstmt;

	public ThenStatement(List<INode> nodeList) {
		this.cmdtype = CommandType.THEN_STATEMENT;
		this.nodeList = nodeList;
		this.isINDPstmt = false;
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

	public boolean isINDP() {
		return isINDPstmt;
	}

	public void setINDP() {
		isINDPstmt = true;
	}

	public int getBegin() {
		return begin;
	}

	public int getEnd() {
		return end;
	}
}
