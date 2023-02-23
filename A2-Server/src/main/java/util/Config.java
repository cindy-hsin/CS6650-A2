package util;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Config {
  public final static int CHANNEL_POOL_SIZE = 10; // TODO: ?? Sensible Range??
  public final static Map<String, String> RMQ_SERVER_CONFIG  = Stream.of(new String[][] {
      { "userName", "cindychen" },
      { "password", "password" },
      { "virtualHost", "swipe_broker" },
      { "hostName", "35.90.239.15" },
      { "portNumber", "5672" }      // 5672 is for RabbitMQ server. 15672 is to access management console. https://stackoverflow.com/a/69523757
  }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

  public final static String EXCHANGE_NAME = "swipedata";
}
