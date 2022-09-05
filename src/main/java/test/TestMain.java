package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import mage.player.ai.MCTSPlayer;
import mage.players.PlayerImpl;

public class TestMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		TestPlayer testPlayer = new TestPlayer("test",RangeOfInfluence.ALL);
//		TestPlayer testPlayer2 = new TestPlayer("test",RangeOfInfluence.ALL);
//		UUID owner = testPlayer.getId();
//		CardSetInfo info = new CardSetInfo("","","",Rarity.COMMON);
//		AbbeyMatron testCard = new AbbeyMatron(owner,info);
//		Plains testPlains = new Plains(owner,info);
//		TestGame testGame = new TestGame();
//		testGame.addPlayer(testPlayer,new Deck());
//		testGame.addPlayer(testPlayer2,new Deck());
//		ArrayList<PermanentCard> lands = lands(1,owner,testGame);
//		ArrayList<Card> hand = new ArrayList<Card>();
//		hand.add(testCard);
//		hand.add(testPlains);
//		
//		testGame.cheat(owner,new ArrayList<Card>(), hand, lands, new ArrayList<Card>(), new ArrayList<Card>());
//		testPlayer.beginTurn(testGame);
//		//HumanPlayer h = new HumanPlayer();
//		
//		for(UUID card: testPlayer.getHand()) {
//			System.out.println(testPlayer.getHand().get(card, testGame));
//		}
//		System.out.println(testGame.getBattlefield());
//		
//		for(Card card: testGame.getBattlefield().getAllPermanents()) {
//			System.out.println(card);
//		}
//		
//		testGame.getState().getTurn().setPhase(new PreCombatMainPhase());
//		Phase testPhase = testGame.getState().getTurn().getPhase();
//		
//		testGame.getState().getTurn().getPhase().setStep(new PreCombatMainStep());
//		
//		testGame.getState().setActivePlayerId(owner);
//		
//		System.out.println(testGame.isMainPhase());
//		System.out.println(testGame.isActivePlayer(owner));
//		System.out.println(testGame.getStack().isEmpty());
//		
//		List<ActivatedAbility> playableList = testPlayer.getPlayable(testGame, false);
//		
//		System.out.println(playableList);
		
		
		MCTSPlayer player2 = new MCTSPlayer(new UUID(0,0));
		System.out.println(player2.getId());
//		LinkedHashMap<UUID,ActivatedAbility> playable = PlayerImpl.getSpellAbilities(owner,testCard, Zone.HAND, testGame);
//		
//		LinkedHashMap<UUID,ActivatedAbility> playable2 = PlayerImpl.getSpellAbilities(owner,testPlains, Zone.HAND, testGame);
//		
//		System.out.println(testCard.getAbilities());
//		
//		System.out.println(testPlains.getAbilities());
//		
//		System.out.println(playable);
//		for(UUID abilityId : playable2.keySet()) {
//			System.out.println(playable2.get(abilityId));
//		}
		
	}
	
	private static ArrayList<PermanentCard> lands(int num,UUID ownerUuid,GameImpl game){
		ArrayList<PermanentCard> paradises = new ArrayList<PermanentCard>();
		CardSetInfo cardInfo = new CardSetInfo("","","",Rarity.COMMON);
		while(paradises.size() < num) {
//			paradises.add(new PermanentCard(new Plains(ownerUuid, cardInfo),ownerUuid,game));
			paradises.add(new PermanentCard(new UndiscoveredParadise(ownerUuid, cardInfo),ownerUuid,game));
		}
		
		return paradises;
	}

}
