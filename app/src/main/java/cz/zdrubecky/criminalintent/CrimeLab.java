package cz.zdrubecky.criminalintent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.zdrubecky.criminalintent.database.CrimeBaseHelper;
import cz.zdrubecky.criminalintent.database.CrimeCursorWrapper;
import cz.zdrubecky.criminalintent.database.CrimeDBSchema.CrimeTable;

// The initial implementation without a db is commented out
public class CrimeLab {
    // A singleton class, available in memory throughout the whole application lifetime
    private static CrimeLab sCrimeLab;

    // This variable uses an interface declaration rather than its specific implementation
//    private List<Crime> mCrimes;
    // Remember in which context was the lab created
    private Context mContext;
    private SQLiteDatabase mDatabase;

    private CrimeLab(Context context) {
        // Keep the app context instead of an activity (so it can be garbage collected, cause there's no reference to it)
        mContext = context.getApplicationContext();
        mDatabase = new CrimeBaseHelper(mContext).getWritableDatabase();

        // The compiler here infers the data type thanks to JAVA 7 diamond notation
//        mCrimes = new ArrayList<>();

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
//        return mCrimes;
        List<Crime> crimes = new ArrayList<>();

        CrimeCursorWrapper cursor = queryCrimes(null, null);

        try {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                crimes.add(cursor.getCrime());
                cursor.moveToNext();
            }
        } finally {
            // Close the cursor so that we don't run out of handles
            cursor.close();
        }

        return crimes;
    }

    public Crime getCrime(UUID id) {
//        for (Crime crime : mCrimes) {
//            if (crime.getId().equals(id)) {
//                return crime;
//            }
//        }
//        return null;
        CrimeCursorWrapper cursor = queryCrimes(
                CrimeTable.Cols.UUID + " = ?",
                new String[] { id.toString() }
        );

        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getCrime();
        } finally {
            cursor.close();
        }
    }

    public void addCrime(Crime crime) {
//        mCrimes.add(crime);
        ContentValues values = getContentValues(crime);

        // The second argument - nullColumnHack - is used to force insert in the case of empty values
        mDatabase.insert(CrimeTable.NAME, null, values);
    }

    public void updateCrime(Crime crime) {
        String uuidString = crime.getId().toString();
        ContentValues values = getContentValues(crime);

        mDatabase.update(CrimeTable.NAME, values, CrimeTable.Cols.UUID + " = ?", new String[] {uuidString});
    }

    public boolean removeCrime(Crime crime) {
//        return mCrimes.remove(crime);
        String uuidString = crime.getId().toString();

        return mDatabase.delete(CrimeTable.NAME, CrimeTable.Cols.UUID + " = ?", new String[] {uuidString}) > 0;
    }

    // Mapping of the crime to the columns
    private static ContentValues getContentValues(Crime crime) {
        ContentValues values = new ContentValues();

        values.put(CrimeTable.Cols.UUID, crime.getId().toString());
        values.put(CrimeTable.Cols.TITLE, crime.getTitle());
        values.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        values.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);

        return values;
    }

    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        // Second arg - null - select all the columns
        Cursor cursor = mDatabase.query(CrimeTable.NAME, null, whereClause, whereArgs, null, null, null);

        // Wrap the cursor nicely in our own implementation
        return new CrimeCursorWrapper(cursor);
    }
}
