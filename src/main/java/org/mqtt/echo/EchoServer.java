package org.mqtt.echo;

import org.mqtt.services.MqttService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class EchoServer extends UnicastRemoteObject implements Echo {

    private List<String> messages = new ArrayList<>();
    private final MqttService mqttService;

    public EchoServer(MqttService mqttService) throws RemoteException {
        super();
        this.mqttService = mqttService;
    }

    public void addMessage(String message) {
        messages.add(message);
    }

    @Override
    public String echo(String message) throws RemoteException {
        messages.add(message);
        mqttService.publish(message);
        return message;
    }

    @Override
    public List<String> getListOfMsg() throws RemoteException {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public void healthCheck() throws RemoteException {
    }

}
