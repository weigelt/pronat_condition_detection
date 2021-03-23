package edu.kit.ipd.pronat.condition_detection.model;

import java.util.List;

import edu.kit.ipd.parse.luna.graph.INode;

/**
 * Abstract class which provides the possible statement-types of an instruction.
 * 
 * @author Sebastian Weigelt
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
