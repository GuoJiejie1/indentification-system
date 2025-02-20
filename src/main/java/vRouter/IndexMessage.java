package vRouter;

import java.math.BigInteger;

public class IndexMessage {
    public BigInteger dataID;
    public BigInteger from;
    public int hops;

    public IndexMessage(BigInteger data, BigInteger origin){
        this.dataID = data;
        this.from = origin;
        this.hops = 1;
    }

    public IndexMessage relay(BigInteger local){
        IndexMessage relay = new IndexMessage(dataID, local);
        relay.hops = this.hops+1;
        return relay;
    }
}
