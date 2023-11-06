package test;

import java.util.ArrayList;
import java.util.HashMap;

import mage.cards.decks.Deck;
import mage.game.Game;
import mage.players.Player;
import test.TestTreePlayer.NextAction;

public class SpanningTree {

	private TestNode root;
	private TestNode current;
	private HashMap<TestNode,ArrayList<TestNode>> tree;
	private TestGame rootGame;
	private TestTreePlayer player;
	private Player opponent;
	
	public SpanningTree(TestTreePlayer player1,Player player2) {
		// TODO Auto-generated constructor stub
		
		this.player = player1;
		this.opponent = player2;
		player.setTree(this);
		this.rootGame = new TestGame(this);
		player.init(rootGame,true);
		opponent.init(rootGame,true);
		rootGame.addPlayer(player,new Deck());
		rootGame.addPlayer(opponent,new Deck());
		
	}
	
	
	public TestTreePlayer getPlayer() {
		return player;
	}



	public void setPlayer(TestTreePlayer player) {
		this.player = player;
	}



	public void triggeringProcess(){
		player.setNextAction(NextAction.TRIGGERED);
		current.expand();
	}
	
	public void chooseProcess() {
		current.expand();
	}
	
	public void priorityProcess() {
		current.expand();	
	}
	
	public void nodeInit() {
		this.root = new TestNode(player.getId(),rootGame);
		this.current = this.root;
	}
	
	public void startSim() {
		rootGame.startSim(player.getId());
	}
	
	public TestNode getRoot() {
		return root;
	}

	public void setRoot(TestNode root) {
		this.root = root;
	}

	public TestNode getCurrent() {
		return current;
	}

	public void setCurrent(TestNode current) {
		this.current = current;
	}

	public HashMap<TestNode, ArrayList<TestNode>> getTree() {
		return tree;
	}
	

}
