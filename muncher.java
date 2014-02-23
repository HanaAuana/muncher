
import java.io.File;
import java.io.FileReader;

import java.io.IOException;

import java.util.Scanner;


class muncher {

	static final boolean DEBUG = false;

	private static String FILENAME;

	public muncher() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//FILENAME = args[0];
		DataBuffer data = new DataBuffer();
		ReaderThread reader = new ReaderThread(data,"input.txt");
		CounterThread counter = new CounterThread(data);
		NumberThread number = new NumberThread(data,"input.txt");
		WriterThread writer = new WriterThread(data);


		Thread[] threads = new Thread[4]; //Set up an array for our threads
		threads[0] = reader;
		threads[1] = counter;
		threads[2] = number;
		threads[3] = writer;
		threads[0].start(); //Start the thread
		threads[1].start();
		threads[2].start();
		threads[3].start();

		//		for(int i = 0; i < threads.length; i++){ //Once we've created all our threads
		//			try {
		//				threads[i].join(); //Wait for them all to finish
		//			} catch (InterruptedException e) {
		//				e.printStackTrace();
		//			}
		//		}


	}

}

class ReaderThread extends Thread 
{ 
	DataBuffer data;
	String filename;
	int lineNum;


	ReaderThread(DataBuffer data, String filename){ //Take a command as a parameter
		this.data = data;
		this.filename = filename;
		this.lineNum = 1;
	}

