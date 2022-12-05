package tools;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import io.github.novacrypto.base58.Base58;


public class FchTools {

	public static long cdd(long value,long birthTime, long spentTime) {
		return Math.floorDiv(value*Math.floorDiv((spentTime-birthTime),(60*60*24)),100000000);
	}
	public static String hashTxo(String tHash,int index,boolean isRawOrder) {
		
		String verseTHash = BytesTools.bytesToHexStringBE(BytesTools.invertArray(BytesTools.hexToByteArray(tHash)));
		
		 byte[] txHashBytes  = BytesTools.invertArray(BytesTools.hexToByteArray(isRawOrder?verseTHash:tHash));
		 byte[] fromIndexBytes = new byte[4];
		 fromIndexBytes = BytesTools.invertArray(BytesTools.intToByteArray(index));
		 String oHash = BytesTools.bytesToHexStringLE(
				Hash.Sha256x2(
					BytesTools.bytesMerger(txHashBytes,fromIndexBytes)
				));	
		return oHash;
	}
	
	public static Map<String, String> pkToAddresses(String pubkey) {
		String fchAddr = FchTools.pubKeyToFchAddr(pubkey);	
		String btcAddr = FchTools.pubKeyToBtcAddr(pubkey);
		String ethAddr = FchTools.pubKeyToEthAddr(pubkey);
		String ltcAddr = FchTools.pubKeyToLtcAddr(pubkey);
		String dogeAddr = FchTools.pubKeyToDogeAddr(pubkey);
		String trxAddr = FchTools.pubKeyToTrxAddr(pubkey);
		
		Map<String, String> map = new HashMap<String, String>();
		map.put("fchAddr", fchAddr);
		map.put("btcAddr", btcAddr);
		map.put("ethAddr", ethAddr );
		map.put("ltcAddr", ltcAddr);
		map.put("dogeAddr", dogeAddr);
		map.put("trxAddr", trxAddr);

	return map;
}
	public static String parsePkFromUnlockScript(String hexScript) {
		byte[] bScript = BytesTools.hexToByteArray(hexScript);
		int sigLen = Byte.toUnsignedInt(bScript[0]);//Length of signature;
		//Skip signature/跳过签名。
		//Read pubKey./读公钥
		byte pubkeyLenB = bScript[sigLen+1]; //公钥长度
		int pubkeyLen =Byte.toUnsignedInt(pubkeyLenB);
		byte[] pubKeyBytes = new byte[pubkeyLen];
		System.arraycopy(bScript, sigLen+2, pubKeyBytes, 0, pubkeyLen); 
		return BytesTools.bytesToHexStringBE(pubKeyBytes);
	}
	public String compressPk65To33(String pk64_65) throws Exception{
		String publicKey = null;
		if (pk64_65.length()==130){
			publicKey = pk64_65.substring(2,pk64_65.length());
		}else if(pk64_65.length()==128){
			publicKey = pk64_65;
		}else{
			throw new Exception("public key is invalid");
		}
		String keyX = publicKey.substring(0,publicKey.length()/2);
		String keyY = publicKey.substring(publicKey.length()/2, publicKey.length());
		String y_d = keyY.substring(keyY.length()-1);
		String header;
		if ((Integer.parseInt(y_d, 16)&1)==0){
			header = "02";
		}else{
			header = "03";
		}
		String pk33 = header + keyX;
		return pk33;
	}
	public static String recoverPK33ToPK65(String PK33) {

		  BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F",16) ;   
		  BigInteger e = new BigInteger("3",16);
		  BigInteger one =new BigInteger("1",16);
		  BigInteger two =new BigInteger("2",16);
		  BigInteger four =new BigInteger("4",16);
		  BigInteger seven =new BigInteger("7",16);
		  String prefix = PK33.substring(0, 2);
 
		  if(prefix.equals("02")||prefix.equals("03")) {
			  BigInteger x = new BigInteger(PK33.substring(2),16);
	
			  BigInteger ySq = (x.modPow(e, p).add(seven)).mod(p);
			  BigInteger y = ySq.modPow(p.add(one).divide(four), p);
  
			  if (!(y.mod(two).equals(new BigInteger(prefix,16).mod(two)))) {
				  y=p.subtract(y);
			  }
			  
			  return "04"+PK33.substring(2)+BytesTools.bytesToHexStringBE(y.toByteArray());
			  }else return null;
		  }
	public static byte[] recoverPK33ToPK65(byte[] PK33) {
		String str = BytesTools.bytesToHexStringBE(PK33);
		return BytesTools.hexToByteArray(recoverPK33ToPK65(str));
		  }

