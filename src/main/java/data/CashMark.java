package data;

public class CashMark {
	private String id;		//input id, the hash of previous txid and index, e.g. the first 32+4 bytes of the input.
	private String addr;	//input address
	private long value;		//input value
	private long cdd;		//input value

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getAddr() {
		return addr;
	}
	public void setAddr(String addr) {
		this.addr = addr;
	}
	public long getValue() {
		return value;
	}
	public void setValue(long value) {
		this.value = value;
	}
	public long getCdd() {
		return cdd;
	}
	public void setCdd(long cdd) {
		this.cdd = cdd;
	}
	
}