	public void run(){ //On run, simply execute a command as usual, but now wrapped in a thread
		while(true){
			synchronized (this.data){
				int emptyLoc = data.hasSpace();
				if(emptyLoc != -1){ //If there's at least one empty spot in the buffer


					while(emptyLoc != -1){
						if(muncher.DEBUG){
							System.out.println("Reading a string into: "+emptyLoc);
						}


						try(Scanner file = new Scanner(new FileReader(new File(filename)));){
							if(file.hasNext()){
								for(int i = 1; i < lineNum; ++i)
								{

									file.nextLine();
								}
								if(file.hasNext()){
									data.setString(emptyLoc ,file.nextLine());
									data.setStatus(emptyLoc, DataBuffer.Status.READ);
									this.lineNum++;
									data.notifyAll();
								}
								else{
									break;
								}
							}
						}
						catch(IOException e){
							e.printStackTrace();
						}
						emptyLoc = data.hasSpace();
					}
				}
				else{
					if(muncher.DEBUG){
						System.out.println("No strings to read");
						//					for(int i = 0; i < 8; i ++){
						//						System.out.println(data.getStatus(i));
						//					}
					}
				}
				if(emptyLoc == -1){
					try {
						data.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

class CounterThread extends Thread 
{ 
	DataBuffer data;


	CounterThread(DataBuffer data){ //Take a command as a parameter

		this.data = data;

	}

	public void run(){ //On run, simply execute a command as usual, but now wrapped in a thread
		while(true){
			synchronized (this.data){
				int readyLoc = data.hasRead();
				if(readyLoc != -1){ //If there's at least one read String in the buffer

					while(readyLoc != -1){
						if(muncher.DEBUG){
							System.out.println("Counting string at: "+readyLoc);
						}

						String toCount = data.getString(readyLoc);
						int charCount = toCount.length();

						data.setString(readyLoc, toCount.concat(" ("+charCount+")"));
						data.setStatus(readyLoc, DataBuffer.Status.COUNTED);
						data.notifyAll();
						readyLoc = data.hasRead();
					}

				}
				else{
					if(muncher.DEBUG){
						System.out.println("No strings to Count");
						//					for(int i = 0; i < 8; i ++){
						//						System.out.println(data.getStatus(i));
						//					}
					}
				}
				if(readyLoc == -1){
					try {
						data.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

class NumberThread extends Thread 
{ 
	DataBuffer data;
	int lineNum;
	String filename;


	NumberThread(DataBuffer data, String filename){ //Take a command as a parameter
		this.data = data;
		this.filename = filename;
	}

	public void run(){ //On run, simply execute a command as usual, but now wrapped in a thread
		while(true){
			synchronized (this.data){
				int countedLoc = data.hasCounted();


				if(countedLoc != -1){ //If there's at least one read String in the buffer

					while(countedLoc != -1){
						if(muncher.DEBUG){
							System.out.println("Numbering String at: "+countedLoc);
						}
						String toNumber;
						if(data.getString(countedLoc).length() < 5){
							toNumber = "";
						}
						else{
							toNumber = data.getString(countedLoc).substring(0, data.getString(countedLoc).length()-5);
						}
						try(Scanner file = new Scanner(new FileReader(new File(filename)));){

							int lineNum = 0;
							while(file.hasNext()){
								lineNum ++;
								String toCheck = file.nextLine();
								if(toNumber.equals(toCheck)){
									String prefix = lineNum+": ";
									data.setString(countedLoc, prefix.concat(data.getString(countedLoc)));
									data.setStatus(countedLoc, DataBuffer.Status.NUMBERED);
									data.notifyAll();
								}
							}
						}
						catch(IOException e){
							e.printStackTrace();
						}
						countedLoc = data.hasCounted();
					}

				}
				else{
					if(muncher.DEBUG){
						System.out.println("No strings to Number");
						//					for(int i = 0; i < 8; i ++){
						//						System.out.println(data.getStatus(i));
						//					}
					}
				}
				//			else{
				//				if(muncher.DEBUG){
				//					System.out.println("No string to number");
				//					for(int i = 0; i < 8; i ++){
				//						System.out.println(data.getString(i));
				//					}
				//					for(int i = 0; i < 8; i ++){
				//						System.out.println(data.getStatus(i));
				//					}
				//				}
				//
				//			}
				if(countedLoc == -1){
					try {
						data.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}


class WriterThread extends Thread 
{ 
	DataBuffer data;


	WriterThread(DataBuffer data){ //Take a command as a parameter

		this.data = data;

	}

	public void run(){ //On run, simply execute a command as usual, but now wrapped in a thread
		while(true){
			synchronized (this.data){
				int numberedLoc = data.hasNumbered();
				if(numberedLoc != -1){ //If there's at least one read String in the buffer
					while(numberedLoc != -1){
						if(muncher.DEBUG){
							System.out.println("Writing string at: "+numberedLoc);
						}
						String toWrite = data.getString(numberedLoc);
						System.out.println(toWrite);
						data.setStatus(numberedLoc, DataBuffer.Status.EMPTY);
						data.notifyAll();
						numberedLoc = data.hasNumbered();
					}

				}
				else{
					if(muncher.DEBUG){
						System.out.println("No strings to write");
						//					for(int i = 0; i < 8; i ++){
						//						System.out.println(data.getStatus(i));
						//					}
					}
				}
				if(numberedLoc == -1){
					try {
						data.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

class DataBuffer {
	String[] data;
	Status[] statusOf;

	enum Status {EMPTY, READ, COUNTED, NUMBERED}; //Enum type to keep track of the status of each string in the buffer

	public DataBuffer() {
		data = new String[8]; //Initialize arrays
		statusOf = new Status[8];
		for (int i = 0; i < statusOf.length; i++){ //Set status of each index to EMPTY
			statusOf[i] = Status.EMPTY;
		}
	}

	public String getString(int which) {

		return data[which];
	}

	public void setString(int which, String newString) {

		data[which] = newString;
	}

	public Status getStatus(int which) {

		return statusOf[which];
	}

	public void setStatus(int which, Status newStatus){

		statusOf[which] = newStatus;
	}

	public int hasSpace(){ //Returns -1 if all indices are full, otherwise returns first empty index
		for(int i = 0; i < statusOf.length; i++){
			if (statusOf[i] == Status.EMPTY){
				return i;
			}
		}
		return -1;
	}

	public int hasRead(){ //Returns -1 if no indices are ready for counting, otherwise returns first ready index
		for(int i = 0; i < statusOf.length; i++){
			if (statusOf[i] == Status.READ){
				return i;
			}
		}
		return -1;
	}

	public int hasCounted(){ //Returns -1 if no indices are ready for numbering, otherwise returns first ready index
		for(int i = 0; i < statusOf.length; i++){
			if (statusOf[i] == Status.COUNTED){
				return i;
			}
		}
		return -1;
	}

	public int hasNumbered(){ //Returns -1 if no indices are ready for writing, otherwise returns first ready index
		int lowest = -1;
		for(int i = 0; i < statusOf.length; i++){
			if (statusOf[i] == Status.NUMBERED){
				return i;
			}
		}
		return -1;
	}
}