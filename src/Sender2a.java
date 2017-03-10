import java.io.IOException;

public class Sender2a {
	public static void main(String[] args) throws IOException {
		
		// ================ Read arguments ===============
		if (args.length != 5) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: java Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
			System.exit(1);
		}
		String localhost = args[0];
		int portNo = Integer.parseInt(args[1]);
		String filename = args[2];
		int retryTimeout = Integer.parseInt(args[3]);
		int windowSize = Integer.parseInt(args[4]);
		
		Client client = new Client(localhost, portNo, filename, retryTimeout, windowSize);
		client.openFile(); // opens image file
		
		Thread rcvtt = new Thread(new RcvThread(client));
		Thread sendtt = new Thread(new SendThread(client));
		rcvtt.start();
		sendtt.start();
	}
}

class RcvThread implements Runnable {
	private Client client;
	public RcvThread(Client client) {
		this.client = client;
	}
	@Override
	public void run() {
		try {
			while (!client.doneACK) {
				client.ackPacket();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class SendThread implements Runnable {
	private Client client;
	public SendThread(Client client) {
		this.client = client;
	}
	@Override
	public void run() {
		while (!client.doneACK) {
			if (client.canSendMore()) {
				try {
					client.sendPacket();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 
		}
	}
}