import java.io.IOException;
import java.util.logging.Level;

public class distriFFMpegHLSTest {
	public static void main(String[] args) throws InterruptedException, IOException{
		//distriFFMpegHLS tester = new distriFFMpegHLS("D:\\ffmpeg\\bin\\citylifestyle.mp4", "D:\\ffmpeg\\bin\\test", distriFFMpegHLS.qualityLevel.MEDIUM, "D:\\ffmpeg\\bin\\ffmpeg", "D:\\ffmpeg\\bin\\ffprobe");
		distriFFMpegHLS tester = new distriFFMpegHLS("/ffmpeg/ION_communication.mp4", "/ffmpeg/output", distriFFMpegHLS.qualityLevel.MEDIUM, "/ffmpeg/ffmpeg", "/ffmpeg/ffprobe");
		tester.transcode();
	}
}
