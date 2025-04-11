package org.example.front_end;

public class FERequest {


    private String method = "null";
    private String userID = "null";
    private String shareType = "null";
    private String oldShareType = "null";
    private String shareID = "null";
    private String oldShareId = "null";
    private String FE_IP_Address = "192.168.230.151";
    private int shareUnits = 0;
    private int sequenceNumber = 0;
    private String MessageType = "00";
    private int retryCount = 1;

    public FERequest(String method, String userID) {
        setMethod(method);
        setUserID(userID);
    }

    public FERequest(int rmNumber, String bugType) {
        setMessageType(bugType + rmNumber);
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getShareType() {
        return shareType;
    }

    public void setShareType(String shareType) {
        this.shareType = shareType;
    }

    public String getOldShareType() {
        return oldShareType;
    }

    public void setOldShareType(String oldShareType) {
        this.oldShareType = oldShareType;
    }

    public String getShareID() {
        return shareID;
    }

    public void setShareID(String shareID) {
        this.shareID = shareID;
    }

    public String getOldShareId() {
        return oldShareId;
    }

    public void setOldShareId(String oldShareId) {
        this.oldShareId = oldShareId;
    }

    public int getShareUnits() {
        return shareUnits;
    }

    public void setShareUnits(int shareUnits) {
        this.shareUnits = shareUnits;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getMessageType() {
        return MessageType;
    }

    public void setMessageType(String messageType) {
        MessageType = messageType;
    }

    public String getFE_IP_Address() {
        return FE_IP_Address;
    }

    public boolean haveRetries() {
        return retryCount > 0;
    }

    public void setFE_IP_Address(String FE_IP_Address) {
        this.FE_IP_Address = FE_IP_Address;
    }

    public void countRetry() {
        retryCount--;
    }

    public String noRequestSendError() {
        return "request: " + getMethod() + " from " + getUserID() + " not sent";
    }

    public String toString() {
        return getSequenceNumber() + ";" +
                getFE_IP_Address().toUpperCase() + ";" +
                getMessageType().toUpperCase() + ";" +
                getMethod().toUpperCase() + ";" +
                getUserID().toUpperCase() + ";" +
                getShareID().toUpperCase() + ";" +
                getShareType().toUpperCase() + ";" +
                getOldShareId().toUpperCase() + ";" +
                getOldShareType().toUpperCase() + ";" +
                getShareUnits();
    }
}
