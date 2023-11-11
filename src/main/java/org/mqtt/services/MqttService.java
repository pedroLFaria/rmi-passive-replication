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

    public MqttService(){
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            mqttPublisher = new MqttClient(broker, publisherId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            connOpts.setConnectionTimeout(10);
            System.out.println("Connecting to broker: " + broker);
            mqttPublisher.connect(connOpts);
            System.out.println("Connected");
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public void subscribe(){
        subscribe(topic);
    }

    public void subscribe(String topic){
        try {
            mqttPublisher.subscribe(topic);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }
    public void publish(String message){
        publish(topic, message);
    }

    public void publish(String topic, String message){
        try {
            var mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            mqttPublisher.publish(topic, mqttMessage);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMqttCallBack(MqttCallback callBack){
        mqttPublisher.setCallback(callBack);
    }
}
