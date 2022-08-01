#How to run the app
- define step function workflow as defined in stepfunction.json
- run ```docker-compose up -d```
- go to your browser at http://localhost:15672/#/exchanges
- add new exchange with the name sf-exchange (Type = topic)
- set your aws credentials in application.yml
- run RedeemPointsAsync.java 
- in postman call POST http://localhost:8080/redeem-points
- you expect to see log: "Walla, Got result for routing key: success, result" 

