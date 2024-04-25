package test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import mage.MageItem;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.Mode;
import mage.abilities.SpecialAction;
import mage.abilities.SpellAbility;
import mage.abilities.TriggeredAbility;
import mage.abilities.common.PassAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.mana.ManaAbility;
import mage.cards.a.AzamiLadyOfScrolls;
import mage.cards.basiclands.BasicLand;
import mage.cards.m.MindOverMatter;
import mage.cards.u.UndiscoveredParadise;
import mage.constants.CardType;
import mage.game.ExileZone;
import mage.game.Game;
import mage.game.GameState;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentCard;
import mage.game.stack.StackObject;
import mage.players.Player;
import mage.target.Target;
import mage.target.TargetCard;
import mage.target.targetpointer.FirstTargetPointer;
import mage.target.targetpointer.TargetPointer;
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
	private Hashtable<String,Integer> parentStates;
	private int tappedPermanentCount = 0;
	private int handCount = 0;
	private int loopStepCount = 0;
	private int level;
	private ArrayList<TargetCard> chooseCardOptionTemp = new ArrayList<TargetCard>();
	private HashMap<Integer,Ability> action_path;
	//
	private Stack<MageItem> availableTarget = new Stack<MageItem>();
	
	
	
	//use String gameState.getValue as gameState
	public TestNode(UUID playerId,Game game) {
		this.game = game;
		this.stateValue = this.getResourceStateValues(game);
		this.playerId = playerId;
		this.terminal = game.getPlayer(playerId).getPlayable(game, true).isEmpty();
		this.parentStates = new Hashtable<String,Integer>();
		this.level = 0;
		this.action_path = new HashMap<Integer,Ability>();
//		this.chooseUse = true;
		// TODO Auto-generated constructor stub
	}
	
	public TestNode(TestNode parent, Game game, Ability action) {
		this.playerId = parent.playerId;
		this.game = game;
		this.stateValue = this.getResourceStateValues(game);
		this.terminal = game.getPlayer(playerId).getPlayable(game, true).isEmpty() || game.checkIfGameIsOver();
		this.parent = parent;
		this.action = action;
		this.level = parent.level + 1;
		this.parentStates = new Hashtable<String,Integer>(parent.parentStates);
		if(!this.parentStates.containsKey(parent.getResourceStateValues(parent.game))) {
			this.parentStates.put(parent.getResourceStateValues(parent.game), 0);
		}
		else {
			this.parentStates.put(parent.getResourceStateValues(parent.game), this.parentStates.get(parent.getResourceStateValues(parent.game))+1);
		}
		this.action_path = new HashMap<Integer,Ability>(parent.action_path);
		if(action != null) {
			System.out.println("This action : " + action);
			this.action_path.put(this.level, action);
			System.out.println("action path : "+ action_path);
			System.out.println("=======================");
		}
		// TODO Auto-generated constructor stub
	}
	
	public TestNode(TestNode parent, Game game, StackObject effect) {
		this.playerId = parent.playerId;
		this.game = game;
		this.stateValue = this.getResourceStateValues(game);
		this.parent = parent;
		this.stackEffect = effect;
		this.level = parent.level + 1;
		this.parentStates = parent.parentStates;
//		this.parentStates.add(parent.getGameStateValue(parent.game));
	}
	
	public String getResourceStateValues(Game game) {
		StringBuilder state = threadLocalBuilder.get();
		GameState gs = game.getState();
		for(Player player : gs.getPlayers().values()) {
			if(player.getId() == playerId) {
				state.append("player").append(player.isPassed()).append(player.getLife()).append("hand");
				state.append(player.getHand().getValue(game));
			}
//			state.append("library").append(player.getLibrary().size());
//			state.append("grayard");
//			state.append(player.getGraveyard().getValue(game));
		}
		state.append("permanents");
		List<String> perms = new ArrayList<String>();
		for (Permanent permanent: gs.getBattlefield().getAllActivePermanents()) {
			perms.add(permanent.getValue(gs));
		}
		Collections.sort(perms);
		state.append(perms);
		state.append("spells|");
		for(StackObject spell : gs.getStack()) {
			state.append(spell.getControllerId()).append(spell.getName());
            state.append(spell.getStackAbility().toString());
            for (UUID modeId : spell.getStackAbility().getModes().getSelectedModes()) {
                Mode mode = spell.getStackAbility().getModes().get(modeId);
                if (!mode.getTargets().isEmpty()) {
                    state.append("targets|");
                    for (Target target : mode.getTargets()) {
                        state.append(target.getTargets());
                    }
                }
            }
		}
		return state.toString();
	}

	
	public String getGameStateValue(Game game) {
		StringBuilder state = threadLocalBuilder.get();
		GameState gs = game.getState();
//		state.append(gs.getTurn().getValue(gs.getTurnNum()));
		for(Player player : gs.getPlayers().values()) {
			if(player.getId() == playerId) {
				state.append("player|").append(player.isPassed()).append(player.getLife()).append("hand");
				state.append(player.getHand().getValue(game));
			}
			state.append("library|").append(player.getLibrary().size());
			state.append("grayard|");
			state.append(player.getGraveyard().getValue(game));
		}
		
		state.append("permanents|");
		List<String> perms = new ArrayList<String>();
		for (Permanent permanent: gs.getBattlefield().getAllActivePermanents()) {
			perms.add(permanent.getValue(gs));
		}
		Collections.sort(perms);
		state.append(perms);
		
		state.append("spells|");
		for(StackObject spell : gs.getStack()) {
			state.append(spell.getControllerId()).append(spell.getName());
            state.append(spell.getStackAbility().toString());
            for (UUID modeId : spell.getStackAbility().getModes().getSelectedModes()) {
                Mode mode = spell.getStackAbility().getModes().get(modeId);
                if (!mode.getTargets().isEmpty()) {
                    state.append("targets|");
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
		System.out.println("expand Lv.: " + this.getLevel());
		
		if(action != null) {
			System.out.println("prev action : "+ action);
		}
		
		if(!children.isEmpty()) {
			
			ArrayList<String> children_action = new ArrayList<String>();
			for(TestNode child:children) {
				children_action.add(child.action.toString());
				child.expand();
			}
			System.out.println("children :" + children_action.toString());
			return;
		}
		int passCounter = 0;
//		for(Ability ability:action_path.values()) {
//			if(ability instanceof PassAbility) {
//				passCounter += 1;
//			}
//		}
//		System.out.println(passCounter);
//		System.out.println("Lv : "+this.level);
//		System.out.println("action : "+this.action);
		System.out.println("path : "+action_path);
		System.out.println("player type: "+game.getPlayer(playerId));
		TestTreePlayer player = (TestTreePlayer) game.getPlayer(playerId);
		
		if(this.parentStates.size() > 1 && isLoop()) {
			File f = new File("parent_state_check.txt");
			if(!f.exists()) {
				try {
					FileWriter myWriter = new FileWriter("parent_state_check.txt");
				    myWriter.write(this.parentStates.toString());
				    myWriter.close();
				    System.out.println("check loop state");
				}
				catch(Exception e) {
					System.out.println("An error occurred.");
				    e.printStackTrace();
				}
			}
		}
		
//		System.out.println("part: " + game.getStep().getStepPart());
		if(this.action != null && !(this.action instanceof PassAbility) && isLoop()) {
			
			game.end();
			game.resume();
			File f = new File("loop_check.txt");
			if(f.exists()) {
				return;
			}
			try {
				FileWriter myWriter = new FileWriter("loop_check.txt");
			    myWriter.write(this.action_path.toString());
			    myWriter.write("---------------------------------");
			    myWriter.write(this.getStateValue());
			    myWriter.close();
			    System.out.println("This combination has loop");
			}
			catch(Exception e) {
				System.out.println("An error occurred.");
			    e.printStackTrace();
			}
			return;
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
//			int passCount = 0;
//			for(Ability ability:abilities) {
//				if(ability instanceof PassAbility) {
//					passCount += 1;
//				}
//			}
			
//			if(passCount > 1) {
//				System.out.println(abilities);
//			}
//			if(passCount <= 0 && !game.getStack().isEmpty()) {
//				System.out.println("pass count: "+passCount);
//				System.out.println("pass: " + abilities.toString());
//				abilities.add(new PassAbility());
//			}
			ArrayList<String> optionName = new ArrayList<String>();
			for (Ability ability:abilities) {
				if(ability.getSourceObject(game) !=null) {
					optionName.add(ability.getSourceObject(game).getName());
				}
			}
			boolean check_option = false;
			for(String name :optionName) {
				if (name.equals("MindOver")) {
					check_option = true;
					break;
				}
			}
			if(check_option) {
//				System.out.println("option size : "+optionName.size());
				System.out.println("activate option : " + abilities);
					
				
			}
			
			for (Ability ability: abilities) {
				System.out.println("ability : "+ability+" cost : "+((ActivatedAbility)ability).getManaCostsToPay());
				if(ability.getSourceObject(game) instanceof PermanentCard && ((PermanentCard)ability.getSourceObject(game)).getCard().getCardType().contains(CardType.LAND) &&((PermanentCard)((PermanentCard)ability.getSourceObject(game)).getCard()).getCard() instanceof UndiscoveredParadise) {
					System.out.println("land");
					System.out.println("'''''''''''");
					continue;
				}
				if(ability.getSourceObject(game) instanceof PermanentCard&& ((PermanentCard)ability.getSourceObject(game)).getCard().getCardType().contains(CardType.LAND)  &&((PermanentCard)((PermanentCard)ability.getSourceObject(game)).getCard()).getCard() instanceof BasicLand) {
					continue;
				}
				TestGame sim = ((TestGame)game).copy();
//       	     logger.info("expand " + ability.toString());
//				System.out.println("check game over: "+sim.checkIfGameIsOver());
//				System.out.println("check game over origin: "+game.checkIfGameIsOver());
				TestTreePlayer simPlayer = (TestTreePlayer) sim.getPlayer(player.getId());
				
				if(ability instanceof SpecialAction) {
					ArrayList<CurrentAction> currentActions = ((TestGame)sim).getCurrentAction();
					if(!currentActions.contains(CurrentAction.SPECIAL)) {
						currentActions.add(CurrentAction.SPECIAL);
					}
					
				}
				if(ability instanceof PassAbility && this.action != null&&  this.action instanceof PassAbility && sim.getState().getStack().isEmpty()) {
					sim.end();
					sim.resume();
				}
				else {
					System.out.println("Activate: " + ability.toString());
//					simPlayer.getManaPool();
					String preResumeState = this.getResourceStateValues(sim);
					boolean check_effect_output = false;
					if (ability instanceof PassAbility && this.action != null && !(this.action instanceof SpellAbility || this.action instanceof PassAbility) && (((PermanentCard)this.action.getSourceObject(game)).getCard()) instanceof MindOverMatter) {
						check_effect_output = true;
//						TargetPointer targetPointer = FirstTargetPointer.getInstance();
//						Permanent target = game.getPermanent(targetPointer.getFirst(game, source));
						this.tappedPermanentCount = sim.getBattlefield().getAllPermanents().stream()
				                .filter(Permanent::isTapped)
				                .collect(Collectors.toList()).size();
						System.out.println(this.action.getTargets().get(0).isChosen());
						System.out.println("stack size before: " + sim.getState().getStack().size());
						System.out.println("before tapped: "+tappedPermanentCount);
						System.out.println("Before pass hand size :" + sim.getPlayer(player.getId()).getHand().size());
					}
					if (simPlayer.activateAbility((ActivatedAbility)ability, sim)) {
						sim.resume();
//						if(this.action == null) {
//							this.action = ability;
//						}
						if(check_effect_output) {
							this.tappedPermanentCount = sim.getBattlefield().getAllPermanents().stream()
					                .filter(Permanent::isTapped)
					                .collect(Collectors.toList()).size();
							System.out.println("stack size after: " + sim.getState().getStack().size());
							System.out.println("after tapped: "+tappedPermanentCount);
							System.out.println("After pass hand size :" + sim.getPlayer(player.getId()).getHand().size());
						}
						String postResumeState = this.getResourceStateValues(sim);
						boolean stateChange = !preResumeState.equals(postResumeState);
						System.out.println("state change: " + stateChange);
						System.out.println("stack size: " + sim.getState().getStack().size());
						System.out.println("phase : "+ sim.getPhase().toString());
						
						if((ability instanceof PassAbility) && stateChange) {
							
							try {
								FileWriter myWriter = new FileWriter("state_change.txt",true);
							    myWriter.write(ability.toString() + "\n");
								myWriter.write(preResumeState + "\n");
							    myWriter.write("--------------------------------- \n");
							    myWriter.write(postResumeState + "\n");
							    myWriter.write(".......................... \n");
							    myWriter.close();
							}
							catch(Exception e) {
								System.out.println("An error occurred.");
							    e.printStackTrace();
							}
						}
						System.out.println("..................");
						children.add(new TestNode(this, sim, ability));
				
					}else {
						sim.end();
						sim.resume();
					}
						
				}
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
			TestGame choose_use_sim = (TestGame) game.copy();
			ArrayList<CurrentAction> currentActions = choose_use_sim.getCurrentAction();
			if(currentActions.contains(CurrentAction.RESOLVE)) {
				//Case: replace event take place when resolving effect
				if(currentActions.contains(CurrentAction.REPLACE)) {
					
				}
				//Case: special action not resolve by stack
				else if(currentActions.contains(CurrentAction.SPECIAL)){
					
				}
				//Case: resolving ability by stack
				else {
					chooseUseResolveProcess(choose_use_sim,player,true);
					chooseUseResolveProcess(choose_use_sim,player,false);
				}
			}
			else if(currentActions.contains(CurrentAction.SPECIAL)) {
				
			}
			break;
		case CHOOSE_REPLACEMENT:
			break;
		case CHOOSE_CARD:
			for(TargetCard target:chooseCardOptionTemp) {
				TestGame choose_card_sim = (TestGame)game.copy();
				ArrayList<CurrentAction> choose_card_currentActions = choose_card_sim.getCurrentAction();
				//Case: replace event take place when resolving effect
				if(choose_card_currentActions.contains(CurrentAction.REPLACE)) {
					
				}
				//Case: special action not resolve by stack
				else if(choose_card_currentActions.contains(CurrentAction.SPECIAL)){
					
				}
				else {
					chooseCardsResolveProcess(choose_card_sim, player, target);
				}
				
			}
			break;
		default:
			break;
		}
//		game = null;
	}
	
	public ArrayList<TargetCard> getChooseCardOptionTemp() {
		return chooseCardOptionTemp;
	}

	public void setChooseCardOptionTemp(ArrayList<TargetCard> chooseCardOptionTemp) {
		this.chooseCardOptionTemp = chooseCardOptionTemp;
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
	
	private void chooseCardsResolveProcess(TestGame sim,Player player,TargetCard target) {
		TestTreePlayer simPlayer = (TestTreePlayer) sim.getPlayer(player.getId());
		simPlayer.getChooseCardsMap().put(this.level+1, target);
		sim.resolve();
		sim.resume();
		children.add(new TestNode(this,sim,sim.getResolvingEffect()));
	}
	
	//may use .getvalue to check if the state has passed but 
	public boolean isLoop() {
		//check same states
		int loopCount = 0;
		 Enumeration<String> keys = parentStates.keys();
	        while (keys.hasMoreElements()) {
	            String key = keys.nextElement();
	            if(parentStates.get(key) >= 5) {
	            	return true;
	            }
//	            System.out.println("Key: " + key + ", Value: " + parentStates.get(key));
	        }
		//check infinite damage
		for(UUID player: this.game.getPlayerList()) {
			if(player != this.playerId) {
				if(this.game.getPlayer(player).getLife() <= 0) {
					return true;
				}
			}
		}
		
		//check infinite draw or mill
		for(UUID player: this.game.getPlayerList()) {
			if(player == this.playerId) {
				if(this.game.getPlayer(player).getLibrary().size() <= 0) {
					return true;
				}
			}
		}
		
		//check no resources reduces
		this.tappedPermanentCount = game.getBattlefield().getAllPermanents().stream()
                .filter(Permanent::isTapped)
                .collect(Collectors.toList()).size();
		System.out.println("tapped: "+tappedPermanentCount);
		System.out.println("player: "+ game.getPlayer(playerId));
		System.out.println("mana: " + game.getPlayer(playerId).getManaAvailable(game));
//		System.out.println("mana pool: "+ game.getPlayer(playerId).getManaPool());
//		if(parent != null) {
//			if(this.handCount >= parent.handCount && this.tappedPermanentCount >= parent.tappedPermanentCount) {
//				this.loopStepCount = parent.loopStepCount + 1;
//			}
//			else {
//				this.loopStepCount = parent.loopStepCount;
//			}
//
//		}
//				
//		if(loopStepCount >= 4) {
//			return true;
//		}
		
		
		
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

	public TestNode getMatchingState(String value) {
		// TODO Auto-generated method stub
		return null;
	}

	public void emancipate() {
		// TODO Auto-generated method stub
		
	}

}
