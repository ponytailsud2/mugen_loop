package test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

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
import mage.util.RandomUtil;

public class AlwaysPassPlayer extends PlayerImpl{

	public AlwaysPassPlayer(String name, RangeOfInfluence range) {
		// TODO Auto-generated constructor stub
		super(name,range);
		human = false;
		
	}
	
	public AlwaysPassPlayer(final AlwaysPassPlayer player) {
		// TODO Auto-generated constructor stub
		super(player);
		
	}

	public AlwaysPassPlayer(UUID uuid, String name) {
		// TODO Auto-generated constructor stub
		super(name,RangeOfInfluence.ONE);
		human = false;
	}

	@Override
	public void abort() {
		// TODO Auto-generated method stub
		abort = true;
	}

	@Override
	public void skip() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AlwaysPassPlayer copy() {
		// TODO Auto-generated method stub
		return new AlwaysPassPlayer(this);
	}

	@Override
	public boolean priority(Game game) {
		// TODO Auto-generated method stub
		this.passed = true;
		resetStoredBookmark(game);
		return true;
	}

	@Override
	public boolean choose(Outcome outcome, Target target, UUID sourceId, Game game) {
		// TODO Auto-generated method stub
		return chooseRandom(target,game);
	}

	@Override
	public boolean choose(Outcome outcome, Target target, UUID sourceId, Game game, Map<String, Serializable> options) {
		// TODO Auto-generated method stub
		return chooseRandom(target,game);
	}

	@Override
	public boolean choose(Outcome outcome, Cards cards, TargetCard target, Game game) {
		// TODO Auto-generated method stub
		if (cards.isEmpty()) {
            return false;
        }
        Set<UUID> possibleTargets = target.possibleTargets(playerId, cards, game);
        if (possibleTargets.isEmpty()) {
            return false;
        }
        Iterator<UUID> it = possibleTargets.iterator();
        int targetNum = RandomUtil.nextInt(possibleTargets.size());
        UUID targetId = it.next();
        for (int i = 0; i < targetNum; i++) {
            targetId = it.next();
        }
        target.add(targetId, game);
        return true;
	}

	@Override
	public boolean chooseTarget(Outcome outcome, Target target, Ability source, Game game) {
		Set<UUID> possibleTargets = target.possibleTargets(source == null ? null : source.getSourceId(), playerId, game);
        if (possibleTargets.isEmpty()) {
            return false;
        }
        if (!target.isRequired(source)) {
            if (RandomUtil.nextInt(possibleTargets.size() + 1) == 0) {
                return false;
            }
        }
        if (possibleTargets.size() == 1) {
            target.addTarget(possibleTargets.iterator().next(), source, game); // todo: addtryaddtarget or return type (see computerPlayer)
            return true;
        }
        Iterator<UUID> it = possibleTargets.iterator();
        int targetNum = RandomUtil.nextInt(possibleTargets.size());
        UUID targetId = it.next();
        for (int i = 0; i < targetNum; i++) {
            targetId = it.next();
        }
        target.addTarget(targetId, source, game);// todo: addtryaddtarget or return type (see computerPlayer)
        return true;
	}

	@Override
	public boolean chooseTarget(Outcome outcome, Cards cards, TargetCard target, Ability source, Game game) {
		if (cards.isEmpty()) {
            return !target.isRequired(source);
        }
        Card card = cards.getRandom(game);
        if (card != null) {
            target.addTarget(card.getId(), source, game); // todo: addtryaddtarget or return type (see computerPlayer)
            return true;
        }
        return false;
	}

