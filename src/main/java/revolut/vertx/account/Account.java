package revolut.vertx.account;

import io.vertx.core.json.JsonObject;

import java.io.Serializable;

public class Account implements Serializable {

  private final int id;

  private String num;

  private Integer balance;



  public Account(JsonObject json) {
    this.num = json.getString("NUM");
    this.balance = json.getInteger("BALANCE");
    this.id = json.getInteger("ID");
  }

  public Account() {
    this.id = -1;
  }

  public Account(String num, Integer balance) {
    if(balance < 0)
      throw new IllegalArgumentException("balance should be above 0");
    this.num = num;
    this.balance = balance;
    this.id = -1;
  }

  public Account(int id, String num, Integer balance) {
    if(balance < 0)
      throw new IllegalArgumentException("balance should be above 0");
    this.id = id;
    this.num = num;
    this.balance = balance;
  }

  public int getId() {
    return id;
  }

  public String getNum() {
    return num;
  }

  public int getBalance() {
    return balance;
  }

}