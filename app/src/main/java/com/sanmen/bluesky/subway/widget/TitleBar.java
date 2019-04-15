package com.sanmen.bluesky.subway.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.sanmen.bluesky.subway.R;

public class TitleBar extends LinearLayout {

    private RelativeLayout relRootView;
    private ImageView imvNavback;
    private TextView txvNavRight;
    private TextView txvNavTitle;
    /*增加右边按钮*/
    private ImageView imvNavRight;

    private int titleBackgroundColor;
    private String titleText;
    private int titleTextColor;
    private int titleTextSize;
    private int navBackImg;
    private boolean showNavBack;
    private String rightText;
    private int rightTextColor;
    private int rightTextSize;

    private int navRightImg;
    private boolean showNavRight;

    /**
     * 标题的点击事件
     */
    private TitleOnClickListener titleOnClickListener;

    public TitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        /**加载布局文件*/
        LayoutInflater.from(context).inflate(R.layout.view_title, this, true);
        relRootView = findViewById(R.id.relRootView);
        imvNavback = findViewById(R.id.imvNavback);
        txvNavTitle = findViewById(R.id.txvNavTitle);
        txvNavRight = findViewById(R.id.txvNavRight);

        imvNavRight = findViewById(R.id.imvNavRight);

        /**获取属性值*/
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TitleBar);
        /**标题相关*/
        titleBackgroundColor = typedArray.getColor(R.styleable.TitleBar_title_background, 0);
        titleText = typedArray.getString(R.styleable.TitleBar_titleText);
        titleTextColor = typedArray.getColor(R.styleable.TitleBar_titleTextColor, ContextCompat.getColor(getContext(), R.color.blackText));
        titleTextSize = typedArray.getColor(R.styleable.TitleBar_titleTextSize, 16);
        /**返回按钮相关*/
        navBackImg = typedArray.getResourceId(R.styleable.TitleBar_left_button_image, R.drawable.ic_back);
        showNavBack = typedArray.getBoolean(R.styleable.TitleBar_showNavBack, true);
        //右边
        rightText = typedArray.getString(R.styleable.TitleBar_rightText);
        rightTextColor = typedArray.getColor(R.styleable.TitleBar_rightTextColor, ContextCompat.getColor(getContext(), R.color.blackText));
        rightTextSize = typedArray.getColor(R.styleable.TitleBar_rightTextSize, 14);
        //右边按钮
        navRightImg = typedArray.getResourceId(R.styleable.TitleBar_right_button_image,R.drawable.ic_setting);
        showNavRight = typedArray.getBoolean(R.styleable.TitleBar_showNavRight,false);

        /**设置值*/
        if (titleBackgroundColor != 0) {
            setTitleBackgroundColor(titleBackgroundColor);
        }
        setTitleText(titleText);
        setTitleTextSize(titleTextSize);
        setTitleTextColor(titleTextColor);
        setRightText(rightText);
        setRightTextColor(titleTextColor);
        setRightTextSize(rightTextSize);
        setShowNavBack(showNavBack);
        setNavBackImg(navBackImg);
        setShowNavRight(showNavRight);
        setNavRightImg(navRightImg);

        imvNavback.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (titleOnClickListener != null) {
                    titleOnClickListener.onLeftClick();
                }
            }
        });

        txvNavRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (titleOnClickListener != null) {
                    titleOnClickListener.onRightClick(view);
                }
            }
        });

        imvNavRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (titleOnClickListener != null) {
                    titleOnClickListener.onRightClick(view);
                }
            }
        });
    }



    public TitleBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ImageView getNavBack() {
        return imvNavback;
    }

    public RelativeLayout getRelRootView() {
        return relRootView;
    }

    public TextView getTitle() {
        return txvNavTitle;
    }

    /**
     * 设置返回按钮的资源图片id
     * @param navBackImg 资源图片id
     */
    public void setNavBackImg(int navBackImg) {
        imvNavback.setImageResource(navBackImg);
    }

    /**
     * 设置是否显示返回按钮
     * @param showNavBack
     */
    public void setShowNavBack(boolean showNavBack) {
        imvNavback.setVisibility(showNavBack ? VISIBLE : INVISIBLE);
    }

    /**
     * 设置右边按钮的资源图片ID
     * @param navRightImg 资源图片ID
     */
    private void setNavRightImg(int navRightImg) {
        imvNavRight.setImageResource(navRightImg);
    }
    /**
     * 设置是否显示右边按钮
     * @param showNavRight
     */
    public void setShowNavRight(boolean showNavRight) {
        imvNavRight.setVisibility(showNavRight ? VISIBLE : INVISIBLE);
    }

    /**
     * 设置标题背景的颜色
     * @param titleBackgroundColor
     */
    public void setTitleBackgroundColor(int titleBackgroundColor) {
        relRootView.setBackgroundColor(titleBackgroundColor);
    }

    /**
     * 设置标题的文字
     * @param titleText
     */
    public void setTitleText(String titleText) {
        txvNavTitle.setText(titleText);
    }

    /**
     * 设置标题的文字颜色
     *
     * @param titleTextColor
     */
    public void setTitleTextColor(int titleTextColor) {
        txvNavTitle.setTextColor(titleTextColor);
    }

    /**
     * 设置标题的文字大小
     *
     * @param titleTextSize
     */
    public void setTitleTextSize(int titleTextSize) {
        txvNavTitle.setTextSize(titleTextSize);
    }

     public void setRightText(String rightText) {
        if (!TextUtils.isEmpty(rightText)){
            txvNavRight.setVisibility(View.VISIBLE);
        }
        txvNavRight.setText(rightText);
    }

    public void setRightTextColor(int rightTextColor) {
        txvNavRight.setTextColor(rightTextColor);
    }

    public void setRightTextSize(int rightTextSize) {
        txvNavRight.setTextSize(rightTextSize);
    }

    /**
     * 设置标题的点击监听
     *
     * @param titleOnClickListener
     */
    public void setOnTitleClickListener(TitleOnClickListener titleOnClickListener) {
        this.titleOnClickListener = titleOnClickListener;
    }

    /**
     * 监听标题点击接口
     */
    public interface TitleOnClickListener {
        /**
         * 返回按钮的点击事件
         */
        void onLeftClick();

        /**
         * 保存按钮的点击事件
         */
        void onRightClick(View view);


    }
}
