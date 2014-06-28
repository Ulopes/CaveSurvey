package com.astoev.cave.survey.model;

import com.astoev.cave.survey.service.Workspace;
import com.astoev.cave.survey.util.DaoUtil;
import com.astoev.cave.survey.util.PointUtil;
import com.astoev.cave.survey.util.StringUtils;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: astoev
 * Date: 1/24/12
 * Time: 11:15 PM
 * To change this template use File | Settings | File Templates.
 */
@DatabaseTable(tableName = "legs")
public class Leg implements Serializable {

	private static final long serialVersionUID = 201312130309L;

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_PROJECT_ID = "project_id";
    public static final String COLUMN_FROM_POINT = "from_point_id";
    public static final String COLUMN_TO_POINT = "to_point_id";
    public static final String COLUMN_GALLERY_ID = "gallery_id";
    public static final String COLUMN_LEFT_VECTOR_ID = "left_vector_id";
    public static final String COLUMN_RIGHT_VECTOR_ID = "right_vector_id";
    public static final String COLUMN_TOP_VECTOR_ID = "top_vector_id";
    public static final String COLUMN_DOWN_VECTOR_ID = "down_vector_id";
    public static final String COLUMN_MIDDLE_POINT_AT_DISTANCE = "middle_point_distance";

    @DatabaseField(generatedId = true, columnName = COLUMN_ID)
    private Integer mId;
    @DatabaseField(canBeNull = false, foreign = true, columnName = COLUMN_FROM_POINT)
    private Point mFromPoint;
    @DatabaseField(foreign = true, canBeNull = false, columnName = COLUMN_TO_POINT)
    private Point mToPoint;
    @DatabaseField(foreign = true, canBeNull = false, foreignAutoRefresh = true, columnName = COLUMN_PROJECT_ID)
    private Project mProject;
    @DatabaseField(columnName = "distance")
    private Float mDistance;
    @DatabaseField(columnName = "azimuth")
    private Float mAzimuth;
    @DatabaseField(columnName = "slope")
    private Float mSlope;
    @DatabaseField(canBeNull = true, foreign = true, columnName = COLUMN_LEFT_VECTOR_ID)
    private Vector mLeftVector;
    @DatabaseField(canBeNull = true, foreign = true, columnName = COLUMN_RIGHT_VECTOR_ID)
    private Vector mRightVector;
    @DatabaseField(canBeNull = true, foreign = true, columnName = COLUMN_TOP_VECTOR_ID)
    private Vector mTopVector;
    @DatabaseField(canBeNull = true, foreign = true, columnName = COLUMN_DOWN_VECTOR_ID)
    private Vector mDownVector;
    @DatabaseField(canBeNull = false, columnName = COLUMN_GALLERY_ID)
    private Integer mGalleryId;
    @DatabaseField(columnName = COLUMN_MIDDLE_POINT_AT_DISTANCE)
    private Float mMiddlePointDistance;


    public Leg() {

    }

    public Leg(Point aFromPoint, Point aToPoint, Project aProject, Integer aGalleryId) {
        mFromPoint = aFromPoint;
        mToPoint = aToPoint;
        mProject = aProject;
        mGalleryId = aGalleryId;
    }

    public Leg createNextLeg() throws SQLException {
        Point nextPoint = PointUtil.generateNextPoint(mGalleryId);
        return new Leg(mToPoint, nextPoint, mProject, mGalleryId);
    }

    public boolean isNew(){
    	return (getId() ==  null);
    }

    /**
     * Helper method to build string representation of the leg. Shows from and to points.
     * 
     * @return String representation
     * @throws SQLException
     */
    public String buildLegDescription() throws SQLException {
    	return buildLegDescription(false);
    }
    
    /**
     * Helper method to build string representation of the leg. Shows from and to points.
     * 
     * @param shortArg - flag if the representation should be short, without spaces.
     * @return String representation
     * @throws SQLException
     */
    public String buildLegDescription(boolean shortArg) throws SQLException{
        Point startPoint, endPoint;
        if (isNew()){
            startPoint = getFromPoint();
            endPoint = getToPoint();
        } else {
            startPoint = DaoUtil.getPoint(getFromPoint().getId());
            endPoint = DaoUtil.getPoint(getToPoint().getId());
        }
        

        StringBuilder builder = new StringBuilder(StringUtils.SPACE);

//        Leg prevLeg = DaoUtil.getLegByToPoint(mFromPoint);
//        if (mGalleryId != null) {
//            if (prevLeg != null &&  !prevLeg.getGalleryId().equals(mGalleryId)) {
//                builder.append(DaoUtil.getGallery(prevLeg.getGalleryId()).getName());
//            } else {
//                builder.append(DaoUtil.getGallery(mGalleryId).getName());
//            }
//        } else {
//            // fresh leg for new gallery
//            builder.append(DaoUtil.getGallery(prevLeg.mGalleryId).getName());
//        }
        builder.append(PointUtil.getGalleryNameForFromPoint(mFromPoint, mGalleryId));
        DaoUtil.refreshPoint(startPoint);
        builder.append(startPoint.getName());
        if (!shortArg){
        	builder.append(StringUtils.SPACE);
        }
        builder.append("->");
        if (!shortArg){
        	builder.append(StringUtils.SPACE);
        }
        
//        if (mGalleryId != null) {
//            builder.append(DaoUtil.getGallery(mGalleryId).getName());
//        } else {
//            // fresh leg for new gallery
//            builder.append(Gallery.generateNextGalleryName(Workspace.getCurrentInstance().getActiveProjectId()));
//        }
        builder.append(PointUtil.getGalleryNameForToPoint(mGalleryId));
        builder.append(endPoint.getName());

        if (isMiddle()) {
            builder.append("@").append(StringUtils.floatToLabel(mMiddlePointDistance));
        }
        return builder.toString();
    }

