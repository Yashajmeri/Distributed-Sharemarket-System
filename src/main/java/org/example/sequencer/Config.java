package org.example.sequencer;

//public class Config {
//
//    public static final String SEQUENCER_IP = "";
//
//    public static final int SEQUENCER_PORT = 8087;
//
//    public static final int REPLICA1_PORT = 8099;
//
//    public static final int REPLICA2_PORT = 9099;
//
//    public static final int REPLICA3_PORT = 9119;
//
//    public static final int REPLICA4_PORT = 9219;
//
//    public static final String REPLICA1_ADDRESS = "";
//
//    public static final String REPLICA2_ADDRESS = "";
//
//    public static final String REPLICA3_ADDRESS = "";
//
//    public static final String REPLICA4_ADDRESS = "";
//
//    public static int getReplicaPort(int i) {
//        switch (i) {
//            case 0 -> {
//                return REPLICA1_PORT;
//            }
//            case 1 -> {
//                return REPLICA2_PORT;
//            }
//            case 2 -> {
//                return REPLICA3_PORT;
//            }
//            case 3 -> {
//                return REPLICA4_PORT;
//            }
//            default -> {
//                return 0;
//            }
//        }
//    }
//
//    public static String getReplicaAddress(int i) {
//        switch (i) {
//            case 0 -> {
//                return REPLICA1_ADDRESS;
//            }
//            case 1 -> {
//                return REPLICA2_ADDRESS;
//            }
//            case 2 -> {
//                return REPLICA3_ADDRESS;
//            }
//            case 3 -> {
//                return REPLICA4_ADDRESS;
//            }
//            default -> {
//                return "localhost";
//            }
//        }
//    }

public enum Config {
    FRONT_END("Front-End", "192.168.230.151", 4999, 4998),
    SEQUENCER("Sequencer", "192.168.230.239", 5999, 5998),
    REPLICA1("Replica1", "192.168.230.151", 6999, 6998),
    REPLICA2("Replica2", "192.168.230.239", 7999, 7998),
    REPLICA3("Replica3", "192.168.230.163", 8999, 8998),
    REPLICA4("Replica4", "192.168.230.66", 9999, 9998),
    FRONT_END_SQ("FrontEnd-SQ", null, 8311, null);
    private final String name;
    private final String ipAddress;
    private final Integer portNumber;
    private final Integer socketPortNumber;

    Config(String name, String ipAddress, Integer portNumber, Integer socketPortNumber) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
        this.socketPortNumber = socketPortNumber;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    public Integer getSocketPortNumber() {
        return socketPortNumber;
    }
}
