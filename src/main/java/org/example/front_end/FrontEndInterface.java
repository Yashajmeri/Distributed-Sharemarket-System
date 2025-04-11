package org.example.front_end;

public interface FrontEndInterface {
    void reportBugInReplicaManager(int rmNumber);

    void reportReplicaManagerDown(int rmNumber);

    int forwardRequestToSequencer(FERequest request);

    void retryRequest(FERequest request);
}
