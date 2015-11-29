import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import KoikeLibrary.KoikeLibraryPart.MyBufferedReader;

/**
 * Jリーグのスコアを予測するコンテストの
 * メインプログラム(重回帰分析)
 * @author lanevok
 *
 */
public class Main3 {

    // 重回帰の説明変数の数
    final int PARASIZE = 10;

    List<Player> playerList;

    /**
     * 選手に関する過去データを管理するクラス
     */
    class Player{
	int gameID;
	int teamID;
	String sectionID;
	double attackCBP;
	double defenseCBP;
	double saveCBP;
	double shootCBP;
	double passCBP;
	double crossCBP;
	double dribbleCBP;

	public Player(int _gameID, int _teamID, String _sectionID, double _attackCBP, double _defenseCBP, double _saveCBP,
		      double _shootCBP, double _passCBP, double _crossCBP, double _dribbleCBP) {
	    gameID = _gameID;
	    teamID = _teamID;
	    sectionID = _sectionID;
	    attackCBP = _attackCBP;
	    defenseCBP = _defenseCBP;
	    saveCBP = _saveCBP;
	    shootCBP = _shootCBP;
	    passCBP = _passCBP;
	    crossCBP = _crossCBP;
	    dribbleCBP = _dribbleCBP;
	}
    }

    /**
     * 試合に関するデータクラス
     */
    class Game{
	int score;		// 試合結果の得点
	String sectionID;		// ステージと節 (2014 = 0-* , 2015 1st = 1-* , 2015 2nd = 2-*)
	double[] teamSpec;		// チームの能力値(主にチームでのCBP)

	/**
	 * コンストラクタ
	 * @param _score 試合結果の得点
	 */
	public Game(int _score) {
	    teamSpec = new double[PARASIZE];
	    score = _score;
	    sectionID = "no setting";
	}

	/**
	 * セクションIDの設定
	 * @param _sectionID 設定するセクションID
	 */
	public void setSectionID(String _sectionID){
	    if(sectionID.equals("no setting")){
		sectionID = _sectionID;
	    }
	    else{
		// エラー処理
		if(!sectionID.equals(_sectionID)){
		    System.err.println("Error : difference sectionID");
		}
	    }
	}

	/**
	 * チームの能力値に加算する
	 * @param i 加算する能力番号
	 * @param value 加算する値
	 */
	public void addTeamSpec(int i, double value){
	    teamSpec[i] += value;
	    teamSpec[i] *= 1000;
	    teamSpec[i] = Math.round(teamSpec[i]);
	    teamSpec[i] /= 1000;
	}

	/**
	 * 自チーム強さと敵チーム強さの差を設定する
	 * @param d 強さの差
	 */
	public void setAdvantage(double d) {
	    teamSpec[PARASIZE-1] = d;
	}

	/**
	 * 自チームの強さを設定する
	 * @param d 自チームの強さ
	 */
	public void setMyTeam(double d) {
	    teamSpec[PARASIZE-2] = d;
	}

	/**
	 * 敵チームの強さを設定する
	 * @param d 敵チームの強さ
	 */
	public void setMatchTeam(double d) {
	    teamSpec[PARASIZE-3] = d;
	}
    }

    // 試合IDとチームIDから試合結果の得点を管理
    Map<String, Integer> resultScore;			// key=gameID+"-"+teamID
    // 試合IDと自チームIDから敵チームとの強さの差を管理
    Map<String, Double> advantage; 			// <gameID+"-"+teamID, advantage>
    // チームIDの強さを管理
    Map<String, Double> ad_ranking;			// ranking for advantage
    // 試合IDと自チームIDから敵の強さを管理
    Map<String, Double> matchRank;			// <gteamID, matchTeamRank>
    // メインのデータ管理
    Map<String, Game> aggregateData;		// key=gameID+"-"+teamID
    // 予測する最終試合のチームIDから敵のチームIDを管理
    Map<String, String> finalMatch;
    // aggregateDataをsectionベースに変換する
    Map<String, Map<String, List<Double>>> sectionData;		// <sectionID, <gteamID, teamSpec>>
    // 重回帰の係数群
    double[] coefficient;

