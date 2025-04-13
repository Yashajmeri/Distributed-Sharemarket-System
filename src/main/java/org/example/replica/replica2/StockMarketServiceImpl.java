package org.example.replica.replica2;

import org.example.replica.resource.ShareType;
import org.example.replica.service.StockMarketService;

import javax.jws.WebService;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@WebService(endpointInterface = "org.example.replica.service.StockMarketService")
public class StockMarketServiceImpl implements StockMarketService, Runnable {
    private final ShareMarket shareMarket;
    private DatagramSocket socket = null;
    private final Map<ShareType, Map<String, Integer>> availableShareMap;
    // userId, shareType$shareId, count
    private final Map<String, Map<String, Integer>> purchasedShareMap;
    // cache
    private final ConcurrentMap<UUID, ResponseHolder> replyMap;

    public StockMarketServiceImpl(ShareMarket shareMarket) {
        super();
        this.shareMarket = shareMarket;
        try {
            this.socket = new DatagramSocket(this.shareMarket.getUdpPort());
        } catch (Exception e) {
            System.err.println("EXCEPTION : Server Error while creating DatagramSocket - " + e.getMessage());
        }
        availableShareMap = new HashMap<>();
        purchasedShareMap = new HashMap<>();
        for (ShareType type : ShareType.values()) {
            availableShareMap.put(type, new HashMap<>());
        }
        replyMap = new ConcurrentHashMap<>();
    }

    @Override
    public String addShare(String shareID, String shareType, int capacity) {
        ShareType type = ShareType.valueOf(shareType.toUpperCase());
        Map<String, Integer> availableShareMap = this.availableShareMap.get(type);

        if (!shareID.startsWith(shareMarket.getCode())) {
            return "FAIL: YOU CAN NOT ADD SHARE OF OTHER SHARE MARKET.";
        }

        if (availableShareMap.containsKey(shareID)) {
            return "FAIL: " + shareID + " IS ALREADY ADDED.";
        }

        availableShareMap.put(shareID, capacity);
        return "SUCCESS: " + type.name() + "-" + shareID + " ADDED.";
    }

    @Override
    public String removeShare(String shareID, String shareType) {
        if (!shareID.startsWith(shareMarket.getCode())) {
            return "FAIL: YOU CAN NOT REMOVE SHARE OF OTHER SHARE MARKET.";
        }

        boolean isPurchased = false;

        if (!purchasedShareMap.isEmpty()) {
            isPurchased = purchasedShareMap.values().stream()
                    .anyMatch(map -> map.containsKey(shareType + "$" + shareID));
        }

        if (isPurchased) {
            return "FAIL: " + shareID + " IS PURCHASED BY SOME BUYERS.";
        }

        if (availableShareMap.containsKey(ShareType.valueOf(shareType.toUpperCase())) && availableShareMap.get(ShareType.valueOf(shareType.toUpperCase())).containsKey(shareID)) {
            availableShareMap.get(ShareType.valueOf(shareType.toUpperCase()))
                    .remove(shareID);
        }
        return "SUCCESS: " + shareID + " REMOVED.";
    }

    @Override
    public String listShareAvailability(String shareType) {
        StringBuilder response = new StringBuilder(getStockAvailabilityString(shareType));

        UUID requestId = UUID.randomUUID();
        ResponseHolder holder = new ResponseHolder(2);
        replyMap.put(requestId, holder);

        StringBuilder message = new StringBuilder();
        List<ShareMarket> otherShareMarkets = Arrays.stream(ShareMarket.values())
                .filter(market -> !market.equals(shareMarket))
                .collect(Collectors.toList());

        message.append("REQUEST ")
                .append(requestId.toString()).append(" ")
                .append("LIST_AVAILABILITY").append(" ")
                .append(shareType);

        sendRequestToServers(otherShareMarkets, message.toString());

        try {
            boolean allResponsesReceived = holder.latch.await(5, TimeUnit.SECONDS);

            if (!allResponsesReceived) {
                System.err.println("Warning: Missing responses from servers");
            }
        } catch (InterruptedException e) {
            System.err.println("Exception while waiting for the response from other servers : " + e.getMessage());
        }

        synchronized (holder.responses) {
            for (String remoteResp : holder.responses) {
                if (!remoteResp.isEmpty()) {
                    if (remoteResp.startsWith("SUCCESS") && remoteResp.split(":").length > 1) {
                        if (!response.toString().isEmpty()) {
                            response.append("\n");
                        }
                        response.append(remoteResp.split(":")[1]);
                    }
                }
            }
        }

        replyMap.remove(requestId);
        if (response.toString().isEmpty()) {
            response.append("SUCCESS: NO SHARE IS AVAILABLE.");
        } else {
            response.insert(0, "SUCCESS:\n");
        }

        return response.toString();
    }

