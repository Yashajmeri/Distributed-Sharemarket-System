package org.example.front_end;


import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.rmi.Remote;

 // WebService Binding is remain to be done
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)

public interface DSMSInterface extends Remote {

    String addShare(String userID, String shareID, String shareType, Integer capacity);

    String removeShare(String userID, String shareID, String shareType);

    String listShareAvailability(String userID, String shareType);

    String purchaseShare(String userID, String buyerID, String shareID, String shareType, Integer units);

    String getShares(String userID, String buyerID);

    String sellShare(String userID, String buyerID, String shareID, Integer units);

    String swapShare(String userID, String buyerID, String oldShareId, String oldShareType, String newShareId, String newShareType);

}
