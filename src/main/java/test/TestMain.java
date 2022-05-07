package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import mage.abilities.ActivatedAbility;
import mage.cards.Card;
import mage.cards.CardSetInfo;
import mage.cards.a.AbbeyMatron;
import mage.cards.basiclands.Plains;
import mage.cards.decks.Deck;
import mage.cards.u.UndiscoveredParadise;
import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.constants.Rarity;
import mage.constants.Zone;
import mage.game.GameImpl;
import mage.game.permanent.PermanentCard;
import mage.game.turn.Phase;
import mage.game.turn.PreCombatMainPhase;
import mage.game.turn.PreCombatMainStep;
import mage.players.PlayerImpl;

public class TestMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestPlayer testPlayer = new TestPlayer("test",RangeOfInfluence.ALL);
		UUID owner = testPlayer.getId();
		CardSetInfo info = new CardSetInfo("","","",Rarity.COMMON);
		AbbeyMatron testCard = new AbbeyMatron(owner,info);
		TestGame testGame = new TestGame();
		testGame.addPlayer(testPlayer,new Deck());
		ArrayList<PermanentCard> lands = lands(3,owner,testGame);
		ArrayList<Card> hand = new ArrayList<Card>();
		hand.add(testCard);
		testGame.cheat(owner,new ArrayList<Card>(), hand, lands, new ArrayList<Card>(), new ArrayList<Card>());
		//HumanPlayer h = new HumanPlayer();
		
		for(UUID card: testPlayer.getHand()) {
			System.out.println(testPlayer.getHand().get(card, testGame));
		}
		System.out.println(testGame.getBattlefield());
		
		for(Card card: testGame.getBattlefield().getAllPermanents()) {
			System.out.println(card);
		}
		
		testGame.getState().getTurn().setPhase(new PreCombatMainPhase());
		Phase testPhase = testGame.getState().getTurn().getPhase();
		
		testGame.getState().getTurn().getPhase().setStep(new PreCombatMainStep());
		
		testGame.getState().setActivePlayerId(owner);
		
		System.out.println(testGame.isMainPhase());
		System.out.println(testGame.isActivePlayer(owner));
		System.out.println(testGame.getStack().isEmpty());
		
		
		
		LinkedHashMap<UUID,ActivatedAbility> playable = PlayerImpl.getSpellAbilities(owner,testCard, Zone.HAND, testGame);
		
		
		
		//System.out.println(playable);
		for(UUID abilityId : playable.keySet()) {
			System.out.println(playable.get(abilityId));
		}
		
	}
	
	private static ArrayList<PermanentCard> lands(int num,UUID ownerUuid,GameImpl game){
		ArrayList<PermanentCard> paradises = new ArrayList<PermanentCard>();
		CardSetInfo cardInfo = new CardSetInfo("","","",Rarity.COMMON);
		while(paradises.size() < num*2) {
			paradises.add(new PermanentCard(new Plains(ownerUuid, cardInfo),ownerUuid,game));
			paradises.add(new PermanentCard(new UndiscoveredParadise(ownerUuid, cardInfo),ownerUuid,game));
		}
		
		return paradises;
	}

}
