package org.example.replica.replica4;

public enum ShareMarket {
    NEW_YORK("New York", "NYK", "localhost", 5010, 9885),
    LONDON("London", "LON", "localhost", 5011, 9886),
    TOKYO("Tokyo", "TOK", "localhost", 5012, 9887);

    private final String name;
    private final String code;
    private final String ipAddress;
    private final Integer rmiPort;
    private final Integer udpPort;

    ShareMarket(String name, String code, String ipAddress, Integer rmiPort, Integer udpPort) {
        this.name = name;
        this.code = code;
        this.ipAddress = ipAddress;
        this.rmiPort = rmiPort;
        this.udpPort = udpPort;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Integer getRmiPort() {
        return rmiPort;
    }

    public Integer getUdpPort() {
        return udpPort;
    }

    public static ShareMarket getShareMarketFromCode(String code) {
        for (ShareMarket shareMarket : ShareMarket.values()) {
            if (shareMarket.getCode().equals(code)) {
                return shareMarket;
            }
        }
        return null;
    }
}
