package org.mqtt.echo;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EchoServer extends UnicastRemoteObject implements Echo {
    private List<String> messages = new ArrayList<>();

    protected EchoServer() throws RemoteException {
        super();
    }

    @Override
    public String echo(String message) throws RemoteException {
        messages.add(message);
        return message;
    }

    @Override
    public List<String> getListOfMsg() throws RemoteException {
        return new ArrayList<>(messages);
    }

    @Override
    public void healthCheck() throws RemoteException {
    }




}
