package revolut.vertx.account;

import io.vertx.core.json.JsonObject;

import java.io.Serializable;
import java.math.BigDecimal;

public class Account implements Serializable {

  private final int id;

  public String num;

  public Integer balance;



  public Account(JsonObject json) {
    this.num = json.getString("NUM");
    this.balance = json.getInteger("BALANCE");
    this.id = json.getInteger("ID");
  }

  public Account() {
    this.id = -1;
  }

  public Account(String num, Integer balance) {
    this.num = num;
    this.balance = balance;
    this.id = -1;
  }

  public Account(int id, String num, Integer balance) {
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

  public void setNum(String num) {
    this.num = num;
  }

  public Integer getBalance() {
    return balance;
  }

  public void setBalance(Integer balance) {
    this.balance = balance;
  }
}