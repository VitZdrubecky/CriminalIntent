package cz.zdrubecky.criminalintent;

import android.content.Intent;
import android.support.v4.app.Fragment;

// This class listens to everyone's events
public class CrimeListActivity
        extends SingleFragmentActivity
        implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks {
    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }

    @Override
    protected int getLayoutResId() {
        // Get the layout using an alias, which lives in its own directories to prevent redundancy and chooses between layouts according to the device dimension
        return R.layout.activity_masterdetail;
    }

    // Interface method used as a handler to pass information from the child fragment to this class
    @Override
    public void onCrimeSelected(Crime crime, boolean isNew, int requestCode) {
        // Check if there's a split view and therefore we're working with a tablet
        if (findViewById(R.id.detail_fragment_container) == null) {
            Intent intent = CrimePagerActivity.newIntent(this, crime.getId(), isNew);
            startActivityForResult(intent, requestCode);
        } else {
            Fragment newDetail = CrimeFragment.newInstance(crime.getId());

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment_container, newDetail)
                    .commit();
        }
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        CrimeListFragment listFragment = (CrimeListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        listFragment.updateUI();
    }
}
