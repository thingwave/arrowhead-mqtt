# Eclipse Arrowhead MQTT Example

## Building
Execute
```
mvn package
```
to build the software.

## Configuration
Edit app.properties in the root folder. It should look something like this:
```
systemName=testsys1
cafile=testcloud2.crt
certfile=mqttbroker.crt
keyfile=mqttbroker2.key

brokerUrl=ssl://127.0.0.1:8883
topic=testsys1/temperature

username=serviceregistry
password=secretpassword
```

## Testing

To run the application, execute the following command:
```
java -jar target/arrowheadmqtt-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Next steps 

