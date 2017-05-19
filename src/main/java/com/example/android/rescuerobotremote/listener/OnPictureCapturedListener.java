package com.example.android.rescuerobotremote.listener;

import java.util.TreeMap;

public interface OnPictureCapturedListener {

    void onCaptureDone(String pictureUrl, byte[] pictureData);

    void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken);
  
}
