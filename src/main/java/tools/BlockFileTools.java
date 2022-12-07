package tools;

public class BlockFileTools {

	public static int getFileOrder(String currentFile) {	
		String s =String.copyValueOf(currentFile.toCharArray(), 3, 5);
		return Integer.parseInt(s);
	}

	public static String getFileNameWithOrder(int i) {
		return "blk"+String.format("%05d",i)+".dat";
	}

	public static String getNextFile(String currentFile) {
		return getFileNameWithOrder(getFileOrder(currentFile)+1);
	}

}
