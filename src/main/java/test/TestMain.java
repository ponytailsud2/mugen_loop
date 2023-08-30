package test;

import java.util.ArrayList;

import java.util.UUID;

import org.apache.log4j.BasicConfigurator;

import mage.abilities.ActivatedAbility;
import mage.cards.Card;
import mage.cards.CardSetInfo;
import mage.cards.a.AbbeyMatron;
import mage.cards.a.Abjure;
import mage.cards.a.Aeolipile;
import mage.cards.a.AnabaShaman;
import mage.cards.a.AngelicPurge;
import mage.cards.b.BattleflightEagle;
import mage.cards.basiclands.Island;
import mage.cards.basiclands.Mountain;
import mage.cards.basiclands.Plains;
import mage.cards.decks.Deck;
import mage.cards.u.UndiscoveredParadise;
import mage.constants.MultiplayerAttackOption;
import mage.constants.PhaseStep;
import mage.constants.RangeOfInfluence;
import mage.constants.Rarity;
import mage.constants.Zone;
import mage.game.GameImpl;
import mage.game.GameOptions;
import mage.game.TwoPlayerDuel;
import mage.game.mulligan.LondonMulligan;
import mage.game.permanent.Permanent;
import mage.game.permanent.PermanentCard;
import mage.game.turn.Phase;
import mage.game.turn.PreCombatMainPhase;
import mage.game.turn.PreCombatMainStep;
import mage.player.ai.ComputerPlayer;
import mage.player.ai.MCTSPlayer;
import mage.players.PlayerImpl;
import test.TestGame.CurrentAction;

public class TestMain {
	
	
	
