package devcpu.assembler.preprocessor;

import devcpu.assembler.RawLine;

public class PreprocessedLine {
	private RawLine rawLine;
	public String text;
	public boolean preprocessorDirective;

	public PreprocessedLine(RawLine line) {
		this.rawLine = line;
		this.text = line.getText();
	}
	
	public RawLine getRawLine() {
		return rawLine;
	}

	public String getText() {
		return text;
	}
}
