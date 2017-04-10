package cz.zdrubecky.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

public class CrimeListFragment extends Fragment {
    // The request code for the crime activity
    private static final int REQUEST_CRIME = 0;

    private int mClickedCrimePosition;
    private boolean mCrimeChanged = false;

    // This class is much easier to maintain than previous implementations with the additional advantage of View animations (like moving or deleting an item)
    private RecyclerView mCrimeRecyclerView;
    private CrimeAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        // Recycler view is a view as any other, so we can get it as such
        mCrimeRecyclerView = (RecyclerView) view.findViewById(R.id.crime_recycler_view);
        // It has to have a manager, like fragment manager - but different. It handles the view positioning itself.
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Wire up the adapter
        updateUI();

        return view;
    }

    // Called by the parent activity, whose onResume is in turn called by the OS
    // This is the safest place to update the fragment
    @Override
    public void onResume() {
        super.onResume();

        updateUI();
    }

    private void updateUI() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimes();

        // If the fragment is already running, update the data in case something changed (some crime)
        if (mAdapter == null) {
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        } else if (mCrimeChanged) {
            mAdapter.notifyItemChanged(mClickedCrimePosition);
        }
    }

    // This thing takes care of one view - exposes everything
    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Crime mCrime;
        private TextView mTitleTextView;
        private TextView mDateTextView;
        private CheckBox mSolvedCheckBox;

        public CrimeHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            // The expensive find() calls are only run once, during the holder instantiation
            mTitleTextView = (TextView) itemView.findViewById(R.id.list_crime_item_title_text_view);
            mDateTextView = (TextView) itemView.findViewById(R.id.list_item_crime_date_text_view);
            mSolvedCheckBox = (CheckBox) itemView.findViewById(R.id.list_item_crime_solved_check_box);
        }

        public void bindCrime(Crime crime) {
            mCrime = crime;
            mTitleTextView.setText(mCrime.getTitle());
            DateFormat dateFormat = new DateFormat();
            mDateTextView.setText(dateFormat.format("EEEE, MMM dd, yyyy", mCrime.getDate()));
            mSolvedCheckBox.setChecked(mCrime.isSolved());
        }

        @Override
        public void onClick(View v) {
            // Set the crime's position for later use
            mClickedCrimePosition = getAdapterPosition();
            Log.d("Crime", "Clicked position = " + mClickedCrimePosition);

            Intent intent = CrimePagerActivity.newIntent(getActivity(), mCrime.getId());
            startActivityForResult(intent, REQUEST_CRIME);
        }
    }

    // This here is a glue between Recycler and ViewHolder, it answers Recycler's requests
    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder> {
        private List<Crime> mCrimes;

        public CrimeAdapter(List<Crime> crimes) {
            mCrimes = crimes;
        }

        // RecyclerView needs a new View to display an item
        @Override
        public CrimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d("Crime", "onCreateViewHolder() called");

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            // Inflate default view for text from android library
//            View view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);

            // Inflate my own view
            View view = layoutInflater.inflate(R.layout.list_item_crime, parent, false);

            return new CrimeHolder(view);
        }

        // This method will bind a ViewHolder’s View to the model object
        @Override
        public void onBindViewHolder(CrimeHolder holder, int position) {
            Log.d("Crime", "onBindViewHolder() called");

            Crime crime = mCrimes.get(position);
            holder.bindCrime(crime);
        }

        @Override
        public int getItemCount() {
            return mCrimes.size();
        }
    }

    // The result calls are routed from the parent activity, but only the activity can set the result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Crime", "The result has returned: " + requestCode + " " + resultCode);

        if (requestCode == REQUEST_CRIME && resultCode == Activity.RESULT_OK) {
            mCrimeChanged = CrimeFragment.wasCrimeChanged(data);
            Log.d("Crime", "The crime was changed = " + mCrimeChanged);
        }
    }
}
