package start;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import com.google.gson.Gson;

public class Configer {
	private String ip;
	private int port;
	private String username;
	private String path;
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
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
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	

	public static Configer initial() throws IOException {

		Configer config = new Configer();
		
		Gson gson = new Gson();
		File configFile = new File("config.json");
		if(configFile.exists()) {
			FileInputStream fis = new FileInputStream(configFile);
			byte[] configJsonBytes = new byte[fis.available()];
			fis.read(configJsonBytes);
			
			String configJson = new String(configJsonBytes);
			config = gson.fromJson(configJson, Configer.class);
			fis.close();
		}else {
			System.out.println("Config first, choise 9 please:\n");
		}
		return config;
	}

	public static void config(Scanner sc, BufferedReader br) throws IOException {
		
		Configer configer = new Configer();
		
		Gson gson = new Gson();
		File configFile = new File("config.json");
		
		FileOutputStream fos = new FileOutputStream(configFile);
		
		System.out.println("Input the IP of ES server:");

		while(true) {
			configer.setIp(br.readLine());
			if (configer.getIp().matches("((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))"))break;
			System.out.println("It must be a IPaddress, like \"100.102.102.10\". Input again.");
		}
		
		System.out.println("Input the port of ES server:");
		
		while(true){
			if(!sc.hasNextInt()) {
				System.out.println("It must be a port. It's a integer between 0 and 655350. Input again.\"");
				configer.setPort(sc.nextInt());
			}
			else {
				configer.setPort(sc.nextInt());
				if( configer.getPort()>0 && configer.getPort()<65535)break;
				System.out.println("It has to be between 0 and 655350. Input again.");
				configer.setPort(sc.nextInt());
			}
		}
		
		System.out.println("Input the username of ES:");
		configer.setUsername(br.readLine());
		
		File file;
		
		while(true) {
			System.out.println("Input the path of freecash block data ending with '/':");
			configer.setPath(br.readLine());
	        file = new File(configer.getPath());
	        if (!file.exists()) {
	        	System.out.println("\nPath doesn't exist.");
	        }else break;
	        break;
		}
		
		fos.write(gson.toJson(configer).getBytes());
		fos.close();
		
		System.out.println("\nConfiged.");	
		sc.nextLine();
	}

}
