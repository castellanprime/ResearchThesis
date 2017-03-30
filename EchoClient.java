import java.io.PrintWriter;
import java.io.StringWriter;
import org.zeromq.ZMQ;
import java.util.*;

public class EchoClient{
	public static void main(String[] args){
		ZMQ.Context context = ZMQ.context(1);
        Scanner scanner = new Scanner(System.in);
        boolean close = false;

        //  Socket to talk to server
        System.out.println("Connecting to echo world server…");

        ZMQ.Socket requester = context.socket(ZMQ.REQ);
        requester.connect("tcp://localhost:5555");
        try {
            while (true){
                System.out.println("Please input a line");
                String line = scanner.nextLine();
                System.out.printf("User input was: %s%n", line);
                if (line.isEmpty() || "NO".equalsIgnoreCase(line)){
                    close = true;
                } 
                requester.send(line.getBytes(), 0);
                byte[] reply = requester.recv(0);
                System.out.println("Received(server reponse): " + new String(reply));
                if (close == true){
                    return;
                }
                System.out.println("Please input a line");
                line = scanner.nextLine();
                System.out.printf("User input was: %s%n", line);
            }
            
        } catch(IllegalStateException | NoSuchElementException e) {
            // System.in has been closed
            System.out.println("System.in was closed; exiting");
        }
        requester.close();
        context.term();
	}
}