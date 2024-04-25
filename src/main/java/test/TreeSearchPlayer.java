package test;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.Mode;
import mage.abilities.Modes;
import mage.abilities.TriggeredAbility;
import mage.abilities.common.PassAbility;
import mage.abilities.costs.VariableCost;
import mage.abilities.costs.mana.ManaCost;
import mage.cards.Card;
import mage.cards.Cards;
import mage.cards.decks.Deck;
import mage.choices.Choice;
import mage.constants.MultiAmountType;
import mage.constants.Outcome;
import mage.constants.RangeOfInfluence;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.combat.CombatGroup;
import mage.game.draft.Draft;
import mage.game.match.Match;
import mage.game.permanent.Permanent;
import mage.game.tournament.Tournament;
import mage.players.Player;
import mage.players.PlayerImpl;
import mage.players.net.UserData;
import mage.players.net.UserGroup;
import mage.target.Target;
import mage.target.TargetAmount;
import mage.target.TargetCard;
import test.TestTreePlayer.NextAction;

public class TreeSearchPlayer extends PlayerImpl { 

	
	private SpanningTree tree;
	protected PassAbility pass = new PassAbility();
	protected transient TestNode root;
	private int loopCheckCounter = 0;
	
	public TreeSearchPlayer(String name) {
		super(name, RangeOfInfluence.ALL);
		this.pass.setControllerId(this.getId());
		human = false;
		this.setTestMode(true);
		userData = UserData.getDefaultUserDataView();
        userData.setAvatarId(64);
        userData.setGroupId(UserGroup.COMPUTER.getGroupId());
        userData.setFlagName("computer.png");
	}
	
	public TreeSearchPlayer(final TreeSearchPlayer player) {
		super(player);
		this.tree = player.tree;
		this.pass.setControllerId(this.getId());
		human = false;
		this.root = player.root;
		this.userData = player.userData;
	}
	
//	public TreeSearchPlayer(PlayerImpl player) {
//		super(player);
//		// TODO Auto-generated constructor stub
//	}

	@Override
	public void abort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void skip() {
		// TODO Auto-generated method stub

	}

	@Override
	public Player copy() {
		// TODO Auto-generated method stub
		return new TreeSearchPlayer(this);
	}

	@Override
	public boolean priority(Game game) {
		// TODO Auto-generated method stub
		
        game.getState().setPriorityPlayerId(playerId);
        game.firePriorityEvent(playerId);
        getNextAction(game, NextAction.PRIORITY);
        Ability ability = root.getAction();
        if (ability == null)
            System.out.println("null ability");
        activateAbility((ActivatedAbility) ability, game);
        if (ability instanceof PassAbility)
            return false;
        return true;
	}

	private void getNextAction(Game game, NextAction nextAction) {
		// TODO Auto-generated method stub
		 if (root != null) {
	            TestNode newRoot;
	            newRoot = root.getMatchingState(game.getState().getValue(game, playerId));
	            if (newRoot != null) {
	                newRoot.emancipate();
	                loopCheckCounter++;
	            } else {
	            	System.out.println("no matching state");
	            }
	            root = newRoot;
		 }
		 calculateActions(game, nextAction);
	}
	
	protected void calculateActions(Game game, NextAction action) {
        if (root == null) {
            TestGame sim = createTestGame(game);
            TestTreePlayer player = (TestTreePlayer) sim.getPlayer(playerId);
            player.setNextAction(action);
            root = new TestNode(playerId, sim);
        }
        applyMCTS(game, action);
        root.emancipate();
    }
	
