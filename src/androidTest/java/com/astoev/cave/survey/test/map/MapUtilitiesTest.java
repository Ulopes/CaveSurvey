package com.astoev.cave.survey.test.map;

import com.astoev.cave.survey.activity.map.MapUtilities;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Created by astoev on 1/20/14.
 */
public class MapUtilitiesTest extends TestCase {

    public void testGetNextGalleryColor() {
        int count = (int) (Math.random() * 5);
        int initial = MapUtilities.getNextGalleryColor(count);

        for (int i=0; i<3; i++) {
            // predictable for same input
            assertEquals(initial, MapUtilities.getNextGalleryColor(count));

            // different for new input
            assertNotSame(initial, MapUtilities.getNextGalleryColor(i));
        }
    }

    public void testIsAzimuthInDegreesValid() {
        assertTrue(MapUtilities.isAzimuthInDegreesValid(0f));
        assertTrue(MapUtilities.isAzimuthInDegreesValid(20f));
        assertTrue(MapUtilities.isAzimuthInDegreesValid(20.1f));
        assertTrue(MapUtilities.isAzimuthInDegreesValid(355f));

        assertFalse(MapUtilities.isAzimuthInDegreesValid(-1f));
        assertFalse(MapUtilities.isAzimuthInDegreesValid(360f));
        assertFalse(MapUtilities.isAzimuthInDegreesValid(365f));
    }

    public void testIsAzimuthInGradsValid() {
        assertTrue(MapUtilities.isAzimuthInGradsValid(0f));
        assertTrue(MapUtilities.isAzimuthInGradsValid(20f));
        assertTrue(MapUtilities.isAzimuthInGradsValid(20.1f));
        assertTrue(MapUtilities.isAzimuthInGradsValid(375f));

        assertFalse(MapUtilities.isAzimuthInGradsValid(-1f));
        assertFalse(MapUtilities.isAzimuthInGradsValid(400f));
        assertFalse(MapUtilities.isAzimuthInGradsValid(420f));
    }

    public void testIsSlopeInDegreesValid() {
        assertTrue(MapUtilities.isSlopeInDegreesValid(0f));
        assertTrue(MapUtilities.isSlopeInDegreesValid(-2.5f));
        assertTrue(MapUtilities.isSlopeInDegreesValid(5.2f));
        assertTrue(MapUtilities.isSlopeInDegreesValid(90f));
        assertTrue(MapUtilities.isSlopeInDegreesValid(-90f));

        assertFalse(MapUtilities.isSlopeInDegreesValid(-91f));
        assertFalse(MapUtilities.isSlopeInDegreesValid(-100f));
        assertFalse(MapUtilities.isSlopeInDegreesValid(+91f));
        assertFalse(MapUtilities.isSlopeInDegreesValid(+100f));
    }

    public void testIsSlopeInGradsValid() {
        assertTrue(MapUtilities.isSlopeInGradsValid(0f));
        assertTrue(MapUtilities.isSlopeInGradsValid(-2.5f));
        assertTrue(MapUtilities.isSlopeInGradsValid(5.2f));
        assertTrue(MapUtilities.isSlopeInGradsValid(100f));
        assertTrue(MapUtilities.isSlopeInGradsValid(-100f));

        assertFalse(MapUtilities.isSlopeInGradsValid(-101f));
        assertFalse(MapUtilities.isSlopeInGradsValid(-120f));
        assertFalse(MapUtilities.isSlopeInGradsValid(+101f));
        assertFalse(MapUtilities.isSlopeInGradsValid(+120f));
    }

    public void testGetMiddleAngle() {
        assertEquals(20f, MapUtilities.getMiddleAngle(10f, 30f));
        assertEquals(130f, MapUtilities.getMiddleAngle(50f, 210f));
        assertEquals(330f, MapUtilities.getMiddleAngle(340f, 320f));
        assertEquals(20f, MapUtilities.getMiddleAngle(340f, 60f));
    }

    public void testApplySlopeToDistance() {
        assertEquals(20f, MapUtilities.applySlopeToDistance(20f, null));
        assertEquals(20f, MapUtilities.applySlopeToDistance(20f, 0f));
        // TODO it seem bad values are produced below
//        assertEquals(10f, MapUtilities.applySlopeToDistance(20f, 45f));
//        assertEquals(10f, MapUtilities.applySlopeToDistance(20f, -45f));
//        assertEquals(0f, MapUtilities.applySlopeToDistance(20f, -90f));
//        assertEquals(0f, MapUtilities.applySlopeToDistance(20f, 90f));
    }

    public void testAddDegrees() {
        assertEquals(110f, MapUtilities.add90Degrees(20f));
        assertEquals(210f, MapUtilities.add90Degrees(120f));
        assertEquals(310f, MapUtilities.add90Degrees(220f));
        assertEquals(50f, MapUtilities.add90Degrees(320f));
    }

    public void testMinusDegrees() {
        assertEquals(10f, MapUtilities.minus90Degrees(100f));
        assertEquals(220f, MapUtilities.minus90Degrees(310f));
        assertEquals(330f, MapUtilities.minus90Degrees(60f));
    }

}
