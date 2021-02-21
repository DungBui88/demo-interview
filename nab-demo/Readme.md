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