    @Override
    public String purchaseShare(String buyerID, String shareID, String shareType, int units) {
        StringBuilder response = new StringBuilder();
        if (!purchasedShareMap.containsKey(buyerID)) {
            purchasedShareMap.put(buyerID, new HashMap<>());
        }
        Map<String, Integer> purchasedShareMap = this.purchasedShareMap.get(buyerID);
        if (purchasedShareMap.containsKey(shareType + "$" + shareID)) {
            response.append("FAIL:")
                    .append("YOU ALREADY PURCHASED THE ")
                    .append(shareID)
                    .append(" OF TYPE ")
                    .append(shareType);
        } else {
            if (isDailyLimitReached(buyerID, shareID, shareType)) {
                // daily limit
                response.append("FAIL:")
                        .append("FOR DAILY LIMIT, YOU ALREADY PURCHASED THE SHARE OF TYPE ")
                        .append(shareID);
            } else {
                boolean isSameServer = shareID.toUpperCase().startsWith(shareMarket.getCode());

                try {
                    if (!isSameServer && isWeeklyLimitReached(buyerID, shareID)) {
                        response.append("FAIL:")
                                .append("FOR WEEKLY LIMIT, YOU ALREADY PURCHASED THE SHARE THREE TIMES FROM OTHER MARKETS.");
                    } else {
                        if (isSameServer) {
                            response.append(purchaseShareFromMarket(shareID, shareType, units));
                        } else {
                            ShareMarket otherShareMarket = ShareMarket.getShareMarketFromCode(shareID.toUpperCase().substring(0, 3));

                            if (otherShareMarket != null) {
                                UUID requestId = UUID.randomUUID();
                                ResponseHolder holder = new ResponseHolder(1);
                                replyMap.put(requestId, holder);

                                String message = "REQUEST " +
                                        requestId.toString() + " " +
                                        "PURCHASE_SHARE" + " " +
                                        buyerID + " " +
                                        shareID + " " +
                                        shareType + " " +
                                        units;

                                processTransaction(response, otherShareMarket, requestId, holder, message);
                            } else {
                                response.append("FAIL:")
                                        .append("UNABLE TO IDENTIFY SERVER.");
                            }
                        }

                        if (response.toString().startsWith("SUCCESS")) {
                            // updating purchaseMap
                            String[] params = response.toString().split(":")[1].split(" ");
                            if (!this.purchasedShareMap.containsKey(buyerID)) {
                                this.purchasedShareMap.put(buyerID, new HashMap<>());
                            }
                            this.purchasedShareMap.get(buyerID).put(params[1] + "$" + params[2], Integer.parseInt(params[3]));
                        }
                    }
                } catch (ParseException parseException) {
                    response.append("FAIL:").append("PARSE EXCEPTION : ").append(parseException.getMessage());
                    System.err.println("Exception while parsing share id : " + parseException.getMessage());
                }
            }
        }

        return response.toString();
    }

    @Override
    public String getShares(String buyerID) {
        if (!purchasedShareMap.containsKey(buyerID)) {
            return "SUCCESS: NO DATA EXISTS!";
        }
        StringBuilder response = new StringBuilder(getPurchasedShareList(buyerID));
        return response.toString();
    }

