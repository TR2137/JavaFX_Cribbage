import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

//This is the Server
public class Game {

	private static int players = 2;								//Number of players (incase I had a 3-player option)
	private static boolean gameEnd;								//if the game is over
	private static Card[] deck = new Card[52];					//holds a deck of cards
	private static Card[] pegCards = new Card[players*4];		//for holding cards while pegging
	private static int pegInd;									//pegging index
	private static int pegCurrentCT;							//running counter for pegging (stop at 31)
	private static int currentPegRunStart;						//for tracking the pegging round
	private static Card upCard;									//to hold the card turned up (cut card)
	private static Card[] crib = new Card[4];					//the cards thrown to the crib
	private static int dealer;									//who is currently the dealer
	private static int pTurn;									//who's turn it is
	private static Player[] pNext = new Player[players];		//list of players to get the next turn
	private static Socket[] socket = new Socket[players];		//sockets for each player
	private static ServerSocket server = null;											//server socket to connect to
	private static ObjectInputStream[] inStrm = new ObjectInputStream[players];			//Io stream for getting game data
	private static ObjectOutputStream[] outStrm = new ObjectOutputStream[players];		//iO stream for sending game data
	private static ServerSocket chatServer = null;										//server socket to connect the chat
	private static Socket[] chatSocket = new Socket[players];							//list of sockets for each player chat
	private static ObjectInputStream[] chatIn = new ObjectInputStream[players];			//Io stream for receiving chat messages
	private static ObjectOutputStream[] chatOut = new ObjectOutputStream[players];		//iO stream for sending chats
	
	//run the game server
	public static void main(String[] args) {
		//initialize arrays
		for(int i = 0; i < players; i++) {
			pNext[i] = null;
			socket[i] = null;
			inStrm[i] = null;
			outStrm[i] = null;
			chatSocket[i] = null;
			chatIn[i] = null;
			chatOut[i] = null;
		}
		
		//open and make the client connections (and chat)
		int cons = 0;
		try {
			server = new ServerSocket(2137);
			System.out.println("Server Started!");
			chatServer = new ServerSocket(7321);
			System.out.println("Chat Server Started!");
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("Server Failed to Start!");
		}
		if(!server.isBound() || !chatServer.isBound()) {
			System.out.println("server didn't start!");
			return;
		}
		while(cons < players) {
			try {
				//game server
				//System.out.println("Awaiting connections");
				socket[cons] = server.accept();
				System.out.println(socket[cons].getInetAddress() + " has connected to server!");
				outStrm[cons] = new ObjectOutputStream(new BufferedOutputStream(socket[cons].getOutputStream()));
				outStrm[cons].flush();
				inStrm[cons] = new ObjectInputStream(new BufferedInputStream(socket[cons].getInputStream()));
				//chat server
				chatSocket[cons] = chatServer.accept();
				chatOut[cons] = new ObjectOutputStream(new BufferedOutputStream(chatSocket[cons].getOutputStream()));
				chatOut[cons].flush();
				chatIn[cons] = new ObjectInputStream(new BufferedInputStream(chatSocket[cons].getInputStream()));
				final int idx = cons;
				//spin off a thread to handle this players chat
				new Thread(new Runnable() {
					@Override public void run() {
						System.out.println("Starting chat thread...");
						chatHandler(idx);
						}
					}).start();
			
				System.out.println(socket[cons].getInetAddress() + " streams initialized");
				//wait for each players name, and create the players as they send their name
				try {
					do {
					CribMSG cm = (CribMSG)inStrm[cons].readObject();
						if(CribMSG.CODES[cm.getType()] == "NAME") {
							pNext[cons] = new Player(cm.getMessage());
							break;
						}
					}while(true);
				}catch(Exception e) {
					e.printStackTrace();
				}
				cons++;
			}catch(IOException ex) {
				System.out.println(ex);
			}
			System.out.println(cons + " players connected");
		}
		clientMSG(CribMSG.codeFor("NAME"), pNext[0].getName(), null, 1);
		clientMSG(CribMSG.codeFor("NAME"), pNext[1].getName(), null, 0);

		//start the game
		gameStart();
		
		//check if the game is over
		if(isGameOver()) {
			System.out.println("Game over, Server closing!");
		}
		else {
			System.out.println("Server Error! Server closing!");
		}
		
		//close all the connections
		while(cons >= 0) {
			cons--;
			try {
				if(cons > socket.length || socket[cons] == null || socket[cons].isClosed() || !socket[cons].isConnected()) {
					continue;
				}
				socket[cons].close();
				socket[cons] = null;
			} catch (IOException e) {}
		}
	}
	
