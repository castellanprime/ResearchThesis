import java.io.PrintWriter;
import java.io.StringWriter;
import org.zeromq.ZMQ;

public class EchoServer{
	public static void main(String[] args){
		ZMQ.Context context = ZMQ.context(1);
		
		ZMQ.Socket responder = context.socket(ZMQ.REP);
		responder.bind("tcp://127.0.0.1:5555");
		try{
			while(!Thread.currentThread().isInterrupted()){
				// Wait for next request from the client
	            byte[] request = responder.recv(0);
	            if (request.length != 0 && !"NO".equalsIgnoreCase(new String(request))){
	            	System.out.println("Received(client request): " + new String(request));

	            	// Do some 'work'
		            Thread.sleep(1000);

		            // Send reply back to client
		            String reply = "Previous command: " + new String(request) +  "\n Next Command: \n";
		            responder.send(reply.getBytes(), 0);	
	            }else {
	            	String reply = "Server is closing";
		            responder.send(reply.getBytes(), 0);
	            	Thread.currentThread().interrupt();
	            }
	            
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