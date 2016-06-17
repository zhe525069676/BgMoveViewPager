package com.zheblog.bgmoveviewpager.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.zheblog.bgmoveviewpager.R;
import com.zheblog.bgmoveviewpager.interfaces.IViewPagerCurrent;

/**
 * Created by liuz on 16/6/17.
 */
public class HomeFragment extends Fragment {

    private View contentView;

    private ImageView btnLeft, btnRight, btnBottom;

    private View.OnClickListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.fragment_home, null);
        return contentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        btnLeft = (ImageView) contentView.findViewById(R.id.btn_left);
        btnLeft.setOnClickListener(mListener);
        btnRight = (ImageView) contentView.findViewById(R.id.btn_right);
        btnRight.setOnClickListener(mListener);
        btnBottom = (ImageView) contentView.findViewById(R.id.btn_bottom);
        btnBottom.setOnClickListener(mListener);
    }

    public void setPagerOnClickListener(View.OnClickListener listener) {
        mListener = listener;
    }

}
