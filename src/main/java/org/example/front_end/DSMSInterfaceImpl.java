package org.example.front_end;

import javax.jws.WebService;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.example.front_end.FrontEndInterfaceImpl.FE_IP_Address;

@WebService(endpointInterface = "org.example.front_end.DSMSInterface")
public class DSMSInterfaceImpl implements DSMSInterface {
    // Configuration constants
    private static final long INITIAL_TIMEOUT = 10000;
    private static final long MIN_TIMEOUT = 2000;
    private static final long MAX_TIMEOUT = 30000;
    private static final double TIMEOUT_SMOOTHING_FACTOR = 0.3;
    private static final int FAILURE_THRESHOLD = 3;

    // Dynamic timeout management
    private static long DYNAMIC_TIMEOUT = INITIAL_TIMEOUT;

    // Failure tracking
    private static final Map<Integer, Integer> rmBugCounts = new HashMap<>();
    private static final Map<Integer, Integer> rmNoResponseCounts = new HashMap<>();

    // Response tracking
    private final List<ServerResponse> responses = new ArrayList<>();
    private long responseTime = DYNAMIC_TIMEOUT;
    private long startTime;
    private CountDownLatch latch;
    private FrontEndInterface inter;

    public DSMSInterfaceImpl(FrontEndInterface inter) {
        super();
        this.inter = inter;
        Runnable task = this::listenForUDPResponses;
        Thread thread = new Thread(task);
        thread.start();
    }

    public DSMSInterfaceImpl() {
        this.inter = null;
    }


    @Override
    public String addShare(String userID, String shareID, String shareType, Integer capacity) {
        FERequest request = new FERequest("addShare", userID);
        request.setShareID(shareID);
        request.setShareType(shareType);
        request.setShareUnits(capacity);
        request.setSequenceNumber(forwardUdpUnicastToSequencer(request));
        System.out.println("DSMS_Implementation :  addShare ->" + request);

        return validateResponses(request);
    }

    @Override
    public String removeShare(String userID, String shareID, String shareType) {
        FERequest request = new FERequest("removeShare", userID);
        request.setShareID(shareID);
        request.setShareType(shareType);
        request.setSequenceNumber(forwardUdpUnicastToSequencer(request));
        System.out.println("DSMS_Implementation :  removeShare ->" + request);

        return validateResponses(request);
    }

    @Override
    public String listShareAvailability(String userID, String shareType) {
        FERequest request = new FERequest("listShareAvailability", userID);
        request.setShareType(shareType);
        request.setSequenceNumber(forwardUdpUnicastToSequencer(request));
        System.out.println("DSMS_Implementation :  listShareAvailability ->" + request);
        return validateResponses(request);
    }

    @Override
    public String purchaseShare(String userID, String buyerID, String shareID, String shareType, Integer units) {
        FERequest request = new FERequest("purchaseShare", buyerID);
        request.setShareID(shareID);
        request.setShareType(shareType);
        request.setShareUnits(units);
        request.setSequenceNumber(forwardUdpUnicastToSequencer(request));
        System.out.println("DSMS_Implementation :  purchaseShare ->" + request);
        return validateResponses(request);
    }

    @Override
    public String getShares(String userID, String buyerID) {
        FERequest request = new FERequest("getShares", buyerID);
        request.setSequenceNumber(forwardUdpUnicastToSequencer(request));
        System.out.println("DSMS_Implementation : getShares ->" + request);

        return validateResponses(request);
    }

    @Override
    public String sellShare(String userID, String buyerID, String shareID, Integer units) {
        FERequest request = new FERequest("sellShare", buyerID);
        request.setShareID(shareID);
        request.setShareUnits(units);
        request.setSequenceNumber(forwardUdpUnicastToSequencer(request));
        System.out.println("DSMS_Implementation :  sellShare ->" + request);

        return validateResponses(request);
    }

    @Override
    public String swapShare(String userID, String buyerID, String oldShareId, String oldShareType, String newShareId, String newShareType) {
        FERequest request = new FERequest("swapShare", buyerID);
        request.setShareID(newShareId);
        request.setShareType(newShareType);
        request.setOldShareId(oldShareId);
        request.setOldShareType(oldShareType);
        request.setSequenceNumber(forwardUdpUnicastToSequencer(request));
        System.out.println("DDSMS_Implementation : swapShare ->" + request);

        return validateResponses(request);
    }

    // Communication Methods
    private int forwardUdpUnicastToSequencer(FERequest clientRequest) {
        startTime = System.nanoTime();
        int sequenceNumber = inter.forwardRequestToSequencer(clientRequest);
        clientRequest.setSequenceNumber(sequenceNumber);
        latch = new CountDownLatch(4);
        waitForResponse();
        return sequenceNumber;
    }

