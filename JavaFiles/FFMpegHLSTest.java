import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class FFMpegHLSTest {
	public static void main(String[] args) throws InterruptedException, IOException{
		FFMpegHLS tester = new FFMpegHLS("D:\\ffmpeg\\bin\\1min.mp4", "D:\\ffmpeg\\bin\\outputs", FFMpegHLS.qualityLevel.LOW, "D:\\ffmpeg\\bin\\ffmpeg", "D:\\ffmpeg\\bin\\ffprobe");
		long startTime = System.nanoTime();
		Process proc = tester.transcode();
		tester.trackProgress(proc);
		long endTime = System.nanoTime();
		System.out.println("Total time: " + (endTime - startTime)/1000000 + "\n");
	}
}
