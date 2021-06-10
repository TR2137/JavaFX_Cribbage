import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

//this is all the player data (used by Game)
public class Player {

	private String name;
	private Card[] hand;
	private int gamePoints;
	
	//constructor no name
	public Player() {
		hand = new Card[6];
		for(int ind = 0; ind < hand.length; ind++) {
			hand[ind] = null;
		}
		gamePoints = 0;
	}
	//constructor with name
	public Player(String playerName) {
		hand = new Card[6];
		setName(playerName);
		for(int ind = 0; ind < hand.length; ind++) {
			hand[ind] = null;
		}
		gamePoints = 0;
	}
	
	//setters and getters
	
	public void setName(String playerName) {	name = playerName;	}
	
	public String getName() {	return name;	}
	
	public Card[] getHand() {
		//we need to return the hand, with no empty slots
		Set<Card> temp = new HashSet<Card>();
		for(int indGH = 0; indGH < hand.length; indGH++) {
			if(hand[indGH] != null) {
				temp.add(hand[indGH]);
			}
		}
		return temp.toArray(new Card[4]);
	}
	
	public int getPoints() {
		return gamePoints;
	}
	
	public void setPoints(int points) {
		gamePoints = points;
	}
	
	//add points to counter
	public void addPoints(int points) {
		gamePoints += points;
		if(gamePoints >= 121) {	gamePoints = 121;	}	//can't go over the board limit
	}
	
	//add a card (given) to hand
	public void addCard(Card c) {
		for(int ind = 0; ind < hand.length; ind++) {
			if(hand[ind] == null) {
				hand[ind] = c;
				return;
			}
		}
	}
	
	//empty the hand of cards
	public void clearHand() {
		for(int ind = 0; ind < hand.length; ind++) {
			hand[ind] = null;
		}
	}
	
	//remove a specific card (given)
	public void removeCard(Card c) {
		if(hand == null) {
			return;
		}
		for(int ind = 0; ind < hand.length; ind++) {
			if(hand[ind] != null && hand[ind].equals(c)) {
				hand[ind] = null;
				return;
			}
		}
	}
	
	//play the card at the given index
	public Card playCard(int index) {
		Card c = hand[index];
		hand[index] = null;
		return c;
	}
	
	/*count the points in a hand
	* @param - cards : the hand to count
	* @param - upCard : the turned card to count with
	* @return - int : the points scored given the hand and up card
	*/
	public int countHand(Card[] cards, Card upCard) {
		if(cards == null || upCard == null) {
			return 0;
		}
		int points = 0;
		int[] suitCT = {0, 0, 0, 0};
		//go through each card, see if it can make points
		Set<Set<Card>> pairs = new HashSet<Set<Card>>();
		Set<Set<Card>> fifteens = new HashSet<Set<Card>>();
		for(int ind = 0; ind < cards.length; ind++) {
			//Pairs
			for(int indP = ind+1; indP < cards.length; indP++) {	//does this match any cards after it
				if(cards[ind].getRank() == cards[indP].getRank()){
					Set<Card> temp = new HashSet<Card>();
					temp.add(cards[ind]);
					temp.add(cards[indP]);
					pairs.add(temp);
				}
			}
			if(cards[ind].getRank() == upCard.getRank()) {		//does this match the upCard
				Set<Card> temp = new HashSet<Card>();
				temp.add(upCard);
				temp.add(cards[ind]);
				pairs.add(temp);
			}
			
			//15s
			//fifteens with 2 cards
			for(int first = ind+1; first <= (cards.length); first++) {
				Set<Card> fif = new HashSet<Card>();
				fif.add(cards[ind]);			//always add the current card as the '0'th card
				if(first < cards.length) {
					fif.add(cards[first]);
				}
				else {
					fif.add(upCard);
				}
				if(fif.size() == 2 && isFifteen(fif)) {
					fifteens.add(fif);
				}
			}
			//fifteens with 3 cards
			for(int first = ind+1; first < cards.length; first++) {
				for(int second = first+1; second <= (cards.length); second++) {
					Set<Card> fif = new HashSet<Card>();
					fif.add(cards[ind]);			//always add the current card as the '0'th card
					fif.add(cards[first]);
					if(second < cards.length) {
						fif.add(cards[second]);
					}
					else {
						fif.add(upCard);
					}
					if(fif.size() == 3 && isFifteen(fif)) {
						fifteens.add(fif);
					}
				}
			}
			//fifteens with 4 cards
			for(int leave = 0; leave < (cards.length+1); leave++) {
				Set<Card> fif = new HashSet<Card>();
				for(int add = 0; add < (cards.length+1); add++) {
					if(leave != add && add < cards.length) {
						fif.add(cards[add]);
					}
					else if(leave != add) {
						fif.add(upCard);
					}
				}
				if(fif.size() == 4 && isFifteen(fif)) {
					fifteens.add(fif);
				}
			}
			//fifteens with all cards (max 5)
			Set<Card> fif = new HashSet<Card>();
			for(int add = ind; add <= cards.length; add++) {
				if(add < cards.length) {
					fif.add(cards[add]);
				}
				else {
					fif.add(upCard);
				}
			}
			if(fif.size() == 5 && isFifteen(fif)) {
				fifteens.add(fif);
			}
			//Knobs
			if(cards[ind].getRank() == 11 && cards[ind].getSuit() == upCard.getSuit()) {
				points += 1;	//knobs is worth 1
			}

			//Flush
			suitCT[cards[ind].getSuit() -1]++;
		}
		points += 2 * pairs.size();			//pairs are worth 2 points
		//System.out.println("pairs for " + 2 * pairs.size());
		points += 2 * fifteens.size();		//fifteens are worth 2 points each
		//System.out.println("15s for " + 2 * fifteens.size());
		//Flush is worth 1 point per card in the matching suit (4-5)
		if(suitCT[0] >= 4) {
			points += suitCT[0];
			if(upCard.getSuit()-1 == 0) {	points++;	}
		}
		else if(suitCT[1] >= 4) {
			points += suitCT[1];
			if(upCard.getSuit()-1 == 1) {	points++;	}
		}
		else if(suitCT[2] >= 4) {
			points += suitCT[2];
			if(upCard.getSuit()-1 == 2) {	points++;	}
		}
		else if(suitCT[3] >= 4) {
			points += suitCT[3];
			if(upCard.getSuit()-1 == 3) {	points++;	}
		}
		//Runs
		Set<Set<Card>> runs = new HashSet<Set<Card>>();
		//run of length 5
		Set<Card> full = new HashSet<Card>();
		full.add(upCard);
		for(int indR = 0; indR < cards.length; indR++) {
			full.add(cards[indR]);
		}
		//System.out.println(full);
		if(isRun(full)) {
			//System.out.println("added");
			runs.add(full);
		}
		//System.out.print("runs: ");
		//System.out.println(runs);
		//runs of length 3 starting with this card
		for(int first = 0; first < cards.length; first++) {
			for(int second = first+1; second < cards.length; second++) {
				for(int third = second+1; third < (cards.length+1); third++) {
					Set<Card> threeR = new HashSet<Card>();
					threeR.add(cards[first]);
					threeR.add(cards[second]);
					if(third < cards.length) {
						threeR.add(cards[third]);
					}
					else {
						threeR.add(upCard);
					}
					//System.out.println(threeR);
					if(isRun(threeR)) {
						//System.out.println("added");
						runs.add(threeR);
					}
				}
			}
		}
		//System.out.print("runs: ");
		//System.out.println(runs);
		//runs of length 4 starting with this card
		for(int leave = 0; leave < (cards.length+1); leave++) {
			Set<Card> fourR = new HashSet<Card>();
			for(int add = 0; add < (cards.length+1); add++) {
				if(leave != add && add < cards.length) {
					fourR.add(cards[add]);
				}
				else if(leave != add) {
					fourR.add(upCard);
				}
			}
			//System.out.println(fourR);
			if(isRun(fourR)) {
				//System.out.println("added");
				runs.add(fourR);
			}
		}
		//System.out.print("runs: ");
		//System.out.println(runs);
		//the length of the run determines the number of points, each run is counted individually
		//Set[] temp = {new HashSet<Card>()};
		Set<Card>[] lRuns = longestRuns(runs);
		if(lRuns != null && lRuns.length > 0 && lRuns[0] != null) {
			points += lRuns[0].size() * lRuns.length;
			//System.out.println("runs are worth: " + (lRuns[0].size() * lRuns.length));
		}		
		return points;
	}
	