	//thread is called to handle chat with this for a given socket
	//@param - index : the index number within the socket array for which we handle chat messages
	private static void chatHandler(int index) {
		//see if there are chat messages
		try {
			CribChat msg = (CribChat)chatIn[index].readObject();
			//time stamp the message
			String time = new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis());
			msg.setTime(time);
			//send the message to everyone
			for(int ind = 0; ind < chatIn.length; ind++) {
				if(chatOut[ind] != null) {
					chatOut[ind].writeObject(msg);
					chatOut[ind].flush();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		//if we are still connected, recursive call
		if(chatSocket[index].isConnected()) {
			chatHandler(index);
		}
	}

	//to broadcast a game move to all users
	private static void broadcast(int type, String msg, Card[] cs) {
		for(int ind = 0; ind < outStrm.length; ind++) {
			clientMSG(type, msg, cs, ind);
		}
	}
	
	/* @description : send a game move to a specific client
	* @param - type : the message type (see CribMSG.java)
	*  @param - msg : the contents of the message
	*  @param - cs : any cards being sent to the user (such as on the deal)
	*  @param - sock : the socket of the user to send this message to
	*/
	private static void clientMSG(int type, String msg, Card[] cs, int sock) {
		if(sock < 0 || sock > socket.length || socket[sock] == null || outStrm[sock] == null) {	return;	}
		CribMSG send = new CribMSG(type, msg, cs);
		try {
			outStrm[sock].writeObject(send);
			outStrm[sock].flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//send out a score update
	private static void updateScores() {
		//update the scoreboard
		clientMSG(CribMSG.codeFor("SCORE"), pNext[0].getPoints() + "," + pNext[1].getPoints(), null, 0);
		clientMSG(CribMSG.codeFor("SCORE"), pNext[1].getPoints() + "," + pNext[0].getPoints(), null, 1);
	}
	
	//initializations for a game
	private static void setup() {
		for(int indS = 1; indS <= 4; indS++) {
			for(int indR = 1; indR <= 13; indR++) {
				deck[((indS-1)*13)+indR-1] = new Card(indS, indR);
			}
		}
		//after making the deck, shuffle thrice
		mergeShuffle();
		mergeShuffle();
		mergeShuffle();
		
		for(int ind = 0; ind < pegCards.length; ind++) {
			pegCards[ind] = null;
		}
		for(int ind = 0; ind < crib.length; ind++) {
			crib[ind] = null;
		}
		gameEnd = false;
		dealer = 0;
		pegInd = 0;
		pegCurrentCT = 0;
		upCard = null;
	}
	
	//make a new deck, clear everyones hands
	private static void resetDeck() {
		deck = new Card[52];
		crib = new Card[4];
		for(int i = 0; i < pNext.length; i++) {
			pNext[i].clearHand();
		}
		for(int indS = 1; indS <= 4; indS++) {
			for(int indR = 1; indR <= 13; indR++) {
				deck[((indS-1)*13)+indR-1] = new Card(indS, indR);
			}
		}
		//after making the deck, shuffle thrice
		cutShuffle();
		mergeShuffle();
		cutShuffle();
		mergeShuffle();
		mergeShuffle();
		
	}
	
	//start the round
	private static void gameStart() {
		System.out.println("Setting up...");
		setup();
		System.out.println("Starting Game");
		runRound();
		gameEnd = true;
	}
	
	//play the round (1 round includes pegging, counting hands, counting crib)
	//this is the heart of all the gameplay
	private static void runRound() {
		//give players cards
		deal();
		for(int ind = 0; ind < pNext.length; ind++) {
			clientMSG(CribMSG.codeFor("NEWHAND"), "", pNext[ind].getHand(), ind);
		}
		//ask them to throw to the crib
		broadcast(CribMSG.codeFor("MSG"), "Please select cards to throw to "+ pNext[dealer].getName() +"'s crib", null);
		broadcast(CribMSG.codeFor("CRIBRQST"), "", null);
		for(int ind = 0; ind < inStrm.length; ind++) {
			//get each players crib cards
			try {
				CribMSG msg;
				do {
					msg = (CribMSG)inStrm[ind].readObject();
					if(CribMSG.CODES[msg.getType()] == "CRIB") {
						break;
					}
				}while(true);
				Card[] toC = msg.getCards();
				//throw them in the crib, and remove them from the players hand
				for(int indC = 0; indC < toC.length; indC++) {
					System.out.println(toC[indC].toString() + " Thrown to crib by " + pNext[ind].getName());
					cardToCrib(toC[indC]);
					pNext[ind].removeCard(toC[indC]);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//once the crib cards have been thrown, start pegging
		startPegRound();
		int lastToPlay = -1;
		do {
			//ask for a card
			broadcast(CribMSG.codeFor("MSG"), " ", null);
			clientMSG(CribMSG.codeFor("MSG"), pNext[pTurn].getName() + ", please select a card to peg.", null, pTurn);
			clientMSG(CribMSG.codeFor("PEGRQST"), "", null, pTurn);
			try {
				CribMSG msg;
				do {
					//get a card and peg it
					msg = (CribMSG)inStrm[pTurn].readObject();
					if(CribMSG.CODES[msg.getType()] == "PEG") {
						Card t = msg.getCards()[0];
						lastToPlay = pTurn;
						pegCard(t);
						System.out.println(pNext[(pTurn+players-1)%pNext.length].getName() + " has pegged " + t.toString() + " " + pegCurrentCT);
						break;
					}
					else if(CribMSG.CODES[msg.getType()] == "GO") {
						//if they can't peg it's a go for the last person to play
						if(lastToPlay != -1 && lastToPlay != pTurn) {
							//this is a go
							pNext[lastToPlay].addPoints(1);
							broadcast(CribMSG.codeFor("MSG"), pNext[pTurn].getName() + " says GO!", null);
							updateScores();
							pTurn = (pTurn +1) % pNext.length;
						}
						else if(lastToPlay != -1 && lastToPlay == pTurn) {
							//we have come full circle in go's, reset to the next cycle
							pegCurrentCT = 0;
							currentPegRunStart = pegInd;
							pTurn = (pTurn +1) % pNext.length;
						}
						broadcast(CribMSG.codeFor("PEG"), pegCurrentCT + "", pegCards);
						break;
					}
				}while(true);		//keep going until no one can play and we break out
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			//check if game is over
			if(isGameOver()) {
				broadcast(CribMSG.codeFor("GAMEOVER"), "", null);
				return;
			}
		}while(pegInd < 8);	//end the big loop once all 8 cards have been played
		if(lastToPlay != -1) {
			//this is last card
			pNext[lastToPlay].addPoints(1);
			broadcast(CribMSG.codeFor("MSG"), "Last Card", null);
			updateScores();
		}
		endPegRound();
		//after pegging, count the hands
		countRound();
		if(isGameOver()) {
			broadcast(CribMSG.codeFor("GAMEOVER"), "", null);
			return;
		}
		//round is done, start the next round (recursive call)
		endRound();
		runRound();
	}
	
	//end a round of play
	private static void endRound() {
		resetDeck();
		broadcast(CribMSG.codeFor("PEG"), "0", null);
		broadcast(CribMSG.codeFor("UPCARD"), "", null);
		updateScores();
		//advance the deal
		dealer = (dealer +1) % pNext.length;
	}
	
	//check if the current game is over (if any player has reached the requisite 121 points
	private static boolean isGameOver() {
		for(int indP = 0; indP < pNext.length; indP++) {
			if(pNext[indP].getPoints() >= 121) {
				return true;
			}
		}
		return gameEnd;
	}
	
	//throw a card to the crib
	private static void cardToCrib(Card c) {
		Set<Card> sc = new HashSet<Card>();
		for(int indC = 0; indC < crib.length; indC++) {
			if(crib[indC] != null && sc.size() < 3) {
				sc.add(crib[indC]);
			}
		}
		sc.add(c);
		crib = sc.toArray(new Card[4]);
	}
	
	//start the pegging
	private static void startPegRound() {
		currentPegRunStart = 0;
		pegCurrentCT = 0;
		//send the upcard
		Card[] c = {upCard};
		broadcast(CribMSG.codeFor("UPCARD"), "", c);
		//if upCard is a Jack, award dealer 2 points
		if(upCard.getRank() == 11) {
			pNext[dealer].addPoints(2);
			broadcast(CribMSG.codeFor("MSG"), pNext[dealer].getName()+ " nobs", null);
			updateScores();
		}
		pTurn = (dealer +1) % pNext.length;
	}
	
	//counts / scores a card in pegging
	private static void pegCard(Card c) {
		if(c == null) {
			return;
		}
		//add card to peg[]
		if(pegInd < pegCards.length && pegCards[pegInd] == null) {
			pegCards[pegInd] = c;
			pegInd++;
			if(c.getRank() >= 10) {
				pegCurrentCT += 10;
			}
			else {
				pegCurrentCT += c.getRank();
			}
		}
		//check if last card incurred points
		//run
		int longestRun = 0;
		Set<Card> run = new HashSet<Card>();
		Set<Integer> ranks = new HashSet<Integer>();
		for(int ind = pegInd-1; ind >= currentPegRunStart; ind--) {
			if(ranks.contains(pegCards[ind].getRank())) {
				break;		//you cannot have multiple of the same rank in a run
			}
			ranks.add(pegCards[ind].getRank());
			run.add(pegCards[ind]);
			if(pNext[pTurn].isRun(run)) {		//cards do not have to be in correct order for a run
				longestRun = run.size();
			}
		}
		if(longestRun >= 3) {
			pNext[pTurn].addPoints(longestRun);
		}
		//pair(s)
		int pairs = 0;
		for(int ind = pegInd-2; ind >= currentPegRunStart; ind--) {
			if(c.getRank() == pegCards[ind].getRank()) {
				pairs++;
			}
			else {
				break;
			}
		}
		pNext[pTurn].addPoints(2*pairs);
		//15 or 31
		if(pegCurrentCT == 15) {
			pNext[pTurn].addPoints(2);
		}
		else if(pegCurrentCT == 31) {
			pNext[pTurn].addPoints(2);
			pegCurrentCT = 0;
			currentPegRunStart = pegInd;
		}
		pTurn = (pTurn +1) % pNext.length;
		updateScores();		//tell everyone the new scores
		broadcast(CribMSG.codeFor("PEG"), pegCurrentCT + "", pegCards);
	}
	
	//finish the pegging round
	private static void endPegRound() {
		for(int ind = 0; ind < pegCards.length; ind++) {
			if(pegCards[ind] != null) {
				pegCards[ind] = null;
			}
		}
		pegInd = 0;
		for(int indP = 0; indP < pNext.length; indP++) {
			clientMSG(CribMSG.codeFor("NEWHAND"), "", pNext[indP].getHand(), indP);
		}
		broadcast(CribMSG.codeFor("PEG"), "0", null);
	}
	
	//count all the hands and crib
	private static void countRound() {
		//count hands, starting after the dealer (dealer counts last)
		for(int ind = 1; ind <= pNext.length; ind++) {
			Player p = pNext[(dealer +ind) % pNext.length];
			int points = p.countHand(p.getHand(), upCard);
			p.addPoints(points);
			broadcast(CribMSG.codeFor("NEWHAND"), "", p.getHand());
			broadcast(CribMSG.codeFor("MSG"), p.getName()+ " Hand Scores "+ points, null);	//alert the points being scored
			updateScores();
			//wait for acknowledgement from each player before counting the next hand
			for(int indP = 0; indP < pNext.length; indP++) {
				CribMSG msg = null;
				do {
					try {
						msg = (CribMSG)inStrm[indP].readObject();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}while(msg == null || CribMSG.CODES[msg.getType()] != "ACK");
			}
		}
		//count the crib
		int points = pNext[dealer].countHand(crib, upCard);
		pNext[dealer].addPoints(points);
		broadcast(CribMSG.codeFor("NEWHAND"), "", crib);
		broadcast(CribMSG.codeFor("MSG"), pNext[dealer].getName()+ " Crib Scores "+ points, null);	//alert the points being scored
		updateScores();
		//wait for acknowledgement from each player before continuing
		for(int indP = 0; indP < pNext.length; indP++) {
			CribMSG msg = null;
			do {
				try {
					msg = (CribMSG)inStrm[indP].readObject();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}while(msg == null || CribMSG.CODES[msg.getType()] != "ACK");
		}
	}
	
	//discard all the cards back into the deck
	private static void discard(Card c) {
		if(c == null) {	return;	}
		for(int ind = 0; ind < deck.length; ind++) {
			if(deck[ind] == null) {
				deck[ind] = c;
				return;
			}
		}
	}
	
	//draw a card off the deck
	private static Card draw() {
		Card c = deck[0];
		deck[0] = null;
		for(int ind = 1; ind < deck.length; ind++) {
			deck[ind-1] = deck[ind];
			deck[ind] = null;
		}
		return c;
	}
	
	//deck shuffle method using a merge / riffle
	private static void mergeShuffle() {
		Card[][] split = cut(deck);		//cut the deck into 2 piles
		Stack<Card> a = new Stack<Card>();
		Stack<Card> b = new Stack<Card>();
		//add the 2 piles to a stack
		for(int i = 0; i < 26; i++) {
			a.push(split[0][i]);
			b.push(split[1][i]);
		}
		Card[] ret = new Card[52];
		//pick random cards to add to the return pile
		for(int ind = 0; ind < 52; ind++) {
			if((!a.empty() && Math.random() > 0.5) || b.empty()) {
				ret[ind] = a.pop();
			}
			else{
				ret[ind] = b.pop();
			}
		}
		deck = ret;
	}
	
	//takes a deck of cards and cuts it into 2 piles
	private static Card[][] cut(Card[] cs){
		Card[][] ret = new Card[2][cs.length/2]; 
		for(int indX = 0; indX < 2; indX++) {
			for(int indY = 0; indY < cs.length/2; indY++) {
				ret[indX][indY] = cs[(indX*(cs.length/2)) + indY];
			}
		}
		return ret;
	}
	
	//deck shuffle by cutting the deck and reversing random sections
	private static void cutShuffle() {
		//cut the deck into 4 pieces
		Card[][] splitA = cut(deck);
		Card[][] splitB1 = cut(splitA[0]);
		Card[][] splitB2 = cut(splitA[1]);
		Card[] ret = new Card[52];
		int retInd = 0;
		for(int indO = 0; indO < 4; indO++) {		//one of the 4 cuts
			int r = (int)(Math.random() %100);
			if(r < 50) { //random half
				//put the cards in order from a given cut (reversing the order of cuts)
				for(int indI = 0; indI < 13; indI++) {		//the cards in a cut
					if(indO < 2) {
						ret[retInd] = splitB2[indO][indI];
					}
					else {
						ret[retInd] = splitB1[indO-2][indI];
					}
					retInd++;
				}
			}
			else {
				//put the cards in backwards (keeping the order of cuts)
				for(int indI = 12; indI >= 0; indI--) {
					if(indO < 2) {
						ret[retInd] = splitB1[indO][indI];
					}
					else {
						ret[retInd] = splitB2[indO-2][indI];
					}
					retInd++;
				}
			}
		}
		deck = ret;		//return the "shuffled" deck
	}
	
	//deal the cards
	private static void deal(){
		//deal enough so that every player gets 4, and there are 4 extra for the crib (6ea for 2p, 5ea +1 for 3p)
		//give cards to each player
		int tempPT = pTurn;
		pTurn = (dealer +1) % pNext.length;
		for(int cardsToDeal = ((players * 4) + 4); cardsToDeal > 0; cardsToDeal--) {
			pNext[pTurn].addCard(draw());
			pTurn = (pTurn +1)  % pNext.length;
		}
		pTurn = tempPT;
		//turn up upCard
		upCard = draw();
	}
}
