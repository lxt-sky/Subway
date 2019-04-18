package com.sanmen.bluesky.subway.widget;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
    private float startAngle=0f;
    private float spinSpeed = 0f;
    private boolean isSpinning=false;
    private String oldText="";
    private String currentText="";
    private Rect textRect = new Rect();
    private float mMaxMoveHeight=80;
    private float mOutterMoveHeight=0;
    private float mCurrentAlphaValue=0;
    private float mCurrentMoveHeight=0;
    private int loadState=0;

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
        spinSpeed = typedArray.getFloat(R.styleable.LoadingCircleView_spin_speed,10f);
        typedArray.recycle();
    }

    private void initVariable() {

        //画圆环
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(borderColor);
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStrokeWidth(borderWidth);

        //画阴影
        mShadowPaint.setStyle(Paint.Style.STROKE);
        mShadowPaint.setColor(Color.parseColor("#a1dfdbdb"));
        mShadowPaint.setAntiAlias(true);
        mShadowPaint.setStrokeWidth(dip2px(16));
    //    mShadowPaint.setShadowLayer(20f,0,0,Color.argb(100,100,100,100));

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

    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int viewSize = Math.min(viewWidth,viewHeight);
        //绘制半径
        float measureRadius = (viewSize-4*borderWidth)/2.0f;
        //中心坐标
        float centerX = viewWidth/2.0f;
        float centerY = viewHeight/2.0f;

        //矩阵
        RectF oval = new RectF();
        oval.left = centerX-measureRadius;
        oval.top = centerY-measureRadius;
        oval.right = centerX+measureRadius;
        oval.bottom = centerY+measureRadius;

        //计算文字绘制X坐标
        int textX= toCalculateTextX(mTextPaint,centerText);
        //计算文本绘制高度
        Paint.FontMetricsInt fm = mTextPaint.getFontMetricsInt();
        //计算文本绘制Y坐标
        int textY =(getHeight()-(fm.descent-fm.ascent))/2-fm.ascent;

        canvas.drawCircle(centerX, centerY, measureRadius, mBorderPaint);

        if (isShowCenterText){
            //中间文字
//            canvas.drawText(centerText,textX,textY,mTextPaint);
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

        //渐变
        SweepGradient gradient = new SweepGradient(centerX,centerY,new int[] {Color.CYAN,Color.DKGRAY,Color.GRAY,Color.LTGRAY,Color.MAGENTA,
                Color.GREEN,Color.TRANSPARENT, Color.BLUE }, null);
        mShadowPaint.setShader(gradient);
        canvas.drawArc(oval,startAngle-90,90f,false,mShadowPaint);

        if (isSpinning){
            updateProgress();
        }

        for (int i=0;i<2;i++){

            if (loadState==0){

                mTextPaint.setAlpha(255);
                canvas.drawText(currentText, toCalculateTextX(mTextPaint,currentText), textY, mTextPaint);
            }else if (loadState==1){
                //位数数字不同，表示需要需要处理移动操作。
                mTextPaint.setAlpha((int) (255 * (1 - mCurrentAlphaValue)));
                canvas.drawText(oldText, toCalculateTextX(mTextPaint,oldText), mOutterMoveHeight + textY, mTextPaint);
                mTextPaint.setAlpha((int) (255 * mCurrentAlphaValue));
                canvas.drawText(currentText,  toCalculateTextX(mTextPaint,currentText), mCurrentMoveHeight + textY, mTextPaint);

            }

        }

    }

    private synchronized void jumpCenterText() {

        ValueAnimator animator = ValueAnimator.ofFloat(mMaxMoveHeight, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentMoveHeight = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.setDuration(1000);
        animator.start();

        ValueAnimator animator1 = ValueAnimator.ofFloat(0, 1);
        animator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentAlphaValue = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator1.setDuration(1000);
        animator1.start();

        ValueAnimator animator2 = ValueAnimator.ofFloat(0, -mMaxMoveHeight);
        animator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mOutterMoveHeight = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator2.setDuration(1000);
        animator2.start();
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

    /**
     * 中心文本的翻页效果
     */
    public void nextText(String text,int stateNum){

        oldText = currentText;
        currentText = text;
        loadState = stateNum;

        jumpCenterText();
    }

    /**
     * 更新其实绘制角度
     */
    private void updateProgress() {

        startAngle+=spinSpeed;
        if (startAngle>360){
            startAngle = 0;
        }
        postInvalidateDelayed(10);
    }

    /**
     * 设置中心文本
     * @param text
     */
    public void setCenterText(String text) {
        isShowCenterText = true;
        this.centerText = text;
        //重新绘制
        invalidate();
    }

    /**
     * 显示中心文本
     * @param state
     */
    public void isShowCenterText(boolean state){
        isShowCenterText = state;
    }

    /**
     * 显示子标题
     * @param state
     */
    public void isShowSubText(boolean state){
        isShowSubText = state;
        invalidate();
    }

    /**
     * 开始旋转
     */
    public void startSpinning(){
        isSpinning = true;
        postInvalidate();
    }

    /**
     * 结束旋转
     */
    public void stopSpinning(){
        isSpinning = false;
        startAngle = 0;
        postInvalidate();
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
