import java.io.File;
import java.io.FileNotFoundException;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.regex.Pattern;


public class logs {
	public static void main(String[] args) throws FileNotFoundException {
		Map<String, Map<String, Long>> map = new TreeMap<String, Map<String, Long>>();

    	Scanner scan = new Scanner(new File("D:\\streaming_397726.esw3ccs_S.201606081400-1500-0"));
		Pattern searchString = Pattern.compile("/i/ionstore/per/g\\d*");
		String line;
		while ((line = scan.findWithinHorizon(searchString, 0)) != null) {
			String groupNo = line.split("/")[4]; 								//group no
		
			searchString = Pattern.compile("/pri/\\d*");			
			line = scan.findInLine(searchString);
			String orgNo = line.split("/")[2] + "/" + groupNo;					//organization number
			
			searchString = Pattern.compile("/.*/.*/.*/.*/.*/.*\\.mp4");
			line = scan.findInLine(searchString);
			String videoName = line.split("/")[6];								//filename of video
			
			searchString = Pattern.compile("\\.csmil/.*\\.ts\\?[\\w\\=\\%\\*/\\&]*\\\t\\d*\\\t");
			line = scan.findInLine(searchString);
			long bytes = Integer.parseInt(scan.nextLine().split("\\\t")[1]);	//sc-content-bytes (use [1] for sc-total-bytes 
			
			if(line!=null){
				if(map.get(orgNo) == null){
					Map<String, Long> subMap = new TreeMap<String, Long>();
					subMap.put(videoName, bytes);
					map.put(orgNo, subMap);
				}
				else{
					Map<String, Long> subMap = new TreeMap<String, Long>(map.get(orgNo));
					if(subMap.get(videoName) == null){
						subMap.put(videoName, bytes);
						map.put(orgNo, subMap);
					}
					else{
						long currentConsumption = subMap.get(videoName);
						long newConsumption = currentConsumption + bytes;
						subMap.put(videoName, newConsumption);
						map.put(orgNo, subMap);
					}
				}	
			}
			searchString = Pattern.compile("/i/ionstore/per/g\\d*");
		}
		
		Iterator<Entry<String, Map<String, Long>>> it = map.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, Map<String, Long>> entry = it.next();
			Iterator<Entry<String, Long>> it2 = entry.getValue().entrySet().iterator();
			System.out.println(entry.getKey());
			while(it2.hasNext()){
				Entry<String, Long> entry2 = it2.next();
				System.out.println("\t"+entry2.getKey()+"  "+entry2.getValue());
			}
		}
	}
}
