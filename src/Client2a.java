/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Timer;
import java.util.TimerTask;

/** Dummynet configuration
mount -t vboxsf dummynetshared /mnt/shared
ipfw add pipe 100 in
ipfw add pipe 200 out
ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
*/

class ResendTask extends TimerTask {
	private Client2a client;
	public ResendTask(Client2a client) {
		this.client = client;
	}
	@Override
	public void run() {
		try {
			client.resendPackets();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		return;
	}
}

public class Client2a extends AbstractClient {

	private Timer timer = new Timer();

	public Client2a(String localhost, int portNo, String filename,
			int retryTimeout, int windowSize) throws IOException {
		super(localhost, portNo, filename, retryTimeout, windowSize);
	}

	/** reschedule client's timer */
	public void rescheduleTimer() {
		timer.cancel();
		timer = new Timer();
		timer.schedule(new ResendTask(this), retryTimeout);
	}
	
	@Override
	public void sendPacket() throws IOException {
		// Client only sends if there are bytes left in file and there is space in the window
		if (imgBytesArrIdx >= imgBytesArrLen) {
			doneSEND = true;
			return;
		}
		if (pktsBuffer.size() >= windowSize) {
			return;
		}
		synchronized (lock) {
			seqNoInt = incre % 65535;
			incre++;

			byte[] buffer = createPacket();

			// ------------------------------ send the packet --------------------------------
			sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
			clientSocket.send(sendPacket);

			// set time for first packet
			if (isFirstPacket) {
				startTime = System.nanoTime();
				isFirstPacket = false;
			}
			// ------------------------------ update values --------------------------------
			if (base == nextseqnum) { // if no unAck'd packets
				rescheduleTimer();
			}
			nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no
			pktsBuffer.add(sendPacket); // add sent packet to packets buffer
		}
	}

	@Override
	public void ackPacket() throws IOException {
		// Tries to receive a packet (with sequence no = base) and ack it.
		
		receivePacket(); // updates rcvSeqNo and ackBuffer
		
		synchronized (lock) {
			if (rcvSeqNoInt < base)
				return;
			
			// -------------- ack packet if received sequence no. is greater than equal to base no.-----------------
			for (int i = 0; i < (rcvSeqNoInt-base+1); i++) {
				pktsBuffer.remove(0); // removes packets from buffer
			}

			base = (rcvSeqNoInt+1) % 65535;

			if (endFlag == (byte)1 && pktsBuffer.size()==0 && rcvSeqNoInt == lastSeqNo) { // ack'ed last packet
				doneACK = true;
				timer.cancel();
				closeAll(); // updates end time
				return;
			}

			if (base == nextseqnum) { // no more unAck'ed packet
				timer.cancel();
			} else {
				rescheduleTimer();
			}
		}
	}
	
	@Override
	public void resendPackets() throws IOException {
		synchronized (lock) {
			for (int i = 0; i < pktsBuffer.size(); i++) {
				clientSocket.send(pktsBuffer.get(i));
			}
			rescheduleTimer();
		}
	}

	@Override
	public void printOutputs() {
		estimatedTimeInNano = endTime - startTime;
		estimatedTimeInSec = ((double)estimatedTimeInNano)/1000000000.0; // convert from nano-sec to sec
		throughput = fileSizeKB/estimatedTimeInSec;
		System.out.println("--------------------- Part2a output --------------------");
		System.out.println("Throughput = "+throughput);
		System.out.println("------------------ Program Terminates ------------------");
	}
}
