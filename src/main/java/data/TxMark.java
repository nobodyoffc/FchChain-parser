package data;

public class TxMark {
	private String id;	
	private int index;
	private long outValue;	//tx hashs
	private long fee;	//tx hashs
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public long getOutValue() {
		return outValue;
	}
	public void setOutValue(long outValue) {
		this.outValue = outValue;
	}
	public long getFee() {
		return fee;
	}
	public void setFee(long fee) {
		this.fee = fee;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}

}
