package org.example.sequencer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static org.example.sequencer.Config.*;

public class Sequencer {

    private static int sequencerID = 0;
    private static final String sequencerIP = SEQUENCER_IP;

    public static void main(String[] args) {
        try (DatagramSocket aSocket = new DatagramSocket(SEQUENCER_PORT, InetAddress.getByName(sequencerIP))) {
            byte[] buffer = new byte[1000];
            System.out.println("Sequencer UDP Server Started");
            while (true) {

                DatagramPacket request = new DatagramPacket(buffer,
                        buffer.length);

                aSocket.receive(request);

                String sentence = new String(request.getData(), 0,
                        request.getLength());

                String[] parts = sentence.split(";");
                int sequencerId1 = Integer.parseInt(parts[0]);
                String ip = request.getAddress().getHostAddress();

                String sentence1 = ip + ";" +
                        parts[2] + ";" +
                        parts[3] + ";" +
                        parts[4] + ";" +
                        parts[5] + ";" +
                        parts[6] + ";" +
                        parts[7] + ";" +
                        parts[8] + ";" +
                        parts[9] + ";";

                System.out.println(sentence1);
                sendMessage(sentence1, sequencerId1, parts[2].equalsIgnoreCase("00"));

                byte[] SeqId = (Integer.toString(sequencerID)).getBytes();
                InetAddress aHost1 = request.getAddress();
                int port1 = request.getPort();

                System.out.println(aHost1 + ":" + port1);
                DatagramPacket request1 = new DatagramPacket(SeqId,
                        SeqId.length, aHost1, port1);
                aSocket.send(request1);
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    public static void sendMessage(String message, int sequencerId1, boolean isRequest) {

        if (sequencerId1 == 0 && isRequest) {
            sequencerId1 = ++sequencerID;
        }
        String finalMessage = sequencerId1 + ";" + message;

        DatagramSocket aSocket = null;
        for (int i = 0; i <= 3; i++) {
            try {
                aSocket = new DatagramSocket();
                byte[] messages = finalMessage.getBytes();
                InetAddress aHost = InetAddress.getByName(Config.getReplicaAddress(i));

                DatagramPacket request = new DatagramPacket(messages,
                        messages.length, aHost, Config.getReplicaPort(i));
                aSocket.send(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
