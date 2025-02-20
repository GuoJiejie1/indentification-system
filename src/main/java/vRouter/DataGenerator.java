package vRouter;

import kademlia.*;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This control generates random search traffic from nodes to random destination node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

// ______________________________________________________________________________________________
public class DataGenerator implements Control {

	// ______________________________________________________________________________________________
	/**
	 * MSPastry Protocol to act
	 */
	private final static String PAR_PROT = "protocol";
	private final static String TURNS = "turns";
	private final static String CYCLES = "cycles";

	/**
	 * MSPastry Protocol ID to act
	 */
	private final int pid;
	private int dataGenerateSimCycle = Integer.MAX_VALUE;
	private int totalSimCycle = Integer.MAX_VALUE;
	UniformRandomGenerator urg;

	private int turns = 0;

	// ______________________________________________________________________________________________
	public DataGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		dataGenerateSimCycle = Configuration.getInt(prefix + "." + TURNS);
		totalSimCycle = Configuration.getInt(prefix + "." + CYCLES);
		urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
	}

	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send 10 random store data message
	 * 
	 * @return boolean
	 */
	public boolean execute() {
		turns++;
		if(turns >= dataGenerateSimCycle + 20){			//wait for all data stored
			if(turns + 20 >= totalSimCycle){     		//finish the last query, no more new
				QueryGenerator.executeFlag = false;
				return false;
			}
			QueryGenerator.executeFlag = true;
			return false;
		}

		if(turns >= dataGenerateSimCycle) return false; //stop store new data
		for(int i=0;i<100;i++){
			Node start;
			do {
				start = Network.get(CommonState.r.nextInt(Network.size()));
			} while ((start == null) || (!start.isUp()));

			VRouterProtocol p = (VRouterProtocol)start.getProtocol(pid);

			BigInteger dataID = urg.generate();
			QueryGenerator.availableData.add(dataID);
			VRouterObserver.dataIndexTraffic.put(dataID,0);
			p.storeData(dataID,pid);
		}
		return false;
	}

	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
