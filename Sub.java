import java.util.HashMap;
import java.util.Map;

import KoikeLibrary.KoikeLibraryPart;
import KoikeLibrary.KoikeLibraryPart.MyBufferedReader;

/**
 * チームのランキング生成
 * (過去全データからチームの勝敗を取得し、
 * 勝ち数-負け数 の値でランキングを作成、値は正規化する)
 * @author lanevok
 *
 */
public class Sub {

    public static void main(String[] args) {
	new Sub().run();
    }

    private void run() {
	Map<Integer, Integer> ranking = new HashMap<Integer, Integer>();
	MyBufferedReader br = new MyBufferedReader("Result.txt");
	String line;
	while((line=br.readLine())!=null){
	    String[] split = line.split(",");
	    int hTeamID = Integer.valueOf(split[4]);
	    int aTeamID = Integer.valueOf(split[7]);
	    int hScore = Integer.valueOf(split[10]);
	    int aScore = Integer.valueOf(split[11]);
	    if(hScore>aScore){
		if(ranking.containsKey(hTeamID)){
		    ranking.put(hTeamID, ranking.get(hTeamID)+1);
		}
		else{
		    ranking.put(hTeamID, 1);
		}
		if(ranking.containsKey(aTeamID)){
		    ranking.put(aTeamID, ranking.get(aTeamID)-1);
		}
		else{
		    ranking.put(aTeamID, -1);
		}
	    }
	    else if(aScore>hScore){
		if(ranking.containsKey(aTeamID)){
		    ranking.put(aTeamID, ranking.get(aTeamID)+1);
		}
		else{
		    ranking.put(aTeamID, 1);
		}
		if(ranking.containsKey(hTeamID)){
		    ranking.put(hTeamID, ranking.get(hTeamID)-1);
		}
		else{
		    ranking.put(hTeamID, -1);
		}
	    }
	}
	ranking = (Map<Integer, Integer>) KoikeLibraryPart.getMapValueSort(ranking);
	for(int teamID : ranking.keySet()){
	    System.out.println(teamID+"\t"+(ranking.get(teamID)+23)*1.0/49);
	}
    }
}
