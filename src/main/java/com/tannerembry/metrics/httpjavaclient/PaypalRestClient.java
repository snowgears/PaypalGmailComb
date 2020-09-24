package com.tannerembry.metrics.httpjavaclient;

import com.paypal.http.serializer.Json;
import com.squareup.okhttp.Response;
import org.json.JSONObject;

import java.util.Date;

public class PaypalRestClient {
  private static final String TOKEN_URL = "/v1/oauth2/token";
  private static final String TRANSACTIONS_URL   = "/v1/reporting/transactions";

  private final String client_id;
  private final String client_secret;
  private final String endpoint;
  private String access_token;

  public PaypalRestClient(String client_id, String client_secret, String endpoint) {
    this.client_id = client_id;
    this.client_secret = client_secret;
    this.endpoint = endpoint;
  }

  public boolean authorize() {
    try {
      Http request = new Http("POST", endpoint, TOKEN_URL);
      request.addHeader("Accept", "application/json");
      request.addHeader("Accept-Language", "en_US");
      request.addParam("grant_type","client_credentials");
      request.signRequest(this.client_id, this.client_secret);
      JSONObject response = (JSONObject) request.executeRequest();

      //if simple auth with client_id+client secret passes, save the access token and use this for future requests
      access_token = response.getString("access_token");
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Enroll new user with Duo authentication.
   * @return JSONObject with all activation information
   */
  public JSONObject transactions(String startDateISO, String endDateISO) {
    try {
      Http request = new Http("GET", endpoint, TRANSACTIONS_URL);
      request.addParam("start_date", startDateISO);
      request.addParam("end_date", endDateISO);
      request.signRequest(access_token);
      return (JSONObject) request.executeRequest();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
