package tests;

import org.junit.Assert;
import org.junit.Test;

import tictactoe.MockTicTacToe;
import tictactoe.Game;
import tictactoe.ITicTacToe;
import tictactoe.Board;

public class TestGame {
	
	/**
	 * unit tests: (GameOverMsg set appropriately, start() should exit,
	 * NotInGame set, etc.)
	 * 1. receive a 'w'
	 * 2. receive a 't'
	 * 3. receive a 'giveup'
	 * 4. receive a normal move
	 * 5. send a 'w'
	 * 6. send a 't'
	 * 7. send a 'giveup'
	 * 8. send a normal move
	 * (receive 'giveup' and receive Game.GIVEUP_TIME second expiry is same,
	 * because a Game.GIVEUP_TIME second expires triggers a 'giveup' message
	 * to be sent)
	 * 9. send Game.GIVEUP_TIME second timer expiry
	 * 10. receive Game.CNXN_LOSS_TIME second timer expiry
	*/

	@Test
	public void testReceiveWin() {
		/**
		 * Scenario where current player is P2, waiting to receive a move.
		 * P1 is about to win.
		 * Board looks like this initially:
		 * xox
		 * oxo
		 * ...
		 * 
		 * Receive move at position 7.
		 * Verify after Game.start() returns that:
		 * - position 7 contains 'x'
		 * - GameOverMsg == "Player 1 wins. You lose."
		 * - Game.NotInGame.get() == True
		 * - nothing is sent
		 */
		Board b = new Board();
		b.insert('x', 1);
		b.insert('o', 2);
		b.insert('x', 3);
		b.insert('o', 4);
		b.insert('x', 5);
		b.insert('o', 6);
		
		MockTicTacToe ttt = new MockTicTacToe(false, 8, "w7", b);
		ttt.getGame().start("",  "");
		
		Assert.assertEquals('x', b.getSymbolAtCoord(2, 0));
		Assert.assertEquals(ITicTacToe.P1_WIN_LOSE, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("", ttt.getFinalMessage());
	}

	@Test
	public void testReceiveTie() {
		/**
		 * Scenario where current player is P2, waiting to receive a move.
		 * The game is about to become a tie.
		 * Board looks like this initially:
		 * oxo
		 * xox
		 * xo.
		 * 
		 * Receive move at position 9.
		 * Verify after Game.start() returns that:
		 * - position 9 contains 'x'
		 * - GameOverMsg == "Tie game."
		 * - Game.NotInGame.get() == True
		 * - nothing is sent
		 */
		
		Board b = new Board();
		b.insert('x', 1);
		b.insert('o', 2);
		b.insert('x', 3);
		b.insert('o', 4);
		b.insert('x', 5);
		b.insert('o', 6);
		b.insert('o', 7);
		b.insert('x', 8);
		
		MockTicTacToe ttt = new MockTicTacToe(false, 8, "t9", b);
		ttt.getGame().start("",  "");
		
		Assert.assertEquals('x', b.getSymbolAtCoord(2, 2));
		Assert.assertEquals(ITicTacToe.TIE_GAME, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("", ttt.getFinalMessage());
	}
	
	@Test
	public void testReceiveGiveup() {
		/**
		 * Scenario where current player is P2, waiting to receive a move.
		 * P1 is about to giveup.
		 * Board is empty.
		 * 
		 * Receive "giveup".
		 * Verify after Game.start() returns that:
		 * - GameOverMsg = "Player 1 has given up. You win."
		 * - Game.NotInGame.get() == True
		 * - nothing is sent
		 */
		
		Board b = new Board();
		
		MockTicTacToe ttt = new MockTicTacToe(false, 8, "giveup", b);
		ttt.getGame().start("", "");
		
		Assert.assertEquals(ITicTacToe.P1_GIVEUP_WIN, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("", ttt.getFinalMessage());
	}
	
	@Test
	public void testReceiveNormalMove() {
		/**
		 * Scenario where current player is P2, waiting to receive a move.
		 * P1 is about to send a move.  After move gets received, P2 will
		 * giveup.
		 * Board looks like this initially:
		 * xo.
		 * ...
		 * ...
		 * 
		 * Receive move at position 3.
		 * Verify after Game.start() returns that:
		 * - position 3 contains 'x'
		 * - GameOverMsg = "You have given up. You lose."
		 * - Game.NotInGame.get() == True
		 * - "giveup" is sent
		 */
		
		Board b = new Board();
		b.insert('x', 1);
		b.insert('o', 2);
		
		MockTicTacToe ttt = new MockTicTacToe(false, -1, "3", b);
		ttt.getGame().start("", "");
		
		Assert.assertEquals('x', b.getSymbolAtCoord(0, 2));
		Assert.assertEquals(ITicTacToe.GIVEN_UP, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("giveup", ttt.getFinalMessage());
	}
	
	@Test
	public void testSendWin() {
		/**
		 * Scenario where current player is P1, sending a move.
		 * P1 is about to win.
		 * Board looks like this initially:
		 * xox
		 * oxo
		 * ...
		 * 
		 * Send move at position 7.
		 * Verify after Game.start() returns that:
		 * - position 7 contains 'x'
		 * - GameOverMsg == "You win."
		 * - Game.NotInGame.get() == True
		 * - "w7" is sent
		 */
		Board b = new Board();
		b.insert('x', 1);
		b.insert('o', 2);
		b.insert('x', 3);
		b.insert('o', 4);
		b.insert('x', 5);
		b.insert('o', 6);
		
		MockTicTacToe ttt = new MockTicTacToe(true, 7, "8", b);
		ttt.getGame().start("",  "");
		
		Assert.assertEquals('x', b.getSymbolAtCoord(2, 0));
		Assert.assertEquals(ITicTacToe.YOU_WIN, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("w7", ttt.getFinalMessage());
	}
	
	@Test
	public void testSendTie() {
		/**
		 * Scenario where current player is P1, sending a move.
		 * The game is about to become a tie.
		 * Board looks like this initially:
		 * oxo
		 * xox
		 * xo.
		 * 
		 * Send move at position 9.
		 * Verify after Game.start() returns that:
		 * - position 9 contains 'x'
		 * - GameOverMsg == "Tie game."
		 * - Game.NotInGame.get() == True
		 * - "t9" is sent
		 */
		
		Board b = new Board();
		b.insert('o', 1);
		b.insert('x', 2);
		b.insert('o', 3);
		b.insert('x', 4);
		b.insert('o', 5);
		b.insert('x', 6);
		b.insert('x', 7);
		b.insert('o', 8);
		
		MockTicTacToe ttt = new MockTicTacToe(true, 9, "8", b);
		ttt.getGame().start("",  "");
		
		Assert.assertEquals('x', b.getSymbolAtCoord(2, 2));
		Assert.assertEquals(ITicTacToe.TIE_GAME, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("t9", ttt.getFinalMessage());
	}

	@Test
	public void testSendGiveup() {
		/**
		 * Scenario where current player is P1, sending a move.
		 * P1 is about to giveup.
		 * Board is empty.
		 * 
		 * Send "giveup".
		 * Verify after Game.start() returns that:
		 * - GameOverMsg = "You have given up. You lose."
		 * - Game.NotInGame.get() == True
		 * - "giveup" is sent
		 */
		
		Board b = new Board();
		
		MockTicTacToe ttt = new MockTicTacToe(true, -1, "8", b);
		ttt.getGame().start("", "");
		
		Assert.assertEquals(ITicTacToe.GIVEN_UP, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("giveup", ttt.getFinalMessage());
	}
	
	@Test
	public void testSendNormalMove() {
		/**
		 * Scenario where current player is P1, sending a move.
		 * P1 is about to send a move.  After move gets received, P2 will
		 * giveup.
		 * Board looks like this initially:
		 * xo.
		 * ...
		 * ...
		 * 
		 * Send move at position 3.
		 * Verify after Game.start() returns that:
		 * - position 3 contains 'x'
		 * - GameOverMsg = "Player 2 has given up. You win."
		 * - Game.NotInGame.get() == True
		 * - "3" is sent
		 */
		
		Board b = new Board();
		b.insert('x', 1);
		b.insert('o', 2);
		
		MockTicTacToe ttt = new MockTicTacToe(true, 3, "giveup", b);
		ttt.getGame().start("", "");
		
		Assert.assertEquals('x', b.getSymbolAtCoord(0, 2));
		Assert.assertEquals(ITicTacToe.P2_GIVEUP_WIN, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("3", ttt.getFinalMessage());
	}
	
	@Test
	public void testReceiveCnxnLossSecondExpiry() {
		/**
		 * Scenario where current player is P2, waiting to receive a move.
		 * P1 does not send a move within Game.CNXN_LOSS_TIME seconds, and
		 * this results in a connection loss.
		 * Board is empty.
		 * 
		 * Receive "giveup".
		 * Verify after Game.start() returns that:
		 * - GameOverMsg = "Connection loss."
		 * - Game.NotInGame.get() == True
		 * - "bye" is sent
		 */
		
		Board b = new Board();
		
		MockTicTacToe ttt = new MockTicTacToe(false, 8, "", b);
		ttt.getGame().start("", "");
		
		Assert.assertEquals(ITicTacToe.CONNECTION_LOSS, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("bye", ttt.getFinalMessage());
	}
	
	@Test
	public void testSendGiveupTimeSecondExpiry() {
		/**
		 * Scenario where current player is P1, sending a move.
		 * P1 does not send a move within Game.GIVEUP_TIME seconds, and thus
		 * gives up.
		 * Board is empty.
		 * 
		 * Receive "giveup".
		 * Verify after Game.start() returns that:
		 * - GameOverMsg = "You have not played a move. You lose."
		 * - Game.NotInGame.get() == True
		 * - "giveup" is sent
		 */
		
		Board b = new Board();
		
		MockTicTacToe ttt = new MockTicTacToe(true, 0, "8", b);
		ttt.getGame().start("", "");
		
		Assert.assertEquals(ITicTacToe.NO_PLAY_MOVE, ttt.getGameOverMsg());
		Assert.assertTrue(Game.NotInGame.get());
		Assert.assertEquals("giveup", ttt.getFinalMessage());
	}
}
