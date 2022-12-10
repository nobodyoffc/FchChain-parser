package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import data.OpReturn;
import parse.ChainParser;
import parse.ResultReadOpReFromFile;

public class OpReFileTools {

	public void writeOpReturnListIntoFile(ArrayList<OpReturn> opList) throws IOException {

		if(opList==null || opList.isEmpty())return;
		String fileName = ChainParser.OpRefileName;
		File opFile;
		FileOutputStream opos;

			while(true) {
				opFile = new File(fileName);
				if(opFile.length()>251658240) {
					fileName =  getNextFile(fileName);
				}else break;
			}
			if(opFile.exists()) {
					opos = new FileOutputStream(opFile,true);
			}else {
				opos = new FileOutputStream(opFile);
			}

		Iterator<OpReturn> iterOp = opList.iterator();
		while(iterOp.hasNext()) {
			ArrayList<byte[]> opArrList = new ArrayList<byte[]>();
			OpReturn op = iterOp.next();
			
			opArrList.add(BytesTools.intToByteArray(120+op.getOpReturn().getBytes().length));
			opArrList.add(BytesTools.hexToByteArray(op.getId()));
			opArrList.add(BytesTools.longToBytes(op.getHeight()));
			opArrList.add(BytesTools.intToByteArray(op.getTxIndex()));
			opArrList.add(op.getSigner().getBytes());
			if(op.getRecipient()==null || op.getRecipient().equals("nobody")) {
				opArrList.add("                                  ".getBytes());
			}else {
				opArrList.add(op.getRecipient().getBytes());
			}
			opArrList.add(BytesTools.longToBytes(op.getCdd()));
			opArrList.add(op.getOpReturn().getBytes());

				opos.write(BytesTools.bytesMerger(opArrList));
		}
		opos.flush();
		opos.close();
	}
	
	public ResultReadOpReFromFile readOpReturnFromFileToList(long pointerInFile,int countWanted) throws IOException{

		long pointer = pointerInFile;
		
		String fileName = ChainParser.OpRefileName;
		File opFile;
		
		opFile = new File(fileName);
		FileInputStream opis;
			opis = new FileInputStream(opFile);
			opis.skip(pointer);

		
		ArrayList<OpReturn> opList = new ArrayList<OpReturn>();
		int count = 0;
		boolean fileEnd = false;
		
		while(pointerInFile < opFile.length() && count < countWanted) {
					
			OpReturn op = new OpReturn();
			
			byte[] length = new byte[4];
			int end = opis.read(length);
			if(end == -1) {
				System.out.println("OpReturn File was parsed completely.");
				fileEnd = true;
				break;
			}
			
			int opLength = BytesTools.bytesToIntBE(length);
			
			byte[] opbytes = new byte[opLength];
			opis.read(opbytes);
			
			pointerInFile += opLength;
			
			int offset=0;
			
			byte[] txidArr = Arrays.copyOfRange(opbytes, offset, offset+32);
			offset+=32;	
			op.setId(BytesTools.bytesToHexStringBE(txidArr));
			
			byte[] heiArr = Arrays.copyOfRange(opbytes, offset, offset+8);
			offset+=8;	
			op.setHeight(BytesTools.bytes8ToLong(heiArr,false));
			
			//If rollback record?
			//如果不是回滚记录点
			if(opLength>40) {
			
				byte[] txIndexArr = Arrays.copyOfRange(opbytes, offset, offset+4);
				offset+=4;	
				op.setTxIndex(BytesTools.bytesToIntBE(txIndexArr));
				
				byte[] signerArr = Arrays.copyOfRange(opbytes, offset, offset+34);
				offset+=34;	
				op.setSigner(new String(signerArr));
				
				byte[] recipientArr = Arrays.copyOfRange(opbytes, offset, offset+34);
				offset+=34;	
				op.setRecipient(new String(recipientArr));
				if(op.getRecipient()=="                                  ")op.setRecipient(null);
				
				byte[] cddArr = Arrays.copyOfRange(opbytes, offset, offset+8);
				offset+=8;	
				op.setCdd(BytesTools.bytes8ToLong(cddArr,false));
				
				byte[] opReArr = Arrays.copyOfRange(opbytes, offset, opLength);
				op.setOpReturn(new String(opReArr));
			}

			opList.add(op);
		
			count++;
			if(count==countWanted) {
				System.out.println(count + " opReturns had been pased.");
				break;
			}
		}	
		ResultReadOpReFromFile result = new ResultReadOpReFromFile();
		result.opReturnList = opList;
		result.pointerInFile = pointerInFile;
		result.count = count;
		result.fileEnd = fileEnd;
		
		opis.close();
		return result;
	}
	
	private static int getFileOrder(String currentFile) {	
		String s =String.copyValueOf(currentFile.toCharArray(), 8, 1);
		return Integer.parseInt(s);
	}

	private static String getFileNameWithOrder(int i) {
		return "opreturn"+String.format("%d",i)+".byte";
	}

	public static String getNextFile(String currentFile) {
		return getFileNameWithOrder(getFileOrder(currentFile)+1);
	}

}
