package org.mqtt.echo;

public enum ServerTypeEnum {
    MASTER,
    CLONE;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
