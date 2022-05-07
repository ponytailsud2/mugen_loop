package test;

import java.util.UUID;

import mage.constants.MultiplayerAttackOption;
import mage.constants.RangeOfInfluence;
import mage.game.Game;
import mage.game.GameImpl;
import mage.game.TwoPlayerDuelType;
import mage.game.match.MatchType;
import mage.game.mulligan.LondonMulligan;
import mage.game.mulligan.Mulligan;

public class TestGame extends GameImpl{

	public TestGame(GameImpl game) {
		super(game);
		// TODO Auto-generated constructor stub
	}
	
	public TestGame() {
		super(MultiplayerAttackOption.LEFT,RangeOfInfluence.ALL,new LondonMulligan(0),20,7);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void play(UUID nextPlayerId) {
		super.play(nextPlayerId);
	}

	public MatchType getGameType() {
		// TODO Auto-generated method stub
		return new TwoPlayerDuelType();
	}

	public int getNumPlayers() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Game copy() {
		// TODO Auto-generated method stub
		return null;
	}

}
