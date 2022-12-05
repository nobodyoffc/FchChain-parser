package data;

public class Address {
	
	private String id;	//fch address
	private String pubkey;		//public key
	private long balance;		//value of fch in satoshi
	private long income;		//total amount of fch received in satoshi
	private long expend;		//total amount of fch payed in satoshi
	private String guide;	//the address of the address which sent the first fch to this address
	private long birthHeight;	//the height where this address got its first fch
	private long lastHeight; 	//the height where this address info changed latest. If roll back happened, lastHei point to the lastHeight before fork.
	private long cdd;		//the total amount of coindays destroyed
	private long cd;		//CoinDays
	private long utxo;		//Count of UTXO

	private String btcAddr;	//the btc address 
	private String ethAddr;	//the eth address
	private String ltcAddr;	//the ltc address
	private String dogeAddr;	//the doge address
	private String trxAddr;	//the doge address

	public String getId() {
		return id;
	}

	public String getTrxAddr() {
		return trxAddr;
	}

	public void setTrxAddr(String trxAddr) {
		this.trxAddr = trxAddr;
	}

	public void setId(String addr) {
		this.id = addr;
	}

	public String getPubkey() {
		return pubkey;
	}

	public void setPubkey(String pubkey) {
		this.pubkey = pubkey;
	}

	public long getBalance() {
		return balance;
	}

	public void setBalance(long balance) {
		this.balance = balance;
	}

	public long getIncome() {
		return income;
	}

	public void setIncome(long income) {
		this.income = income;
	}

	public long getExpend() {
		return expend;
	}

	public void setExpend(long expend) {
		this.expend = expend;
	}

	public String getGuide() {
		return guide;
	}

	public void setGuide(String guide) {
		this.guide = guide;
	}

	public long getBirthHeight() {
		return birthHeight;
	}

	public void setBirthHeight(long birthHeight) {
		this.birthHeight = birthHeight;
	}

	public long getLastHeight() {
		return lastHeight;
	}

	public void setLastHeight(long lastHeight) {
		this.lastHeight = lastHeight;
	}

	public long getCdd() {
		return cdd;
	}

	public void setCdd(long cdd) {
		this.cdd = cdd;
	}

	public String getBtcAddr() {
		return btcAddr;
	}

	public void setBtcAddr(String btcAddr) {
		this.btcAddr = btcAddr;
	}

	public String getEthAddr() {
		return ethAddr;
	}

	public void setEthAddr(String ethAddr) {
		this.ethAddr = ethAddr;
	}

	public String getLtcAddr() {
		return ltcAddr;
	}

	public void setLtcAddr(String ltcAddr) {
		this.ltcAddr = ltcAddr;
	}

	public String getDogeAddr() {
		return dogeAddr;
	}

	public void setDogeAddr(String dogeAddr) {
		this.dogeAddr = dogeAddr;
	}

	public long getCd() {
		return cd;
	}

	public void setCd(long cd) {
		this.cd = cd;
	}

	public long getUtxo() {
		return utxo;
	}

	public void setUtxo(long utxo) {
		this.utxo = utxo;
	}
	
}