    public static boolean canDelete(Leg aLeg) throws SQLException {
        // may enable deletion if already saved
        if (!aLeg.isNew()) {

            // allow middles to be deleted
            if (aLeg.isMiddle()) {
                return true;
            }

            Leg lastLeg = Workspace.getCurrentInstance().getLastLeg(aLeg.getGalleryId());
            // only last leg for now and no other galleries start from here
            if (lastLeg.getId().equals(aLeg.getId()) && !DaoUtil.hasLegsByFromPoint(aLeg.getToPoint())) {
                return true;
            }
        }
        return false;
    }

    public Integer getId() {
        return mId;
    }

    public void setId(Integer aId) {
        mId = aId;
    }

    public Point getFromPoint() {
        return mFromPoint;
    }

    public void setFromPoint(Point aFromPoint) {
        mFromPoint = aFromPoint;
    }

    public Point getToPoint() {
        return mToPoint;
    }

    public void setToPoint(Point aToPoint) {
        mToPoint = aToPoint;
    }

    public Project getProject() {
        return mProject;
    }

    public void setProject(Project aProject) {
        mProject = aProject;
    }

    public Float getDistance() {
        return mDistance;
    }

    public void setDistance(Float aDistance) {
        mDistance = aDistance;
    }

    public Float getAzimuth() {
        return mAzimuth;
    }

    public void setAzimuth(Float aAzimuth) {
        mAzimuth = aAzimuth;
    }

    public Float getSlope() {
        return mSlope;
    }

    public void setSlope(Float aSlope) {
        mSlope = aSlope;
    }

    public Vector getLeftVector() {
        return mLeftVector;
    }

    public void setLeftVector(Vector leftVector) {
        mLeftVector = leftVector;
    }

    public Vector getRightVector() {
        return mRightVector;
    }

    public void setRightVector(Vector rightVector) {
        mRightVector = rightVector;
    }

    public Vector getTopVector() {
        return mTopVector;
    }

    public void setTopVector(Vector topVector) {
        mTopVector = topVector;
    }

    public Vector getDownVector() {
        return mDownVector;
    }

    public void setDownVector(Vector downVector) {
        mDownVector = downVector;
    }

    public Integer getGalleryId() {
        return mGalleryId;
    }

    public void setGalleryId(Integer aGalleryId) {
        mGalleryId = aGalleryId;
    }

    public Float getMiddlePointDistance() {
        return mMiddlePointDistance;
    }

    public void setMiddlePointDistance(Float aMiddlePointDistance) {
        mMiddlePointDistance = aMiddlePointDistance;
    }

    public Float getLeftDistance() {
        return mLeftVector == null ? null : mLeftVector.getDistance();
    }

    public Float getRightDistance() {
        return mRightVector == null ? null : mRightVector.getDistance();
    }

    public Float getTopDistance() {
        return mTopVector == null ? null : mTopVector.getDistance();
    }

    public Float getDownDistance() {
        return  mDownVector == null ? null : mDownVector.getDistance();
    }

    // side effect below, old vector deleted
    private Vector getVectorWithUpdatedDistance(Vector aVector, Float aDistance) throws SQLException {
        if (aDistance == null) {
            if (aVector != null) {
                DaoUtil.deleteVector(aVector);
            }
            return null;
        } else {
            if (aVector == null) {
                Vector v = new Vector();
                v.setGalleryId(mGalleryId);
                v.setPoint(mFromPoint);
                v.setDistance(aDistance);
                return v;
            }
            aVector.setDistance(aDistance);
            return aVector;
        }
    }

    public void populateLeftDistance(Float aDistance) throws SQLException {
        mLeftVector = getVectorWithUpdatedDistance(mLeftVector, aDistance);
    }

    public void populateRightDistance(Float aDistance) throws SQLException {
        mRightVector = getVectorWithUpdatedDistance(mRightVector, aDistance);
    }

    public void populateTopDistance(Float aDistance) throws SQLException {
        mTopVector = getVectorWithUpdatedDistance(mTopVector, aDistance);
    }

    public void populateDownDistance(Float aDistance) throws SQLException {
        mDownVector = getVectorWithUpdatedDistance(mDownVector, aDistance);
    }

    /**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Leg [id=");
		builder.append(mId);
		builder.append(", fromPoint=");
		builder.append(mFromPoint);
		builder.append(", mToPoint=");
		builder.append(mToPoint);
		builder.append(", project=");
		builder.append(mProject);
//		builder.append(", mDistance=");
//		builder.append(mDistance);
//		builder.append(", mDistanceFromStart=");
//		builder.append(mDistanceFromStart);
//		builder.append(", mAzimuth=");
//		builder.append(mAzimuth);
//		builder.append(", mSlope=");
//		builder.append(mSlope);
//		builder.append(", mLeft=");
//		builder.append(mLeft);
//		builder.append(", mRight=");
//		builder.append(mRight);
//		builder.append(", mTop=");
//		builder.append(mTop);
//		builder.append(", mDown=");
//		builder.append(mDown);
		builder.append(", galleryId=");
		builder.append(mGalleryId);
		builder.append("]");
		return builder.toString();
	}

    public boolean isMiddle() {
        return mMiddlePointDistance != null;
    }

}
