package data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import parse.ResultReadOpReFromFile;
import tools.BytesTools;

public class OpReturn {
	public final static String OpReturnFile = "opreturn.byte";
	
	private String id;		//txid
	private long height;		//block height
	private int txIndex;		//tx index in the block
	private String opReturn;	//OP_RETURN text
	private String signer;	//address of the first input.
	private String recipient;	//address of the first output, but the first input address and opReturn output.
	private long cdd;
	
	public static void writeOpReturnListIntoFile(ArrayList<OpReturn> opList) {

		if(opList.isEmpty())return;
		boolean error = false;
		do {
			try {
				File opFile = new File(OpReturnFile);
				FileOutputStream opos;
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
					if(op.getRecipient()==null) {
						opArrList.add("                                  ".getBytes());
					}else {
						opArrList.add(op.getRecipient().getBytes());
					}
					opArrList.add(BytesTools.longToBytes(op.getCdd()));
					opArrList.add(op.getOpReturn().getBytes());
		
						opos.write(BytesTools.bytesMerger(opArrList));
					
				}
				opos.close();	
			} catch (IOException e) {
				e.printStackTrace();
				error=true;
			}
		}while(error==true);
	}
	
	public static ResultReadOpReFromFile readOpReturnFromFileToList(long pointerInFile,int countWanted) throws IOException{

		File opFile = new File(OpReturnFile);
		FileInputStream opis = new FileInputStream(opFile);
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
		opis.close();
		
		ResultReadOpReFromFile result = new ResultReadOpReFromFile();
		result.opReturnList = opList;
		result.pointerInFile = pointerInFile;
		result.count = count;
		result.fileEnd = fileEnd;
		return result;
	}

	public String getId() {
		return id;
	}

	public void setId(String txId) {
		this.id = txId;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}

	public int getTxIndex() {
		return txIndex;
	}

	public void setTxIndex(int txIndex) {
		this.txIndex = txIndex;
	}

	public String getOpReturn() {
		return opReturn;
	}

	public void setOpReturn(String opReturn) {
		this.opReturn = opReturn;
	}

	public String getSigner() {
		return signer;
	}

	public void setSigner(String signer) {
		this.signer = signer;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public long getCdd() {
		return cdd;
	}

	public void setCdd(long cdd) {
		this.cdd = cdd;
	}
	
}
