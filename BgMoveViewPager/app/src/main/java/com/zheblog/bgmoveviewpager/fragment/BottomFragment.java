package com.zheblog.bgmoveviewpager.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.zheblog.bgmoveviewpager.R;
import com.zheblog.bgmoveviewpager.interfaces.IViewPagerCurrent;

/**
 * Created by liuz on 16/6/17.
 */
public class BottomFragment extends Fragment implements View.OnClickListener {

    private View contentView;
    private ImageView btnTop;

    private IViewPagerCurrent mViewPagerListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mViewPagerListener = (IViewPagerCurrent) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.fragment_bottom, null);
        return contentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView(){
        btnTop = (ImageView) contentView.findViewById(R.id.btn_bottom_top);
        btnTop.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mViewPagerListener.setCurrentPager(0);
    }
}
