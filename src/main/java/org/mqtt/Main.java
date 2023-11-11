package org.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.mqtt.echo.Echo;
import org.mqtt.rmi.Hello;

import javax.swing.*;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class Main {
    public static void main(String[] args) {
        connectToMqtt();
    }

    private static void connectToMqtt() {
        var topic = "mqtt-topic";
        var content = "Teste xuxa";
        var qos = 2;
        var broker = "tcp://127.0.0.1:1883";
        var publisherId = "mqtt-publisher";
        var persistence = new MemoryPersistence();

        try {
            var mqttPublisher = new MqttClient(broker, publisherId, persistence);
            mqttPublisher.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.printf("Messagem do topico %s chegou: %s", topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
            var connOpts = new MqttConnectOptions();

            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            connOpts.setConnectionTimeout(10);

            System.out.println("Connecting to broker: " + broker);
            mqttPublisher.connect(connOpts);
            System.out.println("Connected");

            System.out.println("Publishing message: " + content);
            var message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            mqttPublisher.publish(topic, message);
            mqttPublisher.subscribe(topic);

            mqttPublisher.publish(topic, message);
            System.out.println("Message published");

            mqttPublisher.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch (MqttException e) {
            System.out.println("Error message: " + e.getMessage());
            System.out.println("Error cause: " + e.getCause());
            e.printStackTrace();
        }
    }
}