package com.gotye.meetplayer.ui.widget;

import com.gotye.meetplayer.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.MediaController;

public class MySimpleMediaController extends MediaController {

	public MySimpleMediaController(Context context) {
		super(new ContextThemeWrapper(context, R.style.MyPlayerTheme));
	}
	
	public MySimpleMediaController(Context context, AttributeSet attr) {
		super(new ContextThemeWrapper(context, R.style.MyPlayerTheme), attr);
	}
}