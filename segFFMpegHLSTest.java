import java.io.IOException;

public class segFFMpegHLSTest {
	public static void main(String[] args) throws InterruptedException, IOException{
		segFFMpegHLS tester = new segFFMpegHLS("D:\\ffmpeg\\bin\\citylifestyle.mp4", "D:\\ffmpeg\\bin\\test", segFFMpegHLS.qualityLevel.MEDIUM, "D:\\ffmpeg\\bin\\ffmpeg", "D:\\ffmpeg\\bin\\ffprobe");
		tester.transcode();
	}
}
