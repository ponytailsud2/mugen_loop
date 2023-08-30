package test;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


import java.util.Map.Entry;

import mage.ApprovingObject;
import mage.ConditionalMana;
import mage.MageObject;
import mage.Mana;
import mage.abilities.Abilities;
import mage.abilities.AbilitiesImpl;
import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.Mode;
import mage.abilities.Modes;
import mage.abilities.SpecialAction;
import mage.abilities.SpellAbility;
import mage.abilities.TriggeredAbility;
import mage.abilities.common.PassAbility;
import mage.abilities.costs.Cost;
import mage.abilities.costs.VariableCost;
import mage.abilities.costs.mana.ColoredManaCost;
import mage.abilities.costs.mana.ColorlessManaCost;
import mage.abilities.costs.mana.GenericManaCost;
import mage.abilities.costs.mana.HybridManaCost;
import mage.abilities.costs.mana.ManaCost;
import mage.abilities.costs.mana.ManaCosts;
import mage.abilities.costs.mana.MonoHybridManaCost;
import mage.abilities.costs.mana.PhyrexianManaCost;
import mage.abilities.costs.mana.SnowManaCost;
import mage.abilities.mana.ActivatedManaAbilityImpl;
import mage.abilities.mana.ManaOptions;
import mage.cards.Card;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.cards.decks.Deck;
import mage.choices.Choice;
import mage.constants.AsThoughEffectType;
import mage.constants.CardType;
import mage.constants.ManaType;
import mage.constants.MultiAmountType;
import mage.constants.Outcome;
import mage.constants.RangeOfInfluence;
import mage.constants.Zone;
import mage.counters.Counters;
import mage.filter.common.FilterLandCard;
import mage.game.Game;
import mage.game.Graveyard;
import mage.game.combat.CombatGroup;
import mage.game.draft.Draft;
import mage.game.events.GameEvent;
import mage.game.match.Match;
import mage.game.permanent.Permanent;
import mage.game.stack.StackAbility;
import mage.game.stack.StackObject;
import mage.game.tournament.Tournament;
import mage.player.ai.ComputerPlayer;
import mage.player.ai.MCTSPlayer.NextAction;
import mage.players.Library;
import mage.players.ManaPool;
import mage.players.ManaPoolItem;
import mage.players.Player;
import mage.players.PlayerImpl;
import mage.target.Target;
import mage.target.TargetAmount;
import mage.target.TargetCard;
import mage.target.TargetImpl;
import mage.target.Targets;
import mage.util.CardUtil;
import mage.util.ManaUtil;
import mage.util.RandomUtil;


public class TestTreePlayer extends PlayerImpl{


   //private static final Logger logger = Logger.getLogger(MCTSPlayer.class);

   protected PassAbility pass = new PassAbility();
   
   private static HashMap<UUID,List<Targets>> pastTarget = new HashMap<UUID,List<Targets>>();

   private NextAction nextAction;
   
   private transient ManaCost currentUnpaidMana;
   
   private int level;
   
   private StackObject resolvingAbility;
   private int chooseUseRollBack = 0;
   //{Level:answer for this Lv}
   private HashMap<Integer,Boolean> chooseUseMap = new HashMap<Integer,Boolean>();
   private HashMap<Integer,Integer> chooseReplacementMap = new HashMap<Integer,Integer>();
   private SpanningTree tree;
   
   public enum NextAction {
       PRIORITY, TRIGGERED, CHOOSE_USE, CHOOSE_REPLACEMENT, CHOOSE
   }
   

   public TestTreePlayer(String name) {
	   super(name,RangeOfInfluence.ALL);
       this.pass.setControllerId(this.getId());
       human = false;
       this.setTestMode(true);
   }
   
   public TestTreePlayer(final TestTreePlayer player) {
	   super(player);
	   this.tree = player.getTree();
	   this.chooseUseMap = player.getChooseUseMap();
	   this.chooseReplacementMap = player.getChooseReplacementMap();
   }
   
