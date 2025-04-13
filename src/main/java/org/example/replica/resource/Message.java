package org.example.replica.resource;

public class Message implements Comparable<Message> {
    private Integer sequenceID;
    private String frontEndIpAddress;
    private String messageType;
    private String operationType;
    private String userId;
    private String oldShareID;
    private String oldShareType;
    private String newShareID;
    private String newShareType;
    private Integer units;

    public Message(Integer sequenceID, String frontEndIpAddress, String messageType, String operationType, String userId, String newShareType) {
        this.sequenceID = sequenceID;
        this.frontEndIpAddress = frontEndIpAddress;
        this.messageType = messageType;
        this.operationType = operationType;
        this.userId = userId;
        this.newShareType = newShareType;
    }

    public Message(Integer sequenceID, String frontEndIpAddress, String messageType, String operationType, String userId, String oldShareID, String oldShareType) {
        this.sequenceID = sequenceID;
        this.frontEndIpAddress = frontEndIpAddress;
        this.messageType = messageType;
        this.operationType = operationType;
        this.userId = userId;
        this.oldShareID = oldShareID;
        this.oldShareType = oldShareType;
    }

    public Message(Integer sequenceID, String frontEndIpAddress, String messageType, String operationType, String userId, String oldShareID, Integer units) {
        this.sequenceID = sequenceID;
        this.frontEndIpAddress = frontEndIpAddress;
        this.messageType = messageType;
        this.operationType = operationType;
        this.userId = userId;
        this.oldShareID = oldShareID;
        this.units = units;
    }

    public Message(Integer sequenceID, String frontEndIpAddress, String messageType, String operationType, String userId, String newShareID, String newShareType, Integer units) {
        this.sequenceID = sequenceID;
        this.frontEndIpAddress = frontEndIpAddress;
        this.messageType = messageType;
        this.operationType = operationType;
        this.userId = userId;
        this.newShareID = newShareID;
        this.newShareType = newShareType;
        this.units = units;
    }

    public Message(Integer sequenceID, String frontEndIpAddress, String messageType, String operationType, String userId, String oldShareID, String oldShareType, String newShareID, String newShareType) {
        this.sequenceID = sequenceID;
        this.frontEndIpAddress = frontEndIpAddress;
        this.messageType = messageType;
        this.operationType = operationType;
        this.userId = userId;
        this.oldShareID = oldShareID;
        this.oldShareType = oldShareType;
        this.newShareID = newShareID;
        this.newShareType = newShareType;
    }

    public Message(Integer sequenceID, String frontEndIpAddress, String messageType, String operationType, String userId, String oldShareID, String oldShareType, String newShareID, String newShareType, Integer units) {
        this.sequenceID = sequenceID;
        this.frontEndIpAddress = frontEndIpAddress;
        this.messageType = messageType;
        this.operationType = operationType;
        this.userId = userId;
        this.oldShareID = oldShareID;
        this.oldShareType = oldShareType;
        this.newShareID = newShareID;
        this.newShareType = newShareType;
        this.units = units;
    }

    public Integer getSequenceID() {
        return sequenceID;
    }

    public void setSequenceID(Integer sequenceID) {
        this.sequenceID = sequenceID;
    }

    public String getFrontEndIpAddress() {
        return frontEndIpAddress;
    }

    public void setFrontEndIpAddress(String frontEndIpAddress) {
        this.frontEndIpAddress = frontEndIpAddress;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public Integer getUnits() {
        return units;
    }

    public void setUnits(Integer units) {
        this.units = units;
    }

    @Override
    public String toString() {
        return sequenceID + ";" +
                frontEndIpAddress + ";" +
                messageType + ";" +
                operationType + ";" +
                userId + ";" +
                newShareID + ";" +
                newShareType + ";" +
                oldShareID + ";" +
                oldShareType + ";" +
                units;
    }


    @Override
    public int compareTo(Message message) {
        return Integer.compare(this.sequenceID, message.getSequenceID());
    }
}

