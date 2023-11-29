# RMI Passive Replication Project Documentation

This document provides a comprehensive guide to the implementation of a passive replication system using Remote Method Invocation (RMI) and Mosquitto. The project demonstrates the basic application of RMI combined with Mosquitto for achieving passive replication.

## System Requirements

For the successful execution of this project, the following prerequisites must be met:

- **Maven:** Essential for building and managing the project.
- **Java Version:** The project is compatible with Java 11.
- **MQTT Service or Docker:** An active MQTT service is required. This can be achieved either by running the service independently or using Docker Compose for containerization.

It is important to ensure that the MQTT service is operational before proceeding with project deployment.

## Installation and Deployment Instructions

### Docker Utilization

If Docker is preferred for deployment, the following commands should be executed:

```bash
docker compose up --build
docker compose run mqtt-pub sh -c "mosquitto_pub -h mqtt-broker -t mqtt-topic"
```

These commands build and deploy the necessary Docker containers for the MQTT service.

### Project Build

The project can be built using Maven with the command:

```bash
mvn clean package
```

### Server Initialization

After building the project, the servers can be initiated be running the following command repetitive:
```bash
   java -cp target/mqtt-project-1.0-SNAPSHOT.jar org/mqtt/servers/ServerApp
   ```
1. **Registry Service:** The first server initiated serves as the Registry service on port 8088.   

2. **Master Server:** The second server initiated functions as the Master server.

3. **Clone Servers:** Subsequent server instances will act as Clones, denoted as Clones/{i}, where 'i' represents the iteration number.

Additionally, a Registry service can be manually initiated by executing `rmiregistry 8088` in the `target/classes` directory. The system is designed to automatically detect the Registry operating on the specified port and function accordingly.

### Client Application Execution

To simulate client requests to the services, execute the following command:

```bash
java -cp target/mqtt-project-1.0-SNAPSHOT.jar org/mqtt/servers/ClientApp
```

This command activates the command-line client interface for interacting with the services.

---

## References

 - [MQTT Broker with Docker](https://dev.to/abbazs/a-step-by-step-guide-for-starting-a-mosquitto-broker-service-in-a-containers-with-docker-compose-1j8i)
 - [MQTT Client in Java](https://www.baeldung.com/java-mqtt-client)


## License

[MIT](https://choosealicense.com/licenses/mit/)