    protected TestGame createTestGame(Game game) {
        TestGame sim = ((TestGame)game).copy();
        System.out.println(game.getPhase().getStep().getStepPart());
        System.out.println(sim.getPhase().getStep().getStepPart());
        
        for (Player copyPlayer : sim.getState().getPlayers().values()) {
        	if(copyPlayer == null) {
        		System.out.println("check copy player");
        	}
        	System.out.println(copyPlayer.getId());
            Player origPlayer = game.getState().getPlayers().get(copyPlayer.getId());
            if(copyPlayer.getId().equals(playerId)) {
            	 TestTreePlayer newPlayer = new TestTreePlayer(copyPlayer.getId());
                 
                 newPlayer.restore(origPlayer);
                 sim.getState().getPlayers().put(copyPlayer.getId(), newPlayer);
            }
            else {
            	AlwaysPassPlayer opponent = new AlwaysPassPlayer(copyPlayer.getId());
            	opponent.restore(origPlayer);
                sim.getState().getPlayers().put(copyPlayer.getId(), opponent);
            }
//            TestTreePlayer newPlayer = new TestTreePlayer(copyPlayer.getId());
//            
//            newPlayer.restore(origPlayer);
//            if (!newPlayer.getId().equals(playerId)) {
//                int handSize = newPlayer.getHand().size();
//                newPlayer.getLibrary().addAll(newPlayer.getHand().getCards(sim), sim);
//                newPlayer.getHand().clear();
//                newPlayer.getLibrary().shuffle();
//                for (int i = 0; i < handSize; i++) {
//                    Card card = newPlayer.getLibrary().removeFromTop(sim);
//                    card.setZone(Zone.HAND, sim);
//                    newPlayer.getHand().add(card);
//                }
//            } else {
//                newPlayer.getLibrary().shuffle();
//            }
//            sim.getState().getPlayers().put(copyPlayer.getId(), newPlayer);
        }
        System.out.println(game.getPhase().getStep().getStepPart());
        System.out.println(sim.getPhase().getStep().getStepPart());
       
        sim.setSimulation(true);
        sim.resume();
        return sim;
    }
	
	private void applyMCTS(Game game, NextAction action) {
		// TODO Auto-generated method stub
		int thinkTime = 0;
		TestNode current;
		while(thinkTime < 50) {
			
			current = root;

//            // Selection
//            while (!current.isLeaf()) {
//                current = current.select(this.playerId);
//            }

            int result;
            if (!current.isTerminal()) {
                System.out.println("Attempt : "+ thinkTime);
            	// Expansion
                current.expand();
                System.out.println("----------------------------------");
                // Simulation
//                current = current.select(this.playerId);
//                result = current.simulate(this.playerId);
                thinkTime++;
            } else {
                break;
            }
            // Backpropagation
//            current.backpropagate(result);
        }
		game.end();
		
	}

	@Override
	public boolean choose(Outcome outcome, Target target, UUID sourceId, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean choose(Outcome outcome, Target target, UUID sourceId, Game game, Map<String, Serializable> options) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean choose(Outcome outcome, Cards cards, TargetCard target, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean chooseTarget(Outcome outcome, Target target, Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean chooseTarget(Outcome outcome, Cards cards, TargetCard target, Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean chooseTargetAmount(Outcome outcome, TargetAmount target, Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean chooseMulligan(Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean chooseUse(Outcome outcome, String message, Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean chooseUse(Outcome outcome, String message, String secondMessage, String trueText, String falseText,
			Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean choose(Outcome outcome, Choice choice, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean choosePile(Outcome outcome, String message, List<? extends Card> pile1, List<? extends Card> pile2,
			Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean playMana(Ability ability, ManaCost unpaid, String promptText, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int announceXMana(int min, int max, int multiplier, String message, Game game, Ability ability) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int announceXCost(int min, int max, String message, Game game, Ability ability, VariableCost variableCost) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int chooseReplacementEffect(Map<String, String> abilityMap, Game game) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public TriggeredAbility chooseTriggeredAbility(List<TriggeredAbility> abilities, Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Mode chooseMode(Modes modes, Ability source, Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void selectAttackers(Game game, UUID attackingPlayerId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectBlockers(Ability source, Game game, UUID defendingPlayerId) {
		// TODO Auto-generated method stub

	}

	@Override
	public UUID chooseAttackerOrder(List<Permanent> attacker, Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UUID chooseBlockerOrder(List<Permanent> blockers, CombatGroup combatGroup, List<UUID> blockerOrder,
			Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void assignDamage(int damage, List<UUID> targets, String singleTargetName, UUID attackerId, Ability source,
			Game game) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getAmount(int min, int max, String message, Game game) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Integer> getMultiAmount(Outcome outcome, List<String> messages, int min, int max, MultiAmountType type,
			Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sideboard(Match match, Deck deck) {
		// TODO Auto-generated method stub

	}

	@Override
	public void construct(Tournament tournament, Deck deck) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pickCard(List<Card> cards, Deck deck, Draft draft) {
		// TODO Auto-generated method stub

	}

}
