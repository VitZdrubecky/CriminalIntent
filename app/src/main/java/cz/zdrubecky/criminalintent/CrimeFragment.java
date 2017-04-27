package cz.zdrubecky.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {
    private static final String TAG = "CrimeFragment";
    private static final String ARG_CRIME_ID = "crime_id";
    private static final String KEY_CRIME_CHANGED = "crime_changed";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";
    private static final String DIALOG_IMAGE = "DialogImage";
    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;
    private static final int REQUEST_CONTACT = 2;
    private static final int REQUEST_NUMBER = 3;
    private static final int REQUEST_PHOTO = 4;

    private Crime mCrime;
    private boolean mCrimeChanged = false;
    private File mPhotoFile;
    private Point mPhotoViewDimensions;

    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private EditText mTitleField;
    private Button mDateButton;
    private Button mTimeButton;
    private CheckBox mSolvedCheckBox;
    private Button mReportButton;
    private Button mSuspectButton;
    private Button mSuspectCallButton;

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
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);

        // Due to the JAVA's pointers passed by value, the crime is now kept in a lab and can be modified from outside
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);

        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);

        if (savedInstanceState != null) {
            setCrimeChangedResult(savedInstanceState.getBoolean(KEY_CRIME_CHANGED, false));
        }

        setHasOptionsMenu(true);
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

        mReportButton = (Button) v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an implicit intent requesting an action to send
                // User can make some app default for this type of intent
                // The following code can be solved using a ShareCompat.IntentBuilder class
                Intent i = new Intent(Intent.ACTION_SEND);
                // Set the type here, there's no place for it in the constructor
                i.setType("text/plain");
                // Use well known constants
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
                // Force the chooser to be displayed
                i = Intent.createChooser(i, getString(R.string.send_report));

                startActivity(i);
            }
        });

        // This intent is gonna be needed a few times, so it's final and outside any listener
        // It asks OS to pick a contact in the specified place
        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        // Check if there's an app that can handle my request and ONLY the default ones (which are defined through their manifests)
        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mSuspectCallButton = (Button) v.findViewById(R.id.crime_suspect_call);
        mSuspectCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_DIAL, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);

                startActivityForResult(i, REQUEST_NUMBER);
            }
        });

        if (mCrime.getSuspect() == null) {
            mSuspectCallButton.setEnabled(false);
        }

        // MediaStore is the lord and kaiser of all media-related
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Check if there's a place to store the image and if someone could take care of our intent
        boolean canTakePhoto = mPhotoFile != null && captureImage.resolveActivity(packageManager) != null;
        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);
        mPhotoButton.setEnabled(canTakePhoto);
        if (canTakePhoto) {
            // Create the file uri and put it to the intent using a well known constant so that the camera knows to store it instead of returning a thumbnail
            Uri uri = Uri.fromFile(mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mPhotoView = (ImageView) v.findViewById(R.id.crime_photo);
        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (mPhotoFile != null && mPhotoFile.exists()) {
                FragmentManager manager = getFragmentManager();
                ImageEnlargeFragment imageEnlargeFragment = ImageEnlargeFragment.newInstance(mPhotoFile);

                imageEnlargeFragment.show(manager, DIALOG_IMAGE);
            }
            }
        });
        // Set the layout listener to wait for the layout pass
        mPhotoView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // The view dimensions are known
                mPhotoViewDimensions = new Point(mPhotoView.getWidth(), mPhotoView.getHeight());
                Log.d(TAG, "Setting the ImageView dimensions to " + mPhotoViewDimensions.x + ", " + mPhotoViewDimensions.y + " after the layout pass.");
                updatePhotoView();
                mPhotoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_delete_crime:
                // Remove the crime and finish the activity
                if (CrimeLab.get(getActivity()).removeCrime(mCrime)) {
                    getActivity().finish();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(this.getActivity()).updateCrime(mCrime);
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

        Log.d(TAG, "The result code: " + Activity.RESULT_OK);

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
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return (projection)
            String[] queryFields = new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.LOOKUP_KEY
            };
            // Perform your query from an Android contacts db - the contactUri is like a "where" clause here
            // (so the third parameter - selection - is not necessary) because it includes contact' row id
            // The access rights are provided by the Contacts app and extended to me for a one time use!
            Cursor c = getActivity().getContentResolver().query(contactUri, queryFields, null, null, null);

            try {
                if (c.getCount() == 0) {
                    return;
                }

                c.moveToFirst();
                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);

                // TODO get the fucking ID
                int i = c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                String suspectId = c.getString(100);

            } finally {
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            updatePhotoView();
        }
    }

    private void updateDate(Date date) {
        mDateButton.setText(DateFormat.format("EEEE, MMM dd, yyyy", date));
        mTimeButton.setText(DateFormat.format("HH:mm", date));
    }

    private void updatePhotoView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap;

            if (mPhotoViewDimensions != null) {
                bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), mPhotoViewDimensions.x, mPhotoViewDimensions.y);
            } else {
                bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), getActivity());
            }

            mPhotoView.setImageBitmap(bitmap);
        }
    }

    // This method builds the report and should be here instead of a model 'cause it uses resources
    private String getCrimeReport() {
        String solvedString;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        // Send the strings to the placeholders in the predefined order
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);

        return report;
    }
}
