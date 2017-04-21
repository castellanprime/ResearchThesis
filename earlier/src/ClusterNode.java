import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.*;
import java.util.Random;
import java.util.ArrayList;
import org.zeromq.ZMQ;

/**
 *  Pseudocode for alogorithm
 *  1. Server wakes up
 *  2. It sends a connection request to the server
 *  3. It receives a connection reponse.
 *  3. It needs to receive requests and send replies to the client( DEALER)
 */

class Counter {
	private int count = 0;
	public void setCount(int count) { this.count = count; }
	public int getCount() { return this.count; }
}

class WorkerTask implements Runnable {
  	public ZMQ.Socket socket;	
	private final int maxCount;
	private final Counter counter;
	private String toSend;

	public WorkerTask(ZMQ.Context context, String address, int maxCount, Counter counter) {
		this.socket = context.socket(ZMQ.PAIR);
		socket.connect(address);
	    this.maxCount = maxCount; 
	    this.counter = counter;
	    toSend ="";
	}

	public void run() {
	    for (int i = 0; i < this.maxCount; i++) {
	    	int currentCount;
	    	try{
		    	synchronized(this.counter) {
		    		currentCount = this.counter.getCount();
		    		toSend = Integer.toString(currentCount);
		    		socket.send(toSend.getBytes(), 0); 
		    		currentCount++;
		    		this.counter.setCount(currentCount);
		    		System.out.println(Thread.currentThread().getName()
		    	   	+ " at count " + currentCount);
		      	}
		      	Thread.sleep(100);
		      	if (i+1 == this.maxCount){
		      		socket.close();
		      		Thread.currentThread().interrupt();
		      	}
		    } catch (InterruptedException ie){
		    	ie.printStackTrace();
		    }
	    }
	}
}

public class ClusterNode{
	public static void main(String[] args){

		Random rand = new Random();

		ZMQ.Context context = ZMQ.context(1);	// number of iothread threads in 0MQ thread pool

		BlockingQueue<String> storedRepliesQueue =  new LinkedBlockingQueue<>();	// for sending messages to management client

		// Subscribe for hello/Quit messages from the management client
		ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
		subscriber.setRcvHWM(0);
		subscriber.connect("tcp://127.0.0.1:5800");
		subscriber.subscribe("".getBytes());


		// Setup up the replier socket for communicating with management client
		ZMQ.Socket replier = context.socket(ZMQ.DEALER);
		replier.connect("tcp://127.0.0.1:5900");

		// Setup the socket's identity
		String identity = String.format("%d", rand.nextInt(5));
		replier.setIdentity(identity.getBytes());

		// Setup workerthread pair
		ZMQ.Socket workerSocket = context.socket(ZMQ.PAIR);
		workerSocket.bind("inproc://counter");

		// Start up the counter thread
		Counter workerCounter = new Counter();
		WorkerTask worker = new WorkerTask(context, "inproc://counter", 10000, workerCounter);
		Thread threa = new Thread(worker);
		//threa.start();	

		// Initialize poll set
		ZMQ.Poller socketPoller = new ZMQ.Poller(3);
		socketPoller.register(subscriber, ZMQ.Poller.POLLIN);
		socketPoller.register(replier, ZMQ.Poller.POLLIN);
		socketPoller.register(workerSocket, ZMQ.Poller.POLLIN);

		//boolean more = False;
		boolean counter_isStarted = false;

		// Switch messages between sockets
		try{
			while (!Thread.currentThread().isInterrupted()){
				byte []  message;
				socketPoller.poll();
				if (socketPoller.pollin(0)){
					if ("HELLO".equalsIgnoreCase(subscriber.recvStr(0))){
						//replier.sendMore("");
						replier.send("I am here");
						System.out.println("Fuck2");
					}	

					if ("QUIT".equalsIgnoreCase(subscriber.recvStr(0))){
						replier.sendMore("");
						replier.send("Okay, quitting now");
						threa.interrupt();
						break;
					}
				}

				if (socketPoller.pollin(1)){
					String command = replier.recvStr();
					System.out.println("Fuck3");
					String[] words = command.split("\\s");
					System.out.println("Fuck4");
					if ("QUERY".equalsIgnoreCase(words[0])){
						//message = replier.recv(0);
						String value =  new String(message);
						int finalValue = Integer.parseInt(value);
							
						ArrayList<String> listtoSend = new ArrayList<>();
							
						if (storedRepliesQueue.size() + 1 > finalValue){
							// drain the queue to the listtoSend
							storedRepliesQueue.drainTo(listtoSend, finalValue);	
							for (String item: listtoSend){
								replier.send(item);
							}
						}else{
							replier.send("IllegalOperation: Number is too large");
						}
					}else if ("I have seen you".equalsIgnoreCase(command)){
						System.out.println("Fuck4");
						System.out.println(command);

						if (counter_isStarted == false){
							threa.start();
							counter_isStarted = true;
						}
					}
				}

				if (socketPoller.pollin(2)){
					message = workerSocket.recv(0);
					String queryReply = new String(message);
					storedRepliesQueue.offer(queryReply);
				}

			}
		} catch(Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
        }

		workerSocket.close();
		replier.close();
		subscriber.close();
		context.term();
	}
}