import java.io.IOException;

public class FFMpegHLSTest {
	public static void main(String[] args) throws InterruptedException, IOException{
//		List<FFMpeg> Q = new ArrayList<FFMpeg>();
		FFMpegHLS tester = new FFMpegHLS("D:\\ffmpeg\\bin\\Sample Videos\\ION_communication (2).mp4", null, FFMpegHLS.qualityLevel.MEDIUM, "D:\\ffmpeg\\bin\\ffmpeg", "D:\\ffmpeg\\bin\\ffprobe");
		tester.trackProgress(tester.transcode());

//		Q.add(tester);
//		FFMpeg tester1 = new FFMpeg("D:\\ffmpeg\\bin\\Wildlife.wmv", null, FFMpeg.qualityLevel.MEDIUM);
//		Q.add(tester1);
//		FFMpeg tester2 = new FFMpeg("D:\\ffmpeg\\bin\\Wildlife.wmv", null, FFMpeg.qualityLevel.HIGH);
//		Q.add(tester2);

//		FFMpeg.transcode(Q); //TRANSCODE ALL FFmpeg FILES IN PARALLEL, same as FFMpeg.transcode(Q, 0)
//		FFMpeg.transcode(Q, 1); //TRANSCODE ALL FFmpeg FILES SEQUENTIALLY
//		FFMpeg.transcode(Q, 2); //TRANSCODE ALL FFmpeg FILES, WITH 2 RUNNING IN PARALLEL 
	}
}
