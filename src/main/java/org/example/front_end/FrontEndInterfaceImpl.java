package org.example.front_end;

import javax.xml.ws.Endpoint;

public class FrontEndInterfaceImpl {
    public static String FE_IP_Address = "192.168.230.151";
    public static final int FRONT_END_PORT = 4555;
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
      String url ="http://" + FE_IP_Address +":" + FRONT_END_PORT +"/FrontEnd";
      Endpoint.publish(url, dsmsInterface);
      System.out.println("Front-end service is ready !");

  } catch (Exception e) {
      throw new RuntimeException(e);
  }
    }



    private static int sendUnicastToSequencer(FERequest requestFromClient) {
       //TODO:  add UDP Service to sequencer
        return 0;
    }
}
