package tictactoe;

import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Game {
	private Board _board;
	private Player _p1;
	private Player _p2;
	private Recv _recv;
	private IClient _c;
	private ITicTacToe _ttt;
	
	public static int GIVEUP_TIME = 15;
	public static int CNXN_LOSS_TIME = 30;
	
	public static AtomicBoolean NotInGame = new AtomicBoolean(true);
	
	private class Recv {
		private String _recvBuf;
		public synchronized void setRecvBuf(String newBuf) {
			_recvBuf = newBuf;
		}
		
		public synchronized String getRecvBuf() {
			return _recvBuf;
		}
	}
	
	public Game(ITicTacToe ttt, IClient c, Board b) {
		_ttt = ttt;
		_board = b;
		_c = c;
		_p1 = new Player(Player.P1_SYMBOL);
		_p2 = new Player(Player.P2_SYMBOL);
		_recv = new Recv();
		_recv.setRecvBuf("");
	}
	
	public Board getBoard() {
		return _board;
	}
	
	private class CheckGiveupThread implements Runnable {
		private void handleRecvWinTie(char symbol, int pos, boolean isP1) {
			if (isP1)
				_board.insert(Player.P2_SYMBOL, pos);
			else
				_board.insert(Player.P1_SYMBOL, pos);
			_ttt.repaintDisplay();
			if (symbol == 'w') {
				_ttt.showGameOverDialog("Game over. You lose." +
						ITicTacToe.CLICK_TO_RESTART);
				if (isP1)
					_ttt.setGameOverMsg(ITicTacToe.P2_WIN_LOSE);
				else
					_ttt.setGameOverMsg(ITicTacToe.P1_WIN_LOSE);
			}
			else {
				_ttt.showGameOverDialog("Game over. Tie game." +
						ITicTacToe.CLICK_TO_RESTART);
				_ttt.setGameOverMsg(ITicTacToe.TIE_GAME);
			}
		}
		
		@Override
		public void run() {
			/**
			 * Reads from the client socket and saves it into _recv.recvBuf.
			 * The data is a string sent from the other player.
			 * The game is over when:
			 * - the server is disconnected
			 * - a "giveup" is received from the other player, resulting in the
			 *   current player winning the game
			 * - a string beginning with 'w' or 't' is received from the other
			 *   player, signaling a loss for the current player or a tie game
			 * If the game is not over, reads from the client socket continue
			 * to get saved into _recv.recvBuf.
			 */
			while (!Game.NotInGame.get()) {
				String test = "";
				try {
					test = _c.receiveFrom(1);
				} catch (SocketTimeoutException e) {
					continue;
				} catch (Exception e) {
					// server disconnect
					_ttt.setGameOverMsg(ITicTacToe.DISCONNECT);
					Game.NotInGame.set(true);
					return;
				}
				synchronized (_recv) {
					_recv.setRecvBuf(Client.stringToLength(test, "giveup".length()));
					if (_recv.getRecvBuf().equals("giveup")) {
						Game.NotInGame.set(true);
						if (_c.isP1()) {
							_ttt.setGameOverMsg(ITicTacToe.P2_GIVEUP_WIN);
							_ttt.showGameOverDialog(ITicTacToe.P2_GIVEUP_WIN +
									ITicTacToe.CLICK_TO_RESTART);
						}
						else {
							_ttt.setGameOverMsg(ITicTacToe.P1_GIVEUP_WIN);
							_ttt.showGameOverDialog(ITicTacToe.P1_GIVEUP_WIN +
									ITicTacToe.CLICK_TO_RESTART);
						}
						return;
					}
					if (_recv.getRecvBuf() == "")
						continue;
					if (_recv.getRecvBuf().charAt(0) == 'w' ||
							_recv.getRecvBuf().charAt(0) == 't') {
						handleRecvWinTie(_recv.getRecvBuf().charAt(0),
								_recv.getRecvBuf().charAt(1) - '0', _c.isP1());
						Game.NotInGame.set(true);
						return;
					}
				}
			}
		}
	}
	
	private void handleGameOver(boolean currPlayerTurn,
			int currPlayerLastMove) {
		synchronized (_ttt) {			
			if (_ttt.getGameOverMsg() != null &&
					_ttt.getGameOverMsg().equals(ITicTacToe.CLICK_TO_START)) {
				// quitbutton was triggered.
				_ttt.setGameOverMsg(ITicTacToe.GIVEN_UP);
				_c.sendGiveup();
				_ttt.showGameOverDialog(ITicTacToe.GIVEN_UP +
						ITicTacToe.CLICK_TO_RESTART);
			}
			else if (_ttt.getGameOverMsg() != null &&
					(_ttt.getGameOverMsg().equals(ITicTacToe.P1_GIVEUP_WIN) ||
					_ttt.getGameOverMsg().equals(ITicTacToe.P2_GIVEUP_WIN)))
				// other client triggered quitbutton
				;
			else if (_ttt.getGameOverMsg() != null &&
					_ttt.getGameOverMsg().equals(ITicTacToe.DISCONNECT)) {
				// server disconnect
				_ttt.setGameOverMsg(ITicTacToe.CONNECTION_LOSS);
				_c.sendBye();
				_ttt.showGameOverDialog(ITicTacToe.CONNECTION_LOSS +
						ITicTacToe.CLICK_TO_RESTART);
			}
			else if (_ttt.getGameOverMsg() != null && currPlayerTurn &&
					_ttt.getGameOverMsg().equals(ITicTacToe.YOU_WIN)) {
				// current player played a win move
				_c.sendWin(currPlayerLastMove);
				_ttt.showGameOverDialog("Game over. You win." +
						ITicTacToe.CLICK_TO_RESTART);
			}
			else if (_ttt.getGameOverMsg() != null && currPlayerTurn &&
					_ttt.getGameOverMsg().equals(ITicTacToe.TIE_GAME)) {
				// current player played a tie move
				_c.sendTie(currPlayerLastMove);
				_ttt.showGameOverDialog("Game over. Tie game." +
						ITicTacToe.CLICK_TO_RESTART);
			}
			else if (_ttt.getGameOverMsg() != null &&
					(_ttt.getGameOverMsg().contains(ITicTacToe.P1_WIN_LOSE) ||
					_ttt.getGameOverMsg().contains(ITicTacToe.P2_WIN_LOSE) ||
					_ttt.getGameOverMsg().contains(ITicTacToe.TIE_GAME))) {
				// other player received win/tie from CheckGiveupThread
				;
			}
			else {
				// user didn't play move within 30 seconds or
				// not receive move within 45 seconds
				if (currPlayerTurn) {
					_ttt.setGameOverMsg(ITicTacToe.NO_PLAY_MOVE);
					_c.sendGiveup();
					_ttt.showGameOverDialog(ITicTacToe.NO_PLAY_MOVE +
							ITicTacToe.CLICK_TO_RESTART);
				}
				else {
					_ttt.setGameOverMsg(ITicTacToe.CONNECTION_LOSS);
					_c.sendBye();
					_ttt.showGameOverDialog(ITicTacToe.CONNECTION_LOSS +
							ITicTacToe.CLICK_TO_RESTART);
				}
			}
		}
	}
	
	private int currPlayerMove(boolean p1turn) {
		/**
		 * Handle the event when it is the current player's move.  The current
		 * player has Game.GIVEUP_TIME seconds to play a move.  A current
		 * player's move is read from mouse input on a JPanel.
		 * The game is over when:
		 * - Game.GIVEUP_TIME seconds have expired, a "giveup" message will be
		 *   sent to the server, signaling the current player has given up
		 * - the current player pressed the "Quit" button, resulting in the
		 *   current player giving up and a "giveup" message sent to the server
		 * - the current player exited the program, resulting in the current
		 *   player giving up and a "giveup" message sent to the server
		 * - the other player gave up (determined by CheckGiveupThread)
		 * - the server is disconnected
		 * - the current player's move resulted in a win or tie game
		 * If the game is not over, the current player's move gets sent to the
		 * server and then it becomes the other player's move.
		 * If the current player won or tied the game, this method will return
		 * the current player's last inputted move.
		 */
		final TimerThread.Msg msg = new TimerThread.Msg();
		msg.gotMsg = false;
		Runnable timer = new TimerThread(msg, Game.GIVEUP_TIME, _ttt);
		Thread t = new Thread(timer);
		t.start();

		int input = -1;
		if (p1turn) {
			input = _ttt.getInput(_p1.getSymbol());
		}
		if (!p1turn) {
			input = _ttt.getInput(_p2.getSymbol());
		}

		msg.gotMsg = true;
		try {
			t.join();
		} catch (InterruptedException e) {
			System.err.println("Could not join timer thread.");
			System.exit(1);
		}

		_ttt.setTimerfieldText("");

		if (_board.isWin(input) && !Game.NotInGame.get()) {
			_ttt.repaintDisplay();
			Game.NotInGame.set(true);
			_ttt.setGameOverMsg(ITicTacToe.YOU_WIN);
		}
		else if (_board.isTie() && !Game.NotInGame.get()) {
			_ttt.repaintDisplay();
			Game.NotInGame.set(true);
			_ttt.setGameOverMsg(ITicTacToe.TIE_GAME);
		}
		else if (input == -1) {
			// action happened before user could provide input or
			// user not input move within 30 seconds
			Game.NotInGame.set(true);
		}
		else
			_c.sendPosition(input);
		
		return input;
	}
	
	private void otherPlayerMove(boolean p1turn) {
		/**
		 * Handles the event when it is the other player's turn.  Up to
		 * Game.CNXN_LOSS_TIME seconds are spent waiting for a move from the
		 * other player.
		 * The game is over when:
		 * - the current player pressed the "Quit" button, resulting in the
		 *   current player giving up and a "giveup" message sent to the server
		 * - the current player exited the program, resulting in the current
		 *   player giving up and a "giveup" message sent to the server
		 * - the other player gave up (determined by CheckGiveupThread)
		 * - the server is disconnected
		 * - a move had not been received within Game.CNXN_LOSS_TIME seconds,
		 *   resulting in a possible server disconnect from the other player,
		 *   no win/loss sent to server
		 * If the game is not over, the other player's move gets received from
		 * the server and then it becomes the current player's move. 
		 */
		final TimerThread.Msg msg = new TimerThread.Msg();
		msg.gotMsg = false;
		Runnable timer = new TimerThread(msg, Game.CNXN_LOSS_TIME, null);
		Thread t = new Thread(timer);
		t.start();

		int input = -1;
		_recv.setRecvBuf("");
		
		while (!Game.NotInGame.get()) {
			if (_recv.getRecvBuf() == "")
				continue;
			if (Character.isDigit(_recv.getRecvBuf().charAt(0)) &&
					_recv.getRecvBuf().charAt(0) != '0')
				break;
		}
		
		msg.gotMsg = true;
		try {
			t.join();
		} catch (InterruptedException e) {
			System.err.println("Could not join timer thread.");
			System.exit(1);
		}
		
		if (Game.NotInGame.get())
			return;

		input = _recv.getRecvBuf().charAt(0) - '0';

		if (p1turn && !_board.insert(_p1.getSymbol(), input)) {
			System.err.println("Error with receivePosition with input: " +
					input);
			return;
		}
		if (!p1turn && !_board.insert(_p2.getSymbol(), input)) {
			System.err.println("Error with receivePosition with input: " +
					input);
			return;
		}
	}
	
	public void start(String username, String password) {
		if (Game.NotInGame.get()) {
			_ttt.setPlayerfieldText("MatchMaking TicTacToe");
			while (Game.NotInGame.get())
				;
			_ttt.repaintDisplay();
		}

		_ttt.setPlayerfieldText("Searching for opponent...");
		_ttt.setQuitbuttonVisible(true);
		_c.init(username, password, _ttt);
		if (!_c.getDoneInit()) {
			_ttt.setPlayerfieldText("");
			return;
		}

		/**
		 * record string is formatted like this:
		 * r[wins],[losses],[opponent name]
		*/
		String[] initialSplit = _c.getRecord().split(",");
		String winRecord = "";
		String lossRecord = "";
		String opponentUsername = "";
		try {
			winRecord = initialSplit[0].split("r")[1];
			lossRecord = initialSplit[1];
		} catch (Exception e) {
		}
		try {
			opponentUsername = initialSplit[2];
		} catch (Exception e) {
		}
		if (!username.isEmpty() && !password.isEmpty()) {
			_ttt.setWinfieldText("W: " + winRecord);
			_ttt.setLossfieldText("L: " + lossRecord);
		}
		if (PasswordWindow.isValidCredential(opponentUsername))
			_ttt.setOpponentText("Opponent: " + opponentUsername);
		else
			_ttt.setOpponentText("Guest Opponent");
		_ttt.setNumPplText("Players in-game: " + _c.getNumPpl());

		if (_c.isP1())
			_ttt.setPlayerfieldText("You are player 1 (" +
					Player.P1_SYMBOL + ").");
		else
			_ttt.setPlayerfieldText("You are player 2 (" +
					Player.P2_SYMBOL + ").");

		Runnable giveupThread = new CheckGiveupThread();
		Thread gt = new Thread(giveupThread);
		gt.start();
				
		boolean p1turn = false;
		boolean currPlayerTurn = false;
		int currPlayerLastMove = -1;
		while (!Game.NotInGame.get()) {
			p1turn = !p1turn;
			// draw board
			if (p1turn && _c.isP1())
				_ttt.setTurnfieldText("Your turn.");
			else if (p1turn && !_c.isP1())
				_ttt.setTurnfieldText("Player 1 turn.");
			else if (!p1turn && _c.isP1())
				_ttt.setTurnfieldText("Player 2 turn.");
			else
				_ttt.setTurnfieldText("Your turn.");

			_ttt.repaintDisplay();
			
			currPlayerTurn = (p1turn && _c.isP1()) || (!p1turn && !_c.isP1());
			if (currPlayerTurn)
				currPlayerLastMove = currPlayerMove(p1turn);
			else
				otherPlayerMove(p1turn);
		}
		
		try {
			gt.join();
		} catch (InterruptedException e) {
			System.err.println("Could not join giveup thread.");
			System.exit(1);
		}
		
		handleGameOver(currPlayerTurn, currPlayerLastMove);
		
		_ttt.setQuitbuttonVisible(false);
	}
}
