package server;

import util.AckType;
import compute.ServerInterface;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import util.CONST;
import util.Util;


public class Server extends Thread implements ServerInterface {

    private int[] otherServers = new int[CONST.serverNum-1];
    private int myPort;
    static Util util = new Util();
    private Map<String, String> map = new ConcurrentHashMap<>();
    private Map<UUID, String> pendingChanges = new ConcurrentHashMap<>();
    private Map<UUID, Map<Integer, Ack>> pendingPrepareAcks = new ConcurrentHashMap<>();
    private Map<UUID, Map<Integer, Ack>> pendingCommitAcks = new ConcurrentHashMap<>();

    /**
     * take command and respond.
     *
     * @param cmd command received from client
     * @return respond to client.
     * @throws InvalidParameterSpecException if command line is invalid or action is not legal.
     */
    @Override
    public String process(String cmd) throws InvalidParameterSpecException, RemoteException {
        String msg;

        String[] formattedInput = cmd.split(" ", 2);
        cmd = formattedInput[1];

        //check if the command's first arg is valid.
        String[] arguments = cmd.split(" ");
        if (!"putgetdelete".contains(arguments[0].toLowerCase())) {
            throw new InvalidParameterSpecException("Invalid arguments other than PUT/GET/DELETE.");
        }

        switch (arguments[0].toLowerCase()) {
            case "put": {
                String key, value;
                try {
                    key = arguments[1];
                    value = cmd.split("\"")[1];
                    if (value == null || "".equals(value) || key == null || "".equals(key)) {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    //error when command is invalid and  not in the correct format.
                    throw new InvalidParameterSpecException("Invalid PUT operation: PUT <KEY> \"<VALUE>\"");
                }

                map.put(key, value);
                msg = "OK";
                System.out.println(util.getFormattedTime() + "Server " + this.myPort + ": PUT(" + key + ", " + value + ")");
                break;
            }
            case "get": {
                if (arguments.length != 2) {
                    //error when command is invalid and  not in the correct format.
                    throw new InvalidParameterSpecException("Invalid GET operation: GET <key>");
                }
                try {
                    if (!map.containsKey(arguments[1])) {
                        throw new Exception();
                    }
                    msg = map.get(arguments[1]);
                    System.out.println(util.getFormattedTime() + "GET(" + arguments[1] + ")");
                } catch (Exception e) {
                    //error when command is invalid, trying to get using a non-existing key.
                    throw new InvalidParameterSpecException("Invalid Key: key \"" + arguments[1] + "\" does NOT exist");
                }
                break;
            }
            case "delete": {
                if (arguments.length != 2) {
                    //error when command is invalid and  not in the correct format.
                    throw new InvalidParameterSpecException("Invalid GET operation: DELETE <key>");
                }
                try {
                    if (!map.containsKey(arguments[1])) {
                        throw new Exception();
                    }
                    map.remove(arguments[1]);
                    msg = (arguments[1] + " Deleted");
                    System.out.println(util.getFormattedTime() + "DELETE(" + arguments[1] + ")");
                } catch (Exception e) {
                    //error when command is invalid, trying to delete using a non-existing key.
                    throw new InvalidParameterSpecException("Invalid Key: key \"" + arguments[1] + "\" does  NOT exist");
                }
                break;
            }
            default:
                //command is invalid, does not start with right arg.
                throw new InvalidParameterSpecException("input operation is invalid, please use PUT/GET/DELETE");
        }

        return util.getFormattedTime() + msg;
    }

    @Override
    public String process(UUID messageId, String cmd) throws RemoteException, InvalidParameterSpecException {
        String[] arguments = cmd.split(" ");
        int timeout = 100, retry = 5;
        if ("GET".equalsIgnoreCase(arguments[1])) {
            return process(cmd);
        }
        this.pendingChanges.put(messageId, cmd);
        String prepare = callPrepareAndWaitForAck(messageId, cmd, timeout, retry);
        if (!"true".equalsIgnoreCase(prepare)) {
            return util.getFormattedTime()+prepare;
        }

        String commit = callCommitAndWaitForAck(messageId, timeout, retry);
        if (!"true".equalsIgnoreCase(commit)) {
            return util.getFormattedTime()+commit;
        }

        String pendingCMD = this.pendingChanges.get(messageId);

        if (pendingCMD == null) {
            throw new IllegalArgumentException("The message is not in the storage");
        }

        String message = this.process(pendingCMD);
        this.pendingChanges.remove(messageId);

        return message;
    }


    private String callPrepareAndWaitForAck(UUID messageId, String cmd, int timeout, int retry) {

        int AckReceived = 0;
        this.pendingPrepareAcks.put(messageId, new ConcurrentHashMap<>());
        while (retry >= 0) {

            try {
                Thread.sleep(timeout);
            } catch (Exception ex) {
                util.log("wait fail.");
            }
            AckReceived = 0;
            retry--;
            Map<Integer, Ack> map = this.pendingPrepareAcks.get(messageId);
            for (int server : this.otherServers) {
                if (map.containsKey(server) && map.get(server).status == AckType.PREPARED) {
                    AckReceived++;
                } else if(map.containsKey(server) && map.get(server).status == AckType.ABORT){
                    globalAbort(messageId);
                    return map.get(server).getErrMsg();
                }else {

                    util.log("Prepare: Ask for prepare on server " + server);
                    try {
                        this.pendingPrepareAcks.get(messageId).put(server, new Ack());
                        ServerInterface stub = (ServerInterface) Naming.lookup("rmi://localhost/MapService" + server);
                        stub.doPrepare(messageId, cmd, myPort);
                        util.log(map.get(server).status.toString());
                    } catch (Exception ex) {
                        util.log("Something went wrong when prepare, Abort");
                        this.pendingPrepareAcks.get(messageId).put(server, new Ack(AckType.ABORT,ex.getMessage()));
                    }

                }
            }
            this.pendingPrepareAcks.put(messageId,map);
            if (AckReceived == 4) {
                return "true";
            }
        }
        return "false";
    }


    private String callCommitAndWaitForAck(UUID messageId, int timeout, int retry) {

        int areAllAck = 0;
        this.pendingCommitAcks.put(messageId, new ConcurrentHashMap<>());
        while (retry >= 0) {
            try {
                Thread.sleep(timeout);
            } catch (Exception ex) {
                util.log("wait fail.");
            }

            areAllAck = 0;
            retry--;
            Map<Integer, Ack> map = this.pendingCommitAcks.get(messageId);

            for (int server : this.otherServers) {
                if (map.containsKey(server) && map.get(server).status == AckType.COMMITTED) {
                    areAllAck++;
                } else if(map.containsKey(server) && map.get(server).status == AckType.ABORT){
                    globalAbort(messageId);
                    return map.get(server).getErrMsg();
                }else {
                    util.log("Commit: Ask for commit for server " + server);
                    try {
                        this.pendingCommitAcks.get(messageId).put(server, new Ack());
                        ServerInterface stub = (ServerInterface) Naming.lookup("rmi://localhost/MapService" + server);
                        stub.doCommit(messageId, myPort);
                    } catch (Exception ex) {
                        util.log("Something went wrong when commit, Abort");

                        this.pendingCommitAcks.get(messageId).put(server, new Ack(AckType.ABORT,ex.getMessage()));
                    }


                }
            }
            this.pendingCommitAcks.put(messageId,map);
            if (areAllAck == 4) {
                return "true";
            }
        }

        return "false";
    }


    private void globalAbort(UUID messageId) {

        for (int server : this.otherServers) {
            util.log("Abort: Ask for abort for server " + server);
            try {
                ServerInterface stub = (ServerInterface) Naming.lookup("rmi://localhost/MapService" + server);
                stub.doAbort(messageId, myPort);
            } catch (Exception ex) {
                util.log("Something went wrong, removing data from temporary storage" + ex.getMessage());
            }
        }
    }


    @Override
    public void acknowledge(UUID messageId, int yourPort, AckType type) throws RemoteException {
        if(type == AckType.COMMITTED){
            Map<Integer, Ack> temp = this.pendingCommitAcks.getOrDefault(messageId,new ConcurrentHashMap<>());
            temp.put(yourPort, new Ack(type));
            this.pendingCommitAcks.put(messageId, temp);
        }else if(type == AckType.PREPARED){
            Map<Integer, Ack> temp = this.pendingPrepareAcks.getOrDefault(messageId,new ConcurrentHashMap<>());
            temp.put(yourPort, new Ack(type));
            this.pendingPrepareAcks.put(messageId, temp);
        }

    }

    @Override
    public void doCommit(UUID messageId, int callBackServer) throws RemoteException, InvalidParameterSpecException {

        String pendingCMD = this.pendingChanges.get(messageId);

        if (pendingCMD == null) {
            throw new IllegalArgumentException("The message is not in the storage");
        }

        this.process(pendingCMD);
        this.pendingChanges.remove(messageId);
        this.sendAck(messageId, callBackServer, AckType.COMMITTED);
    }

    @Override
    public void doPrepare(UUID messageId, String cmd, int callBackServer) throws RemoteException {
        if (!this.pendingChanges.containsKey(messageId)) {
            this.pendingChanges.put(messageId, cmd);
        }
        sendAck(messageId, callBackServer, AckType.PREPARED);
    }

    @Override
    public void doAbort(UUID messageId, int callBackServer) throws RemoteException {
        this.pendingChanges.remove(messageId);
        sendAck(messageId, callBackServer, AckType.ABORT);
    }

    @Override
    public void setServersInfo(int[] otherServersPorts, int yourPort)
        throws RemoteException {

        this.otherServers = otherServersPorts;
        this.myPort = yourPort;
    }

    private void sendAck(UUID messageId, int destination, AckType type) {
        try {
            ServerInterface stub = (ServerInterface) Naming.lookup("rmi://localhost/MapService" + destination);
            stub.acknowledge(messageId, myPort, type);
        } catch (Exception ex) {
            util.log("Something went wrong in sending Ack, will abort");
            this.pendingChanges.remove(messageId);
        }
    }


}
