package cz.zdrubecky.criminalintent.database;

// This design is for a safe access in the future (modifying / adding columns)
public class CrimeDBSchema {
    public static final class CrimeTable {
        public static final String NAME = "crimes";

        public static final class Cols {
            // Every new columns has to be mentioned on four different places - here, DBHelper, CrimeLab, Cursor
            public static final String UUID = "uuid";
            public static final String TITLE = "title";
            public static final String DATE = "date";
            public static final String SOLVED = "solved";
            public static final String SUSPECT = "suspect";
        }
    }
}
