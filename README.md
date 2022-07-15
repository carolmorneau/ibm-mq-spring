# Toy Client For IBM MQ

##To run: 
Modify source code to fit your specific needs then: 

`mvn spring-boot:run`

## Usage

SEND

`curl 'http://localhost:8081/send?dType=Q&dName=DEV.QUEUE.1&mType=text'`

READ From Queue

`curl 'http://localhost:8081/recv?dName=DEV.QUEUE.1'`

Where:

`dType`: Destination type. Valid values are 'T' and 'Q'

`dName`: Destination name.

`mType`: Message type. Valid values are 'text', 'bytes', 'map', 'stream', 'object', 'noPayload'


