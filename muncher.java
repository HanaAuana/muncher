
import java.io.File;
import java.io.FileReader;

import java.io.IOException;

import java.util.Scanner;


class muncher {
	
	private static String FILENAME;

	public muncher() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FILENAME = args[0];
		DataBuffer data = new DataBuffer();
		ReaderThread reader = new ReaderThread(data,FILENAME, 0);

	}

}

class ReaderThread extends Thread 
{ 
	DataBuffer data;

	ReaderThread(DataBuffer data, String filename, int lineNum){ //Take a command as a parameter
		
		this.data = data;

		try(Scanner file = new Scanner(new FileReader(new File(filename)));){
			
			for(int i = 0; i < lineNum; ++i)
			{
				file.nextLine();
			}
			String s = file.nextLine();
			System.out.println("Line "+ lineNum+": "+ s);
		}
		catch(IOException e){
			e.printStackTrace();
		}


	}

	public void run(){ //On run, simply execute a command as usual, but now wrapped in a thread

	}
}

class DataBuffer {
	String[] data;
	Status[] statusOf;

	enum Status {EMPTY, READ, COUNTED, NUMBERED, WRITTEN};

	public DataBuffer() {
		data = new String[8];
		statusOf = new Status[8];
		for (int i = 0; i < statusOf.length; i++){
			statusOf[i] = Status.EMPTY;
		}
	}

	public String getString(int which) {

		return data[which];
	}

	public void setString(int which, String newString) {

		data[which] = newString;
	}

	public void setStatus(int which, Status newStatus){

		statusOf[which] = newStatus;
	}
}