package org.example.structures;

public class NotificationInfo {

  private int clientID;
  private int notificationID;
  private String title;
  private String payload;
  private long fireAt;

  public NotificationInfo(int clientID, int id, String title, String payload, long fireAt) {
    this.clientID = clientID;
    this.notificationID = id;
    this.title = title;
    this.payload = payload;
    this.fireAt = fireAt;
  }

  public int getClientID() {
    return clientID;
  }

  public int getNotificationID() {
    return this.notificationID;
  }

  public String getTitle() {
    return this.title;
  }

  public String getPayload() {
    return this.payload;
  }

  public long getFireAt() {
    return this.fireAt;
  }

  public void setClientID(int clientID) {
    this.clientID = clientID;
  }

  public void setNotificationID(int notificationID) {
    this.notificationID = notificationID;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public void setFireAt(long fireAt) {
    this.fireAt = fireAt;
  }

  public String toString() {
    return "NotificationInfo {id="
        + this.notificationID
        + " | title="
        + this.title
        + " | payload="
        + this.payload
        + " | fire_at="
        + this.fireAt
        + "}";
  }
}
