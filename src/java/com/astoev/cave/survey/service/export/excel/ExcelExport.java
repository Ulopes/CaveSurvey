package com.astoev.cave.survey.service.export.excel;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.astoev.cave.survey.Constants;
import com.astoev.cave.survey.R;
import com.astoev.cave.survey.model.Gallery;
import com.astoev.cave.survey.model.Leg;
import com.astoev.cave.survey.model.Location;
import com.astoev.cave.survey.model.Note;
import com.astoev.cave.survey.model.Option;
import com.astoev.cave.survey.model.Photo;
import com.astoev.cave.survey.model.Point;
import com.astoev.cave.survey.model.Project;
import com.astoev.cave.survey.model.Sketch;
import com.astoev.cave.survey.model.Vector;
import com.astoev.cave.survey.service.Options;
import com.astoev.cave.survey.util.DaoUtil;
import com.astoev.cave.survey.util.FileStorageUtil;
import com.astoev.cave.survey.util.LocationUtil;
import com.astoev.cave.survey.util.StringUtils;

import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Exports the project's data as xsl file  
 * 
 * User: astoev
 * Date: 2/12/12
 * Time: 7:25 PM
 * 
 * @author astoev
 * @author jmitrev
 */
public class ExcelExport {

    private static int CELL_FROM = 0;
    private static int CELL_TO = 1;
    private static int CELL_LENGHT = 2;
    private static int CELL_AZIMUTH = 3;
    private static int CELL_SLOPE = 4;
    private static int CELL_LEFT = 5;
    private static int CELL_RIGHT = 6;
    private static int CELL_UP = 7;
    private static int CELL_DOWN = 8;
    private static int CELL_NOTE = 9;
    private static int CELL_LATITUDE = 10;
    private static int CELL_LONGITUDE = 11;
    private static int CELL_ALTTITUDE = 12;
    private static int CELL_ACCURACY = 13;
    private static int CELL_DRAWING = 14;
    private static int CELL_PHOTO = 15;
    
    private Context mContext;

    public ExcelExport(Context aContext) {
        mContext = aContext;
    }

