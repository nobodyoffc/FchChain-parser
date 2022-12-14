package data;

import java.util.ArrayList;

public class TxHas {
	private String id;			//txid
	private long height;		//height
	private ArrayList<CashMark> inMarks;
	private  ArrayList<CashMark> outMarks;
	
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
	public ArrayList<CashMark> getInMarks() {
		return inMarks;
	}
	public void setInMarks(ArrayList<CashMark> inMarks) {
		this.inMarks = inMarks;
	}
	public ArrayList<CashMark> getOutMarks() {
		return outMarks;
	}
	public void setOutMarks(ArrayList<CashMark> outMarks) {
		this.outMarks = outMarks;
	}
}
