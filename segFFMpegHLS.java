import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * This program performs distributed video transcoding and segmenting to prepare a video file for HTTP Live Streaming. Makes use of SCP and SSH to communicate with worker nodes. 
 * @author Kanak Kabara
 * @version 1.0
 */
public class segFFMpegHLS {
	/**
	 * Path to the ffmpeg executable 
	 */
	private static String ffmpegPath = "ffmpeg";
	/**
	 * Path to the ffprobe executable
	 */
	private static String ffprobePath = "ffprobe";

	public void setFFProbePath(String ffprobe) {
		segFFMpegHLS.ffprobePath = ffprobe;
	}
	public void setFFMpegPath(String ffmpeg) {
		segFFMpegHLS.ffmpegPath = ffmpeg;
	}
	public String getFFProbePath() {
		return segFFMpegHLS.ffprobePath;
	}
	public String getFFMpegPath() {
		return segFFMpegHLS.ffmpegPath;
	}

	/**
	 * Map to store all the audio meta data gathered from ffprobe
	 */
	private Map<String, String> audioMetaData = new HashMap<String, String>();
	/**
	 * Map to store all the video meta data gathered from ffprobe
	 */
	private Map<String, String> videoMetaData = new HashMap<String, String>();
	
	/**
	 * Specifies the required audio parameters that ffprobe will probe for.
	 */
	private String[] audioReq = { "bit_rate" };
	/**
	 * Specifies the required video parameters that ffprobe will probe for.
	 */
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

	/**
	 * Number of nodes that the transcoding job will be split between. 
	 */
	private int noOfNodes = 2; 
	/**
	 * Array defining the IPs of the nodes performing the transcoding jobs
	 */
	private static String[] IPs = {"172.17.222.240", "172.17.222.215"};
	
	public int getNoOfNodes() {
		return noOfNodes;
	}
	public void setNoOfNodes(int noOfNodes) {
		this.noOfNodes = noOfNodes;
	}
	public String[] getIPs() {
		return IPs;
	}
	public void setIPs(String[] IPs) {
		if(IPs.length == getNoOfNodes())
			this.IPs = IPs;
		else{
			LOGGER.severe("Number of nodes is different from IPs provided!");
			System.exit(1);
		}
	}
	
	/**
	 * A Logger object to log information (INFO, SEVERE etc.)
	 */
	private static Logger LOGGER = Logger.getLogger(segFFMpegHLS.class.getName());

	public static void setLoggerLevel(Level x) {
		segFFMpegHLS.LOGGER.setLevel(x);
	}
	public static Level getLoggerLevel() {
		return segFFMpegHLS.LOGGER.getLevel();
	}

	/**
	 * Path to the source video file.
	 */
	private String sourceFile;
	/**
	 * Folder where all output files will be stored.
	 */
	private String destFolder;
	/**
	 * The complete path to the output files. 
	 */
	private String destFile;
	/**
	 * Name of the output file.
	 */
	private String destFileName;

	public void setSourceFile(String source) {
		this.sourceFile = source;
		if (source != null) {
			Path p = Paths.get(source);	
			try{
				this.destFolder = p.getParent().toString() + "\\" + p.getFileName().toString().split("\\.")[0];
			}catch(Exception e){
				this.destFolder = p.getFileName().toString().split("\\.")[0];
			}
			this.destFileName = p.getFileName().toString().split("\\.")[0];
			setHasAudioStream(hasAudioStream());
		}
	}
	public void setDestFolder(String dest) {
		this.destFolder = dest;
		this.destFile = dest + "/" + destFileName;
		File destF = new File(dest);
		if (!destF.exists()) {
			destF.mkdirs();
			destF.setExecutable(true, false);
			destF.setReadable(true, false);
			destF.setWritable(true, false);
		}
	}
	public String getSourceFile() {
		return this.sourceFile;
	}
	public String getDestFolder() {
		return this.destFolder;
	}

	/**
	 * Boolean value to specify whether a video has an audio stream or not.
	 */
	private boolean hasAudioStream;

	public void setHasAudioStream(boolean hasAudioStream) {
		this.hasAudioStream = hasAudioStream;
	}
	public boolean getHasAudioStream() {
		return this.hasAudioStream;
	}