    public static void main(String[] args) {
	new Main3().run();
    }

    /**
     * 親メソッド
     */
    private void run() {
	setPlayerList();
	setResultScore();
	doDataAggregate();
	calcMLR();
	predict();
    }

    /**
     * 予測する上位メソッド
     */
    private void predict() {
	setSectionData();
	doPickUpTest5();		// 1~5
    }

    /**
     * CBPをそのまま利用し重回帰でスコアを決定した場合、
     * 各セクションでのRMSE精度分布をテストする
     * CBPをそのまま利用するので、重回帰分析の決定係数が高ければ
     * 精度は良い。
     */
    private void doPickUpTest1() {
	List<Double> rmseList = new ArrayList<>();
	for(String sectionID : sectionData.keySet()){
	    if(sectionID.contains("0-")) continue;
	    Map<String, List<Double>> sectionMap = sectionData.get(sectionID);
	    List<Double> targetScore = new ArrayList<Double>();
	    List<Double> answerScore = new ArrayList<Double>();
	    for(String gteamID : sectionMap.keySet()){
		double s = getScoreByMLR(sectionMap.get(gteamID));
		targetScore.add(s);
		answerScore.add(Double.valueOf(resultScore.get(gteamID)));
	    }
	    double rmse = new RMSE(targetScore, answerScore).getRMSE();
	    System.out.print(rmse);
	    if(sectionID.contains("1-17")){
		System.out.print("\t*");
	    }
	    System.out.println();
	    rmseList.add(rmse);
	    //			System.out.println();
	}
	double sum = 0.0;
	double max = Double.MIN_VALUE;
	double min = Double.MAX_VALUE;
	for(double d : rmseList){
	    sum += d;
	    max = Math.max(d, max);
	    min = Math.min(d, min);
	}
	System.err.println("ave: "+(sum/rmseList.size()));
	System.err.println("max: "+max);
	System.err.println("min: "+min);
    }

    /**
     * 各CBP値を最終試合のCBPとして利用し、重回帰で推定したスコアと
     * 全セクションでのRMSE精度分布をテストする
     * 実際のCBPの値を全セクションで実験するのでバラつきが見れる
     */
    private void doPickUpTest2() {
	for(String targetSectionID : sectionData.keySet()){
	    List<Double> rmseList = new ArrayList<>();
	    if(targetSectionID.contains("0-")) continue;
	    Map<String, List<Double>> targetMap = sectionData.get(targetSectionID);
	    List<Double> targetScore = new ArrayList<Double>();
	    for(String gteamID : targetMap.keySet()){
		double s = getScoreByMLR(targetMap.get(gteamID));
		targetScore.add(s);
	    }
	    // 全セクションで実験
	    for(String evaSectionID : sectionData.keySet()){
		if(evaSectionID.contains("0-")) continue;
		List<Double> answerScore = new ArrayList<Double>();
		Map<String, List<Double>> evaMap = sectionData.get(evaSectionID);
		for(String evaGteamID : evaMap.keySet()){
		    answerScore.add(Double.valueOf(resultScore.get(evaGteamID)));
		}
		double rmse = new RMSE(targetScore, answerScore).getRMSE();
		//				System.out.println(rmse);
		rmseList.add(rmse);
	    }
	    double sum = 0.0;
	    double max = Double.MIN_VALUE;
	    double min = Double.MAX_VALUE;
	    for(double d : rmseList){
		sum += d;
		max = Math.max(d, max);
		min = Math.min(d, min);
	    }
	    System.err.println("ave:\t"+(sum/rmseList.size()));
	    System.err.println("max:\t"+max);
	    System.err.println("min:\t"+min);
	    System.err.println();
	}
    }

