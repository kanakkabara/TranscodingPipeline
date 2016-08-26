import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class stream {
	public static void main(String[] args) {
		String command = "/ffmpeg/ffmpeg -re -y -i pipe1 /ffmpeg/out.mp4";
		List<String> commandList = Arrays.asList(command.split(" ")); //ProcessBuilder requires a list that contains each parameter of the command separately
		ProcessBuilder ps = new ProcessBuilder(commandList);
		try {
			final Process proc = ps.start();
			new MyThread().run();
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
	        String currLine = null;
	        try {
	            while((currLine = br.readLine()) != null) {
	                System.out.println(currLine);
	            }
	        } catch (IOException e) {
	            System.out.println("Couldn't read the output.");
	            e.printStackTrace();
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class MyThread extends Thread {
	public void run(){
		System.out.println("Thread reading..");
		try {
			FileWriter fw = new FileWriter("pipe1");
			BufferedWriter bw = new BufferedWriter(fw);
			
			BufferedReader inputFile = new BufferedReader(new InputStreamReader(new FileInputStream("Wildlife.wmv ")));
			String currInputLine = null;
	        while((currInputLine = inputFile.readLine()) != null) {
	        	bw.write(currInputLine);
	        }
	        bw.close();	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
