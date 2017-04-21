import java.io.IOException;
import java.util.*;

public class Bootstrap{
	
	public enum COMMANDS{
		START, STOP, POST, QUERY
	}

	private String [] programs;
	private int numofPrograms;

	public void command(Enum.COMMANDS command, String[] programs, String message){
		switch(command){
			case START: start();
						break;
			case STOP: stop();
						break;
			case POST:	post(message);
						break;
			case QUERY: query(programs);
						break;
			default:
					System.out.println("Wrong example");
		}
	}

	Bootstrap(){
		this.(null);
	}

	Bootstrap (String [] programs){
		this.programs = programs;
		numofPrograms = this.programs.length;
	}

	private Process[] start(){
		
	}
}