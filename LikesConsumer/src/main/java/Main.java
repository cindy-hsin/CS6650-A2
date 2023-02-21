import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
  private final static int NUM_THREADS = 10;
  private final static Map<String, String> RMQ_SERVER_CONFIG  = Stream.of(new String[][] {
      { "userName", "cindychen" },
      { "password", "password" },
      { "virtualHost", "swipe_broker" },
      { "hostName", "34.221.254.107" },
      { "portNumber", "5672" }      // 5672 is for RabbitMQ server. 15672 is to access management console. https://stackoverflow.com/a/69523757
  }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

  public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(NUM_THREADS);
    ConnectionFactory connectionFactory = new ConnectionFactory();
    // Connect to RMQ server. Ref: https://www.rabbitmq.com/api-guide.html#connecting
    connectionFactory.setUsername(RMQ_SERVER_CONFIG.get("userName"));
    connectionFactory.setPassword(RMQ_SERVER_CONFIG.get("password"));
    connectionFactory.setVirtualHost(RMQ_SERVER_CONFIG.get("virtualHost"));
    connectionFactory.setHost(RMQ_SERVER_CONFIG.get("hostName"));
    connectionFactory.setPort(Integer.valueOf(RMQ_SERVER_CONFIG.get("portNumber")));

    Connection connection = connectionFactory.newConnection();

    for (int i = 0; i < NUM_THREADS; i++) {
      Runnable thread = new ConsumerThread(connection, latch);
      new Thread(thread).start();
    }

    latch.await();
    System.out.println("Closed all LikesConsumer Threads.");
  }
}