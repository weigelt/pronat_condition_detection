package edu.kit.ipd.pronat.condition_detection.model;

/**
 * This class represents all possible commandtypes of an instruction.
 *
 * @author Sebastian Weigelt
 * @author Vanessa Steurer
 */
public enum CommandType {
	IF_STATEMENT("IF"), THEN_STATEMENT("THEN"), ELSE_STATEMENT("ELSE"), INDEPENDENT_STATEMENT("INDP");

	private final String tag;

	private CommandType(String tag) {
		this.tag = tag;
	}

	@Override
	public String toString() {
		return getTag();
	}

	protected String getTag() {
		return tag;
	}

}