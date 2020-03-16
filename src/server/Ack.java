package server;

import ENUM.AckType;

public class Ack {
    AckType status;

    String errMsg;

    public Ack(){
        this.status = AckType.PENDING;
    }

    public Ack(AckType ackType){
        this.status = ackType;
    }

    public Ack(AckType ackType, String msg){
        this.status = ackType;
        this.errMsg = msg;
    }


    public AckType getStatus() {
        return status;
    }

    public void setStatus(AckType status) {
        this.status = status;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
}