    /**
     * チームごとのCBP値を過去の値から平均値を取得し、
     * 最終試合のCBPとして適用、重回帰で推定したスコアで、
     * RMSE精度評価を全セクションでテストする
     */
    private void doPickUpTest3() {
	// CBPの合計値算出
	Map<String, List<Double>> sumSpec = new HashMap<String, List<Double>>();
	Map<String, Integer> count = new HashMap<String, Integer>();
	for(String targetSectionID : sectionData.keySet()){
	    Map<String, List<Double>> targetMap = sectionData.get(targetSectionID);
	    for(String gteamID : targetMap.keySet()){
		String[] split = gteamID.split("-");
		String teamID = split[1];
		if(sumSpec.containsKey(teamID)){
		    List<Double> before = sumSpec.get(teamID);
		    List<Double> after = new ArrayList<Double>();
		    List<Double> now = targetMap.get(gteamID);
		    for(int i=0;i<now.size();i++){
			after.add(before.get(i)+now.get(i));
		    }
		    sumSpec.put(teamID, after);
		    count.put(teamID, count.get(teamID)+1);
		}
		else{
		    sumSpec.put(teamID, targetMap.get(gteamID));
		    count.put(teamID, 1);
		}
	    }
	}
	// 平均値の算出フェーズ
	Map<String, List<Double>> aveSpec = new HashMap<String, List<Double>>();
	for(String gteamID : sumSpec.keySet()){
	    List<Double> before = sumSpec.get(gteamID);
	    List<Double> after = new ArrayList<Double>();
	    for(double d : before){
		after.add(d/count.get(gteamID));
	    }
	    aveSpec.put(gteamID, after);
	}
	// 平均CBP群からスコア値推定
	List<Double> targetScore = new ArrayList<Double>();
	for(String SectionID : sectionData.keySet()){
	    if(SectionID.contains("0-")) continue;
	    Map<String, List<Double>> evaMap = sectionData.get(SectionID);
	    for(String GteamID : evaMap.keySet()){
		String[] split = GteamID.split("-");
		String teamID = split[1];
		double d = getScoreByMLR(aveSpec.get(teamID));
		targetScore.add(d);
		//				System.out.printf("%s=%.1f\t\t",teamID, d);
		System.out.printf("%s\t%.6f\n",teamID, d);
	    }
	    System.out.println();
	    break;
	}
	// スコア値を全セクションでRMSE実験
	List<Double> rmseList = new ArrayList<>();
	for(String evaSectionID : sectionData.keySet()){
	    if(evaSectionID.contains("0-")) continue;
	    List<Double> answerScore = new ArrayList<Double>();
	    Map<String, List<Double>> evaMap = sectionData.get(evaSectionID);
	    for(String evaGteamID : evaMap.keySet()){
		answerScore.add(Double.valueOf(resultScore.get(evaGteamID)));
	    }
	    double rmse = new RMSE(targetScore, answerScore).getRMSE();
	    System.out.print(rmse);
	    if(evaSectionID.contains("1-17")){
		System.out.print("\t*");
	    }
	    System.out.println();
	    rmseList.add(rmse);
	}
	double sum = 0.0;
	double max = Double.MIN_VALUE;
	double min = Double.MAX_VALUE;
	for(double d : rmseList){
	    sum += d;
	    max = Math.max(d, max);
	    min = Math.min(d, min);
	}
	System.err.println("ave: "+(sum/rmseList.size()));
	System.err.println("max: "+max);
	System.err.println("min: "+min);
    }

