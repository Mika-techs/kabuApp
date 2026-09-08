package org.kabuapp.kabuapp.core.net;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.kabuapp.kabuapp.core.net.BadRequestException;
import org.kabuapp.kabuapp.core.net.UnauthorisedException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.lilithtechs.metisJson.JsonMapper;

public abstract class ApiService
{
    protected String baseUrl;
    private final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    private final JsonMapper jsonMapper = new JsonMapper();

    protected <T> T executeRequest(
            String url,
            String httpMethod,
            Class<T> clazz,
            Map<String, Object> params,
            Map<String, String> headers,
            Object body) throws Exception
    {

        URI uri;
        try
        {
            URIBuilder uriBuilder = new URIBuilder(baseUrl + url);
            if (params != null && !params.isEmpty())
            {
                params.forEach((key, value) -> uriBuilder.addParameter(key, String.valueOf(value)));
            }
            uri = uriBuilder.build();
        }
        catch (Exception e)
        {
            throw new IOException("Failed to build URI", e);
        }


        HttpUriRequestBase request;

        switch (httpMethod.toUpperCase())
        {
            case "GET":
                request = new HttpGet(uri);
                break;
            case "POST":
                HttpPost httpPost = new HttpPost(uri);
                if (body != null)
                {
                    String jsonBody = jsonMapper.toJson(body);
                    httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
                }
                request = httpPost;
                break;
            default:
                throw new UnsupportedOperationException("HTTP method " + httpMethod + " is not supported.");
        }

        if (headers != null && !headers.isEmpty())
        {
            headers.forEach(request::setHeader);
        }
        request.setHeader("Host", getDefaultHostHeader(baseUrl + url));

        try (CloseableHttpResponse response = httpClient.execute(request))
        {
            int statusCode = response.getCode();
            HttpEntity responseEntity = response.getEntity();
            String responseString = responseEntity != null ? EntityUtils.toString(responseEntity, StandardCharsets.UTF_8) : null;

            if (statusCode < 200 || statusCode >= 300)
            {
                if (statusCode == 400)
                {
                    throw new BadRequestException();
                }
                else if (statusCode == 401)
                {
                    throw new UnauthorisedException();
                }
                throw new Exception("API-Error: " + statusCode + " - " + response.getReasonPhrase() + "\nResponse Body: " + responseString);
            }

            if (clazz.equals(String.class))
            {
                return (T) responseString;
            }
            else if (responseString != null)
            {
                return jsonMapper.fromJson(responseString, clazz);
            }
            else
            {
                return null;
            }
        }
    }

    public void closeHttpClient() throws IOException
    {
        if (httpClient != null)
        {
            httpClient.close();
        }
    }

    private static String getDefaultHostHeader(String urlString) throws MalformedURLException
    {
        URL url = new URL(urlString);
        String host = url.getHost();
        int port = url.getPort();
        if (port != -1)
        {
            return host + ":" + port;
        }
        else
        {
            return host;
        }
    }
}