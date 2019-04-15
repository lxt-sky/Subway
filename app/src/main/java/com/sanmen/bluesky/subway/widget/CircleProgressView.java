package com.sanmen.bluesky.subway.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.sanmen.bluesky.subway.R;

/**
 * @author lxt_bluesky
 * @date 2019/4/1
 * @description
 */
public class CircleProgressView extends View {

    /**
     * 进度条颜色
     */
    private int progressColor;
    /**
     * 圆弧颜色
     */
    private int arcColor;
    /**
     * 进度条宽度
     */
    private float progressWidth;

    private float textSize;

    private int textColor;
    /**
     * 进度条画笔
     */
    private Paint mProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * 圆弧画笔
     */
    private Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * 进度条画笔
     */
    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * 子标题画笔
     */
    private Paint mSubTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * 阴影画笔
     */
    private Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);


    private int progress=0;
    /**
     * 总进度
     */
    private int totalProgress = 100;

    private String centerText;

    private String subText;

    private boolean isShowCenterText = true;

    private boolean isShowSubText = false;



    public CircleProgressView(Context context) {
        this(context,null);
    }

    public CircleProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CircleProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initAttr(context,attrs);
        initVariable();

    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView);
        //属性值初始化
        progressWidth = typedArray.getDimensionPixelSize(R.styleable.CircleProgressView_progress_width,dip2px(20));
        progressColor = typedArray.getColor(R.styleable.CircleProgressView_progress_color,Color.parseColor("#f50000"));
        arcColor = typedArray.getColor(R.styleable.CircleProgressView_arc_color,Color.parseColor("#ffffff"));
        textColor = typedArray.getColor(R.styleable.CircleProgressView_text_color,Color.parseColor("#ffffff"));
        textSize =typedArray.getDimensionPixelSize(R.styleable.CircleProgressView_text_size,dip2px(30));
        isShowCenterText = typedArray.getBoolean(R.styleable.CircleProgressView_show_center_text,true);
        isShowSubText  = typedArray.getBoolean(R.styleable.CircleProgressView_show_sub_text,false);
        subText = typedArray.getString(R.styleable.CircleProgressView_sub_text);
        typedArray.recycle();
    }

    public void initVariable(){
        //设置mBorderPaint画笔
        //设置为绘制描边
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(progressWidth);
        mArcPaint.setColor(arcColor);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mArcPaint.setAntiAlias(true);

        //设置mArcPaint画笔
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setColor(progressColor);
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressPaint.setStrokeWidth(progressWidth);

        //文本画笔
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mTextPaint.setAntiAlias(true);

        mSubTextPaint.setStyle(Paint.Style.FILL);
        mSubTextPaint.setColor(Color.parseColor("#F0ECEC"));
        mSubTextPaint.setTextSize(30);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int viewSize = Math.min(viewWidth,viewHeight);
        //绘制半径
        float measureRadius = (viewSize-progressWidth)/2.0f;
        //中心坐标
        float centerX = viewWidth/2.0f;
        float centerY = viewHeight/2.0f;
        //矩阵
        RectF oval = new RectF();
        oval.left = centerX-measureRadius;
        oval.top = centerY-measureRadius;
        oval.right = centerX+measureRadius;
        oval.bottom = centerY+measureRadius;

        if (progress<100){
            centerText = String.valueOf(progress);
        }else {
            centerText="连接成功 >>";
            mTextPaint.setTextSize(60);
        }
        //计算文字绘制X坐标
        int textX= toCalculateTextX(mTextPaint,centerText);
        //计算文本绘制高度
        Paint.FontMetricsInt fm = mTextPaint.getFontMetricsInt();
        //计算文本绘制Y坐标
        int textY =(getHeight()-(fm.descent-fm.ascent))/2-fm.ascent;

        canvas.drawArc(oval,120f,300f,false,mArcPaint);

        if(progress>0){
            //圆弧
            canvas.drawArc(oval,120f,((float) progress/totalProgress)*300f,false,mProgressPaint);
        }
        if (isShowCenterText){
            //中间文字
            canvas.drawText(centerText,textX,textY,mTextPaint);
        }

        if (isShowSubText){
            //计算文字绘制X坐标
            int subTextX= toCalculateTextX(mSubTextPaint,subText);
            //计算文本绘制高度
            Paint.FontMetricsInt fontM = mSubTextPaint.getFontMetricsInt();
            //计算文本绘制Y坐标
            int subTextY =(getHeight()-(fontM.descent-fontM.ascent))/2-fontM.ascent;
            canvas.drawText(subText,subTextX,subTextY+100,mSubTextPaint);
        }


    }

    private void drawLine(Canvas canvas,float centerX,float centerY) {
        int count = 100;
        int avgAngle = 300/(count-1);
        int lineLength = 8;
        /**起始点**/
        PointF point1 = new PointF();
        /**终止点**/
        PointF point2 = new PointF();
        for (int i = 0; i < count; i++) {
            int angle = avgAngle * i;
            /**起始点坐标**/
//            point1.x = centerX + (float) Math.cos(angle * (Math.PI / 180)) * radius;
//            point1.y = centerX + (float) Math.sin(angle * (Math.PI / 180)) * radius;
        }

    }

    /**
     * 设置进度
     * @param progress 进度值
     */
    public void setProgress(int progress) {

        this.progress = progress;
        //重新绘制
        postInvalidate();
    }
    public void isShowCenterText(boolean state){
        isShowCenterText = state;
    }

    public void isShowSubText(boolean state){
        isShowSubText = state;
        invalidate();
    }

    /**
     * 计算文字绘制X坐标
     * @param text
     * @return
     */
    private int toCalculateTextX(Paint paint,String text) {

        int textWidth = (int) paint.measureText(text);
        if (text!=null){
            return (getMeasuredWidth()-textWidth)/2;
        }
        return 0;
    }

    /**
     * dp转px
     * @param dipVal
     * @return
     */
    private int dip2px(int dipVal){
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dipVal*scale+0.5f);
    }
}
