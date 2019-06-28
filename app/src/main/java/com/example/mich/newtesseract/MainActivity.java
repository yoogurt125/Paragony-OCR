package com.example.mich.newtesseract;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button buttonSuma;
    TextView textView;
    ProgressBar progressBar;
    String dataPath;
    String decodedOcr;
    TessBaseAPI tessBaseAPI;
    private Button btn;
    private ImageView imageview;
    private int GALLERY = 1;
    private Bitmap bitmap;
    TextView currentBalance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestMultiplePermissions();

        dataPath = "/storage/emulated/0/";
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(dataPath, "pol");

        progressBar = findViewById(R.id.progressbar);
        btn = findViewById(R.id.btn);
        buttonSuma = findViewById(R.id.btn_suma);
        imageview = findViewById(R.id.iv);
        textView = findViewById(R.id.textView);
        currentBalance = findViewById(R.id.current_balance);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePhotoFromGallary();

            }
        });
        buttonSuma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForPrice(decodedOcr);
            }
        });
    }


    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    Toast.makeText(MainActivity.this, "Zdjęcie załadowane!", Toast.LENGTH_SHORT).show();
                    imageview.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.VISIBLE);
                    textView.setText("Kliknij na paragon aby wydobyć tekst");
                    imageview.setImageBitmap(bitmap);


                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Niepowodzenie!", Toast.LENGTH_SHORT).show();
                }
            }

        }

        imageview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                tesseractDoItNow();
            }
        });

    }


    public void tesseractDoItNow() {


        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        new Thread(new Runnable() {
            @Override
            public void run() {
                tessBaseAPI.setImage(bitmap);
                decodedOcr = tessBaseAPI.getUTF8Text();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(decodedOcr);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();


        final File path = Environment.getExternalStorageDirectory();

        if (!path.exists()) {
            path.mkdir();
        }

        final File file = new File(path, "Tess_result.txt");

        try {
            file.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.append(decodedOcr);
            outputStreamWriter.close();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }

    void searchForPrice(String string){
        int idxStart, idxEnd;
        float currentBalanceF = Float.parseFloat( currentBalance.getText().toString());
        idxStart = string.lastIndexOf("SUMA PLN");
        String suma = string.substring(idxStart);
        idxEnd = suma.indexOf("\n");
        suma = suma.substring(8,idxEnd);
        suma = suma.replace(',','.');
        suma = suma.replaceAll("\\s+","");
        float sumaf = Float.parseFloat(suma);

        currentBalanceF = currentBalanceF + sumaf;
        String currentBalanceS = Float.toString(currentBalanceF);
        currentBalance.setText(currentBalanceS);
    }


    private void requestMultiplePermissions() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            Log.d("permissions", "all permissions granted");
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            //openSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }


}