    /**
     * チームごとのCBP値を過去の値から平均値を取得し、
     * 最終試合のCBPとして適用、対戦相手とのアドバンテージはデータ引用したのち、
     * 重回帰で推定したスコアで、RMSE精度評価を全セクションでテストする
     */
    private void doPickUpTest4() {
	// CBP合計値を出すフェーズ
	Map<String, List<Double>> sumSpec = new HashMap<String, List<Double>>();
	Map<String, Integer> count = new HashMap<String, Integer>();
	for(String targetSectionID : sectionData.keySet()){
	    Map<String, List<Double>> targetMap = sectionData.get(targetSectionID);
	    for(String gteamID : targetMap.keySet()){
		String[] split = gteamID.split("-");
		String teamID = split[1];
		if(sumSpec.containsKey(teamID)){
		    List<Double> before = sumSpec.get(teamID);
		    List<Double> after = new ArrayList<Double>();
		    List<Double> now = targetMap.get(gteamID);
		    for(int i=0;i<now.size();i++){
			after.add(before.get(i)+now.get(i));
		    }
		    sumSpec.put(teamID, after);
		    count.put(teamID, count.get(teamID)+1);
		}
		else{
		    sumSpec.put(teamID, targetMap.get(gteamID));
		    count.put(teamID, 1);
		}
	    }
	}
	// CBPの平均値を出すフェーズ
	Map<String, List<Double>> aveSpec = new HashMap<String, List<Double>>();
	for(String teamID : sumSpec.keySet()){
	    List<Double> before = sumSpec.get(teamID);
	    List<Double> after = new ArrayList<Double>();
	    for(double d : before){
		after.add(d/count.get(teamID));
	    }
	    aveSpec.put(teamID, after);
	}

	// 対戦相手データ等の書きかえ
	List<Double> targetScore = new ArrayList<Double>();
	for(String SectionID : sectionData.keySet()){
	    if(SectionID.contains("0-")) continue;
	    Map<String, List<Double>> evaMap = sectionData.get(SectionID);
	    for(String GteamID : evaMap.keySet()){
		String[] split = GteamID.split("-");
		String teamID = split[1];

		// aveSpecの対戦相手データ書き換え
		List<Double> spec_before = aveSpec.get(teamID);
		List<Double> spec_after = new ArrayList<Double>();
		double adv = advantage.get(GteamID);
		for(int i=0;i<spec_before.size()-3;i++){
		    spec_after.add(spec_before.get(i));
		}
		spec_after.add(matchRank.get(GteamID));
		spec_after.add(getTeamRank(split[1]));
		spec_after.add(adv);
		aveSpec.put(teamID, spec_after);

		// 値の推定
		double d = getScoreByMLR(aveSpec.get(teamID));
		targetScore.add(d);
		System.out.printf("%s\t%.6f\n",teamID, d);
	    }
	    System.out.println();
	    break;
	}

	// 推定スコアで全セクションRMSE実験
	List<Double> rmseList = new ArrayList<>();
	for(String evaSectionID : sectionData.keySet()){
	    if(evaSectionID.contains("0-")) continue;
	    List<Double> answerScore = new ArrayList<Double>();
	    Map<String, List<Double>> evaMap = sectionData.get(evaSectionID);
	    for(String evaGteamID : evaMap.keySet()){
		answerScore.add(Double.valueOf(resultScore.get(evaGteamID)));
	    }
	    double rmse = new RMSE(targetScore, answerScore).getRMSE();
	    System.out.print(rmse);
	    if(evaSectionID.contains("1-17")){
		System.out.print("\t*");
	    }
	    System.out.println();
	    rmseList.add(rmse);
	}
	double sum = 0.0;
	double max = Double.MIN_VALUE;
	double min = Double.MAX_VALUE;
	for(double d : rmseList){
	    sum += d;
	    max = Math.max(d, max);
	    min = Math.min(d, min);
	}
	System.err.println("ave: "+(sum/rmseList.size()));
	System.err.println("max: "+max);
	System.err.println("min: "+min);
    }

