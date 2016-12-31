import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.JOptionPane;

public final class Client {
	public final static int MAXBUFLEN = 1000;
	private final static String SERVERIP = "54.183.217.40";
//	private final static String SERVERIP = "192.168.218.140";
	private final static String SERVERPORT = "4950";

	private Socket sock;
	private boolean isP1;
	private String username;
	private String password;

	public boolean DoneInit;
	public String Record;

	public Client() {
		DoneInit = false;
	}

	public void init(String username, String password) {
		this.username = username;
		this.password = password;
		String buf = "";

		int retries;
		for (retries = 0; retries < 10; retries++) {
			if (TicTacToe.NotInGame)
				break;

			// connect to parent server
			try {
				createSocketServer(SERVERPORT);
			} catch (Exception e) {
				System.err.println("Could not create socket to parent server. Exiting.");
				e.printStackTrace();
				continue;
			}
			try {
				getNumPpl();
				buf = handleSynAck();
			} catch (Exception e) {
				continue;
			}

			// close connection to parent server
			try {
				this.sock.close();
			} catch (IOException e) {
				System.err.println("Could not close connection to parent server. Exiting.");
				e.printStackTrace();
				continue;
			}

			// connect to child server
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("Could not sleep. Exiting.");
				e.printStackTrace();
				break;
			}
			try {
				createSocketServer(buf);
			} catch (Exception e) {
				System.err.println("Could not create socket to child server. Exiting.");
				e.printStackTrace();
				continue;
			}
			System.out.println("Connected to child server.");
			buf = "";
			try {
				buf = handleChildSynAck();
			} catch (Exception e) {
				continue;
			}
			buf = TicTacToe.stringToLength(buf, "player-1".length());

			try {
				this.sock.setSoTimeout(0);
			} catch (Exception e) {
				continue;
			}

			// get assigned player-1 or player-2
			if (buf.equals("player-1")) {
				this.isP1 = true;
				do {
					buf = "";
					try {
						buf = receiveFromServer();
					} catch (DisconnectException e) {
						System.err.println("Child server exited.");
						e.printStackTrace();
						break;
					}
				} while (buf.charAt(0) != 'r');
				System.out.println("Current record: " + buf);
				this.Record = new String(buf);
				break;
			}
			else if (buf.charAt(0) == 'r') {
				System.out.println("Current record: " + buf);
				this.isP1 = false;
				this.Record = new String(buf);
				break;
			}
			else if (buf.equals("invalidl")) {
				JOptionPane.showMessageDialog(null, "Invalid login credentials. Exiting.");
				System.exit(0);
			}
			else {
				JOptionPane.showMessageDialog(null, "User is currently in game. Exiting.");
				System.exit(0);
			}
		}
		if (retries == 10) {
			JOptionPane.showMessageDialog(null, "Connection failed. Retries limit reached.");
			System.exit(1);
		}
		DoneInit = true;
	}

	public void close() {
		if (this.sock != null) {
			try {
				this.sock.close();
			} catch (IOException e) {
			}
		}
	}

	public void sendPosition(int pos) {
		sendToServer(Integer.toString(pos));
	}

	public boolean isP1() {
		return this.isP1;
	}

	public void sendGiveup() {
		sendToServer("giveup");
	}

	public void sendBye() {
		sendToServer("bye");
	}

	public void sendWin(int pos) {
		String a = "w" + Integer.toString(pos);
		sendToServer(a);
	}

	private String handleSynAck() throws Exception {
		String ack = "";
		try {
			ack = receiveFrom(15);
			System.out.println("Receieved ACK from server.");
		} catch (DisconnectException e) {
			System.out.println("Server disconnected. Exiting.");
			e.printStackTrace();
			throw new Exception();
		} catch (Exception e) {
			throw new Exception();
		}
		return ack;
	}

	private String handleChildSynAck() throws Exception {
		String login = this.username + "," + this.password;
		sendToServer(login);
		String ack = "";
		try {
			ack = receiveFrom(15);
			System.out.println("Receieved ACK from server.");
		} catch (DisconnectException e) {
			System.out.println("Server disconnected. Exiting.");
			e.printStackTrace();
			throw new Exception();
		} catch (Exception e) {
			throw new Exception();
		}
		return ack;
	}

	private void createSocketServer(String port) throws Exception {
		port = TicTacToe.stringToLength(port, SERVERPORT.length());
		try {
			this.sock = new Socket(SERVERIP, Integer.parseInt(port));
		} catch (UnknownHostException e) {
			System.err.println("Can't create socket. Unknown host.");
			throw new Exception();
		} catch (IOException e) {
			System.err.println("Can't create socket. I/O error.");
			throw new Exception();
		} catch (NumberFormatException e) {
			System.err.println("Could not parse port string to int.");
			throw new Exception();
		}
	}

	private void getNumPpl() throws Exception {
		try {
			String numPpl = receiveFrom(15);
			System.out.println("Number of people online: " + numPpl);
		} catch (DisconnectException e) {
			System.out.println("Server disconnected. Exiting.");
			e.printStackTrace();
			throw new Exception();
		} catch (Exception e) {
			throw new Exception();
		}
	}

	private void sendToServer(String msg) {
		try {
			OutputStream os = this.sock.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(msg);
			bw.flush();
		} catch (IOException e) {
			System.err.println("Error send message.");
			e.printStackTrace();
			return;
		}
	}

	private String receiveFrom(int sec) throws DisconnectException, Exception {
		char message[] = new char[Client.MAXBUFLEN];
		try {
			InputStream is = this.sock.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			this.sock.setSoTimeout(sec * 1000);
			if (br.read(message, 0, Client.MAXBUFLEN) == -1)
				throw new DisconnectException();
		} catch (Exception e) {
			throw new Exception();
		}
		return new String(message);
	}

	public String receiveFromServer() throws DisconnectException {
		char message[] = new char[Client.MAXBUFLEN];
		try {
			InputStream is = this.sock.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			if (br.read(message, 0, Client.MAXBUFLEN) == -1)
				throw new DisconnectException();
		} catch (IOException e) {
			System.err.println("Error receive message.");
			e.printStackTrace();
			throw new DisconnectException();
		}
		return new String(message);
	}
}
