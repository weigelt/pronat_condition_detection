package edu.kit.ipd.parse.conditionDetection;

import java.util.List;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * Abstract class which provides the possible statement-types of an instruction.
 * 
 * @author Vanessa Steurer
 */
public abstract class Statement {
	protected CommandType cmdtype;
	protected List<INode> nodeList;

	public Statement() {

	}

	public CommandType getCmdtype() {
		return cmdtype;
	}

	public List<INode> getNodeList() {
		return nodeList;
	}
}
