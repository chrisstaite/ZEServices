package com.yourdreamnet.zeservices;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;

import com.yourdreamnet.zeservices.ui.carstatus.CarStatusFragment;
import com.yourdreamnet.zeservices.ui.chargedata.ChargeDataFragment;
import com.yourdreamnet.zeservices.ui.conditioning.ConditioningFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class MainTabManager extends FragmentPagerAdapter {

    private Context mContext;

    MainTabManager(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position)
        {
            case 0:
                return new CarStatusFragment();
            case 1:
                return new ConditioningFragment();
            case 2:
                return new ChargeDataFragment();
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        int icon = -1;
        int string = -1;
        switch (position)
        {
            case 0:
                icon = R.drawable.ic_baseline_car;
                string = R.string.car_status;
                break;
            case 1:
                icon = R.drawable.ic_baseline_ac_unit;
                string = R.string.air_conditioning;
                break;
            case 2:
                icon = R.drawable.ic_baseline_power;
                string = R.string.charge_data;
                break;
        }
        SpannableString sb = new SpannableString(mContext.getString(string));
        Drawable image = mContext.getResources().getDrawable(icon);
        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
        //ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
        //sb.setSpan(imageSpan, 0, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

}
