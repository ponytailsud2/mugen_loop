package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mage.MageItem;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.Mode;
import mage.abilities.TriggeredAbility;
import mage.game.ExileZone;
import mage.game.Game;
import mage.game.GameState;
import mage.game.permanent.Permanent;
import mage.game.stack.StackObject;
import mage.player.ai.MCTSNode;
import mage.player.ai.MCTSPlayer;
import mage.players.Player;
import mage.target.Target;
import mage.util.ThreadLocalStringBuilder;

public class TestNode {
	
	private static final ThreadLocalStringBuilder threadLocalBuilder = new ThreadLocalStringBuilder(1024);
	public static final boolean USE_ACTION_CACHE = false;
	private TestNode parent;
	private final List<TestNode> children = new ArrayList<TestNode>();
	private Ability action;
	private Game game;
	private final String stateValue;
	private UUID playerId;
	private boolean terminal = false;
	private ArrayList<String> parentStates;
	private int tappedPermanentCount = 0;
	private int handCount = 0;
	private int loopStepCount = 0;
	
	//
	private Stack<MageItem> availableTarget = new Stack<MageItem>();
	
	//use String gameState.getValue as gameState
	public TestNode(UUID playerId,Game game) {
		this.game = game;
		this.stateValue = this.getGameStateValue(game);
		this.playerId = playerId;
		this.terminal = game.getPlayer(playerId).getPlayable(game, true).isEmpty();
		this.parentStates = new ArrayList<String>();
		// TODO Auto-generated constructor stub
	}
	
	public TestNode(TestNode parent, Game game, Ability action) {
		this.playerId = parent.playerId;
		this.game = game;
		this.stateValue = this.getGameStateValue(game);
		this.terminal = game.getPlayer(playerId).getPlayable(game, true).isEmpty();
		this.parent = parent;
		this.action = action;
		
		this.parentStates = parent.parentStates;
		this.parentStates.add(parent.getGameStateValue(parent.game));
		// TODO Auto-generated constructor stub
	}
	
	public String getGameStateValue(Game game) {
		StringBuilder state = threadLocalBuilder.get();
		GameState gs = game.getState();
		state.append(gs.getTurn().getValue(gs.getTurnNum()));
		for(Player player : gs.getPlayers().values()) {
			if(player.getId() == playerId) {
				state.append("player").append(player.isPassed()).append(player.getLife()).append("hand");
				state.append(player.getHand().getValue(game));
			}
			state.append("library").append(player.getLibrary().size());
			state.append("grayard");
			state.append(player.getGraveyard().getValue(game));
		}
		
		state.append("permanents");
		List<String> perms = new ArrayList<String>();
		for (Permanent permanent: gs.getBattlefield().getAllActivePermanents()) {
			perms.add(permanent.getValue(gs));
		}
		Collections.sort(perms);
		state.append(perms);
		
		state.append("spells");
		for(StackObject spell : gs.getStack()) {
			state.append(spell.getControllerId()).append(spell.getName());
            state.append(spell.getStackAbility().toString());
            for (UUID modeId : spell.getStackAbility().getModes().getSelectedModes()) {
                Mode mode = spell.getStackAbility().getModes().get(modeId);
                if (!mode.getTargets().isEmpty()) {
                    state.append("targets");
                    for (Target target : mode.getTargets()) {
                        state.append(target.getTargets());
                    }
                }
            }
		}
		
		 for (ExileZone zone : gs.getExile().getExileZones()) {
	            state.append("exile").append(zone.getName()).append(zone.getValue(game));
	        }
		
		
		return state.toString();
	}
	
	//expand
	public void expand() {
		TestTreePlayer player = (TestTreePlayer) game.getPlayer(playerId);
		if(player.getNextAction() == null) {
			
		}
		switch (player.getNextAction()) {
		case PRIORITY:
			List<Ability> abilities;
			if (!USE_ACTION_CACHE)
				abilities = player.getPlayableOptions(game);
			else
				abilities = getPlayables(player, stateValue, game);
			for (Ability ability: abilities) {
				Game sim = game.copy();
//       	     logger.info("expand " + ability.toString());
				TestTreePlayer simPlayer = (TestTreePlayer) sim.getPlayer(player.getId());
				simPlayer.activateAbility((ActivatedAbility)ability, sim);
				sim.resume();
				children.add(new TestNode(this, sim, ability));
			}
			break;
		case TRIGGERED:
			HashMap<Ability,TriggeredAbility> triggeredAbilities = ((TestGame)game).getTriggeringOptions();
			for (Ability ability: triggeredAbilities.keySet()) {
				Game sim = game.copy();
//       	     logger.info("expand " + ability.toString());
				TestTreePlayer simPlayer = (TestTreePlayer) sim.getPlayer(player.getId());
				sim.getState().removeTriggeredAbility(triggeredAbilities.get(ability));
				simPlayer.triggerAbility((TriggeredAbility)ability, sim);
				
				sim.resume();
				children.add(new TestNode(this, sim, ability));
			}
			break;
		default:
			break;
        
		}
		game = null;
	}
	
	public void expandChoose(Ability source, Target availableTarget,Game game) {
		List<? extends Target> targets = availableTarget.getTargetOptions(source, game);
		
	}
	
	//may use .getvalue to check if the state has passed but 
	public boolean isLoop() {
		//check same states
		for(String state: this.parentStates) {
			if(state.equals(this.stateValue)) {
				return true;
			}
		}
		//check infinite damage
		for(UUID player: this.game.getPlayerList()) {
			if(player != this.playerId) {
				if(this.game.getPlayer(player).getLife() <= 0) {
					return true;
				}
			}
		}
		
		//check no resources reduces
		if(this.handCount >= parent.handCount) {
			if(this.tappedPermanentCount >= parent.tappedPermanentCount) {
				this.loopStepCount = parent.loopStepCount + 1;
			}
		}
		
		if(loopStepCount >= 10) {
			return true;
		}
		
		return false;
	}
	
	public boolean isLeaf() {
		return children.isEmpty();
	}
	
	private static final ConcurrentHashMap<String, List<Ability>> playablesCache = new ConcurrentHashMap<String, List<Ability>>();
	
    private List<Ability> getPlayables(TestTreePlayer player, String stateValue2, Game game2) {
		// TODO Auto-generated method stub
    	if(playablesCache.containsKey(stateValue2)) {
    		return playablesCache.get(stateValue2);
    	}
    	else {
    		List<Ability> abilities = player.getPlayableOptions(game2);
    		playablesCache.put(stateValue2, abilities);
    		return abilities;
    	}
	}

	public boolean isTerminal() {
        return terminal;
    }
    
    public Ability getAction() {
        return action;
    }

    public int getNumChildren() {
        return children.size();
    }

    public TestNode getParent() {
        return parent;
    }
    
    public String getStateValue() {
    	return stateValue;
    }

}
