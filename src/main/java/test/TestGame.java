package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

import mage.MageException;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.TriggeredAbility;
import mage.abilities.effects.ContinuousEffects;
import mage.constants.MultiplayerAttackOption;
import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.constants.Zone;
import mage.game.GameImpl;
import mage.game.GameState;
import mage.game.TwoPlayerDuelType;
import mage.game.events.GameEvent;
import mage.game.match.MatchType;
import mage.game.mulligan.LondonMulligan;
import mage.game.stack.StackObject;
import mage.game.turn.PreCombatMainPhase;
import mage.game.turn.TurnMod;
import mage.players.Player;

public class TestGame extends GameImpl{
	
	private HashMap<Ability,TriggeredAbility> triggeringOptions = new HashMap<Ability,TriggeredAbility>();
	private SpanningTree spanningTree;
	private static final String UNIT_TESTS_ERROR_TEXT = "Error in unit tests";
	private transient Stack<Integer> savedStates = new Stack<>();
	private ContinuousEffects applyingReplaceEffects;
	private TestGame resolvingEffectGame;
	private StackObject resolvingEffect;
	private ArrayList<CurrentAction> currentAction;
	private int infiniteLoopCounter = 0;
	
	public enum CurrentAction {
	       RESOLVE,
	       REPLACE,
	       APPLY,
	       SPECIAL
	   }

//	public TestGame(GameImpl game) {
//		super(game);
//		// TODO Auto-generated constructor stub
//	}
	
	public TestGame(SpanningTree spanningTree) {
		super(MultiplayerAttackOption.LEFT,RangeOfInfluence.ALL,new LondonMulligan(0),20,7);
		// TODO Auto-generated constructor stub
		this.spanningTree = spanningTree;
		
	}
	
	public ContinuousEffects getApplyingReplaceEffects() {
		return applyingReplaceEffects;
	}

	public ArrayList<CurrentAction> getCurrentAction() {
		return currentAction;
	}


	public void setApplyingReplaceEffects(ContinuousEffects applyingReplaceEffects) {
		this.applyingReplaceEffects = applyingReplaceEffects;
	}

	public TestGame(final TestGame game) {
		super(game);
		this.spanningTree = game.spanningTree;
	}
	
	public void startSim(UUID nextPlayerId) {
		super.init(nextPlayerId);
		state.getTurnMods().add(new TurnMod(startingPlayerId, PhaseStep.PRECOMBAT_MAIN));
		playerList = state.getPlayerList(nextPlayerId);
		Player mainPlayer = getPlayer(playerList.get());
		playTurn(mainPlayer);
	}
	