	public static void main(String[] args) {
		//simulate game set up 
		TestTriggerOptionPlayer testPlayer = new TestTriggerOptionPlayer("test");
		AlwaysPassPlayer testPlayer2 = new AlwaysPassPlayer(new UUID(1,1),"test2");
		UUID owner = testPlayer.getId();
		
		
		CardSetInfo info = new CardSetInfo("","","",Rarity.COMMON);
		AbbeyMatron testCard = new AbbeyMatron(owner,info);
		BattleflightEagle eagle = new BattleflightEagle(owner, info);
		AnabaShaman testAnaba = new AnabaShaman(owner,info);
		Plains testPlains = new Plains(owner,info);
		
		TwoPlayerDuel testGame = new TwoPlayerDuel(MultiplayerAttackOption.LEFT,RangeOfInfluence.ALL,new LondonMulligan(0),20,7);
		
//		TestGame testGame = new TestGame(new SpanningTree(testPlayer));
		testGame.setSimulation(false);
		//test
		testPlayer.init(testGame,true);
		testPlayer2.init(testGame,true);
		testGame.addPlayer(testPlayer,new Deck());
		testGame.addPlayer(testPlayer2,new Deck());
		
		System.out.println(testGame.getStartingLife());
		System.out.println(testGame.getPlayer(testPlayer2.getId()).getLibrary().size());
		ArrayList<PermanentCard> lands = lands(6,owner,testGame);
		ArrayList<PermanentCard> lands2 = lands(6,owner,testGame);
		PermanentCard anabafield = new PermanentCard(new AnabaShaman(testPlayer2.getId(),info),testPlayer2.getId(),testGame);
		ArrayList<Card> hand = new ArrayList<Card>();
		ArrayList<Card> hand2 = new ArrayList<Card>();
		AngelicPurge test_sacrifice_card = new AngelicPurge(owner, info);
		PermanentCard test_sacrifice = new PermanentCard(test_sacrifice_card,owner,testGame);		
		//hand.add(testCard);
		hand.add(test_sacrifice);
//		hand.add(eagle);
		//hand.add(testPlains);
//		lands.add(anabafield);
		lands2.add(anabafield);
		testPlayer.updateRange(testGame);
		testPlayer2.updateRange(testGame);
		GameOptions testGameOptions = new GameOptions();
		testGameOptions.testMode = true;
		
		testGame.cheat(testPlayer.getId(),library(testPlayer.getId(),testGame), hand, lands, new ArrayList<Card>(), new ArrayList<Card>());
		
		
		testGame.cheat(testPlayer2.getId(),library(testPlayer2.getId(),testGame), hand2, lands2, new ArrayList<Card>(), new ArrayList<Card>());
		testGame.setGameOptions(testGameOptions);
//		anabafield.removeSummoningSickness();
		
		
		
//		System.out.println(testGame.checkIfGameIsOver());
		testGame.getState().getTurn().setPhase(new PreCombatMainPhase());
		System.out.println(testGame.getPhase());
//		Phase testPhase = testGame.getState().getTurn().getPhase();
		
		testGame.getState().getTurn().getPhase().setStep(new PreCombatMainStep());
		
		testGame.getState().setActivePlayerId(owner);
		
		System.out.println(testGame.isMainPhase());
		System.out.println(testGame.isActivePlayer(owner));
		System.out.println(testGame.getStack().isEmpty());
//		testGame.start(owner);
		System.out.println("Players: "+testGame.getState().getPlayers());
		System.out.println("Mana left: "+testPlayer.getManaAvailable(testGame));
		System.out.println("land id: ");
		for(PermanentCard land: lands) {
			System.out.println(land.getId());
		}
		System.out.println("-------------");
		System.out.println("anaba: "+anabafield.getId());
		System.out.println(testPlayer.getHand());
		System.out.println("Playable: "+testPlayer.getPlayableOptions(testGame));
		System.out.println("Cost: " + testPlayer.getPlayableOptions(testGame).get(0).getCosts().getTargets().toString());
//		testGame.startSim(testPlayer.getId());
		System.out.println("Playable: "+testPlayer2.getPlayableOptions(anabafield.getAbilities().get(0), testGame));
//		System.out.println(testCard);
//		System.out.println(testPlayer.activateAbility(eagle.getSpellAbility(), testGame));
		System.out.println("Mana left: "+testPlayer.getManaAvailable(testGame));
		System.out.println(testGame.getStack());
//		System.out.println("Active: "+testGame.getState().getActivePlayerId());
//		System.out.println(testGame.getState().getPlayerList().size());
//		
//		//testCard.playCard(testGame, Zone.HAND, Zone.BATTLEFIELD, owner);
//		//		testPlayer.beginTurn(testGame);
////		//HumanPlayer h = new HumanPlayer();
		System.out.println(anabafield.isControlledBy(owner));
		System.out.println(anabafield.isPhasedIn());
		//anabafield.removeSummoningSickness();
		System.out.println(anabafield.wasControlledFromStartOfControllerTurn());
		System.out.println("tap: "+anabafield.isTapped());

//		System.out.println(testGame.getBattlefield());
//		System.out.println(testGame.getBattlefield().getAllActivePermanents(testPlayer2.getId()));
		
//		BasicConfigurator.configure();
//		TestTreePlayer testPlayer = new TestTreePlayer("test");
//		AlwaysPassPlayer testPlayer2 = new AlwaysPassPlayer("test",RangeOfInfluence.ALL);
//		UUID owner = testPlayer.getId();
//		TestGame testGame = new TestGame(MultiplayerAttackOption.LEFT,RangeOfInfluence.ALL,new LondonMulligan(0),20,7);
//		CardSetInfo info = new CardSetInfo("","","",Rarity.COMMON);
//		Abjure test_sacrifice = new Abjure(owner, info);
////		AbbeyMatron testCard = new AbbeyMatron(owner,info);
////		BattleflightEagle eagle = new BattleflightEagle(owner, info);
////		AnabaShaman testAnaba = new AnabaShaman(owner,info);
////		Plains testPlains = new Plains(owner,info);
//
////		
//////		TestGame testGame = new TestGame(new SpanningTree(testPlayer));
////		testGame.setSimulation(false);
////		//test
//		testPlayer.init(testGame,true);
//		testPlayer2.init(testGame,true);
//		testGame.addPlayer(testPlayer,new Deck());
//		testGame.addPlayer(testPlayer2,new Deck());
////		
////		System.out.println(testGame.getStartingLife());
////		System.out.println(testGame.getPlayer(testPlayer2.getId()).getLibrary().size());
//		ArrayList<PermanentCard> lands = lands(6,owner,testGame);
//		ArrayList<PermanentCard> lands2 = lands(6,owner,testGame);
////		PermanentCard anabafield = new PermanentCard(new AnabaShaman(owner,info),owner,testGame);
//		ArrayList<Card> hand = new ArrayList<Card>();
//		ArrayList<Card> hand2 = new ArrayList<Card>();
//		hand.add(test_sacrifice);
////		hand.add(eagle);
////		//hand.add(testPlains);
////		lands.add(anabafield);
//		testPlayer.updateRange(testGame);
//		testPlayer2.updateRange(testGame);
//		GameOptions testGameOptions = new GameOptions();
//		testGameOptions.testMode = true;
////		
//		testGame.cheat(testPlayer.getId(),library(testPlayer.getId(),testGame), hand, lands, new ArrayList<Card>(), new ArrayList<Card>());
////		
////		
//		testGame.cheat(testPlayer2.getId(),library(testPlayer2.getId(),testGame), hand2, lands2, new ArrayList<Card>(), new ArrayList<Card>());
//		testGame.setGameOptions(testGameOptions);
//////		anabafield.removeSummoningSickness();
////		System.out.println("eagle: "+eagle.getId());
//		
//////		testGame.startSim(testPlayer.getId());
////		System.out.println("Playable: "+testPlayer2.getPlayableOptions(anabafield.getAbilities().get(0), testGame));
//////		System.out.println(testCard);
////		
////		System.out.println(testGame.getPhase());
//////		System.out.println(testGame.checkIfGameIsOver());
//		testGame.start(owner);
//		System.out.println("Players: "+testGame.getState().getPlayers());
//		System.out.println("Mana left: "+testPlayer.getManaAvailable(testGame));
//		System.out.println(testPlayer.getHand());
//		System.out.println("Playable: "+testPlayer.getPlayableOptions(testGame));
//		System.out.println("Activate: ");
//		System.out.println(testPlayer.activateAbility(eagle.getSpellAbility(), testGame));
//		System.out.println("Mana left: "+testPlayer.getManaAvailable(testGame));
//		System.out.println(testGame.getStack());
////		System.out.println("Active: "+testGame.getState().getActivePlayerId());
////		System.out.println(testGame.getState().getPlayerList().size());
////		
////		//testCard.playCard(testGame, Zone.HAND, Zone.BATTLEFIELD, owner);
////		//		testPlayer.beginTurn(testGame);
//////		//HumanPlayer h = new HumanPlayer();
//		System.out.println(anabafield.isControlledBy(owner));
//		System.out.println(anabafield.isPhasedIn());
//		//anabafield.removeSummoningSickness();
//		System.out.println(anabafield.wasControlledFromStartOfControllerTurn());
//		System.out.println("tap: "+anabafield.isTapped());
//
//		System.out.println(testGame.getBattlefield());
//		System.out.println(testGame.getBattlefield().getAllActivePermanents(testPlayer2.getId()));
//		System.out.println(testPlayer2.getPlayableOptions(null, testGame));
//		System.out.println(testGame.getStack());
//		System.out.println(testPlayer.playCard(testCard, testGame, false, false, null));
////		for(UUID card: testPlayer.getHand()) {
////			System.out.println(testPlayer.getHand().get(card, testGame));
////		}
//		
//		System.out.println(testGame.getBattlefield());
//		System.out.println(testGame.getStack());
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
		
		
//		MCTSPlayer player2 = new MCTSPlayer(new UUID(0,0));
//		System.out.println(player2.getId());
//		ComputerPlayer player4 = new ComputerPlayer(player2);
//	    player4 = (MCTSPlayer) player4;
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
		
//		SpanningTree tree = new SpanningTree();
	}
	
