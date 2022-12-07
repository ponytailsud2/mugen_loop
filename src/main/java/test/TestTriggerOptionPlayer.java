package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import mage.MageObject;
import mage.Mana;
import mage.abilities.Ability;
import mage.abilities.ActivatedAbility;
import mage.abilities.SpellAbility;
import mage.abilities.common.PassAbility;
import mage.abilities.keyword.EquipAbility;
import mage.abilities.keyword.FlashAbility;
import mage.abilities.mana.ActivatedManaAbilityImpl;
import mage.abilities.mana.ManaAbility;
import mage.abilities.mana.ManaOptions;
import mage.cards.Card;
import mage.constants.Zone;
import mage.filter.common.FilterNonlandCard;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.permanent.Permanent;

public class TestTriggerOptionPlayer extends TestTreePlayer {
	
	private Map<Mana, Card> unplayable = new TreeMap<>();
    private List<Card> playableNonInstant = new ArrayList<>();
    private List<Card> playableInstant = new ArrayList<>();
    private List<ActivatedAbility> playableAbilities = new ArrayList<>();
    private HashMap<Ability,Ability> optionTest = new HashMap<Ability,Ability>();

	public TestTriggerOptionPlayer(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	public TestTriggerOptionPlayer(TestTreePlayer player) {
		super(player);
		// TODO Auto-generated constructor stub
	}
	
	protected void findPlayables(Game game) {
        playableInstant.clear();
        playableNonInstant.clear();
        unplayable.clear();
		playableAbilities.clear();
        Set<Card> nonLands = hand.getCards(new FilterNonlandCard(), game);
        ManaOptions available = getManaAvailable(game);
//        available.addMana(manaPool.getMana());

        for (Card card : nonLands) {
            ManaOptions options = card.getManaCost().getOptions();
            if (!card.getManaCost().getVariableCosts().isEmpty()) {
                //don't use variable mana costs unless there is at least 3 extra mana for X
                for (Mana option : options) {
                    option.add(Mana.GenericMana(3));
                }
            }
            for (Mana mana : options) {
                for (Mana avail : available) {
                    if (mana.enough(avail)) {
                        SpellAbility ability = card.getSpellAbility();
                        GameEvent castEvent = GameEvent.getEvent(GameEvent.EventType.CAST_SPELL, ability.getId(), ability, playerId);
                        castEvent.setZone(game.getState().getZone(card.getMainCard().getId()));
                        if (ability != null && ability.canActivate(playerId, game).canActivate()
                                && !game.getContinuousEffects().preventedByRuleModification(castEvent, ability, game, true)) {
                            if (card.isInstant(game)
                                    || card.hasAbility(FlashAbility.getInstance(), game)) {
                                playableInstant.add(card);
                            } else {
                                playableNonInstant.add(card);
                            }
                        }
                    } else if (!playableInstant.contains(card) && !playableNonInstant.contains(card)) {
                        unplayable.put(mana.needed(avail), card);
                    }
                }
            }
        }
        // TODO: wtf?! change to player.getPlayable
        for (Permanent permanent : game.getBattlefield().getAllActivePermanents(playerId)) {
            for (ActivatedAbility ability : permanent.getAbilities().getActivatedAbilities(Zone.BATTLEFIELD)) {
                if (!(ability instanceof ActivatedManaAbilityImpl) && ability.canActivate(playerId, game).canActivate()) {
                    if (ability instanceof EquipAbility && permanent.getAttachedTo() != null) {
                        continue;
                    }
                    ManaOptions abilityOptions = ability.getManaCosts().getOptions();
                    if (!ability.getManaCosts().getVariableCosts().isEmpty()) {
                        //don't use variable mana costs unless there is at least 3 extra mana for X
                        for (Mana option : abilityOptions) {
                            option.add(Mana.GenericMana(3));
                        }
                    }
                    if (abilityOptions.isEmpty()) {
                        playableAbilities.add(ability);
                    } else {
                        for (Mana mana : abilityOptions) {
                            for (Mana avail : available) {
                                if (mana.enough(avail)) {
                                    playableAbilities.add(ability);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Card card : graveyard.getCards(game)) {
            for (ActivatedAbility ability : card.getAbilities(game).getActivatedAbilities(Zone.GRAVEYARD)) {
                if (ability.canActivate(playerId, game).canActivate()) {
                    ManaOptions abilityOptions = ability.getManaCosts().getOptions();
                    if (abilityOptions.isEmpty()) {
                        playableAbilities.add(ability);
                    } else {
                        for (Mana mana : abilityOptions) {
                            for (Mana avail : available) {
                                if (mana.enough(avail)) {
                                    playableAbilities.add(ability);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
	
	@Override
	public boolean priority(Game game) {
//		System.out.println(this.getManaAvailable(game));
		List<ActivatedAbility> abilities = getPlayableAbilities(game);
//		System.out.println(abilities);
//		System.out.println(game.getBattlefield());
//		System.out.println(game.getStack());
//		System.out.println(game.canPlaySorcery(playerId));
		
		findPlayables(game);
		if(playableInstant.isEmpty()&&!game.canPlaySorcery(playerId)) {
			pass(game);
			return true;
		}
//		for(ActivatedAbility possible : abilities) {
//			for(Ability option : getPlayableOptions(possible, game)) {
//				optionTest.put(option, possible);
//			}
//		}
//		System.out.println("option :"+optionTest);
		for(ActivatedAbility possible: abilities) {
			if(!(possible instanceof PassAbility)&&!(possible instanceof ManaAbility)) {
//				System.out.println("Ability: "+possible);
//				System.out.println("Mana left: "+this.getManaAvailable(game));
				if(this.activateAbility(possible, game)) {
//					System.out.println("Mana left: "+this.getManaAvailable(game));
					return true;
				}
			}
		}

		return false;
	}

}