	public static String compressPK65ToPK33(String strPK65) {


		byte[] pk65 = BytesTools.hexToByteArray(strPK65);
		byte[] pk33 = new byte[33];
		byte[] y = new byte[32];
		System.arraycopy(pk65, 1, pk33 , 1, 32);
		System.arraycopy(pk65, 33, y , 0, 32);
		BigInteger Y = new BigInteger(y);
		BigInteger TWO = new BigInteger("2");
		BigInteger ZERO = new BigInteger("0");
		if(Y.mod(TWO) == ZERO ) {
			pk33[0]=0x02;
			}else { 
				pk33[0]=0x03;
				}
		String PK33 =BytesTools.bytesToHexStringLE(BytesTools.invertArray(pk33)) ;
		return PK33;
		}
	public static String compressPK65ToPK33(byte[] bytesPK65) {
		byte[] pk33 = new byte[33];
		byte[] y = new byte[32];
		System.arraycopy(bytesPK65, 1, pk33 , 1, 32);
		System.arraycopy(bytesPK65, 33, y , 0, 32);
		BigInteger Y = new BigInteger(y);
		BigInteger TWO = new BigInteger("2");
		BigInteger ZERO = new BigInteger("0");
		if(Y.mod(TWO) == ZERO ) {
			pk33[0]=0x02;
			}else { 
				pk33[0]=0x03;
				}
		String PK33 =BytesTools.bytesToHexStringLE(BytesTools.invertArray(pk33)) ;
		return PK33;
		}
	
