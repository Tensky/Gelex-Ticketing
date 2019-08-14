package com.interpixel.gelex;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.ToneGenerator;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HAHA" ;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private CaptureRequest.Builder captureRequestBuilder;
    private FirebaseVisionTextRecognizer detector;
    private Handler handler = new Handler();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView textView;
    private String lastNama = "";
    private String lastNim = "";
    private ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    private NfcAdapter nfcAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        textView = findViewById(R.id.textView);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        findViewById(R.id.cameraButton).setOnClickListener(view -> openCamera());
    }

    private void enableNfcAdapter(){
        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 500);
        options.putString(NfcAdapter.EXTRA_NDEF_MESSAGES, "");
        nfcAdapter.enableReaderMode(this, nfcReaderCallback, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, options);
    }

    private final NfcAdapter.ReaderCallback nfcReaderCallback = tag -> {
        Log.d(TAG, "TAGnya:" + tag);
    };

    //Camera State Callback
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice c) {
            Log.d(TAG, "onOpened: BERHASIL");
            cameraDevice =  c;
            handler.post(() -> takePicture());
        }

        @Override
        public void onDisconnected(CameraDevice c) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice c, int i) {

        }
    };

    //UselessShit
    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    //Getting Image
    private ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image resultImage = imageReader.acquireLatestImage();
            FirebaseVisionImage visionImage = FirebaseVisionImage.fromMediaImage(resultImage, FirebaseVisionImageMetadata.ROTATION_0);
            textView.setText("");
            detector.processImage(visionImage).addOnCompleteListener(task -> {
                if(!task.isSuccessful()) return;
                FirebaseVisionText visionText = task.getResult();
                Log.d("HMM", visionText.getText());
                for (FirebaseVisionText.TextBlock textBlock : visionText.getTextBlocks()){
                    Log.d("HMM", "BLOCK:" + textBlock.getText());
                    if(textBlock.getText().matches(".*\\s\\d{2}/\\d{6}/[A-Z]{2}/\\d{5}\\s.*")){
                        String source = textBlock.getText();
                        String nama = source.substring(0, source.indexOf("/") - 2);
                        String nim = source.substring(source.indexOf("/") - 2, source.lastIndexOf("/") + 6);
                        textView.setText(nama + "\n" + nim);
                        if(nama.matches(".*\\d+.*")) break;
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
                        if(lastNama.equals(nama) || lastNim.equals(nim)) break;
                        lastNama = nama;
                        lastNim = nim;
                        Map<String, Object> data = new HashMap<>();
                        data.put("nama", nama);
                        data.put("nim", nim);
                        data.put("waktu", FieldValue.serverTimestamp());
                        db.collection("gelex").add(data);
                        break;
                    }
                }
                handler.postDelayed(() -> takePicture(), 500);
            });
        }
    };


    //Self explanatory
    private void openCamera() {
        //Try to open the camera
        Log.d("TAG", "openCamera");
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
                return;
            }
            cameraManager.openCamera("0", stateCallback, null);
        }catch (Exception e){
            Log.d(TAG, "openCamera: GAGAL" + e.toString());
        }
    }



    private void takePicture(){
        if(cameraDevice == null){
            return;
        }

        //Try to take picture
        try{
            //Setting up imageReader
            imageReaderSettings();

            //Create captureRequest Builder
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());

            //Making Surface for ImageReader
            List<Surface> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(imageReader.getSurface());

            //Create Capture Session
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, new Handler());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "onConfigureFailed: ");
                }
            }, null);

        }catch (Exception e){
            Log.d(TAG, "takePicture: " + e);
        }
    }

    private void imageReaderSettings(){
        CameraCharacteristics cameraCharacteristics = null;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Size[] size = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
        int width = size[0].getWidth();
        int height = size[0].getHeight();
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(imageAvailableListener, null);
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(NfcAdapter.getDefaultAdapter(this) != null) enableNfcAdapter();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraDevice != null)
        cameraDevice.close();
        nfcAdapter.disableForegroundDispatch(this);
    }
}
