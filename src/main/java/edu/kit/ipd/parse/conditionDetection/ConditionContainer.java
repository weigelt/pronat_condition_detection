package edu.kit.ipd.parse.conditionDetection;

/**
 * This class represents one Statement, consisting of an If-, Then- and optional Else-section.
 * Each of these Statements holds a list of all belonging nodes.
 * 
 * @author Vanessa Steurer
 */
public class ConditionContainer {
	private IfStatement ifStmt;
	private ThenStatement thenStmt;
	private ElseStatement elseStmt;
	private int conditionBegin;
	private int conditionEnd;
	
	public ConditionContainer(IfStatement ifStmt, ThenStatement thenStmt, ElseStatement elseStmt) {
		this.ifStmt = ifStmt;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
	}
	
	public boolean hasThenStmt() {
		return thenStmt != null;
	}
	
	public boolean hasElseStmt() {
		return elseStmt != null;
	}
	
	public int setConditionBegin() {
		return this.conditionBegin = ifStmt.getBegin();
	}
	
	public int setConditionEnd() {
		if (hasElseStmt()) {
			return this.conditionEnd = elseStmt.getEnd();
		} else if (hasThenStmt()){
			return this.conditionEnd = thenStmt.getEnd();	
		} else {
			return this.conditionEnd = ifStmt.getEnd();
		}
	}

	public IfStatement getIfStmt() {
		return ifStmt;
	}

	public void setIfStmt(IfStatement ifStmt) {
		this.ifStmt = ifStmt;
	}

	public ThenStatement getThenStmt() {
		return thenStmt;
	}

	public void setThenStmt(ThenStatement thenStmt) {
		this.thenStmt = thenStmt;
	}

	public ElseStatement getElseStmt() {
		return elseStmt;
	}

	public void setElseStmt(ElseStatement elseStmt) {
		this.elseStmt = elseStmt;
	}
	
	public int getConditionBegin() {
		return conditionBegin;
	}
	
	public int getConditionEnd() {
		return conditionEnd;
	}
}
