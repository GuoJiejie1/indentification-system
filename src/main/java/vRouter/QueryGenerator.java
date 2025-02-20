package vRouter;

import kademlia.KademliaCommonConfig;
import kademlia.UniformRandomGenerator;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class QueryGenerator implements Control {
    public static boolean executeFlag = false;
    public static Queue<BigInteger> availableData = new LinkedList<>();
    public static HashMap<BigInteger,Integer> queriedData = new HashMap<>();

    public static ArrayList<BigInteger> indexPath = new ArrayList<>();
    public static ArrayList<BigInteger> indexToPath = new ArrayList<>();
    public static ArrayList<BigInteger> queryPath = new ArrayList<>();
    public static final BigInteger DEBUGTARGET = new BigInteger("1114055198376486755617044701041245316474664586947");
    // ______________________________________________________________________________________________
    /**
     * MSPastry Protocol to act
     */
    private final static String PAR_PROT = "protocol";

    /**
     * MSPastry Protocol ID to act
     */
    private final int pid;
    UniformRandomGenerator urg;

    // ______________________________________________________________________________________________
    public QueryGenerator(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
    }

    // ______________________________________________________________________________________________
    /**
     * every call of this control generates and send 10 random store data message
     *
     * @return boolean
     */
    public boolean execute() {
        if(!executeFlag){
            return false;
        }

        Node start;
        do {
            start = Network.get(CommonState.r.nextInt(Network.size()));
        } while ((start == null) || (!start.isUp()));

        BigInteger query = availableData.poll();
        if(query == null) return false;
        queriedData.put(query,0);
        VRouterObserver.dataQueryTraffic.put(query,0);
        VRouterProtocol p = (VRouterProtocol)start.getProtocol(pid);
        p.lookupMessages.add(new VLookupMessage(query,p.nodeId));

        return false;
    }

    // ______________________________________________________________________________________________

} // End of class
// ___________________________________________________________