package structure.datatypes;

import java.util.Collection;

public class AnnotatedDocument {

    protected String uri;
    protected String text;
    // TODO make List; add constructor that allows Collection and sorts it to List 
    protected Collection<Mention> mentions;

	public AnnotatedDocument(String text) {
		this.text = text;
	}

	public AnnotatedDocument(String text, Collection<Mention> mentions) {
        this.text = text;
        this.mentions = mentions;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Collection<Mention> getMentions() {
		return mentions;
	}

	public void setMentions(Collection<Mention> mentions) {
		this.mentions = mentions;
	}

}
