package vRouter;

import java.math.BigInteger;

public class VLookupMessage {
    public BigInteger dataID;
    public BigInteger from;
    public int forwardHops;
    public int backwardHops;
    public boolean direction;  //true for forward; false for backward;

    public VLookupMessage(BigInteger t, BigInteger from){
        dataID = t;
        this.from = from;
        forwardHops = 1;
        backwardHops = 0;
        direction = true;
    }

    public VLookupMessage forward(BigInteger from){
        VLookupMessage msg = new VLookupMessage(this.dataID,from);
        msg.forwardHops = this.forwardHops +1;
        return msg;
    }

    public VLookupMessage backward(BigInteger from){
        VLookupMessage msg = new VLookupMessage(this.dataID,from);
        msg.backwardHops = this.backwardHops +1;
        msg.direction = false;
        return msg;
    }

}
