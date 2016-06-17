package com.zheblog.bgmoveviewpager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.zheblog.bgmoveviewpager.adapter.VerticalPagerAdapter;
import com.zheblog.bgmoveviewpager.fragment.BottomFragment;
import com.zheblog.bgmoveviewpager.fragment.PagerFragment;
import com.zheblog.bgmoveviewpager.interfaces.IScrollListener;
import com.zheblog.bgmoveviewpager.interfaces.IViewPagerCurrent;
import com.zheblog.bgmoveviewpager.widget.VerticalViewPager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements IScrollListener,IViewPagerCurrent {

    private VerticalViewPager verticalViewPager;
    private VerticalPagerAdapter mAdapter;
    private List<Fragment> mFragments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        verticalViewPager = (VerticalViewPager) findViewById(R.id.vertical_pager);
        PagerFragment pagerFragment = new PagerFragment();
        BottomFragment bottomFragment = new BottomFragment();
        mFragments.add(pagerFragment);
        mFragments.add(bottomFragment);
        mAdapter = new VerticalPagerAdapter(getSupportFragmentManager(), mFragments);
        verticalViewPager.setAdapter(mAdapter);
    }

    @Override
    public void canScrollView(boolean isCanScroll) {
        verticalViewPager.setScroll(isCanScroll);
    }

    @Override
    public void setCurrentPager(int position) {
        verticalViewPager.setCurrentItem(position);
    }
}
