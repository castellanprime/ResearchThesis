import java.io.PrintWriter;
import java.io.StringWriter;
import org.zeromq.ZMQ;

public class Server{
	public static void main(String[] args){
		ZMQ.Context context = ZMQ.context(1);
		
		ZMQ.Socket responder = context.socket(ZMQ.REP);
		responder.bind("tcp://127.0.0.1:5555");
		try{
			while(!Thread.currentThread().isInterrupted()){
				// Wait for next request from the client
	            byte[] request = responder.recv(0);
	            System.out.println("Received Hello");

	            // Do some 'work'
	            Thread.sleep(1000);

	            // Send reply back to client
	            String reply = "World";
	            responder.send(reply.getBytes(), 0);
	        }
    	}catch(Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw.toString());
        }
        responder.close();
        context.term();		
	}
}
