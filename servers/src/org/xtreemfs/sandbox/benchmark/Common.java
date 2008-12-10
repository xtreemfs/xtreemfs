package org.xtreemfs.sandbox.benchmark;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Date;
import java.sql.Time;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

/**
 *
 * @author clorenz
 */
public class Common {
	/**
	 * every dir has a "/" at the end
	 * @param dir
	 * @return
	 */
	public static String correctDir(String dir) {
		if (!dir.endsWith("/")) {
			dir = dir + "/";
		}
		return dir;
	}

	/**
	 * generates randomly filled byte-array
	 *
	 * @param length
	 *            length of the byte-array
	 */
	public static byte[] generateRandomBytes(int length) {
		Random r = new Random(15619681);
		byte[] bytes = new byte[length];

		r.nextBytes(bytes);
		return bytes;
	}

    /**
     * generates randomly Filename
     */
    public static String generateFileId(Random r) throws IllegalArgumentException {
        String id = r.nextInt(100000000) + ":" + r.nextInt(1000000000);
        return id;
    }


/*	public static void deepDirClean(String dir) {
		File file = new File(dir);
		if (!file.exists()) {
			file.mkdir();
		} else {
			for (File fileChild : file.listFiles()) {
				if (fileChild.isDirectory()) {
					deepDirClean(fileChild.getAbsolutePath());
					fileChild.delete();
				} else {
					fileChild.delete();
				}
			}
		}
	}*/

	/**
	 * writes Results into a file (overwrites File, if exists)
	 * @param absFilename
	 * @param list
	 */
	public static void writeToFile(String absFilename, LinkedList<String> list){
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absFilename, true)));
			try {
				for(String s : list){
					out.write(s + "\n");
				}
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(out!=null)
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	/**
	 * formats the input for result-file
	 * @param bench
	 * @param info
	 * @param time
	 * @return
	 */
	public static String formatResultForFile(String bench, String info, long time){
		return new Date(System.currentTimeMillis()) + " " + new Time(System.currentTimeMillis()) + "; " + bench + "; " + info + "; " + time;
	}

	/**
	 * converts a Number with "." into ","
	 * @param n
	 * @return
	 */
	public static String formatNumberToComma(long n){
		DecimalFormat df = (DecimalFormat)DecimalFormat.getInstance(Locale.GERMAN);
		df.applyPattern( "#,###,##0.00" );
		String s = df.format( n );
		return s;
	}

}
