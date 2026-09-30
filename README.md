# notification-system
Scalable notification server

Design:
- Notification server in Java using spring boot starter web server, maven. No unit tests are needed.
- Server should be able to be integrated with third party service such as Apple push Notification service,
  Firebase cloud messaging, Twilio, Nexmo, Sendgrid and Mailchimp.
- For simplicity stub out these third-party services. Use 4 third party service stubs (APN for IOS, FCM for Android, SMS for sms and EMAIL for email)
- Use an API to collect user contact details and store the data in postgresql database.
- Have entities such as User, Device, Notification settings etc., in the DB.
- Notification service should expose API so that other services such as a micro-service, cron job or a distributed system that triggers
  notification sending events can consume the API.
- Notification server should build notification payloads for sending to third party services.
- Notification service should be stateless and should be easily scalable horizontally.
- Use a redis cache for storing user, notification payload and device details.
- There will be separate Rabbitmq message queue for android, IOS push notifications, SMS queue and Email Queue
- Notification service must query the cache or the database to fetch data needed to render a notification.
- Put notification data to message queues for parallel processing.
- Cache contains user info, device info and notification templates.
- DB contains user data, notification settings, device info and templates.
- Workers are a list of servers that pull notification events from the message queue and send them to the corresponding third party service.
- Notification server must retry sending notifications if there is a failure.
- Use a customizable single and simple notification template.
- Notification settings table can contains fields like user_id, channel (push notifications, email or SMS), opt_in etc.
- If the third-party service fails to send a notification, then the notification will be added to the message queue for retrying.
- Notification service must check if the users have opted in for receiving a specific notification type before sending the notification.      
