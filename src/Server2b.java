/* Isabella Chan s1330027 */ 

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;


public class Server2b extends AbstractServer {
	int windowSize;
	ArrayList<DatagramPacket> window = new ArrayList<DatagramPacket>();
	
	public Server2b(int portNo, String filename, int windowSize) throws IOException {
		super(portNo, filename);
		this.windowSize = windowSize;
		for (int i = 0; i < windowSize; i++) {
			window.add(null);
		}
	}
	
	boolean isDone = false;
	@Override
	public void ackPacket() throws IOException {
		
		System.out.println("Trying to ack packet!");
		
		receivePacket();
		
		// update variables based on received packet
		System.out.println("expected: "+expectedSeqNo+"   |   received: "+rcvSeqNo);
		if (rcvSeqNo == expectedSeqNo) {
			System.out.println("rcvSeqNo == expectedSeqNo");
			window.set(0, receivePacket);
			for (int i = 0; i < windowSize; i++) {
				if (window.get(i) == null) 
					break;
				packetSize = window.get(i).getLength();
				byte[] currBuff = new byte[packetSize-3];
				int currIdx = 0;
				for (int j = 3; j < packetSize; j++) {
					currBuff[currIdx] = buffer[i];
					currIdx++;
				}
				out.write(currBuff);
				expectedSeqNo = (expectedSeqNo+1)%65535;
				window.set(i, null);
			}
			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
			System.out.print("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
			serverSocket.send(ackPacket);
			
			if (endFlag == (byte)1) {  // TODO: check this! does expected has to be the last packet?? 
				for (int i = 0; i < windowSize; i++) {
					if (window.get(i) != null) {
						isDone = false;
					}
				}
				if (isDone) {
					closeAll();
					System.out.println("done receiving packet! endFlag == 1");
					return;
				}
			}
		} 
		else if (rcvSeqNo < expectedSeqNo) { // already ack'd packet, resend ack!
			System.out.println("rcvSeqNo < expectedSeqNo");
			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
			serverSocket.send(ackPacket);
			bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
			return;		
		} 
		else if (rcvSeqNo >= ((expectedSeqNo+windowSize) % 65535)) {
			System.out.println("rcvSeqNo >= ((expectedSeqNo+windowSize) % 65535)");
			System.out.println("Severe: Should not reach here!!!");
		} 
		else { // packet received is within window
			System.out.println("Packet received is within window.");
			bw.write("packet received is within window\n");
			DatagramPacket currPacket = receivePacket;
			int idx = rcvSeqNo - expectedSeqNo;
			window.set(idx, currPacket);
		}
	}
	
//	public void receivePacket() throws IOException {
//		receivePacket.setLength(1027);
//		serverSocket.setSoTimeout(0);
//		// -------------------- receiving a packet! ----------------------
//		serverSocket.receive(receivePacket);
//		packetSize = receivePacket.getLength();
//		clientPortNo = receivePacket.getPort();
//		clientIPAddress = receivePacket.getAddress();
//		
//		bw.write("serverSocket : portNo : "+serverSocket.getPort()+"   |   IPAddress : "+serverSocket.getInetAddress()+"\n");
//		bw.write("packet received: packetSize : "+packetSize+"   |   clientPortNo : "+clientPortNo+"   |   clientIPAddress : "+clientIPAddress+"\n");
//
//		rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
//		ackBuffer[0] = buffer[0]; // ackBuffer contains the value of the received sequence no.
//		ackBuffer[1] = buffer[1];
//		endFlag = buffer[2];
//		
//		if (rcvSeqNo>=rcvBase && rcvSeqNo<((rcvBase+windowSize)%65535)) {
//			if (rcvSeqNo == rcvBase) {
//				// send all the ack'd packets up
//				window.set(0, receivePacket);
//				for (DatagramPacket pkt : window) { // for each packet, (consequentially) write image
//					if (pkt == null) 
//						break;
//					packetSize = pkt.getLength();
//					byte[] currBuff = new byte[packetSize-3];
//					int currIdx = 0;
//					for (int i = 3; i < packetSize; i++) {
//						currBuff[currIdx] = buffer[i];
//						currIdx++;
//					}
//					out.write(currBuff);
//					rcvBase = (rcvBase+1) % 65535;
//					window.set(window.indexOf(pkt), null);
//				}
//				ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
//				bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
//				serverSocket.send(ackPacket); // send ACK to client
//				bw.write("updated expectedSeqNo : "+rcvBase+"\n");
//				
//				if (endFlag == (byte) 1) { // is last packet
//					out.close();
//					serverSocket.close();
//					bw.close();
//					fw.close();
//					return;
//				}
//			} else {
//				DatagramPacket currPacket = receivePacket;
//				int idx = rcvSeqNo - rcvBase;
//				window.set(idx, currPacket);
//			}
//		} else if (rcvSeqNo < rcvBase){ // already ack'd packet, resend ack!
//			bw.write("rcvSeqNo is not >= expectedSeqNo!\n");
//			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
//			serverSocket.send(ackPacket);
//			bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
//			return;
//		} else { // disregard
//			System.out.println("Severe: Should not reach here!!!");
//			bw.write("received a packet that is bigger than window size??\n");
//		}
//	}
}
