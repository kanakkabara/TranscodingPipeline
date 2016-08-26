import java.io.IOException;
import java.util.logging.Level;

public class segFFMpegHLSTest {
	public static void main(String[] args) throws InterruptedException, IOException{
		//segFFMpegHLS tester = new segFFMpegHLS("D:\\ffmpeg\\bin\\citylifestyle.mp4", "D:\\ffmpeg\\bin\\test", segFFMpegHLS.qualityLevel.MEDIUM, "D:\\ffmpeg\\bin\\ffmpeg", "D:\\ffmpeg\\bin\\ffprobe");
		segFFMpegHLS tester = new segFFMpegHLS("/ffmpeg/citylifestyle.mp4", "/ffmpeg/output", segFFMpegHLS.qualityLevel.MEDIUM, "/ffmpeg/ffmpeg", "/ffmpeg/ffprobe");
		tester.transcode();
	}
}
