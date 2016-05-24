package com.gotye.meetplayer.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 自定义textview实现在GridView实现跑马灯的效果，复写view里面的isFocused()方法，默认情况下是不会有效果的，
 * 而且gridview也不可点击
 * 
 * @author dennis
 *
 */
public class MyMarqueeTextView extends TextView {

	private boolean bMarquee = true;
	public MyMarqueeTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public MyMarqueeTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public void setMarquee(boolean ON) {
        bMarquee = ON;
	}

	@Override
	public boolean isFocused()
	{
		return true;
	}
}



