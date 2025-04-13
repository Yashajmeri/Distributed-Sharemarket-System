package org.example.front_end;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static org.example.sequencer.Config.FRONT_END;
import static org.example.sequencer.Config.FRONT_END_SQ;
import static org.example.sequencer.Config.SEQUENCER;

public class FrontEndInterfaceImpl {
    public static String FE_IP_Address = FRONT_END.getIpAddress();
    public static final int FRONT_END_PORT = FRONT_END.getPortNumber();

    public static void main(String[] args) {
        try {
            FrontEndInterface frontEndInterface = new FrontEndInterface() {
                @Override
                public void reportBugInReplicaManager(int rmNumber) {
                    FERequest errorMessage = new FERequest(rmNumber, "1");
                    System.out.println("Rm:" + rmNumber + "has bug");
                    sendUnicastToSequencer(errorMessage);
                }

                @Override
                public void reportReplicaManagerDown(int rmNumber) {
                    FERequest errorMessage = new FERequest(rmNumber, "2");
                    System.out.println("Rm:" + rmNumber + "has bug");
                    sendUnicastToSequencer(errorMessage);
                }

                @Override
                public int forwardRequestToSequencer(FERequest request) {

                    return sendUnicastToSequencer(request);
                }

                @Override
                public void retryRequest(FERequest request) {
                    System.out.println("No response from all Rms, Retrying request...");
                    sendUnicastToSequencer(request);
                }
            };

            DSMSInterfaceImpl dsmsInterface = new DSMSInterfaceImpl(frontEndInterface);
            String url = "http://" + FE_IP_Address + ":" + FRONT_END_PORT + "/FrontEnd";
            Endpoint.publish(url, dsmsInterface);
            System.out.println("Front-end service is ready !");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static int sendUnicastToSequencer(FERequest requestFromClient) {
        int sequenceID = 0;
        if (requestFromClient == null) {
            throw new IllegalArgumentException("requestFromClient cannot be null");
        }
        System.out.println("DSMS_Implementation:sendUnicastToSequencer ->" + requestFromClient.toString());
        try (DatagramSocket aSocket = new DatagramSocket(FRONT_END.getSocketPortNumber())) {
            String dataFromClient = requestFromClient.toString();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName(SEQUENCER.getIpAddress());
            DatagramPacket requestToSequencer = new DatagramPacket(message, message.length, aHost, SEQUENCER.getSocketPortNumber());
            aSocket.send(requestToSequencer);
            aSocket.setSoTimeout(1000);
            // Receiving
            byte[] receiveMessage = new byte[1000];
            DatagramPacket response = new DatagramPacket(receiveMessage, receiveMessage.length);
            aSocket.receive(response);
            String responseFromSequencer = new String(response.getData(), 0, response.getLength());
            System.out.println("DSMS_Implementation:sendUnicastToSequencer -> " + responseFromSequencer);
            sequenceID = Integer.parseInt(responseFromSequencer.trim());
            System.out.println("DSMS_Implementation:sendUnicastToSequencer : Sequence ID -> " + sequenceID);

        } catch (SocketException e) {
            System.out.println("Failed: " + requestFromClient.noRequestSendError());
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Failed: " + requestFromClient.noRequestSendError());
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        }
        return sequenceID;
    }
}