	/**
	 * Array specifying the various resolutions of the output files.  
	 */
	private int[] RESOLUTIONS = { 360, 432, 540, 720, 1080 };
	/**
	 * Array specifying the pixel indices for each resolution and quality level. 
	 */
	private double[][] pixelIndex = { {0.04, 0.062, 0.089}, {0.04, 0.062, 0.086},
			{0.04, 0.063, 0.083}, {0.04, 0.062, 0.08}, {0.04, 0.061, 0.089}};
	/**
	 * Array specifying the audio bit rate for each resolution and quality level.
	 */
	private int[][] audioBitRates = { {64, 64, 64}, {64, 64, 64}, {64, 96, 96}, {128,
			128, 128}, {256, 256, 256} };
	/**
	 * The base frame rate we want each video file to play at (except if video file has a lower frame rate, then frame rate is not changed).
	 */
	private int baseFrameRate = 25;

	public int[] getResolutions() {
		return RESOLUTIONS;
	}
	public double[][] getPixelIndexArray() {
		return pixelIndex;
	}
	public int[][] getAudioBitRates() {
		return audioBitRates;
	}
	public int getBaseFrameRate() {
		return baseFrameRate;
	}
	public void setResolutions(int[] resolutions) {
		RESOLUTIONS = resolutions;
	}
	public void setPixelIndexArray(double[][] pixelIndexArray) {
		pixelIndex = pixelIndexArray;
	}
	public void setAudioBitRates(int[][] AudioBitRates) {
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
			double[][] store = new double[splitStr.length/3][3];
			int i = 0, j = 0;
			for (String x : splitStr) {
				store[i][j] = Double.parseDouble(x.trim());
				j++;
				if(j==3){
					j = 0; 
					i++;
				}
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
			int[][] store = new int[splitStr.length/3][3];
			int i = 0, j = 0;
			for (String x : splitStr) {
				store[i][j] = Integer.parseInt(x.trim());
				j++;
				if(j==3){
					j = 0; 
					i++;
				}
			}
			audioBitRates = store;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Integer corresponding the quality level (0 = LOW, 1 = MED, 2 = HIGH)
	 */
	private int qualityInt;
	/**
	 * Enumerated quality levels.
	 */
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
			LOGGER.severe("Please use LOW, MEDIUM or HIGH for video quality.");
			break;
		}
	}
	public String getVideoQuality() {
		return segFFMpegHLS.qualityLevel.values()[qualityInt].toString();
	}

