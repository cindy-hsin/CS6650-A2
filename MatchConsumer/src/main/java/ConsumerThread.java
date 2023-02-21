import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ConsumerThread implements Runnable{

  private static final String QUEUE_NAME = "match";
  private static final String EXCHANGE_NAME = "swipedata";
  private Connection connection;
  private CountDownLatch latch;


  public ConsumerThread(Connection connection, CountDownLatch latch) {
    this.connection = connection;
    this.latch = latch;
  }

  @Override
  public void run() {
    try {
      final Channel channel = connection.createChannel();

      // Declare exchange in Consumer side as well,
      // in case Consumer is started before Producer(Servlet).
      channel.exchangeDeclare(EXCHANGE_NAME, "fanout",true); // Durable, consistent with Server

      // Durable, Non-exclusive(Can be shared across different channels),
      // Non-autoDelete, classic queue.
      channel.queueDeclare(QUEUE_NAME, true, false, false, new HashMap<>(Map.of("x-queue-type", "classic")));
      channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");   // No Routing key in fanout mode

      // Max one message per consumer (to guarantee even distribution)
      channel.basicQos(1);
      System.out.println(" [*] MatchConsumer Thread waiting for messages. To exit press CTRL+C");

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        // Store data into a thread-safe hashmap
        // TODO: What kind of message do we get?

        // Manual Acknowledgement
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        System.out.println( "Callback thread ID = " + Thread.currentThread().getId() + " Received '" + message + "'");
      };

      // No autoAck, to ensure that Consumer only acknowledges Queue after the message got processed succesfully.
      // Nolocal (TODO: confirm): If nolocal, means that the server will not send messages to the connection that published them.
      // IsNot exclusive. If exclusive, queues may only be accessed by the current connection. (But we want Another Consumer to access this queue as well)
      // server-generated consumerTag
      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});


      //TODO: Set a timeout for this thread to close?? (After a period of time, if there is no more msg form the queue,
      // then close the thread by calling latch.countDown())

    } catch (IOException e) {
      Logger.getLogger(ConsumerThread.class.getName()).log(Level.SEVERE, null, e);
    }
  }
}
