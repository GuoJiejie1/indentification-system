package vRouter;

import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * This class implements a simple observer of search time and hop average in finding a node in the network
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class TrafficObserver implements Control {

	public TrafficObserver(String prefix) {
	}

	/**
	 * print the statistical snapshot of the current situation
	 * 
	 * @return boolean always false
	 */
	public boolean execute() {
		// get the real network size
		int sz = Network.size();
		for (int i = 0; i < Network.size(); i++)
			if (!Network.get(i).isUp())
				sz--;
		//存储效率评估
		String storeS = String.format("[time=%d]:[N=%d current nodes UP] [D=%f max index h] [%f avg index h] [%d total bf n] [%f avg bf perNode]",
				CommonState.getTime(),
				sz,
				VRouterObserver.indexHop.getMax(),
				VRouterObserver.indexHop.getAverage(),
				VRouterObserver.bloomFilterCount.getN(),
				((float) VRouterObserver.bloomFilterCount.getN() / (float) Network.size())
		);

		String storeC = String.format("%f",
				((float) VRouterObserver.bloomFilterCount.getN() / (float) Network.size())
		);

		String indexC = String.format("%f",
				((float) VRouterObserver.bloomFilterCount.getN() / (float) Network.size())
		);

		//索引构建网络开销评估
		String trafficI = calculateTraffic(VRouterObserver.dataIndexTraffic);
		//查询开销评估
		String trafficQ = calculateTraffic(VRouterObserver.dataQueryTraffic);

		//查询效率评估
		String queryS = String.format("hops:max=%f,avg=%f;",
				VRouterObserver.totalSuccessHops.getMax(),
				VRouterObserver.totalSuccessHops.getAverage()
		);

		//查询效率评估
		String indexS = String.format("index hops:max=%f,avg=%f;",
				VRouterObserver.indexHop.getMax(),
				VRouterObserver.indexHop.getAverage()
		);

		System.err.println(indexS+trafficI);

		return false;
	}

	public String calculateTraffic(HashMap<BigInteger,Integer> trafficMap){
		int max = 0,min = Integer.MAX_VALUE;
		float sum = 0;
		for (Integer i:trafficMap.values()) {
			max = Math.max(max,i);
			min = Math.min(min,i);
			sum += i;
		}
		float avg = sum/trafficMap.size();


		//网络开销评估
		return String.format("traffic:max=%d,avg=%f",
				max,avg);
	}
}