	@Override
	public void playPriority(UUID activePlayerId, boolean resuming) {
        int errorContinueCounter = 0;
        infiniteLoopCounter = 0;
        int rollbackBookmark = 0;
        clearAllBookmarks();
        try {
            applyEffects();
            while (!isPaused() && !checkIfGameIsOver() && !this.getTurn().isEndTurnRequested()) {
                if (!resuming) {
                    state.getPlayers().resetPassed();
                    state.getPlayerList().setCurrent(activePlayerId);
                } else {
                    state.getPlayerList().setCurrent(this.getPriorityPlayerId());
                }
                fireUpdatePlayersEvent();
                Player player;
                while (!isPaused() && !checkIfGameIsOver()) {
                    try {
                        if (rollbackBookmark == 0) {
                            rollbackBookmark = bookmarkState();
                        }
                        player = getPlayer(state.getPlayerList().get());
                        state.setPriorityPlayerId(player.getId());
                        while (!player.isPassed() && player.canRespond() && !isPaused() && !checkIfGameIsOver()) {
                            if (!resuming) {
                                // 603.3. Once an ability has triggered, its controller puts it on the stack as an object that's not a card the next time a player would receive priority
                                checkStateAndTriggered();
                                applyEffects();
                                if (state.getStack().isEmpty()) {
                                    resetLKI();
                                }
                                saveState(false);
                                if (isPaused() || checkIfGameIsOver()) {
                                    return;
                                }
                                // resetPassed should be called if player performs any action
                                if (player.priority(this)) {
                                	spanningTree.priorityProcess();
                                    if (executingRollback()) {
                                        return;
                                    }
                                    getState().handleSimultaneousEvent(this); // needed here to handle triggers e.g. from paying costs like sacrificing a creatures before LKIShort is cleared
                                    applyEffects();
                                }
                                if (isPaused()) {
                                    return;
                                }
                            }
                            resuming = false;
                        }
                        resetShortLivingLKI();
                        resuming = false;
                        if (isPaused() || checkIfGameIsOver()) {
                            return;
                        }
                        if (allPassed()) {
                            if (!state.getStack().isEmpty()) {
                                //20091005 - 115.4
                                resolve();
                                checkConcede();
                                applyEffects();
                                state.getPlayers().resetPassed();
                                fireUpdatePlayersEvent();
                                resetShortLivingLKI();
                                break;
                            } else {
                                resetLKI();
                                return;
                            }
                        }
                    } catch (Exception ex) {
//                        logger.fatal("Game exception gameId: " + getId(), ex);
                        if ((ex instanceof NullPointerException)
                                && errorContinueCounter == 0 && ex.getStackTrace() != null) {
//                            logger.fatal(ex.getStackTrace());
                        }
                        this.fireErrorEvent("Game exception occurred: ", ex);

                        // stack info
                        String info = this.getStack().stream().map(MageObject::toString).collect(Collectors.joining("\n"));
//                        logger.info(String.format("\nStack before error %d: \n%s\n", this.getStack().size(), info));

                        // rollback game to prev state
                        GameState restoredState = restoreState(rollbackBookmark, "Game exception: " + ex.getMessage());
                        rollbackBookmark = 0;

                        if (errorContinueCounter > 15) {
                            throw new MageException("Iterated player priority after game exception too often, game ends! Last error:\n "
                                    + ex.getMessage());
                        }

                        if (restoredState != null) {
                            this.informPlayers(String.format("Auto-restored to %s due game error: %s", restoredState, ex.getMessage()));
                        } else {
//                            logger.error("Can't auto-restore to prev state.");
                        }

                        Player activePlayer = this.getPlayer(getActivePlayerId());
                        if (activePlayer != null && !activePlayer.isTestsMode()) {
                            errorContinueCounter++;
                            continue;
                        } else {
                            throw new MageException(UNIT_TESTS_ERROR_TEXT);
                        }
                    } finally {
                        setCheckPlayableState(false);
                    }
                    state.getPlayerList().getNext();
                }
            }
        } catch (Exception ex) {
//            logger.fatal("Game exception " + ex.getMessage(), ex);
            this.fireErrorEvent("Game exception occurred: ", ex);
            this.end();

            // don't catch game errors in unit tests, so test framework can process it (example: errors in AI simulations)
            if (ex.getMessage() != null && ex.getMessage().equals(UNIT_TESTS_ERROR_TEXT)) {
                //this.getContinuousEffects().traceContinuousEffects(this);
                throw new IllegalStateException(UNIT_TESTS_ERROR_TEXT);
            }
        } finally {
            resetLKI();
            clearAllBookmarks();
            setCheckPlayableState(false);
        }
    }
	
	private void clearAllBookmarks() {
        if (!simulation) {
            while (!savedStates.isEmpty()) {
                savedStates.pop();
            }
            gameStates.remove(0);
            for (Player player : getPlayers().values()) {
                player.setStoredBookmark(-1);
            }
        }
    }
	
	@Override
	public boolean checkTriggered() {
        boolean played = false;
        state.getTriggers().checkStateTriggers(this);
        for (UUID playerId : state.getPlayerList(state.getActivePlayerId())) {
            Player player = getPlayer(playerId);
            while (player.canRespond()) { // player can die or win caused by triggered abilities or leave the game
                List<TriggeredAbility> abilities = state.getTriggered(player.getId());
//                TestTreePlayer test = new TestTreePlayer("test");
                
                if (abilities.isEmpty()) {
                    break;
                }
                
                // triggered abilities that don't use the stack have to be executed first (e.g. Banisher Priest Return exiled creature
                for (Iterator<TriggeredAbility> it = abilities.iterator(); it.hasNext(); ) {
                    TriggeredAbility triggeredAbility = it.next();
                    if (!triggeredAbility.isUsesStack()) {
                        state.removeTriggeredAbility(triggeredAbility);
                        played |= player.triggerAbility(triggeredAbility, this);
                        it.remove();
                    }
                }
                
                if (abilities.isEmpty()) {
                    break;
                }
                
                for(TriggeredAbility triggering : abilities) {
                	for(Ability option: player.getPlayableOptions(triggering, this)) {
                		triggeringOptions.put(option, triggering);
                	}
                }               
                
                if(player instanceof TestTreePlayer) {
                	spanningTree.triggeringProcess();
                	this.pause();
                	triggeringOptions.clear();
                	break;
                }
                else if (abilities.size() == 1) {
//                	System.out.println("option size: "+player.getPlayableOptions(abilities.get(0), this).size());
                	System.out.println("option : "+player.getPlayableOptions(abilities.get(0), this));
                	for(Ability ta:player.getPlayableOptions(abilities.get(0), this)) {
                		System.out.println("optionID :"+ta.getId());
                	}
                    state.removeTriggeredAbility(abilities.get(0));
                    played |= player.triggerAbility(abilities.get(0), this);
                } else {
                    TriggeredAbility ability = player.chooseTriggeredAbility(abilities, this);
                    if (ability != null) {
                        state.removeTriggeredAbility(ability);
                        played |= player.triggerAbility(ability, this);
                    }
                }
            }
        }
        
        return played;
    }
	