	//checks if the given set of cards can be used to make a run
	//used for counting hands
	public boolean isRun(Set<Card> cards){
		if(cards.size() < 3) {
			return false;
		}
		Card[] cs = cards.toArray(new Card[cards.size()]);
		int[] ranks = new int[cs.length];
		for(int ind = 0; ind < ranks.length; ind++) {
			ranks[ind] = cs[ind].getRank();
			if(ind > 0 && ranks[ind-1] != ranks[ind]-1) {
				return false;
			}
		}
		return true;
	}
	
	//gets the set of cards with the longest length run
	//used for counting hands
	private Set<Card>[] longestRuns(Set<Set<Card>> runs){
		Set[] temp = new Set[runs.size()];
		int[] length = {0, 0, 0};		//3, 4, 5 length runs
		Iterator<Set<Card>> it = runs.iterator();
		for(int i = 0; i < temp.length && it.hasNext(); i++) {
			temp[i] = it.next();		//the next run -> array
			if(temp[i].size() > 5 || temp[i].size() < 3) {
				System.out.println("bad set");
				break;
			}
			length[temp[i].size()-3]++;	//add to run counter at the length of this run
		}
		Set<Card>[] ret;
		int idx = 0;
		//check for the longest runs (in size order)
		//add any runs of that length to the return array
		if(length[2] > 0) {		//length 5 runs
			ret = (Set<Card>[])new Set[length[2]];
			for(int i = 0; i < temp.length; i++) {
				if(temp[i].size() == 5) {
					ret[idx] = temp[i];
					idx++;
				}
			}
		}
		else if(length[1] > 0) {		//length 4 runs
			ret = (Set<Card>[])new Set[length[1]];
			for(int i = 0; i < temp.length; i++) {
				if(temp[i].size() == 4) {
					ret[idx] = temp[i];
					idx++;
				}
			}
		}
		else if(length[0] > 0) {		//length 3 runs
			ret = (Set<Card>[])new Set[length[0]];
			for(int i = 0; i < temp.length; i++) {
				if(temp[i].size() == 3) {
					ret[idx] = temp[i];
					idx++;
				}
			}
		}
		else {
			ret = null;
		}
		return ret;		//return the longest size run (all occurrences of that size)
		
	}
	
	//checks to see if the given set of cards adds up to exactly 15
	//used for counting hands
	private boolean isFifteen(Set<Card> cards){
		int sum = 0;
		Card[] cs = cards.toArray(new Card[cards.size()]);
		for(int i = 0; i < cs.length; i++){
			int temp = cs[i].getRank();
			if(temp >= 10) {
				sum += 10;
			}
			else {
				sum += temp;
			}
		}
		if(sum == 15) {
			//System.out.println(cards);
			return true;
		}
		return false;
	}
	
}
