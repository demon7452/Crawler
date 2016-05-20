package com.utils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.CodingErrorAction;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
//import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

public class HttpClientUtils {
	private static PoolingHttpClientConnectionManager poolingHttpClientConnectionManager=null;
	private static CloseableHttpClient httpClient=null;
	
	/**
	 * -1表示永不超时
	 */
	private int connectTimeout=-1;
	private int readTimeout=-1;
	private int socketTimeout=-1;
	private String encoding;
	private String proxy;
	
	private List<Address> addresses;
	private Random random;
	static{
		
		try {
//			SSLContext sslContext =  SSLContexts.custom().useTLS().build();
			SSLContext sslContext = SSLContextBuilder.create().build();
			sslContext.init(null, new TrustManager[]{
					//x509证书
					new X509TrustManager() {
						
						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}
						
						@Override
						public void checkServerTrusted(X509Certificate[] arg0, String arg1)
								throws CertificateException {
							
						}
						
						@Override
						public void checkClientTrusted(X509Certificate[] arg0, String arg1)
								throws CertificateException {
							
						}
					}
			}, null);
			//注册http和https协议
			Registry<ConnectionSocketFactory> registry=RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http",PlainConnectionSocketFactory.INSTANCE)
					.register("https",new SSLConnectionSocketFactory(sslContext))
					.build();
			poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(registry);
			poolingHttpClientConnectionManager.setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).build());
			poolingHttpClientConnectionManager.setDefaultConnectionConfig(ConnectionConfig.custom().setMalformedInputAction(CodingErrorAction.IGNORE).setUnmappableInputAction(CodingErrorAction.IGNORE).setMessageConstraints(
						MessageConstraints.custom().setMaxHeaderCount(200).setMaxLineLength(2000).build()
					).build());
			poolingHttpClientConnectionManager.setMaxTotal(200);
			poolingHttpClientConnectionManager.setDefaultMaxPerRoute(20);
			poolingHttpClientConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost("localhost",80)), 50);
			
			
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*HttpClientBuilder builder = HttpClientBuilder.create();
		builder.setConnectionManager(poolingHttpClientConnectionManager);*/
	}
	
	public HttpClientUtils() {
		addresses=new ArrayList<>();
		random=new Random();
		if(StringUtils.isNotBlank(proxy)){
			for(String s:proxy.split(",")){
				String [] sarr = s.split(":");
				Address ipModel = new Address(sarr[0], sarr.length>1?Integer.parseInt(sarr[1]):80);
				addresses.add(ipModel);
			}
		}
		Address address = getRandomIp();
		HttpClientBuilder builder = HttpClients.custom();
		if(address!=null){
			builder.setProxy(new HttpHost(address.getHost(),address.getPort()));
		}
		httpClient=  builder.setConnectionManager(poolingHttpClientConnectionManager).build();
		
	}
	
	private Address getRandomIp(){
		return addresses.size()>0?addresses.get(random.nextInt(addresses.size())):null;
	}
	
	public String httpGet(String url){
		return httpGet(url, null);
	}
	
	public String httpGet(String url,Map<String,String> params){
		return httpGet(url, params, null, 0);
	}
	
	public String httpGet(String url,String address,int port){
		return httpGet(url, null, address, port);
	}
	
	public String httpGet(String url,Map<String,String> params,String address,int port){
		if(params != null && params.size()>0){
			String sp = "";
			boolean encodingIsCorrect=true;
			for(Entry<String,String> entry:params.entrySet()){
				sp += "&"+entry.getKey()+"=";
				String value = entry.getValue();
				if(StringUtils.isNotEmpty(encoding) && encodingIsCorrect){
					try {
						value = URLEncoder.encode(value, encoding);
					} catch (UnsupportedEncodingException e) {
						encodingIsCorrect=false;
					}
				}
				sp+=value;
			}
			int index = url.indexOf("?"); 
			if(index!=-1){
				url += sp;
			}else{
				url+="?"+sp.substring(1);
			}
		}
		
				HttpGet httpGet = new HttpGet(url);
		RequestConfig.Builder builder = RequestConfig.custom().setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).setConnectionRequestTimeout(readTimeout);
		if(StringUtils.isNotBlank(address)){
			if(port == 0){
				port=80;
			}
			HttpHost host = new HttpHost(address, port);
			builder.setProxy(host);
		}
		httpGet.setConfig(builder.build());
		try {
			CloseableHttpResponse response =  httpClient.execute(httpGet);
			int status = response.getStatusLine().getStatusCode();
			if(status!=200 && status !=301 && status != 302){
				throw new RuntimeException(String.format("get response error for url:\"%s\",status code:%d",url,status));
			}
			String result = null;
			HttpEntity entity =null;
			try {
				entity= response.getEntity();
				if(entity!=null){
					if(StringUtils.isEmpty(encoding)){
						result = EntityUtils.toString(entity);
					}else{
						result = EntityUtils.toString(entity,encoding);
					}
				}
			}catch(Exception e){
			}finally{
				if(entity!=null){
					entity.getContent().close();
				}
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			httpGet.releaseConnection();
		}
		return null;
	}

	
	public class Address{
		private String host;
		private Integer port;
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public Integer getPort() {
			return port;
		}
		public void setPort(Integer port) {
			this.port = port;
		}
		public Address(String host, Integer port) {
			super();
			this.host = host;
			this.port = port;
		}
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getProxy() {
		return proxy;
	}

	public void setProxy(String proxy) {
		this.proxy = proxy;
	}

	public List<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}
}
