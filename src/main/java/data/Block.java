package data;

public class Block {
	//from block head
	private long size;		//block size
	private long height;		//block height
	private String version;		//version
	private String preId;	//previous block hash
	private String merkleRoot;	//merkle tree root
	private long time;		//block timestamp
	private long diffTarget;		//The current difficulty target
	private long nonce;		//nonce
	private int txCount;		//number of TXs included

	//calculated
	private String id;		//block hash
	private long inValueT;		//total amount of all inputs values in satoshi
	private long outValueT;		//total amount of all outputs values in satoshi
	private long fee;		//total amount of tx fee in satoshi
	private long cdd;		//total amount of coindays destroyed	
	
	
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public long getHeight() {
		return height;
	}
	public void setHeight(long height) {
		this.height = height;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getPreId() {
		return preId;
	}
	public void setPreId(String preId) {
		this.preId = preId;
	}
	public String getMerkleRoot() {
		return merkleRoot;
	}
	public void setMerkleRoot(String merkleRoot) {
		this.merkleRoot = merkleRoot;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public long getDiffTarget() {
		return diffTarget;
	}
	public void setDiffTarget(long diffTarget) {
		this.diffTarget = diffTarget;
	}
	public long getNonce() {
		return nonce;
	}
	public void setNonce(long nonce) {
		this.nonce = nonce;
	}
	public int getTxCount() {
		return txCount;
	}
	public void setTxCount(int txCount) {
		this.txCount = txCount;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public long getInValueT() {
		return inValueT;
	}
	public void setInValueT(long inValueT) {
		this.inValueT = inValueT;
	}
	public long getOutValueT() {
		return outValueT;
	}
	public void setOutValueT(long outValueT) {
		this.outValueT = outValueT;
	}
	public long getFee() {
		return fee;
	}
	public void setFee(long fee) {
		this.fee = fee;
	}
	public long getCdd() {
		return cdd;
	}
	public void setCdd(long cdd) {
		this.cdd = cdd;
	}
}