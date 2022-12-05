package esClient;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class StartClient{
	
	private ElasticsearchClient esClient;

	private RestClient restClient;
	private ElasticsearchTransport transport;
	private String host;
	private int port;
	private String username;
	
	static final Logger log = LoggerFactory.getLogger(StartClient.class);
	
	public void setParams(String host,int port) {
		this.host = host;
		this.port = port;
	}
	
	public void setParams(String host,int port,String username) {
		this.host = host;
		this.port = port;
		this.username=username;
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public ElasticsearchClient getClientHttp() throws ElasticsearchException, IOException {
		
		System.out.println("Creating a client...");
		
		try {
		
		// Create a client without authentication check
		this.restClient = RestClient.builder(
				new HttpHost(host, port))
				.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
		        	@Override
					public RequestConfig.Builder customizeRequestConfig(
							RequestConfig.Builder requestConfigBuilder) {
						return requestConfigBuilder.setConnectTimeout(5000 * 1000) // 连接超时（默认为1秒）
								.setSocketTimeout(6000 * 1000);// 套接字超时（默认为30秒）//更改客户端的超时限制默认30秒现在改为100*1000分钟
					}
		        })
				.build();

		// Create the transport with a Jackson mapper
		this.transport = new RestClientTransport(
				restClient, new JacksonJsonpMapper());

		// And create the API client
		ElasticsearchClient client = new ElasticsearchClient(transport);
		
		log.info("Client has been created: {}",client.info().toString());
		
		return client;
		
		}catch(Exception e) {
			log.error("The elasticsearch server may need a authorization. Try \"2 Create aclient in HTTPS net.\". Error info: {}",e);
		return null;
		}
	}
	

	public ElasticsearchClient getClientHttps(String password) throws ElasticsearchException, IOException, NoSuchAlgorithmException, KeyManagementException{
		
		System.out.println("Creating a client with authentication...");
		
	    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
	    
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        
		this.restClient = RestClient.builder(new HttpHost(host, port,"https"))
				.setHttpClientConfigCallback(h ->h.setDefaultCredentialsProvider(credentialsProvider))
				.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
		        	@Override
					public RequestConfig.Builder customizeRequestConfig(
							RequestConfig.Builder requestConfigBuilder) {
						return requestConfigBuilder.setConnectTimeout(5000 * 1000) // 连接超时（默认为1秒）
								.setSocketTimeout(6000 * 1000);// 套接字超时（默认为30秒）//更改客户端的超时限制默认30秒现在改为100*1000分钟
					}
		        })
				.build();
        
		// Create the transport with a Jackson mapper
		this.transport = new RestClientTransport(
				restClient, new JacksonJsonpMapper());

		// And create the API client
		ElasticsearchClient client = new ElasticsearchClient(transport);
		
		System.out.println(client.info().toString());
		
		System.out.println("Client has been created: "+client.toString());
		log.info("Client has been created:{} ",client.toString());
		
		return client;
	}


	public void shutdownClient() throws IOException {
			if(this.esClient!=null)this.esClient.shutdown();
			if(this.transport!=null)this.transport.close();
			if(this.restClient!=null)this.restClient.close();
			log.info("Client has been closed:{} ");
	}

	public ElasticsearchClient getEsClient() {
		return esClient;
	}

}
