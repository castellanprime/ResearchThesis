import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.Scanner;
import org.zeromq.ZMQ;


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
				System.out.println("Enter your commands(START | s, QUIT | q ): ");
				String command = readIn.nextLine();
				if (!command.isEmpty()){
					socket.send(command.getBytes(), 0);
				}
				Thread.sleep(2000);
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

public class ManagementClient{
	public static void main(String [] args){
		
		ZMQ.Context context = ZMQ.context(1);

		Random rand = new Random();
		int id = rand.nextInt(10) + 1;

		// Setup pair socket 
		ZMQ.Socket userInputSocket = context.socket(ZMQ.PAIR);
		userInputSocket.bind("inproc://userinput");

		// Setup dealer socket
		ZMQ.Socket dealerSocket = context.socket(ZMQ.DEALER);
		String identity = "client-" + id;
		dealerSocket.setIdentity(identity.getBytes());
		dealerSocket.connect("tcp://127.0.0.1:5050");

		// Initialise the pollin set
		ZMQ.Poller dealerTestPoller = new ZMQ.Poller(1);
		dealerTestPoller.register(userInputSocket, ZMQ.Poller.POLLIN);		// POLLIN/POLLOUT only listen for incoming/outgoing messages 
		dealerTestPoller.register(dealerSocket, ZMQ.Poller.POLLIN);

		// Setup pair with UserInputReader Socket
		UserInputReader reader = new UserInputReader(context, "inproc://userinput");
		Thread commands = new Thread(reader);
		commands.start();

		// Main loop
		try{
			while (!Thread.currentThread().isInterrupted()){
				String message;
				dealerTestPoller.poll();

				if (dealerTestPoller.pollin(0)){
					message = new String(userInputSocket.recv(0));
					if ("START".equalsIgnoreCase(message) || "QUIT".equalsIgnoreCase(message)){
						dealerSocket.send(message.getBytes(), 0);
					} else{
						System.out.println("This command is not recognised!!!");
					}
				}

				if (dealerTestPoller.pollin(1)){
					message = new String(dealerSocket.recv(0));
					System.out.println("Received from client = " + message);
					if ("GOODBYE".equalsIgnoreCase(message)){
						reader.stopReader();
						Thread.currentThread().interrupt();
					}
				}
			}
		} catch (Exception e){
			StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
		}

		userInputSocket.close();
		dealerSocket.close();
		context.term();
	}
}