	/**
	 * Contructor that defines the source file. The value for Destination Folder, Output Quality, FFMpeg and FFProbe paths are set to the default values (Source folder, "LOW", "ffmpeg" and "ffprobe").
	 * @param sourceFile The file that all the operations will be carried out on.
	 */
	public segFFMpegHLS(String sourceFile) {
		this(sourceFile, null, null, null, null);
	}
	/**
	 * Contructor that defines the source file and the destination folder. The value for Output Quality, FFMpeg and FFProbe paths are set to the default values ("LOW", "ffmpeg" and "ffprobe").
	 * @param sourceFile The file that all the operations will be carried out on.
	 * @param destFolder Folder where all the output files will be placed.
	 */
	public segFFMpegHLS(String sourceFile, String destFolder) {
		this(sourceFile, destFolder, null, null, null);
	}
	/**
	 * Constructor that defines the source file, the destination folder and the quality of the output files. FFMpeg and FFProbe paths are set to the default values ("ffmpeg" and "ffprobe"). 
	 * @param sourceFile The file that all the operations will be carried out on.
	 * @param destFolder Folder where all the output files will be placed.
	 * @param quality Quality of the output videos; has three values - LOW, MEDIUM and HIGH.
	 */
	public segFFMpegHLS(String sourceFile, String destFolder, qualityLevel quality) {
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
	public segFFMpegHLS(String sourceFile, String destFolder, qualityLevel quality, String ffmpegPath, String ffprobePath) {
		setLoggerLevel(Level.SEVERE);
		
		if (ffmpegPath != null)
			setFFMpegPath(ffmpegPath);

		if (ffprobePath != null)
			setFFProbePath(ffprobePath);

		setSourceFile(sourceFile);

		if (destFolder == null) {
			if (sourceFile == null){
				this.destFolder = null;
				this.destFile = null;
			}
			else{
				Path p = Paths.get(sourceFile);
				setDestFolder(p.getParent().toString());
			}
		} else
			setDestFolder(destFolder);

		if (quality == null)
			setVideoQuality(segFFMpegHLS.qualityLevel.LOW);
		else
			setVideoQuality(quality);

		if(RESOLUTIONS.length != pixelIndex.length){
			LOGGER.severe("Pixel Index array is incorrectly defined! Each resolution defined in the RESOLUTIONS array must be accompanied by 3 values in the pixel index array, corresponding to the HIGH, MEDIUM, LOW values.");
			System.exit(1);
		}
		
		if(RESOLUTIONS.length != audioBitRates.length){
			LOGGER.severe("Audio Bitrates array is incorrectly defined! Each resolution defined in the RESOLUTIONS array must be accompanied by 3 values in the audio bitrates array, corresponding to the HIGH, MEDIUM, LOW values.");
			System.exit(1);
		}
		
		if(IPs.length != noOfNodes ){
			LOGGER.severe("Number of IPs specified does not match the number of nodes specified.");
			System.exit(1);
		}
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
			e.printStackTrace();
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
					+ " -v error -show_streams -select_streams a \"" + sourceFile + "\"";
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
		double aspectRatio = Double.parseDouble(getVideoMetaData("width")) / Double.parseDouble(getVideoMetaData("height"));
		double width = aspectRatio * height;
		videoMetaData.put("widthOutput", Double.toString(width));
		int outputBitrate = (int) (width * height * getOutputFrameRate() * pixelIndex[index][qualityInt]);

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
			return (audioBitRates[index][qualityInt] * 1000 < Integer
					.parseInt(getAudioMetaData("bit_rate"))) ? audioBitRates[index][qualityInt] * 1000
					: Integer.parseInt(getAudioMetaData("bit_rate"));
		return 0;
	}

	/**
	 * Method to perform FFProbe on the sourceFile and store the metadata in Maps (audioMetaData and videoMetaData). Fetches the required probe parameters from the audioReq and videoReq arrays that can be altered using the addAudioReq and addVideoReq methods. 
	 * @return True/False depending on whether the process was a success or a failure
	 * @throws IOException Thrown when there is an error in reading the FFProbe output. 
	 * @throws InterruptedException Thrown if the FFProbe thread is interrupted by another thread while it is waiting
	 */
	public boolean getMetadata() throws IOException, InterruptedException {
		System.out.print("Getting Metadata for "+sourceFile);
		String x = "", command, str2;
		Process probe;
		BufferedReader in;

		if (hasAudioStream) {
			for (String str : audioReq)
				x = x + "," + str;
			command = getFFProbePath()
					+ " -v error -select_streams a:0 -show_entries stream=" + x
					+ " -of default=noprint_wrappers=1 " + getSourceFile()+ "";
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
					return false;
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
				+ " -of default=noprint_wrappers=1 " + getSourceFile();
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
				return false;
			}
		}
		in.close();
		String[] AFR = getVideoMetaData("avg_frame_rate").split("/"); // converts the default frame_rate output which is in the form a/b to a decimal value
		int FPS = Integer.parseInt(AFR[0]) / Integer.parseInt(AFR[1]);
		addVideoMetaData("avg_frame_rate", Integer.toString(FPS));
		LOGGER.info("Video: " + videoMetaData);
		System.out.println(" . . . Done!");
		return true;
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
		Scanner scan = new Scanner(proc.getErrorStream()); // all progress information is part of ErrorStream not OutputStream
		double durationInSeconds = Double.parseDouble(videoMetaData
				.get("duration"));
		LOGGER.info("Total video duration: " + durationInSeconds + " seconds.");
		Pattern timePattern = Pattern.compile("(?<=time=)[\\d:.]*"); // look for the line that contains "time=d:d:d.d"
		String time;
		System.out.println("Progress: Start");
		
		while ((time = scan.findWithinHorizon(timePattern, 0)) != null) {
			try{
				String[] splitTime = time.split(":");
				double progress = (Integer.parseInt(splitTime[0]) * 3600
						+ Integer.parseInt(splitTime[1]) * 60 + Double
						.parseDouble(splitTime[2])) / durationInSeconds; //convert the d:d:d.d format to seconds 
				System.out.printf("Progress ["+destFile+"_"+getVideoQuality()+"_..]: %.2f%%%n", progress * 100 * noOfNodes);
			}catch(NumberFormatException e){
				LOGGER.severe(scan.nextLine());
			}
		}
		
		if (proc.exitValue() != 0) { // if process exits without errors, output Progress: Done. Else output the errors generated by the ffmpeg commands
			while (scan.hasNextLine())
				LOGGER.severe(scan.nextLine());
		}
	}
	
