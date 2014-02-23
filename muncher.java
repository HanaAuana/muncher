import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;


class muncher {

	static final boolean DEBUG = false;
	static boolean RUN = true;

	private static String FILENAME;
	
	static String READ_NAME = "Reader";
	static String COUNT_NAME = "Counter";
	static String NUMBER_NAME = "Numberer";
	static String WRITE_NAME = "Writer";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FILENAME = args[0];
		DataBuffer data = new DataBuffer(4, READ_NAME, COUNT_NAME, NUMBER_NAME ,WRITE_NAME);
		ReaderThread reader = new ReaderThread(data,FILENAME);
		CounterThread counter = new CounterThread(data);
		NumberThread number = new NumberThread(data,FILENAME);
		WriterThread writer = new WriterThread(data);


		Thread[] threads = new Thread[4]; //Set up an array for our threads
		threads[0] = reader;
		threads[1] = counter;
		threads[2] = number;
		threads[3] = writer;
		
		threads[0].setName("Reader"); //Start the threads
		threads[1].setName("Counter");
		threads[2].setName("Numberer");
		threads[3].setName("Writer");

		threads[0].start(); //Start the threads
		threads[1].start();
		threads[2].start();
		threads[3].start();

		for(int i = 0; i < threads.length; i++){ //Once we've created all our threads
			try {
				threads[i].join(); //Wait for them all to finish
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		


	}

}

class ReaderThread extends Thread 
{ 
	DataBuffer data;
	String filename;
	int lineNum;
	Scanner in;

	ReaderThread(DataBuffer data, String filename){
		this.data = data;
		this.filename = filename;
		this.lineNum = 1;
		this.in = new Scanner(System.in);
	}

	public void run(){
		while(muncher.RUN){
			synchronized (this.data){
				int emptyLoc = data.hasSpace();
				if(emptyLoc != -1){ //If there's at least one empty spot in the buffer
					while(emptyLoc != -1){
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

	CounterThread(DataBuffer data){	
		this.data = data;
	}

	public void run(){ 
		while(muncher.RUN){
			synchronized (this.data){
				int readyLoc = data.hasRead();
				if(readyLoc != -1){ //If there's at least one read String in the buffer
					while(readyLoc != -1){
						String toCount = data.getString(readyLoc);
						int charCount = toCount.length();
						data.setString(readyLoc, toCount.concat(" ("+charCount+")"));
						data.setStatus(readyLoc, DataBuffer.Status.COUNTED);
						data.notifyAll();
						readyLoc = data.hasRead();
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


	NumberThread(DataBuffer data, String filename){
		this.data = data;
		this.filename = filename;
	}

	public void run(){
		while(muncher.RUN){
			synchronized (this.data){
				int[] countedArray = data.hasCounted();
				int countedLoc = countedArray[0];
				int countedLocCount = countedArray[1];
				if(countedLoc != -1){ //If there's at least one counted String in the buffer
					while(countedLoc != -1){
						String prefix = countedLocCount+": ";
						data.setString(countedLoc, prefix.concat(data.getString(countedLoc)));
						data.setStatus(countedLoc, DataBuffer.Status.NUMBERED);
						data.notifyAll();
						countedArray = data.hasCounted();
						countedLoc = countedArray[0];
						countedLocCount = countedArray[1];
					}
				}
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

	WriterThread(DataBuffer data){
		this.data = data;
	}
	public void run(){
		while(muncher.RUN){
			
			synchronized (this.data){
				int numberedLoc = data.lowestNumbered();
				if(numberedLoc != -1){ //If there's at least one numbered String in the buffer
					while(numberedLoc != -1){
						String toWrite = data.getString(numberedLoc);
						System.out.println(toWrite);
						data.setString(numberedLoc,"");
						data.setStatus(numberedLoc, DataBuffer.Status.EMPTY);
						data.notifyAll();
						numberedLoc = data.lowestNumbered();
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
	int[] accessCounts;
	
	String READ_NAME;
	String COUNT_NAME;
	String NUMBER_NAME;
	String WRITE_NAME;
	

	enum Status {EMPTY, READ, COUNTED, NUMBERED}; //Enum type to keep track of the status of each string in the buffer

	public DataBuffer(int numThreads, String rName, String cName, String nName, String wName) {
		data = new String[8]; //Initialize arrays
		statusOf = new Status[8];
		for (int i = 0; i < statusOf.length; i++){ //Set status of each index to EMPTY
			statusOf[i] = Status.EMPTY;
		}
		accessCounts = new int[numThreads];//Initalize an array to track the count of times a thread has accessed this buffer
		for(int i = 0; i < accessCounts.length; i++){ //Set counts to start at 1
			accessCounts[i] = 1;
		}
		
		READ_NAME = rName;
		COUNT_NAME = cName;
		NUMBER_NAME = nName;
		WRITE_NAME = wName;
	}

	public String getString(int which) {

		return data[which];
	}

	public void setString(int which, String newString) {
		
		incrementCount();

		data[which] = newString;
	}
	
	public void incrementCount(){
		String curThread = Thread.currentThread().getName();
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

	public int[] hasCounted(){ //Returns -1 if no indices are ready for numbering, otherwise returns first ready index
		for(int i = 0; i < statusOf.length; i++){
			if (statusOf[i] == Status.COUNTED){
				return new int[] {i, accessCounts[2] };
			}
		}
		return new int[] {-1, accessCounts[2] };
	}

	public boolean hasNumbered(){ //Returns true if an index is ready for writing, otherwise returns false
		int lowest = -1;
		for(int i = 0; i < statusOf.length; i++){
			if (statusOf[i] == Status.NUMBERED){
				return true;
			}
		}
		return false;
	}

	public int lowestNumbered(){ //Returns -1 if no indices are ready for writing, otherwise returns the index of the String with the lowest line number
		if(hasNumbered())
		{
			int lowestIndex = 0;
			int lowestNum = -1;
			for(int i = 0; i < statusOf.length; i++){
				if (statusOf[i] == Status.NUMBERED){
					String first = getString(i);
					lowestIndex = i;
					lowestNum = Integer.parseInt(first.substring(0,first.indexOf(':')));

					for(int j = 0; j < statusOf.length; j++){
						if (statusOf[j] == Status.NUMBERED){
							String second = getString(j);
							int otherNum = Integer.parseInt(second.substring(0,second.indexOf(':')));

							if ( otherNum < lowestNum){
								lowestIndex = j;
								lowestNum = otherNum;
							}
						}
					}
				}
			}
			return lowestIndex;
		}
		else
		{
			return -1;
		}
	}
}