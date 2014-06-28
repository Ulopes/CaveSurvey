package com.astoev.cave.survey.activity.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;

import com.astoev.cave.survey.Constants;
import com.astoev.cave.survey.R;
import com.astoev.cave.survey.activity.UIUtilities;
import com.astoev.cave.survey.model.Gallery;
import com.astoev.cave.survey.model.Leg;
import com.astoev.cave.survey.model.Vector;
import com.astoev.cave.survey.service.Workspace;
import com.astoev.cave.survey.util.DaoUtil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: astoev
 * Date: 2/1/12
 * Time: 11:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class MapView extends View {

    public static final int POINT_RADIUS = 3;
    public static final int MIDDLE_POINT_RADIUS = 2;
    public static final int MEASURE_POINT_RADIUS = 2;
    public static final int CURR_POINT_RADIUS = 8;
    private final static int LABEL_DEVIATION_X = 5;
    private final static int LABEL_DEVIATION_Y = 5;
    Paint polygonPaint = new Paint();
    Paint polygonWidthPaint = new Paint();
    Paint overlayPaint = new Paint();
    Paint youAreHerePaint = new Paint();
    Paint gridPaint = new Paint();
    Paint vectorsPaint = new Paint();
    Paint vectorPointPaint = new Paint();
    private float scale = 10;
    private int mapCenterMoveX = 0;
    private int mapCenterMoveY = 0;
    private float initialMoveX = 0;
    private float initialMoveY = 0;
    Point northCenter = new Point();

    List<Integer> processedLegs = new ArrayList<Integer>();
    
    private SparseArray<Point2D> mapPoints = new SparseArray<Point2D>();
    private SparseIntArray galleryColors = new SparseIntArray();
    private SparseArray<String> galleryNames = new SparseArray<String>();

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        polygonPaint.setColor(Color.RED);
        polygonPaint.setStrokeWidth(2);
        polygonWidthPaint.setColor(Color.RED);
        polygonWidthPaint.setStrokeWidth(1);
        overlayPaint.setColor(Color.WHITE);
        youAreHerePaint.setColor(Color.WHITE);
        youAreHerePaint.setAlpha(50);
        // semi transparent white
        gridPaint.setColor(Color.parseColor("#11FFFFFF"));
        gridPaint.setStrokeWidth(1);
        vectorsPaint.setStrokeWidth(1);
        vectorsPaint.setStyle(Paint.Style.STROKE);
        vectorsPaint.setPathEffect(new DashPathEffect(new float[]{2, 6}, 0));
        vectorsPaint.setAlpha(50);
        vectorPointPaint.setStrokeWidth(1);
        vectorPointPaint.setAlpha(50);
    }

    @Override
    public void onDraw(Canvas canvas) {

        try {
            processedLegs.clear();
            mapPoints.clear();
            galleryColors.clear();
            galleryNames.clear();

            // prepare map surface
            int maxX = canvas.getWidth();
            int maxY = canvas.getHeight();

            int centerX = maxX / 2;
            int centerY = maxY / 2;

            int spacing = 5;

            // grid
            int gridStep = 20;
            for (int x=0; x<maxX; x++) {
                canvas.drawLine(x*gridStep + spacing, spacing, x*gridStep + spacing, maxY - spacing, gridPaint);
            }

            for (int y=0; y<maxY; y++) {
                canvas.drawLine(spacing, y*gridStep + spacing, maxX - spacing, y*gridStep + spacing, gridPaint);
            }

            String pointLabel;

            // load the points
            List<Leg> legs = DaoUtil.getCurrProjectLegs(true);

            while (processedLegs.size() < legs.size()) {
                for (Leg l : legs) {

                    if (processedLegs.size() == legs.size()) {
                        break;
                    }

                    if (!processedLegs.contains(l.getId())) {
                        // first leg ever
                        Point2D first;

                        if (processedLegs.size() == 0) {
                            first = new Point2D(Float.valueOf(centerX), Float.valueOf(centerY), l.getLeftDistance(), l.getRightDistance(), MapUtilities.getAzimuthInDegrees(l.getAzimuth()));
                        } else {
                            // update previously created point with the correct values for left/right/up/down
                            first = mapPoints.get(l.getFromPoint().getId());
                            first.setLeft(l.getLeftDistance());
                            first.setRight(l.getRightDistance());
                            if (l.getAzimuth() != null) {
                                first.setAzimuth(MapUtilities.getAzimuthInDegrees(l.getAzimuth()));
                            } else {
                                first.setAzimuth(null);
                            }
                        }

                        if (mapPoints.get(l.getFromPoint().getId()) == null) {
                            if (!l.isMiddle()) {
                                mapPoints.put(l.getFromPoint().getId(), first);
                            }

                            // draw first point
                            if (!l.isMiddle()) {
                                //color
                                if (galleryColors.get(l.getGalleryId(), Constants.NOT_FOUND) == Constants.NOT_FOUND) {
                                    galleryColors.put(l.getGalleryId(), MapUtilities.getNextGalleryColor(galleryColors.size()));
                                    Gallery gallery = DaoUtil.getGallery(l.getGalleryId());
                                    galleryNames.put(l.getGalleryId(), gallery.getName());
                                }
                                polygonPaint.setColor(galleryColors.get(l.getGalleryId()));
                                polygonWidthPaint.setColor(galleryColors.get(l.getGalleryId()));
                                vectorsPaint.setColor(galleryColors.get(l.getGalleryId()));
                                vectorPointPaint.setColor(galleryColors.get(l.getGalleryId()));

                                DaoUtil.refreshPoint(l.getFromPoint());

                                pointLabel = galleryNames.get(l.getGalleryId()) + l.getFromPoint().getName();
                                canvas.drawText(pointLabel, mapCenterMoveX + first.getX() + LABEL_DEVIATION_X, mapCenterMoveY + first.getY() + LABEL_DEVIATION_Y, polygonPaint);
                                canvas.drawCircle(mapCenterMoveX + first.getX(), mapCenterMoveY + first.getY(), POINT_RADIUS, polygonPaint);
                            }
                        }

                        float deltaX;
                        float deltaY;
                        double galleryWidthAngle;
                        if (l.getDistance() == null || l.getAzimuth() == null) {
                            deltaX = 0;
                            deltaY = 0;
                        } else {
                            float legDistance;
                            if (l.isMiddle()) {
                                legDistance = MapUtilities.applySlopeToDistance(l.getMiddlePointDistance(), MapUtilities.getSlopeInDegrees(l.getSlope()));
                            } else {
                                legDistance = MapUtilities.applySlopeToDistance(l.getDistance(), MapUtilities.getSlopeInDegrees(l.getSlope()));
                            }
                            deltaY = -(float) (legDistance * Math.cos(Math.toRadians(MapUtilities.getAzimuthInDegrees(l.getAzimuth())))) * scale;
                            deltaX = (float) (legDistance * Math.sin(Math.toRadians(MapUtilities.getAzimuthInDegrees(l.getAzimuth())))) * scale;
                        }

                        Point2D second = new Point2D(first.getX() + deltaX, first.getY() + deltaY);

                        if (mapPoints.get(l.getToPoint().getId()) == null || l.isMiddle()) {
                            if (!l.isMiddle()) {
                                mapPoints.put(l.getToPoint().getId(), second);
                            }

                            // color
                            if (galleryColors.get(l.getGalleryId(), Constants.NOT_FOUND) == Constants.NOT_FOUND) {
                                galleryColors.put(l.getGalleryId(), MapUtilities.getNextGalleryColor(galleryColors.size()));
                                Gallery gallery = DaoUtil.getGallery(l.getGalleryId());
                                galleryNames.put(l.getGalleryId(), gallery.getName());
                            }
                            polygonPaint.setColor(galleryColors.get(l.getGalleryId()));
                            polygonWidthPaint.setColor(galleryColors.get(l.getGalleryId()));
                            vectorsPaint.setColor(galleryColors.get(l.getGalleryId()));
                            vectorPointPaint.setColor(galleryColors.get(l.getGalleryId()));

//                            Log.i(Constants.LOG_TAG_UI, "Drawing leg " + l.getFromPoint().getName() + ":" + l.getToPoint().getName() + "-" + l.getGalleryId());

                            if (Workspace.getCurrentInstance().getActiveLegId().equals(l.getId())) {
                                // you are here
                                if (l.isMiddle()) {
                                    canvas.drawCircle(mapCenterMoveX + second.getX(), mapCenterMoveY + second.getY(), CURR_POINT_RADIUS, youAreHerePaint);
                                } else {
                                    canvas.drawCircle(mapCenterMoveX + first.getX(), mapCenterMoveY + first.getY(), CURR_POINT_RADIUS, youAreHerePaint);
                                }

                            }

                            DaoUtil.refreshPoint(l.getToPoint());
                            if (l.isMiddle()) {
                                canvas.drawCircle(mapCenterMoveX + second.getX(), mapCenterMoveY + second.getY(), MIDDLE_POINT_RADIUS, polygonPaint);
                            } else {
                                pointLabel = galleryNames.get(l.getGalleryId()) + l.getToPoint().getName();
                                canvas.drawText(pointLabel, mapCenterMoveX + second.getX() + LABEL_DEVIATION_X, mapCenterMoveY + second.getY() + LABEL_DEVIATION_Y, polygonPaint);
                                canvas.drawCircle(mapCenterMoveX + second.getX(), mapCenterMoveY + second.getY(), POINT_RADIUS, polygonPaint);
                            }

                        }

                        // leg
                        if (!l.isMiddle()) {
                            canvas.drawLine(mapCenterMoveX + first.getX(), mapCenterMoveY + first.getY(), mapCenterMoveX + second.getX(), mapCenterMoveY + second.getY(), polygonPaint);
                        }

                        Leg prevLeg = DaoUtil.getLegByToPointId(l.getFromPoint().getId());

                        // left
                        if (first.getLeft() != null && first.getLeft()> 0) {
                            if (prevLeg == null  || l.isMiddle()) {
                                // first point by 90 degree left
                                galleryWidthAngle = Math.toRadians(MapUtilities.minus90Degrees(first.getAzimuth()));
                            } else {
                                // each other by the bisector
                                if (l.getGalleryId().equals(prevLeg.getGalleryId())) {
                                    galleryWidthAngle = Math.toRadians(MapUtilities.minus90Degrees(MapUtilities.getMiddleAngle(MapUtilities.getAzimuthInDegrees(prevLeg.getAzimuth()), first.getAzimuth())));
                                } else {
                                    galleryWidthAngle = Math.toRadians(MapUtilities.minus90Degrees(first.getAzimuth()));
                                }
                            }

                            deltaY = -(float) (first.getLeft() * Math.cos(galleryWidthAngle) * scale);
                            deltaX = (float) (first.getLeft() * Math.sin(galleryWidthAngle) * scale);
                            if (l.isMiddle()) {
                                canvas.drawCircle(mapCenterMoveX + second.getX() + deltaX, mapCenterMoveY + second.getY() + deltaY, MEASURE_POINT_RADIUS, polygonWidthPaint);
                            } else {
                                canvas.drawCircle(mapCenterMoveX + first.getX() + deltaX, mapCenterMoveY + first.getY() + deltaY, MEASURE_POINT_RADIUS, polygonWidthPaint);
                            }
                        }

                        // right
                        if (first.getRight() != null && first.getRight()> 0) {
                            if (prevLeg == null || l.isMiddle()) {
                                // first point by 90 degree left
                                galleryWidthAngle = Math.toRadians(MapUtilities.add90Degrees(first.getAzimuth()));
                            } else {
                                // each other by the bisector
                                if (l.getGalleryId().equals(prevLeg.getGalleryId())) {
                                    galleryWidthAngle = Math.toRadians(MapUtilities.add90Degrees(MapUtilities.getMiddleAngle(MapUtilities.getAzimuthInDegrees(prevLeg.getAzimuth()), first.getAzimuth())));
                                } else {
                                    galleryWidthAngle = Math.toRadians(MapUtilities.add90Degrees(first.getAzimuth()));
                                }
                            }
                            deltaY = -(float) (first.getRight() * Math.cos(galleryWidthAngle) * scale);
                            deltaX = (float) (first.getRight() * Math.sin(galleryWidthAngle) * scale);
                            if (l.isMiddle()) {
                                canvas.drawCircle(mapCenterMoveX + second.getX() + deltaX, mapCenterMoveY + second.getY() + deltaY, MEASURE_POINT_RADIUS, polygonWidthPaint);
                            } else {
                                canvas.drawCircle(mapCenterMoveX + first.getX() + deltaX, mapCenterMoveY + first.getY() + deltaY, MEASURE_POINT_RADIUS, polygonWidthPaint);
                            }
                        }

                        if (!l.isMiddle()) {
                            // vectors
                            List<Vector> vectors = DaoUtil.getLegVectors(l);
                            if (vectors != null) {
                                for (Vector v : vectors) {
                                    float legDistance = MapUtilities.applySlopeToDistance(v.getDistance(), MapUtilities.getSlopeInDegrees(v.getSlope()));
                                    deltaY = -(float) (legDistance * Math.cos(Math.toRadians(MapUtilities.getAzimuthInDegrees(v.getAzimuth())))) * scale;
                                    deltaX = (float) (legDistance * Math.sin(Math.toRadians(MapUtilities.getAzimuthInDegrees(v.getAzimuth())))) * scale;
                                    if (l.isMiddle()) {
                                        canvas.drawLine(mapCenterMoveX + second.getX(), mapCenterMoveY + second.getY(), mapCenterMoveX + second.getX() + deltaX, mapCenterMoveY + second.getY() + deltaY, vectorsPaint);
                                        canvas.drawCircle(mapCenterMoveX + second.getX() + deltaX, mapCenterMoveY + second.getY() + deltaY, 2, vectorPointPaint);
                                    } else {
                                        canvas.drawLine(mapCenterMoveX + first.getX(), mapCenterMoveY + first.getY(), mapCenterMoveX + first.getX() + deltaX, mapCenterMoveY + first.getY() + deltaY, vectorsPaint);
                                        canvas.drawCircle(mapCenterMoveX + first.getX() + deltaX, mapCenterMoveY + first.getY() + deltaY, 2, vectorPointPaint);
                                    }
                                }
                            }
                        }

                        processedLegs.add(l.getId());
                    }

                }
            }


            // borders
            //top
            canvas.drawLine(spacing, spacing, maxX - spacing, spacing, overlayPaint);
            //right
            canvas.drawLine(maxX - spacing, spacing, maxX - spacing, maxY - spacing, overlayPaint);
            // bottom
            canvas.drawLine(spacing, maxY - spacing, maxX - spacing, maxY - spacing, overlayPaint);
            //left
            canvas.drawLine(spacing, maxY - spacing, spacing, spacing, overlayPaint);

            // north
            northCenter.set(maxX - 20, 30);
            canvas.drawLine(northCenter.x, northCenter.y, northCenter.x + 10, northCenter.y + 10, overlayPaint);
            canvas.drawLine(northCenter.x + 10, northCenter.y + 10, northCenter.x, northCenter.y - 20, overlayPaint);
            canvas.drawLine(northCenter.x, northCenter.y - 20, northCenter.x - 10, northCenter.y + 10, overlayPaint);
            canvas.drawLine(northCenter.x - 10, northCenter.y + 10, northCenter.x, northCenter.y, overlayPaint);
            canvas.drawText("N", northCenter.x + 5, northCenter.y - 10, overlayPaint);

            canvas.drawText("x" + (int)scale, 20, 25, overlayPaint);

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to draw map activity", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    public void zoomOut() {
        scale--;
        invalidate();
    }

    public void zoomIn() {
        scale++;
        invalidate();
    }

    public boolean canZoomOut() {
        return scale > 1;
    }

    public boolean canZoomIn() {
        return scale < 50;
    }

    public void resetMove(float aX, float aY) {
        initialMoveX = aX;
        initialMoveY = aY;
    }

    public void move(float x, float y) {
        mapCenterMoveX += (x - initialMoveX);
        initialMoveX = x;
        mapCenterMoveY += (y - initialMoveY);
        initialMoveY = y;
        invalidate();
    }

    public byte[] getPngDump() {

        // render
        Bitmap returnedBitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = this.getBackground();
        if (bgDrawable!=null) {
            bgDrawable.draw(canvas);
        }
        draw(canvas);

        // crop borders etc
        returnedBitmap = Bitmap.createBitmap(returnedBitmap, 30, 30, this.getWidth() - 60, this.getHeight() - 60);

        // return
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        returnedBitmap.compress(Bitmap.CompressFormat.PNG, 50, buff);
        return buff.toByteArray();
    }

}
