import java.io.Serializable;

//an object structure to send chat messages between the server and clients
public class CribChat implements Serializable {
	
	//serialized for data security
	private static final long serialVersionUID = -7350994107201331546L;
	private String message;
	private String sender;
	private String timestamp;
	
	//constructor - bare
	public CribChat() {
		setWhoFrom("");
		setMsg("");
	}
	
	/* Constructor
	* @param - from : who sent the message
	* @param - msg : contents of the message
	*/
	public CribChat(String from, String msg) {
		setWhoFrom(from);
		setMsg(msg);
	}
	
	//getters and setters
	
	public String getMsg() {
		return message;
	}
	public void setMsg(String msg) {
		message = clean(msg);
	}
	public String getWhoFrom() {
		return sender;
	}
	public void setWhoFrom(String from) {
		sender = clean(from);
	}
	public String getTime() {
		return timestamp;
	}
	public void setTime(String time) {
		timestamp = time;
	}

	//removes characters that don't belong
	//leaves A-Z, a-z, 0-9, certain special characters
	private String clean(String st) {
		String ret = "";
		for(int ind = 0; ind < st.length(); ind++) {
			char ch = st.charAt(ind);
			if((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
				ret += ch;
			}
			else if((ch >= ' ' && ch <= '/') || (ch == ':' || ch == '=' || ch == '?' || ch == '@' || ch == '_' || ch == '^')) {
				ret += ch;
			}
		}
		return ret;
	}
	
}
