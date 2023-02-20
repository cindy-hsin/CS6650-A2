import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.concurrent.TimeoutException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import datamodel.*;
import com.google.gson.Gson;
import rmqpool.RMQChannelFactory;
import rmqpool.RMQChannelPool;

@WebServlet(name = "SwipeServlet", value = "/swipe")
public class SwipeServlet extends HttpServlet {
  private final static String LEFT = "left";
  private final static String RIGHT = "right";

  private final static int CHANNEL_POOL_SIZE = 10; // TODO: ?? Sensible Range??
  private final static String QUEUE_SERVER_URL = "localhost";

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
    this.validateRequestBody(request, response, responseMsg, gson);

    // If everything is valid, send the Swipe data to RabbitMQ queue
    if (response.getStatus() == HttpServletResponse.SC_CREATED) {

    }


    // Send the response status & message back to client
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


  private void validateRequestBody(HttpServletRequest request, HttpServletResponse response, ResponseMsg responseMsg, Gson gson) {
    try {
      StringBuilder sb = new StringBuilder();
      String s;
      while ((s = request.getReader().readLine()) != null) {
        sb.append(s);
      }

      SwipeDetails swipeDetails = (SwipeDetails) gson.fromJson(sb.toString(), SwipeDetails.class);

      if (!swipeDetails.isSwiperValid()) {
        responseMsg.setMessage("User not found: invalid swiper id: "+ swipeDetails.getSwiper());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else if (!swipeDetails.isSwipeeValid()) {
        responseMsg.setMessage("User not found: invalid swipee id: " + swipeDetails.getSwipee());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else if (!swipeDetails.isCommentValid()) {
        responseMsg.setMessage("Invalid inputs: comment cannot exceed 256 characters");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } else {
        responseMsg.setMessage("Write successful!");
        response.setStatus(HttpServletResponse.SC_CREATED);
      }
    } catch (IOException e) {
      e.printStackTrace();
      responseMsg.setMessage(e.getMessage());
    }

  }
  /**
   *
   * Send Message to Queue
   * Ref: Ian's book P136
   */

  private boolean sendMessageToQueue(JsonObject message) {
    try {
      Channel channel = pool.borrowObject();
      channel.basicPublish(// arguments omitted for brevity)
          pool.returnObject(channel);
      return true;
    } catch (Exception e) {
      logger.info("Failed to send message to RabbitMQ");
      return false;
    }
  }

}
