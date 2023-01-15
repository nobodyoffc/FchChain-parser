package data;

public class OpReturn {

	private String id;		//txid
	private long height;		//block height
	private int txIndex;		//tx index in the block
	private String opReturn;	//OP_RETURN text
	private String signer;	//address of the first input.
	private String recipient;	//address of the first output, but the first input address and opReturn output.
	private long time;
	private long cdd;
	
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
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
}
