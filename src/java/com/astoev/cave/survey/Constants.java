package com.astoev.cave.survey;

import com.astoev.cave.survey.model.Option;

import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: astoev
 * Date: 1/29/12
 * Time: 5:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class Constants {

    public static final String LOG_TAG_DB = "CaveSurveyDB";
    public static final String LOG_TAG_UI = "CaveSurveyUI";
    public static final String LOG_TAG_SERVICE = "CaveSurveySVC";
    public static final String LOG_TAG_BT = "CaveSurveyBT";

    public static final String DATE_FORMAT = "yyyyMMdd_HHmmss";

    public static final String LEG_SELECTED = "leg";
    public static final String LEG_NOTE = "legNote";

    public static final String GALLERY_NEW = "new_gallery";
    
    public static Locale DEFAULT_LOCALE = Locale.US;
    
    public static final int NOT_FOUND = -1;
    public static final String STRING_NOT_FOUND = "NOT_FOUND";

    public static enum Measures {distance, slope, angle, up, down, left, right}
    public static enum MeasureTypes {distance, slope, angle}
    public static enum MeasureUnits {meters, degrees, grads}

    public static final String MEASURE_TARGET_KEY = "target";
    public static final String MEASURE_VALUE_KEY = "value";
    public static final String MEASURE_TYPE_KEY = "type";
    public static final String MEASURE_UNIT_KEY = "unit";
    
    public static final float GRAD_TO_DEC = (Option.MAX_VALUE_AZIMUTH_DEGREES / (float)Option.MAX_VALUE_AZIMUTH_GRADS);
    public static final float DEC_TO_GRAD = (Option.MAX_VALUE_AZIMUTH_GRADS / (float)Option.MAX_VALUE_AZIMUTH_DEGREES);
}
