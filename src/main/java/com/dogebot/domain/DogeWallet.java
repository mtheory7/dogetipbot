package com.dogebot.domain;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.persistence.*;

@Entity
@Table(name = "dogewallet")
public class DogeWallet {
  @Column(name = "privkey")
  private String privateKey;

  @Column(name = "address")
  private String address;

  @Column(name = "twitterId")
  private String twitterUserId;

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long Id;

  public DogeWallet() {}

  public DogeWallet(String twitterUserId) {
    // Get new dogecoin pub and priv keys yo
    this.twitterUserId = twitterUserId;
    this.address = getPublicKey();
    this.privateKey = getPrivateKey(this.address);
  }

  @Override
  public String toString() {
    String ADDRESS_URL = "https://blockchair.com/dogecoin/address/";
    return "Address:\n" + ADDRESS_URL + this.address + "\nPrivate Key:\n" + this.privateKey;
  }

  public Double getBalance() {
    Logger logger = Logger.getLogger(DogeWallet.class);
    Double balance = null;
    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost("http://localhost:22555");

      httpPost.setEntity(
          new StringEntity(
              "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"getbalance\",\"params\":[\""
                  + this.twitterUserId
                  + "\"]}"));
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials("", "");
      httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));
      CloseableHttpResponse response = client.execute(httpPost);
      String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
      JSONObject object = new JSONObject(jsonResponse);
      balance = Double.valueOf(object.get("result").toString());
      client.close();
    } catch (Exception e) {
      logger.error("There was an exception while attempting to call getbalance()");
    }
    return balance;
  }

  private String getPublicKey() {
    Logger logger = Logger.getLogger(DogeWallet.class);
    String pubKey = null;
    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost("http://localhost:22555");

      httpPost.setEntity(
          new StringEntity(
              "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"getnewaddress\",\"params\":[]}"));
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials("", "");
      httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));
      CloseableHttpResponse response = client.execute(httpPost);
      String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
      JSONObject object = new JSONObject(jsonResponse);
      pubKey = object.get("result").toString();
      client.close();
    } catch (Exception e) {
      logger.error("There was an exception while attempting to call getnewaddress()");
    }
    return pubKey;
  }

  private String getPrivateKey(String address) {
    Logger logger = Logger.getLogger(DogeWallet.class);
    String privKey = null;
    try {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost("http://localhost:22555");
      httpPost.setEntity(
          new StringEntity(
              "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"dumpprivkey\",\"params\":[\""
                  + address
                  + "\"]}"));
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials("", "");
      httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));
      CloseableHttpResponse response = client.execute(httpPost);
      String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
      JSONObject object = new JSONObject(jsonResponse);
      privKey = object.get("result").toString();
      logger.trace("Setting account \"" + this.twitterUserId + "\" with address: " + address);
      httpPost.setEntity(
          new StringEntity(
              "{\"jsonrpc\":\"1.0\",\"id\":\"curltext\",\"method\":\"setaccount\",\"params\":[\""
                  + address
                  + "\",\""
                  + this.twitterUserId
                  + "\"]}"));
      client.execute(httpPost);
      client.close();
    } catch (Exception e) {
      logger.error(
          "There was an exception while attempting to call dumpprivkey() on address " + address);
    }
    return privKey;
  }
}
