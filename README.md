# Toy Client For IBM MQ

## To run 
Modify source code to fit your specific needs then: 

`mvn spring-boot:run`

## Usage

### SEND

`curl 'http://localhost:8081/send?dType=Q&dName=DEV.QUEUE.1&mType=text'`

`curl 'http://localhost:8081/send?dType=T&dName=Topic&mType=bytes'`

### READ From Queue

`curl 'http://localhost:8081/recv?dName=DEV.QUEUE.1'`

Where:

`dType`: Destination type. Valid values are 'T' and 'Q'

`dName`: Destination name.

`mType`: Message type. Valid values are 'text', 'bytes', 'map', 'stream', 'object', 'noPayload'

### Measure Performance

Note: All query parameters can be omitted. Defaults match values in the samples below

First, spool messages on a queue: 

`curl -X POST 'http://localhost:8081/performance?dName=DEV.QUEUE.1&numMsgs=10000&numBytes=1024'`

and then read them back:

`curl -X GET 'http://localhost:8081/performance?dName=DEV.QUEUE.1&numMsgs=10000&transacted=true'`


