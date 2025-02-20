package vRouter;

import kademlia.KademliaCommonConfig;
import kademlia.UniformRandomGenerator;
import kademlia.Util;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * Initialization class that performs the bootsrap filling the k-buckets of all initial nodes.<br>
 * In particular every node is added to the routing table of every other node in the network. In the end however the various nodes
 * doesn't have the same k-buckets because when a k-bucket is full a random node in it is deleted.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class StateBuilder implements peersim.core.Control {

	private static final String PAR_PROT = "protocol";

	private String prefix;
	private int vrouterID;

	public StateBuilder(String prefix) {
		this.prefix = prefix;
		vrouterID = Configuration.getPid(this.prefix + "." + PAR_PROT);
	}

	// ______________________________________________________________________________________________
	public final VRouterProtocol get(int i) {
		return ((VRouterProtocol) (Network.get(i)).getProtocol(vrouterID));
	}

	// ______________________________________________________________________________________________
	public static void o(Object o) {
		System.out.println(o);
	}

	// ______________________________________________________________________________________________
	public boolean execute() {
		//random set node ID
		UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);

		for (int i = 0; i < Network.size(); ++i) {
			BigInteger tmp;
			tmp = urg.generate();
			((VRouterProtocol) (Network.get(i).getProtocol(vrouterID))).setNodeId(tmp);
		}

		// Sort the network by nodeId (Ascending)
		Network.sort(new Comparator<Node>() {
			public int compare(Node o1, Node o2) {
				Node n1 = (Node) o1;
				Node n2 = (Node) o2;
				VRouterProtocol p1 = (VRouterProtocol) (n1.getProtocol(vrouterID));
				VRouterProtocol p2 = (VRouterProtocol) (n2.getProtocol(vrouterID));
				return Util.put0(p1.nodeId).compareTo(Util.put0(p2.nodeId));
			}
		});

		int sz = Network.size();

		// for every node take 50 random node and add to k-bucket of it
		for (int i = 0; i < sz; i++) {
			Node iNode = Network.get(i);
			VRouterProtocol iKad = (VRouterProtocol) (iNode.getProtocol(vrouterID));

			for (int k = 0; k < 50; k++) {
				VRouterProtocol jKad = (VRouterProtocol) (Network.get(CommonState.r.nextInt(sz)).getProtocol(vrouterID));
				iKad.routingTable.addNeighbour(jKad.nodeId);
			}
		}

		// add other 50 near nodes
		for (int i = 0; i < sz; i++) {
			Node iNode = Network.get(i);
			VRouterProtocol iKad = (VRouterProtocol) (iNode.getProtocol(vrouterID));

			int start = i;
			if (i > sz - 50) {
				start = sz - 25;
			}
			for (int k = 0; k < 50; k++) {
				start = start++;
				if (start > 0 && start < sz) {
					VRouterProtocol jKad = (VRouterProtocol) (Network.get(start++).getProtocol(vrouterID));
					iKad.routingTable.addNeighbour(jKad.nodeId);
				}
			}
		}

		return false;

	} // end execute()

}