    /**
     * チームごとのCBP値を過去の値から平均値を取得し、
     * 最終試合のCBPとして適用、対戦相手は最終試合を利用、
     * 重回帰でスコアを推定する
     */
    private void doPickUpTest5() {
	// CBPの合計値算出フェーズ
	Map<String, List<Double>> sumSpec = new HashMap<String, List<Double>>();
	Map<String, Integer> count = new HashMap<String, Integer>();
	for(String targetSectionID : sectionData.keySet()){
	    Map<String, List<Double>> targetMap = sectionData.get(targetSectionID);
	    for(String gteamID : targetMap.keySet()){
		String[] split = gteamID.split("-");
		String teamID = split[1];
		if(sumSpec.containsKey(teamID)){
		    List<Double> before = sumSpec.get(teamID);
		    List<Double> after = new ArrayList<Double>();
		    List<Double> now = targetMap.get(gteamID);
		    for(int i=0;i<now.size();i++){
			after.add(before.get(i)+now.get(i));
		    }
		    sumSpec.put(teamID, after);
		    count.put(teamID, count.get(teamID)+1);
		}
		else{
		    sumSpec.put(teamID, targetMap.get(gteamID));
		    count.put(teamID, 1);
		}
	    }
	}
	// CBPの平均値算出フェーズ
	Map<String, List<Double>> aveSpec = new HashMap<String, List<Double>>();
	for(String teamID : sumSpec.keySet()){
	    List<Double> before = sumSpec.get(teamID);
	    List<Double> after = new ArrayList<Double>();
	    for(double d : before){
		after.add(d/count.get(teamID));
	    }
	    aveSpec.put(teamID, after);
	}

	// 最終節データの適用と値の推定(RMSE実験はしない、答えがないから)
	setFinalMatch();
	for(String teamID : aveSpec.keySet()){
	    if(teamID.equals("133")||teamID.equals("199")||teamID.equals("30116")) continue;
	    List<Double> spec_before = aveSpec.get(teamID);
	    List<Double> spec_after = new ArrayList<Double>();
	    for(int i=0;i<spec_before.size()-3;i++){
		spec_after.add(spec_before.get(i));
	    }
	    //			System.out.println("team : "+teamID);
	    spec_after.add(getTeamRank(finalMatch.get(teamID)));		// match team
	    spec_after.add(getTeamRank(teamID));		// my team
	    spec_after.add(getAdvantage(teamID, finalMatch.get(teamID)));		// advantage
	    aveSpec.put(teamID, spec_after);
	    double d = getScoreByMLR(aveSpec.get(teamID));
	    System.out.printf("%s\t%.6f\n",teamID, d);
	}
    }

    /**
     * 最終節の試合の対戦相手Map生成
     */
    private void setFinalMatch() {
	finalMatch = new HashMap<String, String>();
	finalMatch.put("120", "127");
	finalMatch.put("127", "120");
	finalMatch.put("122", "136");
	finalMatch.put("136", "122");
	finalMatch.put("270", "269");
	finalMatch.put("269", "270");
	finalMatch.put("86", "238");
	finalMatch.put("238", "86");
	finalMatch.put("124", "30528");
	finalMatch.put("30528", "124");
	finalMatch.put("150", "126");
	finalMatch.put("126", "150");
	finalMatch.put("129", "130");
	finalMatch.put("130", "129");
	finalMatch.put("132", "193");
	finalMatch.put("193", "132");
	finalMatch.put("128", "294");
	finalMatch.put("294", "128");
    }

    /**
     * 重回帰分析でスコア推定
     * @param list 重回帰分析の説明変数の値
     * @return スコア値
     */
    private double getScoreByMLR(List<Double> list) {
	double res = coefficient[0];
	for(int i=1;i<coefficient.length;i++){
	    res += coefficient[i]*list.get(i-1);
	}
	res = Math.max(0, res);
	// 四捨五入
	//		res = Math.round(res);
	return res;
    }

    /**
     * セクションベースのデータに改変する。
     */
    private void setSectionData() {
	sectionData = new HashMap<String, Map<String, List<Double>>>();
	for(String gteamID : aggregateData.keySet()){
	    Game g = aggregateData.get(gteamID);
	    if(sectionData.containsKey(g.sectionID)){
		Map<String, List<Double>> map = sectionData.get(g.sectionID);
		if(map.containsKey(gteamID)){
		    System.err.println("Error : exist gtemaID");
		}
		List<Double> spec = new ArrayList<Double>();
		for(double d : g.teamSpec){
		    spec.add(d);
		}
		map.put(gteamID, spec);
		sectionData.put(g.sectionID, map);
	    }
	    else{
		Map<String, List<Double>> map = new HashMap<String, List<Double>>();
		List<Double> spec = new ArrayList<Double>();
		for(double d : g.teamSpec){
		    spec.add(d);
		}
		map.put(gteamID, spec);
		sectionData.put(g.sectionID, map);
	    }
	}
    }

