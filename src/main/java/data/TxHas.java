package data;

import java.util.ArrayList;

public class TxHas {
	private String id;			//txid
	private long height;		//height
	private ArrayList<TxoMark> inMarks;
	private  ArrayList<TxoMark> outMarks;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public long getHeight() {
		return height;
	}
	public void setHeight(long height) {
		this.height = height;
	}
	public ArrayList<TxoMark> getInMarks() {
		return inMarks;
	}
	public void setInMarks(ArrayList<TxoMark> inMarks) {
		this.inMarks = inMarks;
	}
	public ArrayList<TxoMark> getOutMarks() {
		return outMarks;
	}
	public void setOutMarks(ArrayList<TxoMark> outMarks) {
		this.outMarks = outMarks;
	}
}