    @Override
    public String sellShare(String buyerID, String shareID, int units) {
        Map<String, Integer> purchasedMap = purchasedShareMap.get(buyerID);
        StringBuilder response = new StringBuilder();
        ShareType shareType = null;

        for (ShareType type : ShareType.values()) {
            if (purchasedMap.containsKey(type.name() + "$" + shareID)) {
                shareType = type;
                break;
            }
        }

        if (shareType == null) {
            response.append("FAIL:")
                    .append("YOU DON'T HAVE SHARES TO SELL.");
        } else {
            Integer purchasedUnits = purchasedMap.get(shareType.name() + "$" + shareID);
            if (purchasedUnits < units) {
                response.append("FAIL:")
                        .append("YOU DON'T HAVE THAT MUCH SHARES TO SELL.");
            } else {
                if (shareID.startsWith(shareMarket.getCode())) {
                    response.append(sellShareToMarket(shareID, shareType.name(), units));
                } else {
                    ShareMarket otherShareMarket = ShareMarket.getShareMarketFromCode(shareID.toUpperCase().substring(0, 3));

                    if (otherShareMarket != null) {
                        UUID requestId = UUID.randomUUID();
                        ResponseHolder holder = new ResponseHolder(1);
                        replyMap.put(requestId, holder);

                        String message = "REQUEST " +
                                requestId.toString() + " " +
                                "SELL_SHARE" + " " +
                                buyerID + " " +
                                shareID + " " +
                                shareType.name() + " " +
                                units;

                        processTransaction(response, otherShareMarket, requestId, holder, message);
                    }

                }

                if (response.toString().startsWith("SUCCESS")) {
                    // remove from user map as well
                    String[] params = response.toString().split(":")[1].split(" ");
                    if (purchasedUnits.equals(units)) {
                        purchasedMap.remove(shareType.name() + "$" + shareID);
                    } else {
                        purchasedMap.put(shareType.name() + "$" + shareID, purchasedUnits - units);
                    }
                }
            }
        }
        return response.toString();
    }

    @Override
    public String swapShares(String buyerID, String oldShareID, String oldShareType, String newShareID, String newShareType) {
        ShareType oldType = ShareType.valueOf(oldShareType);
        StringBuilder response = new StringBuilder();

        if (!purchasedShareMap.containsKey(buyerID)) {
            return "FAIL: NO DATA EXISTS!";
        } else {
            Map<String, Integer> buyerShares = purchasedShareMap.get(buyerID);
            if (!buyerShares.containsKey(oldType.name() + "$" + oldShareID)) {
                // user don't have any shares to swap
                response.append("FAIL:")
                        .append("YOU DON'T HAVE SHARES OF ")
                        .append(oldShareID)
                        .append(" TO SWAP WITH ")
                        .append(newShareID);
            } else {
                Integer purchasedQuantity = buyerShares.get(oldType.name() + "$" + oldShareID);
                ShareType newType = ShareType.valueOf(newShareType);

                boolean isAvailable = false;
                if (newShareID.startsWith(shareMarket.getCode())) {
                    isAvailable = checkAvailability(newShareID, newShareType, purchasedQuantity);
                } else {
                    ShareMarket otherShareMarket = ShareMarket.getShareMarketFromCode(newShareID.substring(0, 3));
                    if (otherShareMarket != null) {
                        UUID requestId = UUID.randomUUID();
                        ResponseHolder holder = new ResponseHolder(1);
                        replyMap.put(requestId, holder);

                        String requestMessage = "REQUEST " +
                                requestId.toString() + " " +
                                "CHECK_AVAILABILITY" + " " +
                                newShareID + " " +
                                newShareType + " " +
                                purchasedQuantity;

                        List<ShareMarket> otherShareMarketList = new ArrayList<>();
                        otherShareMarketList.add(otherShareMarket);

                        sendRequestToServers(otherShareMarketList, requestMessage);

                        try {
                            boolean allResponsesReceived = holder.latch.await(5, TimeUnit.SECONDS);

                            if (!allResponsesReceived) {
                                System.err.println("Warning: Missing responses from servers");
                            }
                        } catch (InterruptedException e) {
                            System.err.println("Exception while waiting for the response from other servers : " + e.getMessage());
                        }

                        synchronized (holder.responses) {
                            String responseFromOtherServer = holder.responses.get(0);
                            if (responseFromOtherServer.startsWith("SUCCESS")) {
                                if (responseFromOtherServer.split(":")[1].equalsIgnoreCase("true")) {
                                    isAvailable = true;
                                }
                            }
                        }
                    }
                }

                if (isAvailable) {
                    // sell first and then buy
                    String sellShareResponse = sellShare(buyerID, oldShareID, purchasedQuantity);

                    if (sellShareResponse.startsWith("SUCCESS")) {
                        String purchaseResponse = purchaseShare(buyerID, newShareID, newShareType, purchasedQuantity);
                        if (purchaseResponse.startsWith("SUCCESS")) {
                            response.append("SUCCESS:")
                                    .append("YOUR SHARES ")
                                    .append(oldShareID)
                                    .append(" WERE SWAPPED WITH ")
                                    .append(newShareID);
                        } else {
                            response.append(purchaseResponse);
                            purchaseShare(buyerID, oldShareID, oldShareType, purchasedQuantity);
                        }
                    } else {
                        response.append(sellShareResponse);
                    }
                } else {
                    response.append("FAIL:")
                            .append("WE DON'T HAVE ENOUGH QUANTITY OF ")
                            .append(newShareID)
                            .append(" TO SWAP.");
                }
            }
        }

        return response.toString();
    }

