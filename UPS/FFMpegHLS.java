import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
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
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * This program performs video transcoding and segmenting to prepare a video file for HTTP Live Streaming.
 * @author Kanak Kabara
 * @version 1.0
 */
public class FFMpegHLS {
	/**
	 * Path to the ffmpeg executable 
	 */
	private static String ffmpegPath = "ffmpeg";
	/**
	 * Path to the ffprobe executable
	 */
	private static String ffprobePath = "ffprobe";

	public void setFFProbePath(String ffprobe) {
		FFMpegHLS.ffprobePath = ffprobe;
	}
	public void setFFMpegPath(String ffmpeg) {
		FFMpegHLS.ffmpegPath = ffmpeg;
	}
	public String getFFProbePath() {
		return FFMpegHLS.ffprobePath;
	}
	public String getFFMpegPath() {
		return FFMpegHLS.ffmpegPath;
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
	 * A Logger object to log information (INFO, SEVERE etc.)
	 */
	private static Logger LOGGER = Logger.getLogger(segFFMpegHLS.class.getName());

	public static void setLoggerLevel(Level x) {
		FFMpegHLS.LOGGER.setLevel(x);
	}
	public static Level getLoggerLevel() {
		return FFMpegHLS.LOGGER.getLevel();
	}

	/**
	 * Path to the source video file.
	 */
	private String sourceFile;
	/**
	 * Folder where all output files will be stored.
	 */
	private String destFolder;

	public void setSourceFile(String source) {
		this.sourceFile = source;
		if (source != null) {
			Path p = Paths.get(source);	
			try{
				this.destFolder = p.getParent().toString() + "\\" + p.getFileName().toString().split("\\.")[0];
			}catch(Exception e){
				this.destFolder = p.getFileName().toString().split("\\.")[0];
			}
			setHasAudioStream(hasAudioStream());
		}
	}
	public void setDestFolder(String dest) {
		this.destFolder = dest + "\\" + Paths.get(sourceFile).getFileName().toString().split("\\.")[0];
		File destF = new File(dest);
		if (!destF.exists()) {
			destF.mkdir();
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
			System.out.println("Please use LOW, MEDIUM or HIGH for video quality.");
			break;
		}
	}
	public String getVideoQuality() {
		return FFMpegHLS.qualityLevel.values()[qualityInt].toString();
	}

	/**
	 * Contructor that defines the source file. The value for Destination Folder, Output Quality, FFMpeg and FFProbe paths are set to the default values (Source folder, "LOW", "ffmpeg" and "ffprobe").
	 * @param sourceFile The file that all the operations will be carried out on.
	 */
	public FFMpegHLS(String sourceFile) {
		this(sourceFile, null, null, null, null);
	}
	/**
	 * Contructor that defines the source file and the destination folder. The value for Output Quality, FFMpeg and FFProbe paths are set to the default values ("LOW", "ffmpeg" and "ffprobe").
	 * @param sourceFile The file that all the operations will be carried out on.
	 * @param destFolder Folder where all the output files will be placed.
	 */
	public FFMpegHLS(String sourceFile, String destFolder) {
		this(sourceFile, destFolder, null, null, null);
	}
	/**
	 * Constructor that defines the source file, the destination folder and the quality of the output files. FFMpeg and FFProbe paths are set to the default values ("ffmpeg" and "ffprobe"). 
	 * @param sourceFile The file that all the operations will be carried out on.
	 * @param destFolder Folder where all the output files will be placed.
	 * @param quality Quality of the output videos; has three values - LOW, MEDIUM and HIGH.
	 */
	public FFMpegHLS(String sourceFile, String destFolder, qualityLevel quality) {
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
	public FFMpegHLS(String sourceFile, String destFolder, qualityLevel quality, String ffmpegPath, String ffprobePath) {
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
			setVideoQuality(FFMpegHLS.qualityLevel.LOW);
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
				return false;
			}
		}
		in.close();
		String[] AFR = getVideoMetaData("avg_frame_rate").split("/"); // converts the default frame_rate output which is in the form a/b to a decimal value
		int FPS = Integer.parseInt(AFR[0]) / Integer.parseInt(AFR[1]);
		addVideoMetaData("avg_frame_rate", Integer.toString(FPS));
		LOGGER.info("Video: " + videoMetaData);
		System.out.println(" . . . Done");
		return true;
	}

