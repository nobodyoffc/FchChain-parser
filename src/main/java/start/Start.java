package start;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import data.Block;
import esClient.StartClient;
import parser.ChainParser;
import parser.Preparer;
import parser.OpReFileTools;
import writeEs.IndicesFCH;

public class Start {
	
	private static int MenuItemsNum =6;
	private static final Logger log = LoggerFactory.getLogger(Start.class);
	static Scanner sc = new Scanner(System.in);
	
	public static void main(String[] args)throws Exception{

		StartClient startMyEsClient = new StartClient();
		ElasticsearchClient esClient = null;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		log.info("Start.");
		System.out.println(" << FreeChain >> v0.0.1 2023.1.24\n"	);
		boolean more = true;
		while(more) {		
			Configer config = Configer.initial();
				
			System.out.println(
					"	-----------------------------\n"
					+ "	Menu\n"
					+ "	-----------------------------\n"
					+"	1 Create a Java HTTP Client\n"
					+"	2 Create a Java HTTPS Client\n"
					+"	3 Start New Parse\n"
					+"	4 Restart from interruption\n"
					+"	5 Manual start from a height\n"
					+"	6 Config\n"
					+"	0 Exit\n"
					+ "	-----------------------------"
					);

			int choice = choose();
			
			String path;
			long bestHeight;
			switch(choice) {
			case 1:
				startMyEsClient.setParams(config.getIp(),config.getPort());
				esClient= creatClient(startMyEsClient);
				break;
			case 2:
				startMyEsClient.setParams(config.getIp(),config.getPort(),config.getUsername());
				esClient = creatSslClient(startMyEsClient);
				break;
			case 3:
				if(esClient==null) {
					System.out.println("Create a Java client for ES first.");
					break;
				}	
				
				System.out.println("Start from 0, all indices and opreturn*.byte will be deleted. Do you want? y or n:");			
				String delete = sc.next();		
				if (delete.equals("y")) {					
					System.out.println("Do you sure? y or n:");				
					delete = sc.next();			
					if (delete.equals("y")) {	
						File blk = new File(config.getPath(),"blk00000.dat");
						if(!blk.exists()) {
							System.out.println("blk00000.dat isn't found in "+config.getPath()+". Input 6 to config the path:");
							break;
						}
						
						deleteOpReFiles();
						
						path = config.getPath();
						bestHeight = -1;
						
						IndicesFCH.deleteAllIndices(esClient);
						
						java.util.concurrent.TimeUnit.SECONDS.sleep(3);
						IndicesFCH.createAllIndices(esClient);
						new Preparer().prepare(esClient,path,bestHeight);
						break;
					}else break;
				}else break;

			case 4:
				
				if(esClient==null) {
					System.out.println("Create a Java client for ES first.");
					break;
				}
				System.out.println("Do you sure to restart from bestHeight in ES? y or n:");	
				String restart = sc.next();							
	
				if (restart.equals("y")) {	
					
					Block bestBlock = ChainParser.getBestBlock(esClient);
					bestHeight = bestBlock.getHeight();
					
					System.out.println("Restarting from BestHeight: "+(bestHeight-1)+" ...");
					
					path = config.getPath();
					bestHeight = bestHeight-1;
					
					new Preparer().prepare(esClient,path,bestHeight);
					break;
				}else break;
				
			case 5:
				
				System.out.println("Input the height that parsing begin with: ");
				while(!sc.hasNextLong()){
					System.out.println("\nInput the number of the height:");
					sc.next();
				}	
				path = config.getPath();
				bestHeight = sc.nextLong();
				new Preparer().prepare(esClient,path, bestHeight);
				break;
			case 6:
				Configer.config(sc,br);
				break;
			case 0:
				//startMyEsClient.esClient.shutdown();
				if(esClient!=null)startMyEsClient.shutdownClient();
				System.out.println("Exited, see you again.");
				sc.close();
				return;
			}
		}
		sc.close();
	}	
	
	private static void deleteOpReFiles() {
		
		String fileName = ChainParser.OpRefileName;
		File file;
		
		while(true) {
			file = new File(fileName);
			if(file.exists()) {
				file.delete();
				fileName = OpReFileTools.getNextFile(fileName);
			}else break;
		}
	}

	private static int choose() throws IOException {
		System.out.println("\n\nInput the number you want to do:\n");
		int choice = 0;
		while(true) {
			while(!sc.hasNextInt()){
				System.out.println("\nInput one of the integers shown above.");
				sc.next();
			}
			choice = sc.nextInt();
		if(choice <= MenuItemsNum && choice>=0)break;
		System.out.println("\nInput one of the integers shown above.");
		}
		return choice;
	}

	private static ElasticsearchClient creatClient(StartClient startMyEsClient) {
		
		ElasticsearchClient esClient = startMyEsClient.getEsClient();

		if(esClient!=null) {
			System.out.println("There has been a client:"+esClient.toString()+"\nPress any key return...");
			sc.nextLine();
			return null;
		}
		if(startMyEsClient.getHost()==null) {
			System.out.println("No IP. Choose 6 to config first. Press any key return...");
			sc.nextLine();
			return null;
		}
		if(startMyEsClient.getPort()==0) {
			System.out.println("No port. Choose 6 to config first. Press any key return...\"");	
			sc.nextLine();
			return null;
		}
		
		try {
			esClient = startMyEsClient.getClientHttp();
			System.out.println("ES client for HTTP created.");
			return esClient;
		} catch (ElasticsearchException e) {
			log.error("Create esClient failed.",e);
			System.out.println("Create esClient failed. See log...\n");
			return null;
		} catch (IOException e) {
			log.error("Create esClient failed.",e);
			System.out.println("Create esClient failed. See log...\n");
			return null;
		}
	}
	private static ElasticsearchClient creatSslClient(StartClient startMyEsClient) {
		
		ElasticsearchClient esClient = startMyEsClient.getEsClient();
		
		if(esClient!=null) {
			System.out.println("There has been a client:"+esClient.toString()+"\nPress any key return...");
			sc.nextLine();
			return null;
		}
		if(startMyEsClient.getHost()==null) {
			System.out.println("No IP. Choose 6 to config first. Press any key return...");
			sc.nextLine();
			return null;
		}
		if(startMyEsClient.getPort()==0) {
			System.out.println("No port. Choose 6 to config first. Press any key return...\"");	
			sc.nextLine();
			return null;
		}
		if(startMyEsClient.getUsername()==null) {
			System.out.println("No userName. Choose 6 to config first. Press any key return...\"");	
			sc.nextLine();
			return null;
		}
		
		System.out.println("Input the password:");
		String password = sc.next();

		try {
			esClient = startMyEsClient.getClientHttps(password);
		} catch (KeyManagementException | ElasticsearchException | NoSuchAlgorithmException | IOException e) {
			log.error("Create esClient failed.",e);
			System.out.println("Create esClient failed. See log...\n");
		}
		return esClient;
		
	}
	

}