   @Override
   public boolean priority(Game game) {
//	   if(game.getsta)
	   
	   game.pause();
       nextAction = NextAction.PRIORITY;
       return false;
   }
   
   
   
//   @Override
//   public boolean triggerAbility(TriggeredAbility source, Game game) {
//	   List<TriggeredAbility> triggeringAbilities = game.getState().getTriggered(playerId);
////	   System.out.println(triggeringAbilities);
//	   game.pause();
//	   nextAction = NextAction.TRIGGERED;
//	   game.resume();
//	   return true;
//   }
   
   public SpanningTree getTree() {
	return tree;
}

public void setTree(SpanningTree tree) {
	this.tree = tree;
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
//           System.out.println("Playable: |"+ability.getRule()+" | "+game.getPlayer(playerId).getPlayableOptions(ability, game));
           for(Ability abOptions:game.getPlayer(playerId).getPlayableOptions(ability, game)) {
        	   for(Target target:abOptions.getAllSelectedTargets()) {
        		   System.out.println("target: "+ target.getFirstTarget());
        	   }
        	   for(Cost cost: abOptions.getCosts()) {
        		   for(Target target:cost.getTargets()) {
        			   System.out.println("cost target: " + target.getFirstTarget());
        		   }
        	   }
           }
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
   
//   @Override
//   public boolean triggerAbility(TriggeredAbility source,Game game) {
//	   if (source != null && source.canChooseTarget(game, playerId)) {
//           Ability ability;
//           List<Ability> options = getPlayableOptions(source, game);
//           if (options.isEmpty()) {
//               ability = source;
//           } else {
//               if (options.size() == 1) {
//                   ability = options.get(0);
//                   
//               } else {
//            	   if(pastTarget.containsKey(source.getId())) {
//            		   ability = options.get(pastTarget.get(source.getId()).size());
//            	   }
//            	   
//                   ability = options.get(0);
//               }
//               if(!pastTarget.containsKey(source.getId())) {
//            	   ArrayList<Targets> targets = new ArrayList<Targets>();
//            	   targets.add(ability.getTargets());
//            	   pastTarget.put(source.getId(), targets);
//               }
//               else {
//            	   pastTarget.get(source.getId()).add(ability.getTargets());
//               }
//           }
//           if (ability.isUsesStack()) {
//               game.getStack().push(new StackAbility(ability, playerId));
//               if (ability.activate(game, false)) {
//                   game.fireEvent(new GameEvent(GameEvent.EventType.TRIGGERED_ABILITY, ability.getId(), ability, ability.getControllerId()));
//                   return true;
//               }
//           } else {
//               if (ability.activate(game, false)) {
//                   ability.resolve(game);
//                   return true;
//               }
//           }
//       }
//       return false;
//   }

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

   @Override
   public boolean choose(Outcome outcome, Target target, UUID sourceId, Game game, Map<String, Serializable> options) {
	   
	  return false;
	   
   }
   
   public Target nextTarget(Ability source, Target availableTarget,Game game) {
	   List<? extends Target> targets = availableTarget.getTargetOptions(source, game);
	   return availableTarget;
	  
   }
   
   public NextAction getNextAction() {
       return nextAction;
   }

   public void setNextAction(NextAction action) {
       this.nextAction = action;
   }
   
   private List<MageObject> getSortedProducers(ManaCosts<ManaCost> unpaid, Game game) {
       List<MageObject> unsorted = this.getAvailableManaProducers(game);
       unsorted.addAll(this.getAvailableManaProducersWithCost(game));
       Map<MageObject, Integer> scored = new HashMap<>();
       for (MageObject mageObject : unsorted) {
           int score = 0;
           for (ManaCost cost : unpaid) {
               Abilities:
               for (ActivatedManaAbilityImpl ability : mageObject.getAbilities().getAvailableActivatedManaAbilities(Zone.BATTLEFIELD, playerId, game)) {
                   for (Mana netMana : ability.getNetMana(game)) {
                       if (cost.testPay(netMana)) {
                           score++;
                           break Abilities;
                       }
                   }
               }
           }
           if (score > 0) { // score mana producers that produce other mana types and have other uses higher
               score += mageObject.getAbilities().getAvailableActivatedManaAbilities(Zone.BATTLEFIELD, playerId, game).size();
               score += mageObject.getAbilities().getActivatedAbilities(Zone.BATTLEFIELD).size();
               if (!mageObject.getCardType(game).contains(CardType.LAND)) {
                   score += 2;
               } else if (mageObject.getCardType(game).contains(CardType.CREATURE)) {
                   score += 2;
               }
           }
           scored.put(mageObject, score);
       }
       return sortByValue(scored);
   }
   
   private List<MageObject> sortByValue(Map<MageObject, Integer> map) {
       List<Entry<MageObject, Integer>> list = new LinkedList<>(map.entrySet());
       Collections.sort(list, new Comparator<Entry<MageObject, Integer>>() {
           @Override
           public int compare(Entry<MageObject, Integer> o1, Entry<MageObject, Integer> o2) {
               return (o1.getValue().compareTo(o2.getValue()));
           }
       });
       List<MageObject> result = new ArrayList<>();
       for (Entry<MageObject, Integer> entry : list) {
           result.add(entry.getKey());
       }
       return result;
   }
   
   private Abilities<ActivatedManaAbilityImpl> getManaAbilitiesSortedByManaCount(MageObject mageObject, final Game game) {
       Abilities<ActivatedManaAbilityImpl> manaAbilities = mageObject.getAbilities().getAvailableActivatedManaAbilities(Zone.BATTLEFIELD, playerId, game);
       if (manaAbilities.size() > 1) {
           // Sort mana abilities by number of produced manas, to use ability first that produces most mana (maybe also conditional if possible)
           Collections.sort(manaAbilities, new Comparator<ActivatedManaAbilityImpl>() {
               @Override
               public int compare(ActivatedManaAbilityImpl a1, ActivatedManaAbilityImpl a2) {
                   int a1Max = 0;
                   for (Mana netMana : a1.getNetMana(game)) {
                       if (netMana.count() > a1Max) {
                           a1Max = netMana.count();
                       }
                   }
                   int a2Max = 0;
                   for (Mana netMana : a2.getNetMana(game)) {
                       if (netMana.count() > a2Max) {
                           a2Max = netMana.count();
                       }
                   }
                   return a2Max - a1Max;
               }
           });
       }
       return manaAbilities;
   }
   
   boolean canUseAsThoughManaToPayManaCost(ManaCost checkCost, Ability abilityToPay, Mana manaOption, Ability manaAbility, MageObject manaProducer, Game game) {
       // asThoughMana can change producing mana type, so you must check it here
       // cause some effects adds additional checks in getAsThoughManaType (example: Draugr Necromancer with snow mana sources)

       // simulate real asThoughMana usage
       ManaPoolItem possiblePoolItem;
       if (manaOption instanceof ConditionalMana) {
           ConditionalMana conditionalNetMana = (ConditionalMana) manaOption;
           possiblePoolItem = new ManaPoolItem(
                   conditionalNetMana,
                   manaAbility.getSourceObject(game),
                   conditionalNetMana.getManaProducerOriginalId() != null ? conditionalNetMana.getManaProducerOriginalId() : manaAbility.getOriginalId()
           );
       } else {
           possiblePoolItem = new ManaPoolItem(
                   manaOption.getRed(),
                   manaOption.getGreen(),
                   manaOption.getBlue(),
                   manaOption.getWhite(),
                   manaOption.getBlack(),
                   manaOption.getGeneric() + manaOption.getColorless(),
                   manaProducer,
                   manaAbility.getOriginalId(),
                   manaOption.getFlag()
           );
       }

       // cost can contains multiple mana types, must check each type (is it possible to pay a cost)
       for (ManaType checkType : ManaUtil.getManaTypesInCost(checkCost)) {
           // affected asThoughMana effect must fit a checkType with pool mana
           ManaType possibleAsThoughPoolManaType = game.getContinuousEffects().asThoughMana(checkType, possiblePoolItem, abilityToPay.getSourceId(), abilityToPay, abilityToPay.getControllerId(), game);
           if (possibleAsThoughPoolManaType == null) {
               continue; // no affected asThough effects
           }
           boolean canPay;
           if (possibleAsThoughPoolManaType == ManaType.COLORLESS) {
               // colorless can be payed by any color from the pool
               canPay = possiblePoolItem.count() > 0;
           } else {
               // colored must be payed by specific color from the pool (AsThough already changed it to fit with mana pool)
               canPay = possiblePoolItem.get(possibleAsThoughPoolManaType) > 0;
           }
           if (canPay) {
               return true;
           }
       }

       return false;
   }
   
   public boolean triggerAbility(TriggeredAbility source, Game game) {
//     logger.info("trigger");
     if (source != null && source.canChooseTarget(game, playerId)) {
    
         if (source.isUsesStack()) {
             game.getStack().push(new StackAbility(source, playerId));
             if (source.activate(game, false)) {
                 game.fireEvent(new GameEvent(GameEvent.EventType.TRIGGERED_ABILITY, source.getId(), source, source.getControllerId()));
                 
                 return true;
             }
         } else {
             if (source.activate(game, false)) {
                 source.resolve(game);
                 
                 return true;
             }
         }
     }
     return false;
 }

   protected boolean playManaHandling(Ability ability, ManaCost unpaid, final Game game) {
//     log.info("paying for " + unpaid.getText());
     ApprovingObject approvingObject = game.getContinuousEffects().asThough(ability.getSourceId(), AsThoughEffectType.SPEND_OTHER_MANA, ability, ability.getControllerId(), game);
     ManaCost cost;
     List<MageObject> producers;
     if (unpaid instanceof ManaCosts) {
         ManaCosts<ManaCost> manaCosts = (ManaCosts<ManaCost>) unpaid;
         cost = manaCosts.get(manaCosts.size() - 1);
         producers = getSortedProducers((ManaCosts) unpaid, game);
     } else {
         cost = unpaid;
         producers = this.getAvailableManaProducers(game);
         producers.addAll(this.getAvailableManaProducersWithCost(game));
     }
     for (MageObject mageObject : producers) {
         // use color producing mana abilities with costs first that produce all color manas that are needed to pay
         // otherwise the computer may not be able to pay the cost for that source
         ManaAbility:
         for (ActivatedManaAbilityImpl manaAbility : getManaAbilitiesSortedByManaCount(mageObject, game)) {
             int colored = 0;
             for (Mana mana : manaAbility.getNetMana(game)) {
                 if (!unpaid.getMana().includesMana(mana)) {
                     continue ManaAbility;
                 }
                 colored += mana.countColored();
             }
             if (colored > 1 && (cost instanceof ColoredManaCost)) {
                 for (Mana netMana : manaAbility.getNetMana(game)) {
                     if (cost.testPay(netMana)) {
                         if (netMana instanceof ConditionalMana && !((ConditionalMana) netMana).apply(ability, game, getId(), cost)) {
                             continue;
                         }
                         if (approvingObject != null && !canUseAsThoughManaToPayManaCost(cost, ability, netMana, manaAbility, mageObject, game)) {
                             continue;
                         }
                         if (activateAbility(manaAbility, game)) {
                             return true;
                         }
                     }
                 }
             }
         }
     }

     for (MageObject mageObject : producers) {
         // pay all colored costs first
         for (ActivatedManaAbilityImpl manaAbility : getManaAbilitiesSortedByManaCount(mageObject, game)) {
             if (cost instanceof ColoredManaCost) {
                 for (Mana netMana : manaAbility.getNetMana(game)) {
                     if (cost.testPay(netMana) || approvingObject != null) {
                         if (netMana instanceof ConditionalMana && !((ConditionalMana) netMana).apply(ability, game, getId(), cost)) {
                             continue;
                         }
                         if (approvingObject != null && !canUseAsThoughManaToPayManaCost(cost, ability, netMana, manaAbility, mageObject, game)) {
                             continue;
                         }
                         if (activateAbility(manaAbility, game)) {
                             return true;
                         }
                     }
                 }
             }
         }
         // pay snow covered mana
         for (ActivatedManaAbilityImpl manaAbility : getManaAbilitiesSortedByManaCount(mageObject, game)) {
             if (cost instanceof SnowManaCost) {
                 for (Mana netMana : manaAbility.getNetMana(game)) {
                     if (cost.testPay(netMana) || approvingObject != null) {
                         if (netMana instanceof ConditionalMana && !((ConditionalMana) netMana).apply(ability, game, getId(), cost)) {
                             continue;
                         }
                         if (approvingObject != null && !canUseAsThoughManaToPayManaCost(cost, ability, netMana, manaAbility, mageObject, game)) {
                             continue;
                         }
                         if (activateAbility(manaAbility, game)) {
                             return true;
                         }
                     }
                 }
             }
         }
         // then pay hybrid
         for (ActivatedManaAbilityImpl manaAbility : getManaAbilitiesSortedByManaCount(mageObject, game)) {
             if (cost instanceof HybridManaCost) {
                 for (Mana netMana : manaAbility.getNetMana(game)) {
                     if (cost.testPay(netMana) || approvingObject != null) {
                         if (netMana instanceof ConditionalMana && !((ConditionalMana) netMana).apply(ability, game, getId(), cost)) {
                             continue;
                         }
                         if (approvingObject != null && !canUseAsThoughManaToPayManaCost(cost, ability, netMana, manaAbility, mageObject, game)) {
                             continue;
                         }
                         if (activateAbility(manaAbility, game)) {
                             return true;
                         }
                     }
                 }
             }
         }
         // then pay mono hybrid
         for (ActivatedManaAbilityImpl manaAbility : getManaAbilitiesSortedByManaCount(mageObject, game)) {
             if (cost instanceof MonoHybridManaCost) {
                 for (Mana netMana : manaAbility.getNetMana(game)) {
                     if (cost.testPay(netMana) || approvingObject != null) {
                         if (netMana instanceof ConditionalMana && !((ConditionalMana) netMana).apply(ability, game, getId(), cost)) {
                             continue;
                         }
                         if (approvingObject != null && !canUseAsThoughManaToPayManaCost(cost, ability, netMana, manaAbility, mageObject, game)) {
                             continue;
                         }
                         if (activateAbility(manaAbility, game)) {
                             return true;
                         }
                     }
                 }
             }
         }
         // pay colorless
         for (ActivatedManaAbilityImpl manaAbility : getManaAbilitiesSortedByManaCount(mageObject, game)) {
             if (cost instanceof ColorlessManaCost) {
                 for (Mana netMana : manaAbility.getNetMana(game)) {
                     if (cost.testPay(netMana) || approvingObject != null) {
                         if (netMana instanceof ConditionalMana && !((ConditionalMana) netMana).apply(ability, game, getId(), cost)) {
                             continue;
                         }
                         if (approvingObject != null && !canUseAsThoughManaToPayManaCost(cost, ability, netMana, manaAbility, mageObject, game)) {
                             continue;
                         }
                         if (activateAbility(manaAbility, game)) {
                             return true;
                         }
                     }
                 }
             }
         }
         // finally pay generic
         for (ActivatedManaAbilityImpl manaAbility : getManaAbilitiesSortedByManaCount(mageObject, game)) {
             if (cost instanceof GenericManaCost) {
                 for (Mana netMana : manaAbility.getNetMana(game)) {
                     if (cost.testPay(netMana) || approvingObject != null) {
                         if (netMana instanceof ConditionalMana && !((ConditionalMana) netMana).apply(ability, game, getId(), cost)) {
                             continue;
                         }
                         if (approvingObject != null && !canUseAsThoughManaToPayManaCost(cost, ability, netMana, manaAbility, mageObject, game)) {
                             continue;
                         }
                         if (activateAbility(manaAbility, game)) {
                             return true;
                         }
                     }
                 }
             }
         }
     }

     // pay phyrexian life costs
     if (cost instanceof PhyrexianManaCost) {
         return cost.pay(ability, game, ability, playerId, false, null) || approvingObject != null;
     }

     // pay special mana like convoke cost (tap for pay)
     // GUI: user see "special" button while pay spell's cost
     // TODO: AI can't prioritize special mana types to pay, e.g. it will use first available
     SpecialAction specialAction = game.getState().getSpecialActions().getControlledBy(this.getId(), true)
             .values().stream().findFirst().orElse(null);
     ManaOptions specialMana = specialAction == null ? null : specialAction.getManaOptions(ability, game, unpaid);
     if (specialMana != null) {
         for (Mana netMana : specialMana) {
             if (cost.testPay(netMana) || approvingObject != null) {
                 if (netMana instanceof ConditionalMana && !((ConditionalMana) netMana).apply(ability, game, getId(), cost)) {
                     continue;
                 }
                 specialAction.setUnpaidMana(unpaid);
                 if (activateAbility(specialAction, game)) {
                     return true;
                 }
                 // only one time try to pay
                 break;
             }
         }
     }

     return false;
 }
   
   @Override
   protected boolean specialAction(SpecialAction action, Game game) {
	   
	   return false;
   }
   
   public void resetChoose() {
	   
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
public Player copy() {
	// TODO Auto-generated method stub
	return new TestTreePlayer(this);
}

@Override
public boolean choose(Outcome outcome, Target target, UUID sourceId, Game game) {
	// TODO Auto-generated method stub
	
	return true;
}

@Override
public boolean choose(Outcome outcome, Cards cards, TargetCard target, Game game) {
	// TODO Auto-generated method stub
	
	return true;
}

@Override
public boolean choose(Outcome outcome, Choice choice, Game game) {
	// TODO Auto-generated method stub
	return false;
}

//for sacrifice/
@Override
public boolean chooseTarget(Outcome outcome, Target target, Ability source, Game game) {
	// TODO Auto-generated method stub
	return false;
}

@Override
public boolean chooseTarget(Outcome outcome, Cards cards, TargetCard target, Ability source, Game game) {
	// TODO Auto-generated method stub
	List<Card> cardChoices = new ArrayList<>(cards.getCards(target.getFilter(), source != null ? source.getSourceId() : null, playerId, game));
	
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
	return chooseUse(outcome,message,"",null,null,source,game);
}

@Override
public boolean chooseUse(Outcome outcome, String message, String secondMessage, String trueText, String falseText,
		Ability source, Game game) {
	
		if(chooseUseMap.containsKey(this.tree.getCurrent().getLevel())) {
			return this.chooseUseMap.get(this.tree.getCurrent().getLevel());
		}
		//do expand process
		game.pause();
		this.nextAction = NextAction.CHOOSE_USE;
		this.tree.chooseProcess();
		
		return false;
}

@Override
public boolean choosePile(Outcome outcome, String message, List<? extends Card> pile1, List<? extends Card> pile2,
		Game game) {
	// TODO Auto-generated method stub
	return chooseUse(outcome,message,"",null,null,null,game);
}


@Override
public boolean playMana(Ability ability, ManaCost unpaid, String promptText, Game game) {
	payManaMode = true;
    currentUnpaidMana = unpaid;
    try {
        return playManaHandling(ability, unpaid, game);
    } finally {
        currentUnpaidMana = null;
        payManaMode = false;
    }
}

@Override
public int announceXMana(int min, int max, int multiplier, String message, Game game, Ability ability) {
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
	int level = this.getTree().getCurrent().getLevel();
	if(this.chooseReplacementMap.containsKey(level)) {
		return this.chooseReplacementMap.get(level);
	}
	
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
public UUID chooseBlockerOrder(List<Permanent> blockers, CombatGroup combatGroup, List<UUID> blockerOrder, Game game) {
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
  

public int getChooseUseRollBack() {
	return chooseUseRollBack;
}




public HashMap<Integer, Boolean> getChooseUseMap() {
	return chooseUseMap;
}



public void setChooseUseMap(HashMap<Integer, Boolean> chooseUseMap) {
	this.chooseUseMap = chooseUseMap;
}



public StackObject getResolvingAbility() {
	   return resolvingAbility;
  }

  public void setResolvingAbility(StackObject resolvingAbility) {
	   this.resolvingAbility = resolvingAbility;
  }

public HashMap<Integer, Integer> getChooseReplacementMap() {
	return chooseReplacementMap;
}

public void setChooseReplacementMap(HashMap<Integer, Integer> chooseReplacementMap) {
	this.chooseReplacementMap = chooseReplacementMap;
}


}