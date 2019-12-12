package com.github.epubparsersampleandroidapplication;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.ReadingException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;

public class FragmentOne extends Fragment {

    private static String message;
    private TextView startMsg;
    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private Button takePicture;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int CAMERA_REQUEST = 1888; // field

    private FragmentOne mInstance;
    private ArrayList<String> BookList = new ArrayList<>();
    final public static ArrayList<String> RecommendList = new ArrayList<>();

    public FragmentOne() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RecommendBooks(BookList);
        return inflater.inflate(R.layout.fragment_one, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolbar);

        txtSpeechInput = (TextView) getView().findViewById(R.id.txtSpeechInput_fragOne);
        btnSpeak = (ImageButton) getView().findViewById(R.id.btnSpeak_fragOne);
        takePicture = getView().findViewById(R.id.camera_fragOne);
        Log.d("click", btnSpeak.toString());

        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("click", "image clicked");
                promptSpeechInput();

            }

        });

        takePicture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        ((GridView) getView().findViewById(R.id.personal_book_info)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String clickedItemFilePath = ((BookInfo) adapterView.getAdapter().getItem(i)).getFilePath();
                askForWidgetToUse(clickedItemFilePath);
            }
        });

        ((GridView) getView().findViewById(R.id.recomend_book_info)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String clickedItemFilePath = ((BookInfo) adapterView.getAdapter().getItem(i)).getFilePath();
                askForWidgetToUse(clickedItemFilePath);
            }
        });

        startMsg = (TextView) getView().findViewById(R.id.startText);


        //new ListBookInfoTask().execute();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getActivity().getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void askForWidgetToUse(final String filePath) {
        Log.d("BEFORE", "askForWidgetToUse: ");
        final Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("filePath", filePath);
        Log.d("AFTER", "askForWidgetToUse: ");
        new AlertDialog.Builder(getActivity())
                .setTitle("Pick your widget")
                .setMessage("Textview or vWebView?")
                .setPositiveButton("TextView", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        intent.putExtra("isWebView", false);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("WebView", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        intent.putExtra("isWebView", true);
                        startActivity(intent);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onResume(){
        Log.d("Fragments","Fragments called");
        String message = getInstance();
        if(message != null){
            Log.d("Message",message);
            if(!BookList.contains(message)){
                BookList.add(message);
            }
            RecommendBooks(BookList);
            new ListBookInfoTask().execute();
        }
        Log.d("BookList",BookList.toString());
        super.onResume();
        //OnResume Fragment
    }

    private class ListBookInfoTask extends AsyncTask<Object, Object, List<BookInfo>> {

        private Exception occuredException;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startMsg.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<BookInfo> doInBackground(Object... params) {
            Log.d("GridView", "Inside GridView");
            List<BookInfo> bookInfoList = searchForPdfFiles();

            Reader reader = new Reader();
            for (BookInfo bookInfo : bookInfoList) {
                try {
                    reader.setInfoContent(bookInfo.getFilePath());

                    String title = reader.getInfoPackage().getMetadata().getTitle();
                    if (title != null && !title.equals("")) {
                        bookInfo.setTitle(reader.getInfoPackage().getMetadata().getTitle());
                    } else { // If title doesn't exist, use fileName instead.
                        Log.d("FILENAME USING", "doInBackground: ");
                        int dotIndex = bookInfo.getTitle().lastIndexOf('.');
                        bookInfo.setTitle(bookInfo.getTitle().substring(0, dotIndex));
                    }

                    bookInfo.setCoverImage(reader.getCoverImage());
                    Log.d("COVER", "doInBackground: " + reader.getCoverImage());
                } catch (ReadingException e) {
                    occuredException = e;
                    e.printStackTrace();
                }
            }
            return bookInfoList;
        }

        @Override
        protected void onPostExecute(List<BookInfo> bookInfoList) {
            super.onPostExecute(bookInfoList);
            startMsg.setVisibility(View.GONE);

            if (bookInfoList != null) {
                BookInfoGridAdapter adapter = new BookInfoGridAdapter(getActivity().getApplicationContext(), bookInfoList);
                ((GridView) getView().findViewById(R.id.personal_book_info)).setAdapter(adapter);
            }

            if (occuredException != null) {
                Toast.makeText(getActivity().getApplicationContext(), occuredException.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private List<BookInfo> searchForPdfFiles() {
        boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        List<BookInfo> bookInfoList = null;

        if (isSDPresent) {
            bookInfoList = new ArrayList<>();

            List<File> files;

//            read from database instead

            Log.d("BookList",BookList.toString());

            for(int i=0;i<BookList.size();i++){
                File file = getFileFromAssets(BookList.get(i));
                BookInfo bookInfo = new BookInfo();

                bookInfo.setTitle(file.getName());
                bookInfo.setFilePath(file.getPath());
                bookInfoList.add(bookInfo);
            }

            Log.d("BookInfoList",bookInfoList.toString());
            /*File sampleFile = getFileFromAssets("pg28885-images_new.epub");
            File gas_oil = getFileFromAssets("Gas_and_Oil_Engines,_Simply_Explained_by_Walter_C._Runciman.epub");
            File opportunities_oil = getFileFromAssets("Opportunities_in_Engineering_by_Charles_M._Horton.epub");*/

            /*files.add(0, sampleFile);
            files.add(1, gas_oil);
            files.add(2, opportunities_oil);

            for (File file : files) {
                BookInfo bookInfo = new BookInfo();

                bookInfo.setTitle(file.getName());
                bookInfo.setFilePath(file.getPath());

                bookInfoList.add(bookInfo);
            }*/
        }
        return bookInfoList;
    }

    public File storeFile(byte[] bytes) {
        File file = new File(getActivity().getCacheDir() + "/" + "books" );
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    public File getFileFromAssets(String fileName) {

        File file = new File(getActivity().getCacheDir() + "/" + fileName);

        if (!file.exists()) try {

            InputStream is = getActivity().getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return file;
    }

    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    inFiles.addAll(getListFiles(file));
                } else {
                    if (file.getName().endsWith(".epub")) {
                        inFiles.add(file);
                    }
                }
            }
        }
        return inFiles;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == -1 && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d("textValue", result.get(0));
                    txtSpeechInput.setText(result.get(0));
                }
                break;
            }
        }

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap picture = (Bitmap) data.getExtras().get("data");//this is your bitmap image and now you can do whatever you want with this
            TextRecognizer textRecognizer = new TextRecognizer.Builder(getActivity().getApplicationContext()).build();
            Frame imageFrame = new Frame.Builder()
                    .setBitmap(picture)                 // your image bitmap
                    .build();

            String imageText = "";

            SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                imageText = textBlock.getValue();                   // return string
            }
            txtSpeechInput.setText(imageText);
            Log.d("TEXT", "onActivityResult: " + imageText);
        }
    }

    public static void setInstance(String value){
        message = value;
    }

    public static String getInstance(){
        return message;
    }

    public void RecommendBooks(ArrayList<String> BookList){
        final FragmentOne f1 = new FragmentOne();
        Log.d("BookList in Recommend",BookList.toString());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final List<Type> mArrayList = new ArrayList<>();
        //FirebaseDatabase storage = FirebaseDatabase.getInstance();
        Log.d("SIZE", "RecommendBooks: " + BookList.size());

        for(int i=0; i<BookList.size();i++){
            Log.d("HERE", "RecommendBooks: ");
            Log.d("Book ID",BookList.get(i));
            String BookName = BookList.get(i).replace("_"," ");
            int index = BookName.indexOf("by");
            BookName = BookName.substring(0,index-1);
            Log.d("BookName",BookName);
            Task<QuerySnapshot> docRef = db.collection(BookName).get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String title = document.getData().toString();
                                    Log.d("Title before processing",title);
                                    if(title!="" && title!=null){
                                        int index = title.indexOf("=");
                                        title=title.substring(index+1,title.length()-1);
                                        Log.d("TITLEEEEE", title);
                                        if(!((f1.RecommendList).contains(title))){
                                            f1.RecommendList.add(title);
                                        }
                                        Log.d("RecommendList",f1.RecommendList.toString());
                                    }
                                    //Log.d("FIND", document.getId() + " => " + document.getData());
                                }
                                new ListRecommendInfoTask().execute();
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        }

    }

    private class ListRecommendInfoTask extends AsyncTask<Object, Object, List<BookInfo>> {

        private Exception occuredException;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startMsg.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<BookInfo> doInBackground(Object... params) {
            Log.d("GridView", "Inside GridView");
            Log.d("Reco in Async",FragmentOne.RecommendList.toString());
            List<BookInfo> bookInfoList = searchForEPubFiles();

            Reader reader = new Reader();
            for (BookInfo bookInfo : bookInfoList) {
                try {
                    reader.setInfoContent(bookInfo.getFilePath());

                    String title = reader.getInfoPackage().getMetadata().getTitle();
                    if (title != null && !title.equals("")) {
                        bookInfo.setTitle(reader.getInfoPackage().getMetadata().getTitle());
                    } else { // If title doesn't exist, use fileName instead.
                        Log.d("FILENAME USING", "doInBackground: ");
                        int dotIndex = bookInfo.getTitle().lastIndexOf('.');
                        bookInfo.setTitle(bookInfo.getTitle().substring(0, dotIndex));
                    }

                    bookInfo.setCoverImage(reader.getCoverImage());
                    Log.d("COVER", "doInBackground: " + reader.getCoverImage());
                } catch (ReadingException e) {
                    occuredException = e;
                    e.printStackTrace();
                }
            }
            return bookInfoList;
        }

        @Override
        protected void onPostExecute(List<BookInfo> bookInfoList) {
            super.onPostExecute(bookInfoList);
            startMsg.setVisibility(View.GONE);

            if (bookInfoList != null) {
                BookInfoGridAdapter adapter = new BookInfoGridAdapter(getActivity().getApplicationContext(), bookInfoList);
                ((GridView) getView().findViewById(R.id.recomend_book_info)).setAdapter(adapter);
            }

            if (occuredException != null) {
                Toast.makeText(getActivity().getApplicationContext(), occuredException.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private List<BookInfo> searchForEPubFiles() {
        boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        FragmentOne f1 = new FragmentOne();
        List<BookInfo> bookInfoList = null;

        if (isSDPresent) {
            bookInfoList = new ArrayList<>();

            List<File> files;

//            read from database instead

            Log.d("BookList",f1.RecommendList.toString());

            for(int i=0;i<f1.RecommendList.size();i++){
                File file = getFileFromAssets(f1.RecommendList.get(i));
                BookInfo bookInfo = new BookInfo();

                bookInfo.setTitle(file.getName());
                bookInfo.setFilePath(file.getPath());
                bookInfoList.add(bookInfo);
            }

            Log.d("BookInfoList",bookInfoList.toString());
            /*File sampleFile = getFileFromAssets("pg28885-images_new.epub");
            File gas_oil = getFileFromAssets("Gas_and_Oil_Engines,_Simply_Explained_by_Walter_C._Runciman.epub");
            File opportunities_oil = getFileFromAssets("Opportunities_in_Engineering_by_Charles_M._Horton.epub");*/

            /*files.add(0, sampleFile);
            files.add(1, gas_oil);
            files.add(2, opportunities_oil);

            for (File file : files) {
                BookInfo bookInfo = new BookInfo();

                bookInfo.setTitle(file.getName());
                bookInfo.setFilePath(file.getPath());

                bookInfoList.add(bookInfo);
            }*/
        }
        return bookInfoList;
    }
}
