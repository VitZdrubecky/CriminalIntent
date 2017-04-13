package cz.zdrubecky.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.List;
import java.util.UUID;

public class CrimePagerActivity extends AppCompatActivity {
    // This can be private, no on else has access to it
    private static final String EXTRA_CRIME_ID = "cz.zdrubecky.criminalintent.crime_id";
    public static final String EXTRA_CRIME_IS_NEW = "cz.zdrubecky.criminalintent.crime_is_new";

    // By default, the pager loads two additional fragments, one on either side
    private ViewPager mViewPager;
    private List<Crime> mCrimes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_pager);

        mViewPager = (ViewPager) findViewById(R.id.activity_crime_pager_view_pager);
        mCrimes = CrimeLab.get(this).getCrimes();

        FragmentManager fragmentManager = getSupportFragmentManager();

        // Adapter to communicate between ViewPager and its views
        // It needs a FragmentManager to manage the views, so we've created one for him
        // FragmentStatePagerAdapter is a nice encapsulation of the needed communication
        // In case there's a small number of fragments to manage (=low memory), we can use FragmentPagerAdapter instead
        // If we want to host common Views instead of Fragments, we make our own adapter implementing the PagerAdapter interface
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                Crime crime = mCrimes.get(position);

                return CrimeFragment.newInstance(crime.getId());
            }

            @Override
            public int getCount() {
                return mCrimes.size();
            }
        });

        // Take the UUID from an intent and use it to get the current crime
        UUID crimeId = (UUID) getIntent().getSerializableExtra(EXTRA_CRIME_ID);
        for (int i = 0; i < mCrimes.size(); i++) {
            if (mCrimes.get(i).getId().equals(crimeId)) {
                mViewPager.setCurrentItem(i);
                break;
            }
        }
    }

    public static Intent newIntent(Context packageContext, UUID crimeId, boolean isNew) {
        Intent intent = new Intent(packageContext, CrimePagerActivity.class);
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        intent.putExtra(EXTRA_CRIME_IS_NEW, isNew);

        return intent;
    }
}
