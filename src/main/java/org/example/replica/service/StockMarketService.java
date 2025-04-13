package org.example.replica.service;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface StockMarketService {
    String addShare (String shareID, String shareType, int capacity);
    String removeShare (String shareID, String shareType);
    String listShareAvailability (String shareType);
    String purchaseShare (String buyerID, String shareID, String shareType, int units);
    String getShares (String buyerID);
    String sellShare (String buyerID, String shareID, int units);
    String swapShares (String buyerID, String oldShareID, String oldShareType, String newShareID, String newShareType);
}
