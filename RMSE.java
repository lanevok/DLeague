import java.util.ArrayList;
import java.util.List;

/**
 * RMSE(Root Mean Squared Error)
 * @author lanevok
 *
 */
public class RMSE {

    List<Double> target;
    List<Double> answer;

    /**
     * RMSEコンストラクタ
     * @param _target 予測対象のDouble型リスト
     * @param _answer 正解のDouble型リスト
     */
    public RMSE(List<Double> _target, List<Double> _answer) {
	target = new ArrayList<Double>();
	answer = new ArrayList<Double>();
	for(Double d : _target){
	    target.add(d);
	}
	for(Double d : _answer){
	    answer.add(d);
	}
    }

    /**
     * RMSEの計算と結果の取得
     * @return RMSEのdouble値
     */
    public double getRMSE() {
	if(target.size()!=answer.size()){
	    System.err.println("RMSE Error : difference size");
	}
	double res = 0.0;
	for(int i=0;i<target.size();i++){
	    double a = answer.get(i);
	    double b = target.get(i);
	    res += Math.pow((a-b),2);
	}
	res /= target.size();
	res = Math.sqrt(res);
	return res;
    }

    public static void main(String[] args) {
	List<Double> t = new ArrayList<Double>();
	List<Double> a = new ArrayList<Double>();
	t.add(0.0);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	t.add(1.5);			a.add(2.0);
	System.out.println(new RMSE(t, a).getRMSE());
    }
}
