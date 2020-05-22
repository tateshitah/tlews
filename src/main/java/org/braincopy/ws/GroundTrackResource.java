/*

Copyright (c) 2013 - 2020 Hiroaki Tateshita

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

This program is originally from JAXA 
https://github.com/JAXA-OPEN-API/example_api
and expanded by Hiroaki Tateshita

the portions are :
 
Copyright (c) 2013 jaxa

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

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.braincopy.orbit.ConstantNumber;
import org.braincopy.orbit.PositionECI;
import org.braincopy.orbit.PositionLLH;

import sgp4v.ObjectDecayed;
import sgp4v.SatElsetException;
import sgp4v.Sgp4Data;
import sgp4v.Sgp4Unit;

/**
 * 
 * @author Hiroaki Tateshita
 * @version 0.6.0
 * 
 */
//@WebServlet("/groundTrack")
@Path("groundTrack")
public class GroundTrackResource {

	protected static DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
	protected static DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
	protected static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	@Context
	ServletContext context;

	public GroundTrackResource() {
		super();
		DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
		TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * 
	 * @param format
	 *            xml, json or jsonp
	 * @param dateTimeStr
	 *            YYYY-MM-DD_HH:mm:ss
	 * @param term
	 *            [s]
	 * @param step
	 *            [s]
	 * @param noradCatId
	 * 			  4 or 5 digits number assumed
	 * @param callback
	 * @return
	 */
	@GET
	public Response getIt(@DefaultValue("xml") @QueryParam("format") String format,
			@DefaultValue("-9999") @QueryParam("dateTime") String dateTimeStr,
			@DefaultValue("86400") @QueryParam("term") int term, @DefaultValue("86400") @QueryParam("step") int step,
			@DefaultValue("25544") @QueryParam("norad_cat_id") String noradCatId,
			@DefaultValue("callback") @QueryParam("callback") String callback) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		try {
			calendar.setTime(DATETIME_FORMAT.parse(dateTimeStr));
			// System.err.println("debug: HourOfDay should be 12-> "
			// + calendar.get(Calendar.HOUR_OF_DAY));
		} catch (ParseException e) {
			return getFormattedError(Response.status(406),
					"Invalid Parameter: \"dateTime\", " + e.getLocalizedMessage() + ".", format, callback);
		}

		SpaceTrackWorker worker = new SpaceTrackWorker();
		worker.setContext(context);
		int startYear, stopYear;
		double startDay, stopDay;

		String dataEntity = "";
		int numOfResultsOfEachSat = term / step;
		if (numOfResultsOfEachSat == 0) {
			numOfResultsOfEachSat = 1;
		}
		try {
			ArrayList<TLEString> tleList = worker.getTLEList(calendar, noradCatId);
			Iterator<TLEString> ite = tleList.iterator();
			TLEString tempTLE;
			Sgp4Unit sgp4 = new Sgp4Unit();

			while (ite.hasNext()) {
				tempTLE = (TLEString) ite.next();
				if (tempTLE.isAvailable()) {
					startYear = calendar.get(Calendar.YEAR);
					startDay = worker.getDoubleDay(calendar);
					calendar.add(Calendar.SECOND, term);
					stopYear = calendar.get(Calendar.YEAR);
					stopDay = worker.getDoubleDay(calendar);
					calendar.add(Calendar.SECOND, -term);// move to original
															// date

					Vector<Sgp4Data> results = sgp4.runSgp4(tempTLE.getLine1(), tempTLE.getLine2(), startYear, startDay,
							stopYear, stopDay, step / 60);// step's unit is
															// second
					PositionECI posEci = null;
					PositionLLH posllh = null;
					GregorianCalendar dateAndTime = null;
					double days = 0;
					Sgp4Data data = null;
					for (int i = 0; i < numOfResultsOfEachSat; i++) {
						data = (Sgp4Data) results.elementAt(i);
						days = startDay + i * (double) step / (double) ConstantNumber.SECONDS_DAY;
						dateAndTime = worker.getCalendarFmYearAndDays(startYear, days);
						// System.err.println("debug: Month should be 7 (Aug)->
						// "
						// + dateAndTime.get(Calendar.MONTH));
						posEci = new PositionECI(data.getX() * ConstantNumber.RADIUS_OF_EARTH,
								data.getY() * ConstantNumber.RADIUS_OF_EARTH,
								data.getZ() * ConstantNumber.RADIUS_OF_EARTH, dateAndTime);
						posllh = posEci.convertToECEF().convertToLLH();
						dataEntity += createSatObsInfo(dateAndTime, tempTLE.getNoradCatalogID(), posllh.getLat(),
								posllh.getLon(), posllh.getHeight(), format);
					}
				}
			}
			if ("json".equalsIgnoreCase(format) || "jsonp".equalsIgnoreCase(format)) {
				dataEntity = dataEntity.substring(0, dataEntity.length() - 1);
			}
			return getFormattedResponse(Response.ok(), dataEntity, format, callback);

		} catch (IOException e) {
			System.err.println("please check the ini file: " + e.getMessage());
			return getFormattedError(Response.status(501), "contact system admin. might be ini file problem", format,
					callback);
		} catch (ObjectDecayed e) {
			System.err.println("please check the ini file: " + e.getMessage());
			return getFormattedError(Response.status(501), "contact system admin. might be ini file problem", format,
					callback);
		} catch (SatElsetException e) {
			System.err.println("please check the ini file: " + e.getMessage());
			return getFormattedError(Response.status(501), "contact system admin. might be ini file problem", format,
					callback);
		} catch (SQLException e) {
			return getFormattedError(Response.status(501), "database problem. ", format, callback);
		}

	}

