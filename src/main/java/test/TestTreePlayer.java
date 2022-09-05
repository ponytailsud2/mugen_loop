package test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.SpellAbility;
import mage.abilities.TriggeredAbility;
import mage.abilities.common.PassAbility;
import mage.abilities.costs.VariableCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.costs.mana.ManaCost;
import mage.cards.Card;
import mage.cards.Cards;
import mage.cards.decks.Deck;
import mage.choices.Choice;
import mage.constants.MultiAmountType;
import mage.constants.Outcome;
import mage.game.Game;
import mage.game.combat.CombatGroup;
import mage.game.draft.Draft;
import mage.game.match.Match;
import mage.game.permanent.Permanent;
import mage.game.tournament.Tournament;
import mage.player.ai.ComputerPlayer;


public class TestTreePlayer extends ComputerPlayer{


   //private static final Logger logger = Logger.getLogger(MCTSPlayer.class);

   protected PassAbility pass = new PassAbility();

   private NextAction nextAction;

   public enum NextAction {
       PRIORITY, SELECT_ATTACKERS
   }

   public TestTreePlayer(UUID id) {
	   super(id);
       this.pass.setControllerId(id);
   }


   protected List<ActivatedAbility> getPlayableAbilities(Game game) {
       List<ActivatedAbility> playables = getPlayable(game, true);
       playables.add(pass);
       return playables;
   }

   public List<Ability> getPlayableOptions(Game game) {
       List<Ability> all = new ArrayList<Ability>();
       List<ActivatedAbility> playables = getPlayableAbilities(game);
       for (ActivatedAbility ability: playables) {
           List<Ability> options = game.getPlayer(playerId).getPlayableOptions(ability, game);
           if (options.isEmpty()) {
               if (!ability.getManaCosts().getVariableCosts().isEmpty()) {
                   simulateVariableCosts(ability, all, game);
               }
               else {
                   all.add(ability);
               }
           }
           else {
               for (Ability option: options) {
                   if (!ability.getManaCosts().getVariableCosts().isEmpty()) {
                       simulateVariableCosts(option, all, game);
                   }
                   else {
                       all.add(option);
                   }
               }
           }
       }
       return all;
   }

   protected void simulateVariableCosts(Ability ability, List<Ability> options, Game game) {
       int numAvailable = getAvailableManaProducers(game).size() - ability.getManaCosts().manaValue();
       int start = 0;
       if (!(ability instanceof SpellAbility)) {
           //only use x=0 on spell abilities
           if (numAvailable == 0)
               return;
           else
               start = 1;
       }
       for (int i = start; i < numAvailable; i++) {
           Ability newAbility = ability.copy();
           newAbility.getManaCostsToPay().add(new GenericManaCost(i));
           options.add(newAbility);
       }
   }

   public List<List<UUID>> getAttacks(Game game) {
       List<List<UUID>> engagements = new ArrayList<List<UUID>>();
       List<Permanent> attackersList = super.getAvailableAttackers(game);
       //use binary digits to calculate powerset of attackers
       int powerElements = (int) Math.pow(2, attackersList.size());
       StringBuilder binary = new StringBuilder();
       for (int i = powerElements - 1; i >= 0; i--) {
           binary.setLength(0);
           binary.append(Integer.toBinaryString(i));
           while (binary.length() < attackersList.size()) {
               binary.insert(0, '0');
           }
           List<UUID> engagement = new ArrayList<UUID>();
           for (int j = 0; j < attackersList.size(); j++) {
               if (binary.charAt(j) == '1') {
                   engagement.add(attackersList.get(j).getId());
               }
           }
           engagements.add(engagement);
       }
       return engagements;
   }

   public List<List<List<UUID>>> getBlocks(Game game) {
       List<List<List<UUID>>> engagements = new ArrayList<List<List<UUID>>>();
       int numGroups = game.getCombat().getGroups().size();
       if (numGroups == 0) {
           return engagements;
       }

       //add a node with no blockers
       List<List<UUID>> engagement = new ArrayList<List<UUID>>();
       for (int i = 0; i < numGroups; i++) {
           engagement.add(new ArrayList<UUID>());
       }
       engagements.add(engagement);

       List<Permanent> blockers = getAvailableBlockers(game);
       addBlocker(game, engagement, blockers, engagements);

       return engagements;
   }

   private List<List<UUID>> copyEngagement(List<List<UUID>> engagement) {
       List<List<UUID>> newEngagement = new ArrayList<List<UUID>>();
       for (List<UUID> group: engagement) {
           newEngagement.add(new ArrayList<UUID>(group));
       }
       return newEngagement;
   }

   protected List<Permanent> remove(List<Permanent> source, Permanent element) {
       List<Permanent> newList = new ArrayList<Permanent>();
       for (Permanent permanent : source) {
           if (!permanent.equals(element)) {
               newList.add(permanent);
           }
       }
       return newList;
   }
   
   protected void addBlocker(Game game, List<List<UUID>> engagement, List<Permanent> blockers, List<List<List<UUID>>> engagements) {
       if (blockers.isEmpty())
           return;
       int numGroups = game.getCombat().getGroups().size();
       //try to block each attacker with each potential blocker
       Permanent blocker = blockers.get(0);
//       if (logger.isDebugEnabled())
//           logger.debug("simulating -- block:" + blocker);
       List<Permanent> remaining = remove(blockers, blocker);
       for (int i = 0; i < numGroups; i++) {
           if (game.getCombat().getGroups().get(i).canBlock(blocker, game)) {
               List<List<UUID>>newEngagement = copyEngagement(engagement);
               newEngagement.get(i).add(blocker.getId());
               engagements.add(newEngagement);
//                   logger.debug("simulating -- found redundant block combination");
               addBlocker(game, newEngagement, remaining, engagements);  // and recurse minus the used blocker
           }
       }
       addBlocker(game, engagement, remaining, engagements);
   }

   public NextAction getNextAction() {
       return nextAction;
   }

   public void setNextAction(NextAction action) {
       this.nextAction = action;
   }

}