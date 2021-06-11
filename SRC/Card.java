import java.io.Serializable;

//card images from :	https://opengameart.org/content/playing-cards-vector-png
//back from :			https://i.pinimg.com/originals/8f/5e/12/8f5e12f669e4cdbdd487db296fa79bde.jpg

//an Object class holding details of a unique card
public class Card implements Serializable {

	//serialized for network transport
	private static final long serialVersionUID = 2766886532476941905L;
	public static final int SPADES = 1;
	public static final int HEARTS = 2;
	public static final int CLUBS = 3;
	public static final int DIAMONDS = 4;
	private int suit;
	private int rank;
	
	//constructor (make an empty card)
	public Card() {
		setSuit(0);
		setRank(0);
	}
	
	/*overloaded constructor with all data
	* @param - s : suit, final class integer
	* @param - r : rank or number (1-13 for A to K respectively)
	*/
	public Card(int s, int r) {
		setSuit(s);
		setRank(r);
	}
	
	//getters and setters
	
	public int getSuit() {
		return suit;
	}
	public void setSuit(int s) {
		if(s < 1 || s > 4) {
			return;
		}
		suit = s;
	}
	public int getRank() {
		return rank;
	}
	public void setRank(int r) {
		if(r < 1 || r > 13) {
			return;
		}
		rank = r;
	}
	
	//class methods
	
	//gets the full filename for this card (to display image)
	public static String getCardFileName(int suit, int rank) {
		if(rank < 1 || rank > 13 || suit < 1 || suit > 4) {	return "joker.png";	}
		String ret = "";
		if(rank == 1) {	ret += "ace";	}
		else if(rank == 11) {	ret += "jack";	}
		else if(rank == 12) {	ret += "queen";	}
		else if(rank == 13) {	ret += "king";	}
		else {	ret += rank;	}
		ret += "_of_";
		if(suit == SPADES) {	ret += "spades.png";	}
		else if(suit == HEARTS) {	ret += "hearts.png";	}
		else if(suit == CLUBS) {	ret += "clubs.png";	}
		else if(suit == DIAMONDS) {	ret += "diamonds.png";	}
		return ret;
	}
	
	//checks to see if the given Card is the same
	public boolean equals(Card c) {
		if(c.getRank() == rank && c.getSuit() == suit) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (suit*64) ^ rank;
	}
	
	//String or text form of the card
	public String toString() {
		String ret = "";
		if(rank == 1) {	ret += "Ace";	}
		else if(rank == 11) {	ret += "Jack";	}
		else if(rank == 12) {	ret += "Queen";	}
		else if(rank == 13) {	ret += "King";	}
		else {	ret += rank;	}
		ret += ".Of.";
		if(suit == SPADES) {	ret += "Spades";	}
		else if(suit == HEARTS) {	ret += "Hearts";	}
		else if(suit == CLUBS) {	ret += "Clubs";	}
		else if(suit == DIAMONDS) {	ret += "Diamonds";	}
		return ret;
	}
}
