package org.example.replica.replica2;

import org.example.replica.resource.Message;
import org.example.replica.service.StockMarketService;
import org.example.sequencer.Config;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ReplicaManager {
    private static Integer lastSequenceID = 1;
    private static final ConcurrentMap<Integer, Message> messageMap = new ConcurrentHashMap<>();
    private static final Queue<Message> messageQueue = new PriorityBlockingQueue<>();
    private static StockMarketService stockMarketService = null;
    private static final Logger logger = Logger.getLogger(ReplicaManager.class.getName());
    private static boolean isServerUp = true;

    private static final Config currentReplica = Config.REPLICA2;
    private static final String REPLICA_MANAGER_NAME = "ReplicaManager2";
    private static final String REPLICA_NAME = "REPLICA2";
    private static final String REPLICA_SHORT = "RM2";

    public static void main(String[] args) {
        Runnable task = () -> {
            try (DatagramSocket socket = new DatagramSocket(currentReplica.getPortNumber())) {
                byte[] buffer = new byte[1000];
                logger.log(Level.INFO, REPLICA_MANAGER_NAME + " UDP started...");
                Runnable requestThread = () -> {
                    try {
                        executeRequests();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Exception while executing requests : " + e.getMessage());
                    }
                };
                Thread thread = new Thread(requestThread);
                thread.start();

                while (true) {
                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    socket.receive(request);

                    String data = new String(request.getData(), 0, request.getLength());
                    String[] params = data.split(";");

                    /*
                        Message Types:
                        00 - Simple message
                        01 - Sync request between the RMs
                        02 - Update of Message Queue (For lost messages)
                        11 - RM1 - bug
                        21 - RM2 - bug
                        31 - RM3 - bug
                        41 - RM4 - bug
                        12 - RM1 - down
                        22 - RM2 - down
                        32 - RM3 - down
                        42 - RM4 - down
                    */
                    switch (params[2]) {
                        case "00" -> {
                            Message message = requestStringToMessage(data);
                            Message messageForMulticast = requestStringToMessage(data);
                            messageForMulticast.setMessageType("01");
                            multicast(messageForMulticast);

                            if (message.getSequenceID() - lastSequenceID > 1) {
                                Message getUpdateMessage = new Message(0, null, "02",
                                        lastSequenceID.toString(), message.getSequenceID().toString(),
                                        REPLICA_NAME, null, null, null, 0);
                                multicast(getUpdateMessage);
                            }
                            messageQueue.add(message);
                            messageMap.put(message.getSequenceID(), message);
                        }
                        case "01" -> {
                            Message message = requestStringToMessage(data);
                            if (!messageMap.containsKey(message.getSequenceID())) {
                                messageMap.put(message.getSequenceID(), message);
                            }
                        }
                        case "02" ->
                                multicastOfMissingMessages(Integer.parseInt(params[3]), Integer.parseInt(params[4]), params[5]);
                        case "03" -> {
                            if (params[5].equalsIgnoreCase(REPLICA_SHORT)) {
                                updateMessageList(params[1]);
                            }
                        }
                        case "11" -> {
                            Message message = requestStringToMessage(data);
                            logger.log(Level.WARNING, "RM-1 has Bug: " + message);
                        }
                        case "21" -> {
                            Message message = requestStringToMessage(data);
                            logger.log(Level.WARNING, "RM-2 has Bug: " + message);
                        }
                        case "31" -> {
                            Message message = requestStringToMessage(data);
                            logger.log(Level.WARNING, "RM-3 has Bug: " + message);
                        }
                        case "41" -> {
                            Message message = requestStringToMessage(data);
                            logger.log(Level.WARNING, "RM-4 has Bug: " + message);
                        }
                        case "22" -> {
                            Thread handlingCrashThread = getCrashThread();
                            handlingCrashThread.start();
                            logger.log(Level.INFO, REPLICA_NAME + " handled the crash!!");
                            isServerUp = true;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception: " + e);
            }
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private static Thread getCrashThread() {
        Runnable handlingCrashTask = () -> {
            try {
                isServerUp = false;
                Thread.sleep(5000);
                String[] args = {""};
                StockMarketPublisher.main(args);
                Thread.sleep(5000);
                logger.log(Level.INFO, REPLICA_NAME + " Reloading servers...");
                reloadReplicaData();
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Exception while handling crash failure: " + exception.getMessage());
            }
        };
        return new Thread(handlingCrashTask);
    }

    private static void executeRequests() throws Exception {
        while (true) {
            synchronized (ReplicaManager.class) {
                Iterator<Message> messageIterator = messageQueue.iterator();
                while (messageIterator.hasNext()) {
                    Message requestMessage = messageIterator.next();
                    if (requestMessage.getSequenceID().equals(lastSequenceID) && isServerUp) {
                        logger.log(Level.INFO, REPLICA_NAME + " is executing message request. Request: " + requestMessage);
                        String response = processRequest(requestMessage);
                        logger.log(Level.INFO, "RESPONSE: " + response);
                        Message responseMessage = new Message(requestMessage.getSequenceID(), response, REPLICA_SHORT,
                                requestMessage.getOperationType(), requestMessage.getUserId(),
                                requestMessage.getOldShareID(), requestMessage.getOldShareType(),
                                requestMessage.getNewShareID(), requestMessage.getNewShareType(),
                                requestMessage.getUnits());
                        lastSequenceID += 1;
                        sendResponseToFrontEnd(responseMessage.toString(), requestMessage.getFrontEndIpAddress());
                        messageQueue.poll();
                    }
                }
            }
        }
    }

    private static String processRequest(Message requestMessage) throws Exception {
        if (requestMessage.getUserId().length() < 3 || !"AB".contains(requestMessage.getUserId().toUpperCase().charAt(3) + "")) {
            return null;
        }

        setStockMarketService(requestMessage.getUserId());

        if (requestMessage.getUserId().toUpperCase().charAt(3) == 'A' && requestMessage.getOperationType().equalsIgnoreCase("addShare")) {
            return stockMarketService.addShare(requestMessage.getNewShareID(), requestMessage.getNewShareType(), requestMessage.getUnits());
        } else if (requestMessage.getUserId().toUpperCase().charAt(3) == 'A' && requestMessage.getOperationType().equalsIgnoreCase("removeShare")) {
            return stockMarketService.removeShare(requestMessage.getNewShareID(), requestMessage.getNewShareType());
        } else if (requestMessage.getUserId().toUpperCase().charAt(3) == 'A' && requestMessage.getOperationType().equalsIgnoreCase("listShareAvailability")) {
            return stockMarketService.listShareAvailability(requestMessage.getNewShareType());
        } else if (requestMessage.getOperationType().equalsIgnoreCase("purchaseShare")) {
            return stockMarketService.purchaseShare(requestMessage.getUserId(), requestMessage.getNewShareID(), requestMessage.getNewShareType(), requestMessage.getUnits());
        } else if (requestMessage.getOperationType().equalsIgnoreCase("sellShare")) {
            return stockMarketService.sellShare(requestMessage.getUserId(), requestMessage.getNewShareID(), requestMessage.getUnits());
        } else if (requestMessage.getOperationType().equalsIgnoreCase("getShares")) {
            return stockMarketService.getShares(requestMessage.getUserId());
        } else if (requestMessage.getOperationType().equalsIgnoreCase("swapShare")) {
            return stockMarketService.swapShares(requestMessage.getUserId(), requestMessage.getOldShareID(), requestMessage.getOldShareType(), requestMessage.getNewShareID(), requestMessage.getNewShareType());
        }
        return null;
    }

    private static Message requestStringToMessage(String request) {
        String[] params = request.split(";");
        return new Message(Integer.parseInt(params[0]), // sequence_id
                params[1], params[2], // frontEndIpAddress, messageType
                params[3], // operationType
                params[4], // userId
                params[7], params[8], // oldShareID, oldShareType
                params[5], params[6], // newShareID, newShareType
                Integer.parseInt(params[9]));   // units
    }

    private static void updateMessageList(String data) {
        String[] requests = data.split("@");
        for (String request : requests) {
            Message message = requestStringToMessage(request);
            if (!messageMap.containsKey(message.getSequenceID())) {
                messageQueue.add(message);
                messageMap.put(message.getSequenceID(), message);
            }
        }
    }

    private static void multicast(Message message) {
        int port = currentReplica.getPortNumber();
        List<Config> replicas = getReplicasExcept();

        for (int i = 0; i < 3; i++) {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] data = message.toString().getBytes();
                InetAddress frontEndInetAddress = InetAddress.getByName(replicas.get(i).getIpAddress());

                DatagramPacket request = new DatagramPacket(data, data.length, frontEndInetAddress, replicas.get(i).getSocketPortNumber());
                socket.send(request);
                logger.log(Level.INFO, "Multicast message to " + frontEndInetAddress.getHostAddress() + ":" + replicas.get(i).getSocketPortNumber());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception while multicasting the request: ", e.getMessage());
            }
        }
    }

    private static void multicastOfMissingMessages(Integer startingSequenceId, Integer endingSequenceId, String replica) {
        StringBuilder response = new StringBuilder();
        for (Map.Entry<Integer, Message> entry : messageMap.entrySet()) {
            if (entry.getKey() > startingSequenceId && entry.getKey() < endingSequenceId) {
                response.append(entry.getValue()).append("@");
            }
        }

        if (response.length() > 1) {
            response.delete(response.length() - 1, response.length());
        }

        Message responseMessage = new Message(0, response.toString(), "03", startingSequenceId.toString(), endingSequenceId.toString(), replica, null, null, null, 0);
        multicast(responseMessage);
    }

    private static ShareMarket setStockMarketService(String userId) throws MalformedURLException, URISyntaxException {
        ShareMarket shareMarket = ShareMarket.getShareMarketFromCode(userId.substring(0, 3));
        if (shareMarket != null) {
            String hostAddress = "http://" + currentReplica.getIpAddress() + ":";
            URI uri = new URI(
                    "http",
                    null,
                    currentReplica.getIpAddress(),
                    currentReplica.getPortNumber(),
                    "/" + shareMarket.getCode(),
                    "wsdl",
                    null
            );
            QName qName = new QName("http://replica2.replica.example.org/", "StockMarketServiceImplService");
            Service service = Service.create(uri.toURL(), qName);
            stockMarketService = service.getPort(StockMarketService.class);
            return shareMarket;
        }
        return null;
    }

    private static void sendResponseToFrontEnd(String response, String frontEndIpAddress) {
        try (DatagramSocket socket = new DatagramSocket(currentReplica.getSocketPortNumber());) {
            byte[] bytes = response.getBytes();
            InetAddress frontEndInetAddress = InetAddress.getByName(frontEndIpAddress);
            DatagramPacket request = new DatagramPacket(bytes, bytes.length, frontEndInetAddress, 4545);
            socket.send(request);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception while sending response to the front-end: ", e.getMessage());
        }
    }

    private static void reloadReplicaData() {
        // TODO
        Set<Integer> sequenceNumbersSet = messageMap.keySet();
        sequenceNumbersSet.stream().sorted()
                .forEach(sequenceNumber -> {
                    try {
                        processRequest(messageMap.get(sequenceNumber));
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Exception while reloading replica data: ", e.getMessage());
                        logger.log(Level.SEVERE, "SEQUENCE NUMBER: " + sequenceNumber);
                        logger.log(Level.SEVERE, "REQUEST DATA: " + messageMap.get(sequenceNumber));
                    }
                    if (sequenceNumber >= lastSequenceID)
                        lastSequenceID = sequenceNumber + 1;
                });
        messageQueue.clear();
    }

    private static List<Config> getReplicasExcept() {
        return Stream.of(Config.REPLICA1, Config.REPLICA2, Config.REPLICA3, Config.REPLICA4)
                .filter(replica -> !replica.equals(currentReplica))
                .toList();
    }
}

