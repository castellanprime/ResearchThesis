import java.io.PrintWriter;
import java.io.StringWriter;
import org.zeromq.ZMQ;
import java.util.*;

/**
 *  Pseudocode for alogorithm
 *  1. Client wakes up
 *  2. It waits for all the servers in the cluster to connect.
 *  3. It needs to send requests to all/some servers and receive replies from them(ROUTER)
 */

class InputReader implements Runnable{
	public ZMQ.Socket socket;
	public Scanner userinput = new Scanner(System.in);

	InputReader(ZMQ.Context context, String address){
		this.socket = context.socket(ZMQ.PAIR);
		socket.connect(address);
	}

	@Override
	public void run(){
		while(true){
			try{
				System.out.println("Enter your commands(START | s, QUIT | q): ");
				if (userinput.hasNext()){
					String input = userinput.next();
					socket.send(input.getBytes(), 0);
					if ("q".equalsIgnoreCase(input) || "QUIT".equalsIgnoreCase(input)){
						socket.close();
						break;
					}
				}
				Thread.sleep(2000);
			} catch(InterruptedException ie){
				ie.printStackTrace();
			}
		}
	}

}

class HeartBeatWorker implements Runnable{
	public ZMQ.Socket socket;
	private volatile boolean stopped;
	private String stringToSend;

	HeartBeatWorker(ZMQ.Context context, String address, String stringToSend){
		this.socket = context.socket(ZMQ.PUB);
		this.stringToSend = stringToSend;
		socket.setLinger(5000);
		socket.setSndHWM(0);
		socket.bind(address);	
	}

	@Override
	public void run(){
		while(!stopped){
			socket.send(stringToSend.getBytes(), 0);
			try{
				System.out.println("Just sent " + stringToSend);
				Thread.sleep(10);
			} catch(InterruptedException ie){
				ie.printStackTrace();
			}
		}	
		socket.close();
	}

	public void stopService(){
		stopped = true;
	}
}


public class ManagementPortal{

	public static void main(String[] args){

		int SUBSCRIBERS_REQUIRED = 0, CLUSTER_STARTS = 0, subscribers = 0;

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

		// Setup sychronizer port
		ZMQ.Socket synchronizer= context.socket(ZMQ.ROUTER);
		synchronizer.bind("tcp://127.0.0.1:5900");

		// Setup inputthread pair
		ZMQ.Socket inputsocket = context.socket(ZMQ.PAIR);
		inputsocket.bind("inproc://reader");

		// Start up the inputhread
		InputReader inputservice = new InputReader(context, "inproc://reader");
		Thread thre = new Thread(inputservice);
		thre.start();


		// Initialize poll set
		ZMQ.Poller poller = new ZMQ.Poller(1);
		poller.register(inputsocket, ZMQ.Poller.POLLIN);

		try{
			while(!Thread.currentThread().isInterrupted()){
				byte [] message;
				poller.poll();

				if(poller.pollin(0)){
					String command = inputsocket.recvStr();
					String[] words = command.split("\\s");
					if (("START".equalsIgnoreCase(words[0]) || "s".equalsIgnoreCase(words[0])) && CLUSTER_STARTS == 0){
						++CLUSTER_STARTS;

						// Setup heartbeat service
						heartbeatworker service = new heartbeatworker(context, "tcp://127.0.0.1:5800", "HELLO");

						// Start heartbeat service	
						Thread thr1 = new Thread(service);
						thr1.start();

						while (subscribers < SUBSCRIBERS_REQUIRED){

							String response = synchronizer.recvStr();
							//message = synchronizer.recv();
							System.out.println("Subscriber " + subscribers + "  has replied: Reponse :" + new String(response));

							//synchronizer.sendMore(" ");
							synchronizer.send("I have seen you");
							subscribers++;
						}

						// First initialization part
						if(subscribers == SUBSCRIBERS_REQUIRED){
							service.stopService();
						}
					}else if (("START".equalsIgnoreCase(words[0]) || "s".equalsIgnoreCase(words[0])) && CLUSTER_STARTS > 0){
						System.out.println("Cluster already started.");
					}else if ("QUIT".equalsIgnoreCase(words[0]) || "q".equalsIgnoreCase(words[0])){

						// Setup heartbeat service
						heartbeatworker service = new heartbeatworker(context, "tcp://127.0.0.1:5800", words[0]);

						// Start heartbeat service	
						Thread thr2 = new Thread(service);
						thr2.start();

						// Tell subscribers to go to sleep.
						while (subscribers > 0){
							byte [] envelope = synchronizer.recv();
							message = synchronizer.recv();

							System.out.println("Subscriber " + subscribers + "  has replied: Reponse :" + new String(message));
							subscribers--;
						}
						
						if (subscribers == 0){
							service.stopService();
							Thread.currentThread().interrupt();
						}

					}else if ("QUERY".equalsIgnoreCase(words[0])){
						int numberToRetrieve = Integer.parseInt(words[1]);
						synchronizer.send(words[0]);
						synchronizer.sendMore(words[1]);

						message = synchronizer.recv();
						String response = new String(message);
						if (!"IllegalOperation: Number is too large".equalsIgnoreCase(response)){
							for (int i = 0; i < numberToRetrieve; numberToRetrieve++){
								System.out.println("Received " + response + ": from Server");
							}
						}
					} 		
				}
			}
		} catch(Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
        }

		// Close ports
		inputsocket.close();
		synchronizer.close();
		context.term();		
	}
}