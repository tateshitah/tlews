/*
Copyright (c) 2013-2020 Hiroaki Tateshita

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package org.braincopy.ws;

/**
 * 
 * @author Hiroaki Tateshita
 * 
 */
public class TLEString {
	public static int AVAILABLE = 1;
	public static int NO_AVAILABLE = 2;
	private int status;
	private String line1;
	private String line2;
	private String noradCatalogID;

	public String getNoradCatalogID() {
		return noradCatalogID;
	}

	public void setNoradCatalogID(String noradCatalogID) {
		this.noradCatalogID = noradCatalogID;
	}

	public String getLine2() {
		return line2;
	}

	public void setLine2(String line2) {
		this.line2 = line2;
	}

	public String getLine1() {
		return line1;
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	public String toString() {
		return "NORAD CAT NO: " + this.noradCatalogID;
	}

	public boolean isAvailable() {
		return this.status == AVAILABLE;
	}

	public void setAvailable(boolean isAvailable) {
		if (isAvailable) {
			this.status = AVAILABLE;
		} else {
			this.status = NO_AVAILABLE;
		}
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int statusIn) {
		this.status = statusIn;
	}

}