	public static String hash160ToFCHAddr(String hash160Hex) {

		byte[] b =BytesTools.hexToByteArray(hash160Hex);
		
		byte[] d = {0x23};
		byte[] e = new byte[21];
		System.arraycopy(d, 0, e, 0, 1);
		System.arraycopy(b, 0, e, 1, 20);
		
 		byte[] c = Hash.Sha256x2(e);
 		byte[] f = new byte[4];
		System.arraycopy(c, 0, f, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(e, f);
 		
		return Base58.base58Encode(addrRaw);
	}
	public static String hash160ToFCHAddr(byte[] hash160Bytes) {
		
		byte[] prefixForFch = {0x23};
		byte[] hash160WithPrefix = new byte[21];
		System.arraycopy(prefixForFch, 0, hash160WithPrefix, 0, 1);
		System.arraycopy(hash160Bytes, 0, hash160WithPrefix, 1, 20);
		
		
 		byte[] hashWithPrefix = Hash.Sha256x2(hash160WithPrefix);
 		byte[] checkHash = new byte[4];
		System.arraycopy(hashWithPrefix, 0, checkHash, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(hash160WithPrefix, checkHash);
 		
		return Base58.base58Encode(addrRaw);
	}
	public static String hash160ToBTCAddr(String hash160Hex) {

		byte[] b =BytesTools.hexToByteArray(hash160Hex);
		
		byte[] d = {0x00};
		byte[] e = new byte[21];
		System.arraycopy(d, 0, e, 0, 1);
		System.arraycopy(b, 0, e, 1, 20);
		
 		byte[] c = Hash.Sha256x2(e);
 		byte[] f = new byte[4];
		System.arraycopy(c, 0, f, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(e, f);
 		
		return Base58.base58Encode(addrRaw);
	}
	public static String hash160ToBTCAddr(byte[] hash160Bytes) {
		byte[] d = {0x00};
		byte[] e = new byte[21];
		System.arraycopy(d, 0, e, 0, 1);
		System.arraycopy(hash160Bytes, 0, e, 1, 20);
		
 		byte[] c = Hash.Sha256x2(e);
 		byte[] f = new byte[4];
		System.arraycopy(c, 0, f, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(e, f);
 		
		return Base58.base58Encode(addrRaw);
	}
	public static String hash160ToDOGEAddr(String hash160Hex) {

		byte[] b =BytesTools.hexToByteArray(hash160Hex);
		
		byte[] d = {0x1e};
		byte[] e = new byte[21];
		System.arraycopy(d, 0, e, 0, 1);
		System.arraycopy(b, 0, e, 1, 20);
		
 		byte[] c = Hash.Sha256x2(e);
 		byte[] f = new byte[4];
		System.arraycopy(c, 0, f, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(e, f);
 		
		return Base58.base58Encode(addrRaw);
	}
	public static String hash160ToDOGEAddr(byte[] hash160Bytes) {
		byte[] d = {0x1e};
		byte[] e = new byte[21];
		System.arraycopy(d, 0, e, 0, 1);
		System.arraycopy(hash160Bytes, 0, e, 1, 20);
		
 		byte[] c = Hash.Sha256x2(e);
 		byte[] f = new byte[4];
		System.arraycopy(c, 0, f, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(e, f);
 		
		return Base58.base58Encode(addrRaw);
	}
	public static String hash160ToLTCAddr(String hash160Hex) {

		byte[] b =BytesTools.hexToByteArray(hash160Hex);
		
		byte[] d = {0x30};
		byte[] e = new byte[21];
		System.arraycopy(d, 0, e, 0, 1);
		System.arraycopy(b, 0, e, 1, 20);
		
 		byte[] c = Hash.Sha256x2(e);
 		byte[] f = new byte[4];
		System.arraycopy(c, 0, f, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(e, f);
 		
		return Base58.base58Encode(addrRaw);
	}
	public static String hash160ToLTCAddr(byte[] hash160Bytes) {

		byte[] d = {0x30};
		byte[] e = new byte[21];
		System.arraycopy(d, 0, e, 0, 1);
		System.arraycopy(hash160Bytes, 0, e, 1, 20);
		
 		byte[] c = Hash.Sha256x2(e);
 		byte[] f = new byte[4];
		System.arraycopy(c, 0, f, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(e, f);
 		
		return Base58.base58Encode(addrRaw);
	}
	public static String hash160ToTRXAddr(byte[] hash160Bytes) {

		byte[] d = {0x41};
		byte[] e = new byte[21];
		System.arraycopy(d, 0, e, 0, 1);
		System.arraycopy(hash160Bytes, 0, e, 1, 20);
		
 		byte[] c = Hash.Sha256x2(e);
 		byte[] f = new byte[4];
		System.arraycopy(c, 0, f, 0, 4);
 		byte[] addrRaw = BytesTools.bytesMerger(e, f);
 		
		return Base58.base58Encode(addrRaw);
	}

	public static String pubKeyToFchAddr(String a) {	
		byte[] b = Hash.Sha256(BytesTools.hexToByteArray(a));		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToFCHAddr(h);
		return address;
	}
	public static String pubKeyToFchAddr(byte[] a) {	
		byte[] b = Hash.Sha256(a);		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToFCHAddr(h);
		return address;
	}
	public static String pubKeyToBtcAddr(String a) {	
		byte[] b = Hash.Sha256(BytesTools.hexToByteArray(a));		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToBTCAddr(h);
		return address;
	}
	public static String pubKeyToBtcAddr(byte[] a) {	
		byte[] b = Hash.Sha256(a);		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToBTCAddr(h);
		return address;
	}
	public static String pubKeyToTrxAddr(String a) {	
		byte[] b = Hash.Sha256(BytesTools.hexToByteArray(a));		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToTRXAddr(h);
		return address;
	}
	public static String pubKeyToTrxAddr(byte[] a) {	
		byte[] b = Hash.Sha256(a);		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToTRXAddr(h);
		return address;
	}
	public static String pubKeyToDogeAddr(String a) {	
		byte[] b = Hash.Sha256(BytesTools.hexToByteArray(a));		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToDOGEAddr(h);
		return address;
	}
	public static String pubKeyToDogeAddr(byte[] a) {	
		byte[] b = Hash.Sha256(a);		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToDOGEAddr(h);
		return address;
	}
	public static String pubKeyToLtcAddr(String a) {	
		byte[] b = Hash.Sha256(BytesTools.hexToByteArray(a));		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToLTCAddr(h);
		return address;
	}
	public static String pubKeyToLtcAddr(byte[] a) {	
		byte[] b = Hash.Sha256(a);		
		byte[] h = Hash.Ripemd160(b);
		String address = FchTools.hash160ToLTCAddr(h);
		return address;
	}
	public static String pubKeyToEthAddr(String a) {

		String str = Hash.sha3(a);
		String address = "0x"+str.substring(26);
		return address;
	}
	public static String pubKeyToEthAddr(byte[] b) {
		byte[] b1 = Hash.sha3(b);
		String address = "0x"+BytesTools.bytesToHexStringLE(b1).substring(26);
		return address;
	}

}




