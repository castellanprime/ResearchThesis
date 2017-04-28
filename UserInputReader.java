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


/**
 *  This reads user input in the form of commands and sends it to the entire cluster
 *  or a particular node in the cluster
 *  -	START : 									Starts the cluster
 *  -	QUERY [nodenum] [numberofdebugmessages]: 	Retrieves a certain amount of debug 
 *  												messages from a node in cluster
 *  -	STOP [nodenum]:								Stop a particular node
 *  -	RESTART [nodenum]:							Restart a particular node
 *  - 	QUIT :										Shutdown the entire cluster
 */


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;
import org.zeromq.ZMQ;


public class UserInputReader{
	
	public static void main(String [] args){
		ZMQ.Context context = ZMQ.context(1);
		Scanner readIn = new Scanner(System.in);
		ZMQ.Socket socket =  context.socket(ZMQ.REQ);
		socket.connect("tcp://127.0.0.1:6000");
		try{
			while(!Thread.currentThread().isInterrupted()){
				try{
					System.out.println("Enter your commands(START, QUERY [nodenum] [number of messages], " + 
								"STOP [nodenum], RESTART [nodenum], QUIT): ");
					String command = readIn.nextLine();
					if (command.isEmpty()){
						command = "CONTINUE";
					}
					socket.send(command.getBytes(), 0);
					System.out.println("[Input]: Just sent (" + command +")");
					String reply = new String(socket.recv(0));
					System.out.println("[Input]: Received (" + reply + ")");
					if ("QUIT".equalsIgnoreCase(command)){
						break;
					}
					Thread.sleep(9000);			// Arbitary sleep times
				} catch (InterruptedException ie){
					Thread.currentThread().interrupt();
					ie.printStackTrace();
				}
			}
		} catch (Exception e){
			StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
		}

		socket.close();
		context.term();
	}
}