	/**
	 * Returns the progress at the current moment in the process. 
	 * @param proc The process representing a transcode job i.e. the process returned by transcode()
	 * @return Progress in percent, or -2 if ungraceful exit, -1 if graceful exit, and 0 if some other error (mostly if ffmpeg is performing post-processing). 
	 */
	private double getProgress(Process proc){
		Scanner scan = new Scanner(proc.getErrorStream()); // all progress information is part of ErrorStream not OutputStream		
		double durationInSeconds = Double.parseDouble(videoMetaData
				.get("duration"));
		Pattern timePattern = Pattern.compile("(?<=time=)[\\d:.]*"); // look for the line that contains "time=d:d:d.d"
		String time = scan.findWithinHorizon(timePattern, 0);
		
		if(time!=null){ //if timePattern is found, it means that ffmpeg command is running successfully; hence track its progress.  
			String[] splitTime = time.split(":");
			double progress = (Integer.parseInt(splitTime[0]) * 3600
					+ Integer.parseInt(splitTime[1]) * 60 + Double
					.parseDouble(splitTime[2])) / durationInSeconds; //convert the d:d:d.d format to seconds 
			return ((progress * 100)>100)? 100 : progress*100 ;
		}else{ // if timePattern is not found, either the ffmpeg process has completed or quit unexpectedly.  
			if(!isRunning(proc)){
				if (proc.exitValue() != 0) { // If progress has quit unexpectedly, output the errors generated by the ffmpeg commands
					while (scan.hasNextLine())
						LOGGER.severe(scan.nextLine());
					return -2; //signifies that process has terminated ungracefully 
				}
				else return -1; //signifies that the process has terminated gracefully
			} 
			else return 0; //signifies that process is still running but the timePattern could not be found (mostly means that the actual transcoding is over, but ffmpeg is post processing the file)
		}
	}

	/**
	 * Method to process a single FFMpeg object by distributing one task on all nodes
	 * @return Boolean value denoting whether transcode was successful or not.  
	 */
	public boolean transcode() {
		try {
			PrintWriter writer = new PrintWriter(destFile+"_playlist.m3u8", "UTF-8");
			writer.println("#EXTM3U");
			
			if(!getMetadata())
				return false;
			
			double chunkTime = Double.parseDouble(getVideoMetaData("duration"))/noOfNodes;
			boolean flag = true;
			List<Chunk> list = new ArrayList<Chunk>();
			
			System.out.print("Sending files . . . ");
			for(int i=0; i<noOfNodes; i++){
				String command = "scp "+sourceFile+" "+IPs[i]+":/ffmpeg";
				Process proc = runCommand(command);
				Scanner scan = new Scanner(proc.getErrorStream()); 				
				while(scan.hasNext())
					LOGGER.severe(scan.nextLine());
				proc.waitFor();
			}
			System.out.println("Done!");
			
			for(int i=0; i<noOfNodes; i++){
				int index = 0;
				String command = "ssh "+IPs[i]+" "+ ffmpegPath + " -report -y -ss "+Double.toString(chunkTime*i)+" -i \"" + sourceFile + "\"";
				for (int x : RESOLUTIONS) {
					if (Integer.parseInt(videoMetaData.get("height")) > x || (Integer.parseInt(videoMetaData.get("height")) == x && getPixelIndex() > 0.1)) {
						int outputAudioBitrate = getOutputAudioBitrate(index);
						int outputVideoBitrate = getOutputVideoBitrate(x, index);
						int outputFrameRate = getOutputFrameRate();
						
						if(outputVideoBitrate != Integer.parseInt(getVideoMetaData("bit_rate"))){
							command = command
									+ " -t "+chunkTime+" -threads 0"
									+ " -map 0 -codec:v libx264 -profile:v high -level:v 4.0 -preset faster -crf 18 -codec:a aac "
									+ " -maxrate " + outputVideoBitrate  
									+ " -bufsize "+ 2 * outputVideoBitrate
									+ " -r " + outputFrameRate
									+ " -vf scale=-2:" + x
									+ " -b:a " + outputAudioBitrate
									+ " -f ssegment -segment_list \"" + destFile +"_"+x+"p_playlist"+i+".m3u8\""
									+ " -segment_list_flags +live"
									+ " -segment_time 10"
									+ " \"" + destFile + "_"+x+"p_"+i+"_%03d.ts\"";
							
							if(flag){
								writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH="+outputVideoBitrate+",RESOLUTION="+ (int) Double.parseDouble(getVideoMetaData("widthOutput"))+"x"+x+",CODECS=avc1.640028,mp4a.40.2\"");
								writer.println("http://172.17.222.240/ffmpegTest/test_stream/"+destFileName+"_"+x+"p_playlist.m3u8");
							}
							index++;
						}else{
							command = command 
								+ " -t "+chunkTime+" -threads 0"
								+ " -map 0 -codec:v libx264 -profile:v high -level:v 4.0 -preset faster -crf 18 -codec:a aac "
								+ " -r " + outputFrameRate
								+ " -b:a " + outputAudioBitrate + " -ac 1"
								+ " -f ssegment -segment_list \"" + destFile + "_"+getVideoMetaData("height")+"p_playlist"+i+".m3u8\""
								+ " -segment_list_flags +live"
								+ " -segment_time 10"
								+ " \"" + destFile + "_"+getVideoMetaData("height")+"p_"+i+"_%03d.ts\"";					
							
							if(flag){
								writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH="+getVideoMetaData("bit_rate")+",RESOLUTION="+ getVideoMetaData("width")+"x"+getVideoMetaData("height")+",CODECS=avc1.640028,mp4a.40.2\"");
								writer.println("http://172.17.222.240/ffmpegTest/test_stream/"+destFileName+"_"+getVideoMetaData("height")+"p_playlist.m3u8");
							}
							index++;
							break;
						}
					}
				}
				flag = false;
				Chunk chunk = new Chunk(IPs[i], command, runCommand(command));
				list.add(chunk);
			}
			generatePreview();
			
