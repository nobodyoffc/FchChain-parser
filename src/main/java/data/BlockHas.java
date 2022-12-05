package data;

import java.util.ArrayList;

public class BlockHas {
	private long height;		//height
	private String id;		//block hash
	private ArrayList<TxMark> txMarks;
	
	public long getHeight() {
		return height;
	}
	public void setHeight(long height) {
		this.height = height;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public ArrayList<TxMark> getTxMarks() {
		return txMarks;
	}
	public void setTxMarks(ArrayList<TxMark> txMarks) {
		this.txMarks = txMarks;
	}
	
}
