package com.tannerembry.metrics;

import com.squareup.okhttp.Response;
import com.tannerembry.metrics.httpjavaclient.PaypalRestClient;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class PaypalInitializer {

    private PaypalRestClient paypalRestClient;

    public PaypalInitializer() {
        try {
            ConfigPropertyValues propertyValues = new ConfigPropertyValues();
            paypalRestClient = new PaypalRestClient(propertyValues.getClientID(), propertyValues.getClientSecret(), propertyValues.getEndpoint());

            paypalRestClient.authorize();

        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public List<PaypalPurchase> getAllPurchases(){
        //TODO call all transaction search calls (time frames capped at 31 day periods)
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);

        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.MONTH, Calendar.JUNE);
        startCal.set(Calendar.DAY_OF_MONTH, 30);
        startCal.set(Calendar.YEAR, 2018);
        String startDateISO = df.format(startCal.getTime());

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.MONTH, Calendar.JULY);
        endCal.set(Calendar.DAY_OF_MONTH, 31);
        endCal.set(Calendar.YEAR, 2018);
        String endDateISO = df.format(endCal.getTime());

        JSONObject transactions = paypalRestClient.transactions(startDateISO, endDateISO);
        System.out.println(transactions.toString());

        return null;
    }
}
