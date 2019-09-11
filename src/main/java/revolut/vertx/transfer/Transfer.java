package revolut.vertx.transfer;

import io.vertx.core.json.JsonObject;

import java.io.Serializable;

public class Transfer implements Serializable {

  private final int id;
  private int fromId;
  private int toId;
  private long millis;
  private Integer sum;



  public Transfer(JsonObject json) {
    this.fromId = json.getInteger("FROM_ID");
    this.toId = json.getInteger("TO_ID");
    this.millis = json.getLong("MILLIS");
    this.sum = json.getInteger("SUM");
    this.id = json.getInteger("ID");
  }

  public Transfer() {
    this.id = -1;
  }

  public Transfer(Integer id, Integer fromId, Integer toId, Long millis, Integer sum) {
    this.id = id;
    this.fromId = fromId;
    this.toId = toId;
    this.millis = millis;
    this.sum = sum;
  }

  public Transfer(Integer fromId, Integer toId, Long millis, Integer sum) {
    this.id = -1;
    this.fromId = fromId;
    this.toId = toId;
    this.millis = millis;
    this.sum = sum;
  }

  public int getId() {
    return id;
  }

  public int getFromId() {
    return fromId;
  }

  public int getToId() {
    return toId;
  }

  public long getMillis() {
    return millis;
  }

  public Integer getSum() {
    return sum;
  }

}