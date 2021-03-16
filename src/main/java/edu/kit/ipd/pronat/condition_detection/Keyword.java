package edu.kit.ipd.pronat.condition_detection;

/**
 * This class represents a spotted keyword in the input text. The keywords can
 * consist of more than one word, this is the reason why there is a keywordBegin
 * and keywordEnd.
 * 
 * @author Sebastian Weigelt
 * @author Vanessa Steurer
 */
public class Keyword {
	private CommandType cmdtype;
	private String keyword;
	private int keywordBegin;
	private int keywordEnd;
	private static final String DUMMY = "DUMMY_KEYWORD";
	private boolean invalid;

	public Keyword(CommandType cmdtype, String keyword, int keywordBegin, int keywordEnd) {
		this.cmdtype = cmdtype;
		this.keyword = keyword;
		this.keywordBegin = keywordBegin;
		this.keywordEnd = keywordEnd;
		invalid = false;
	}

	public CommandType getCmdtype() {
		return cmdtype;
	}

	public void setCmdtype(CommandType cmdtype) {
		this.cmdtype = cmdtype;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public int getKeywordBegin() {
		return keywordBegin;
	}

	public void setKeywordBegin(int keywordBegin) {
		this.keywordBegin = keywordBegin;
	}

	public int getKeywordEnd() {
		return keywordEnd;
	}

	public void setKeywordEnd(int keywordEnd) {
		this.keywordEnd = keywordEnd;
	}

	public boolean isDummy() {
		return getKeyword().equals(DUMMY);
	}

	public boolean isInvalid() {
		return invalid;
	}

	public void setInvalid() {
		invalid = true;
	}

	@Override
	public String toString() {
		return keyword + " (" + keywordBegin + ":" + keywordEnd + ")";
	}
}