	/**
	 * 
	 * @param builder
	 * @param data_entity
	 * @param format
	 * @param callback
	 * @return
	 */
	private Response getFormattedResponse(ResponseBuilder builder, String data_entity, String format, String callback) {
		if ("xml".equalsIgnoreCase(format)) {
			String entity = format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<response><result>ok</result><values>%s</values>" + "<ver>0.5.0</ver></response>", data_entity);
			builder = builder.entity(entity);
			builder = builder.type(MediaType.TEXT_XML_TYPE);
		} else if ("json".equalsIgnoreCase(format)) {
			String entity = format("{\"result\":\"ok\",\"values\":[%s]}", data_entity);
			builder = builder.entity(entity);
			builder = builder.type(MediaType.APPLICATION_JSON_TYPE);
		} else if ("jsonp".equalsIgnoreCase(format)) {
			String entity = format("%s({\"result\":\"ok\",\"values\":[%s]})", callback, data_entity);
			builder = builder.entity(entity);
			builder = builder.type("application/javascript");
		} else {
			String entity = format("%s", data_entity);
			builder = builder.entity(entity);
		}
		builder = builder.encoding("utf-8");
		return builder.build();
	}

	/**
	 * 
	 * @param dateAndTime
	 * @param noradCatalogID
	 * @param latitude
	 * @param longitude
	 * @param height
	 * @param format
	 * @return
	 */
	public String createSatObsInfo(GregorianCalendar dateAndTime, String noradCatalogID, double latitude,
			double longitude, double height, String format) {
		String result = "";

		if ("xml".equalsIgnoreCase(format)) {
			result = format(
					"<SatObservation><Sensor><SensorLocation>" + "<Latitude units=\"degrees\">%f</Latitude>"
							+ "<Longitude units=\"degrees\">%f</Longitude>" + "<Altitude units=\"meters\">%f</Altitude>"
							+ "</SensorLocation></Sensor>" + "<SatelliteNumber>%s</SatelliteNumber>"
							+ "<ObDate>%s</ObDate>" + "<ObTime>%s</ObTime></SatObservation>",
					latitude * 180 / Math.PI, longitude * 180 / Math.PI, height, noradCatalogID,
					DATE_FORMAT.format(dateAndTime.getTime()), TIME_FORMAT.format(dateAndTime.getTime()));
		} else if ("json".equalsIgnoreCase(format) || "jsonp".equalsIgnoreCase(format)) {
			result = format(
					"{\"SatObservation\": {" + "\"Sensor\": {" + "\"SensorLocation\": {" + "\"Latitude\": %f,"
							+ "\"Longitude\": %f,\"Altitude\": %f}}," + "\"SatelliteNumber\": %s,"
							+ "\"ObDate\": \"%s\"," + "\"ObTime\": \"%s\"}},",
					latitude * 180 / Math.PI, longitude * 180 / Math.PI, height, noradCatalogID,
					DATE_FORMAT.format(dateAndTime.getTime()), TIME_FORMAT.format(dateAndTime.getTime()));
		}
		return result;
	}

	/**
	 * 
	 * @param builder
	 * @param message
	 * @param format
	 * @return
	 */
	protected Response getFormattedError(ResponseBuilder builder, String message, String format) {
		if ("xml".equalsIgnoreCase(format)) {
			String entity = format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<response><result>error</result><message>%s</message></response>", message);
			builder = builder.entity(entity);
			builder = builder.type(MediaType.TEXT_XML_TYPE);
		} else if ("json".equalsIgnoreCase(format)) {
			String entity = format("{\"result\": \"error\", \"message\": \"%s\"}", message.replaceAll("\"", "\\\""));
			builder = builder.entity(entity);
			builder = builder.type(MediaType.APPLICATION_JSON_TYPE);
		} else {
			builder = builder.entity(message);
		}
		builder = builder.encoding("utf-8");
		return builder.build();
	}

	/**
	 * @param builder
	 * @param message
	 * @param format
	 * @param callback
	 * @return
	 */
	protected Response getFormattedError(ResponseBuilder builder, String message, String format, String callback) {
		if ("jsonp".equalsIgnoreCase(format)) {
			String entity = format("%s({\"result\": \"error\", \"message\": \"%s\"})", callback,
					message.replaceAll("\"", "\\\""));
			builder = builder.entity(entity);
			builder = builder.type("application/javascript");
			builder = builder.encoding("utf-8");
			return builder.build();
		} else {
			return getFormattedError(builder, message, format);
		}
	}
}
