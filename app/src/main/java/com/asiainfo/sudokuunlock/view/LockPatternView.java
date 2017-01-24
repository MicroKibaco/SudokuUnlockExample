package com.asiainfo.sudokuunlock.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.asiainfo.sudokuunlock.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 图案锁
 */

class LockPatternView extends View {

    //矩阵
    private Matrix mMatrix = new Matrix();

    //画笔
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    //绘制这九个点
    private Point[][] mPoints = new Point[3][3];

    private boolean isInit, isSlected, iSuplift, moveNoPoint;

    private Point.OnPatternChangeListener mListener;

    //横屏和竖屏
    private float width, high, offsetX, offsetY, bitmapRadius, moveX, moveY;

    private Bitmap mPointNormal, mpointPressed, pointError, mLinePressed, mLineError;

    //选中点的数量
    private static final int POINT_SIZE = 5;

    //按下点的集合
    private List<Point> pointList = new ArrayList<Point>();


    public LockPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockPatternView(Context context) {
        super(context);
    }

    public LockPatternView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 自定义的点
     */

    public static class Point {

        //正常
        public static int STATE_NORMAL = 0;

        //选中
        public static int STATE_PRESSED = 1;

        //错误
        public static int STATE_ERROR = 2;

        private static OnPatternChangeListener mListener;

        public float x, y;

        public int index = 0, state = 0;

        public Point() {
        }

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * 两点之间的距离
         */
        public static double distance(Point a, Point b) {

            return Math.sqrt(Math.abs(a.x - b.x) * Math.abs(a.x - b.x) + Math.abs(a.y - b.y) * Math.abs(a.y - b.y));

        }

        /**
         * 是否重合
         */

        public static boolean with(float pointX, float pointY, float r, float moveX, float moveY) {

            float mRedius = (pointX - moveX) * (pointX - moveX);

            float mDistance = (pointY - moveY) * (pointY - moveY);

            return Math.sqrt(mRedius - mDistance) < r;
        }

        /**
         * 图案的监听器
         */
        public static interface OnPatternChangeListener{

            /**
             * 图案改变
             * @param password
             */
            void  onPatternChange(String password);
        }

        /**
         * 设置图案的监听器
         * @param listener
         */
        public static void setOnPatternChangeListener(OnPatternChangeListener listener){

            if (listener!=null){

                mListener = listener;

            }
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (!isInit) {

            initPoints();
        }

        //画点
        point2Canvaus(canvas);

        //画线
        if (pointList.size() > 0) {

            Point a = pointList.get(0);

            //绘制九宫格坐标点
            for (int i = 0; i < pointList.size(); i++) {

                Point b = pointList.get(i);
                line2Canvas(canvas, a, b);
                a = b;
            }

            //绘制鼠标坐标点
            if (moveNoPoint) {

                line2Canvas(canvas, a, new Point(moveX, moveY));
            }
        }
    }

    /**
     * 设置绘制不成立
     */
    public void resetPoint() {

        for (int i = 0; i < pointList.size(); i++) {

            Point point = pointList.get(i);
            point.state = Point.STATE_NORMAL;
        }

        pointList.clear();
    }

    /**
     * 设置绘制错误
     */
    public void errorPoint() {

        for (Point point : pointList) {

            point.state = Point.STATE_ERROR;
        }
    }

    /**
     * 初始化点
     */
    public void initPoints() {

        //获取宽高
        width = getWidth();
        high = getHeight();


        if (width > high) {

            offsetX = (width - high) / 2;

            //竖屏
        } else {

            offsetY = (high - width) / 2;
            high = width;

        }

        //3.图片资源

        mPointNormal = BitmapFactory.decodeResource(getResources(), R.drawable.oval_normal);
        mpointPressed = BitmapFactory.decodeResource(getResources(), R.drawable.oval_pressed);
        pointError = BitmapFactory.decodeResource(getResources(), R.drawable.oval_erro);
        mLinePressed = BitmapFactory.decodeResource(getResources(), getResources().getColor(R.color.gesture_green));
        mLineError = BitmapFactory.decodeResource(getResources(), getResources().getColor(R.color.gesture_red));


        //获取九个点的坐标
        mPoints[0][0] = new Point(offsetX + width / 4, offsetY + width / 4);
        mPoints[0][1] = new Point(offsetX + width / 2, offsetY + width / 4);
        mPoints[0][2] = new Point(offsetX + width * 3 / 4, offsetY + width / 4);

        mPoints[1][0] = new Point(offsetX + width / 4, offsetY + width / 2);
        mPoints[1][1] = new Point(offsetX + width / 2, offsetY + width / 2);
        mPoints[1][2] = new Point(offsetX + width * 3 / 4, offsetY + width / 2);

        mPoints[2][0] = new Point(offsetX + width / 4, offsetY + width * 3 / 4);
        mPoints[2][1] = new Point(offsetX + width / 2, offsetY + width * 3 / 4);
        mPoints[2][2] = new Point(offsetX + width * 3 / 4, offsetY + width * 3 / 4);

        //5.图片资源的半径
        bitmapRadius = mPointNormal.getHeight() / 2;

        //6.设置密码
        int index = 1;
        for (Point[]  points : mPoints) {
            for (Point point : points) {

                point.index = index;
                index++;
            }

        }

        //7.初始化完成
        isInit = true;


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        moveY = event.getY();

        moveX = event.getX();

        Point mCheckPoint = null;

        moveNoPoint = false;

        iSuplift = false;


        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                resetPoint();

                mCheckPoint = checkSelectPoint();

                if (mCheckPoint != null) {

                    isSlected = true;
                }

                break;


            case MotionEvent.ACTION_MOVE:

                if (isSlected) {

                    mCheckPoint = checkSelectPoint();

                    if (mCheckPoint == null) {

                        moveNoPoint = true;
                    }
                }


                break;


            case MotionEvent.ACTION_UP:

                iSuplift = true;
                isSlected = false;


                break;


            default:
                break;


        }
        //选中重复检查

