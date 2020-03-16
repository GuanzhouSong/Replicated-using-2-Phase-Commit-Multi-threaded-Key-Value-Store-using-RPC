package client;

import compute.ServerInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.UUID;
import util.Util;
import java.rmi.Naming;

public class Client {

    final static int serverNum = 5;

    public static void main(String[] args) throws Exception {
        ServerInterface[] stubs = new ServerInterface[5];
        Util util = new Util();
        Scanner sc = new Scanner(System.in);
        for (int i = 0; i < serverNum; i++) {
            stubs[i] = (ServerInterface) Naming.lookup("rmi://localhost/MapService"+i);
        }
        System.out.println(util.getFormattedTime() + "Please enter machine number, function and values:");
        System.out.println(util.getFormattedTime() + "Example: [0-4] <GET/PUT/DELETE> <VALUE1> <VALUE2>");
        while (true) {
            try {
                System.out.print(util.getFormattedTime());
                String input = sc.nextLine();

                //in case of client exit.
                if (input == null || "exit".equalsIgnoreCase(input)) {
                    System.out.println(util.getFormattedTime() + "Client Exit");
                    break;
                }

                String[] formattedInput = input.split(" ",2);

                int port = Integer.parseInt(formattedInput[0]);
                if(port >= serverNum){
                    util.log("please choose a machine number between 0 - 4");
                    continue;
                }
                String res = stubs[port].process(UUID.randomUUID(),input);

                System.out.println(res);
            } catch (Exception e) {
                util.log(e.getMessage());
            }
        }

        for (int i = 0; i < serverNum; i++) {
            System.exit(i);
        }
    }


}

	   
  