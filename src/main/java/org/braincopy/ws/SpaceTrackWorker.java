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

import static java.lang.String.format;
import static java.sql.DriverManager.getConnection;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletContext;

import org.braincopy.orbit.ConstantNumber;
import org.braincopy.orbit.PositionECEF;
import org.braincopy.orbit.PositionECI;
import org.braincopy.orbit.PositionENU;
import org.braincopy.orbit.PositionLLH;

import sgp4v.Sgp4Data;
import sgp4v.Sgp4Unit;

/**
 * 
 * @author Hiroaki Tateshita
 * @version 0.6.5
 * 
 */
public class SpaceTrackWorker {

	final private static String baseURL = "https://www.space-track.org";
	final private static String authPath = "/ajaxauth/login";
	ServletContext context;

	/**
	 * properties file includes connecting information to Spacetrack.
	 */
	private Properties spaceTrackProperties;

	/**
	 * properties file includes connecting information to PostgreSQL.
	 */
	private Properties dbProperties;

	/**
	 * 
	 */
	private HttpsURLConnection httpsConnection;

	/**
	 * 
	 */
	private Connection dbConnection;

	/**
	 * @param context the context to set
	 */
	public void setContext(ServletContext context) {
		this.context = context;
	}

	public static void main(String[] args) {
		try {

			SpaceTrackWorker worker = new SpaceTrackWorker();
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			calendar.set(2012, 7, 21, 00, 00, 00);
			ArrayList<TLEString> tleList = worker.getTLEList(calendar, "25544");
			Iterator<TLEString> ite = tleList.iterator();
			TLEString tempTLE;
			Sgp4Unit sgp4 = new Sgp4Unit();
			int startYear, stopYear;
			double startDay, stopDay, step;
			step = 60;// 3h, 180m, 10800s
			double latDeg = 26.1312;// [deg]
			double lonDeg = 127.4048;// [deg]

			PositionECEF userPos = new PositionLLH(latDeg * Math.PI / 180, lonDeg * Math.PI / 180, 0).convertToECEF();

			while (ite.hasNext()) {
				tempTLE = (TLEString) ite.next();
				if (tempTLE.isAvailable()) {

					calendar.add(Calendar.DAY_OF_YEAR, 0);
					startYear = calendar.get(Calendar.YEAR);
					startDay = worker.getDoubleDay(calendar);
					calendar.add(Calendar.DAY_OF_YEAR, 1);// move to original
					// date
					stopYear = calendar.get(Calendar.YEAR);
					stopDay = worker.getDoubleDay(calendar);

					Vector<Sgp4Data> results = sgp4.runSgp4(tempTLE.getLine1(), tempTLE.getLine2(), startYear, startDay,
							stopYear, stopDay, step);
					PositionECI posEci = null;
					PositionENU posEnu = null;
					GregorianCalendar dateAndTime = null;
					double days = 0;
					Sgp4Data data = null;
					for (int i = 0; i < results.size(); i++) {
						data = (Sgp4Data) results.elementAt(i);
						days = startDay + i * step / (60 * 24);
						dateAndTime = worker.getCalendarFmYearAndDays(startYear, days);

						posEci = new PositionECI(data.getX() * ConstantNumber.RADIUS_OF_EARTH,
								data.getY() * ConstantNumber.RADIUS_OF_EARTH,
								data.getZ() * ConstantNumber.RADIUS_OF_EARTH, dateAndTime);
						posEnu = PositionENU.convertToENU(posEci.convertToECEF(), userPos);

						System.out.println(tempTLE.getNoradCatalogID() + "\t" + dateAndTime.get(Calendar.YEAR) + "/"
								+ (dateAndTime.get(Calendar.MONTH) + 1) + "/" + dateAndTime.get(Calendar.DAY_OF_MONTH)
								+ " " + dateAndTime.get(Calendar.HOUR_OF_DAY) + ":" + dateAndTime.get(Calendar.MINUTE)
								+ "\t" + posEnu.getAzimuth() * 180 / Math.PI + "\t "
								+ posEnu.getElevation() * 180 / Math.PI + "\t");

					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param year
	 * @param days
	 * @return
	 */
	public GregorianCalendar getCalendarFmYearAndDays(int year, double days) {
		GregorianCalendar result = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		result.set(Calendar.YEAR, year);
		int daysOfYear = (int) days;
		result.set(Calendar.DAY_OF_YEAR, daysOfYear + 1);
		double hours = (days - (double) daysOfYear) * 24;
		int hourOfDay = (int) hours;
		result.set(Calendar.HOUR_OF_DAY, hourOfDay);
		double minutes = (hours - (double) hourOfDay) * 60;
		int minuteOfHour = (int) minutes;
		result.set(Calendar.MINUTE, minuteOfHour);
		result.set(Calendar.SECOND, (int) ((minutes - (double) minuteOfHour) * 60));
		return result;
	}

	/**
	 * 
	 * @param calendar
	 * @return
	 */
	public double getDoubleDay(Calendar calendar) {
		double result = (double) calendar.get(Calendar.DAY_OF_YEAR) - 1;
		result += ((double) calendar.get(Calendar.HOUR_OF_DAY)) / 24.0;
		result += ((double) calendar.get(Calendar.MINUTE)) / (24.0 * 60);
		result += ((double) calendar.get(Calendar.SECOND)) / (24.0 * 60 * 60);
		return result;
	}

	/**
	 * Get TLE data from Space Track website. If internal DB (postgresql) already
	 * has enough data, worker will not try to get TLE from space-track.
	 * 
	 * @param calendar
	 * @param noradCatId
	 * @throws IOException
	 * @throws SQLException
	 */
	public ArrayList<TLEString> getTLEList(Calendar calendar, String noradCatId) throws IOException, SQLException {
		ArrayList<TLEString> result = new ArrayList<TLEString>();

		loadPropertiesFiles();

		String[] noradCatalogIDList = new String[1];
		noradCatalogIDList[0]=noradCatId;

		/*
		 * including establishment of db connection
		 */
		setTLEfmDB(result, noradCatalogIDList, calendar);

		/*
		 * in case of not enough tle info in internal DB, the worker will try to get
		 * from space track. the worker make "noradCatalogIDList" based on the list in
		 * "satelliteDataBase.txt" usually if the worker try to get tle from space-track
		 * once, the result should be stored in the internal DB so worker will not use
		 * space track from second try but use database.
		 */
		if (noradCatalogIDList.length > result.size()) {
			System.out.println("trying to get the TLE from Space Track. ");
			result.clear();
			/*
			 * Authentication including establish https connection.
			 */
			spaceTrackAuthentication();

			/*
			 * create query for space track
			 */
			String query = createQueryForSpaceTrack(noradCatalogIDList, calendar);

			URL url = new URL(baseURL + query);

			BufferedReader br = new BufferedReader(new InputStreamReader((url.openStream())));

			String[] temp;
			TLEString tleString = null;
			String tempNoradCatID;
			String output;
			while ((output = br.readLine()) != null) {
				temp = output.split(" ");
				if (temp[0].equals("1")) {
					tleString = new TLEString();
					tleString.setLine1(output);
					if ((output = br.readLine()) != null) {
						temp = output.split(" ");
						if (temp[0].equals("2")) {
							tempNoradCatID = temp[1];
							for (int i = 0; i < result.size(); i++) {
								if (((TLEString) result.get(i)).getNoradCatalogID().equals(tempNoradCatID)) {
									result.remove(i);
								}
							}

							tleString.setLine2(output);
							tleString.setNoradCatalogID(tempNoradCatID);
							tleString.setAvailable(true);
							result.add(tleString);
						}
					}
				}
				// System.out.println(temp[0] + "," + temp[1] + "," + temp[2]);
			}
			httpsConnection.disconnect();
			if (result.size() < noradCatalogIDList.length) {
				addNoAvailableTLE(result, noradCatalogIDList);
			}
			addTLEtoDB(result, calendar);
			dbConnection.close();
		} else {
			System.out.println("use database");
		}
		return result;
	}

	/**
	 * 
	 * @param tleStringList
	 * @param noradCatalogIDList
	 */
	private void addNoAvailableTLE(ArrayList<TLEString> tleStringList, String[] noradCatalogIDList) {
		TLEString tleString;
		for (int j = 0; j < noradCatalogIDList.length; j++) {
			check: for (int i = 0; i < tleStringList.size(); i++) {
				if (noradCatalogIDList[j].equals(tleStringList.get(i).getNoradCatalogID())) {
					break check;
				}
				if (i == tleStringList.size() - 1) {
					tleString = new TLEString();
					tleString.setAvailable(false);
					tleString.setNoradCatalogID(noradCatalogIDList[j]);
					tleStringList.add(tleString);
					System.out.println("no-available tle added: " + noradCatalogIDList[j]);
				}
			}
		}

	}

	/**
	 * 
	 * @param tleStringList
	 * @param calendar
	 * @throws SQLException
	 */
	private void addTLEtoDB(ArrayList<TLEString> tleStringList, Calendar calendar) throws SQLException {
		Iterator<TLEString> ite = tleStringList.iterator();
		TLEString tleString = null;
		String sql = "SELECT noradCatalogID,card1,card2 FROM tle_tbl" + " WHERE date = ? and noradcatalogid = ?";
		PreparedStatement selectStatement = dbConnection.prepareStatement(sql);
		PreparedStatement updateStatement;
		PreparedStatement insertStatement;
		ResultSet resultSet;
		while (ite.hasNext()) {
			tleString = ite.next();
			selectStatement.setDate(1, new Date(calendar.getTimeInMillis()));
			selectStatement.setString(2, String.valueOf(tleString.getNoradCatalogID()));
			resultSet = selectStatement.executeQuery();// result should be 1.
			if (resultSet.next()) {
				sql = "update tle_tbl set card1 = ?, card2 = ?, status = ? " + "where date = ? and noradcatalogid = ? ";
				updateStatement = dbConnection.prepareStatement(sql);
				updateStatement.setString(1, tleString.getLine1());
				updateStatement.setString(2, tleString.getLine2());
				updateStatement.setInt(3, tleString.getStatus());
				updateStatement.setDate(4, new Date(calendar.getTimeInMillis()));
				updateStatement.setString(5, tleString.getNoradCatalogID());
				updateStatement.execute();
				System.out.println("db updated.");
				updateStatement.close();
			} else {
				sql = "insert into tle_tbl (date, noradcatalogid, card1, card2, status) " + "values (?,?,?,?,?)";
				insertStatement = dbConnection.prepareStatement(sql);
				insertStatement.setDate(1, new Date(calendar.getTimeInMillis()));
				insertStatement.setString(2, tleString.getNoradCatalogID());
				insertStatement.setString(3, tleString.getLine1());
				insertStatement.setString(4, tleString.getLine2());
				insertStatement.setInt(5, tleString.getStatus());
				insertStatement.execute();
				System.out.println("db inserted.");
				insertStatement.close();
			}
		}
		selectStatement.close();
	}

	/**
	 * get TLE information from internal database and set the information to the
	 * List which is the first parameter of this method. before using this method,
	 * the connection with internal database should be established.
	 * 
	 * @param tleStringList
	 * @param noradCatalogIDList
	 * @param calendar
	 * @throws IOException
	 * @throws SQLException
	 */
	private void setTLEfmDB(ArrayList<TLEString> tleStringList, String[] noradCatalogIDList, Calendar calendar)
			throws IOException, SQLException {
		dbConnection = loadConnection();
		String sql = "SELECT noradCatalogID,card1,card2,status FROM tle_tbl" + " WHERE date = ? and ";
		sql += "(noradCatalogID = ? ";
		for (int i = 1; i < noradCatalogIDList.length; i++) {
			sql += "OR noradCatalogID = ? ";
		}
		sql += ") and (status = 1 OR status = 2)";
		PreparedStatement statement = dbConnection.prepareStatement(sql);
		statement.setDate(1, new Date(calendar.getTimeInMillis()));
		for (int i = 0; i < noradCatalogIDList.length; i++) {
			statement.setString(i + 2, noradCatalogIDList[i]);
		}
		ResultSet resultSet = statement.executeQuery();
		TLEString tleString;
		while (resultSet.next()) {
			tleString = new TLEString();
			tleString.setNoradCatalogID(resultSet.getString(1));
			tleString.setLine1(resultSet.getString(2));
			tleString.setLine2(resultSet.getString(3));
			tleString.setStatus(resultSet.getInt(4));
			tleStringList.add(tleString);
		}
		statement.close();
	}

	/**
	 * 
	 * @return Connection to PostgreSQL
	 * @throws IOException
	 * @throws SQLException
	 */
	protected Connection loadConnection() throws IOException, SQLException {

		String host = dbProperties.getProperty("hostname");
		String port = dbProperties.getProperty("port");
		String db = dbProperties.getProperty("database");
		String user = dbProperties.getProperty("user");
		String password = dbProperties.getProperty("password");

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
		}

		String url = format("jdbc:postgresql://%s:%s/%s", host, port, db);
		return getConnection(url, user, password);
	}

	/**
	 * @return list of string of norad catalog id
	 */
	/*
	private String[] getNoradCatalogID(String gnssStr) {
		String qzss = spaceTrackProperties.getProperty("QZSS");
		String gps = spaceTrackProperties.getProperty("GPS") + "," + spaceTrackProperties.getProperty("GPS-Block-IIF")
				+ "," + spaceTrackProperties.getProperty("GPS-III");
		String galileo = spaceTrackProperties.getProperty("GAL");
		String noradCatNumStr = "";
		if (gnssStr.contains("G") && gps != null) {
			noradCatNumStr += gps + ",";
		}
		if (gnssStr.contains("E") && galileo != null) {
			noradCatNumStr += galileo + ",";
		}
		if (gnssStr.contains("J") && qzss != null) {
			noradCatNumStr += qzss;
		}
		if (noradCatNumStr.endsWith(",")) {
			noradCatNumStr = noradCatNumStr.substring(0, noradCatNumStr.length() - 1);
		}
		return noradCatNumStr.split(",");
	}*/

	/**
	 * load properties files: space_track.ini and db.ini This method should be
	 * called before loadConnection().
	 * 
	 * @throws IOException
	 */
	public void loadPropertiesFiles() throws IOException {
		if (spaceTrackProperties == null) {
			spaceTrackProperties = new Properties();
			dbProperties = new Properties();
			try {
				if (context != null) {
					spaceTrackProperties.load(context.getResourceAsStream("WEB-INF/conf/space_track.ini"));
					dbProperties.load(context.getResourceAsStream("WEB-INF/conf/db.ini"));
				} else {
					spaceTrackProperties.load(new FileInputStream("src/main/webapp/WEB-INF/conf/space_track.ini"));
					dbProperties.load(new FileInputStream("src/main/webapp/WEB-INF/conf/db.ini"));
				}
			} catch (IOException e) {
				throw new IOException("during loading ini file: " + e.getMessage());
			}
		}
	}

	/**
	 * create query for space track.
	 * 
	 * @param gnssStr
	 * @param calendar
	 * @return
	 */
	private String createQueryForSpaceTrack(String[] noradCatalogIDList, Calendar calendar) {
		String query;
		String noradCatNumStr = noradCatalogIDList[0];
		for (int i = 1; i < noradCatalogIDList.length; i++) {
			noradCatNumStr += "," + noradCatalogIDList[i];
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
		query = "/basicspacedata/query/class/tle/EPOCH/";
		calendar.add(Calendar.DAY_OF_YEAR, -10);
		query += dateFormat.format(calendar.getTime());
		query += "--";
		calendar.add(Calendar.DAY_OF_YEAR, 10);
		query += dateFormat.format(calendar.getTime());
		// calendar.add(Calendar.DAY_OF_YEAR, -1);// move to original date.
		query += "/NORAD_CAT_ID/";
		query += noradCatNumStr;
		query += "/orderby/TLE_LINE1%20ASC/format/tle";
		return query;
	}

	/**
	 * Authentication. in this method, https connection will be created.
	 * 
	 * @throws IOException
	 */
	private void spaceTrackAuthentication() throws IOException {
		String userName = spaceTrackProperties.getProperty("user");
		String password = spaceTrackProperties.getProperty("password");
		CookieManager manager = new CookieManager();
		manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(manager);

		URL url = new URL(baseURL + authPath);

		httpsConnection = (HttpsURLConnection) url.openConnection();
		httpsConnection.setDoOutput(true);
		httpsConnection.setRequestMethod("POST");

		String input = "identity=" + userName + "&password=" + password;

		OutputStream os = httpsConnection.getOutputStream();
		os.write(input.getBytes());
		os.flush();
		BufferedReader br = new BufferedReader(new InputStreamReader((httpsConnection.getInputStream())));

		String output;
		System.out.println("Output from Server .... \n");
		while ((output = br.readLine()) != null) {
			System.out.println(output);
		}
	}

}