        if (!iSuplift && isSlected && mCheckPoint != null) {

            //交叉点
            if (crossPoint(mCheckPoint)) {

                moveNoPoint = true;
                //新点
            } else {
                mCheckPoint.state = Point.STATE_PRESSED;

                pointList.add(mCheckPoint);
            }
        }

        //绘制结束
        if (iSuplift) {


            if (pointList.size() == 1) {

                resetPoint();

                //绘制错误
            } else if (pointList.size() < 5 && pointList.size() > 2) {

                errorPoint();
                //绘制成功
            }else {
                if (mListener!=null){

                    String passwordStr = "";

                    for (int i = 0; i < pointList.size(); i++) {

                        passwordStr =passwordStr+ pointList.get(i).index;
                    }

                    mListener.onPatternChange(passwordStr);

                }
            }


        }

        //刷新view
        postInvalidate();
        return true;
    }

    /**
     * 交叉点
     */
    private boolean crossPoint(Point point) {

        if (pointList.contains(point)) {

            return true;
        }
        return false;
    }

    /**
     * 将点绘制到画布上
     */
    private void point2Canvaus(Canvas canvas) {

        for (int i = 0; i < mPoints.length; i++) {
            for (int j = 0; j < mPoints[i].length; j++) {

                Point mSudoPoint = mPoints[i][j];
                if (mSudoPoint.state == mSudoPoint.STATE_PRESSED) {
                    canvas.drawBitmap(mpointPressed, mSudoPoint.x - bitmapRadius, mSudoPoint.y - bitmapRadius, mPaint);

                } else if (mSudoPoint.state == Point.STATE_ERROR) {

                    canvas.drawBitmap(pointError, mSudoPoint.x - bitmapRadius, mSudoPoint.y - bitmapRadius, mPaint);

                } else {

                    canvas.drawBitmap(mPointNormal, mSudoPoint.x - bitmapRadius, mSudoPoint.y - bitmapRadius, mPaint);
                }
            }
        }
    }

    /**
     * 画线
     */

    private void line2Canvas(Canvas canvas, Point a, Point b) {

     //   float degres = getDegres(a, b);
        float mlineLength = (float) Point.distance(a, b);
     //   canvas.rotate(degres,a.x,a.y);

        if (a.state == Point.STATE_PRESSED) {

            mMatrix.setScale(mlineLength / mLinePressed.getWidth(), 1);
            mMatrix.postTranslate(a.x - mLinePressed.getWidth() / 2, a.y - mLinePressed.getHeight() / 2);
            canvas.drawBitmap(mLinePressed, mMatrix, mPaint);

        } else {

            mMatrix.setScale(mlineLength / mLineError.getWidth(), 1);
            mMatrix.postTranslate(a.x - mLineError.getWidth() / 2, a.y - mLineError.getHeight() / 2);
            canvas.drawBitmap(mLineError, mMatrix, mPaint);

        }

     //   canvas.rotate(-degres,a.x,a.y);

    }

    /**
     * 检查是否为选中
     */
    private Point checkSelectPoint() {

        for (int i = 0; i < mPoints.length; i++) {

            for (int j = 0; j < mPoints[i].length; j++) {

                Point mGetPoint = mPoints[i][j];

                if (Point.with(mGetPoint.x, mGetPoint.y, bitmapRadius, moveX, moveY)) {

                    return mGetPoint;
                }

            }
        }
        return null;
    }

    /**
     * 获取角度
     */
/*
    public float getDegres(Point a, Point b) {

        float ax = a.x;
        float ay = a.y;
        float bx = b.x;
        float by = b.y;
        float degrees = 0;

        if (bx == ax) {//y轴相等,90度成270度


            if (by > ay) {

                degrees = 90;

            } else if (by < ay) {

                degrees = 270;
            }


        } else if (by == ay) {


            if (bx > ax) {

                degrees = 0;
            } else if (bx < ax) {

                degrees = 180;
            }

        }else {
            if (bx >ax){
                if (by > ay){

                    degrees = 0;
                    degrees = degrees+switchDegress
                }
            }

        }

    }*/


}
