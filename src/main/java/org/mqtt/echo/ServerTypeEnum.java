package org.mqtt.echo;

public enum ServerTypeEnum {
    REGISTRY,
    MASTER,
    CLONE;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
