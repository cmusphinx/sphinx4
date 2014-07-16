package edu.cmu.sphinx.decoder.adaptation;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class CountsReader {

	private String filepath;
	private float[][][] wt_mean;
	private float[][][] wt_var;
	private float[][][] dnom;
	private int[] veclen;
	private int n_cb;
	private int n_feat;
	private int n_density;
	private String header;

	public void setHeader(String header) {
		this.header = header;
	}

	public CountsReader(String filepath) {
		super();
		this.filepath = filepath;
	}

	public CountsReader() {
	}

	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	public void setWt_mean(float[][][] wt_mean) {
		this.wt_mean = wt_mean;
	}

	public void setWt_var(float[][][] wt_var) {
		this.wt_var = wt_var;
	}

	public void setDnom(float[][][] dnom) {
		this.dnom = dnom;
	}

	public void setVeclen(int[] veclen) {
		this.veclen = veclen;
	}

	public void setN_cb(int n_cb) {
		this.n_cb = n_cb;
	}

	public void setN_feat(int n_feat) {
		this.n_feat = n_feat;
	}

	public void setN_density(int n_density) {
		this.n_density = n_density;
	}

	public float[][][] getWt_mean() {
		return wt_mean;
	}

	public float[][][] getWt_var() {
		return wt_var;
	}

	public float[][][] getDnom() {
		return dnom;
	}

	public int[] getVeclen() {
		return veclen;
	}

	public int getN_cb() {
		return n_cb;
	}

	public int getN_feat() {
		return n_feat;
	}

	public int getN_density() {
		return n_density;
	}

	public String getHeader() {
		return header;
	}

	public int swapInt(int number) {
		int swapped = 0, byte1, byte2, byte3, byte4;

		byte1 = (number >> 0) & 0xff;
		byte2 = (number >> 8) & 0xff;
		byte3 = (number >> 16) & 0xff;
		byte4 = (number >> 24) & 0xff;
		swapped = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4 << 0;

		return swapped;
	}

	public int swapedReadInt(DataInputStream is) {
		int number, swapped = 0;

		try {
			number = is.readInt();
			swapped = swapInt(number);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return swapped;
	}

	public float swapedReadFloat(DataInputStream is) {
		int number, swappedInt;
		float swappedFloat = 0;

		try {
			number = Float.floatToIntBits(is.readFloat());
			swappedInt = swapInt(number);
			swappedFloat = Float.intBitsToFloat(swappedInt);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return swappedFloat;
	}

	public void read() {

		int n_cb = 0, n_feat = 0, n_density = 0, bom, has_means, has_vars, pass2var, n, nc, d1, d2, d3;
		FileInputStream fp = null;
		DataInputStream is = null;
		String header = "";
		int[] veclen = null;
		float[][][] wt_mean = null;
		float[][][] wt_var = null;
		float[][][] dnom = null;
		float[] buf = null;
		byte[] ba = null;

		try {
			fp = new FileInputStream(filepath);
			is = new DataInputStream(fp);
			ba = new byte[40];
			is.read(ba, 0, 40);

			for (int i = 0; i < ba.length; i++) {
				header += (char) ba[i];
			}

			bom = is.readInt();
			has_means = swapedReadInt(is);
			has_vars = swapedReadInt(is);
			pass2var = swapedReadInt(is);
			n_cb = swapedReadInt(is);
			n_density = swapedReadInt(is);
			n_feat = swapedReadInt(is);

			veclen = new int[n_feat];

			for (int i = 0; i < n_feat; i++) {
				veclen[i] = swapedReadInt(is);
			}

			n = swapedReadInt(is);

			if (has_means == 1) {
				buf = new float[n];

				for (int i = 0; i < n; i++) {
					buf[i] = swapedReadFloat(is);
				}

				wt_mean = new float[n_cb][n_feat][n_density];

				for (int i = 0, b_i = 0; i < n_cb; i++) {
					for (int j = 0; j < n_feat; j++) {
						for (int k = 0; k < n_density; k++) {
							wt_mean[i][j][k] = buf[b_i];
							b_i += veclen[j];
						}
					}
				}

			} else {
				System.out.println("No means available!");
			}

			n = swapedReadInt(is);

			if (has_vars == 1) {
				buf = new float[n];

				for (int i = 0; i < n; i++) {
					buf[i] = swapedReadFloat(is);
				}

				wt_var = new float[n_cb][n_feat][n_density];

				for (int i = 0, b_i = 0; i < n_cb; i++) {
					for (int j = 0; j < n_feat; j++) {
						for (int k = 0; k < n_density; k++) {
							wt_var[i][j][k] = buf[b_i];
							b_i += veclen[j];
						}
					}
				}

			} else {
				System.out.println("No variances available!");
			}

			d1 = swapedReadInt(is);
			d2 = swapedReadInt(is);
			d3 = swapedReadInt(is);
			n = swapedReadInt(is);
			buf = new float[n];

			for (int i = 0; i < n; i++) {
				buf[i] = swapedReadFloat(is);
			}

			if (n != d1 * d2 * d3) {
				System.out.println("Dimensions mismatch!");
			}

			dnom = new float[d1][d2][d3];
			nc = 0;

			for (int i = 0; i < d1; i++) {
				for (int j = 0; j < d2; j++) {
					for (int k = 0; k < d3; k++) {
						dnom[i][j][k] = buf[nc++];
					}
				}
			}

		} catch (IOException e) {
			System.out.println("Could not open file!");
			e.printStackTrace();
		}

		this.setWt_mean(wt_mean);
		this.setWt_var(wt_var);
		this.setDnom(dnom);
		this.setN_cb(n_cb);
		this.setN_density(n_density);
		this.setN_feat(n_feat);
		this.setVeclen(veclen);
		this.setHeader(header);

		try {
			fp.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}