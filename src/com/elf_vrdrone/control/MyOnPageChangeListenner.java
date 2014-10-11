package com.elf_vrdrone.control;

import com.ElecFreaks.ELF_vrdrone.R;

import android.content.Context;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.widget.ImageView;

public class MyOnPageChangeListenner implements OnPageChangeListener{

	private ImageView[] imageViews;
	private Context context;
	private int currentPageIndex;
	
	public MyOnPageChangeListenner(Context context, ImageView[] imageViews, int initPage) {
		// TODO Auto-generated constructor stub
		this.imageViews = imageViews;
		this.context = context;
		currentPageIndex = initPage;
		onPageSelected(initPage);
	}
	
	@Override
	public void onPageScrollStateChanged(int arg0) {
		// arg0=0:ʲô��û��, arg0=1:���ڻ���, arg0=2:�������
		// TODO Auto-generated method stub
	}

	/*
	 * arg0 :��ǰҳ�棬������������ҳ��
	 * arg1:��ǰҳ��ƫ�Ƶİٷֱ�
	 * arg2:��ǰҳ��ƫ�Ƶ�����λ�� */
	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
	
	public int getCurrentPageIndex(){
		return currentPageIndex;
	}

	@Override
	public void onPageSelected(int arg0) {
		// TODO Auto-generated method stub
		currentPageIndex = arg0;
		if(imageViews != null){
			for(int i=0; i<imageViews.length; i++){
				if(i == arg0)
					imageViews[i].setImageDrawable(context.getResources().getDrawable(R.drawable.btn_radio_on));
				else
					imageViews[i].setImageDrawable(context.getResources().getDrawable(R.drawable.btn_radio_off));
			}
		}
	}
}
