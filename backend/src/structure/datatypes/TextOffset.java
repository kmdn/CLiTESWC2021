package structure.datatypes;

public class TextOffset implements Comparable<TextOffset> {
	public int offset = 0;
	public String text = null;

	public TextOffset text(final String text) {
		this.text = text;
		return this;
	}

	public TextOffset offset(final int offset) {
		this.offset = offset;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TextOffset) {
			final TextOffset objTO = (TextOffset) obj;
			return this.offset == objTO.offset
					&& ((this.text == null && objTO.text == null) || (this.text.equals(objTO.text)));
		}
		return super.equals(obj);
	}

	@Override
	public int compareTo(TextOffset o) {
		return this.offset + (this.text == null ? 0 : this.text.length())
				- (o.offset + (o.text == null ? 0 : o.text.length()));
	}

	@Override
	public String toString() {
		return "[" + this.offset + "] \"" + this.text + "\"";
	}

}