    private void waitForResponse() {
        try {
            System.out.printf(" DSMS_Implementation : Waiting for Response -> Responses Remain : %d%n", latch.getCount());
            boolean timeoutReached = latch.await(DYNAMIC_TIMEOUT, TimeUnit.MILLISECONDS);
            if (timeoutReached) {
                setDynamicTimeout();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("DSMS_Implementation : Response waiting interrupted: " + e.getMessage());
        }
    }

    private void setDynamicTimeout() {

        DYNAMIC_TIMEOUT = (long) (TIMEOUT_SMOOTHING_FACTOR * responseTime * 3 +
                (1 - TIMEOUT_SMOOTHING_FACTOR) * DYNAMIC_TIMEOUT);

        // Ensure timeout stays within bounds
        DYNAMIC_TIMEOUT = Math.max(MIN_TIMEOUT, Math.min(DYNAMIC_TIMEOUT, MAX_TIMEOUT));

        System.out.println("DSMS_Implementation : Adjusted dynamic timeout to: " + DYNAMIC_TIMEOUT + "ms");
    }

    // Response Handling
    private void addReceivedResponse(ServerResponse response) {
        long endTime = System.nanoTime();
        responseTime = (endTime - startTime) / 1_000_000;
        System.out.println("DSMS_Implementation : Response received in " + responseTime + "ms");
        responses.add(response);
        notifyResponseReceived();
    }

    private void notifyResponseReceived() {
        if (latch != null) {
            latch.countDown();
            System.out.printf("DSMS_Implementation : notifyResponseReceived -> Remaining responses expected: %d%n", latch.getCount());
        }
    }

    // Response Validation
    private String retryRequest(FERequest request) {
        System.out.println("DSMS_Implementation : Retrying request-> " + request);
        startTime = System.nanoTime();
        inter.retryRequest(request);
        latch = new CountDownLatch(4);
        waitForResponse();
        return validateResponses(request);
    }

    private String validateResponses(FERequest request) {
        int responsesReceived = 4 - (int) latch.getCount();

        if (responsesReceived > 0) {
            return determineConsensusResponse(request);
        }
        // No responses case
        handleTotalFailure(request);
        return "DSMS_Implementation : Fail-> No response from any server";
    }

    private String determineConsensusResponse(FERequest request) {
        Map<Integer, ServerResponse> rmResponses = getValidResponses(request.getSequenceNumber());

        if (rmResponses.isEmpty()) {
            return "DSMS_Implementation : Fail -> No valid responses received";
        }

        String consensus = findConsensusResponse(rmResponses);
        evaluateRmPerformance(rmResponses, consensus);

        return consensus != null ? consensus : "DSMS_Implementation : Fail-> No consensus reached";
    }

    // Response Analysis
    private Map<Integer, ServerResponse> getValidResponses(int sequenceNumber) {
        Map<Integer, ServerResponse> resultMap = new HashMap<>();


        for (ServerResponse response : responses) {

            if (response.getSequenceID() == sequenceNumber) {

                int rmNumber = response.getRmNumber();


                if (!resultMap.containsKey(rmNumber)) {
                    resultMap.put(rmNumber, response);
                }

            }
        }

        return resultMap;

    }

    private String findConsensusResponse(Map<Integer, ServerResponse> rmResponses) {
        Map<String, Long> responseCounts = rmResponses.values().stream()
                .collect(Collectors.groupingBy(
                        ServerResponse::getResponse,
                        Collectors.counting()
                ));
        long totalCount = responseCounts.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long Grade = validGrade(totalCount);
        return responseCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() >= Grade) //  Majority responses
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private long validGrade(long responseCount) {
        if (responseCount % 2 == 0)
            return responseCount / 2;
        else return (responseCount / 2) + 1;
    }

    // Failure Handling
    private void evaluateRmPerformance(Map<Integer, ServerResponse> rmResponses, String correctResponse) {
        rmResponses.forEach((rmNumber, response) -> {
            boolean isCorrect = correctResponse != null &&
                    correctResponse.equals(response.getResponse());
            updateRmStatus(rmNumber, true, isCorrect);
        });

        // Check for RMs that didn't respond
        for (int rmNumber = 1; rmNumber <= 4; rmNumber++) {
            if (!rmResponses.containsKey(rmNumber)) {
                updateRmStatus(rmNumber, false, false);
            }
        }
    }

    private void handleTotalFailure(FERequest request) {
        System.out.println("DSMS_Implementation : No responses received for request: " + request);

        if (request.haveRetries()) {
            request.countRetry();
            retryRequest(request);
        }

        // Mark all RMs as potentially down
        for (int rmNumber = 1; rmNumber <= 4; rmNumber++) {
            updateRmStatus(rmNumber, false, false);
        }
    }

    // RM status Management
    private void updateRmStatus(int rmNumber, boolean responded, boolean correct) {
        if (!responded) {
            int count = rmNoResponseCounts.merge(rmNumber, 1, Integer::sum);
            if (count >= FAILURE_THRESHOLD) {
                System.out.println("DSMS_Implementation : RM" + rmNumber + " marked as down");
                rmNoResponseCounts.put(rmNumber, 0);
                if (inter != null) {
                    inter.reportReplicaManagerDown(rmNumber);
                }
            }
        } else if (!correct) {
            int count = rmBugCounts.merge(rmNumber, 1, Integer::sum);
            if (count >= FAILURE_THRESHOLD) {
                System.out.println("DSMS_Implementation : RM" + rmNumber + " marked as buggy");
                rmBugCounts.put(rmNumber, 0);
                if (inter != null) {
                    inter.reportBugInReplicaManager(rmNumber);
                }
            }
        } else {
            rmNoResponseCounts.put(rmNumber, 0);
            rmBugCounts.put(rmNumber, 0);
        }
    }

    // UDP Listener
    private void listenForUDPResponses() {
        final int MAX_UDP_PACKET_SIZE = 65507;
        try (DatagramSocket socket = new DatagramSocket(4545, InetAddress.getByName(FE_IP_Address));) {

            byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
            System.out.println("FRONT END Started on " + InetAddress.getByName(FE_IP_Address) + " : " + FE_IP_Address);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String response = new String(packet.getData(), 0, packet.getLength()).trim();
                    ServerResponse RmResponse = new ServerResponse(response);
                    System.out.println("DSMS_Implementation :Response received from Replica "+ RmResponse.getRmNumber()+ "->" + response);

                    System.out.println("DSMS_Implementation : Receiving The Response ");
                    addReceivedResponse(RmResponse);
                } catch (IOException e) {
                    System.err.println("Error processing UDP packet: " + e.getMessage());
                }

            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }


    }


}

