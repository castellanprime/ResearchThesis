/**
 *  Copyright 2017 Okusanya Oluwadamilola
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.*;
import java.util.Random;
import java.util.ArrayList;
import org.zeromq.ZMQ;

/**
 *	As a very general advice: use bind on the most stable points in your architecture and connect
 *	from the more volatile endpoints. For request/reply the service provider might be point where 
 *	you bind and the client uses connect. Like plain old TCP.
 * 
 */

// To use the dealer socket, you have to use an id

class Ping {
	private int count = 0;
	public void setCount(int count) { this.count = count; }
	public int getCount() { return this.count; }
}

class PingWorker implements Runnable{
	private volatile boolean stopped;
	private final Ping ping =  new Ping();
	public ZMQ.Socket socket;

	PingWorker(ZMQ.Context context, String address){
		this.socket = context.socket(ZMQ.PAIR);
		socket.connect(address);
	} 

	@Override
	public void run(){
		while(!stopped){
			try{
				synchronized(this.ping){
					int step = this.ping.getCount();
					step++;
					this.ping.setCount(step);
					socket.send((Integer.toString(step)).getBytes(), 0);
				}
				Thread.sleep(300);
			} catch (InterruptedException ie){
				ie.printStackTrace();
			}
		}

		if (stopped == true){
			socket.close();
		}
	}

	public void stopWorker(){
		stopped = true;
	}
}


public class ClusterNode{
	public static void main(String [] args){

		ZMQ.Context context = ZMQ.context(1);

		Random rand = new Random();
		int clientId = rand.nextInt(10) + 1;

		BlockingQueue<String> storedCounts =  new LinkedBlockingQueue<>();

		// Setup pair socket 
		ZMQ.Socket pingWorkerSocket = context.socket(ZMQ.PAIR);
		pingWorkerSocket.bind("inproc://pingWorker");

		// Setup dealer socket
		ZMQ.Socket dealerSocket = context.socket(ZMQ.DEALER);
		String identity = "Client-" + clientId;
		dealerSocket.setIdentity(identity.getBytes());
		dealerSocket.connect("tcp://127.0.0.1:5050");	


		// Setup subscriber socker
		ZMQ.Socket subscriberSocket = context.socket(ZMQ.SUB);
		subscriberSocket.connect("tcp://127.0.0.1:5300");
		subscriberSocket.subscribe("".getBytes());

		// Setup heartbeatreplier socket
		ZMQ.Socket stopNodeSocket = context.socket(ZMQ.REQ);
		stopNodeSocket.connect("tcp://127.0.0.1:5900");

		// Initialise the pollin set
		ZMQ.Poller dealerTestPoller = new ZMQ.Poller(3);
		dealerTestPoller.register(pingWorkerSocket, ZMQ.Poller.POLLIN);		// POLLIN/POLLOUT only listen for incoming/outgoing messages 
		dealerTestPoller.register(subscriberSocket, ZMQ.Poller.POLLIN);
		dealerTestPoller.register(dealerSocket, ZMQ.Poller.POLLIN);	
		
		// Setup pair with pingWorkerSocket
		PingWorker worker = new PingWorker(context, "inproc://pingWorker");
		Thread pings = new Thread(worker);
		
		int numberToSend = 0;
		String id = "";
		boolean isNodeUp = false;
		boolean stopPingThread = false;

		// Main loop
		try{
			while (!Thread.currentThread().isInterrupted()){
				String message;
				dealerTestPoller.poll();

				// Messages from counter
				if (dealerTestPoller.pollin(0)){
					message = new String(pingWorkerSocket.recv(0));
					System.out.println("Received message = " + message);
					storedCounts.offer(message);
				}
			
				// Responding to heartbeats
				if (dealerTestPoller.pollin(1)){
					message = new String(subscriberSocket.recv(0));
					System.out.println("[Heartbeats[: Received (" + message + ")");
					stopNodeSocket.send("I am here".getBytes(), 0);
					System.out.println("[Heartbeats[: Just sent( I am here)");
					message = new String(stopNodeSocket.recv(0));
					System.out.println("[Heartbeats[: Received (" + message + ")");
					if ("I have seen you".equalsIgnoreCase(message) && isNodeUp == false){
						dealerSocket.send("I am ready".getBytes(), 0);
						System.out.println("[Heartbeats]: Just sent(I am ready)");
						isNodeUp = true;
					}
				}

				// Responding to commands from server
				if (dealerTestPoller.pollin(2)){
					//id = new String(dealerSocket.recv(0));
					message = new String(dealerSocket.recv(0));
					String [] messageParts = message.split("\\s");
					if ("START".equalsIgnoreCase(messageParts[0])){
						if (isNodeUp== true){
							dealerSocket.send("Starting counter".getBytes(), 0);
							System.out.println("[Setup]: Starting counter");
							pings.start();
						} else {
							dealerSocket.send("[Error]: Cluster has already started".getBytes(), 0);
						}
					}

					if ("QUIT".equalsIgnoreCase(messageParts[0])){
						if (numberToSend == 0){
							stopPingThread = true;
						} else {
							System.out.println("[Error]: System is still sending " + numberToSend + " messages");
						}
					}


					if ("QUERY".equalsIgnoreCase(messageParts[0])){
						numberToSend = Integer.parseInt(messageParts[1]);
						if (numberToSend > storedCounts.size() - 2 || numberToSend <= 0){
							String error = "Illegal number of messages to retrieve";
							if (numberToSend <= 0){
								error = error + "Try a larger number than" + numberToSend; 
							} else {
								error = error + "Try a smaller number than" + numberToSend;
							}
							dealerSocket.send(error.getBytes(), 0);
						} else {
							ArrayList<String> listtoSend = new ArrayList<>();
							storedCounts.drainTo(listtoSend, numberToSend);	
							for (String item: listtoSend){
								dealerSocket.send(item.getBytes(), 0);
								System.out.println("Sending " + item + " to dealer");
								numberToSend--;
							}
						}
					}
				}

				if (stopPingThread == true){
					stopNodeSocket.send("Shutting down!!!".getBytes(), 0);
					message = new String(stopNodeSocket.recv(0));
					System.out.println("[Heartbeats]: Received (" + message + ")");
					if ("GOODBYE".equalsIgnoreCase(message)){
						worker.stopWorker();
						Thread.currentThread().interrupt();
					}
				}

			}
		} catch(Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
        }

        stopNodeSocket.close();
        pingWorkerSocket.close();
        dealerSocket.close();
        context.term();
	}
}