package structure.config.constants;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Enumeration handling credentials from files on a lazy-loading basis <br>
 * Note: Security has been taken into account (not storing credentials in String
 * objects due to dumping dangers, ...), but we do not claim perfect handling of
 * said credentials
 * 
 * @author Kristian Noullet
 *
 */
public enum EnumUserAccounts {
	// TAGTOG_CONNECTION_TEST(EnumProperty.AUTHENTICATE_TAGTOG_TESTING), //
	// TAGTOG_CONNECTION(EnumProperty.AUTHENTICATE_TAGTOG), //
	VIRTUOSO_DBA(EnumProperty.AUTHENTICATE_VIRTUOSO),//
	;
	private transient final char[] username, password;
	private transient final EnumProperty property;

	EnumUserAccounts(final char[] username, final char[] password) {
		this.username = username;
		this.password = password;
		this.property = null;
	}

	EnumUserAccounts(final EnumProperty property) {
		this.username = null;// property.get("user");
		this.password = null;// property.get("password");
		this.property = property;
	}

	EnumUserAccounts(final String username, final char[] password) {
		this(username.toCharArray(), password);
	}

	public byte[] getBytesPassword() {
		if (this.password == null && this.property != null) {
			return getBytes(property.get("password"));
		}
		if (this.password != null) {
			return getBytes(this.password);
		}
		return null;
	}

	public byte[] getBytesUsername() {
		if (this.username == null && this.property != null) {
			return getBytes(property.get("user"));
		}
		if (this.username != null) {
			return getBytes(this.username);
		}
		return null;
	}

	/**
	 * Transforms character array to byte array for basic authentication usage<br>
	 * <b>Note</b>: Uses UTF-8 as specified in java's http-connection's
	 * recommendation
	 * https://stackoverflow.com/questions/5513144/converting-char-to-byte#9670279
	 * 
	 * @param chars input array
	 * @return input as bytes (UTF-8)
	 */
	public byte[] getBytes(final char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	public byte[] getUserpassBytes() {
		return getUserpassBytes(':');
	}

	public byte[] getUserpassBytes(char sep) {
		char[] userPassArr = new char[username.length + 1 + password.length];
		int i = 0;
		for (; i < username.length; ++i) {
			userPassArr[i] = username[i];
		}
		userPassArr[i] = sep;
		i++;
		for (; i < username.length + 1 + password.length; ++i) {
			userPassArr[i] = password[i - username.length - 1];
		}
		return getBytes(userPassArr);
	}
}
