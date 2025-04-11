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
        FRONT_END("Front-End", "FRONT-END", 4999, null),
        SEQUENCER("Sequencer", "SEQUENCER", 5999, null),
        REPLICA1("Replica1", "REPLICA1", 6999, 6998),
        REPLICA2("Replica2", "REPLICA2", 7999, 7998),
        REPLICA3("Replica3", "REPLICA3", 8999, 8998),
        REPLICA4("Replica4", "REPLICA4", 9999, 9998);

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
