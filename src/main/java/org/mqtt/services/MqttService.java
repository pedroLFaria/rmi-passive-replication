package org.mqtt.services;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttService {
    public static String topic = "mqtt-topic";
    //Quality of Service = 2 offers the highest level of service in MQTT
    public static int qos = 2;
    public static String broker = "tcp://127.0.0.1:1883";
    public static String publisherId = "mqtt-publisher";
    private final MqttClient mqttPublisher;
    private MqttConnectOptions connOpts;

    public MqttService(String clientId){
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            mqttPublisher = new MqttClient(broker, clientId, persistence);
            connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            connOpts.setConnectionTimeout(10);
            
            mqttPublisher.connect(connOpts);
            
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public void connect() throws MqttSecurityException, MqttException{
        System.out.println("Connecting to broker: " + broker);
        mqttPublisher.connect(connOpts);
        System.out.println("Connected");
    }

    public void disconnect() throws MqttException{
        System.out.println("Disconnecting from broker: " + broker);
        mqttPublisher.disconnect();
        System.out.println("Disconnected");
    }

    public void subscribe() throws MqttException{
        subscribe(topic);
    }

    public void subscribe(String topic) throws MqttException {
        try {
            System.out.println("Subscribing to topic " + topic);
            mqttPublisher.subscribe(topic);
            System.out.println("Subscribed");
        } catch (MqttException e) {
            System.out.println("Failure in subscribing to topic " + topic);
            throw new MqttException(e);
        }
    }

    public void unsubscribe() throws MqttException{
        unsubscribe(topic);
    }

    public void unsubscribe(String topic) throws MqttException {
        try {
            System.out.println("Unsubscribing to topic " + topic);
            mqttPublisher.unsubscribe(topic);
            System.out.println("Unsubscribed");
        } catch (MqttException e) {
            System.out.println("Failure in Unsubscribing to topic " + topic);
            throw new MqttException(e);
        }
    }

    public void publish(String message) {
        try {
            publish(topic, message);
        } catch (MqttException e) {
            System.out.println("Failiure on sending message!");
            throw new RuntimeException(e);
        }
    }

    public void publish(String topic, String message) throws MqttException{
        var mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(qos);
        System.out.println("Publishing message to topic: " + topic);
        mqttPublisher.publish(topic, mqttMessage);
        System.out.println("Published");
    }

    public void setMqttCallBack(MqttCallback callBack){
        mqttPublisher.setCallback(callBack);
    }
}
