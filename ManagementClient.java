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
import java.util.Random;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;
import org.zeromq.ZMQ;

/*
class UserInputReader implements Runnable{
	private volatile boolean stopped;
	private Scanner readIn = new Scanner(System.in);
	public ZMQ.Socket socket;

	public UserInputReader(ZMQ.Context context, String address){
		this.socket = context.socket(ZMQ.PAIR);
		socket.connect(address);
	}

	@Override
	public void run(){
		while(!stopped){
			try{
				System.out.println("Enter your commands(START, QUERY [nodenum] [number of messages], QUIT): ");
				String command = readIn.nextLine();
				if (!command.isEmpty()){
					socket.send(command.getBytes(), 0);
				}
				Thread.sleep(9000);
			} catch (InterruptedException ie){
				ie.printStackTrace();
			}
		}

		if (stopped == true){
			socket.close();
		}
	}

	public void stopReader(){
		stopped = true;
	}
}
*/

class HeartbeatWorker implements Runnable{
	private volatile boolean stopped;
	private final String stringToSend = "Hello: Are you there?";
	public ZMQ.Socket socket;
	private int time;

	HeartbeatWorker(ZMQ.Context context, String address){
		this.socket = context.socket(ZMQ.PUB);
		socket.setLinger(5000);
		socket.setSndHWM(0);
		socket.bind(address);
		this.setSleepTime(300);
	}

	public void setSleepTime(int time){
		this.time = time;
	}

	@Override
	public void run(){
		while(stopped == false){
			// If there is no stop, then there would be a Context terminated exception.
			socket.send(stringToSend.getBytes(), 0);	
			try{
				System.out.println("[Heartbeats]: Just sent (" + stringToSend + ")") ;	
				Thread.sleep(time);
			} catch (InterruptedException ie){
				ie.printStackTrace();
			}
		}
		if (stopped == true){
			socket.close();
		}
	}

	public void stopHeartBeats(){
		stopped = true;
	}
}

public class ManagementClient{
	public static void main(String [] args){

		int SUBSCRIBERS_REQUIRED = 0, subscribers = 0;

		// Check for input
		if (args.length != 1){
			System.out.println("Usage: java ManagementClient <number of servers>");
			System.exit(1);
		} else if (args.length == 1){
			try {
		        SUBSCRIBERS_REQUIRED = Integer.parseInt(args[args.length - 1]);
		    } catch (NumberFormatException e) {
		        System.err.println("Argument" + args[args.length - 1] + " must be an integer.");
		        System.exit(2);
		    }
		}
		
		ZMQ.Context context = ZMQ.context(1);

		// Setup pair socket 
		ZMQ.Socket userInputSocket = context.socket(ZMQ.REP);
		userInputSocket.bind("tcp://127.0.0.1:6000");
	
		// Setup router socket
		ZMQ.Socket routerSocket = context.socket(ZMQ.ROUTER);
		routerSocket.bind("tcp://127.0.0.1:5050");

		// Setup heartbeatReplier socket
		ZMQ.Socket shutDownNodeSocket = context.socket(ZMQ.REP);
		shutDownNodeSocket.bind("tcp://127.0.0.1:5900");


		// Initialise the pollin set
		ZMQ.Poller routerTestPoller = new ZMQ.Poller(3);
		routerTestPoller.register(userInputSocket, ZMQ.Poller.POLLIN);		// POLLIN/POLLOUT only listen for incoming/outgoing messages 
		routerTestPoller.register(shutDownNodeSocket, ZMQ.Poller.POLLIN);
		routerTestPoller.register(routerSocket, ZMQ.Poller.POLLIN);
		
		// Setup pair with UserInputReader Socket
		//UserInputReader reader = new UserInputReader(context, "inproc://userinput");
		//Thread commands = new Thread(reader);
		//commands.start();

		int numberToSend = 0;
		String id = "";

		HashMap<String, String> nodeIdentites = new HashMap<String, String>();

		// Initialize heartbeatworker
		HeartbeatWorker worker = new HeartbeatWorker(context, "tcp://127.0.0.1:5300");
		Thread heartbeats = new Thread(worker);

		boolean isClusterStarted = false, isClusterRunning = false, isContinueSet = false;

		// Main loop
		try{
			while (!Thread.currentThread().isInterrupted()){
				String message;
				routerTestPoller.poll();

				// Sending user commands
				if (routerTestPoller.pollin(0)){
					message = new String(userInputSocket.recv(0));
					String [] messageParts = message.split("\\s");
					StringBuilder st = new StringBuilder();
					if ("START".equalsIgnoreCase(messageParts[0]) && isClusterStarted == false){
						heartbeats.start();
						isClusterStarted = true;
					} else if ("QUIT".equalsIgnoreCase(messageParts[0])){
						for (String nodeId: nodeIdentites.values()){
							routerSocket.send(nodeId, ZMQ.SNDMORE);
							routerSocket.send(message.getBytes(), 0);
						}
					} else if ("QUERY".equalsIgnoreCase(messageParts[0])){
						String nodeId = nodeIdentites.get(messageParts[1]);
						routerSocket.send(nodeId, ZMQ.SNDMORE);
						message = messageParts[0] + " " + messageParts[2];
						routerSocket.send(message.getBytes(), 0);
					} else if ("CONTINUE".equalsIgnoreCase(messageParts[0])){
						isContinueSet = true;
					} else {
						System.out.println("This command is not recognised!!!");
					}

					if ( isContinueSet == true){
						st.append("[Input]: The cluster identities : ( ");
						for (String key : nodeIdentites.keySet()){
							st.append(key);
							st.append(" ");
						}
						st.append(" )");
						isContinueSet = false;
						userInputSocket.send(st.toString().getBytes(), 0);
					}else {
						userInputSocket.send(message.getBytes(), 0);
					}
				}


				// Synchronization sequence/ Shutdown sequence
				if (routerTestPoller.pollin(1)){
					message = new String(shutDownNodeSocket.recv(0));
					System.out.println("[Heartbeats]: Received (" + message + ")");
					if ("I am here".equalsIgnoreCase(message)){
						shutDownNodeSocket.send("I have seen you".getBytes(), 0);
						System.out.println("[Heartbeats]: Just sent(I have seen you)");
					}

					if ("Shutting down!!!".equalsIgnoreCase(message)){ 
						shutDownNodeSocket.send("GOODBYE".getBytes(), 0);
						worker.stopHeartBeats();
						//reader.stopReader();
						Thread.currentThread().interrupt();
					}
				}

				// Receiving and replying to heartbeats
				if (routerTestPoller.pollin(2)){
					id = new String(routerSocket.recv(0));
					message = new String(routerSocket.recv(0));
					System.out.println("Received: (" + message + ") from ClusterNode" + id);
					if ("I am ready".equalsIgnoreCase(message)){
						String key = "ClusterNode" + id;
						if (nodeIdentites.containsKey(key) == false){
							nodeIdentites.put(key, id);
							if (subscribers < SUBSCRIBERS_REQUIRED + 1){
								subscribers++;
							}
							System.out.println("[Setup]: Added new subscriber");
						}
					}
					if (subscribers == SUBSCRIBERS_REQUIRED && isClusterRunning == false){
						for (String nodeId: nodeIdentites.values()){
							routerSocket.send(nodeId, ZMQ.SNDMORE);
							routerSocket.send("START".getBytes(), 0);
							System.out.println("[Setup]: Just sent (START)");
						}
						isClusterRunning = true;
					} 
				}				
			}
		} catch (Exception e){
			StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
		}

		shutDownNodeSocket.close();
		userInputSocket.close();
		routerSocket.close();
		context.term();
	}
}
