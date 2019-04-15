package com.sanmen.bluesky.subway.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import androidx.annotation.Nullable;
import com.sanmen.bluesky.subway.R;

/**
 * @author lxt_bluesky
 * @date 2019/4/11
 * @description
 */
public class LoadingCircleView extends View {

    /**
     * 进度条画笔
     */
    private Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 阴影画笔
     */
    private Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * 中心文本画笔
     */
    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 子标题画笔
     */
    private Paint mSubTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int borderWidth;
    private int borderColor;
    private int textColor;
    private int textSize;
    private boolean isShowCenterText;
    private boolean isShowSubText;
    private String subText = "下一页";
    private String centerText = "";
    private float angle =0;

    private ObjectAnimator objectAnimator;

    public LoadingCircleView(Context context) {
       this(context,null);
    }

    public LoadingCircleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LoadingCircleView(Context context,@Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context,attrs);
        initVariable();
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingCircleView);
        //属性值初始化
        borderWidth = typedArray.getDimensionPixelSize(R.styleable.LoadingCircleView_border_width,dip2px(20));
        borderColor = typedArray.getColor(R.styleable.LoadingCircleView_border_color,Color.parseColor("#f50000"));
        textColor = typedArray.getColor(R.styleable.LoadingCircleView_text_color,Color.parseColor("#ffffff"));
        textSize =typedArray.getDimensionPixelSize(R.styleable.LoadingCircleView_text_size,dip2px(30));
        isShowCenterText = typedArray.getBoolean(R.styleable.LoadingCircleView_show_center_text,true);
        isShowSubText  = typedArray.getBoolean(R.styleable.LoadingCircleView_show_sub_text,false);
        subText = typedArray.getString(R.styleable.LoadingCircleView_sub_text);
        typedArray.recycle();
    }

    private void initVariable() {

        //画圆环
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(borderColor);
        mBorderPaint.setAntiAlias(true);
//        mBorderPaint.setShadowLayer(20,0,0,Color.argb(128, 249, 94, 94));
        mBorderPaint.setStrokeWidth(borderWidth);
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);  // 关闭硬件加速,setShadowLayer 才会有效
//        this.setWillNotDraw(false);

        //画阴影
        mShadowPaint.setStyle(Paint.Style.STROKE);
        mShadowPaint.setColor(Color.parseColor("#a1dfdbdb"));
        mShadowPaint.setAntiAlias(true);
        mShadowPaint.setStrokeWidth(dip2px(10));

        //文本画笔
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mTextPaint.setAntiAlias(true);
        //子标题画笔
        mSubTextPaint.setStyle(Paint.Style.FILL);
        mSubTextPaint.setColor(Color.parseColor("#F0ECEC"));
        mSubTextPaint.setTextSize(30);

        //属性动画-旋转
        objectAnimator = ObjectAnimator.ofFloat(this,"rotation",0f,360f);
        objectAnimator.setDuration(3000);//设置动画时间
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator.setRepeatMode(ObjectAnimator.RESTART);


    }

    @Override
    protected void onDraw(Canvas canvas) {


        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int viewSize = Math.min(viewWidth,viewHeight);
        //绘制半径
        float measureRadius = (viewSize-borderWidth)/2.0f;
        //中心坐标
        float centerX = viewWidth/2.0f;
        float centerY = viewHeight/2.0f;

        //矩阵
        RectF oval = new RectF();
        oval.left = centerX-measureRadius+5f;
        oval.top = centerY-measureRadius+5f;
        oval.right = centerX+measureRadius+5f;
        oval.bottom = centerY+measureRadius+5f;

        //计算文字绘制X坐标
        int textX= toCalculateTextX(mTextPaint,centerText);
        //计算文本绘制高度
        Paint.FontMetricsInt fm = mTextPaint.getFontMetricsInt();
        //计算文本绘制Y坐标
        int textY =(getHeight()-(fm.descent-fm.ascent))/2-fm.ascent;

        canvas.drawCircle(centerX, centerY, measureRadius, mBorderPaint);

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

        canvas.save();

        //渐变
        SweepGradient gradient = new SweepGradient(centerX,centerY,new int[] {Color.CYAN,Color.DKGRAY,Color.GRAY,Color.LTGRAY,Color.MAGENTA,
                Color.GREEN,Color.TRANSPARENT, Color.BLUE }, null);
        mShadowPaint.setShader(gradient);
        canvas.drawArc(oval,0f,120f,false,mShadowPaint);
        //旋转
//        canvas.rotate(angle,centerX+5f,centerY+5f);

        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int mWidth , mHeight ;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);


        if (widthMode == MeasureSpec.EXACTLY) {
            mWidth = widthSize;
        } else {
            mWidth = 169;
            if (widthMode == MeasureSpec.AT_MOST) {
                mWidth = Math.min(mWidth, widthSize);
            }

        }
        if (heightMode == MeasureSpec.EXACTLY) {
            mHeight = heightSize;
        } else {
            mHeight = 169;
            if (heightMode == MeasureSpec.AT_MOST) {
                mHeight = Math.min(mWidth, heightSize);
            }

        }
        setMeasuredDimension(mWidth, mHeight);

    }

    public void playAnimation(){
        objectAnimator.start();
    }

    public void pauseAnimation(){
        objectAnimator.pause();

    }

    public void cancelAnimation(){
        objectAnimator.pause();
        objectAnimator.end();
    }

    public void setCenterText(String text) {
        isShowCenterText = true;
        this.centerText = text;
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

    class RotateAnimator extends RotateAnimation{

        public RotateAnimator(float fromDegrees, float toDegrees, float pivotX, float pivotY) {
            super(fromDegrees, toDegrees, pivotX, pivotY);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            angle = interpolatedTime * 360;
        }
    }
}
