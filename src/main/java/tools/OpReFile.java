package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import data.OpReturn;
import parse.ResultReadOpReFromFile;

public class OpReFile {
	
	
	private  String fileName = "opreturn00000.byte";
	private File opFile;
	private FileOutputStream opos;
	private FileInputStream opis;
	
	private boolean write;
	
	public OpReFile() {
		write = true;
		try {
			while(true) {
				opFile = new File(fileName);
				if(opFile.length()>15728640) {
					fileName =  getNextFile(fileName);
				}else break;
			}
			if(opFile.exists()) {
	
					opos = new FileOutputStream(opFile,true);
	
			}else {
				opos = new FileOutputStream(opFile);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public OpReFile(String fileName,long pointer) {
			write = false;			
			opFile = new File(fileName);
			try {
				opis = new FileInputStream(opFile);
				opis.skip(pointer);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public void writeOpReturnListIntoFile(ArrayList<OpReturn> opList) throws IOException {

		if(opList==null || opList.isEmpty())return;

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
	}
	
	public ResultReadOpReFromFile readOpReturnFromFileToList(long pointerInFile,int countWanted) throws IOException{

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
		return result;
	}
	
	public void close() throws IOException {
		if(write) {
			opos.close();
		}else 
			opis.close();
	}
	private int getFileOrder(String currentFile) {	
		String s =String.copyValueOf(currentFile.toCharArray(), 8, 4);
		return Integer.parseInt(s);
	}

	private String getFileNameWithOrder(int i) {
		return "opreturn"+String.format("%04d",i)+".dat";
	}

	private String getNextFile(String currentFile) {
		return getFileNameWithOrder(getFileOrder(currentFile)+1);
	}

}
