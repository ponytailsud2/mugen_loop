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
import mage.abilities.SpecialAction;
import mage.abilities.TriggeredAbility;
import mage.abilities.common.PassAbility;
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
import test.TestGame.CurrentAction;

public class TestNode {
	
	private static final ThreadLocalStringBuilder threadLocalBuilder = new ThreadLocalStringBuilder(1024);
	public static final boolean USE_ACTION_CACHE = false;
	private TestNode parent;
	private final List<TestNode> children = new ArrayList<TestNode>();
	private Ability action;
	private StackObject stackEffect;
	private Game game;
	private final String stateValue;
	private UUID playerId;
	private boolean terminal = false;
	private ArrayList<String> parentStates;
	private int tappedPermanentCount = 0;
	private int handCount = 0;
	private int loopStepCount = 0;
	private int level;
	
	//
	private Stack<MageItem> availableTarget = new Stack<MageItem>();
	
	//use String gameState.getValue as gameState
	public TestNode(UUID playerId,Game game) {
		this.game = game;
		this.stateValue = this.getGameStateValue(game);
		this.playerId = playerId;
		this.terminal = game.getPlayer(playerId).getPlayable(game, true).isEmpty();
		this.parentStates = new ArrayList<String>();
		this.level = 0;
//		this.chooseUse = true;
		// TODO Auto-generated constructor stub
	}
	
	public TestNode(TestNode parent, Game game, Ability action) {
		this.playerId = parent.playerId;
		this.game = game;
		this.stateValue = this.getGameStateValue(game);
		this.terminal = game.getPlayer(playerId).getPlayable(game, true).isEmpty();
		this.parent = parent;
		this.action = action;
		this.level = parent.level + 1;
		this.parentStates = parent.parentStates;
		this.parentStates.add(parent.getGameStateValue(parent.game));
		// TODO Auto-generated constructor stub
	}
	
	public TestNode(TestNode parent, Game game, StackObject effect) {
		this.playerId = parent.playerId;
		this.game = game;
		this.stateValue = this.getGameStateValue(game);
		this.parent = parent;
		this.stackEffect = effect;
		this.level = parent.level + 1;
		this.parentStates = parent.parentStates;
		this.parentStates.add(parent.getGameStateValue(parent.game));
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
	
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	//expand
	public void expand() {
		TestTreePlayer player = (TestTreePlayer) game.getPlayer(playerId);
		if(isLoop()) {
			System.out.println("This combination has loop");
		}
		if(player.getNextAction() == null) {
			return;
		}
		switch (player.getNextAction()) {
		case PRIORITY:
			List<Ability> abilities;
			if (!USE_ACTION_CACHE)
				abilities = player.getPlayableOptions(game);
			else
				abilities = getPlayables(player, stateValue, game);
			if(!game.getStack().isEmpty()) {
				abilities.add(new PassAbility());
			}
			for (Ability ability: abilities) {
				System.out.println(ability);
				Game sim = game.copy();
//       	     logger.info("expand " + ability.toString());
				TestTreePlayer simPlayer = (TestTreePlayer) sim.getPlayer(player.getId());
				if(ability instanceof SpecialAction) {
					ArrayList<CurrentAction> currentActions = ((TestGame)sim).getCurrentAction();
					if(!currentActions.contains(CurrentAction.SPECIAL)) {
						currentActions.add(CurrentAction.SPECIAL);
					}
					
				}
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
				//remove chosen triggered ability
				sim.getState().removeTriggeredAbility(triggeredAbilities.get(ability));
				//trigger the ability
				simPlayer.triggerAbility((TriggeredAbility)ability, sim);
				
				sim.resume();
				children.add(new TestNode(this, sim, ability));
			}
			break;
		case CHOOSE_USE:
			TestGame sim = (TestGame) game.copy();
			ArrayList<CurrentAction> currentActions = ((TestGame)sim).getCurrentAction();
			if(currentActions.contains(CurrentAction.RESOLVE)) {
				//Case: replace event take place when resolving effect
				if(currentActions.contains(CurrentAction.REPLACE)) {
					
				}
				//Case: special action not resolve by stack
				else if(currentActions.contains(CurrentAction.SPECIAL)){
					
				}
				//Case: resolving ability by stack
				else {
					chooseUseResolveProcess(sim,player,true);
					chooseUseResolveProcess(sim,player,false);
				}
			}
			else if(currentActions.contains(CurrentAction.SPECIAL)) {
				
			}
			break;
		case CHOOSE_REPLACEMENT:
			
			break;
		default:
			break;
		}
		game = null;
	}
	
	public void expandChoose(Ability source, Target availableTarget,Game game) {
		List<? extends Target> targets = availableTarget.getTargetOptions(source, game);
		
		
	}
	
	private void chooseUseResolveProcess(TestGame sim,Player player,boolean chooseUse) {
		
		TestTreePlayer simPlayer = (TestTreePlayer) sim.getPlayer(player.getId());
		simPlayer.getChooseUseMap().put(this.level+1, chooseUse);
		sim.resolve();
		sim.resume();
		children.add(new TestNode(this,sim,sim.getResolvingEffect()));
	}
	
	//may use .getvalue to check if the state has passed but 
	public boolean isLoop() {
		//check same states
		int loopCount = 0;
		for(String state: this.parentStates) {
			if(state.equals(this.stateValue)) {
				loopCount++;
				if(loopCount >= 5) {
					return true;
				}
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