    /**
     * 重回帰分析で決定係数と係数を得る(ライブラリ依存)
     */
    private void calcMLR() {
	OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
	double[] y = new double[aggregateData.size()];
	double[][] x = new double[aggregateData.size()][];
	int index = 0;
	for(String gteamID : aggregateData.keySet()){
	    Game g = aggregateData.get(gteamID);
	    y[index] = Double.valueOf(g.score);
	    x[index] = new double[PARASIZE];
	    for(int i=0;i<PARASIZE;i++){
		x[index][i] = g.teamSpec[i];
	    }
	    index++;
	}
	regression.newSampleData(y, x);
	System.out.println(regression.calculateAdjustedRSquared());
	System.out.println("精度: "+Math.round(regression.calculateAdjustedRSquared()*100)+"%");
	coefficient = regression.estimateRegressionParameters();
	System.out.println("\t切片: "+Math.round(coefficient[0]*10000)*1.0/10000);
	for(int i=1;i<coefficient.length;i++) {
	    System.out.println("\t第"+i+"係数: "+Math.round(coefficient[i]*10000)*1.0/10000);
	}
    }

    /**
     * メインのデータにCBPを適用するなどデータ加工メソッド
     */
    private void doDataAggregate() {
	aggregateData = new HashMap<String, Game>();
	for(String gteamID : resultScore.keySet()){
	    aggregateData.put(gteamID, new Game(resultScore.get(gteamID)));
	}

	// セクションIDのコピー
	for(Player p : playerList){
	    String gteamID = String.valueOf(p.gameID)+"-"+String.valueOf(p.teamID);
	    Game g = aggregateData.get(gteamID);
	    g.setSectionID(p.sectionID);
	    aggregateData.put(gteamID, g);
	}

	// 攻撃CBP和
	for(Player p : playerList){
	    String gteamID = String.valueOf(p.gameID)+"-"+String.valueOf(p.teamID);
	    Game g = aggregateData.get(gteamID);
	    g.addTeamSpec(0, p.attackCBP);
	    aggregateData.put(gteamID, g);
	}

	// 守備CBP和
	for(Player p : playerList){
	    String gteamID = String.valueOf(p.gameID)+"-"+String.valueOf(p.teamID);
	    Game g = aggregateData.get(gteamID);
	    g.addTeamSpec(1, p.defenseCBP);
	    aggregateData.put(gteamID, g);
	}

	// セーブCBP和
	for(Player p : playerList){
	    String gteamID = String.valueOf(p.gameID)+"-"+String.valueOf(p.teamID);
	    Game g = aggregateData.get(gteamID);
	    g.addTeamSpec(2, p.saveCBP);
	    aggregateData.put(gteamID, g);
	}

	// シュートCBP和
	for(Player p : playerList){
	    String gteamID = String.valueOf(p.gameID)+"-"+String.valueOf(p.teamID);
	    Game g = aggregateData.get(gteamID);
	    g.addTeamSpec(3, p.shootCBP);
	    aggregateData.put(gteamID, g);
	}

	// パスCBP和
	for(Player p : playerList){
	    String gteamID = String.valueOf(p.gameID)+"-"+String.valueOf(p.teamID);
	    Game g = aggregateData.get(gteamID);
	    g.addTeamSpec(4, p.passCBP);
	    aggregateData.put(gteamID, g);
	}

	// クロスCBP和
	for(Player p : playerList){
	    String gteamID = String.valueOf(p.gameID)+"-"+String.valueOf(p.teamID);
	    Game g = aggregateData.get(gteamID);
	    g.addTeamSpec(5, p.crossCBP);
	    aggregateData.put(gteamID, g);
	}

	// ドリブルCBP和
	for(Player p : playerList){
	    String gteamID = String.valueOf(p.gameID)+"-"+String.valueOf(p.teamID);
	    Game g = aggregateData.get(gteamID);
	    g.addTeamSpec(6, p.dribbleCBP);
	    aggregateData.put(gteamID, g);
	}

	// 相手チーム能力
	for(String gTeamID : advantage.keySet()){
	    Game g = aggregateData.get(gTeamID);
	    g.setMatchTeam(matchRank.get(gTeamID));
	    aggregateData.put(gTeamID, g);
	}

	// 自分チーム能力
	for(String gTeamID : advantage.keySet()){
	    Game g = aggregateData.get(gTeamID);
	    String[] split = gTeamID.split("-");
	    g.setMyTeam(getTeamRank(split[1]));
	    aggregateData.put(gTeamID, g);
	}

	// 対戦相手とのアドバンテージ
	for(String gTeamID : advantage.keySet()){
	    Game g = aggregateData.get(gTeamID);
	    g.setAdvantage(advantage.get(gTeamID));
	    aggregateData.put(gTeamID, g);
	}
    }

