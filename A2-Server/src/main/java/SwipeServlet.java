import com.google.gson.JsonObject;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import datamodel.*;
import com.google.gson.Gson;
import javax.swing.event.DocumentEvent.ElementChange;
import rmqpool.RMQChannelFactory;
import rmqpool.RMQChannelPool;

@WebServlet(name = "SwipeServlet", value = "/swipe")
public class SwipeServlet extends HttpServlet {
  private final static String LEFT = "left";
  private final static String RIGHT = "right";

  private final static int CHANNEL_POOL_SIZE = 10; // TODO: ?? Sensible Range??
  private final static String QUEUE_SERVER_URL = "localhost";
  private final static String EXCHANGE_NAME = "swipedata";

  private RMQChannelPool pool;


  @Override
  public void init() throws ServletException {
    super.init();

    ConnectionFactory connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(QUEUE_SERVER_URL);

    final Connection connection;
    try {
      connection = connectionFactory.newConnection();
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException(e);
    }
    RMQChannelFactory channelFactory = new RMQChannelFactory(connection);

    this.pool = new RMQChannelPool(CHANNEL_POOL_SIZE, channelFactory);
  }

  /**
   * Fully validate the URL and JSON payload
   * If valid, format the incoming **Swipe **data and send it as a payload to a remote queue,
   * and then return success to the client
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {



  }


  private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    ResponseMsg responseMsg = new ResponseMsg();
    Gson gson = new Gson();

    String urlPath = request.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      responseMsg.setMessage("missing path parameter");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      return;
    }

    // check if URL is valid! "left" or right""
    if (!this.isUrlValid(urlPath)) {
      responseMsg.setMessage("invalid path parameter: should be " + LEFT + " or " + RIGHT);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      return;
    }

    // Check if request body/payload is valid, and set corresponding response status & message
    String reqBodyJsonStr = this.getJsonStrFromReq(request);
    boolean isReqBodyValid = this.validateRequestBody(reqBodyJsonStr, response, responseMsg, gson);

    if (!isReqBodyValid) {
      // Send the response status(Failed) & message back to client
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      return;
    }

    // If request body is valid, send the Swipe data to RabbitMQ queue
    if (this.sendMessageToQueue(reqBodyJsonStr)) { //TODO: Check argument type: JsonObject?? String??
      responseMsg.setMessage("Succeeded in sending message to RabbitMQ!");
      response.setStatus(HttpServletResponse.SC_CREATED);
    } else {
      responseMsg.setMessage("Failed to send message to RabbitMQ");
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    response.getOutputStream().print(gson.toJson(responseMsg));
    response.getOutputStream().flush();
  }



  private boolean isUrlValid(String urlPath) {
    /**
     * Check if url path param: {leftorright} has value "left" or "right"
     */
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    String[] urlParts = urlPath.split("/");
    if (urlParts.length == 2 && (urlParts[1].equals(LEFT) || urlParts[1].equals(RIGHT)))
      return true;
    return false;
  }


  private boolean validateRequestBody(String reqBodyJsonStr,HttpServletResponse response, ResponseMsg responseMsg, Gson gson) {
      SwipeDetails swipeDetails = (SwipeDetails) gson.fromJson(reqBodyJsonStr, SwipeDetails.class);

      if (!swipeDetails.isSwiperValid()) {
        responseMsg.setMessage("User not found: invalid swiper id: "+ swipeDetails.getSwiper());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return false;
      } else if (!swipeDetails.isSwipeeValid()) {
        responseMsg.setMessage("User not found: invalid swipee id: " + swipeDetails.getSwipee());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return false;
      } else if (!swipeDetails.isCommentValid()) {
        responseMsg.setMessage("Invalid inputs: comment cannot exceed 256 characters");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return false;
      }

      return true;
  }

  private String getJsonStrFromReq(HttpServletRequest request) throws IOException {
    StringBuilder sb = new StringBuilder();
    String s;
    while ((s = request.getReader().readLine()) != null) {
      sb.append(s);
    }

    return sb.toString();
  }

  /**
   *
   * Send Message to Queue, with a Publish-Subscribe pattern
   * Ref: Ian's book P136
   */

  private boolean sendMessageToQueue(String message) {  //TODO: argument type: JsonObject?? String??
    try {
      Channel channel = this.pool.borrowObject();
      // Declare a durable exchange
      channel.exchangeDeclare(EXCHANGE_NAME, "fanout", true);
      // Publish a persistent message. Route key is ignored ("") in fanout mode.
      channel.basicPublish(EXCHANGE_NAME, "", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes("UTF-8"));
      // TODO: Add Publisher Confirm
      this.pool.returnObject(channel);
      return true;
    } catch (Exception e) {
      Logger.getLogger(SwipeServlet.class.getName()).info("Failed to send message to RabbitMQ");
      return false;
    }
  }

}