	/**
	 * Checks if a process is still running or has terminated. 
	 * @param process Process to be checked.
	 * @return True if process is still running, false otherwise.
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
		long startTime = System.nanoTime();
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
			System.out.printf("Progress ["+getDestFolder()+"_"+getVideoQuality()+"_..]: %.2f%%%n", progress * 100);

		}
		
		if (proc.exitValue() != 0) { // if process exits without errors, output Progress: Done. Else output the errors generated by the ffmpeg commands
			while (scan.hasNextLine())
				LOGGER.severe(scan.nextLine());
		} else{
			long endTime = System.nanoTime();
			System.out.println("Transcode + segment time: \t\t" + (endTime - startTime)/1000000 + "");
			System.out.println("Duration: \t\t\t\t"+durationInSeconds+", bitrate: " + getVideoMetaData("bit_rate"));
			System.out.println("Progress: Done");
		}
	}
	
	/**
	 * Returns the progress at the current moment in the process. 
	 * @param proc The process representing a transcode job i.e. the process returned by transcode()
	 * @return Progress in percent, or -1 if errors occur in ffmpeg process. 
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
			Path p = Paths.get(sourceFile);	
			String outputFileName = p.getFileName().toString().split("\\.")[0];
			PrintWriter writer = new PrintWriter(destFolder+"_"+getVideoQuality()+"_playlist.m3u8", "UTF-8");
			writer.println("#EXTM3U");
			
			if(!getMetadata())
				return null;
			
			int index = 0;
			String command = ffmpegPath + "  -report -y -i \"" + sourceFile + "\"";

			for (int x : RESOLUTIONS) {
				if (Integer.parseInt(videoMetaData.get("height")) > x
						|| (Integer.parseInt(videoMetaData.get("height")) == x && getPixelIndex() > 0.1)) {
					int outputAudioBitrate = getOutputAudioBitrate(index);
					int outputVideoBitrate = getOutputVideoBitrate(x, index);
					int outputFrameRate = getOutputFrameRate();
					
					if(outputVideoBitrate != Integer.parseInt(getVideoMetaData("bit_rate"))){
						command = command
								+ " -map 0 -vcodec libx264 -profile:v high -level:v 4.0 -preset faster -crf 18 -codec:a aac "
								+ " -maxrate " + outputVideoBitrate  
								+ " -bufsize "+ 2 * outputVideoBitrate
								+ " -r " + outputFrameRate
								+ " -vf scale=-2:" + x
								+ " -b:a " + outputAudioBitrate
								+ " -f ssegment -segment_list \"" + destFolder + "_" + getVideoQuality() + "_"+ outputVideoBitrate/1000 + "k_"+x+"p_playlist.m3u8\""
								+ " -segment_list_flags +live"
								+ " -segment_time 15"
								+ " \"" + destFolder + "_" + getVideoQuality() + "_"+ outputVideoBitrate/1000 + "k_"+x+"p_%03d.ts\"";
						
						writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH="+outputVideoBitrate+",RESOLUTION="+ (int) Double.parseDouble(getVideoMetaData("widthOutput"))+"x"+x+",CODECS=avc1.640028,mp4a.40.2\"");
						writer.println("http://172.17.222.240/ffmpegTest/test_stream/"+outputFileName+"_"+getVideoQuality()+"/"+outputFileName+"_" + getVideoQuality() + "_"+ outputVideoBitrate/1000 + "k_"+x+"p_playlist.m3u8");
						index++;
					}else{
						command = command 
							+ " -map 0 -vcodec libx264 -profile:v high -level:v 4.0 -preset faster -crf 18 -codec:a aac "
							+ " -r " + outputFrameRate
							+ " -b:a " + outputAudioBitrate + " -ac 1"
							+ " -f ssegment -segment_list \"" + destFolder + "_" + getVideoQuality() + "_"+ Integer.parseInt(getVideoMetaData("bit_rate"))/1000 + "k_"+getVideoMetaData("height")+"p_playlist.m3u8\""
							+ " -segment_list_flags +live"
							+ " -segment_time 12"
							+ " \"" + destFolder + "_" + getVideoQuality() + "_"+ Integer.parseInt(getVideoMetaData("bit_rate"))/1000 + "k_"+getVideoMetaData("height")+"p_%03d.ts\"";
						
						writer.println("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH="+getVideoMetaData("bit_rate")+",RESOLUTION="+ getVideoMetaData("width")+"x"+getVideoMetaData("height")+",CODECS=avc1.640028,mp4a.40.2\"");
						writer.println("http://172.17.222.240/ffmpegTest/test_stream/"+outputFileName+"_"+getVideoQuality()+"/"+outputFileName+"_" + getVideoQuality() + "_"+ Integer.parseInt(getVideoMetaData("bit_rate"))/1000 + "k_"+getVideoMetaData("height")+"p_playlist.m3u8");
						index++;
						break;
					}
				}
			}		
			generatePreview();
			Process proc = runCommand(command);
			writer.close();
			return proc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Method to batch process a list of FFMpegHLS objects. Performs conversions parallely. 
	 * @param list A list consisting of all the FFMpeg objects to be transcoded. 
	 */
	public static void transcode(List<FFMpegHLS> list){
		Map<FFMpegHLS, Process> procs = new HashMap<FFMpegHLS, Process>();
				
		Iterator<FFMpegHLS> itr = list.iterator();
		while(itr.hasNext()){
			FFMpegHLS obj = itr.next();
			final Process proc = obj.transcode();
			if(proc != null) //if transcode returns null, error has occured. 
				procs.put(obj, proc); //place the transcode process in the map so that we can track its progress.
			else
				itr.remove(); //Failed to run ffprobe command in getMetadata or exception caught in the transcode process; so remove from the list
		}	
		Map<FFMpegHLS, Process> procs2 = new HashMap<FFMpegHLS, Process>(procs);
		final Iterator<Entry<FFMpegHLS, Process>> it2 = procs2.entrySet().iterator();
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	System.out.println("\nKILLING PROCESSES!");
		    	while(it2.hasNext()){
		    		Entry<FFMpegHLS, Process> entry = it2.next();
		    		System.out.println("Killed "+entry.getValue());
					entry.getValue().destroy();
		    	}
		    }
		});
		
		System.out.println("Starting transcoding");
		Iterator<Entry<FFMpegHLS, Process>> it = procs.entrySet().iterator();
		while(procs.size() > 0){
			while(it.hasNext())
			{
				Entry<FFMpegHLS, Process> entry = it.next();
				double progress = entry.getKey().getProgress(entry.getValue());
				if(entry.getKey().isRunning(entry.getValue())){ //if the process is still running..
					if(progress != -1) //..and the progress is != -1, it means the process is running successfully and we can fetch its progress percentage
						System.out.printf("Progress ["+entry.getKey().getDestFolder()+"_"+entry.getKey().getVideoQuality()+"_..]: %.2f%%%n", progress);
				}
				else{
					if(entry.getValue().exitValue() != 0) // if process has terminated unxpectedly, remove it from the list as it is not completed successfully
						list.remove(entry.getKey());
					it.remove(); //remove process from map regardless of exit status (to make sure its not tracked further)
				}
			}
			it = procs.entrySet().iterator();
		}
		
		for(FFMpegHLS obj: list){
			System.out.println(obj.getSourceFile()+"\t --> \t"+obj.getDestFolder()+"_"+obj.getVideoQuality()+"_...k.mp4");
		}
	}

	/**
	 * Method to batch process a list of FFMpeg objects. Performs conversion on 'limit' number of files simultaneously. Limit=0 runs all files in parallel; Limit=1 is a sequential approach. 
	 * @param list A list consisting of all the FFMpeg objects to be transcoded.
	 * @param limit Number of files to be processed in parallel. 
	 */
	public static void transcode(List<FFMpegHLS> list, int limit){
		if(limit == 0) //if limit is 0, process all ffmpeg objects in parallel. 
			transcode(list);
		else if(limit==1){ //if limit is 1, process all ffmpeg objects sequentially. 
			for(FFMpegHLS obj: list){ 
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
				List<FFMpegHLS> Q1 = list.subList(start, end); //create a sublist of size limit
				start = end; 
				end = end + limit;
				FFMpegHLS.transcode(Q1); //transcode sublist of size limit
			}
			if(remainder>0){ // get the remainder of objects left in the list, if any..
				List<FFMpegHLS> Q1 = list.subList(start, list.size());
				FFMpegHLS.transcode(Q1); //..and transcode them in parallel  
			}
		}
	}

	/**
	 * Method to generate the required poster image and the image strip to be used for the video preview in JWPlayer. Also generates a .vtt file that acts as an index for the video preview.
	 * @throws InterruptedException If the thread processing the image strip is interrupted by another thread while it is waiting, then the wait is ended and an InterruptedException is thrown.
	 * @throws IOException Thrown if there is an error in performing I/O operation on the .vtt file or the image strip
	 */
	public void generatePreview() throws InterruptedException, IOException{
		long startTime = System.nanoTime();

		Path p = Paths.get(sourceFile);
		String str = p.getFileName().toString().split("\\.")[0];
		
		int nthSecond = 5;
		int noOfFrames = (int) Double.parseDouble(getVideoMetaData("duration")) / nthSecond;
		if(getVideoMetaData("nb_frames").equals("N/A")){
			int frames = Integer.parseInt(getVideoMetaData("avg_frame_rate")) * (int)Double.parseDouble(getVideoMetaData("duration"));
			addVideoMetaData("nb_frames", Integer.toString(frames));
		}
		
		int nthFrame = Integer.parseInt(getVideoMetaData("nb_frames")) / noOfFrames;
		String command = ffmpegPath + " -v error -y -i \"" + sourceFile
				+ "\" -threads 2 -frames 1 -q:v 1 -vf \"select=not(mod(n\\," + nthFrame
				+ ")),scale=-1:120,tile=" + noOfFrames + "x1\" \"" + destFolder
				+ "_" + getVideoQuality() + "_preview.jpg\"";
		Process proc1 = runCommand(command);
		command = ffmpegPath + " -ss "
				+ ((int) Double.parseDouble(getVideoMetaData("duration")) / 2)
				+ " -y  -i \"" + sourceFile + "\" "
				+ " -threads 2"
				+ " -f mjpeg -vframes 1 \""
				+ destFolder + "_" + getVideoQuality() + "_poster.jpg\" ";
		runCommand(command);

		PrintWriter writer = new PrintWriter(destFolder+"_"+getVideoQuality()+"_preview.vtt", "UTF-8");
		writer.println("WEBVTT");
		int x = 0;
		
		System.out.print("Generating preview and poster . . . ");
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
		long endTime = System.nanoTime();
		System.out.println("Generated preview and poster in \t" + (endTime - startTime)/1000000 + "");

	}
}