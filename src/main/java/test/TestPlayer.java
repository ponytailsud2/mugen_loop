package test;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mage.abilities.Ability;
import mage.abilities.Mode;
import mage.abilities.Modes;
import mage.abilities.TriggeredAbility;
import mage.abilities.costs.VariableCost;
import mage.abilities.costs.mana.ManaCost;
import mage.cards.Card;
import mage.cards.Cards;
import mage.cards.decks.Deck;
import mage.choices.Choice;
import mage.constants.MultiAmountType;
import mage.constants.Outcome;
import mage.constants.RangeOfInfluence;
import mage.game.Game;
import mage.game.combat.CombatGroup;
import mage.game.draft.Draft;
import mage.game.match.Match;
import mage.game.permanent.Permanent;
import mage.game.tournament.Tournament;
import mage.players.Player;
import mage.players.PlayerImpl;
import mage.target.Target;
import mage.target.TargetAmount;
import mage.target.TargetCard;

public class TestPlayer extends PlayerImpl {

	public TestPlayer(String name, RangeOfInfluence range) {
		super(name, range);
		// TODO Auto-generated constructor stub
	}

	public TestPlayer(UUID id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	public TestPlayer(PlayerImpl player) {
		super(player);
		// TODO Auto-generated constructor stub
	}
	

	public void abort() {
		// TODO Auto-generated method stub

	}

	public void skip() {
		// TODO Auto-generated method stub

	}

	public Player copy() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean priority(Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean choose(Outcome outcome, Target target, UUID sourceId, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean choose(Outcome outcome, Target target, UUID sourceId, Game game, Map<String, Serializable> options) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean choose(Outcome outcome, Cards cards, TargetCard target, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean chooseTarget(Outcome outcome, Target target, Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean chooseTarget(Outcome outcome, Cards cards, TargetCard target, Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean chooseTargetAmount(Outcome outcome, TargetAmount target, Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean chooseMulligan(Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean chooseUse(Outcome outcome, String message, Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean chooseUse(Outcome outcome, String message, String secondMessage, String trueText, String falseText,
			Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean choose(Outcome outcome, Choice choice, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean choosePile(Outcome outcome, String message, List<? extends Card> pile1, List<? extends Card> pile2,
			Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean playMana(Ability ability, ManaCost unpaid, String promptText, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	public int announceXMana(int min, int max, int multiplier, String message, Game game, Ability ability) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int announceXCost(int min, int max, String message, Game game, Ability ability, VariableCost variableCost) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int chooseReplacementEffect(Map<String, String> abilityMap, Game game) {
		// TODO Auto-generated method stub
		return 0;
	}

	public TriggeredAbility chooseTriggeredAbility(List<TriggeredAbility> abilities, Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	public Mode chooseMode(Modes modes, Ability source, Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	public void selectAttackers(Game game, UUID attackingPlayerId) {
		// TODO Auto-generated method stub

	}

	public void selectBlockers(Ability source, Game game, UUID defendingPlayerId) {
		// TODO Auto-generated method stub

	}

	public UUID chooseAttackerOrder(List<Permanent> attacker, Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	public UUID chooseBlockerOrder(List<Permanent> blockers, CombatGroup combatGroup, List<UUID> blockerOrder,
			Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	public void assignDamage(int damage, List<UUID> targets, String singleTargetName, UUID attackerId, Ability source,
			Game game) {
		// TODO Auto-generated method stub

	}

	public int getAmount(int min, int max, String message, Game game) {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<Integer> getMultiAmount(Outcome outcome, List<String> messages, int min, int max, MultiAmountType type,
			Game game) {
		// TODO Auto-generated method stub
		return null;
	}

	public void sideboard(Match match, Deck deck) {
		// TODO Auto-generated method stub

	}

	public void construct(Tournament tournament, Deck deck) {
		// TODO Auto-generated method stub

	}

	public void pickCard(List<Card> cards, Deck deck, Draft draft) {
		// TODO Auto-generated method stub

	}

}
