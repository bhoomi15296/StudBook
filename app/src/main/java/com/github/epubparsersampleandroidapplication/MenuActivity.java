package com.github.epubparsersampleandroidapplication;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;


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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.ReadingException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MenuActivity extends Fragment {

    private ProgressBar progressBar;
    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private ImageButton takePicture;
    private Button searchButton;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int CAMERA_REQUEST = 1888; // field

    FirebaseStorage firebaseStorage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_menu, container, false);


    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolbar);

        txtSpeechInput = (TextView) getView().findViewById(R.id.txtSpeechInput);
        btnSpeak = (ImageButton) getView().findViewById(R.id.btnSpeak);
        searchButton =  getView().findViewById(R.id.button_search);
        takePicture =  (ImageButton) getView().findViewById(R.id.camera);
        Log.d("click",btnSpeak.toString());

        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("click","image clicked");
                promptSpeechInput();

            }

        });

        takePicture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                Intent cameraIntent = new  Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);


            }
        });



        ((GridView) getView().findViewById(R.id.grid_book_info)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String clickedItemFilePath = ((BookInfo) adapterView.getAdapter().getItem(i)).getFilePath();
                askForWidgetToUse(clickedItemFilePath);
            }
        });


        progressBar = (ProgressBar) getView().findViewById(R.id.progressbar);

        new ListBookInfoTask().execute();
        firebaseStorage = FirebaseStorage.getInstance();

    }


    private class ListBookInfoTask extends AsyncTask<Object, Object, List<BookInfo>> {

        private Exception occuredException;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<BookInfo> doInBackground(Object... params) {
            List<BookInfo> bookInfoList = new ArrayList<>();
            try {
            bookInfoList = searchForPdfFiles(); }
            catch (IOException ie) {

            }

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
                    Log.d("COVER", "doInBackground: "+ reader.getCoverImage());
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
            progressBar.setVisibility(View.GONE);

            if (bookInfoList != null) {
                final BookInfoGridAdapter adapter = new BookInfoGridAdapter(getActivity().getApplicationContext(), bookInfoList);

                ((GridView) getView().findViewById(R.id.grid_book_info)).setAdapter(adapter);

                searchButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Log.d("TEXT", "onClick: " + txtSpeechInput.getText());
                        adapter.getFilter().filter(txtSpeechInput.getText());

                    }
                });

            }

            if (occuredException != null) {
                Toast.makeText(getActivity().getApplicationContext(), occuredException.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private List<BookInfo> searchForPdfFiles() throws IOException {
        boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        List<BookInfo> bookInfoList = null;

        if (isSDPresent) {
            bookInfoList = new ArrayList<>();

            final List<File> files = getListFiles(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));

//            read from database instead
//            final StorageReference listRef = firebaseStorage.getReference("Music");

//            listRef.listAll()
//                    .addOnSuccessListener(new OnSuccessListener<ListResult>() {
//                        @Override
//                        public void onSuccess(ListResult listResult) {
//                            for (StorageReference prefix : listResult.getPrefixes()) {
//                                Log.d("TAG", "onSuccess: "+prefix);
//                                // All the prefixes under listRef.
//                                // You may call listAll() recursively on them.
//                            }
//
//                            for (final StorageReference item : listResult.getItems()) {
//                                // All the items under listRef.
//                                Log.d("ITEM", "onSuccess: " + item);
//                                final long ONE_MEGABYTE = 1024 * 1024 *5;
//
//                                item.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
//                                    @Override
//                                    public void onSuccess(byte[] bytes) {
//                                        // Data for "images/island.jpg" is returns, use this as needed
//                                       File test = storeFile(bytes);
////                                       files.add(item.hashCode(), test);
//                                    }
//                                }).addOnFailureListener(new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception exception) {
//                                        // Handle any errors
//                                    }
//                                });
//
//
//                            }
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            // Uh-oh, an error occurred!
//                            Log.d("ITEM", "onFailure: " + e);
//                        }
//                    });
//

            AssetManager assetManager = getResources().getAssets();
            String[] books = assetManager.list("");
            int i = 0;
            for(String book : books) {
                if(book.endsWith(".epub")) {
                    files.add(i++, getFileFromAssets(book));
                }
            }

//            File sampleFile = getFileFromAssets("pg28885-images_new.epub");
//            File gas_oil = getFileFromAssets("Gas_and_Oil_Engines,_Simply_Explained_by_Walter_C._Runciman.epub");
//            File opportunities_oil = getFileFromAssets("Opportunities_in_Engineering_by_Charles_M._Horton.epub");
//
//            files.add(0, sampleFile);
//            files.add(1, gas_oil);
//            files.add(2, opportunities_oil);


            for (File file : files) {
                BookInfo bookInfo = new BookInfo();
                bookInfo.setTitle(file.getName());
                bookInfo.setFilePath(file.getPath());

                bookInfoList.add(bookInfo);
            }
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
        Log.d("TEST", "getFileFromAssets: " + file);
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

    private void askForWidgetToUse(final String filePath) {
        Log.d("BEFORE", "askForWidgetToUse: ");
        final Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("filePath", filePath);
        Log.d("FilePath in Menu",filePath);
        Log.d("AFTER", "askForWidgetToUse: ");
        new AlertDialog.Builder(getActivity())
                .setTitle("Pick your widget")
                .setMessage("Textview or WebView?")
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == -1 && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d("textValue",result.get(0));
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

}
