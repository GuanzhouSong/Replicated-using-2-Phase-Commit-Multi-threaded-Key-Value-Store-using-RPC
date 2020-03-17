package server;

import compute.ServerInterface;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import util.CONST;
import util.Util;

public class Coordinator extends Thread {


    static Server[] servers = new Server[CONST.serverNum];

    public static void main(String[] args) throws Exception {

        Util util = new Util();
        for (int i = 0; i < CONST.serverNum; i++) {
            try {
                servers[i] = new Server();
                ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(servers[i], 0);

                Naming.rebind("rmi://localhost:1099/MapService"+i, stub);
                int[] others = new int[4];
                int k = 0;
                for (int j = 0; j < CONST.serverNum; j++) {
                    if(j != i){
                        others[k++] = j;
                    }
                }
                stub.setServersInfo(others,i);
                util.log("Server "+i+" is running.");

            } catch (Exception e) {
                System.err.println("Server exception: " + e.toString());
            }

            Thread serverThread = new Thread();
            serverThread.start();
        }
    }


}
