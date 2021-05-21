package org.braincopy.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

import org.braincopy.orbit.ConstantNumber;
import org.braincopy.orbit.PositionECI;
import org.braincopy.orbit.PositionLLH;
import org.junit.Test;

import sgp4v.ObjectDecayed;
import sgp4v.SatElsetException;
import sgp4v.Sgp4Data;
import sgp4v.Sgp4Unit;

public class SpaceTrackWorkerTest {
    @Test
    public void test() {
        System.out.println("hello!");

        Sgp4Unit sgp4 = new Sgp4Unit();
        TLEString tempTLE = new TLEString();

        // This is TLE for ISS at May 11th 2021 11:30 JST
        // WEST of NewZealand Lat 40S, Lon 142W
        tempTLE.setLine1("1 25544U 98067A   21130.82534197  .00000540  00000-0  17959-4 0  9990");
        tempTLE.setLine2("2 25544  51.6449 167.5222 0002900 345.0941 184.7772 15.48989745282809");
        tempTLE.setNoradCatalogID("25544");

        SpaceTrackWorker worker = new SpaceTrackWorker();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2021, 4, 11, 02, 30, 00);// 4 means May
        int startYear = 2021, stopYear = 2021;
        double startDay, stopDay;

        // System.out.println(calendar.getTime());
        startYear = calendar.get(Calendar.YEAR);
        startDay = worker.getDoubleDay(calendar);
        calendar.add(Calendar.MINUTE, 90);
        // System.out.println(calendar.getTime());
        stopYear = calendar.get(Calendar.YEAR);
        stopDay = worker.getDoubleDay(calendar);

        int step = 100;// sec

        try {
            Vector<Sgp4Data> results = sgp4.runSgp4(tempTLE.getLine1(), tempTLE.getLine2(), startYear, startDay,
                    stopYear, stopDay, (double)step / 60.0);

            PositionECI posEci = null;
            PositionLLH posLLH = null;
            GregorianCalendar dateAndTime = null;
            double days = 0;

            Sgp4Data data = null;
            for (int i = 0; i < results.size(); i++) {
                data = (Sgp4Data) results.elementAt(i);
                days = startDay + i * (double) step / (60.0 * 60.0 * 24.0);
                dateAndTime = worker.getCalendarFmYearAndDays(startYear, days);

                posEci = new PositionECI(data.getX() * ConstantNumber.RADIUS_OF_EARTH,
                        data.getY() * ConstantNumber.RADIUS_OF_EARTH, data.getZ() * ConstantNumber.RADIUS_OF_EARTH,
                        dateAndTime);
                posLLH = posEci.convertToECEF().convertToLLH();

                System.out.println(tempTLE.getNoradCatalogID() + "\t" + dateAndTime.get(Calendar.YEAR) + "/"
                        + (dateAndTime.get(Calendar.MONTH) + 1) + "/" + dateAndTime.get(Calendar.DAY_OF_MONTH) + " "
                        + dateAndTime.get(Calendar.HOUR_OF_DAY) + ":" + dateAndTime.get(Calendar.MINUTE) + "\t"
                        + posLLH.getLat() * 180 / Math.PI + "\t " + posLLH.getLon() * 180 / Math.PI + "\t"
                        + posLLH.getHeight());
            }
        } catch (ObjectDecayed e) {
            fail("cannot create sgp4data Vector: " + e);
            e.printStackTrace();
        } catch (SatElsetException e) {
            fail("cannot create sgp4data Vector2: " + e);
            e.printStackTrace();
        } // step's unit is

    }

    @Test
    public void testGetDoubleDay() {
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2021, 4, 11, 02, 30, 00);// 4 means May 11 2:30.00
        SpaceTrackWorker worker = new SpaceTrackWorker();
        double days = worker.getDoubleDay(calendar);
        assertEquals(131.10417, days, 0.001);
        calendar.add(Calendar.SECOND,5400);
        days = worker.getDoubleDay(calendar);
        assertEquals(131.16666, days, 0.001);
        System.out.println(""+days);
    }
}
