package assignment2.servlet.util;

public class Pair {
  private boolean isUrlPathValid;
  private String direction;

  public Pair(boolean isUrlPathValid, String direction) {
    this.isUrlPathValid = isUrlPathValid;
    this.direction = direction;
  }

  public boolean isUrlPathValid() {
    return isUrlPathValid;
  }

  public String getDirection() {
    return direction;
  }
}