    private void processTransaction(StringBuilder response, ShareMarket otherShareMarket, UUID requestId, ResponseHolder holder, String message) {
        List<ShareMarket> otherShareMarketList = new ArrayList<>();
        otherShareMarketList.add(otherShareMarket);
        sendRequestToServers(otherShareMarketList, message);

        try {
            boolean allResponsesReceived = holder.latch.await(5, TimeUnit.SECONDS);

            if (!allResponsesReceived) {
                System.err.println("Warning: Missing responses from servers");
            }
        } catch (InterruptedException e) {
            System.err.println("Exception while waiting for the response from other servers : " + e.getMessage());
        }

        synchronized (holder.responses) {
            for (String remoteResp : holder.responses) {
                if (!remoteResp.isEmpty()) {
                    response.append(remoteResp);
                }
            }
        }
        replyMap.remove(requestId);
    }

    private String getStockAvailabilityString(String shareType) {
        StringBuilder response = new StringBuilder();
        Map<String, Integer> shareMap = availableShareMap.get(ShareType.valueOf(shareType.toUpperCase()));
        if (shareMap.isEmpty()) {
            return "";
        }
        response.append(shareType).append(" - ");

        for (Map.Entry<String, Integer> entry : shareMap.entrySet()) {
            response.append(entry.getKey()).append(" ").append(entry.getValue()).append(", ");
        }
        response.delete(response.length() - 2, response.length());
        return response.toString();
    }

    private String purchaseShareFromMarket(String shareID, String shareType, Integer units) {
        Map<String, Integer> shareMap = availableShareMap.get(ShareType.valueOf(shareType.toUpperCase()));
        StringBuilder response = new StringBuilder();
        if (shareMap.containsKey(shareID)) {
            Integer availableUnits = shareMap.get(shareID);
            if (availableUnits > 0) {
                int boughtUnits = availableUnits - units > 0 ? units : availableUnits;
                shareMap.put(shareID, availableUnits - boughtUnits);
                response.append("SUCCESS:")
                        .append("BUY ")
                        .append(shareType)
                        .append(" ")
                        .append(shareID.toUpperCase())
                        .append(" ")
                        .append(boughtUnits);
            } else {
                response.append("FAIL:")
                        .append("YOU CAN NOT BUT SHARE AS THERE ARE NO UNITS AVAILABLE.");
            }
        } else {
            response.append("FAIL:")
                    .append("NO SHARE FOUND WITH THE ID ")
                    .append(shareID)
                    .append(" TYPE ")
                    .append(shareType);
        }
        return response.toString();
    }

