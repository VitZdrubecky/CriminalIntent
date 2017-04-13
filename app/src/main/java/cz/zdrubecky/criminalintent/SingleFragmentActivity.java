package cz.zdrubecky.criminalintent;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

// FragmentActivity lets me use the support library fragments, AppCompatActivity is its subclass and gives us a toolbar!
public abstract class SingleFragmentActivity extends AppCompatActivity {
    // Returns the specific fragment to be added to the manager
    protected abstract Fragment createFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = createFragment();

            // The first method returns a fragment transaction, which can used for chaining
            fm.beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();
        }
    }
}
