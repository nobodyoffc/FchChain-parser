package tools;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.Gson;

public class JavaTools {

	public static <T>ArrayList<T> deepListCopy(ArrayList<T> origList, Class<T> class1) {
		ArrayList<T> destList = new ArrayList<T>();
		Gson gson = new Gson();
		Iterator<T> iterAddr = origList.iterator();
		while(iterAddr.hasNext()) {
			T bm = iterAddr.next();
			String bmJson = gson.toJson(bm);
			T am = gson.fromJson(bmJson, class1);
			destList.add(am);
		}
		return destList;
	}
}
