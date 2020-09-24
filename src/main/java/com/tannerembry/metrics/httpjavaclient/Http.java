package com.tannerembry.metrics.httpjavaclient;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class Http {
    public final static int BACKOFF_FACTOR = 2;
    public final static int INITIAL_BACKOFF_MS = 1000;
    public final static int MAX_BACKOFF_MS = 32000;
    public final static int DEFAULT_TIMEOUT_SECS = 60;
    private final static int RATE_LIMIT_ERROR_CODE = 429;

    public final static String HmacSHA1 = "HmacSHA1";
    public final static String HmacSHA512 = "HmacSHA512";
    public final static String UserAgentString = "Paypal API Java";

    private String method;
    private String host;
    private String uri;
    private String signingAlgorithm;
    private Headers.Builder headers;
    Map<String, String> params = new HashMap<String, String>();
    private Random random = new Random();
    private OkHttpClient httpClient;

    public static SimpleDateFormat RFC_2822_DATE_FORMAT
        = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z",
                               Locale.US);

    public static MediaType FORM_ENCODED = MediaType.parse("application/x-www-form-urlencoded");

    public Http(String in_method, String in_host, String in_uri) {
        this(in_method, in_host, in_uri, DEFAULT_TIMEOUT_SECS);
    }

    public Http(String in_method, String in_host, String in_uri, int timeout) {
        method = in_method.toUpperCase();
        host = in_host;
        uri = in_uri;
        signingAlgorithm = "HmacSHA1";

        headers = new Headers.Builder();
        headers.add("Host", host);
        headers.add("user-agent", UserAgentString);

        httpClient = new OkHttpClient();
        httpClient.setConnectTimeout(timeout, TimeUnit.SECONDS);
        httpClient.setWriteTimeout(timeout, TimeUnit.SECONDS);
        httpClient.setReadTimeout(timeout, TimeUnit.SECONDS);
    }

    public Object executeRequest() throws Exception {
        JSONObject result = (JSONObject)executeJSONRequest();
        return result;
    }

    public Object executeJSONRequest() throws Exception {
        JSONObject result = new JSONObject(executeRequestRaw());
        return result;
    }

    public String executeRequestRaw() throws Exception {
        Response response = executeHttpRequest();
        return response.body().string();
    }

    public Response executeHttpRequest() throws Exception {
      String url = "https://" + host + uri;
      String queryString = createQueryString();

      Request.Builder builder = new Request.Builder();
      if (method.equals("POST")) {
        builder.post(RequestBody.create(FORM_ENCODED, queryString));
      } else if (method.equals("PUT")) {
        builder.put(RequestBody.create(FORM_ENCODED, queryString));
      } else if (method.equals("GET")) {
        if (queryString.length() > 0) {
          url += "?" + queryString;
        }
        builder.get();
      } else if(method.equals("DELETE")) {
        if (queryString.length() > 0) {
          url += "?" + queryString;
        }
        builder.delete();
      } else {
        throw new UnsupportedOperationException("Unsupported method: "
            + method);
      }

      // finish and execute request
      Request request = builder.headers(headers.build()).url(url).build();
      return executeRequest(request);
    }

    private Response executeRequest(Request request) throws Exception {
        long backoffMs = INITIAL_BACKOFF_MS;
        while (true) {
            Response response = httpClient.newCall(request).execute();
            if (response.code() != RATE_LIMIT_ERROR_CODE || backoffMs > MAX_BACKOFF_MS) {
                return response;
            }

            sleep(backoffMs + random.nextInt(1000));
            backoffMs *= BACKOFF_FACTOR;
        }
    }

    protected void sleep(long ms) throws Exception {
        Thread.sleep(ms);
    }

    public void signRequest(String client_id, String client_secret)
      throws UnsupportedEncodingException {
        signRequestBasic(client_id, client_secret, 2);
    }

    public void signRequestBasic(String client_id, String client_secret, int sig_version)
            throws UnsupportedEncodingException {
        String date = formatDate(new Date());
        String auth = client_id + ":" + client_secret;
        String header = "Basic " + Base64.encodeBytes(auth.getBytes());
        addHeader("Authorization", header);
        if (sig_version == 2) {
            addHeader("Date", date);
        }
    }

    public void signRequest(String access_token)
            throws UnsupportedEncodingException {
        signRequestBearer(access_token, 2);
    }

    public void signRequestBearer(String access_token, int sig_version)
            throws UnsupportedEncodingException {
        String date = formatDate(new Date());
        String auth = access_token;
        String header = "Bearer " + access_token;
        addHeader("Authorization", header);
        if (sig_version == 2) {
            addHeader("Date", date);
        }
    }

    protected String signHMAC(String skey, String msg) {
        try {
            byte[] sig_bytes = Util.hmac(signingAlgorithm,
                                         skey.getBytes(),
                                         msg.getBytes());
            String sig = Util.bytes_to_hex(sig_bytes);
            return sig;
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDate(Date date) {
        // Could use ThreadLocal or a pool of format objects instead
        // depending on the needs of the application.
        synchronized (RFC_2822_DATE_FORMAT) {
            return RFC_2822_DATE_FORMAT.format(date);
        }
    }

    public void addHeader(String name, String value) {
        headers.add(name, value);
    }

    public void addParam(String name, String value) {
      params.put(name, value);
    }

    public void setProxy(String host, int port) {
        this.httpClient.setProxy(
            new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port))
        );
    }

    public void setSigningAlgorithm(String algorithm)
      throws NoSuchAlgorithmException {
        if (algorithm != HmacSHA1 && algorithm != HmacSHA512) {
            throw new NoSuchAlgorithmException(algorithm);
        }
        signingAlgorithm = algorithm;
    }

    protected String canonRequest(String date, int sig_version)
      throws UnsupportedEncodingException {
        String canon = "";
        if (sig_version == 2) {
            canon += date + "\n";
        }
        canon += method.toUpperCase() + "\n";
        canon += host.toLowerCase() + "\n";
        canon += uri + "\n";
        canon += createQueryString();

        return canon;
    }

    private String createQueryString()
        throws UnsupportedEncodingException {
      ArrayList<String> args = new ArrayList<String>();
      ArrayList<String> keys = new ArrayList<String>();

      for (String key : params.keySet()) {
        keys.add(key);
      }

      Collections.sort(keys);

      for (String key : keys) {
        String name = URLEncoder
            .encode(key, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
        String value = URLEncoder
            .encode(params.get(key), "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
        args.add(name + "=" + value);
      }

      return Util.join(args.toArray(), "&");
    }
}
