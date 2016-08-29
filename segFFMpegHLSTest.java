import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class segFFMpegHLSTest {
	public static void main(String[] args) throws InterruptedException, IOException{
		segFFMpegHLS tester = new segFFMpegHLS("/ffmpeg/citylifestyle.mp4", "/ffmpeg/output/citylifestyle", segFFMpegHLS.qualityLevel.MEDIUM, "/ffmpeg/ffmpeg", "/ffmpeg/ffprobe");
		segFFMpegHLS tester1 = new segFFMpegHLS("/ffmpeg/ION_communication(2).mp4", "/ffmpeg/output/IONCOM", segFFMpegHLS.qualityLevel.MEDIUM, "/ffmpeg/ffmpeg", "/ffmpeg/ffprobe");
		segFFMpegHLS tester2 = new segFFMpegHLS("/ffmpeg/HomeworkManagement240315.mp4", "/ffmpeg/output/HW", segFFMpegHLS.qualityLevel.MEDIUM, "/ffmpeg/ffmpeg", "/ffmpeg/ffprobe");
		segFFMpegHLS tester3 = new segFFMpegHLS("/ffmpeg/In-classevaluation160415.mp4", "/ffmpeg/output/inclass", segFFMpegHLS.qualityLevel.MEDIUM, "/ffmpeg/ffmpeg", "/ffmpeg/ffprobe");

		List<segFFMpegHLS> Q = new ArrayList<segFFMpegHLS>();
		Q.add(tester);
		Q.add(tester1);
		Q.add(tester2);
		Q.add(tester3);

		tester.transcode(Q);
		//tester.transcode();
	}
}
