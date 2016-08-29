import java.io.IOException;

public class FFMpegHLSTest {
	public static void main(String[] args) throws InterruptedException, IOException{
		FFMpegHLS tester = new FFMpegHLS(args[0], null, FFMpegHLS.qualityLevel.MEDIUM, null, null);
		tester.trackProgress(tester.transcode());
		tester.concat();
	}
}
