package com.infolinks.loadbalancer;

import com.infolinks.loadbalancer.client.ConnectionManager;
import com.infolinks.loadbalancer.client.LoadBalancedHttpClient;
import com.infolinks.loadbalancer.client.ServerGroupManager;
import com.infolinks.loadbalancer.utils.PropertiesAccessor;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by: yaron
 */
public class BasicTests {

    public static void main(String[] args)
    {
        LoadBalancedHttpClient httpClient = setUpHttpClient();
        HttpGet getMethod = null;
        CloseableHttpResponse response = null;

        String resString = "";
        try {
            getMethod = new HttpGet("http://lbtest.infolinks.com/company");
            response = httpClient.execute(getMethod);
            String encoding = ClientUtils.getEncodingFromResponse(response);
            resString = IOUtils.toString(response.getEntity().getContent(), encoding);

            System.out.println(response.getStatusLine());

            getMethod = new HttpGet("http://lbtest.infolinks.com/company");
            response = httpClient.execute(getMethod);
            encoding = ClientUtils.getEncodingFromResponse(response);
            resString = IOUtils.toString(response.getEntity().getContent(), encoding);

            System.out.println(response.getStatusLine());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null)
            {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static LoadBalancedHttpClient setUpHttpClient()
    {
        Properties connManagerProperties = new Properties();
        connManagerProperties.put("client.conn.socketTimeout", "30000");
        ConnectionManager connectionManager = new ConnectionManager();
        connectionManager.configure(new PropertiesAccessor(connManagerProperties));

        Properties serverGroupProperties = new Properties();
        serverGroupProperties.put("lbServer.lbtest.infolinks.com","www.infolinks.com:80,test-qa.com:80");
        ServerGroupManager serverGroupManager = new ServerGroupManager();
        serverGroupManager.configure(new PropertiesAccessor(serverGroupProperties));

        LoadBalancedHttpClient httpClient = new LoadBalancedHttpClient();
        httpClient.setConnectionManager(connectionManager);
        httpClient.setServerGroupManager(serverGroupManager);
        return httpClient;
    }
}
