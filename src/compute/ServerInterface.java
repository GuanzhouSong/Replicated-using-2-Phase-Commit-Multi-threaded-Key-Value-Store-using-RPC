package compute;

import util.AckType;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.spec.InvalidParameterSpecException;
import java.util.UUID;

public interface ServerInterface extends Remote {

    String process(UUID messageId, String cmd) throws InvalidParameterSpecException, RemoteException;

    String process(String cmd) throws InvalidParameterSpecException, RemoteException;
    //String KeyValue(UUID messageId, String functionality, String key, String value) throws RemoteException;

    void acknowledge(UUID messageId, int callBackServer, AckType type) throws RemoteException;

    void doAbort(UUID messageId, int callBackServer) throws RemoteException;

    void doCommit(UUID messageId, int callBackServer) throws RemoteException, InvalidParameterSpecException;

    void doPrepare(UUID messageId, String cmd, int callBackServer) throws RemoteException;

    void setServersInfo(int[] OtherServersPorts, int yourPorts) throws RemoteException;

}
