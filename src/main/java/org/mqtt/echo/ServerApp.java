package org.mqtt.echo;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerApp {
    static String host = "//localhost";
    static String port = "8088";
    static String serviceName = "EchoServer";
    static MqttService mqttService;
    static ServerTypeEnum serverType;
    static int cloneId = 0;

    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname","127.0.0.1");
            initiateRegisty();
            mqttService = new MqttService();
            EchoServer echoServer = new EchoServer(mqttService);
            String serverName = bindName(echoServer);
            if(notMaster(serverName))healthCheckMaster();
            System.out.println("ObjetoServidor esta ativo! Com nome de servidor: "+ serverName);
        } catch (Exception e) {
            System.err.println("Exceção no servidor Echo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean notMaster(String serverName) {
        return !serverName.equals("//localhost:8088/EchoServer/master");
    }

    private static void initiateRegisty(){
        try {
            LocateRegistry.createRegistry(8088);
        } catch (ExportException e) {
            System.out.println("Registry já iniciado!");
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


    private static void healthCheckMaster() throws RemoteException {
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                Echo objetoRemoto = (Echo) Naming.lookup(":/master");
                while(true) {
                    objetoRemoto.healthCheck();
                    Thread.sleep(6000);
                    System.out.println("Server Master still alive!");
                }
            } catch (RemoteException e) {
                electeNewMaster();
            } catch (MalformedURLException | NotBoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, 10, TimeUnit.SECONDS);

    }

    private static void shouldBeNewMaster() {
        
    }

    private static String bindName(EchoServer echoServer) throws MalformedURLException, RemoteException {
        serverType = ServerTypeEnum.MASTER;
        String fullAddress = getFullAddress();
        try {
            Naming.lookup(fullAddress);
        } catch (NotBoundException e) {
            Naming.rebind(fullAddress, echoServer);
            return fullAddress;
        }
        return bindClone(echoServer);
    }

    private static String bindClone(EchoServer echoServer) throws MalformedURLException, RemoteException {
        serverType = ServerTypeEnum.CLONE;
        cloneId++;
        String fullAddress = getFullAddress();
        try {
            Naming.lookup(fullAddress);
        } catch (NotBoundException e) {
            Naming.rebind(fullAddress, echoServer);
            return fullAddress;
        }
        mqttService.subscribe();
        mqttService.setMqttCallBack(callback(echoServer));
        return bindClone(echoServer);
    }

    private static String getFullAddress(){
        if(serverType.equals(ServerTypeEnum.MASTER)){
            return String.format("%s:%s/%s/%s", host, port, serviceName, serverType);
        }
        return String.format("%s:%s/%s/%s/%d", host, port, serviceName, serverType, cloneId);            
    }

    private static MqttCallback callback(EchoServer echoServer){
        return new MqttCallback(){

            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("Disconected from Mqtt Server!");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                echoServer.addMessage(mqttMessage.toString());
                System.out.println("Mensagem adicionada no servidor: " + mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        };
    }
}
