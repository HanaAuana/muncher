// Michael Lim

// How to use:
// Pass the name of the input file via command line
// Threads will continue to run indefinitely, so
// user will need to manually kill the program
//

import java.io.File;        //Imports for file reading
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;


class muncher {
	static boolean RUN = true; //Global constant to determine whether threads keep running

	static String FILENAME; //Global constant recording the input file name
	
	static String READ_NAME = "Reader"; // Names that will be used for the threads
	static String COUNT_NAME = "Counter";
	static String NUMBER_NAME = "Numberer";
	static String WRITE_NAME = "Writer";

	/**
	 * Creates a DataBuffer and 4 threads, then reads, counts, numbers, and writes 
	 * lines in an input file
	 */
	public static void main(String[] args) {
		FILENAME = args[0]; //Gets file name from command line argument
		
		DataBuffer data = new DataBuffer(4, READ_NAME, COUNT_NAME, NUMBER_NAME ,WRITE_NAME);
		
		ReaderThread reader = new ReaderThread(data,FILENAME); //Create threads, passing the
		CounterThread counter = new CounterThread(data);       //shared DataBuffer
		NumberThread number = new NumberThread(data);
		WriterThread writer = new WriterThread(data);


		Thread[] threads = new Thread[4]; //Set up an array for our threads
		threads[0] = reader;
		threads[1] = counter;
		threads[2] = number;
		threads[3] = writer;
		
		threads[0].setName(READ_NAME); //Set thread names
		threads[1].setName(COUNT_NAME);
		threads[2].setName(NUMBER_NAME);
		threads[3].setName(WRITE_NAME);

		threads[0].start(); //Start the threads
		threads[1].start();
		threads[2].start();
		threads[3].start();

		for(int i = 0; i < threads.length; i++){ //For each thread,
			try {
				threads[i].join(); //wait for them all to finish
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

class ReaderThread extends Thread { //Thread that reads a file line by line
	DataBuffer data;                //and stores each line in a DataBuffer
	String filename;
	int lineNum;
	Scanner in;

	ReaderThread(DataBuffer data, String filename){ //Initialize fields
		this.data = data;
		this.filename = filename;
		this.lineNum = 1;
		this.in = new Scanner(System.in);
	}

	public void run(){//Reads a string in from a file and stores it in the buffer
		while(muncher.RUN){ //Continuously loop to keep reading lines
			synchronized (this.data){ //Synchronize on the shared DataBuffer
				
				int emptyLoc = data.hasSpace(); //Get the location of an empty buffer slot
				if(emptyLoc != -1){ //If there's at least one empty spot in the buffer
					while(emptyLoc != -1){ //As long as we have empty buffer space
						
						try(Scanner file = new Scanner(new FileReader(new File(filename)));){
							if(file.hasNext()){ //If there's another line to read
								
								for(int i = 1; i < lineNum; ++i){ // Skip lines we've already read
									file.nextLine();
								}
								
								if(file.hasNext()){ //If there's still another line
									
									data.setString(emptyLoc ,file.nextLine()); //Store the next line
									data.setStatus(emptyLoc, DataBuffer.Status.READ); //Set status for that String to READ
									this.lineNum++; //Increment line counter
									data.notifyAll(); //Let other threads know we're done
								}
								else{ //If we've read everything, just exit
									break;
								}
							}
						}
						catch(IOException e){
							e.printStackTrace();
						}
						emptyLoc = data.hasSpace(); //See if we have another empty slot
					}
				}
				if(emptyLoc == -1){ //If we don't have a spot in our buffer
					try {
						data.wait(); //Release our lock and wait
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}


class CounterThread extends Thread { 
	DataBuffer data;

	CounterThread(DataBuffer data){	
		this.data = data;
	}

	public void run(){ //Counts the number of characters in a string from the buffer
		while(muncher.RUN){ //Continuously loop to keep counting lines
			synchronized (this.data){ //Sync on shared buffer
				
				int readyLoc = data.hasRead(); //Get the location of a buffer slot with a read String
				if(readyLoc != -1){ //If there's at least one read String in the buffer
					while(readyLoc != -1){ //While there are read strings
						
						String toCount = data.getString(readyLoc); //Get a string
						int charCount = toCount.length(); //Count it
						
						data.setString(readyLoc, toCount.concat(" ("+charCount+")")); //Set the String
						data.setStatus(readyLoc, DataBuffer.Status.COUNTED);// Set the strings status to COUNTED
						data.notifyAll(); //Let other threads know that we're ready
						
						readyLoc = data.hasRead(); //Get the next string to count
					}
				}
				if(readyLoc == -1){ //If there are no strings to count
					try {
						data.wait(); //Release lock and wait
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

class NumberThread extends Thread { 
	DataBuffer data;
	int lineNum;

	NumberThread(DataBuffer data){
		this.data = data;
	}

	public void run(){//Prepends a line number to a string from the buffer
		while(muncher.RUN){//Continuously loop to keep numbering lines
			synchronized (this.data){//Sync on shared buffer
				
				int[] countedArray = data.hasCounted();//Get an array containing the index of a counted string, and the line number
				int countedLoc = countedArray[0];      // Get the location of the counted string
				int countedLocCount = countedArray[1]; //Get the line number for the string
				
				if(countedLoc != -1){ //If there's at least one counted String in the buffer
					while(countedLoc != -1){//While there are strings to number
						
						String prefix = countedLocCount+": "; //Create the prefix 
						
						data.setString(countedLoc, prefix.concat(data.getString(countedLoc))); //Set the string in the buffer
						data.setStatus(countedLoc, DataBuffer.Status.NUMBERED); //Change the String's status to NUMBERED
						data.notifyAll(); //Let the other threads know we're done
						
						countedArray = data.hasCounted(); //Get the next counted index/line number pair
						countedLoc = countedArray[0];
						countedLocCount = countedArray[1];
					}
				}
				if(countedLoc == -1){ //If there's no counted strings
					try {
						data.wait(); //Release our lock and wait
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}


class WriterThread extends Thread { 
	DataBuffer data;

	WriterThread(DataBuffer data){
		this.data = data;
	}
	public void run(){//Prints a string from the buffer
		while(muncher.RUN){//Continuously loop to keep writing lines
			synchronized (this.data){//Sync on shared buffer
				
				int numberedLoc = data.lowestNumbered();//Get the location of the lowest numbered string
				if(numberedLoc != -1){ //If there's a numbered String in the buffer
					
					while(numberedLoc != -1){//While there is a numbered string 
						String toWrite = data.getString(numberedLoc); //Get the string from the buffer
						System.out.println(toWrite); //Print the string
						
						data.setString(numberedLoc,""); //Set the location in the buffer to ""
						data.setStatus(numberedLoc, DataBuffer.Status.EMPTY); //Set the status to EMPTY
						data.notifyAll(); //Let the other threads know we're done
						
						numberedLoc = data.lowestNumbered(); //Get the next numbered String
					}
				}
				if(numberedLoc == -1){//If there are no strings ready to write
					try {
						data.wait();//Release our lock and wait
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
	int[] accessCounts;
	
	String READ_NAME; //Declare strings to store thread names
	String COUNT_NAME;
	String NUMBER_NAME;
	String WRITE_NAME;

	enum Status {EMPTY, READ, COUNTED, NUMBERED}; //Enum type to keep track of the status of
												  // each string in the buffer
	public DataBuffer(int numThreads, String rName, String cName, String nName, String wName) {
		data = new String[8]; //Initialize arrays
		statusOf = new Status[8];
		for (int i = 0; i < statusOf.length; i++){ //Set status of each index to EMPTY
			statusOf[i] = Status.EMPTY;
		}
		accessCounts = new int[numThreads];//Initalize an array to track the count of
											// times a thread has accessed this buffer
		for(int i = 0; i < accessCounts.length; i++){ //Set counts to start at 1
			accessCounts[i] = 1;
		}
		
		READ_NAME = rName; //Set thread names
		COUNT_NAME = cName;
		NUMBER_NAME = nName;
		WRITE_NAME = wName;
	}

	public String getString(int which) { //Return string at a given index

		return data[which];
	}

	public void setString(int which, String newString) {//Sets a string to a given value at a given index
		
		incrementCount(); //Increments the counter each time a string is set

		data[which] = newString;
	}
	
	public void incrementCount(){ //Increments a counter for the thread currently accessing the buffer
		String curThread = Thread.currentThread().getName(); //Get the name of the current thread
		if(curThread.equals(READ_NAME)){
			accessCounts[0]++;
		}
		else if(curThread.equals(COUNT_NAME)){
			accessCounts[1]++;
		}
		else if(curThread.equals(NUMBER_NAME)){
			accessCounts[2]++;
		}
		else if(curThread.equals(WRITE_NAME)){
			accessCounts[3]++;
		}
	}

	public Status getStatus(int which) {//Get the status at a given index

		return statusOf[which];
	}

	public void setStatus(int which, Status newStatus){//Sets the status at a given index to a given value 

		statusOf[which] = newStatus;
	}

	public int hasSpace(){                        //Returns -1 if all indices are full,
		for(int i = 0; i < statusOf.length; i++){// otherwise returns first empty index
			if (statusOf[i] == Status.EMPTY){
				return i;
			}
		}
		return -1;
	}

	public int hasRead(){           //Returns -1 if no indices are ready for counting, 
		for(int i = 0; i < statusOf.length; i++){ //otherwise returns first ready index
			if (statusOf[i] == Status.READ){
				return i;
			}
		}
		return -1;
	}

	public int[] hasCounted(){      //Returns -1 if no indices are ready for numbering, 
		for(int i = 0; i < statusOf.length; i++){ //otherwise returns first ready index
			if (statusOf[i] == Status.COUNTED){
				return new int[] {i, accessCounts[2] };
			}
		}
		return new int[] {-1, accessCounts[2] };
	}

	public boolean hasNumbered(){ //Returns true if an index is ready for writing
		for(int i = 0; i < statusOf.length; i++){ //, otherwise returns false
			if (statusOf[i] == Status.NUMBERED){
				return true;
			}
		}
		return false;
	}

	public int lowestNumbered(){ //Returns -1 if no indices are ready for writing, otherwise
		if(hasNumbered()){       // returns the index of the String with the lowest line number
			
			int lowestIndex = 0; // Initialize index marker
			int lowestNum = -1; //Initialize lowest line number as -1
			
			for(int i = 0; i < statusOf.length; i++){//Look at the status of each string
				if (statusOf[i] == Status.NUMBERED){ //If a given string is NUMBERED, and ready to write
					
					String first = getString(i);//Get the string at the current index
					lowestIndex = i;//Set the marker to this index
					lowestNum = Integer.parseInt(first.substring(0,first.indexOf(':'))); //Set the lowest number to this number

					for(int j = 1; j < statusOf.length; j++){//Loop through the rest of the indices
						if (statusOf[j] == Status.NUMBERED){ //If this second string is NUMBERED and ready to write
							
							String second = getString(j); //Get the string at this index
							int otherNum = Integer.parseInt(second.substring(0,second.indexOf(':'))); //Get the line number

							if ( otherNum < lowestNum){ //If the line number for the second string is lower than the current lowest
								lowestIndex = j; //Set the lowest index to the current index
								lowestNum = otherNum; //Set the lowest number to the current number
							}
						}
					}
				}
			}
			return lowestIndex; //Return the index of the string with the lowest line number
		}
		else{
			return -1; //If there are no NUMBERED strings ready to write, return -1
		}
	}
}