    public String runExport(Project aProject) throws Exception {

        Log.i(Constants.LOG_TAG_SERVICE, "Start excel export ");

        Workbook wb = new HSSFWorkbook();

        try {

            Sheet sheet = createHeader(aProject.getName(), wb);

            CreationHelper helper = wb.getCreationHelper();

            // legs
            List<Leg> legs = DaoUtil.getCurrProjectLegs(false);

            int rowCounter = 0;
            Integer lastGalleryId = null, prevGalleryId = null;
            SparseArray<String> galleryNames = new SparseArray<String>();

            for (Leg l : legs) {


                Point fromPoint = l.getFromPoint();
                DaoUtil.refreshPoint(fromPoint);
                Point toPoint = l.getToPoint();
                DaoUtil.refreshPoint(toPoint);

                if (Constants.STRING_NOT_FOUND.equals(galleryNames.get(l.getGalleryId(), Constants.STRING_NOT_FOUND))) {
                    Gallery gallery = DaoUtil.getGallery(l.getGalleryId());
                    galleryNames.put(l.getGalleryId(), gallery.getName());
                }
                if (lastGalleryId == null) {
                    lastGalleryId = l.getGalleryId();
                }

                if (l.getGalleryId().equals(lastGalleryId)) {
                    prevGalleryId = l.getGalleryId();
                } else {
                    prevGalleryId = DaoUtil.getLegByToPointId(l.getFromPoint().getId()).getGalleryId();
                }

                List<Leg> middles = DaoUtil.getLegsMiddles(l);

                // postpone display logic if will be handled inside middles
                if (middles == null || middles.size() == 0) {

                    rowCounter++;

                    Row legRow = sheet.createRow(rowCounter);
                    Cell from = legRow.createCell(CELL_FROM);

                    if (l.getGalleryId().equals(lastGalleryId)) {
                        from.setCellValue(galleryNames.get(l.getGalleryId()) + fromPoint.getName());
                    } else {
                        from.setCellValue(galleryNames.get(prevGalleryId) + fromPoint.getName());
                    }

                    Cell to = legRow.createCell(CELL_TO);
                    to.setCellValue(galleryNames.get(l.getGalleryId()) + toPoint.getName());

                    // distance, azimuth, inclination
                    exportLegMeasures(l, legRow);

                    //up/down/left/right
                    exportArroundMeasures(l, legRow);

                    // note
                    exportNote(l, legRow, wb);

                    // location
                    exportLocation(legRow, fromPoint);

                    // sketch
                    exportSketches(helper, l, legRow);

                    // picture
                    exportPhotos(helper, l, legRow);

                }

                // middles
                if (middles != null && middles.size() > 0) {

                    int index = 0;
                    float prevLength = 0;
                    String fromPointName;
                    if (l.getGalleryId().equals(prevGalleryId)) {
                        fromPointName = galleryNames.get(l.getGalleryId()) + fromPoint.getName();
                    } else {
                        fromPointName = galleryNames.get(prevGalleryId) + fromPoint.getName();
                    }
                    String toPointName = galleryNames.get(l.getGalleryId()) + toPoint.getName();
                    String lastMiddleName = null;

                    // middles
                    Leg prevMiddle = null;
                    for (Leg middle : middles) {
                        rowCounter++;
                        index ++;
                        Row middleRow = sheet.createRow(rowCounter);

                        Cell middleFrom = middleRow.createCell(CELL_FROM);
                        middleFrom.setCellValue(lastMiddleName == null ? fromPointName : lastMiddleName);

                        Cell middleTo = middleRow.createCell(CELL_TO);
                        middleTo.setCellValue(fromPointName + "-" + toPointName + "@" + StringUtils.floatToLabel(middle.getMiddlePointDistance()));
                        lastMiddleName = middleTo.getStringCellValue();

                        Cell middleLength = middleRow.createCell(CELL_LENGHT);
                        middleLength.setCellValue(StringUtils.floatToLabel(middle.getMiddlePointDistance() - prevLength));

                        exportLegCompass(l, middleRow);
                        exportLegSlope(l, middleRow);


                        if (index == 1) {
                            exportArroundMeasures(l, middleRow);

                            // export extras skipped above
                            exportNote(l, middleRow, wb);
                            exportLocation(middleRow, fromPoint);
                            exportSketches(helper, l, middleRow);
                            exportPhotos(helper, l, middleRow);
                        } else {
                            exportArroundMeasures(prevMiddle, middleRow);
                        }

                        prevLength = middle.getMiddlePointDistance();
                        prevMiddle = middle;
                    }

                    // last explicit leg
                    rowCounter++;
                    Row middleLastRow = sheet.createRow(rowCounter);

                    Cell middleLastFrom = middleLastRow.createCell(CELL_FROM);
                    middleLastFrom.setCellValue(lastMiddleName);

                    Cell middleLastTo = middleLastRow.createCell(CELL_TO);
                    middleLastTo.setCellValue(toPointName);

                    Cell middleLastDistance = middleLastRow.createCell(CELL_LENGHT);
                    middleLastDistance.setCellValue(StringUtils.floatToLabel(l.getDistance() - prevLength));

                    exportLegCompass(l, middleLastRow);
                    exportLegSlope(l, middleLastRow);

                    exportArroundMeasures(middles.get(middles.size()-1), middleLastRow);

                }

                // vectors
                List<Vector> vectors = DaoUtil.getLegVectors(l);
                if (vectors != null) {
                    int vectorCounter = 1;
                    for (Vector v : vectors) {
                        rowCounter++;
                        Row vectorRow = sheet.createRow(rowCounter);

                        Cell vectorFrom = vectorRow.createCell(CELL_FROM);
                        String fromPointName;
                        if (l.getGalleryId().equals(prevGalleryId)) {
                            fromPointName = galleryNames.get(l.getGalleryId()) + fromPoint.getName();
                        } else {
                            fromPointName = galleryNames.get(prevGalleryId) + fromPoint.getName();
                        }
                        vectorFrom.setCellValue(fromPointName);

                        Cell vectorTo = vectorRow.createCell(CELL_TO);
                        vectorTo.setCellValue(fromPointName + "-" + galleryNames.get(l.getGalleryId()) + toPoint.getName() + "-v" + vectorCounter);

                        Cell vectorLength = vectorRow.createCell(CELL_LENGHT);
                        vectorLength.setCellValue(StringUtils.floatToLabel(v.getDistance()));

                        Cell vectorCompass = vectorRow.createCell(CELL_AZIMUTH);
                        vectorCompass.setCellValue(StringUtils.floatToLabel(v.getAzimuth()));

                        Cell vectorClinometer = vectorRow.createCell(CELL_SLOPE);
                        vectorClinometer.setCellValue(StringUtils.floatToLabel(v.getSlope()));

                        vectorCounter++;
                    }
                }

                lastGalleryId = l.getGalleryId();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            return FileStorageUtil.addProjectExport(aProject, in);
        } catch (Exception t) {
            Log.e(Constants.LOG_TAG_SERVICE, "Failed with export", t);
            throw t;
        }
    }

    private void exportLegMeasures(Leg l, Row legRow) throws SQLException {
        exportLegDistance(l, legRow);
        exportLegCompass(l,legRow);
        exportLegSlope(l, legRow);
    }

    private void exportNote(Leg l, Row legRow, Workbook wb) throws SQLException {
        Cell note = legRow.createCell(CELL_NOTE);
        Note n = DaoUtil.getActiveLegNote(l);
        if (n != null) {
            note.setCellValue(n.getText());
            CellStyle cs = wb.createCellStyle();
            cs.setWrapText(true);
            note.setCellStyle(cs);
        }
    }

    private void exportLegSlope(Leg l, Row legRow) throws SQLException {
        Cell clinometer = legRow.createCell(CELL_SLOPE);
        if (l.getSlope() != null) {
            clinometer.setCellValue(StringUtils.floatToLabel(l.getSlope()));
        }
    }

    private void exportLegDistance(Leg l, Row legRow) throws SQLException {
        Cell length = legRow.createCell(CELL_LENGHT);
        if (l.getDistance() != null) {
            length.setCellValue(StringUtils.floatToLabel(l.getDistance()));
        }
    }


    private void exportLegCompass(Leg l, Row legRow) throws SQLException {
        Cell compass = legRow.createCell(CELL_AZIMUTH);
        if (l.getAzimuth() != null) {
            compass.setCellValue(StringUtils.floatToLabel(l.getAzimuth()));
        }
    }

    private void exportArroundMeasures(Leg l, Row legRow) throws SQLException {
        Cell left = legRow.createCell(CELL_LEFT);
        if (l.getLeftDistance() != null) {
            left.setCellValue(StringUtils.floatToLabel(l.getLeftDistance()));
        }
        Cell right = legRow.createCell(CELL_RIGHT);
        if (l.getRightDistance() != null) {
            right.setCellValue(StringUtils.floatToLabel(l.getRightDistance()));
        }
        Cell up = legRow.createCell(CELL_UP);
        if (l.getTopDistance() != null) {
            up.setCellValue(StringUtils.floatToLabel(l.getTopDistance()));
        }
        Cell down = legRow.createCell(CELL_DOWN);
        if (l.getDownDistance() != null) {
            down.setCellValue(StringUtils.floatToLabel(l.getDownDistance()));
        }
    }

    private void exportPhotos(CreationHelper helper, Leg l, Row legRow) throws SQLException {
        Photo photo = DaoUtil.getPhotoByLeg(l);
        if (photo != null) {
            Hyperlink fileLink = helper.createHyperlink(HSSFHyperlink.LINK_FILE);

            Cell photoCell = legRow.createCell(CELL_PHOTO);
            String path = photo.getFSPath();
            String name = new File(path).getName();

            fileLink.setAddress(name);
            photoCell.setCellValue(name);
            photoCell.setHyperlink(fileLink);
        }
    }

    private void exportSketches(CreationHelper helper, Leg l, Row legRow) throws SQLException {
        Sketch sketch = DaoUtil.getScetchByLeg(l);
        if (sketch != null) {
            Hyperlink fileLink = helper.createHyperlink(HSSFHyperlink.LINK_FILE);

            Cell sketchCell = legRow.createCell(CELL_DRAWING);
            String path = sketch.getFSPath();
            String name = new File(path).getName();

            fileLink.setAddress(name);
            sketchCell.setCellValue(name);
            sketchCell.setHyperlink(fileLink);
        }
    }

    private void exportLocation(Row legRow, Point fromPoint) throws SQLException {
        Location location = DaoUtil.getLocationByPoint(fromPoint);
        if (location != null) {
            Cell latitude = legRow.createCell(CELL_LATITUDE);
            latitude.setCellValue(LocationUtil.formatLatitude(location.getLatitude()));
            Cell longitude = legRow.createCell(CELL_LONGITUDE);
            longitude.setCellValue(LocationUtil.formatLongitude(location.getLongitude()));
            Cell altitude = legRow.createCell(CELL_ALTTITUDE);
            altitude.setCellValue(location.getAltitude());
            Cell accuracy = legRow.createCell(CELL_ACCURACY);
            accuracy.setCellValue(location.getAccuracy());
        }
    }

    private Sheet createHeader(String aProjectName, Workbook wb) {
        Sheet sheet = wb.createSheet(aProjectName);
        Row headerRow = sheet.createRow(0);
        // header cells
        Cell headerFrom = headerRow.createCell(CELL_FROM);
        headerFrom.setCellValue(mContext.getString(R.string.main_table_header_from));
        Cell headerTo = headerRow.createCell(CELL_TO);
        headerTo.setCellValue(mContext.getString(R.string.main_table_header_to));
        Cell headerLength = headerRow.createCell(CELL_LENGHT);
        String distanceTitle = mContext.getString(R.string.distance) + " - " + Options.getOptionValue(Option.CODE_DISTANCE_UNITS);
        headerLength.setCellValue(distanceTitle);
        Cell headerCompass = headerRow.createCell(CELL_AZIMUTH);
        String azimuthTitle = mContext.getString(R.string.azimuth) + " - " + Options.getOptionValue(Option.CODE_AZIMUTH_UNITS);
        headerCompass.setCellValue(azimuthTitle);
        Cell headerClinometer = headerRow.createCell(CELL_SLOPE);
        String clinometerTitle = mContext.getString(R.string.slope) + " - " + Options.getOptionValue(Option.CODE_SLOPE_UNITS);
        headerClinometer.setCellValue(clinometerTitle);
        Cell headerLeft = headerRow.createCell(CELL_LEFT);
        headerLeft.setCellValue(mContext.getString(R.string.left));
        Cell headerRight = headerRow.createCell(CELL_RIGHT);
        headerRight.setCellValue(mContext.getString(R.string.right));
        Cell headerUp = headerRow.createCell(CELL_UP);
        headerUp.setCellValue(mContext.getString(R.string.up));
        Cell headerDown = headerRow.createCell(CELL_DOWN);
        headerDown.setCellValue(mContext.getString(R.string.down));
        Cell headerNote = headerRow.createCell(CELL_NOTE);
        headerNote.setCellValue(mContext.getString(R.string.main_table_header_note));

        Cell gpsLatitude = headerRow.createCell(CELL_LATITUDE);//gps_latitude
        gpsLatitude.setCellValue(mContext.getString(R.string.gps_latitude));
        Cell gpsLongitude = headerRow.createCell(CELL_LONGITUDE);
        gpsLongitude.setCellValue(mContext.getString(R.string.gps_longitude));
        Cell gpsAltitude = headerRow.createCell(CELL_ALTTITUDE);
        gpsAltitude.setCellValue(mContext.getString(R.string.gps_altitude));
        Cell gpsAccuracy = headerRow.createCell(CELL_ACCURACY);
        gpsAccuracy.setCellValue(mContext.getString(R.string.gps_accuracy));

        Cell headerDrawing = headerRow.createCell(CELL_DRAWING);
        headerDrawing.setCellValue(mContext.getString(R.string.main_table_header_drawing));
        Cell headerPhoto = headerRow.createCell(CELL_PHOTO);
        headerPhoto.setCellValue(mContext.getString(R.string.main_table_header_photo));
        return sheet;
    }
}