	private static ArrayList<PermanentCard> lands(int num,UUID ownerUuid,GameImpl game){
		ArrayList<PermanentCard> paradises = new ArrayList<PermanentCard>();
		CardSetInfo cardInfo = new CardSetInfo("","","",Rarity.COMMON);
		while(paradises.size() < num) {
			paradises.add(new PermanentCard(new Plains(ownerUuid, cardInfo),ownerUuid,game));
//			paradises.add(new PermanentCard(new Island(ownerUuid, cardInfo),ownerUuid,game));
//			paradises.add(new PermanentCard(new Mountain(ownerUuid, cardInfo),ownerUuid,game));
//			paradises.add(new PermanentCard(new AnabaShaman(ownerUuid,cardInfo),ownerUuid,game));
//			paradises.add(new PermanentCard(new UndiscoveredParadise(ownerUuid, cardInfo),ownerUuid,game));
		}
		
		return paradises;
	}

	private static ArrayList<Card> library(UUID ownerUuid,GameImpl game){
		ArrayList<Card> mockLibrary = new ArrayList<Card>();
		CardSetInfo cardInfo = new CardSetInfo("","","",Rarity.COMMON);
		for(int i = 0;i<4;i++) {
			mockLibrary.add(new UndiscoveredParadise(ownerUuid, cardInfo));
		}
		return mockLibrary;
	}
	
}
