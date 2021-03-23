package edu.kit.ipd.pronat.condition_detection.model;

import java.util.List;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * This class represents an else-clause.
 * 
 * @author Sebastian Weigelt
 * @author Vanessa Steurer
 *
 */
public class ElseStatement extends Statement {
	private int begin;
	private int end;

	public ElseStatement(List<INode> nodeList) {
		cmdtype = CommandType.ELSE_STATEMENT;
		this.nodeList = nodeList;
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