	private boolean playTurn(Player player) {
        boolean skipTurn = false;
        state.setActivePlayerId(player.getId());
        //skipTurn = state.getTurn().play(this, player);
        PreCombatMainPhase testMainPhase = new PreCombatMainPhase();
        
        
        state.getTurn().setPhase(testMainPhase);
        //System.out.println(player.getPlayableOptions(this));
        testMainPhase.play(this, id);
        
        
        if (isPaused() || checkIfGameIsOver()) {
            return false;
        }
        if (!skipTurn) {
            endOfTurn();
            state.setTurnNum(state.getTurnNum() + 1);
        }

        return true;
    }
	
	private boolean checkStopOnTurnOption() {
        if (gameOptions.stopOnTurn != null && gameOptions.stopAtStep == PhaseStep.UNTAP) {
            if (gameOptions.stopOnTurn.equals(state.getTurnNum())) {
                winnerId = null; //DRAW
                saveState(false);
                return true;
            }
        }
        return false;
    }
	
	@Override
	public void resolve() {
		if(!this.currentAction.contains(CurrentAction.RESOLVE)) {
			this.currentAction.add(CurrentAction.RESOLVE);
		}
		
        StackObject top = null;
        try {
            top = state.getStack().peek();
            if(top.getControllerId()==spanningTree.getPlayer().getId()) {
            	this.resolvingEffectGame = new TestGame(this);
            	this.resolvingEffect = top;
            	spanningTree.getPlayer().setResolvingAbility(top);
            }
            top.resolve(this);
            resetControlAfterSpellResolve(top.getId());
        } finally {
            if (top != null) {
                state.getStack().remove(top, this); // seems partly redundant because move card from stack to grave is already done and the stack removed
                rememberLKI(top.getSourceId(), Zone.STACK, top);
                checkInfiniteLoop(top.getSourceId());
                if (!getTurn().isEndTurnRequested()) {
                    while (state.hasSimultaneousEvents()) {
                        state.handleSimultaneousEvent(this);
                    }
                }
            }
        }
        this.currentAction.remove(CurrentAction.RESOLVE);
    }
	
	@Override
    public boolean replaceEvent(GameEvent event) {
		if(!this.currentAction.contains(CurrentAction.REPLACE)) {
			this.currentAction.add(CurrentAction.REPLACE);
		}
		
		this.settingApplyingReplaceEvent(event, null);
		boolean result = state.replaceEvent(event, this);
		this.currentAction.remove(CurrentAction.REPLACE);
		return result;
    }

    @Override
    public boolean replaceEvent(GameEvent event, Ability targetAbility) {
    	if(!this.currentAction.contains(CurrentAction.REPLACE)) {
			this.currentAction.add(CurrentAction.REPLACE);
		}
    	this.settingApplyingReplaceEvent(event, targetAbility);
    	boolean result = state.replaceEvent(event, this);
		this.currentAction.remove(CurrentAction.REPLACE);
		return result;
    }
    
    private void settingApplyingReplaceEvent(GameEvent event,Ability targetAbility) {
    	if(!this.getState().getContinuousEffects().preventedByRuleModification(event, targetAbility,this,false)) {
    		this.applyingReplaceEffects = this.getState().getContinuousEffects();
    	}
    }

	public MatchType getGameType() {
		// TODO Auto-generated method stub
		return new TwoPlayerDuelType();
	}

	public int getNumPlayers() {
		// TODO Auto-generated method stub
		return 2;
	}

	public TestGame copy() {
		// TODO Auto-generated method stub
		return new TestGame(this);
	}

	public HashMap<Ability, TriggeredAbility> getTriggeringOptions() {
		return triggeringOptions;
	}

	public void setTriggeringOptions(HashMap<Ability, TriggeredAbility> triggeringOptions) {
		this.triggeringOptions = triggeringOptions;
	}

	public StackObject getResolvingEffect() {
		return resolvingEffect;
	}

	public void setResolvingEffect(StackObject resolvingEffect) {
		this.resolvingEffect = resolvingEffect;
	}
	
	

}
