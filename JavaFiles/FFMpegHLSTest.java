import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class FFMpegHLSTest {
	public static void main(String[] args) throws InterruptedException, IOException{
		//timeMe();
		List<FFMpegHLS> Q = new ArrayList<FFMpegHLS>();
		FFMpegHLS tester = new FFMpegHLS("D:\\ffmpeg\\bin\\Community.mp4", "C:\\Apache22\\htdocs\\ffmpegTest\\test_stream\\Community_MEDIUM", FFMpegHLS.qualityLevel.MEDIUM, "D:\\ffmpeg\\bin\\ffmpeg", "D:\\ffmpeg\\bin\\ffprobe");
		//Q.add(tester);
		FFMpegHLS tester1 = new FFMpegHLS("D:\\ffmpeg\\bin\\Wildlife.wmv", "C:\\Apache22\\htdocs\\ffmpegTest\\test_stream\\Wildlife_MEDIUM", FFMpegHLS.qualityLevel.MEDIUM);
		Q.add(tester1);
//		FFMpegHLS tester2 = new FFMpegHLS("D:\\ffmpeg\\bin\\Wildlife.wmv", null, FFMpegHLS.qualityLevel.HIGH);
//		Q.add(tester2);
		
		//FFMpegHLS.setLoggerLevel(Level.INFO);
		FFMpegHLS.transcode(Q); //TRANSCODE ALL FFmpeg FILES IN PARALLEL, same as FFMpeg.transcode(Q, 0)
		//FFMpegHLS.transcode(Q, 1); //TRANSCODE ALL FFmpeg FILES SEQUENTIALLY
		//FFMpegHLS.transcode(Q, 2); //TRANSCODE ALL FFmpeg FILES, WITH 2 RUNNING IN PARALLEL 
	}
	public static void timeMe(){
		final long start = System.currentTimeMillis();	
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
				long end = System.currentTimeMillis();
				System.out.println("Runtime: "+(end-start)+" milliseconds");
		    }
		});
	}
}
