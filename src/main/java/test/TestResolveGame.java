package test;

import java.util.UUID;

import mage.constants.MultiplayerAttackOption;
import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.game.Game;
import mage.game.GameImpl;
import mage.game.TwoPlayerDuelType;
import mage.game.match.MatchType;
import mage.game.mulligan.Mulligan;
import mage.game.turn.TurnMod;
import mage.players.Player;

public class TestResolveGame extends GameImpl {

	public TestResolveGame(MultiplayerAttackOption attackOption, RangeOfInfluence range, Mulligan mulligan,
			int startingLife, int startingHandSize) {
		super(attackOption, range, mulligan, startingLife, startingHandSize);
		// TODO Auto-generated constructor stub
	}
	
	public TestResolveGame(GameImpl game) {
		super(game);
		// TODO Auto-generated constructor stub
	}
	
	public void checkTriggeringAbility() {
		Player player = getPlayer(state.getPlayerList().get());
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
	
	public void stackResolve() {
		if (!state.getStack().isEmpty()) {
            //20091005 - 115.4
			System.out.println("Resolving");
            resolve();
            checkConcede();
			System.out.println("Effect Applying");
            applyEffects();
            System.out.println("Reset player pass actions");
            state.getPlayers().resetPassed();
            fireUpdatePlayersEvent();
            resetShortLivingLKI();
        } else {
            resetLKI();
        }
	}

	@Override
	public MatchType getGameType() {
		// TODO Auto-generated method stub
		return new TwoPlayerDuelType();
	}

	@Override
	public int getNumPlayers() {
		// TODO Auto-generated method stub
		return 2;
	}

	 @Override
	 protected void init(UUID choosingPlayerId) {
	    super.init(choosingPlayerId);
	    state.getTurnMods().add(new TurnMod(startingPlayerId, PhaseStep.DRAW));
	 }
	
	@Override
	public Game copy() {
		// TODO Auto-generated method stub
		return new TestResolveGame(this);
	}

}
