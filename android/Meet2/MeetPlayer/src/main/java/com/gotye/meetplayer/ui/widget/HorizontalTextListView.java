package com.gotye.meetplayer.ui.widget;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gotye.meetplayer.R;

/**
 * 水平滚动封装类
 * 
 * @author sugarzhang
 */
public class HorizontalTextListView extends LinearLayout
{
    // private static final int COLOR_SELECTED = Color.WHITE;
    // private static final int COLOR_UNSELECTED = Color.GRAY;

    private LayoutInflater inflater;

    private LinearLayout linearLayout;

    private OnItemClickListener onItemClickListener;

    private View selectedTextView;

    private Context mContext;

    // 背景
    private ImageView bgImageView;
    
    private TextView mTitle;

    /**
     * <构造函数>
     * 
     * @param context Context
     * @param attrs AttributeSet
     */
    public HorizontalTextListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        init(context, attrs);
    }

    /**
     * 初始化
     */
    private void init(Context context, AttributeSet attrs)
    {
        this.mContext = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.horizontal_text_listview, this);
        linearLayout = (LinearLayout) findViewById(R.id.horizontal_list);
        selectedTextView = null;

        //bgImageView = (ImageView) findViewById(R.id.horizontal_bg);

    }

    /**
     * 改变背景
     * 
     * @param id 图片id
     */
    public void setBg(int id)
    {
        if (bgImageView != null)
        {
            bgImageView.setBackgroundResource(id);
        }
    }

    /**
     * 设置数据
     * 
     * @param list List<String>
     * @see [类、类#方法、类#成员]
     */
    public void setList(ArrayList<String> list)
    {
        setList(list, -1, -1);
    }

    /**
     * 设置数据
     * 
     * @param list List<String>
     * @param background int
     * @param textColor int
     * @param childResId int
     * @see [类、类#方法、类#成员]
     */
    public void setList(ArrayList<String> list, int background, int textColor, int childResId)
    {
        linearLayout.removeAllViews();

        selectedTextView = null;
        View view;
        TextView textView;
        // TextView countTextview;
        int i = 0;
        for (String str : list)
        {
            // int nameBeginIndex = 0;
            // int nameEndIndex = str.indexOf("(");
            // String name = str.substring(nameBeginIndex, nameEndIndex);
            // String count = str.substring(nameEndIndex + 1, str.length() -
            // 1);

            view = inflater.inflate(childResId, null);

            textView = (TextView) view.findViewById(R.id.horizontal_text_item_text);
            // countTextview = (TextView) view.findViewById(R.id.count);
            textView.setText(str);
            // countTextview.setText(count);
            /*
             * count = (TextView)view.findViewById(R.id.count);
             * count.setText(""+20);
             */
            if (textColor != -1)
            {
                textView.setTextColor(mContext.getResources().getColorStateList(textColor));
            }

            if (background != -1)
            {
                textView.setBackgroundResource(background);
            }

            linearLayout.addView(view, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.FILL_PARENT));

            final int position = i;
            // final View parent = view;
            // parent.post(new Runnable() {
            // @Override
            // public void run() {
            // Rect bounds = new Rect();
            /*
             * TextView mButton = (TextView) parent
             * .findViewById(R.id.horizontal_text_item_text);
             * mButton.setEnabled(true); mButton.setOnClickListener(new
             * View.OnClickListener() {
             * @Override public void onClick(View view) { boolean b = false;
             * if (onItemClickListener != null) { b =
             * onItemClickListener.onItemClick(position); } if (b) {
             * setSelection(position); } } });
             */

            view.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    boolean b = false;

                    if (onItemClickListener != null)
                    {
                        b = onItemClickListener.onItemClick(position);
                    }

                    if (b)
                    {
                        setSelection(position);
                    }
                }
            });

            // mButton.getHitRect(bounds);
            // bounds.left -= 50;
            // bounds.right += 50;
            // bounds.bottom += 50;
            // bounds.top -= 50;
            //
            // TouchDelegate touchDelegate = new TouchDelegate(bounds,
            // mButton);
            //
            // if (View.class.isInstance(mButton.getParent())) {
            // ((View) mButton.getParent())
            // .setTouchDelegate(touchDelegate);
            // }
            // }
            // });
            // UtilMethod.enlargeClickArea(view, 0, 0, 0, 100);
            i++;
        }
    }

    /**
     * 设置数据
     * 
     * @param list List<String>
     * @param itemBackground 每一项的背景
     * @param textColor 字体颜色
     * @see [类、类#方法、类#成员]
     */
    public void setList(ArrayList<String> list, int itemBackground, int textColor)
    {
        linearLayout.removeAllViews();

        selectedTextView = null;
        View view;
        TextView textView;
        int i = 0;
        for (String str : list)
        {
            view = inflater.inflate(R.layout.horizontal_text_item, null);

            textView = (TextView) view.findViewById(R.id.horizontal_text_item_text);

            textView.setText(str);
            textView.setFocusable(true);
            textView.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        v.setBackgroundColor(Color.LTGRAY);
                    }
                    else {
                        v.setBackgroundColor(Color.WHITE);
                    }
                }
            });
            // textView.setTextColor(COLOR_UNSELECTED);
            if (textColor != -1)
            {
                textView.setTextColor(mContext.getResources().getColorStateList(textColor));
            }

            if (itemBackground != -1)
            {
                textView.setBackgroundResource(itemBackground);
            }

            linearLayout.addView(view, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.FILL_PARENT));

            final int position = i;
            textView.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    boolean b = false;

                    if (onItemClickListener != null)
                    {
                        b = onItemClickListener.onItemClick(position);
                    }

                    if (b)
                    {
                        setSelection(position);
                    }
                }
            });

            enlargeClickArea(textView);

            i++;
        }
    }

    /**
     * 加大点击范围
     *
     * @param view view
     * @see [类、类#方法、类#成员]
     */
    public static void enlargeClickArea(final View view)
    {
        final int ENLARGE = 15;

        if (view == null)
        {
            return;
        }
        ViewParent parent = view.getParent();
        if (parent == null || !View.class.isInstance(parent))
        {
            return;
        }
        final View parentView = (View) parent;
        parentView.post(new Runnable()
        {
            @Override
            public void run()
            {
                Rect bounds = new Rect();

                view.getHitRect(bounds);
                bounds.left -= ENLARGE;
                bounds.right += ENLARGE;
                bounds.bottom += ENLARGE;
                bounds.top -= ENLARGE;

                TouchDelegate touchDelegate = new TouchDelegate(bounds, view);

                parentView.setTouchDelegate(touchDelegate);
            }
        });
    }
    
    /**
     * 设置数据
     * 
     * @param list List<String>
     * @param itemBackground 每一项的背景
     * @param textColor 字体颜色
     * @see [类、类#方法、类#成员]
     */
    public void setList(ArrayList<String> list, int itemBackground, int textColor, String title)
    {
        linearLayout.removeAllViews();

        selectedTextView = null;
        if (mTitle != null)
        {
            mTitle.setText(title);
        }
        
        View view;
        TextView textView;
        int i = 0;
        for (String str : list)
        {
            view = inflater.inflate(R.layout.horizontal_text_item, null);

            textView = (TextView) view.findViewById(R.id.horizontal_text_item_text);

            textView.setText(str);
            // textView.setTextColor(COLOR_UNSELECTED);
            if (textColor != -1)
            {
                textView.setTextColor(mContext.getResources().getColorStateList(textColor));
            }

            if (itemBackground != -1)
            {
                textView.setBackgroundResource(itemBackground);
            }

            linearLayout.addView(view, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.FILL_PARENT));

            final int position = i;
            textView.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    boolean b = false;

                    if (onItemClickListener != null)
                    {
                        b = onItemClickListener.onItemClick(position);
                    }

                    if (b)
                    {
                        setSelection(position);
                    }
                }
            });

            enlargeClickArea(textView);

            i++;
        }
    }

    /**
     * 选中某一项
     * 
     * @param position int
     * @see [类、类#方法、类#成员]
     */
    public void setSelection(int position)
    {
        if (selectedTextView != null)
        {
            // selectedTextView.setTextColor(COLOR_UNSELECTED);
            selectedTextView.setSelected(false);
        }

        /*
         * TextView textView = (TextView) linearLayout.getChildAt(position)
         * .findViewById(R.id.horizontal_text_item_text); if (textView !=
         * null) { selectedTextView = textView; //
         * selectedTextView.setTextColor(COLOR_SELECTED);
         * selectedTextView.setSelected(true); }
         */
        selectedTextView = linearLayout.getChildAt(position);
        selectedTextView.setSelected(true);
    }

    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.onItemClickListener = listener;
    }

    /** 点击事件 */
    public static interface OnItemClickListener
    {
        /**
         * 点击事件
         * 
         * @param position int
         * @return 点击是否执行
         * @see [类、类#方法、类#成员]
         */
        boolean onItemClick(int position);
    }

}