    private String sellShareToMarket(String shareID, String shareType, Integer units) {
        Map<String, Integer> shareMap = availableShareMap.get(ShareType.valueOf(shareType.toUpperCase()));
        shareMap.put(shareID, shareMap.get(shareID) + units);
        return "SUCCESS:" +
                "SELL " +
                shareType + " " +
                shareID + " " +
                units;
    }

    private String getPurchasedShareList(String buyerID) {
        Map<String, Integer> purchasedShareList = purchasedShareMap.get(buyerID);
        StringBuilder response = new StringBuilder();
        for (Map.Entry<String, Integer> entry : purchasedShareList.entrySet()) {
            String[] params = entry.getKey().split("\\$");
            // type$Id
            response.append(params[1]).append(" ")
                    .append(params[0]).append(" ")
                    .append(entry.getValue()).append(", ");
        }
        if (!purchasedShareList.isEmpty()) {
            response.delete(response.length() - 2, response.length());
        }
        return response.toString();
    }

    private boolean checkAvailability(String shareID, String shareType, Integer units) {
        ShareType type = ShareType.valueOf(shareType);
        if (availableShareMap.get(type).containsKey(shareID)) {
            Integer availableQuantity = availableShareMap.get(type).get(shareID);
            return availableQuantity >= units;
        }
        return false;
    }