			System.out.println("Transcoding . . . ");
			List<Chunk> errors = trackChunks(list);
			if(errors.isEmpty()){ //All chunks completed successfully
				System.out.println("Done!");
				
				System.out.print("Fetching files . . . ");
				for(int i=0; i<noOfNodes; i++){
					String command = "scp "+IPs[i]+":"+destFolder+"/*.* "+destFolder;
					Process proc = runCommand(command);
					Scanner scan = new Scanner(proc.getErrorStream()); 				
					while(scan.hasNext())
						LOGGER.severe(scan.nextLine());
					proc.waitFor();
				}
				System.out.println("Done!");
				
				combinePlaylists();
				
				writer.close();
				return true;
			}else{//repeat command of failed chunks on different IPs
				//TODO Fault tolerance; Decide how to repeat chunks if any failures occur. 
				for(Chunk chunk: errors){
					LOGGER.severe(chunk.IP+" failed!");
				}
				writer.close();
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Method to process a single FFMpeg object by sending one task on node specified
	 * @param IP
	 * @return Boolean value denoting whether transcode was successful or not.  
	 */
	public Chunk transcode(String IP) {
		try {
			PrintWriter writer = new PrintWriter(destFile+"_playlist.m3u8", "UTF-8");
			writer.println("#EXTM3U");
			
			if(!getMetadata())
				return null;
			
			double chunkTime = Double.parseDouble(getVideoMetaData("duration"));
			boolean flag = true;
		
			System.out.print("Sending files to " + IP);
			String command = "scp "+sourceFile+" "+IP+":/ffmpeg";
			Process proc = runCommand(command);
			Scanner scan = new Scanner(proc.getErrorStream()); 				
			while(scan.hasNext())
				LOGGER.severe(scan.nextLine());
			proc.waitFor();
			System.out.println(" . . . Done!");
			
			command = "ssh "+IP+" mkdir -p -m 777 "+destFolder;
			runCommand(command);
			
			int index = 0;
			command = "ssh "+IP+" "+ ffmpegPath + " -report -y -i \"" + sourceFile + "\"";
			for (int x : RESOLUTIONS) {
				if (Integer.parseInt(videoMetaData.get("height")) > x || (Integer.parseInt(videoMetaData.get("height")) == x && getPixelIndex() > 0.1)) {
					int outputAudioBitrate = getOutputAudioBitrate(index);
					int outputVideoBitrate = getOutputVideoBitrate(x, index);
					int outputFrameRate = getOutputFrameRate();
					
					if(outputVideoBitrate != Integer.parseInt(getVideoMetaData("bit_rate"))){
						command = command
								+ " -threads 0"
								+ " -map 0 -codec:v libx264 -profile:v high -level:v 4.0 -preset faster -crf 18 -codec:a aac "
								+ " -maxrate " + outputVideoBitrate  
								+ " -bufsize "+ 2 * outputVideoBitrate
								+ " -r " + outputFrameRate
								+ " -vf scale=-2:" + x
								+ " -b:a " + outputAudioBitrate
								+ " -f ssegment -segment_list \"" + destFile +"_"+x+"p_playlist.m3u8\""
								+ " -segment_list_flags +live"
								+ " -segment_time 10"
								+ " \"" + destFile + "_"+x+"p_%03d.ts\"";
						
						if(flag){
							writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH="+outputVideoBitrate+",RESOLUTION="+ (int) Double.parseDouble(getVideoMetaData("widthOutput"))+"x"+x+",CODECS=avc1.640028,mp4a.40.2\"");
							writer.println("http://172.17.222.240/ffmpegTest/test_stream/"+destFileName+"_"+x+"p_playlist.m3u8");
						}
						index++;
					}else{
						command = command 
							+ " -t "+chunkTime+" -threads 0"
							+ " -map 0 -codec:v libx264 -profile:v high -level:v 4.0 -preset faster -crf 18 -codec:a aac "
							+ " -r " + outputFrameRate
							+ " -b:a " + outputAudioBitrate + " -ac 1"
							+ " -f ssegment -segment_list \"" + destFile + "_"+getVideoMetaData("height")+"p_playlist.m3u8\""
							+ " -segment_list_flags +live"
							+ " -segment_time 10"
							+ " \"" + destFile + "_"+getVideoMetaData("height")+"p_%03d.ts\"";					
						
						if(flag){
							writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH="+getVideoMetaData("bit_rate")+",RESOLUTION="+ getVideoMetaData("width")+"x"+getVideoMetaData("height")+",CODECS=avc1.640028,mp4a.40.2\"");
							writer.println("http://172.17.222.240/ffmpegTest/test_stream/"+destFileName+"_"+getVideoMetaData("height")+"p_playlist.m3u8");
						}
						index++;
						break;
					}
				}
			}
			flag = false;	
			generatePreview();
			writer.close();
			return new Chunk(IP, command, runCommand(command), this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * In the multi-threaded transcoding process, each node in the network will create its own set of segments and .m3u8 playlists. This function combines all the .m3u8 playlists generated by all the nodes.
	 * @param outputFileName Name of the output file without the extension. [movie.mp4 will have outputFileName as movie]
	 * @throws FileNotFoundException Exception thrown if the .m3u8 files are not found
	 */
	private void combinePlaylists() throws FileNotFoundException{
		//TODO Known error that if the input file name contains any regex-characters, the combine process wont work.
		
		System.out.print("Combining playlists . . . ");

		int[] newRes = new int[RESOLUTIONS.length + 1];
		int count = 0;
		for(int y: RESOLUTIONS){
			newRes[count] = y;
			count++;
		}
		newRes[count] = Integer.parseInt(getVideoMetaData("height"));
		setResolutions(newRes);
		
		for (int x : newRes) {
			String regex = destFileName+"_"+x+"p_playlist.*\\.m3u8";
			final Pattern pat = Pattern.compile(regex);

			File[] playlists = new File(destFolder).listFiles(new FileFilter(){
		        public boolean accept(File f) {
		            return pat.matcher(f.getName()).matches();
		        }
		    });
		    	  	
		    PrintWriter writer = new PrintWriter(destFile+"_"+x+"p_playlist.m3u8");
		    writer.println("#EXTM3U");
			writer.println("#EXT-X-VERSION:3");
			writer.println("#EXT-X-MEDIA-SEQUENCE:0");
			writer.println("#EXT-X-ALLOW-CACHE:YES");
			writer.println("#EXT-X-TARGETDURATION:"+ (int) ((Double.parseDouble(getVideoMetaData("duration"))/noOfNodes)+1) );
			
			boolean keep = false;
		    for(File playlist: playlists){
		    	keep = true;
		    	Scanner scan = new Scanner(playlist);
		    	Pattern searchString = Pattern.compile("#EXTINF:"); 
		    	while ((scan.findWithinHorizon(searchString, 0)) != null) {
		    		writer.println("#EXTINF:"+scan.nextLine());
		    		writer.println(scan.nextLine());
				}
		    	scan.close();
		    	playlist.delete();
		    }
		    writer.println("#EXT-X-ENDLIST");
		    writer.close();
		    
		    if(!keep){
		    	new File(destFile+"_"+x+"p_playlist.m3u8").delete();
		    }
		}
		System.out.println("Done!");
	}
	
	/**
	 * Blocking method to track progress of chunks at all the nodes. 
	 * @param A list of Chunk objects, representing the processes running at the worker nodes. 
	 * @return A list containing all the chunks that failed. 
	 */
	public List<Chunk> trackChunks(List<Chunk> list){
		Iterator<Chunk> it = list.iterator();
		List<Chunk> errorChunks = new ArrayList<Chunk>();
		
		while(list.size()>0){
			while(it.hasNext())
			{
				Chunk chunk = it.next();
				double progress = this.getProgress(chunk.proc);
				if(this.isRunning(chunk.proc)){ //if the process is still running..
					if(progress > 0){ //..and the progress is > 0, it means the process is running successfully and we can fetch its progress percentage
						System.out.printf("Progress ["+chunk.IP+"]: %.2f%%%n", progress);
					}
				}
				else{ //process has stopped running
					if(progress == -1) // Successful
						chunk.complete = true;	
					if(progress == -2) // Some error has occured 
						errorChunks.add(chunk);
					
					it.remove(); //remove process from list to make sure its not tracked further, regardless of how it ended
				}
			}
			System.out.println();
			it = list.iterator();
		}
		return errorChunks;
	}

	/**
	 * Method to generate the required poster image and the image strip to be used for the video preview in JWPlayer. Also generates a .vtt file that acts as an index for the video preview.
	 * @throws InterruptedException If the thread processing the image strip is interrupted by another thread while it is waiting, then the wait is ended and an InterruptedException is thrown.
	 * @throws IOException Thrown if there is an error in performing I/O operation on the .vtt file or the image strip
	 */
	public void generatePreview() throws InterruptedException, IOException{
		int nthSecond = 5;
		int noOfFrames = (int) Double.parseDouble(getVideoMetaData("duration")) / nthSecond;
		if(getVideoMetaData("nb_frames").equals("N/A")){
			int frames = Integer.parseInt(getVideoMetaData("avg_frame_rate")) * (int)Double.parseDouble(getVideoMetaData("duration"));
			addVideoMetaData("nb_frames", Integer.toString(frames));
		}
		
		int nthFrame = Integer.parseInt(getVideoMetaData("nb_frames")) / noOfFrames;
		String command = ffmpegPath + " -v error -y -i " + sourceFile
				+ " -threads 0 -frames 1 -q:v 1 -vf select=not(mod(n\\," + nthFrame
				+ ")),scale=-1:120,tile=" + noOfFrames + "x1 " + destFile
				+ "_" + getVideoQuality() + "_preview.jpg";
		Process proc1 = runCommand(command);
		
		command = ffmpegPath + " -ss "
				+ ((int) Double.parseDouble(getVideoMetaData("duration")) / (new Random().nextInt(10)+1) )
				+ " -y -i " + sourceFile
				+ " -threads 0"
				+ " -f mjpeg -vframes 1 "
				+ destFile + "_" + getVideoQuality() + "_poster.jpg";
		Process proc2 = runCommand(command);
		
		PrintWriter writer = new PrintWriter(destFile+"_"+getVideoQuality()+"_preview.vtt", "UTF-8");
		writer.println("WEBVTT");
		int x = 0;
		
		System.out.print("Generating preview and poster . . . ");
		proc1.waitFor();
		proc2.waitFor();
		System.out.println("Done!");
		BufferedImage bimg = ImageIO.read(new File(destFile+"_"+getVideoQuality()+"_preview.jpg"));
		int width = bimg.getWidth() / noOfFrames;
		
		String start = "00:00:00";
		for(int i=nthSecond; i<(int) Double.parseDouble(getVideoMetaData("duration")); i = i + nthSecond){
			String end = String.format("%02d:%02d:%02d",
					TimeUnit.SECONDS.toHours(i),
				    TimeUnit.SECONDS.toMinutes(i),
				    TimeUnit.SECONDS.toSeconds(i) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(i))
				);
			writer.println(start + " --> " + end);
			

			writer.println(destFileName+"_"+getVideoQuality()+"_preview.jpg#xywh="+x+",0,"+width+","+120);
			x = x + width;
			writer.println();
			start = end;
		}
		writer.close();	
	}

	/**
	 * Concatenates the different .ts files to produce one .mp4 file
	 * @throws FileNotFoundException If the .ts files are not found in the destination folder, FileNotFoundException exception is thrown. 
	 */
	public void concat() throws FileNotFoundException{
		System.out.print("Concat . . ");
		
		for (int x : RESOLUTIONS) {
			if (Integer.parseInt(videoMetaData.get("height")) > x
					|| (Integer.parseInt(videoMetaData.get("height")) == x && getPixelIndex() > 0.1)) {
				String regex = destFileName+"_"+x+"p_.*\\.ts";
				
			    final Pattern p = Pattern.compile(regex);
			    File[] files = new File(destFolder).listFiles(new FileFilter(){
			        public boolean accept(File file) {
			            return p.matcher(file.getName()).matches();
			        }
			    });
			    
			    String fileName = destFile+"_"+x+"p_segments.txt";
			    PrintWriter writer = new PrintWriter(fileName);
			    for(File segments: files){
			    	writer.println("file '"+segments.getAbsolutePath()+"'");
			    }
			    writer.close();
			    String command = ffmpegPath + " -v error -f concat -safe 0 -y -i \""+fileName+"\" -c copy -bsf:a aac_adtstoasc \""+destFile+"_"+x+"p.mp4\"";
			    runCommand(command);
			}
		}	
		System.out.println("Done!");
	}

	/**
	 * Method to batch process a list of FFMpegHLS objects. Performs conversions parallely. 
	 * @param list A list consisting of all the FFMpeg objects to be transcoded. 
	 */
	public void transcode(List<segFFMpegHLS> list){
		Iterator<segFFMpegHLS> itr = list.iterator();
		List<Chunk> chunks = new ArrayList<Chunk>();
		
		System.out.println("Transcoding . . . ");
		int i = 0;
		while(itr.hasNext() && i<noOfNodes){
			chunks.add(itr.next().transcode(IPs[i++]));
		}
		
		Iterator<Chunk> chunksItr = chunks.iterator();
		List<Chunk> errorChunks = new ArrayList<Chunk>();
		List<Chunk> completeChunks = new ArrayList<Chunk>(chunks);

		while(chunks.size()>0){
			List<Chunk> newChunks = new ArrayList<Chunk>();
			while(chunksItr.hasNext())
			{
				Chunk chunk = chunksItr.next();
				double progress = this.getProgress(chunk.proc);
				if(this.isRunning(chunk.proc)){ //if the process is still running..
					if(progress > 0){ //..and the progress is > 0, it means the process is running successfully and we can fetch its progress percentage
						System.out.printf("Progress ["+chunk.obj.destFileName+": "+chunk.IP+"]: %.2f%%%n", progress);
					}
				}
				else{ //process has stopped running
					if(progress == -1){ // Successful
						System.out.println(chunk.obj.destFileName+" Complete!");
						chunk.complete = true;
						completeChunks.add(chunk);
						
						System.out.print("Fetching "+chunk.obj.destFileName+" files from " + chunk.IP);
						String command = "scp "+chunk.IP+":"+chunk.obj.destFolder+"/*.* "+chunk.obj.destFolder;
						Process proc = runCommand(command);
						Scanner scan = new Scanner(proc.getInputStream()); 				
						while(scan.hasNext())
							LOGGER.severe(scan.nextLine());
						try {
							proc.waitFor();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						System.out.println(" . . . Done!");
						
						if(itr.hasNext())
							newChunks.add(itr.next().transcode(chunk.IP));
					}
					if(progress == -2) // Some error has occured 
						errorChunks.add(chunk);

					chunksItr.remove(); //remove process from list to make sure its not tracked further, regardless of how it ended
				}
			}
			chunks.addAll(newChunks);
			chunksItr = chunks.iterator();
			System.out.println();
		}
		
		Iterator<Chunk> itr2 = completeChunks.iterator();
		if(errorChunks.isEmpty()){ //All chunks completed successfully
			System.out.println("Done!");
		}else{//repeat command of failed chunks on different IPs
			//TODO Fault tolerance; Decide how to repeat chunks if any failures occur. 
			for(Chunk ch: errorChunks){
				LOGGER.severe(ch.IP+" failed!");
			}
		}
	}
}

class Chunk{
	String IP;
	String command;
	boolean complete;
	Process proc;
	segFFMpegHLS obj; 
	
	/**
	 * Constructor for a Chunk object, representing a chunk of an input video
	 * @param IP The IP where the command is running
	 * @param command Command being run 
	 * @param proc Process representing the commmand
	 */
	public Chunk(String IP, String command, Process proc){
		this(IP, command, proc, null);
	}
	
	public Chunk(String IP, String command, Process proc, segFFMpegHLS obj){
		this.IP = IP;
		this.command = command;
		this.complete = false;
		this.proc = proc;
		this.obj = obj;
	}
}