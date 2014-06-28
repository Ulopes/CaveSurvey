package com.astoev.cave.survey.service.ormlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

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
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: astoev
 * Date: 1/24/12
 * Time: 10:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final int DATABASE_VERSION_1 = 1;
    private static final int DATABASE_VERSION_2 = 2;
    private static final int DATABASE_VERSION_3 = 3;
    private static final int DATABASE_VERSION_LATEST = 4;
    private static final String DATABASE_NAME = "CaveSurvey";

    private Dao<Leg, Integer> mLegDao;
    private Dao<Location, Integer> mLocationDao;
    private Dao<Note, Integer> mNoteDao;
    private Dao<Photo, Integer> mPhotoDao;
    private Dao<Point, Integer> mPointDao;
    private Dao<Project, Integer> mProjectDao;
    private Dao<Sketch, Integer> mSketchDao;
    private Dao<Gallery, Integer> mGalleryDao;
    private Dao<Option, Integer> mOptionsDao;
    private Dao<Vector, Integer> mVectorsDao;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION_LATEST, R.raw.ormlite_config);
        Log.i(Constants.LOG_TAG_DB, "Initialize DatabaseHelper");
        try {
            mLegDao = getDao(Leg.class);
            mLocationDao = getDao(Location.class);
            mNoteDao = getDao(Note.class);
            mPhotoDao = getDao(Photo.class);
            mPointDao = getDao(Point.class);
            mProjectDao = getDao(Project.class);
            mSketchDao = getDao(Sketch.class);
            mGalleryDao = getDao(Gallery.class);
            mOptionsDao = getDao(Option.class);
            mVectorsDao = getDao(Vector.class);

            Log.i(Constants.LOG_TAG_DB, "Dao's created");
        } catch (SQLException e) {
            Log.e(Constants.LOG_TAG_DB, "Failed to initialize DatabaseHelper", e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            Log.i(Constants.LOG_TAG_DB, "Create DB tables");
            TableUtils.createTableIfNotExists(connectionSource, Project.class);
            TableUtils.createTableIfNotExists(connectionSource, Note.class);
            TableUtils.createTableIfNotExists(connectionSource, Photo.class);
            TableUtils.createTableIfNotExists(connectionSource, Location.class);
            TableUtils.createTableIfNotExists(connectionSource, Sketch.class);
            TableUtils.createTableIfNotExists(connectionSource, Point.class);
            TableUtils.createTableIfNotExists(connectionSource, Leg.class);
            TableUtils.createTableIfNotExists(connectionSource, Gallery.class);
            TableUtils.createTableIfNotExists(connectionSource, Option.class);
            TableUtils.createTableIfNotExists(connectionSource, Vector.class);
            Log.i(Constants.LOG_TAG_DB, "Tables created");
        } catch (SQLException e) {
            Log.e(Constants.LOG_TAG_DB, "Failed to create DB tables", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase aSqLiteDatabase, ConnectionSource aConnectionSource, int aOldVersion, int aNewVersion) {

        if (aOldVersion < DATABASE_VERSION_LATEST) {

            Log.i(Constants.LOG_TAG_DB, "Performing DB update...");

            try {
                aSqLiteDatabase.beginTransaction();

                if (aOldVersion < DATABASE_VERSION_2) {
                    applyDBV2(aSqLiteDatabase);
                }

                if (aOldVersion < DATABASE_VERSION_3) {
                    applyDBV3(aSqLiteDatabase);
                }

//                if (aOldVersion < DATABASE_VERSION_LATEST) {
//                    applyDBV4(aSqLiteDatabase);
//                }

            } finally {
                aSqLiteDatabase.endTransaction();
            }

            Log.i(Constants.LOG_TAG_DB, "End DB update...");
        } else {
            Log.i(Constants.LOG_TAG_DB, "DB up to update");
        }
    }

    private void applyDBV4(SQLiteDatabase aSqLiteDatabase) {
        Log.i(Constants.LOG_TAG_DB, "Upgrading DB to V4");
        aSqLiteDatabase.execSQL("alter table legs add column left_vector_id decimal default null");
        aSqLiteDatabase.execSQL("alter table legs add column right_vector_id decimal default null");
        aSqLiteDatabase.execSQL("alter table legs add column up_vector_id decimal default null");
        aSqLiteDatabase.execSQL("alter table legs add column down_vector_id decimal default null");

        SQLiteStatement vectorInsert = aSqLiteDatabase.compileStatement("insert into vectors (id, point_id, gallery_id, distance)" +
                " values((select max(id) from vectors) + 1, ?, ?, ?)");
        SQLiteStatement legUpdate = aSqLiteDatabase.compileStatement("update legs set left_vector_id = ?, right_vector_id = ?," +
                "top_vector_id = ?, down_vector_id = ? where id = ? and gallery_id = ?");
        Cursor c = aSqLiteDatabase.rawQuery("select id, galleryid, left, right, top, down from legs", null);
        while (c.moveToNext()) {

            Long legId = c.getLong(0);
            Long galleryId = c.getLong(1);

            // left
            vectorInsert.bindLong(0, legId);
            vectorInsert.bindLong(1, galleryId);
            vectorInsert.bindLong(2, c.getLong(2));
            Long left_id = vectorInsert.executeInsert();

            // right
            vectorInsert.bindLong(0, legId);
            vectorInsert.bindLong(1, galleryId);
            vectorInsert.bindLong(2, c.getLong(3));
            Long right_id = vectorInsert.executeInsert();

            // top
            vectorInsert.bindLong(0, legId);
            vectorInsert.bindLong(1, galleryId);
            vectorInsert.bindLong(2, c.getLong(4));
            Long top_id = vectorInsert.executeInsert();

            // down
            vectorInsert.bindLong(0, legId);
            vectorInsert.bindLong(1, galleryId);
            vectorInsert.bindLong(2, c.getLong(5));
            Long down_id = vectorInsert.executeInsert();

            // update leg
            legUpdate.bindLong(0, left_id);
            legUpdate.bindLong(1, right_id);
            legUpdate.bindLong(2, top_id);
            legUpdate.bindLong(3, down_id);
            legUpdate.bindLong(4, legId);
            legUpdate.bindLong(5, galleryId);
            legUpdate.execute();
        }

        aSqLiteDatabase.execSQL("alter table legs drop column left");
        aSqLiteDatabase.execSQL("alter table legs drop column right");
        aSqLiteDatabase.execSQL("alter table legs drop column up");
        aSqLiteDatabase.execSQL("alter table legs drop column down");

        Log.i(Constants.LOG_TAG_DB, "Upgrade success");
    }

    private void applyDBV3(SQLiteDatabase aSqLiteDatabase) {
        Log.i(Constants.LOG_TAG_DB, "Upgrading DB to V3");
        aSqLiteDatabase.execSQL("alter table vectors add column gallery_id decimal default null");
        aSqLiteDatabase.execSQL("update vectors set gallery_id = " +
                "(select min(gallery_id) from legs where from_point_id = id)");

        aSqLiteDatabase.execSQL("alter table photos add column gallery_id decimal default null");
        aSqLiteDatabase.execSQL("update photos set gallery_id = " +
                "(select min(gallery_id) from legs where from_point_id = id)");

        aSqLiteDatabase.execSQL("alter table sketches add column gallery_id decimal default null");
        aSqLiteDatabase.execSQL("update sketches set gallery_id = " +
                "(select min(gallery_id) from legs where from_point_id = id)");

        aSqLiteDatabase.execSQL("alter table notes add column gallery_id decimal default null");
        aSqLiteDatabase.execSQL("update notes set gallery_id = " +
                "(select min(gallery_id) from legs where from_point_id = id)");

        aSqLiteDatabase.setTransactionSuccessful();

        Log.i(Constants.LOG_TAG_DB, "Upgrade success");
    }

    private void applyDBV2(SQLiteDatabase aSqLiteDatabase) {
        Log.i(Constants.LOG_TAG_DB, "Upgrading DB to V2");
        aSqLiteDatabase.execSQL("alter table legs add column middle_point_distance decimal default null");
        Log.i(Constants.LOG_TAG_DB, "Upgrade success");
    }

    public Dao<Leg, Integer> getLegDao() {
        return mLegDao;
    }

    public Dao<Location, Integer> getLocationDao() {
        return mLocationDao;
    }

    public Dao<Note, Integer> getNoteDao() {
        return mNoteDao;
    }

    public Dao<Photo, Integer> getPhotoDao() {
        return mPhotoDao;
    }

    public Dao<Point, Integer> getPointDao() {
        return mPointDao;
    }

    public Dao<Project, Integer> getProjectDao() {
        return mProjectDao;
    }

    public Dao<Sketch, Integer> getSketchDao() {
        return mSketchDao;
    }

    public Dao<Gallery, Integer> getGalleryDao() {
        return mGalleryDao;
    }

    public Dao<Option, Integer> getOptionsDao() {
        return mOptionsDao;
    }

    public Dao<Vector, Integer> getVectorsDao() {
        return mVectorsDao;
    }
}
