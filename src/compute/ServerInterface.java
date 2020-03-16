package compute;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;
import server.AckType;

public interface ServerInterface extends Remote {

    String KeyValue(UUID messageId, String functionality, String key, String value) throws RemoteException;

    void ackMe(UUID messageId, int callBackServer, AckType type) throws RemoteException;

    void go(UUID messageId, int callBackServer) throws RemoteException;

    void prepareKeyValue(UUID messageId, String functionality, String key, String value, int callBackServer) throws RemoteException;

    void setServersInfo(int[] OtherServersPorts, int yourPorts) throws RemoteException;

    int getPort() throws RemoteException;
}