	@Override
	public boolean chooseTargetAmount(Outcome outcome, TargetAmount target, Ability source, Game game) {
		Set<UUID> possibleTargets = target.possibleTargets(source == null ? null : source.getSourceId(), playerId, game);
        if (possibleTargets.isEmpty()) {
            return !target.isRequired(source);
        }
        if (!target.isRequired(source)) {
            if (RandomUtil.nextInt(possibleTargets.size() + 1) == 0) {
                return false;
            }
        }
        if (possibleTargets.size() == 1) {
            target.addTarget(possibleTargets.iterator().next(), target.getAmountRemaining(), source, game); // todo: addtryaddtarget or return type (see computerPlayer)
            return true;
        }
        Iterator<UUID> it = possibleTargets.iterator();
        int targetNum = RandomUtil.nextInt(possibleTargets.size());
        UUID targetId = it.next();
        for (int i = 0; i < targetNum; i++) {
            targetId = it.next();
        }
        target.addTarget(targetId, RandomUtil.nextInt(target.getAmountRemaining()) + 1, source, game); // todo: addtryaddtarget or return type (see computerPlayer)
        return true;
	}

	@Override
	public boolean chooseMulligan(Game game) {
		return false;
	}

	@Override
	public boolean chooseUse(Outcome outcome, String message, Ability source, Game game) {
		 return this.chooseUse(outcome, message, null, null, null, source, game);
	}

	@Override
	public boolean chooseUse(Outcome outcome, String message, String secondMessage, String trueText, String falseText,
			Ability source, Game game) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean choose(Outcome outcome, Choice choice, Game game) {
		choice.setRandomChoice();
        return true;
	}
	
	protected boolean chooseRandom(Target target, Game game) {
        Set<UUID> possibleTargets = target.possibleTargets(playerId, game);
        if (possibleTargets.isEmpty()) {
            return false;
        }
        if (possibleTargets.size() == 1) {
            target.add(possibleTargets.iterator().next(), game);
            return true;
        }
        Iterator<UUID> it = possibleTargets.iterator();
        int targetNum = RandomUtil.nextInt(possibleTargets.size());
        UUID targetId = it.next();
        for (int i = 0; i < targetNum; i++) {
            targetId = it.next();
        }
        target.add(targetId, game);
        return true;
    }

	@Override
	public boolean choosePile(Outcome outcome, String message, List<? extends Card> pile1, List<? extends Card> pile2,
			Game game) {
		// TODO Auto-generated method stub
		return RandomUtil.nextBoolean();
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
		return abilities.get(RandomUtil.nextInt(abilities.size()));
	}

	@Override
	public Mode chooseMode(Modes modes, Ability source, Game game) {
		Iterator<Mode> it = modes.getAvailableModes(source, game).iterator();
        Mode mode = it.next();
        if (modes.size() == 1) {
            return mode;
        }
        int modeNum = RandomUtil.nextInt(modes.getAvailableModes(source, game).size());
        for (int i = 0; i < modeNum; i++) {
            mode = it.next();
        }
        return mode;
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
		int remainingDamage = damage;
        UUID targetId;
        int amount;
        while (remainingDamage > 0) {
            if (targets.size() == 1) {
                targetId = targets.get(0);
                amount = remainingDamage;
            } else {
                targetId = targets.get(RandomUtil.nextInt(targets.size()));
                amount = RandomUtil.nextInt(damage + 1);
            }
            Permanent permanent = game.getPermanent(targetId);
            if (permanent != null) {
                permanent.damage(amount, attackerId, source, game, false, true);
                remainingDamage -= amount;
            } else {
                Player player = game.getPlayer(targetId);
                if (player != null) {
                    player.damage(amount, attackerId, source, game);
                    remainingDamage -= amount;
                }
            }
            targets.remove(targetId);
        }
	}

	@Override
	public int getAmount(int min, int max, String message, Game game) {
		// TODO Auto-generated method stub
		return RandomUtil.nextInt(max - min) + min;
	}

	@Override
	public List<Integer> getMultiAmount(Outcome outcome, List<String> messages, int min, int max, MultiAmountType type,
			Game game) {
		int needCount = messages.size();
		return MultiAmountType.prepareMaxValues(needCount, min, max);
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
