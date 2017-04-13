package cz.zdrubecky.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import java.sql.Time;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {
    private static final String ARG_CRIME_ID = "crime_id";
    private static final String KEY_CRIME_CHANGED = "crime_changed";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";
    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;

    private Crime mCrime;
    private boolean mCrimeChanged = false;

    private EditText mTitleField;
    private Button mDateButton;
    private Button mTimeButton;
    private CheckBox mSolvedCheckBox;

    // This is a convention method, used to offer the arguments init right after the fragment has been created but before it'd been attached
    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        // Save the ID in fragment rather than parent activity so that they can be decoupled and function independently
        // it is an equivalent of saved instance state, but this is more explicit
        args.putSerializable(ARG_CRIME_ID, crimeId);
        
        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d("CrimeFragment", "onCreate()");

        super.onCreate(savedInstanceState);

        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);

        // Due to the JAVA's pointers passed by value, the crime is now kept in a lab and can be modified from outside
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);

        if (savedInstanceState != null) {
            setCrimeChangedResult(savedInstanceState.getBoolean(KEY_CRIME_CHANGED, false));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                setCrimeChangedResult(true);
            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This returns the parent activity's manager so the dialog will be this one's sibling
                FragmentManager fragmentManager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());

                // This frag is now the target of the dialog and is able to receive results
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);

                // By putting in the manager, the dialog is added and committed for us using its unique name - we could do the same manually using transaction
                // This setup is better suited for tablets, where the fragments can be stacked above one another
                dialog.show(fragmentManager, DIALOG_DATE);

                setCrimeChangedResult(true);
            }
        });

        mTimeButton = (Button) v.findViewById(R.id.crime_time);
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                TimePickerFragment dialog = TimePickerFragment.newInstance(mCrime.getDate());

                dialog.setTargetFragment(CrimeFragment.this, REQUEST_TIME);

                dialog.show(fragmentManager, DIALOG_TIME);
            }
        });

        updateDate(mCrime.getDate());

        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
                setCrimeChangedResult(true);
            }
        });

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CRIME_CHANGED, mCrimeChanged);
    }

    // Set the result intent for the calling activity
    private void setCrimeChangedResult(boolean crimeChanged) {
        // Ugly hack to let the calling activity know to reload the whole crime list
        boolean isCrimeNew = getActivity().getIntent().getBooleanExtra(CrimePagerActivity.EXTRA_CRIME_IS_NEW, false);

        mCrimeChanged = !isCrimeNew && crimeChanged;

        Intent data = new Intent();
        data.putExtra(KEY_CRIME_CHANGED, crimeChanged);

        Log.d("Crime", "The result code: " + Activity.RESULT_OK);

        // The result can only be set by the parent activity
        getActivity().setResult(Activity.RESULT_OK, data);
    }

    public static boolean wasCrimeChanged(Intent data) {
        return data.getBooleanExtra(KEY_CRIME_CHANGED, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK) {
            return;
        }

        if(requestCode == REQUEST_DATE) {
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);

            mCrime.setDate(date);
            updateDate(mCrime.getDate());
        } else if (requestCode == REQUEST_TIME) {
            Date date = (Date) data.getSerializableExtra(TimePickerFragment.EXTRA_TIME);

            mCrime.setDate(date);
            updateDate(mCrime.getDate());
        }
    }

    private void updateDate(Date date) {
        DateFormat dateFormat = new DateFormat();
        mDateButton.setText(dateFormat.format("EEEE, MMM dd, yyyy", date));
        mTimeButton.setText(dateFormat.format("HH:mm", date));
    }
}
