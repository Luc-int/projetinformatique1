package com.example.lucgibaud.projetinformatique1;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class Lire extends AppCompatActivity {

    private TextView mTextView;
    private TextView idTextView;
    private TextView tnfTextView;
    private TextView typeTextView;
    private TextView messageTextView;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    IntentFilter[] mFilters;
    String[][] mTechLists = new String[][]{new String[]{NfcA.class.getName()}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lire);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bouton servant à retourner sur la page principale

        Button retour_menu = (Button) findViewById(R.id.retour_menu);
        retour_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent int1 = new Intent(Lire.this, Main.class);
                startActivity(int1);
            }
        });


        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mTextView = (TextView) findViewById(R.id.textView_lire_nfc);
        idTextView = (TextView) findViewById(R.id.textView_lire_nfc_id);
        tnfTextView = (TextView) findViewById(R.id.textView_lire_nfc_tnf);
        typeTextView = (TextView) findViewById(R.id.textView_lire_nfc_type);
        messageTextView = (TextView) findViewById(R.id.textView_lire_nfc_message);


/*
Vérifie que la NFC est disponible sur l'appareil et, le cas échéant, activée
 */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            Toast.makeText(this, "La NFC n'est malheureusement pas disponible sur votre appareil ...", Toast.LENGTH_LONG).show();
            finish();
            return;
        } else if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "La NFC est désactivée", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        IntentFilter ndef1 = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        mFilters = new IntentFilter[]{
                ndef1,
        };
        if (getIntent() != null) {
            resolveIntent(getIntent());
        }

    }

/*
Pour l'instant l'application ne reconnait que le premier message d'une carte (dans le cas ou il en y aurait plusieurs)
 */

    void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] id = tagFromIntent.getId();
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] messages;
            if (rawMsgs != null) {
                messages = new NdefMessage[rawMsgs.length];
                messages[0] = (NdefMessage) rawMsgs[0];
                NdefRecord record = messages[0].getRecords()[0];
                short tnf = record.getTnf();
                byte[] type = record.getType();
                try {
                    String message = readText(record);
                    actualisation(id, tnf, type, message);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                actualisation(id);
            }
        }
    }

    /**
     * readText a pour objectif renvoyer le message (en String) si il y en a un sur la carte
     * <p/>
     * Source : Cette fonction n'a pas été codée mais prise sur internet (http://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278)
     */
    private String readText(NdefRecord record) throws UnsupportedEncodingException {
        byte[] payload = record.getPayload();
        String textEncoding;
        if ((payload[0] & 128) == 0) textEncoding = "UTF-8";
        else textEncoding = "UTF-16";
        int languageCodeLength = payload[0] & 0063;
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    /**
     * convertByteArrayToHexString a pour objectif de convertir un byte[] en hexadécimal
     * <p/>
     * Source : Cette fonction n'a pas été codée mais prise sur internet (http://stackoverflow.com/questions/26326952/id-on-nfc-tag-not-unique)
     */
    public static String convertByteArrayToHexString(byte[] b) {
        if (b != null) {
            StringBuilder s = new StringBuilder(2 * b.length);
            for (int i = 0; i < b.length; ++i) {
                final String t = Integer.toHexString(b[i]);
                final int l = t.length();
                if (l > 2) {
                    s.append(t.substring(l - 2));
                } else {
                    if (l == 1) {
                        s.append("0");
                    }
                    s.append(t);
                }
            }
            return s.toString();
        } else {
            return "";
        }
    }

    /*
    Fonction servant à actualiser le texte
     */

    private void actualisation(byte[] id) {
        String hexa_id = convertByteArrayToHexString(id);
        mTextView.setText("Tag (sans message lisible) reconnu !");
        idTextView.setText("ID : " + hexa_id);
        tnfTextView.setText("");
        typeTextView.setText("");
        messageTextView.setText("");

    }

    private void actualisation(byte[] id, short tnf, byte[] type, String message) {
        String hexa_id = convertByteArrayToHexString(id);
        String hexa_type = convertByteArrayToHexString(type);
        mTextView.setText("Tag contenant un message reconnu !");
        idTextView.setText("ID : " + hexa_id);
        tnfTextView.setText("TNF : " + tnf);
        typeTextView.setText("Type : " + hexa_type);
        messageTextView.setText("Message :" + "\n" + message);

    }


    @Override
    public void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
        resolveIntent(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

}
