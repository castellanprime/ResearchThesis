import java.io.PrintWriter;
import java.io.StringWriter;
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

class workertask implements Runnable {
  	public ZMQ.Socket socket;	
	private final int maxCount;
	private final Counter counter;

	public workertask(ZMQ.Context context, String address, int maxCount, Counter counter) {
		this.socket = context.socket(ZMQ.PAIR);
		socket.connect(address);
	    this.maxCount = maxCount; 
	    this.counter = counter;
	}

	public void run() {
	    for (int i = 0; i < this.maxCount; i++) {
	    	int currentCount;
	    	synchronized(this.counter) {
	    		socket.send((String.valueOf(this.counter.getCount())).getBytes(), 0);
	    		currentCount = this.counter.getCount();
	    		currentCount++;
	    		this.counter.setCount(currentCount);
	      	}
	      	System.out.println(Thread.currentThread().getName()
	    	   	+ " at count " + currentCount);
	      	if (i+1 == this.maxCount){
	      		socket.close();
	      		Thread.currentThread().interrupt();
	      	}
	    }
	    socket.close();
	}
}


public class ClusterServer{
	public static void main(String[] args){
		ZMQ.Context context = ZMQ.context(1);

		// Receive hello from the management client
		ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
		subscriber.setRcvHWM(0);
		subscriber.connect("tcp://127.0.0.1:5800");
		subscriber.subscribe("".getBytes());

		ZMQ.Socket heartbeatReplier = context.socket(ZMQ.REQ);
		heartbeatReplier.connect("tcp://127.0.0.1:5900");

		if ("Hello".equalsIgnoreCase(subscriber.recvStr(0))){
			heartbeatReplier.send("I am here".getBytes(), 0);

			byte [] reply = heartbeatReplier.recv(0);
			System.out.println("Reply from server for initial hello:" + new String(reply));
		}

		// Setup workerthread pair
		ZMQ.Socket workersocket = context.socket(ZMQ.PAIR);
		workersocket.bind("inproc://step2");

		// Start up the inputhread
		Counter r_counter = new Counter();
		workertask worker = new workertask(context, "inproc://step2", 10000, r_counter);
		Thread threa = new Thread(worker);
		threa.start();	

		while (!Thread.currentThread().isInterrupted()){

			byte [] inputrecv = workersocket.recv(0);
			heartbeatReplier.send(inputrecv, 0);

			if ("QUIT".equalsIgnoreCase(subscriber.recvStr(0))){
				threa.interrupt();
				//Problem is here
				heartbeatReplier.send(" I am going to sleep".getBytes(), 0);

				byte [] reply = heartbeatReplier.recv(0);
				String serverResponse = new String(reply);
				System.out.println("Reply from server for initial hello: " + serverResponse);
				if ("GoodBye".equalsIgnoreCase(serverResponse)){
					Thread.currentThread().interrupt();
				}
			}
		}
				
		workersocket.close();
        heartbeatReplier.close();
		subscriber.close();
		context.term();
	}
}