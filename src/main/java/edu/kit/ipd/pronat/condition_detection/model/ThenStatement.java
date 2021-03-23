package edu.kit.ipd.pronat.condition_detection.model;

import java.util.List;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * This class represents a then-clause.
 * 
 * @author Sebastian Weigelt
 * @author Vanessa Steurer
 *
 */
public class ThenStatement extends Statement {
	private int begin;
	private int end;
	private boolean isINDPstmt;

	public ThenStatement(List<INode> nodeList) {
		cmdtype = CommandType.THEN_STATEMENT;
		this.nodeList = nodeList;
		isINDPstmt = false;
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
