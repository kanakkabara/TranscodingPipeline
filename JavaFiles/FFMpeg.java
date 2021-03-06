import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class FFMpeg {
	private static String ffmpegPath = "ffmpeg";
	private static String ffprobePath = "ffprobe";

	public void setFFProbePath(String ffprobe) {
		FFMpeg.ffprobePath = ffprobe;
	}
	public void setFFMpegPath(String ffmpeg) {
		FFMpeg.ffmpegPath = ffmpeg;
	}
	public String getFFProbePath() {
		return FFMpeg.ffprobePath;
	}
	public String getFFMpegPath() {
		return FFMpeg.ffmpegPath;
	}

	private Map<String, String> audioMetaData = new HashMap<String, String>();
	private Map<String, String> videoMetaData = new HashMap<String, String>();
	
	private String[] audioReq = { "bit_rate" };
	private String[] videoReq = { "height", "width", "avg_frame_rate", "bit_rate", "duration", "nb_frames" };

	public void addAudioReq(String audioParam){
		List<String> listFromArray = Arrays.asList(audioReq);
		List<String> tempList = new ArrayList<String>(listFromArray);
		tempList.add(audioParam);
		String[] tempArray = new String[tempList.size()];
		audioReq = tempList.toArray(tempArray);
	}
	public void addVideoReq(String videoParam){
		List<String> listFromArray = Arrays.asList(videoReq);
		List<String> tempList = new ArrayList<String>(listFromArray);
		tempList.add(videoParam);
		String[] tempArray = new String[tempList.size()];
		videoReq = tempList.toArray(tempArray);
	}
	public String[] getAudioReq(){
		return audioReq;
	}
	public String[] getVideoReq(){
		return videoReq;
	}
		
	public void addAudioMetaData(String key, String value) {
		this.audioMetaData.put(key, value);
	}
	public void addVideoMetaData(String key, String value) {
		this.videoMetaData.put(key, value);
	}
	public String getAudioMetaData(String key) {
		return this.audioMetaData.get(key);
	}
	public String getVideoMetaData(String key) {
		return this.videoMetaData.get(key);
	}

	private static Logger LOGGER = Logger.getLogger(FFMpeg.class.getName());

	public static void setLoggerLevel(Level x) {
		FFMpeg.LOGGER.setLevel(x);
	}
	public static Level getLoggerLevel() {
		return FFMpeg.LOGGER.getLevel();
	}

	private String sourceFile;
	private String destFolder;

	public void setSourceFile(String source) {
		this.sourceFile = source;
		if (source != null) {
			Path p = Paths.get(source);	
			this.destFolder = p.getParent().toString() + "\\" + p.getFileName().toString().split("\\.")[0];
			setHasAudioStream(hasAudioStream());
		}
	}
	public void setDestFolder(String dest) {
		this.destFolder = dest + "\\" + Paths.get(sourceFile).getFileName().toString().split("\\.")[0];
	}
	public String getSourceFile() {
		return this.sourceFile;
	}
	public String getDestFolder() {
		return this.destFolder;
	}

	private boolean hasAudioStream;

	public void setHasAudioStream(boolean hasAudioStream) {
		this.hasAudioStream = hasAudioStream;
	}
	public boolean getHasAudioStream() {
		return this.hasAudioStream;
	}

	private int[] RESOLUTIONS = { 360, 432, 540, 720, 1080 };
	private double[] pixelIndex = { 0.04, 0.062, 0.089, 0.04, 0.062, 0.086,
			0.04, 0.063, 0.083, 0.04, 0.062, 0.08, 0.04, 0.061, 0.089 };
	private int[] audioBitRates = { 64, 64, 64, 64, 64, 64, 64, 96, 96, 128,
			128, 128, 256, 256, 256 };
	private int baseFrameRate = 25;

	public int[] getResolutions() {
		return RESOLUTIONS;
	}
	public double[] getPixelIndexArray() {
		return pixelIndex;
	}
	public int[] getAudioBitRates() {
		return audioBitRates;
	}
	public int getBaseFrameRate() {
		return baseFrameRate;
	}
	public void setResolutions(int[] resolutions) {
		RESOLUTIONS = resolutions;
	}
	public void setPixelIndexArray(double[] pixelIndexArray) {
		pixelIndex = pixelIndexArray;
	}
	public void setAudioBitRates(int[] AudioBitRates) {
		audioBitRates = AudioBitRates;
	}
	public void setBaseFrameRate(int BaseFrameRate) {
		baseFrameRate = BaseFrameRate;
	}
	
	public void setResolutions(String sourceFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(sourceFile));
			String str = br.readLine();
			String[] splitStr = str.split(",");
			int[] store = new int[splitStr.length];
			int i = 0;
			for (String x : splitStr) {
				store[i] = Integer.parseInt(x.trim());
				i++;
			}
			RESOLUTIONS = store;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void setPixelIndexArray(String sourceFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(sourceFile));
			String str = br.readLine();
			String[] splitStr = str.split(",");
			double[] store = new double[splitStr.length];
			int i = 0;
			for (String x : splitStr) {
				store[i] = Double.parseDouble(x.trim());
				i++;
			}
			pixelIndex = store;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void setAudioBitRates(String sourceFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(sourceFile));
			String str = br.readLine();
			String[] splitStr = str.split(",");
			int[] store = new int[splitStr.length];
			int i = 0;
			for (String x : splitStr) {
				store[i] = Integer.parseInt(x.trim());
				i++;
			}
			audioBitRates = store;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int qualityInt;
	public static enum qualityLevel {
		LOW, MEDIUM, HIGH
	}

	public void setVideoQuality(qualityLevel quality) {
		switch (quality) {
		case LOW:
			this.qualityInt = 0;
			break;
		case MEDIUM:
			this.qualityInt = 1;
			break;
		case HIGH:
			this.qualityInt = 2;
			break;
		default:
			System.out.println("Please use LOW, MEDIUM or HIGH for video quality.");
			break;
		}
	}
	public String getVideoQuality() {
		return FFMpeg.qualityLevel.values()[qualityInt].toString();
	}

	/**
	 * Contructor that defines the source file. The value for Destination Folder, Output Quality, FFMpeg and FFProbe paths are set to the default values (Source folder, "LOW", "ffmpeg" and "ffprobe").
	 * @param sourceFile The file that all the operations will be carried out on.
	 */
	public FFMpeg(String sourceFile) {
		this(sourceFile, null, null, null, null);
	}
	/**
	 * Contructor that defines the source file and the destination folder. The value for Output Quality, FFMpeg and FFProbe paths are set to the default values ("LOW", "ffmpeg" and "ffprobe").
	 * @param sourceFile The file that all the operations will be carried out on.
	 * @param destFolder Folder where all the output files will be placed.
	 */
	public FFMpeg(String sourceFile, String destFolder) {
		this(sourceFile, destFolder, null, null, null);
	}
	/**
	 * Constructor that defines the source file, the destination folder and the quality of the output files. FFMpeg and FFProbe paths are set to the default values ("ffmpeg" and "ffprobe"). 
	 * @param sourceFile The file that all the operations will be carried out on.
	 * @param destFolder Folder where all the output files will be placed.
	 * @param quality Quality of the output videos; has three values - LOW, MEDIUM and HIGH.
	 */
	public FFMpeg(String sourceFile, String destFolder, qualityLevel quality) {
		this(sourceFile, destFolder, quality, null, null);
	}
	/**
	 * Constructor that defines all the required parameters for performing a transcoding job. 
	 * @param sourceFile The file that all the operations will be carried out on.
	 * @param destFolder Folder where all the output files will be placed.
	 * @param quality Quality of the output videos; has three values - LOW, MEDIUM and HIGH.
	 * @param ffmpegPath System path to ffmpeg.exe. 
	 * @param ffprobePath System path to ffprobe.exe. 
	 */
	public FFMpeg(String sourceFile, String destFolder, qualityLevel quality, String ffmpegPath, String ffprobePath) {
		setLoggerLevel(Level.SEVERE);
		
		if (ffmpegPath != null)
			setFFMpegPath(ffmpegPath);

		if (ffprobePath != null)
			setFFProbePath(ffprobePath);

		setSourceFile(sourceFile);

		if (destFolder == null) {
			if (sourceFile == null)
				this.destFolder = null;
			else
				this.destFolder = sourceFile.split("\\.")[0];
		} else
			setDestFolder(destFolder);

		if (quality == null)
			setVideoQuality(FFMpeg.qualityLevel.LOW);
		else
			setVideoQuality(quality);
	}

	/**
	 * Method to run a command as used in the command line
	 * @param command String of the command that is to be run 
	 * @return Process representing the command after it is run on the command line
	 */
	private Process runCommand(String command) {
		LOGGER.info("Command: " + command);
		List<String> commandList = Arrays.asList(command.split(" ")); //ProcessBuilder requires a list that contains each parameter of the command separately
		ProcessBuilder ps = new ProcessBuilder(commandList);
		try {
			Process proc = ps.start();
			return proc;
		} catch (Exception e) {
			LOGGER.severe("Unable to start process");
			return null;
		}
	}

	/**
	 * Method to check whether a giving sourceFile has an audio stream or not. 
	 * @return True if given video file has an audio stream, false otherwise.
	 */
	public boolean hasAudioStream() {
		if (sourceFile != null) {
			String command = getFFProbePath()
					+ " -v error -show_streams -select_streams a " + sourceFile;
			Process probe = runCommand(command);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					probe.getInputStream()));
			try {
				return ((command = in.readLine()) != null); //if there is no output from the ffprobe, there is no audio stream in the file
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * Method to calculate the pixel density (bits/pixel) of the source video
	 * @return The pixel density of the source video
	 */
	private double getPixelIndex() {
		return Double.parseDouble(getVideoMetaData("bit_rate"))
				/ (Double.parseDouble(getVideoMetaData("width"))
						* Double.parseDouble(getVideoMetaData("height")) * Double
						.parseDouble(getVideoMetaData("avg_frame_rate")));
	}

	/**
	 * Method to calculate the optimum frame rate for the output video 
	 * @return The optimum frame rate for the output video
	 */
	private int getOutputFrameRate() {
		return (baseFrameRate < Integer
				.parseInt(getVideoMetaData("avg_frame_rate"))) ? baseFrameRate
				: Integer.parseInt(getVideoMetaData("avg_frame_rate"));
	}

	/**
	 * Method to calculate the optimum bitrate for the video stream
	 * @param height Height of the source video to calculate optimum width based on the aspect ratio of the source video 
	 * @param index Index to lookup in the pixelIndex array
	 * @return The optimum bitrate for the video stream
	 */
	private int getOutputVideoBitrate(int height, int index) {
		double aspectRatio = Double.parseDouble(getVideoMetaData("width"))
				/ Double.parseDouble(getVideoMetaData("height"));
		double width = aspectRatio * height;
		int outputBitrate = (int) (width * height * getOutputFrameRate() * pixelIndex[index]);

		int bit_rate = Integer.parseInt(getVideoMetaData("bit_rate"));
		return (bit_rate < outputBitrate) ? bit_rate : outputBitrate;
	}

	/**
	 * Method to calculate the optimum bitrate for the audio stream
	 * @param index Index to lookup in the audioBitRates array
	 * @return The optimum bitrate for the audio stream
	 */
	private int getOutputAudioBitrate(int index) {
		if (hasAudioStream)
			return (audioBitRates[index] * 1000 < Integer
					.parseInt(getAudioMetaData("bit_rate"))) ? audioBitRates[index] * 1000
					: Integer.parseInt(getAudioMetaData("bit_rate"));
		return 0;
	}

	/**
	 * Method to perform FFProbe on the sourceFile and store the metadata in Maps (audioMetaData and videoMetaData). Fetches the required probe parameters from the audioReq and videoReq arrays that can be altered using the addAudioReq and addVideoReq methods. 
	 * @return Integer value corresponding to whether the FFProbe was successful (return 0) or there was an error (return 1)
	 * @throws IOException Thrown when there is an error in reading the FFProbe output. 
	 * @throws InterruptedException Thrown if the FFProbe thread is interrupted by another thread while it is waiting
	 */
	public int getMetadata() throws IOException, InterruptedException {
		System.out.print("Getting Metadata for "+sourceFile);
		String x = "", command, str2;
		Process probe;
		BufferedReader in;

		if (hasAudioStream) {
			for (String str : audioReq)
				x = x + "," + str;
			command = getFFProbePath()
					+ " -v error -select_streams a:0 -show_entries stream=" + x
					+ " -of default=noprint_wrappers=1 \"" + getSourceFile()
					+ "\"";
			probe = runCommand(command);
			probe.waitFor();

			if (probe.exitValue() == 0) {
				in = new BufferedReader(new InputStreamReader(
						probe.getInputStream()));
				while ((str2 = in.readLine()) != null) {
					String[] split = str2.split("="); // output of ffprobe is in the form key=value, so string is split and key and value stored in the map
					addAudioMetaData(split[0], split[1]);
				}
			} else {
				in = new BufferedReader(new InputStreamReader(
						probe.getErrorStream())); // if ffprobe fails, log the error messages
				while ((str2 = in.readLine()) != null){
					LOGGER.severe(str2);
					return 1;
				}
			}
			in.close();
			LOGGER.info("Audio: " + audioMetaData);
		}

		x = "";
		for (String str : videoReq)
			x = x + "," + str;
		command = getFFProbePath()
				+ " -v error -select_streams v:0 -show_entries stream=" + x
				+ " -of default=noprint_wrappers=1 \"" + getSourceFile() + "\"";
		probe = runCommand(command);
		probe.waitFor();

		if (probe.exitValue() == 0) {
			in = new BufferedReader(new InputStreamReader(
					probe.getInputStream()));
			while ((str2 = in.readLine()) != null) {
				String[] split = str2.split("=");
				addVideoMetaData(split[0], split[1]);
			}
		} else {
			in = new BufferedReader(new InputStreamReader(
					probe.getErrorStream()));
			while ((str2 = in.readLine()) != null){
				LOGGER.severe(str2);
				return 1;
			}
		}
		in.close();
		String[] AFR = getVideoMetaData("avg_frame_rate").split("/"); // converts the default frame_rate output which is in the form a/b to a decimal value
		int FPS = Integer.parseInt(AFR[0]) / Integer.parseInt(AFR[1]);
		addVideoMetaData("avg_frame_rate", Integer.toString(FPS));
		LOGGER.info("Video: " + videoMetaData);
		System.out.println(" . . . Done");
		return 0;
	}

	/**
	 * Checks if a process is still running or has terminated. 
	 * @param process Process to be checked.
	 * @return True/False depending on the process
	 */
	private boolean isRunning(Process process) {
	    try {
	        process.exitValue();
	        return false;
	    } catch (Exception e) { //if exitValue() is called when process is running, an exception occurs. hence true is returned. 
	        return true;
	    }
	}
	
	/**
	 * Blocking method for tracking progress of a transcoding process. Displays progress report every second or displays the error messages output by FFMpeg.
	 * @param proc The process representing a transcode job i.e. the process returned by transcode()
	 */
	public void trackProgress(Process proc){
		long startTime = System.currentTimeMillis();
		Scanner scan = new Scanner(proc.getErrorStream()); // all progress information is part of ErrorStream not OutputStream
		double durationInSeconds = Double.parseDouble(videoMetaData
				.get("duration"));
		LOGGER.info("Total video duration: " + durationInSeconds + " seconds.");
		Pattern timePattern = Pattern.compile("(?<=time=)[\\d:.]*"); // look for the line that contains "time=d:d:d.d"
		String time;
		System.out.println("Progress: Start");
		
		while ((time = scan.findWithinHorizon(timePattern, 0)) != null) {
			String[] splitTime = time.split(":");
			double progress = (Integer.parseInt(splitTime[0]) * 3600
					+ Integer.parseInt(splitTime[1]) * 60 + Double
					.parseDouble(splitTime[2])) / durationInSeconds; //convert the d:d:d.d format to seconds 
			System.out.printf("Progress ["+getDestFolder()+"_"+getVideoQuality()+"]: %.2f%%%n", progress * 100);

		}
		
		if (proc.exitValue() != 0) { // if process exits without errors, output Progress: Done. Else output the errors generated by the ffmpeg commands
			while (scan.hasNextLine())
				LOGGER.severe(scan.nextLine());
		} else{
			long endTime = System.currentTimeMillis();
			System.out.println("\nDuration: " + durationInSeconds);
			System.out.println("Progress: Done; Time taken: " + (endTime - startTime) + "ms");
		}
	}
	
	/**
	 * Returns the progress at the current moment in the process. 
	 * @param proc The process representing a transcode job i.e. the process returned by transcode()
	 * @return Progress in percent, or -1 if errors occur in ffmpeg process. 
	 * @throws InterruptedException
	 */
	private double getProgress(Process proc){
		Scanner scan = new Scanner(proc.getErrorStream()); // all progress information is part of ErrorStream not OutputStream		
		double durationInSeconds = Double.parseDouble(videoMetaData
				.get("duration"));
		//LOGGER.info("Total video duration: " + durationInSeconds + " seconds.");
		Pattern timePattern = Pattern.compile("(?<=time=)[\\d:.]*"); // look for the line that contains "time=d:d:d.d"
		String time = scan.findWithinHorizon(timePattern, 0);
		
		if(time!=null){ //if timePattern is found, it means that ffmpeg command is running successfully; hence track its progress.  
			String[] splitTime = time.split(":");
			double progress = (Integer.parseInt(splitTime[0]) * 3600
					+ Integer.parseInt(splitTime[1]) * 60 + Double
					.parseDouble(splitTime[2])) / durationInSeconds; //convert the d:d:d.d format to seconds 
			return ((progress * 100)>100)? 100 : progress*100 ;
		}else{ // if timePattern is not found, either the ffmpeg process has completed or quit unexpectedly.  
			if (proc.exitValue() != 0) { // If progress has quit unexpectedly, output the errors generated by the ffmpeg commands
				while (scan.hasNextLine())
					LOGGER.severe(scan.nextLine());
			}
			return -1; //return -1 to signify that the process has terminated
		}
	}

	/**
	 * Method to process a single FFMpeg object. Returns a Process object that can be passed to the trackProgress(Process) method to get the per-second progress report.
	 * @return Process object of the encoding process, or null if ffprobe failure occurs or if exceptions are caught. 
	 */
	public Process transcode() {
		try {
			if(getMetadata() == 1)
				return null;
			int j = 0;
			String command = ffmpegPath + " -v error -stats -report -y -i \""
					+ sourceFile + "\"";
			for (int x : RESOLUTIONS) {
				if (Integer.parseInt(videoMetaData.get("height")) > x
						|| (Integer.parseInt(videoMetaData.get("height")) == x && getPixelIndex() > 0.1)) {
					int index = qualityInt + (j * 3);
					int outputAudioBitrate = getOutputAudioBitrate(index);
					int outputVideoBitrate = getOutputVideoBitrate(x, index);
					int outputFrameRate = getOutputFrameRate();
					
					command = command
							+ " -vcodec libx264 -preset faster -crf 18" 
							+ " -maxrate " + outputVideoBitrate  
							+ " -bufsize "+ 2 * outputVideoBitrate
							+ " -r " + outputFrameRate
							+ " -vf scale=-2:" + x
							+ " -b:a " + outputAudioBitrate
							+ " \"" + destFolder + "_" + getVideoQuality() + "_" + outputVideoBitrate/1000 + "k.mp4\"";
					j++;
				}
			}
			Process proc = runCommand(command);
			generatePreview();
			return proc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Method to speed up a video to 2x. Returns a Process object that can be passed to the trackProgress(Process) method to get the per-second progress report.
	 * @return Process object of the encoding process, or null if ffprobe failure occurs or if exceptions are caught. 
	 */
	public Process speedUp(){
		try {
			if(getMetadata() == 1)
				return null;
			int j = 0;
			String command = ffmpegPath + " -v error -stats -report -y -i \""
					+ sourceFile + "\"";
			for (int x : RESOLUTIONS) {
				if (Integer.parseInt(videoMetaData.get("height")) > x
						|| (Integer.parseInt(videoMetaData.get("height")) == x && getPixelIndex() > 0.1)) {
					int index = qualityInt + (j * 3);
					int outputAudioBitrate = getOutputAudioBitrate(index);
					int outputVideoBitrate = getOutputVideoBitrate(x, index);
					int outputFrameRate = getOutputFrameRate();
					
					command = command
							+ " -vcodec libx264 -preset faster -crf 18" 
							+ " -maxrate " + outputVideoBitrate  
							+ " -bufsize "+ 2 * outputVideoBitrate
							+ " -r " + outputFrameRate
							+ " -b:a " + outputAudioBitrate + " -ac 1"
							+ " -filter_complex \"[0:v]setpts=0.5*PTS,scale=-2:"+x+"[v];[0:a]atempo=2.0[a]\" -map \"[v]\" -map \"[a]\""
							+ " \"" + destFolder + "_" + getVideoQuality() + "_" + outputVideoBitrate/1000 + "k_2x.mp4\"";
				
					j++;
				}
			}
			//Process proc = null;
			Process proc = runCommand(command);
			generatePreview();
			return proc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**
	 * Method to slow down a video to 0.5x. Returns a Process object that can be passed to the trackProgress(Process) method to get the per-second progress report.
	 * @return Process object of the encoding process, or null if ffprobe failure occurs or if exceptions are caught. 
	 */
	public Process slowDown(){
		try {
			if(getMetadata() == 1)
				return null;
			int j = 0;
			String command = ffmpegPath + " -v error -stats -report -y -i \""
					+ sourceFile + "\"";
			for (int x : RESOLUTIONS) {
				if (Integer.parseInt(videoMetaData.get("height")) > x
						|| (Integer.parseInt(videoMetaData.get("height")) == x && getPixelIndex() > 0.1)) {
					int index = qualityInt + (j * 3);
					int outputAudioBitrate = getOutputAudioBitrate(index);
					int outputVideoBitrate = getOutputVideoBitrate(x, index);
					int outputFrameRate = getOutputFrameRate();
					
					command = command
							+ " -vcodec libx264 -preset faster -crf 18" 
							+ " -maxrate " + outputVideoBitrate  
							+ " -bufsize "+ 2 * outputVideoBitrate
							+ " -r " + outputFrameRate
							+ " -b:a " + outputAudioBitrate + " -ac 1"
							+ " -filter_complex \"[0:v]setpts=2.0*PTS,scale=-2:"+x+"[v];[0:a]atempo=0.5[a]\" -map \"[v]\" -map \"[a]\""
							+ " \"" + destFolder + "_" + getVideoQuality() + "_" + outputVideoBitrate/1000 + "k_2x.mp4\"";
				
					j++;
				}
			}
			//Process proc = null;
			Process proc = runCommand(command);
			generatePreview();
			return proc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Method to batch process a list of FFMpeg objects. Performs conversions parallely. 
	 * @param list A list consisting of all the FFMpeg objects to be transcoded. 
	 * @throws InterruptedException
	 */
	public static void transcode(List<FFMpeg> list){
		System.out.println("Starting transcoding");
		Map<FFMpeg, Process> procs = new HashMap<FFMpeg, Process>();
				
		Iterator<FFMpeg> itr = list.iterator();
		while(itr.hasNext()){
			FFMpeg obj = itr.next();
			final Process proc = obj.transcode();
			if(proc != null) //if transcode returns null, error has occured. 
				procs.put(obj, proc); //place the transcode process in the map so that we can track its progress.
			else
				itr.remove(); //Failed to run ffprobe command in getMetadata or exception caught in the transcode process; so remove from the list
		}	
		Map<FFMpeg, Process> procs2 = new HashMap<FFMpeg, Process>(procs);
		final Iterator<Entry<FFMpeg, Process>> it2 = procs2.entrySet().iterator();
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	System.out.println("\nKILLING PROCESSES!");
		    	while(it2.hasNext()){
		    		Entry<FFMpeg, Process> entry = it2.next();
		    		System.out.println("Killed "+entry.getValue());
					entry.getValue().destroy();
		    	}
		    }
		});
		
		Iterator<Entry<FFMpeg, Process>> it = procs.entrySet().iterator();
		while(procs.size() > 0){
			while(it.hasNext())
			{
				Entry<FFMpeg, Process> entry = it.next();
				double progress = entry.getKey().getProgress(entry.getValue());
				if(entry.getKey().isRunning(entry.getValue())){ //if the process is still running..
					if(progress != -1) //..and the progress is != -1, it means the process is running successfully and we can fetch its progress percentage
						System.out.printf("Progress ["+entry.getKey().getDestFolder()+"_"+entry.getKey().getVideoQuality()+"]: %.2f%%%n", progress);
				}
				else{
					if(entry.getValue().exitValue() != 0) // if process has terminated unxpectedly, remove it from the list as it is not completed successfully
						list.remove(entry.getKey());
					it.remove(); //remove process from map regardless of exit status (to make sure its not tracked further)
				}
			}
			it = procs.entrySet().iterator();
		}
		
		for(FFMpeg obj: list){
			System.out.println("URL: "+obj.getURL());
			System.out.println(obj.getSourceFile()+"\t --> \t"+obj.getDestFolder()+"_"+obj.getVideoQuality()+"_...k.mp4");
		}
	}

	/**
	 * Method to batch process a list of FFMpeg objects. Performs conversion on 'limit' number of files simultaneously. Limit=0 runs all files in parallel; Limit=1 is a sequential approach. 
	 * @param list A list consisting of all the FFMpeg objects to be transcoded.
	 * @param limit Number of files to be processed in parallel. 
	 */
	public static void transcode(List<FFMpeg> list, int limit){
		if(limit == 0) //if limit is 0, process all ffmpeg objects in parallel. 
			transcode(list);
		else if(limit==1){ //if limit is 1, process all ffmpeg objects sequentially. 
			for(FFMpeg obj: list){ 
				Process proc = obj.transcode();
				if(proc != null)
					obj.trackProgress(proc);
			}
		}
		else{

			int subLists = list.size() / limit;
			int remainder = list.size() % limit;
			
			int start = 0;
			int end = limit;
			for(int i=0; i<subLists; i++){
				List<FFMpeg> Q1 = list.subList(start, end); //create a sublist of size limit
				start = end; 
				end = end + limit;
				FFMpeg.transcode(Q1); //transcode sublist of size limit
			}
			if(remainder>0){ // get the remainder of objects left in the list, if any..
				List<FFMpeg> Q1 = list.subList(start, list.size());
				FFMpeg.transcode(Q1); //..and transcode them in parallel  
			}
		}
	}
	
	/**
	 * Method to generate the portion of URL which specifies the different variant bitrates available.  
	 * @return URL containing multiple bitrates definition 
	 */
	public String getURL(){
		Path p = Paths.get(sourceFile);
		String URL = "/"+ p.getFileName().toString().split("\\.")[0]+"_"+getVideoQuality()+"_,";
		int j = 0;
		for (int x : RESOLUTIONS) {
			if (Integer.parseInt(videoMetaData.get("height")) > x
					|| (Integer.parseInt(videoMetaData.get("height")) == x && getPixelIndex() > 0.1)) {
				int index = qualityInt + (j * 3);
				int outputVideoBitrate = getOutputVideoBitrate(x, index);
				URL = URL + outputVideoBitrate/1000+"k,";
				
				j++;
			}
		}
		URL = URL + ".mp4.csmil/master.m3u8";
		return URL;
	}
	
	/**
	 * Method to generate the required poster image and the image strip to be used for the video preview in JWPlayer. Also generates a .vtt file that acts as an index for the video preview.
	 * @throws FileNotFoundException Exception thrown if the .vtt file cannot be written to if it exists or cannot be created if it doesn't exist.
	 * @throws UnsupportedEncodingException Exception thrown if the given file encoding format is not supported. 
	 */
	public void generatePreview() throws InterruptedException, IOException{
		long startTime = System.currentTimeMillis();
		
		Path p = Paths.get(sourceFile);
		String str = p.getFileName().toString().split("\\.")[0];
		
		int nthSecond = 2;
		int noOfFrames = (int) Double.parseDouble(getVideoMetaData("duration")) / nthSecond;
		int nthFrame = Integer.parseInt(getVideoMetaData("nb_frames")) / noOfFrames;
		String command = ffmpegPath + " -y -i \"" + sourceFile + "\" -frames 1 -q:v 1 -vf \"select=not(mod(n\\,"+nthFrame+")),scale=-1:120,tile="+noOfFrames+"x1\" \""+destFolder+"_"+getVideoQuality()+"_preview.jpg\"";
		Process proc1 = runCommand(command);
		command = ffmpegPath + " -ss " + ((int) Double.parseDouble(getVideoMetaData("duration")) / 2) + " -y  -i \""+sourceFile+"\" -f mjpeg -vframes 1 \""+destFolder+"_"+getVideoQuality()+"_poster.jpg\" ";
		runCommand(command);

		PrintWriter writer = new PrintWriter(destFolder+"_"+getVideoQuality()+"_preview.vtt", "UTF-8");
		writer.println("WEBVTT");
		int x = 0;
		
		System.out.print("Generating preview and poster...");
		proc1.waitFor();
		System.out.println("Done!");
		BufferedImage bimg = ImageIO.read(new File(destFolder+"_"+getVideoQuality()+"_preview.jpg"));
		int width = bimg.getWidth() / noOfFrames;
		
		String start = "00:00:00";
		for(int i=nthSecond; i<(int) Double.parseDouble(getVideoMetaData("duration")); i = i + nthSecond){
			String end = String.format("%02d:%02d:%02d",
					TimeUnit.SECONDS.toHours(i),
				    TimeUnit.SECONDS.toMinutes(i),
				    TimeUnit.SECONDS.toSeconds(i) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(i))
				);
			writer.println(start + " --> " + end);
			
			x = x + width;
			writer.println(str+"_"+getVideoQuality()+"_preview.jpg#xywh="+x+",0,"+width+","+120);
			writer.println();
			start = end;
		}
		writer.close();	
		long endTime = System.currentTimeMillis();
		System.out.println("Generated preview and poster in " + (endTime - startTime) + "ms");
	}
}