package util;

import java.net.InetAddress;
import java.security.spec.InvalidParameterSpecException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Scanner;

public class Util {

    /**
     * return current time in readable format.
     * @return string of time.
     */
    public String getFormattedTime() {
        DateFormat pstFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return pstFormat.format(System.currentTimeMillis()) + ": ";
    }

    public void log(String message){
        System.out.println(this.getFormattedTime()+message);
    }

    /**
     * take command and respond.
     * @param store main map to store key-value information
     * @param cmd command received from client
     * @return respond to client.
     * @throws InvalidParameterSpecException if command line is invalid or action is not legal.
     */
    public String process(HashMap<String, String> store, String cmd) throws InvalidParameterSpecException {
        String msg;

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

                store.put(key, value);
                msg = "OK";
                System.out.println(getFormattedTime() + "PUT(" + key + ", " + value + ")");
                break;
            }
            case "get": {
                if (arguments.length != 2) {
                    //error when command is invalid and  not in the correct format.
                    throw new InvalidParameterSpecException("Invalid GET operation: GET <key>");
                }
                try {
                    if (!store.containsKey(arguments[1])) {
                        throw new Exception();
                    }
                    msg = store.get(arguments[1]);
                    System.out.println(getFormattedTime() + "GET(" + arguments[1] + ")");
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
                    if (!store.containsKey(arguments[1])) {
                        throw new Exception();
                    }
                    store.remove(arguments[1]);
                    msg = (arguments[1] + " Deleted");
                    System.out.println(getFormattedTime() + "DELETE(" + arguments[1] + ")");
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

        return getFormattedTime() + msg;
    }


}