    private boolean isDailyLimitReached(String buyerID, String shareID, String shareType) {
        if (purchasedShareMap.containsKey(buyerID)) {
            Map<String, Integer> shareMap = purchasedShareMap.get(buyerID);
            char[] ch = new char[]{'M', 'A', 'E'};
            for (char c : ch) {
                if (shareMap.containsKey(shareType + "$" + shareID.substring(0, 3) + c + shareID.substring(4))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWeeklyLimitReached(String buyerID, String shareID) throws ParseException {
        Calendar purchasingShareCalendar = stringToCalendar(shareID.substring(shareID.length() - 6));
        if (purchasedShareMap.containsKey(buyerID)) {
            Map<String, Integer> shareMap = purchasedShareMap.get(buyerID);

            long count = shareMap.keySet().stream()
                    .filter(id -> {
                        try {
                            Calendar purchasedShareCalendar = stringToCalendar(id.substring(id.length() - 6));
                            return !id.startsWith(shareMarket.getCode()) && purchasedShareCalendar.get(Calendar.YEAR) == purchasingShareCalendar.get(Calendar.YEAR) &&
                                    purchasedShareCalendar.get(Calendar.WEEK_OF_YEAR) == purchasingShareCalendar.get(Calendar.WEEK_OF_YEAR);
                        } catch (ParseException e) {
                            return false;
                        }
                    }).count();
            return count >= 3;
        }
        return false;
    }

    private Calendar stringToCalendar(String dateString) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyy");
        simpleDateFormat.setLenient(false);

        Date date = simpleDateFormat.parse(dateString);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar;
    }

    private void sendRequestToServers(List<ShareMarket> shareMarkets, String message) {
        try {
            for (ShareMarket shareMarket : shareMarkets) {
                DatagramPacket sendPacket = new DatagramPacket(
                        message.getBytes(), message.length(),
                        InetAddress.getByName(shareMarket.getIpAddress()), shareMarket.getUdpPort());
                socket.send(sendPacket);
            }
        } catch (UnknownHostException e) {
            System.err.println("EXCEPTION while looking for host by name : " + e.getMessage());
        } catch (IOException e) {
            System.err.println("EXCEPTION while sending packet : " + e.getMessage());
        }
    }

    private String processRequest(String requestMessage) throws RemoteException {
        // REQUEST UUID TYPE_OF_REQUEST REQUIRED PARAMS
        // 1. LIST_AVAILABILITY
        // 2. PURCHASE_SHARE
        // 3. SELL_SHARE
        // 4. GET_SHARE
        // 5. CHECK_AVAILABILITY
        String[] params = requestMessage.split(" ");
        StringBuilder response = new StringBuilder();
        response.append("REPLY ")
                .append(params[1])
                .append(" ");
        if (params.length > 2) {
            UUID requestID = UUID.fromString(params[1]);

            switch (params[2]) {
                case "LIST_AVAILABILITY":
                    response.append("SUCCESS:")
                            .append(getStockAvailabilityString(params[3]));
                    break;
                case "PURCHASE_SHARE":
                    String purchaseShareResponse = this.purchaseShareFromMarket(params[4], params[5], Integer.parseInt(params[6]));

                    if (purchaseShareResponse.startsWith("SUCCESS")) {
                        String[] purchaseShareParams = purchaseShareResponse.split(":")[1].split(" ");
                        if (!this.purchasedShareMap.containsKey(params[3])) {
                            this.purchasedShareMap.put(params[3], new HashMap<>());
                        }
                        this.purchasedShareMap.get(params[3]).put(purchaseShareParams[1] + "$" + purchaseShareParams[2], Integer.parseInt(purchaseShareParams[3]));
                    }
                    response.append(purchaseShareResponse);
                    break;
                case "SELL_SHARE":
                    Integer units = Integer.parseInt(params[6]);
                    Map<String, Integer> purchasedMap = purchasedShareMap.get(params[3]);
                    Integer purchasedUnits = purchasedMap.get(params[5] + "$" + params[4]);
                    if (purchasedUnits < units) {
                        response.append("FAIL:")
                                .append("YOU DON'T HAVE THAT MUCH SHARES TO SELL.");
                    } else {
                        String sellShareResponse = this.sellShareToMarket(params[4], params[5], units);
                        if (sellShareResponse.startsWith("SUCCESS")) {
                            String[] stringArr = sellShareResponse.split(":")[1].split(" ");
                            if (purchasedUnits.equals(units)) {
                                purchasedMap.remove(params[5] + "$" + params[4]);
                            } else {
                                purchasedMap.put(params[5] + "$" + params[4], purchasedUnits - units);
                            }
                        }
                        response.append(sellShareResponse);
                    }

                    break;
                case "GET_SHARE":
                    response.append("SUCCESS:")
                            .append(getPurchasedShareList(params[3]));
                    break;
                case "CHECK_AVAILABILITY":
                    boolean availability = checkAvailability(params[3], params[4], Integer.parseInt(params[5]));
                    response.append("SUCCESS:")
                            .append(availability);
                    break;
                default:
                    System.err.println("EXCEPTION: Invalid request parameters : " + requestMessage);
                    response.append("FAIL:")
                            .append("Invalid request parameters : ")
                            .append(requestMessage);
                    break;
            }
        }
        return response.toString();
    }

    private void processResponse(String[] params) {
        try {
            UUID uuid = UUID.fromString(params[1]);
            ResponseHolder holder = replyMap.get(uuid);

            if (holder != null) {
                synchronized (holder.responses) {
                    StringBuilder response = new StringBuilder();
                    for (int i = 2; i < params.length; i++) {
                        response.append(params[i]).append(" ");
                    }
                    response.delete(response.length() - 1, response.length());
                    holder.responses.add(response.toString());
                    holder.latch.countDown();
                }
            }
        } catch (Exception e) {
            System.err.println("EXCEPTION : Server Error while processing response from other server.txt - " + e.getMessage());
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                String[] stringArr = message.split(" ");
                if (stringArr[0].equalsIgnoreCase("REPLY")) {
                    // REPLY UUID STATUS:MESSAGE
                    processResponse(stringArr);
                } else if (stringArr[0].equalsIgnoreCase("REQUEST")) {
                    // send reply
                    String response = processRequest(message);
                    DatagramPacket sendPacket = new DatagramPacket(
                            response.getBytes(), response.length(),
                            receivePacket.getAddress(),
                            receivePacket.getPort()
                    );
                    socket.send(sendPacket);
                } else {
                    System.err.println("INVALID PACKET : ");
                    System.err.println("SOURCE : " + receivePacket.getAddress().getHostAddress());
                }
            } catch (Exception e) {
                System.err.println("EXCEPTION : " + e.getMessage());
            }
        }
    }

    private static class ResponseHolder {
        final CountDownLatch latch;
        final List<String> responses = new ArrayList<>();

        ResponseHolder(int expectedResponses) {
            this.latch = new CountDownLatch(expectedResponses);
        }
    }
}
