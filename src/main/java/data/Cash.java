package data;

public class Cash {
	
	//calculated
	private String id;	//hash of this cash: sha256(sha256(tx + index)).
	
	//from utxo
	private int outIndex;		//index of cash. Order in cashs of the tx when created.
	private String type;	//type of the script. P2PKH,P2SH,OP_RETURN,Unknown,MultiSig
	private String addr; 	//address
	private long value;		//in satoshi
	private String lockScript;	//LockScript
	private String txId;		//txid, hash in whice this cash was created.
	private int txIndex;		//Order in block of the tx in which this cash was created.
	private long birthTime;		//Block time when this cash is created.
	private long birthHeight;		//Block height.
	
	//from input
	private long spentTime;	//Block time when spent.
	private String spentTxId;	//Tx hash when spent.
	private long spentHeight; 	//Block height when spent.
	private int spentIndex;		//Order in inputs of the tx when spent.	
	private String unlockScript;	//unlock script.
	private String sigHash;	//sigHash.
	private String sequence;	//nSequence

	private long cdd;		//CoinDays Destroyed
	private long cd;		//CoinDays
	private boolean valid;	//Is this cash valid (utxo), or spent (stxo);
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getOutIndex() {
		return outIndex;
	}
	public void setOutIndex(int outIndex) {
		this.outIndex = outIndex;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
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
	public String getLockScript() {
		return lockScript;
	}
	public void setLockScript(String lockScript) {
		this.lockScript = lockScript;
	}
	public String getTxId() {
		return txId;
	}
	public void setTxId(String txId) {
		this.txId = txId;
	}
	public int getTxIndex() {
		return txIndex;
	}
	public void setTxIndex(int txIndex) {
		this.txIndex = txIndex;
	}
	public long getBirthTime() {
		return birthTime;
	}
	public void setBirthTime(long birthTime) {
		this.birthTime = birthTime;
	}
	public long getBirthHeight() {
		return birthHeight;
	}
	public void setBirthHeight(long birthHeight) {
		this.birthHeight = birthHeight;
	}
	public long getSpentTime() {
		return spentTime;
	}
	public void setSpentTime(long spentTime) {
		this.spentTime = spentTime;
	}
	public String getSpentTxId() {
		return spentTxId;
	}
	public void setSpentTxId(String spentTxId) {
		this.spentTxId = spentTxId;
	}
	public long getSpentHeight() {
		return spentHeight;
	}
	public void setSpentHeight(long spentHeight) {
		this.spentHeight = spentHeight;
	}
	public int getSpentIndex() {
		return spentIndex;
	}
	public void setSpentIndex(int spentIndex) {
		this.spentIndex = spentIndex;
	}
	public String getUnlockScript() {
		return unlockScript;
	}
	public void setUnlockScript(String unlockScript) {
		this.unlockScript = unlockScript;
	}
	public String getSigHash() {
		return sigHash;
	}
	public void setSigHash(String sigHash) {
		this.sigHash = sigHash;
	}
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public long getCdd() {
		return cdd;
	}
	public void setCdd(long cdd) {
		this.cdd = cdd;
	}
	public long getCd() {
		return cd;
	}
	public void setCd(long cd) {
		this.cd = cd;
	}
	public boolean isValid() {
		return valid;
	}
	public void setValid(boolean valid) {
		this.valid = valid;
	}
}
