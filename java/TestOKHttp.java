import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class TestOKHttp
{
  public static void main(String[] args) throws IOException
  {
    System.out.println("proxyHost proxyPort proxyUser proxyPass (or empty for no proxy)");

    String host = null;
    int port = -1;
    String user = null;
    String pass = null;

    if (args.length >= 4)
    {
      host = args[0];
      port = Integer.parseInt(args[1]);
      user = args[2];
      pass = args[3];
      System.out.println("using " + host + ":" + port);
    }

    Builder builder = new OkHttpClient().newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1, Protocol.SPDY_3));

    if (host != null)
    {
      String proxyUser = user;
      String proxyPassword = pass;
      Authenticator proxyAuthenticator = new Authenticator()
      {
        @Override
        public Request authenticate(Route route, Response response) throws IOException
        {
          String credential = Credentials.basic(proxyUser, proxyPassword);
          return response.request().newBuilder()
              .header("Proxy-Authorization", credential)
              .build();
        }
      };

      builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)))
          .proxyAuthenticator(proxyAuthenticator);
    }

    try
    {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
      {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers()
        {
          return new java.security.cert.X509Certificate[] {};
        }
      } };

      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    OkHttpClient client = builder.build();

    okhttp3.Request.Builder requestBuilder = new Request.Builder();
    // requestBuilder.header(header.GetKey(), header.GetValue());
    requestBuilder.url("https://www.google.com");

    Request request = requestBuilder.build();

    Response response = client.newCall(request).execute();

    System.out.println("Response code " + response.code());
    System.out.println(response.toString());
    System.out.println(response.message());
    System.out.println(response.body().string());
  }
}
