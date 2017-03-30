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

class inputreader implements Runnable{
	public ZMQ.Socket socket;
	public Scanner userinput = new Scanner(System.in);
	private volatile boolean stopped;


	inputreader(ZMQ.Context context, String address){
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
				Thread.sleep(1000);
			} catch(InterruptedException ie){
				ie.printStackTrace();
			}
		}
	}

}


class heartbeatworker implements Runnable{
	public ZMQ.Socket socket;
	private volatile boolean stopped;
	private String stringToSend;

	heartbeatworker(ZMQ.Context context, String address, String stringToSend){
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

public class ManagementClient{

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
		ZMQ.Socket synchronizer= context.socket(ZMQ.REP);
		synchronizer.bind("tcp://127.0.0.1:5900");

		// Setup inputthread pair
		ZMQ.Socket inputsocket = context.socket(ZMQ.PAIR);
		inputsocket.bind("inproc://step1");

		// Start up the inputhread
		inputreader inputservice = new inputreader(context, "inproc://step1");
		Thread thre = new Thread(inputservice);
		thre.start();

		try{	
			while (!thre.isInterrupted()){

				// Poll for input from input thread

				byte [] inputread = inputsocket.recv(0);
				String userinput = new String(inputread);

				// The start command
				if (("START".equalsIgnoreCase(userinput) || "s".equalsIgnoreCase(userinput)) && CLUSTER_STARTS == 0){
					++CLUSTER_STARTS;

					// Setup heartbeat service
					heartbeatworker service = new heartbeatworker(context, "tcp://127.0.0.1:5800", "Hello");

					// Start heartbeat service	
					Thread thr1 = new Thread(service);
					thr1.start();

					while (subscribers < SUBSCRIBERS_REQUIRED){

						byte[] reply = synchronizer.recv(0);
						System.out.println("Subscriber " + subscribers + "  has replied: Reponse :" + new String(reply));

						synchronizer.send("I have seen you".getBytes(),0);
						subscribers++;
					}

					// First initialization part
					if(subscribers == SUBSCRIBERS_REQUIRED){
						service.stopService();
					}

				} else if ("QUIT".equalsIgnoreCase(userinput) || "q".equalsIgnoreCase(userinput)){

					// Setup heartbeat service
					heartbeatworker service = new heartbeatworker(context, "tcp://127.0.0.1:5800", userinput);

					// Start heartbeat service	
					Thread thr2 = new Thread(service);
					thr2.start();

					// Tell subscribers to go to sleep.
					while (subscribers > 0){

						byte [] reply = synchronizer.recv(0);

						System.out.println("Subscriber " + subscribers + "  has indicated sleep: Reponse :" + new String(reply));

						synchronizer.send("GoodBye");
						subscribers--;
					}

					if (subscribers == 0){
						service.stopService();
						thre.interrupt();
					}

				} else if (("START".equalsIgnoreCase(userinput) || "s".equalsIgnoreCase(userinput)) && CLUSTER_STARTS > 0){ 
					System.out.println("Cluster already started.");
				} else {
					byte [] reply = synchronizer.recv(0);
					System.out.println("Got a new message: " + new String(reply));
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