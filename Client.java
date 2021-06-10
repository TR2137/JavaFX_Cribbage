import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javafx.application.*;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;

//Board Peg Hole Overlay :	https://dlpng.com/png/1560808
//Colored Board :			https://commons.wikimedia.org/wiki/File:Cribbage_Board.svg

public class Client extends Application{

	//private Image pegIMG;
	private static Image cardBackIMG;
	private static Image[][] cards = {};
	private static int sizeX = 1000;			//width of window
	private static int sizeY = 600;				//height of window
	//sockets, streams for game data and chat
	private static Socket socket = null;
	private static ObjectInputStream inStream = null;
	private static ObjectOutputStream outStream = null;
	private static Socket chatSocket = null;
	private static ObjectInputStream chatIn = null;
	private static ObjectOutputStream chatOut = null;
	//player data, game data
	private static Card[] p1Hand = {};
	private static boolean[] selected = {false, false, false, false, false, false};
	private static Card[] pegged = {};
	private static int peggedCT = 0;
	private static Card upCard = null;
	private static String p1Name = "Player 1";
	private static String p2Name = "Player 2";
	private static Menu p1Score;
	private static Menu p2Score;
	private static Menu pegScore;
	private static int p1Points = 0;
	private static int p2Points = 0;
	//GUI stuff
	//private static Label lab;
	//private Button butt;
	private static Menu lab;
	private static TextArea ta = new TextArea();
	private static Canvas canvas;
	private static PointMap[][] pegMap = new PointMap[2][60];
	private static PointMap pegOut;
	//state machine
	private static double state = 0.0;
	
	
	public static void main(String[] args){
		//game server
		do {
			try {
				socket = new Socket("127.0.0.1", 2137);
			}catch(IOException ex) {
				ex.printStackTrace();
			}
		}while(socket == null || !socket.isConnected());
		System.out.println("Client Connected!");
		state = 0.1;

		try {
			inStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			outStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			outStream.flush();
			System.out.println("Streams Initialized!");
			state = 0.2;
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Thread(new Runnable() {
			@Override public void run() {
				//System.out.println("Starting message thread...");
				messageChecker();
				}
			}).start();
		//chat server
		try {
			chatSocket = new Socket("127.0.0.1", 7321);
			System.out.println("Chat Connected!");

		}catch(IOException ex) {
			ex.printStackTrace();
		}
		try {
			chatIn = new ObjectInputStream(new BufferedInputStream(chatSocket.getInputStream()));
			chatOut = new ObjectOutputStream(new BufferedOutputStream(chatSocket.getOutputStream()));
			chatOut.flush();
			System.out.println("Chat Initialized!");
			state = 0.3;
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Thread(new Runnable() {
			@Override public void run() {
				//System.out.println("Starting message thread...");
				chatChecker();
				}
			}).start();
		
		launch(args);
	}

	//javaFX function to setup the GUI windows
	public void start(Stage primaryStage) throws Exception{
		loadIMG();			//start loading the pictures to show the cards
		buildMap();			//set up the gui point map
		state = 0.4;
		VBox root = new VBox();
		buildMenuBar(root, primaryStage);
		
		canvas = new Canvas(sizeX, sizeY);
		//canvas.setOnMousePressed((event) -> System.out.println("" + event.getX() + ", "+ event.getY()));
		canvas.setOnMousePressed((event)->process(event.getX(), event.getY()));
		draw();
		
		root.getChildren().add(canvas);
		
		Scene scene = new Scene(root, sizeX-20, sizeY);
		primaryStage.setTitle("Cribbage by Travis Reese");
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.show();
		chatStage();			//creates the window for chat
		playerNameStage();		//creates the window to get the players name
	}

	//draws everything on the canvas
	private static void draw() {
		drawBackground(canvas);
		drawBoard(canvas);
		drawOpponent(canvas);
		drawPegged(canvas);
		drawPlayer(canvas);
		p1Score.setText(p1Name + " : " + p1Points);
		p2Score.setText(p2Name + " : " + p2Points);
		pegScore.setText("Pegged : " + peggedCT);
	}
	
	//process the user input (card sends or plays, score ack, gameover ack)
	private void process(double x, double y) {
		if(x >= 875 && x <= 965 && y >= 525 && y <= 560) {
			//this is a button press
			//System.out.println("button");
			CribMSG msg = new CribMSG();
			if(state == 1.0) {
				//CRIBRQST
				Card[] temp = {null, null};
				int[] tmpind = {-1, -1};
				for(int ind = 0; ind < selected.length; ind++) {
					if(selected[ind]) {
						if(tmpind[0] == -1) {
							tmpind[0] = ind;
						}
						else if(tmpind[1] == -1) {
							tmpind[1] = ind;
						}
						else {
							return;
						}
					}
				}
				if(tmpind[0] != -1 && tmpind[1] != -1) {
					temp[1] = pull(p1Hand, tmpind[1]);
					temp[0] = pull(p1Hand, tmpind[0]);
					msg.setCards(temp);
					msg.setType(CribMSG.codeFor("CRIB"));
					try {
						outStream.writeObject(msg);
						outStream.flush();
						//System.out.println("CRIB");
						cardsDown();
						state = 1.1;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			}
			else if(state == 1.2) {
				//PEGRQST
				if(!pegCheck()) {
					//no valid cards to peg
					msg.setType(CribMSG.codeFor("GO"));
					try {
						outStream.writeObject(msg);
						outStream.flush();
						//System.out.println("GO");
						state = 1.3;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					int idx = -1;
					for(int ind = 0; ind < selected.length; ind++) {
						if(selected[ind] && idx == -1) {
							idx = ind;
						}
						else if(selected[ind]) {
							idx = -2;
						}
					}
					if(idx >= 0) {
						if(validPeg(p1Hand[idx])) {
							Card[] temp = {pull(p1Hand, idx)};
							msg.setCards(temp);
							msg.setType(CribMSG.codeFor("PEG"));
							try {
								outStream.writeObject(msg);
								outStream.flush();
								//System.out.println("PEG");
								cardsDown();
								state = 1.3;
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						else {
							selected[idx] = false;
						}
					}
				}
			}
			else {
				msg.setType(CribMSG.codeFor("ACK"));
				try {
					outStream.writeObject(msg);
					outStream.flush();
					//System.out.println("ACK");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			cardsDown();
		}
		else if(x >= sizeX/3 && x <= (sizeX/3 + p1Hand.length*75) && y >= sizeY*0.7 && y <= (sizeY*0.7 + 109)) {
			//selecting cards in hand
			int cardIndex = (int)((x - sizeX/3)/75);
			if(p1Hand[cardIndex] == null) {
				selected[cardIndex] = false;
			}
			else {
				select(cardIndex);
			}
			//System.out.println("card " + cardIndex);
		}
		else {
			//clicking on something that doesn't have a function
			//System.out.println(x + ", " + y);
		}
		draw();
	}
	
	//flips all the cards face down
	private void cardsDown() {
		for(int ind = 0; ind < selected.length; ind++) {
			selected[ind] = false;
		}
	}
	
	//selects a card at the index (x)
	private void select(int x) {
		if(x < 0 || x > selected.length || x > p1Hand.length) {
			return;
		}
		int ct = 0;
		for(int ind = 0; ind < selected.length; ind++) {
			if(selected[ind]) {
				ct++;
			}
		}
		if(ct > 1) {
			selected[x] = false;
		}
		else {
			selected[x] = !selected[x];
		}
	}

	//creates the window to get a players name
	private void playerNameStage() {
		Stage namePrompt = new Stage();
		namePrompt.setTitle("Player Name");
		VBox vb = new VBox();
		vb.setPadding(new Insets(10, 10, 10, 10));
		HBox hb = new HBox();
		hb.setPadding(new Insets(2, 2, 2, 2));
		Label lab1 = new Label("Enter Your Name:  ");
		Label lab2 = new Label("Letters and Numbers only (1-16)");
		TextField tf1 = new TextField();
		Button b1 = new Button("SAVE");
		b1.setDefaultButton(true);
		b1.setOnAction((event) -> b1Press(tf1.getCharacters(), namePrompt));
		hb.getChildren().addAll(tf1, b1);
		vb.getChildren().addAll(new Label(""), lab1, hb, lab2);
		Scene scenePN = new Scene(vb, 275, 150);
		namePrompt.setScene(scenePN);
		namePrompt.setResizable(false);
		namePrompt.setAlwaysOnTop(true);
		namePrompt.show();
	}
	
	//the button function for name window
	private void b1Press(CharSequence str, Stage s) {
		String pn = cleanName(str);
		if(pn.length() > 0 && pn.length() < 16) {
			updatePlayerName(pn);
			s.close();
		}
		if(state < 1) {		//only if game is not started
			state = 0.7;
		}
	}
	
	//the window for chats
	private void chatStage() {
		Stage chatW = new Stage();
		chatW.setTitle("Cribbage Game Chat");
		VBox vb = new VBox();
		HBox hb = new HBox();
		
		TextField tf2 = new TextField();
		Button b2 = new Button("SEND");
		ta.setPrefSize(445, 275);
		ta.setWrapText(true);
		ta.setEditable(false);
		tf2.setPrefSize(385, 35);
		b2.setPrefSize(75, 35);
		b2.setDefaultButton(true);
		b2.setOnAction((event)->{
			sendchat(tf2.getCharacters());
			tf2.setText("");
			});
		hb.getChildren().addAll(tf2, b2);
		vb.getChildren().addAll(ta, hb);
		Scene sceneChat = new Scene(vb, 450, 300);
		chatW.setScene(sceneChat);
		chatW.setResizable(false);
		chatW.show();
	}
	
	//write a chat message to the network
	private void sendchat(CharSequence str) {
		CribChat msg = new CribChat(p1Name, str.toString());
		try {
			chatOut.writeObject(msg);
			chatOut.flush();
		} catch (Exception e) {}
		
	}
	
	//checks if new chats have been sent
	private static void chatChecker() {
		try {
			CribChat msg = (CribChat)chatIn.readObject();
			String toAdd = "\n" + msg.getWhoFrom() + " (" + msg.getTime() + "): " + msg.getMsg();
			ta.setText(ta.getText() + toAdd);
		} catch (Exception e) {
			return;
		}
		if(chatSocket.isConnected()) {
			chatChecker();
		}
	}
	
	//makes sure the name given only has valid characters
	private String cleanName(CharSequence str) {
		String st = str.toString();
		//System.out.println(st);
		String ret = "";
		for(int ind = 0; ind < st.length(); ind++) {
			char ch = st.charAt(ind);
			if((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
				ret += ch;
			}
		}
		return ret;
	}
	
	//tells the server a new name for this player
	private void updatePlayerName(String name) {
		CribMSG msg = new CribMSG(CribMSG.codeFor("NAME"), name, null);
		p1Name = name;
		p1Score.setText(p1Name + ": " + p1Points);
		//System.out.println(name);
		//send the message to the server
		try {
			outStream.writeObject(msg);
			outStream.flush();
			System.out.println("NAME");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//puts all the menus on the top of the main window
	private void buildMenuBar(VBox r, Stage ms) {
		MenuBar menuBar = new MenuBar();
		Menu menu = new Menu("Options");
		
		//closes the window
		MenuItem quitItem = new MenuItem("Quit");
		quitItem.setOnAction((event) -> ms.close());
		menu.getItems().add(quitItem);
		
		//opens the window to set name
		MenuItem nameItem = new MenuItem("Name");
		nameItem.setOnAction((event) -> playerNameStage());
		menu.getItems().add(nameItem);
		
		//opens the chat window
		MenuItem chatItem = new MenuItem("Chat");
		chatItem.setOnAction((event) -> chatStage());
		menu.getItems().add(chatItem);
		
		//Scores
		pegScore = new Menu("Pegged : " + peggedCT);
		p1Score = new Menu(p1Name + " : " + p1Points);
		p2Score = new Menu(p2Name + " : " + p2Points);
		lab = new Menu("Welcome!");

		
		menuBar.getMenus().add(menu);
		menuBar.getMenus().add(pegScore);
		menuBar.getMenus().add(p1Score);
		menuBar.getMenus().add(p2Score);
		menuBar.getMenus().add(lab);
		r.getChildren().add(menuBar);
	}
	
	//draws the background on the given canvas
	//do this before drawing the cards or board
	private static void drawBackground(Canvas cv) {
		GraphicsContext g = cv.getGraphicsContext2D();
		g.setFill(Color.GREEN);
		g.fillRect(0, 0, cv.getWidth(), cv.getHeight());
		g.setFill(Color.BLACK);
		g.fillRect(sizeX-125, sizeY-75, 90, 35);
		g.setFill(Color.DARKGRAY);
		g.fillRect(sizeX-124, sizeY-74, 88, 33);
		g.setFill(Color.LIGHTGRAY);
		g.fillRect(sizeX-121, sizeY-71, 82, 27);
		g.setFill(Color.BLACK);
		g.fillText("SEND", sizeX-96, sizeY-52, 40);
	}
	
	//draws the board with pegs
	private static void drawBoard(Canvas cv) {
		GraphicsContext g = cv.getGraphicsContext2D();
		g.setFill(Color.TAN);
		g.fillRect(20, 25, sizeX/6, sizeY-60);
		//g.drawImage(pegIMG, 20, 25, sizeX/6, sizeY-60);
		if(upCard != null) {
			g.drawImage(cards[upCard.getSuit()-1][upCard.getRank()-1], sizeX/5, (sizeY/2-80), 90, 130);
		}
		else {
			g.drawImage(cardBackIMG, sizeX/5, (sizeY/2-80), 90, 130);
		}
		drawPegHoles(cv);
		drawPegs(cv);
	}
	
	//draws the pegs in the correct point positions
	private static void drawPegs(Canvas cv) {
		GraphicsContext g = cv.getGraphicsContext2D();
		g.setFill(Color.CRIMSON);
		if(p1Points >= 121 || p1Points < 1) {
			g.fillRoundRect(pegOut.getX(), pegOut.getY(), 4, 7, 2, 2);
		}
		else {
			g.fillRoundRect(pegMap[0][(p1Points-1)%60].getX(), pegMap[0][(p1Points-1)%60].getY()-21, 5, 25, 5, 18);
		}
		g.setFill(Color.ROYALBLUE);
		if(p2Points >= 121 || p2Points < 1) {
			g.fillRoundRect(pegOut.getX(), pegOut.getY()-21, 5, 25, 5, 18);
		}
		else {
			g.fillRoundRect(pegMap[1][(p2Points-1)%60].getX(), pegMap[1][(p2Points-1)%60].getY()-21, 5, 25, 5, 18);
		}
	}
	
	//draws the peg holes on the board to put the pegs in
	private static void drawPegHoles(Canvas cv) {
		GraphicsContext g = cv.getGraphicsContext2D();
		g.setFill(Color.BLACK);
		for(int ind = 0; ind < 60; ind++) {
			g.strokeOval(pegMap[0][ind].getX(), pegMap[0][ind].getY(), 5, 5);
			g.strokeOval(pegMap[1][ind].getX(), pegMap[1][ind].getY(), 5, 5);
		}
		g.strokeOval(pegOut.getX(), pegOut.getY(), 5, 5);
	}
	
	//figures out and builds a map grid for the pegs
	private void buildMap() {
			int minX = 40;
			int minY = 60;
			int gapX = (sizeX/6-60)/7;
			int gapY = (sizeY-130)/66;
			int vind = 0;
			for(int indA = 0; indA < 6; indA++) {
				for(int indB = 0; indB < 11; indB++) {
					if(indB%2 == 0 && indB != 10) {
						pegMap[0][vind] = new PointMap(gapX*7 + minX, gapY*(indA*11 + indB) + minY);		//right column, player1
						pegMap[0][59-vind] = new PointMap(gapX + minX, gapY*(indA*11 + indB) + minY);		//left column, player1
						pegMap[1][vind] = new PointMap(gapX*6 + minX, gapY*(indA*11 + indB) + minY);		//right column, player2
						pegMap[1][59-vind] = new PointMap(gapX*2 + minX, gapY*(indA*11 + indB) + minY);		//left column, player2
						vind++;
					}
				}
			}
			pegOut = new PointMap(gapX*4 + minX, (sizeY-130)/2 + minY);
	}
	
	//draws all the player's cards
	private static void drawPlayer(Canvas cv) {
		GraphicsContext g = cv.getGraphicsContext2D();
		if(p1Hand == null) {
			//p1Hand = new Card[6];
			return;
		}
		if(p1Hand == null || cards == null || selected == null) {
			return;
		}
		for(int ind = 0; ind < p1Hand.length; ind++) {
			if(p1Hand[ind] == null) {
				continue;
			}
			if(ind < selected.length && selected[ind]) {
				g.drawImage(cards[p1Hand[ind].getSuit()-1][p1Hand[ind].getRank()-1], sizeX/3 + 75*ind, sizeY*0.7-20, 75, 109);
				//g.drawImage(cardBackIMG, sizeX/3 + 75*ind, sizeY*0.7-20, 75, 109);
			}
			else {
				g.drawImage(cards[p1Hand[ind].getSuit()-1][p1Hand[ind].getRank()-1], sizeX/3 + 75*ind, sizeY*0.7, 75, 109);
				//g.drawImage(cardBackIMG, sizeX/3 + 75*ind, sizeY*0.7, 75, 109);
			}
		}
	}
	
	//draw the other persons cards
	private static void drawOpponent(Canvas cv) {
		GraphicsContext g = cv.getGraphicsContext2D();
		g.drawImage(cardBackIMG, sizeX/3 +25, 35, 75, 109);
		g.drawImage(cardBackIMG, sizeX/3 +75, 35, 75, 109);
		g.drawImage(cardBackIMG, sizeX/3 +125, 35, 75, 109);
		g.drawImage(cardBackIMG, sizeX/3 +175, 35, 75, 109);
	}
	
	//draw the cards during the pegging round
	private static void drawPegged(Canvas cv) {
		GraphicsContext g = cv.getGraphicsContext2D();
		if(pegged == null) {	
			//pegged = new Card[8];
			return;
		}
		for(int ind = 0; ind < pegged.length; ind++) {
			if(pegged == null || cards == null || pegged[ind] == null) {
				continue;
			}
			g.drawImage(cards[pegged[ind].getSuit()-1][pegged[ind].getRank()-1], (sizeX/3) + (65*ind), (sizeY/2) -65, 75, 109);
			//g.drawImage(cardBackIMG, sizeX/3 + 65*ind, sizeY/2 -65, 75, 109);
		}
	}
	
	//checks for game related messages from the server
	private static void messageChecker() {
		try {
			//System.out.println("reading message ");
			CribMSG msg = (CribMSG)inStream.readObject();
			Platform.runLater(new Runnable() {
				@Override public void run() {
					handleMessage(msg);
					}
				});
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		/*AnimationTimer at = new AnimationTimer() {
			@Override
			public void handle(long time) {}
		};
		at.handle(1000);
		at.start();
		
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}*/
		if(socket != null && socket.isConnected()) {
			messageChecker();
		}
	}
	
	//figures out what to do with a game message
	private static void handleMessage(CribMSG cm) {
		String code = CribMSG.CODES[cm.getType()];
		//System.out.println("Received: " + code);
		switch(code) {
			case "MSG" : 
				lab.setText(cm.getMessage());
				break;
			case "SCORE" :
				int[] scrs = scoreParser(cm.getMessage());
				p1Points = scrs[0];
				p2Points = scrs[1];
				p1Score.setText(p1Name + " : " + p1Points);
				p2Score.setText(p2Name + " : " + p2Points);
				break;
			case "NEWHAND" :
				p1Hand = cm.getCards();
				break;
			case "PEGRQST" :
				state = 1.2;
				break;
			case "CRIBRQST" :
				state = 1.0;
				break;
			case "PLAY":
				//should never be sent to the client
				break;
			case "CRIB" : 
				//should never be sent to the client
				break;
			case "UPCARD" :
				if(cm.getCards() != null) {
					upCard = cm.getCards()[0];
				}
				else {
					upCard = null;
				}
				break;
			case "NAME" : 
				//assume it's the other players name
				p2Name = cm.getMessage();
				p2Score.setText(p2Name + ": " + p2Points);
				break;
			case "GAMEOVER" :
				lab.setText("Game Over!");
				break;
			case "PEG" :
				pegScore.setText("Pegged : " + peggedCT);
				pegged = cm.getCards();
				peggedCT = stringToInt(cm.getMessage());
				break;
			case "ACK" :
				break;
			case "GO" :
				//should never be sent to the client
				break;
		}
		draw();		//redraw
	}
	
	//safely converts a string to integer
	private static int stringToInt(String st) {
		int ret = 0;
		for(int ind = 0; ind < st.length(); ind++) {
			char ch = st.charAt(ind);
			if(ch >= '0' && ch <= '9') {
				ret *=10;
				ret += (int)(ch - '0');
			}
		}
		return ret;
	}
	
	//safely gets score info into integer array
	private static int[] scoreParser(String st) {
		int[] ret = {0, 0};
		int index = 0;
		for(int ind = 0; ind < st.length(); ind++) {
			char ch = st.charAt(ind);
			if(ch >= '0' && ch <= '9') {
				ret[index] *=10;
				ret[index] += (int)(ch- '0');
			}
			else if(ch == ',') {
				index++;
			}
		}
		return ret;
	}
	
	//checks to see if any card can be pegged (other wise it's a go)
	private boolean pegCheck() {
		for(int ind = 0; ind < p1Hand.length; ind++) {
			if(p1Hand[ind] == null) {
				continue;
			}
			else if(validPeg(p1Hand[ind])) {
				return true;
			}
		}
		return false;
	}
	
	//helper method from pegCheck
	//check if this card can be pegged
	private boolean validPeg(Card c) {
		if(c == null) {
			return false;
		}
		if(c.getRank() > 10 && (peggedCT+10) <= 31) {
			return true;
		}
		else if((c.getRank() + peggedCT) <= 31) {
			return true;
		}
		return false;
	}
	
	//gets the card out of hand from a given index
	private Card pull(Card[] cs, int index) {
		if(index < 0 || index > cs.length) {
			return null;
		}
		Card c = cs[index];
		cs[index] = null;
		for(int ind = index; ind < cs.length-1; ind++) {
			if(cs[ind+1] == null) {
				break;
			}
			cs[ind] = cs[ind+1];
			cs[ind+1] = null;
		}
		return c;
	}
	
	//gets the pictures buffered and ready to draw on screen
	private void loadIMG(){
		try {
			//load images
		//	pegIMG = new Image("file:///C:/Users/treese/Dropbox/Home%20Share/NMU/Class/CS480%20S19%20Cribbage/IMG/peg_holes.png", false);
			cardBackIMG = new Image("http://euclid.nmu.edu/~treese/IMG/back.jpg", false);
			cards = new Image[4][13];
			for(int suit = 0; suit < 4; suit++){
				for(int rank = 0; rank < 13; rank++){
					String fn = Card.getCardFileName(suit+1, rank+1);
					fn = "http://euclid.nmu.edu/~treese/IMG/" + fn;
					cards[suit][rank] = new Image(fn, false);
					//System.out.println(cards[suit][rank].getProgress());
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		//System.out.println(pegIMG.getProgress());
		//System.out.println(cardBackIMG.getProgress());
	}

}
