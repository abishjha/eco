package edu.ramapo.ajha.eco;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;

class Database{
    private static final String TAG = "Database";

    private static DatabaseReference mDatabase;
    private static GoogleSignInAccount mUserAccount;

    private static final String DB_KEY_AUTHOR = "author";
    private static final String DB_KEY_AUTHORID = "authorID";
    private static final String DB_KEY_CONTENT = "content";
    private static final String DB_KEY_DOCID = "docID";
    private static final String DB_KEY_TIME = "time";
    private static final String DB_KEY_TITLE = "title";

    private static final String DB_DOCUMENT_META_DATA = "meta-data";
    private static final String DB_DOCUMENT_CONTENT = "content";


    // initializes and/or resets the mDatabase reference to the top of the JSON tree
    private static void init(){
        if(mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            Log.w(TAG, "connected to database");
        }
    }

    // checks if a user is present in the app database, if not, adds the user
    static void registerUser(final GoogleSignInAccount account){
        init();

        mUserAccount = account;

        // code to register user
        final DatabaseReference users = mDatabase.child(AppContext.getContext().getString(R.string.db_users));

        if(account.getId() != null) {
            users.child(account.getId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                Log.w(TAG, "user exists " + account.getDisplayName());
                            } else {
                                Log.w(TAG, "adding user " + account.getDisplayName());

                                users.child(account.getId()).child("name").setValue(account.getDisplayName());
                                users.child(account.getId()).child("email").setValue(account.getEmail());
                                //users.child(account.getId()).child("photo").setValue(account.getPhotoUrl());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.w(TAG, "register user operation cancelled");
                        }
                    });
        }
    }

    static void getMetaData(final String section, final RecyclerView recyclerView){
        init();

        DatabaseReference newRef = mDatabase.child(section).child(DB_DOCUMENT_META_DATA);

        newRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.w(TAG, "[GET DATA] data extracted from DB");

                recyclerView.setAdapter(new MyAdapter((HashMap) dataSnapshot.getValue(), section));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "[GET DATA] DB operation cancelled");
            }
        });
    }

    static void getDetailedData(String section, String entryID, final DetailActivity viewActivity){
        init();

        DatabaseReference newRef = mDatabase.child(section).child(DB_DOCUMENT_CONTENT).child(entryID);

        newRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.w(TAG, "[GET DATA] data extracted from DB");

                viewActivity.setData((HashMap) dataSnapshot.getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "[GET DATA] DB operation cancelled");
            }
        });
    }

    static void insertData(String section, String title, String content){
        HashMap<String, String> entry = new HashMap<>();
        entry.put(DB_KEY_TITLE, title);
        entry.put(DB_KEY_CONTENT, content);

        insertData(section, entry);
    }

    static void insertData(String section, HashMap<String, String> entry){
        init();

        DatabaseReference metaDataDocument = mDatabase.child(section).child(DB_DOCUMENT_META_DATA);
        DatabaseReference contentDocument = mDatabase.child(section).child(DB_DOCUMENT_CONTENT);

        if(!entry.containsKey(DB_KEY_TITLE) || !entry.containsKey(DB_KEY_CONTENT))
            return;

        // generate a random universally unique identifier for the entry
        String entryID = UUID.randomUUID().toString();
        entry.put(DB_KEY_DOCID, entryID);

        HashMap<String, String> metaDataEntry = getMetaDataMap(entry);
        HashMap<String, String> contentEntry = getContentMap(entry);

        metaDataDocument.child(entryID).setValue(metaDataEntry);
        contentDocument.child(entryID).setValue(contentEntry);
    }

    private static HashMap<String, String> getMetaDataMap(HashMap<String, String> entry){
        HashMap<String, String> modifiedEntry = new HashMap<>(entry);
        modifiedEntry.remove(DB_KEY_CONTENT);

        modifiedEntry.put(DB_KEY_AUTHOR, mUserAccount.getDisplayName());
        modifiedEntry.put(DB_KEY_TIME, getTodaysDate());
        return modifiedEntry;
    }

    private static HashMap<String, String> getContentMap(HashMap<String, String> entry){
        HashMap<String, String> modifiedEntry = new HashMap<>(entry);
        modifiedEntry.put(DB_KEY_AUTHOR, mUserAccount.getDisplayName());
        modifiedEntry.put(DB_KEY_AUTHORID, mUserAccount.getId());
        modifiedEntry.put(DB_KEY_TIME, getTodaysDate());
        return modifiedEntry;
    }

    private static String getTodaysDate(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate localDate = LocalDate.now();
        return dtf.format(localDate);
    }

    // print the entire database for debug purposes
    static void printDB(){
        init();

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Log.w(TAG, "[DEBUG] data found in DB.  printing...");
                    System.out.println(dataSnapshot.getValue());
                }
                else{
                    Log.w(TAG, "[DEBUG] data not found in DB");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "[DEBUG] DB operation cancelled");
            }
        });
    }
}