    /**
     * 対戦相手の結果スコアのMap作成など、下処理。
     */
    private void setResultScore() {
	resultScore = new HashMap<String, Integer>();
	initAdvantage();
	MyBufferedReader br = new MyBufferedReader("Result.txt");
	String line;
	while((line=br.readLine())!=null){
	    String[] split = line.split(",");
	    String hTeamID = split[4];
	    String hgTeamID = split[0]+"-"+hTeamID;
	    String aTeamID = split[7];
	    String agTeamID = split[0]+"-"+aTeamID;
	    int hScore = Integer.valueOf(split[10]);
	    int aScore = Integer.valueOf(split[11]);
	    resultScore.put(hgTeamID, hScore);
	    resultScore.put(agTeamID, aScore);
	    advantage.put(hgTeamID, getAdvantage(hTeamID, aTeamID));
	    advantage.put(agTeamID, getAdvantage(aTeamID, hTeamID));
	    matchRank.put(hgTeamID, getTeamRank(aTeamID));
	    matchRank.put(agTeamID, getTeamRank(hTeamID));
	}
	br.close();
    }

    /**
     * チームごのアドバンテージ値Mapの作成
     */
    private void initAdvantage() {
	advantage = new HashMap<String, Double>();
	ad_ranking = new HashMap<String, Double>();
	matchRank = new HashMap<String, Double>();
	ad_ranking.put("86", 0.67);
	ad_ranking.put("120", 0.73);
	ad_ranking.put("122", 1.0);
	ad_ranking.put("124", 0.67);
	ad_ranking.put("126", 0.0);
	ad_ranking.put("127", 0.47);
	ad_ranking.put("128", 0.9);
	ad_ranking.put("129", 0.84);
	ad_ranking.put("130", 0.49);
	ad_ranking.put("132", 0.65);
	ad_ranking.put("136", 0.35);
	ad_ranking.put("150", 0.31);
	ad_ranking.put("193", 0.29);
	ad_ranking.put("238", 0.24);
	ad_ranking.put("269", 0.53);
	ad_ranking.put("270", 0.69);
	ad_ranking.put("294", 0.22);
	ad_ranking.put("30528", 0.22);
	ad_ranking.put("133", 0.27);
	ad_ranking.put("199", 0.31);
	ad_ranking.put("30116", 0.0);
    }

    /**
     * チームの強さ値を得る
     * @param src チームID
     * @return 強さ値
     */
    private Double getTeamRank(String src) {
	return ad_ranking.get(src);
    }

    /**
     * 自チームが敵チームに対しどれだけアドバンテージがあるか値を得る
     * (自チーム-敵チーム)
     * @param src 自チームID
     * @param dst 敵チームID
     * @return アドバンテージ値
     */
    private Double getAdvantage(String src, String dst) {
	//		System.out.println(src+" --- "+dst);
	double d = ad_ranking.get(src)-ad_ranking.get(dst);
	d *= 1000;
	d = Math.round(d);
	d /= 1000;
	return d;
    }

    /**
     * テキストデータから選出CBPデータの必要な部分のみ読み込む
     */
    private void setPlayerList() {
	MyBufferedReader br = new MyBufferedReader("CBP.txt");
	playerList = new ArrayList<Player>();
	String line;
	while((line=br.readLine())!=null){
	    String[] split = line.split(",");
	    playerList.add(new Player(Integer.valueOf(split[0]),Integer.valueOf(split[6]),Integer.valueOf(split[3])+"-"+Integer.valueOf(split[4]),
				      Double.valueOf(split[22]),
				      Double.valueOf(split[23]),
				      Double.valueOf(split[24]),
				      Double.valueOf(split[18]),
				      Double.valueOf(split[19]),
				      Double.valueOf(split[20]),
				      Double.valueOf(split[21])
				      ));
	}
	br.close();
    }
}
