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

// To use the ROUTER socket, you have to use an id

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
	public static void main(String []  args){

		ZMQ.Context context = ZMQ.context(1);

		BlockingQueue<String> storedCounts =  new LinkedBlockingQueue<>();

		// Setup pair socket 
		ZMQ.Socket pingWorkerSocket = context.socket(ZMQ.PAIR);
		pingWorkerSocket.bind("inproc://pingWorker");

		// Setup router socket
		ZMQ.Socket routerSocket = context.socket(ZMQ.ROUTER);
		routerSocket.bind("tcp://127.0.0.1:5050");

		// Initialise the pollin set
		ZMQ.Poller routerTestPoller = new ZMQ.Poller(1);
		routerTestPoller.register(pingWorkerSocket, ZMQ.Poller.POLLIN);		// POLLIN/POLLOUT only listen for incoming/outgoing messages 
		routerTestPoller.register(routerSocket, ZMQ.Poller.POLLIN);

		// Setup pair with pingWorkerSocket
		PingWorker worker = new PingWorker(context, "inproc://pingWorker");
		Thread pings = new Thread(worker);
		//pings.start();
		
		int numberToSend = 0;
		String id = "";
		boolean isClusterRunning = false;

		// Main loop
		try{
			while (!Thread.currentThread().isInterrupted()){
				String message;
				routerTestPoller.poll();

				// if pingWorker has received a message from 
				if (routerTestPoller.pollin(0)){
					message = new String(pingWorkerSocket.recv(0));
					System.out.println("Received message = " + message);
					storedCounts.offer(message);
				}

				if (storedCounts.size() == 100){
					ArrayList<String> listtoSend = new ArrayList<>();
							// drain the queue to the listtoSend
					numberToSend = storedCounts.size() - 2;
					storedCounts.drainTo(listtoSend, numberToSend);	
					for (String item: listtoSend){
						routerSocket.send(id.getBytes(), ZMQ.SNDMORE);
						routerSocket.send(item.getBytes(), 0);
						System.out.println("Sending " + item + " to dealer");
						numberToSend--;
					}
				}

				if (routerTestPoller.pollin(1)){
					id = new String(routerSocket.recv(0));  
					message = new String(routerSocket.recv(0));
					if ("QUIT".equalsIgnoreCase(message)){
						if (numberToSend == 0){
							worker.stopWorker();
							routerSocket.send(id, ZMQ.SNDMORE);
							routerSocket.send("GOODBYE".getBytes(), 0);
							Thread.currentThread().interrupt();
						} else {
							System.out.println("System is still sending " + numberToSend + " messages");
						}
					}

					if ("START".equalsIgnoreCase(message) && isClusterRunning == false){
						routerSocket.send(id, ZMQ.SNDMORE);
						if (isClusterRunning == false){
							routerSocket.send("Starting counter".getBytes(), 0);
							pings.start();
							isClusterRunning = true;	
						}else {
							routerSocket.send("Cluster has already started".getBytes(), 0);
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

        pingWorkerSocket.close();
        routerSocket.close();
        context.term();
	}
}