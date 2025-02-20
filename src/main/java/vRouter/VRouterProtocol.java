package vRouter;

/**
 * A Kademlia implementation for PeerSim extending the EDProtocol class.<br>
 * See the Kademlia bibliografy for more information about the protocol.
 *
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

import kademlia.*;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

//__________________________________________________________________________________________________
public class VRouterProtocol implements Cloneable, CDProtocol {

	// VARIABLE PARAMETERS
	final String PAR_K = "K";
	final String PAR_ALPHA = "ALPHA";
	final String PAR_BITS = "BITS";
	final String PAR_EXPECTED_ELEMENTS = "EXPECTED_ELEMENTS";
	final String FALSE_POSITIVE_PROB = "FALSE_POSITIVE_PROB";

	private static String prefix = null;
	private int vRouterID;

	public Queue<VLookupMessage> lookupMessages;
	public Queue<IndexMessage> indexMessages;
	public HashMap<BigInteger,Integer> dataStorage;

	public HashMap<BigInteger,Integer> handledIndex = new HashMap<>();
	public HashMap<BigInteger,Integer> handledQuery = new HashMap<>();

	/**
	 * allow to call the service initializer only once
	 */
	private static boolean _ALREADY_INSTALLED = false;

	/**
	 * nodeId of this pastry node
	 */
	public BigInteger nodeId;

	/**
	 * routing table of this pastry node
	 */
	public RoutingTable routingTable;

	/**
	 * routing table with BloomFilter
	 */
	public BloomFilterRoutingTable bfRoutingTable;


	/**
	 * Replicate this object by returning an identical copy.<br>
	 * It is called by the initializer and do not fill any particular field.
	 *
	 * @return Object
	 */
	public Object clone() {
		VRouterProtocol dolly = new VRouterProtocol(VRouterProtocol.prefix);
		return dolly;
	}

	/**
	 * Used only by the initializer when creating the prototype. Every other instance call CLONE to create the new object.
	 *
	 * @param prefix
	 *            String
	 */
	public VRouterProtocol(String prefix) {
		this.nodeId = null; // empty nodeId
		VRouterProtocol.prefix = prefix;
		_init();
		routingTable = new RoutingTable();
		bfRoutingTable = new BloomFilterRoutingTable();
		dataStorage = new HashMap<>();
		lookupMessages = new LinkedList<>();
		indexMessages = new LinkedList<>();
	}

	/**
	 * This procedure is called only once and allow to inizialize the internal state of KademliaProtocol. Every node shares the
	 * same configuration, so it is sufficient to call this routine once.
	 */
	private void _init() {
		// execute once
		if (_ALREADY_INSTALLED)
			return;

		// read paramaters
		KademliaCommonConfig.K = Configuration.getInt(prefix + "." + PAR_K, KademliaCommonConfig.K);
		KademliaCommonConfig.ALPHA = Configuration.getInt(prefix + "." + PAR_ALPHA, KademliaCommonConfig.ALPHA);
		KademliaCommonConfig.BITS = Configuration.getInt(prefix + "." + PAR_BITS, KademliaCommonConfig.BITS);
		VRouterCommonConfig.EXPECTED_ELEMENTS = Configuration.getInt(prefix + "." + PAR_EXPECTED_ELEMENTS, VRouterCommonConfig.EXPECTED_ELEMENTS);
		VRouterCommonConfig.FALSE_POSITIVE_PROB = Configuration.getDouble(prefix + "." + FALSE_POSITIVE_PROB, VRouterCommonConfig.FALSE_POSITIVE_PROB);

		_ALREADY_INSTALLED = true;
	}

	/**
	 * Search through the network the Node having a specific node Id, by performing binary serach (we concern about the ordering
	 * of the network).
	 * 
	 * @param searchNodeId
	 *            BigInteger
	 * @return Node
	 */
	private Node nodeIdtoNode(BigInteger searchNodeId) {
		if (searchNodeId == null)
			return null;

		int inf = 0;
		int sup = Network.size() - 1;
		int m;

		while (inf <= sup) {
			m = (inf + sup) / 2;

			BigInteger mId = ((VRouterProtocol) Network.get(m).getProtocol(vRouterID)).nodeId;

			if (mId.equals(searchNodeId))
				return Network.get(m);

			if (mId.compareTo(searchNodeId) < 0)
				inf = m + 1;
			else
				sup = m - 1;
		}

		// perform a traditional search for more reliability (maybe the network is not ordered)
		BigInteger mId;
		for (int i = Network.size() - 1; i >= 0; i--) {
			mId = ((VRouterProtocol) Network.get(i).getProtocol(vRouterID)).nodeId;
			if (mId.equals(searchNodeId))
				return Network.get(i);
		}

		return null;
	}

	/**
	 * set the current NodeId
	 * 
	 * @param tmp
	 *            BigInteger
	 */
	public void setNodeId(BigInteger tmp) {
		this.nodeId = tmp;
		this.routingTable.nodeId = tmp;
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		this.vRouterID = protocolID;
		while(!lookupMessages.isEmpty()){
//			VRouterObserver.totalLoookupHop.add(1);
			VLookupMessage msg = lookupMessages.poll();
			if(msg == null) continue;
			routingTable.addNeighbour(msg.from);
			if(msg.dataID.equals(QueryGenerator.DEBUGTARGET)){
				QueryGenerator.queryPath.add(Util.distance(msg.dataID,this.nodeId));
			}
			handleLookupMessage(msg,protocolID);
		}
		while(!indexMessages.isEmpty()){
			IndexMessage msg = indexMessages.poll();
			if(msg == null) continue;
			routingTable.addNeighbour(msg.from);
			if(msg.dataID.equals(QueryGenerator.DEBUGTARGET)){
				QueryGenerator.indexPath.add(Util.distance(msg.dataID,this.nodeId));
			}
			handleIndexMessage(msg,protocolID);
		}
	}

	public void handleLookupMessage(VLookupMessage msg,int protocolID){
		if(VRouterObserver.dataQueryTraffic.get(msg.dataID)!=null){
			int msgs = VRouterObserver.dataQueryTraffic.get(msg.dataID);
			msgs ++;
			VRouterObserver.dataQueryTraffic.put(msg.dataID,msgs);
		}
		//data has been found drop msg
		if(QueryGenerator.queriedData.get(msg.dataID) == 1){
//			VRouterObserver.droppedLookupMessage.add(msg.forwardHops + msg.backwardHops);
			return;
		}

		if(handledQuery.containsKey(msg.dataID)) return;

		//found data locally
		if(dataStorage.containsKey(msg.dataID)){
			QueryGenerator.queriedData.put(msg.dataID,1);
			VRouterObserver.successLookupForwardHop.add(msg.forwardHops);
			VRouterObserver.successLookupBackwardHop.add(msg.backwardHops);
			VRouterObserver.totalSuccessHops.add(msg.forwardHops+msg.backwardHops);
			return;
		}

		List<BigInteger> backwardList = bfRoutingTable.getMatch(msg.dataID);
		if(backwardList != null){
			//remove the closer nodes to target dataID from backward list
			backwardList.removeIf( n -> Util.distance(n,msg.dataID).compareTo(Util.distance(this.nodeId, msg.dataID)) < 0);

			//found data in backward routing table
			if(backwardList.size() > 0){
				//backward to next father nodes
				for (BigInteger n: backwardList) {
					Node nextHop = this.nodeIdtoNode(n);
					VRouterProtocol nextProtocol = (VRouterProtocol) nextHop.getProtocol(protocolID);
					VLookupMessage nextMsg = msg.backward(this.nodeId);
					nextProtocol.lookupMessages.add(nextMsg);
				}
			}else{
//				//如果是后向查找，既没有找到数据，也没有找到下一跳，则该消息是失败消息 。。但是从哪一跳开始算失败，无法判断。。最好方法还是总的 - 成功的
//				if(!msg.direction){
//					VRouterObserver.droppedLookupMessage.add(msg.forwardHops + msg.backwardHops);
//				}
			}
		}


		//only forward message need forward
		if(msg.direction){
			VLookupMessage nextHop = msg.forward(this.nodeId);
			BigInteger[] closerNodes = getCloserNodes(nextHop.dataID);
			//send message to closer node
			for(int i=0;i<closerNodes.length;i++){
				Node targetNode = this.nodeIdtoNode(closerNodes[i]);
				VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);
				targetPro.lookupMessages.add(nextHop);
//			VRouterObserver.totalIndexHop.add(1);
			}
		}
		handledQuery.put(msg.dataID,1);
	}

	public void handleIndexMessage(IndexMessage msg, int protocolID){
		this.vRouterID = protocolID;
		if(VRouterObserver.dataIndexTraffic.get(msg.dataID)!=null){
			int msgs = VRouterObserver.dataIndexTraffic.get(msg.dataID);
			msgs ++;
			VRouterObserver.dataIndexTraffic.put(msg.dataID,msgs);
		}
		ContactWithBloomFilter bfContact = this.bfRoutingTable.get(msg.from);
		if(bfContact == null){
			bfContact = new ContactWithBloomFilter(msg.from);
			this.bfRoutingTable.put(bfContact);
		}
		bfContact.add(msg.dataID);

		if(handledIndex.containsKey(msg.dataID)){
			return;
		}

		IndexMessage relay = msg.relay(this.nodeId);
		BigInteger[] closerNodes = getCloserNodes(msg.dataID);


		//send message to closer node
		for (BigInteger closerNode : closerNodes) {
			Node targetNode = this.nodeIdtoNode(closerNode);
			VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);
			targetPro.indexMessages.add(relay);
			if(msg.dataID.equals(QueryGenerator.DEBUGTARGET)){
				QueryGenerator.indexToPath.add(Util.distance(msg.dataID,targetPro.nodeId));
			}
//			VRouterObserver.totalIndexHop.add(1);
		}
		//local node is the closest;
		if(closerNodes.length ==0){
			VRouterObserver.indexHop.add(msg.hops);
		}
		handledIndex.put(msg.dataID,1);
	}

	public void storeData(BigInteger dataID, int protocolID){
		dataStorage.put(dataID,0);
		IndexMessage msg = new IndexMessage(dataID,this.nodeId);
		BigInteger[] closerNodes = getCloserNodes(msg.dataID);

		//send message to closer node
		for(int i=0;i<closerNodes.length;i++){
			Node targetNode = this.nodeIdtoNode(closerNodes[i]);
			VRouterProtocol targetPro = (VRouterProtocol) targetNode.getProtocol(protocolID);
			targetPro.indexMessages.add(msg);
		}
	}

	public BigInteger[] getCloserNodes(BigInteger targetID){
		BigInteger[] neighbors = routingTable.getNeighbours(targetID);

		int targetFlag = 0;
		for(int i=0;i<neighbors.length;i++){
			//target is farther than local; stop
			if(Util.distance(neighbors[i],targetID).compareTo(Util.distance(this.nodeId,targetID)) >=0 ) break;
			targetFlag++;
		}

		BigInteger[] closerNodes = new BigInteger[Math.min(targetFlag, KademliaCommonConfig.ALPHA)];
		//copy neighbours
		for(int i=0;i<Math.min(targetFlag, KademliaCommonConfig.ALPHA);i++){
			closerNodes[i] = neighbors[i];
		}
		return closerNodes;
	}



	public void sendMessage(VLookupMessage target){
		lookupMessages.add(target);
	}
}
