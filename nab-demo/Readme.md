This project for Learning and Interview purpose.

Problem Statement
1. Bank ABC want to provide a new feature on its website. The feature is to purchase prepaid data
   for a SIM card by getting a voucher code from a 3rd party. Anyone can purchase the prepaid data
   on the website without login.
2. The 3rd party provides an API for the client to call via HTTP(s) protocol to get the voucher code
   The API always returns a voucher code after 3 to 120 seconds, it depends on the network traffic
   at that time.
3. The bank wants to build a new service(s) to integrate with that 3rd party. But it expects that the
   API will return voucher code or a message that says the request is being processed within 30
   seconds.
4. If the web application receives the voucher code, it will show on the website for customers to use.
   In case that the code can't be returned in-time, the customer can receive it via SMS later.
5. The customer can check all the voucher codes that they have purchased by phone number on the
   website, but it needs to be secured.
6. Assume that the payment has been done before the website call to the services to get the voucher
   code.

How to start project:
- Maven build: mvn clean install
- Starting with java stand alone: cd target && java -jar *.jar

Notes:
The spring boot application using in-memory h2 database, also supported swagger UI and h2 UI.
Default app will start on port 9090, for interaction, for more detail used please take a look at application.yml.
Please use http://localhost:9090/swagger for using APIs and http://localhost:9090/h2-console for managing database.

About technologies and library used:
- Spring boot
- Spring security
- Spring actuator for application health check.
- Spring webflux for connection to client/service using WebClient
- Spring doc for document API and exposing swagger-ui
- Spring data jpa + h2database for storing data
- Spring retry for handling errors and recover system.
- Lombok for support coding