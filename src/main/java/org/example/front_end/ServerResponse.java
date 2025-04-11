package org.example.front_end;

public class ServerResponse {
    private int sequenceID = 0;
    private String response = "null";
    private int rmNumber = 0;
    private String method = "null";
    private String userID = "null";
    private String newShareID = "null";
    private String newShareType = "null";
    private String oldShareID = "null";
    private String oldShareType = "null";


    public ServerResponse(String udpMessage) {
        setUdpMessage(udpMessage.trim());
        String[] responseParts = getUdpMessage().split(";");
        setSequenceID(Integer.parseInt(responseParts[0]));
        setResponse(responseParts[1].trim());
        setRmNumber(responseParts[2]);
        setMethod(responseParts[3].trim());
        setUserID(responseParts[4].trim());
        setNewShareID(responseParts[5].trim());
        setNewShareType(responseParts[6].trim());
        setOldShareID(responseParts[7].trim());
        setOldShareType(responseParts[8].trim());
        setShareUnits(Integer.parseInt(responseParts[9].trim()));

    }


    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    private int shareUnits = 0;
    private String udpMessage = "null";

    public void setSequenceID(int sequenceID) {
        this.sequenceID = sequenceID;
    }

    public void setResponse(String response) {
        isSuccess = response.contains("SUCCESS");
        this.response = response;
    }

    public void setRmNumber(String rmNumber) {

        if (rmNumber.equalsIgnoreCase("RM1")) {
            this.rmNumber = 1;
        } else if (rmNumber.equalsIgnoreCase("RM2")) {
            this.rmNumber = 2;
        } else if (rmNumber.equalsIgnoreCase("RM3")) {
            this.rmNumber = 3;
        } else if (rmNumber.equalsIgnoreCase("RM4")) {
            this.rmNumber = 4;
        } else {
            this.rmNumber = 0;
        }
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getNewShareID() {
        return newShareID;
    }

    public void setNewShareID(String newShareID) {
        this.newShareID = newShareID;
    }

    public String getNewShareType() {
        return newShareType;
    }

    public void setNewShareType(String newShareType) {
        this.newShareType = newShareType;
    }

    public String getOldShareID() {
        return oldShareID;
    }

    public void setOldShareID(String oldShareID) {
        this.oldShareID = oldShareID;
    }

    public String getOldShareType() {
        return oldShareType;
    }

    public void setOldShareType(String oldShareType) {
        this.oldShareType = oldShareType;
    }

    public int getShareUnits() {
        return shareUnits;
    }

    public void setShareUnits(int shareUnits) {
        this.shareUnits = shareUnits;
    }

    public String getUdpMessage() {
        return udpMessage;
    }

    public void setUdpMessage(String udpMessage) {
        this.udpMessage = udpMessage;
    }

    private boolean isSuccess = false;

    public String getResponse() {
        return "";
    }

    public int getSequenceID() {
        return 0;
    }

    public Integer getRmNumber() {
        return 0;
    }
public boolean equals(Object obj) {
    if (obj != null) {
    if(obj instanceof ServerResponse) {
       ServerResponse sr = (ServerResponse)obj;
return sr.getMethod().equals(this.getMethod())
        && sr.getUserID().equals(this.getUserID())
        && sr.sequenceID == this.getSequenceID()
        && sr.isSuccess == this.isSuccess;
    }
    }
return false;
}
}
