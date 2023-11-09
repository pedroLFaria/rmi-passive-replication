package org.mqtt.echo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Echo extends Remote {
    String echo(String message) throws RemoteException;
    List<String> getListOfMsg() throws RemoteException;
    void healthCheck() throws RemoteException;
}
