import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class FFMpegTest {
	public static void main(String[] args) throws InterruptedException, IOException{
		//timeMe();
		List<FFMpeg> Q = new ArrayList<FFMpeg>();
		FFMpeg tester = new FFMpeg("D:\\ffmpeg\\bin\\citylifestyle.mp4", null, FFMpeg.qualityLevel.MEDIUM, "D:\\ffmpeg\\bin\\ffmpeg", "D:\\ffmpeg\\bin\\ffprobe");
		Q.add(tester);
//		FFMpeg tester1 = new FFMpeg("D:\\ffmpeg\\bin\\Wildlife.wmv", null, FFMpeg.qualityLevel.MEDIUM);
//		Q.add(tester1);
//		FFMpeg tester2 = new FFMpeg("D:\\ffmpeg\\bin\\Wildlife.wmv", null, FFMpeg.qualityLevel.HIGH);
//		Q.add(tester2);

//		FFMpeg.setLoggerLevel(Level.INFO);
		FFMpeg.transcode(Q); //TRANSCODE ALL FFmpeg FILES IN PARALLEL, same as FFMpeg.transcode(Q, 0)
//		FFMpeg.transcode(Q, 1); //TRANSCODE ALL FFmpeg FILES SEQUENTIALLY
//		FFMpeg.transcode(Q, 2); //TRANSCODE ALL FFmpeg FILES, WITH 2 RUNNING IN PARALLEL 
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