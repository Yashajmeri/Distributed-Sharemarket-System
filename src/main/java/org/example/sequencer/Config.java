package org.example.sequencer;

public class Config {

    public static final String SEQUENCER_IP = "";

    public static final int SEQUENCER_PORT = 8087;

    public static final int REPLICA1_PORT = 8099;

    public static final int REPLICA2_PORT = 9099;

    public static final int REPLICA3_PORT = 9119;

    public static final int REPLICA4_PORT = 9219;

    public static final String REPLICA1_ADDRESS = "";

    public static final String REPLICA2_ADDRESS = "";

    public static final String REPLICA3_ADDRESS = "";

    public static final String REPLICA4_ADDRESS = "";

    public static int getReplicaPort(int i) {
        switch (i) {
            case 0 -> {
                return REPLICA1_PORT;
            }
            case 1 -> {
                return REPLICA2_PORT;
            }
            case 2 -> {
                return REPLICA3_PORT;
            }
            case 3 -> {
                return REPLICA4_PORT;
            }
            default -> {
                return 0;
            }
        }
    }

    public static String getReplicaAddress(int i) {
        switch (i) {
            case 0 -> {
                return REPLICA1_ADDRESS;
            }
            case 1 -> {
                return REPLICA2_ADDRESS;
            }
            case 2 -> {
                return REPLICA3_ADDRESS;
            }
            case 3 -> {
                return REPLICA4_ADDRESS;
            }
            default -> {
                return "localhost";
            }
        }
    }

}
