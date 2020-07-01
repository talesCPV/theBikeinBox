package thebikeinbox.com.br;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class GPS_Screen extends AppCompatActivity {

    Button btnVolta, btnStart, btnSalvar, btnReset;
    TextView lblHora, lblTimer, lblAlt, lblDist, lblAltAcum, lblVel, lblVelMed;
    Thread thread;
    int Dia,Mes,Ano,H,M,S,cont, tempoVel;
    Double dist, distVel, altAcum, vel, velmed, tempoTot ;
    Double lastAlt, lastLat, lastLon, nextAlt, nextLat, nextLon;
    String GPX, hora_atual;
    boolean rec;
    private GPS myService;
    private boolean isServiceBound;
    private ServiceConnection serviceConnection;
    private Intent serviceIntent;

    final String ARQUIVO = "training.GPX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_g_p_s__screen);
        serviceIntent = new Intent(this, GPS.class);
        btnVolta = findViewById(R.id.btnFechar);
        btnStart = findViewById(R.id.btnStart);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnReset = findViewById(R.id.btnResetar);
        lblHora = findViewById(R.id.lblHora);
        lblTimer = findViewById(R.id.lblTempo);
        lblAlt = findViewById(R.id.lblAlt);
        lblDist = findViewById(R.id.lblDist);
        lblAltAcum = findViewById(R.id.lblAcum);
        lblVel = findViewById(R.id.lblVel);
        lblVelMed = findViewById(R.id.lblVelMed);
        ResetTrain();
        btnStart.setText("START");

        btnVolta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(GPS_Screen.this, MainActivity.class));
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ResetTrain();
            }
        });

        btnSalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SalvarGPX();
            }
        });

        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!thread.isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                HoraAtual();
                                Timer();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

    }


    // TEXTO *************************************************

    public void SalvarGPX() {
        try {
            FileOutputStream out = openFileOutput(ARQUIVO, MODE_APPEND);
            out.write(GPX.getBytes());
            out.close();
            UploadGPX();
        } catch (Exception e) {
            Log.e("ERRO", e.getMessage());
        }
    }

    // FIM TEXTO*********************************************
    // Bind Service*********************************************

    private void bindService(){
        if(serviceConnection == null){
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    System.out.println("binding...");
                    GPS.MyServiceBinder myServiceBinder = (GPS.MyServiceBinder)iBinder;
                    myService=myServiceBinder.getService();
                    isServiceBound =true;
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    isServiceBound =false;
                }
            };
        }
        bindService(serviceIntent,serviceConnection,Context.BIND_AUTO_CREATE);
    }


    private void unbindService(){
        if(isServiceBound){
            unbindService(serviceConnection);
            isServiceBound=false;
        }
    }


    private void calcData(int i) {
        if (rec) {
            getLatLon();
            if (lastLat != 0 && lastLat != nextLat && lastLon != nextLon) {
                Double dist_points = haversine(lastLat, lastLon, nextLat, nextLon);
                dist += dist_points;
                distVel += dist_points;
                Double med = dist / (H + M / 60 + S / 3600);
                if (med > 0.1 && med < 210.00) {
                    velmed = med;
                }

                if (nextAlt > lastAlt) { // Alt Acumulada
                    altAcum += (nextAlt - lastAlt);
                }

                if (cont >= i) {  // Velocidade
                    vel = distVel / i / 3600;
                    distVel = 0.00;
                    cont = 0;
                    if (vel > 0.1  && vel < 210.00 ) {
                        lblVel.setText(String.format("%.2f", vel));
                    }
                }

                lblDist.setText(String.format("%.2f", dist));
                lblVelMed.setText(String.format("%.2f", velmed));
                lblAlt.setText(String.format("%.2f", nextAlt));
                lblAltAcum.setText(String.format("%.2f", altAcum));
                AddPoint();
            }
            lastAlt = nextAlt;
            lastLat = nextLat;
            lastLon = nextLon;
        }
    }

    private void getLatLon(){
        if(isServiceBound){
            nextLat =  myService.lat_ini;
            nextLon = myService.lon_ini;
            nextAlt = myService.alt_ini;
//            System.out.println( lastLat.toString()+"|");
        }else{
            System.out.println("Service not Bound");
        }
    }
    // FIM BindService*********************************************

    public void StartTrain(View view){
        if(btnStart.getText().toString() == "START"){
            startService(serviceIntent);
            thread.start();
            btnStart.setText("PAUSE");
            btnSalvar.setEnabled(false);
            btnReset.setEnabled(false);
            btnVolta.setEnabled(false);
            rec = true;
        }else{
            stopService(serviceIntent);
//            unbindService();
            thread.interrupt();
            btnStart.setText("START");
            btnSalvar.setEnabled(true);
            btnReset.setEnabled(true);
            btnVolta.setEnabled(true);
            rec = false;
            GPX += "  </trkseg>\n" +
                    " </trk>\n" +
                    "</gpx>";
        }
    }

    public void AddPoint(){
        if(GPX.equals("")) { //CABEÇALHO
            GPX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<gpx creator=\"theBikeinBox_App\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\" version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\">\n" +
                    " <metadata>\n" +
                    "  <time>" + Ano + "-" + String.format("%02d",Mes) + "-" + String.format("%02d",Dia) + "T" + hora_atual + "Z</time>\n" +
                    " </metadata>\n" +
                    " <trk>\n" +
                    "  <name>" + String.format("%02d",Dia) + "/" + String.format("%02d",Mes) + "/" + Ano + " - " + hora_atual + " - Ride </name>\n" +
                    "  <type>1</type>\n" +
                    "  <trkseg>\n";
        }

        GPX += "   <trkpt lat=\"" + nextLat + "\" lon=\"" + nextLon + "\">\n" +
                "    <ele>" + nextAlt + "</ele>\n" +
                "    <time>" + Ano + "-" + String.format("%02d",Mes) + "-" + String.format("%02d",Dia) + "T" + hora_atual + "Z</time>\n" +
                "    <extensions>\n" +
                "     <gpxtpx:TrackPointExtension>\n" +
                "      <gpxtpx:atemp>20</gpxtpx:atemp>\n" +
                "     </gpxtpx:TrackPointExtension>\n" +
                "    </extensions>\n" +
                "   </trkpt>\n";
    }

    public void ResetTrain(){
        GPX = "";
        H = 0;
        M = 0;
        S = 0;
        cont = 0;
        tempoVel = 0;
        dist = 0.0;
        distVel = 0.00;
        lastAlt = 0.00;
        lastLat = 0.0;
        lastLon = 0.0;
        nextAlt = 0.00;
        nextLat = 0.00;
        nextLon = 0.00;
        altAcum = 0.00;
        vel = 0.00;
        velmed = 0.00;
        tempoTot = 0.00;
        lblDist.setText("0.00");
        lblTimer.setText("00:00:00");
        lblAltAcum.setText("0.00");
        deleteFile(ARQUIVO);
        rec = false;
    }

    public void HoraAtual(){
        SimpleDateFormat dateFormat_hora = new SimpleDateFormat("HH:mm:ss");
        Date data = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(data);
        Date data_atual = cal.getTime();
        Ano = cal.get(Calendar.YEAR);
        Mes = cal.get(Calendar.MONTH);
        Dia = cal.get(Calendar.DAY_OF_MONTH);
        hora_atual = dateFormat_hora.format(data_atual);
        lblHora.setText(hora_atual);
    }

    public void Timer(){
        if(H==0 && M==0 && S==0){bindService();} // liga o bindService pela 1 vez
        cont ++;
        S ++;
        tempoVel ++;
        if(S>=60){
            bindService(); // reativa o bindService a cada min
            S=0;
            M++;
            if(M>=60){
                M=0;
                H++;
            }
        }
        lblTimer.setText(String.format("%02d",H)+":"+String.format("%02d",M)+":"+String.format("%02d",S));
        tempoTot = new Double(H) + new Double(M)/60 +  new Double(S)/3600;
        calcData(5); // i = tempo em seg para atualização do calculo de velocidade
    }

    private Double toRad(Double value){
        /** Converts numeric degrees to radians */
        return value * Math.PI / 180;
    }

    public Double haversine(Double lat1, Double lon1, Double lat2, Double lon2){

        int R = 6371; // km
        Double dLat = toRad(lat2-lat1);
        Double dLon = toRad(lon2-lon1);
        Double a = Math.sin(dLat/2) * Math.sin(dLat/2) +  Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *  Math.sin(dLon/2) * Math.sin(dLon/2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        Double d = R * c;

        return(d) ;
    }

    public void UploadGPX(){
        new UploadFileAsync().execute("");
    }

    private class UploadFileAsync extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            System.out.println("entrou");
            try {
                String sourceFileUri = ARQUIVO;

                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile = getFileStreamPath(sourceFileUri);
                if (sourceFile.isFile()) {
                    System.out.println("é file");

                    try {
                        String upLoadServerUri = "http://www.thebikeinbox.com.br/maps/config/getAppFile.php?user=tales";
                        System.out.println("URL ");

                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(
                                sourceFile);
                        URL url = new URL(upLoadServerUri);

                        // Open a HTTP connection to the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE",
                                "multipart/form-data");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("arquivo", sourceFileUri);
                        System.out.println("Connection ");

                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"arquivo\";filename=\""
                                + sourceFileUri + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);
                        System.out.println("dos ok ");

                        // create a buffer of maximum size
                        bytesAvailable = fileInputStream.available();

                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        // read file and write it into form...
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {
                            System.out.println("while...");

                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math
                                    .min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0,
                                    bufferSize);

                        }

                        // send multipart form data necesssary after file
                        // data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens
                                + lineEnd);

                        // Responses from the server (code and message)
                        int serverResponseCode = conn.getResponseCode();
                        String serverResponseMessage = conn
                                .getResponseMessage();

                        if (serverResponseCode == 200) {
                            System.out.println("terminou o upload");
                        }

                        // close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();

                    } catch (Exception e) {
                        // dialog.dismiss();
                        e.printStackTrace();
                        System.out.println("exception A");

                    }
                    // dialog.dismiss();
                }

            } catch (Exception ex) {
                // dialog.dismiss();
                ex.printStackTrace();
                System.out.println("exception B");

            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


}