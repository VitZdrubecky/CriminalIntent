package cz.zdrubecky.criminalintent;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CrimeLab {
    // A singleton class, available in memory throughout the whole application lifetime
    private static CrimeLab sCrimeLab;

    // This variable uses an interface rather than its specific implementation
    private List<Crime> mCrimes;

    private CrimeLab(Context context) {
        // The compiler here infers the data type thanks to JAVA 7 diamond notation
        mCrimes = new ArrayList<>();

        // Generate some dummy crimes
//        for (int i = 0; i < 100; i++) {
//            Crime crime = new Crime();
//            crime.setTitle("Crime #" + i);
//            crime.setSolved(i % 2 == 0); // Every other one
//            mCrimes.add(crime);
//        }
    }

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }

        return sCrimeLab;
    }

    public List<Crime> getCrimes() {
        return mCrimes;
    }

    public Crime getCrime(UUID id) {
        for (Crime crime : mCrimes) {
            if (crime.getId().equals(id)) {
                return crime;
            }
        }

        return null;
    }

    public void addCrime(Crime crime) {
        mCrimes.add(crime);
    }
}
