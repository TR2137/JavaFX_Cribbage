import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

//an object structure to send game information between the server and clients
public class CribMSG implements Serializable {

	//serialized to ensure data
	private static final long serialVersionUID = -880133080849779441L;
	private Card[] cards;
	private String msg;
	private int messageType;
	//the types of messages to be sent
	public final static String[] CODES = {"MSG", "SCORE", "NEWHAND", "PEGRQST", "CRIBRQST", "PLAY", "CRIB", "UPCARD", "NAME", "GAMEOVER", "PEG", "ACK", "GO"};
	
	//constructor - bare
	public CribMSG() {
		cards = null;
		msg = "";
	}
	
	/* Constructor - full
	* @Param - code : code array location
	* @Param - message : message data string
	* @Param - cs : any cards that are being sent with the message
	*/
	public CribMSG(int code, String message, Card[] cs) {
		setType(code);
		setCards(cs);
		setMessage(message);
	}
	
	//getters and setters
	
	public Card[] getCards() {
		return cards;
	}
	public void setCards(Card[] cs) {
		if(cs == null) {
			cards = null;
			return;
		}
		Set<Card> temp = new LinkedHashSet<Card>();
		for(int i = 0; i < cs.length; i++) {
			if(cs[i] != null) {
				temp.add(cs[i]);
			}
		}
		cards = temp.toArray(new Card[1]);
	}
	public String getMessage() {
		return msg;
	}
	public void setMessage(String message) {
		msg = message;
	}

	public int getType() {
		return messageType;
	}

	public void setType(int code) {
		messageType = code;
	}
	
	//return the code number (array location) for a specific message type
	public static int codeFor(String st) {
		for(int ind = 0; ind < CODES.length; ind++) {
			if(st == CODES[ind]) {	return ind;	}
		}
		return -1;
	}
}
