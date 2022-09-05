package com.faptastic.webcamx.utils

import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URL


object Upload {
    
    private fun internetAvailable(): Boolean {
        try {
            InetAddress.getByName("google.com")
         } catch (e: Exception) {
            return false
        }
        return true
    }

    fun uploadJPEG(dest:String, data:ByteArrayOutputStream): Boolean
    {
        //val sourceFilePath = strings[0]
        //var destURL = strings[1]
        var destURL = dest

        // Setup HTTPS connection
        //destURL = destURL.replace("http://", "https://");
        //if (!destURL.startsWith("https://")) destURL = "https://" + destURL;
        //destURL = destURL.replace("https://", "http://")
        //if (!destURL.startsWith("http://")) destURL = "http://$destURL"
        if (!internetAvailable()) {
            Log.e("UploadTask", "Internet isn't available. Cancelling upload.")
            return false
        }
        val lineEnd = "\r\n"
        val twoHyphens = "--"
        val boundary = "*****"


        try {

            // Setup HRRPs
            val url = URL(destURL)
            //HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.requestMethod = "POST"
            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("ENCTYPE", "multipart/form-data")
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")
            conn.setRequestProperty("uploaded_file", "upload.jpg")
            val dos = DataOutputStream(conn.outputStream)
            val fileName = "upload.jpg"
            dos.writeBytes(twoHyphens + boundary + lineEnd)
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"$fileName\"$lineEnd")
            dos.writeBytes(lineEnd)

            // Write image data / binary
            data.writeTo(dos)

            dos.writeBytes(lineEnd)
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

            // Get Server response
            val serverResponseCode = conn.responseCode
            //val serverResponseMessage = conn.responseMessage
            val br = BufferedReader(InputStreamReader(conn.inputStream) as Reader)

            // Get response
            val output = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                output.append(line)
            }
            Log.i("UploadTask", "HTTP Response is: '$output': $serverResponseCode")
            if (serverResponseCode != 200) {
                Log.e("UploadTask", "Failed to upload. Server Error.")
            }

            //close the streams //
            br.close()
            dos.flush()
            dos.close()
            conn.disconnect()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            Log.e("UploadTask", "Bad URL : " + e.message)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UploadTask", "Exception : " + e.message)
        }
        return true
    }
}
