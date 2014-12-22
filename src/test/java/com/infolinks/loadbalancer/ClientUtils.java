package com.infolinks.loadbalancer;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.http.MediaType;

import java.nio.charset.Charset;

/**
 * Created by yaron
 */
public class ClientUtils {

    public static String getEncodingFromResponse(CloseableHttpResponse closeableResponse)
    {
        String encoding = "UTF-8";
        MediaType contentType = getMediaTypeFromResponse(closeableResponse);
        if (contentType != null) {
            Charset charSet = contentType.getCharSet();
            if (charSet != null) {
                encoding = charSet.name();
            }
        }
        return encoding;
    }

    public static MediaType getMediaTypeFromResponse(CloseableHttpResponse closeableResponse)
    {
        if (closeableResponse.getFirstHeader("Content-Type") == null)
            return null;
        return MediaType.parseMediaType(closeableResponse.getFirstHeader("Content-Type").getValue());
    }

}