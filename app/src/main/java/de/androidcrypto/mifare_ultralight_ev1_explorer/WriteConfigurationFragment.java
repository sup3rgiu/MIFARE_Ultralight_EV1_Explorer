package de.androidcrypto.mifare_ultralight_ev1_explorer;

import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.authenticateUltralightEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.customPack;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.customPassword;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.defaultPack;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.defaultPassword;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.identifyUltralightEv1Tag;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.identifyUltralightFamily;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.pagesToRead;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.writeAuth0UltralightEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.writePageMifareUltralightEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.writeProtUltralightEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.writePasswordPackUltralightEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.bytesToHexNpe;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.doVibrate;

import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteConfigurationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteConfigurationFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "ReadCounterFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public WriteConfigurationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReceiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WriteConfigurationFragment newInstance(String param1, String param2) {
        WriteConfigurationFragment fragment = new WriteConfigurationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private TextView readResult;
    private RadioButton rbNoAuth, rbDefaultAuth, rbCustomAuth;
    private RadioButton rbMemoryWriteProtection, rbMemoryWriteReadProtection;
    private RadioButton rbMemoryNoClearing, rbMemoryClearing;
    private RadioButton rbChangePasswordNone, rbChangePasswordDefault, rbChangePasswordCustom;
    private AutoCompleteTextView authenticationRequiredPage;
    private View loadingLayout;
    private String outputString = ""; // used for the UI output
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;
    private boolean isTagUltralight = false;
    private int storageSize = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this.getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        readResult = getView().findViewById(R.id.tvReadResult);
        rbNoAuth = getView().findViewById(R.id.rbNoAuth);
        rbDefaultAuth = getView().findViewById(R.id.rbDefaultAuth);
        rbCustomAuth = getView().findViewById(R.id.rbCustomAuth);
        rbMemoryWriteProtection = getView().findViewById(R.id.rbMemoryWriteProtection);
        rbMemoryWriteReadProtection = getView().findViewById(R.id.rbMemoryWriteReadProtection);
        rbMemoryNoClearing = getView().findViewById(R.id.rbMemoryNoClearing);
        rbMemoryClearing = getView().findViewById(R.id.rbMemoryClearing);
        rbChangePasswordNone = getView().findViewById(R.id.rbChangePasswordNone);
        rbChangePasswordDefault = getView().findViewById(R.id.rbChangePasswordDefault);
        rbChangePasswordCustom = getView().findViewById(R.id.rbChangePasswordCustom);
        loadingLayout = getView().findViewById(R.id.loading_layout);

        // page number from where an authentication is necessary
        String[] type = new String[]{"4", "6", "8", "12", "16", "255"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                getView().getContext(),
                R.layout.drop_down_item,
                type);
        authenticationRequiredPage = getView().findViewById(R.id.authRequiredPage);
        authenticationRequiredPage.setText(type[5]);
        authenticationRequiredPage.setAdapter(arrayAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_write_configuration, container, false);
    }

    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an NcfA Technology Type

        boolean success;
        boolean authSuccess = false;

        Log.d(TAG, "NFC tag discovered");
        playSinglePing();
        setLoadingLayoutVisibility(true);
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            readResult.setText("");
            readResult.setBackgroundColor(getResources().getColor(R.color.white));
        });

        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception
        nfcA = NfcA.get(tag);

        if (nfcA == null) {
            writeToUiAppend("The tag is not readable with NfcA classes, sorry");
            writeToUiFinal(readResult);
            setLoadingLayoutVisibility(false);
            returnOnNotSuccess();
            return;
        }

        // get card details
        byte[] tagId = nfcA.getTag().getId();
        int maxTransceiveLength = nfcA.getMaxTransceiveLength();
        String[] techList = nfcA.getTag().getTechList();
        StringBuilder sb = new StringBuilder();
        sb.append("Technical Data of the Tag").append("\n");
        sb.append("Tag ID: ").append(bytesToHexNpe(tagId)).append("\n");
        sb.append("maxTransceiveLength: ").append(maxTransceiveLength).append(" bytes").append("\n");
        sb.append("Tech-List:").append("\n");
        sb.append("Tag TechList: ").append(Arrays.toString(techList)).append("\n");
        if (identifyUltralightFamily(nfcA)) {
            sb.append("The Tag seems to be a MIFARE Ultralight Family tag").append("\n");
            isTagUltralight = true;
        } else {
            sb.append("The Tag IS NOT a MIFARE Ultralight tag").append("\n");
            sb.append("** End of Processing **").append("\n");
            isTagUltralight = false;
        }
        writeToUiAppend(sb.toString());

        // stop processing if not an Ultralight Family tag
        if (!isTagUltralight) {
            returnOnNotSuccess();
            return;
        }

        try {
            nfcA.connect();

            if (nfcA.isConnected()) {
                // get the version
                storageSize = identifyUltralightEv1Tag(nfcA);
                sb = new StringBuilder();
                if (storageSize == 0) {
                    sb.append("The Tag IS NOT a MIFARE Ultralight EV1 tag").append("\n");
                    sb.append("** End of Processing **").append("\n");
                    isTagUltralight = false;
                } else if (storageSize == 48) {
                    sb.append("The Tag is a MIFARE Ultralight EV1 tag with 48 bytes user memory size").append("\n");
                    pagesToRead = 20;
                    isTagUltralight = true;
                } else if (storageSize == 128) {
                    sb.append("The Tag is a MIFARE Ultralight EV1 tag with 128 bytes user memory size").append("\n");
                    pagesToRead = 41;
                    isTagUltralight = true;
                } else {
                    sb.append("The Tag IS NOT a MIFARE Ultralight EV1 tag").append("\n");
                    sb.append("** End of Processing **").append("\n");
                    isTagUltralight = false;
                }
                writeToUiAppend(sb.toString());
                if (!isTagUltralight) {
                    returnOnNotSuccess();
                    return;
                }

                if (rbNoAuth.isChecked()) {
                    writeToUiAppend("No Authentication requested");
                    authSuccess = true;
                } else if (rbDefaultAuth.isChecked()) {
                    writeToUiAppend("Authentication with Default Password requested");
                    // authenticate with default password and pack
                    int authResult = authenticateUltralightEv1(nfcA, defaultPassword, defaultPack);
                    if (authResult == 1) {
                        writeToUiAppend("authentication with Default Password and Pack: SUCCESS");
                        authSuccess = true;
                    } else {
                        writeToUiAppend("authentication with Default Password and Pack: FAILURE " + authResult);
                        authSuccess = false;
                    }
                } else {
                    writeToUiAppend("Authentication with Custom Password requested");
                    // authenticate with custom password and pack
                    int authResult = authenticateUltralightEv1(nfcA, customPassword, customPack);
                    if (authResult == 1) {
                        writeToUiAppend("authentication with Custom Password and Pack: SUCCESS");
                        authSuccess = true;
                    } else {
                        writeToUiAppend("authentication with Custom Password and Pack: FAILURE " + authResult);
                        authSuccess = false;
                    }
                }

                if (!authSuccess) {
                    writeToUiAppend("The authentication was not successful, operation aborted.");
                    returnOnNotSuccess();
                    return;
                }

                // get page for memory protection start
                String choiceString = authenticationRequiredPage.getText().toString();
                byte defineAuth0Page = (byte) Integer.parseInt(choiceString);

                // write Auth0
                success = writeAuth0UltralightEv1(nfcA, defineAuth0Page);
                writeToUiAppend("Status of writeAuth0 command to page 32: " + success);

                // write Prot
                boolean defineWriteOnlyRestricted;
                if (rbMemoryWriteProtection.isChecked()) {
                    defineWriteOnlyRestricted = true;
                } else {
                    defineWriteOnlyRestricted = false;
                }
                success = writeProtUltralightEv1(nfcA, defineWriteOnlyRestricted);
                writeToUiAppend("Status of writeAuth1 command to WriteRestrictedOnly: " + success);

                // clearing of the free user memory
                if (rbMemoryNoClearing.isChecked()) {
                    writeToUiAppend("No Memory Clearing requested");
                } else {
                    writeToUiAppend("Memory Clearing requested");
                    // try to write to all pages in the range 04h .. 27h  (04d .. 39d)
                    byte[] emptyPage = new byte[4];
                    success = true; // to run the first write command
                    if (pagesToRead == 20) {
                        for (int i = 4; i <= 15; i++) {
                            if (success) success = writePageMifareUltralightEv1(nfcA, i, emptyPage);
                            writeToUiAppend("Writing to page " + i + ": " + success);
                        }
                    } else if (pagesToRead == 41) {
                        for (int i = 4; i <= 35; i++) {
                            if (success) success = writePageMifareUltralightEv1(nfcA, i, emptyPage);
                            writeToUiAppend("Writing to page " + i + ": " + success);
                        }
                    } else {
                        writeToUiAppend("Unknown tag size, writing aborted");
                        success = false;
                    }
                    if (success) writeToUiAppend("Memory Clearing done");
                }

                // change password - options are no change, change to default or change to custom
                if (rbChangePasswordNone.isChecked()) {
                    // no password change
                    writeToUiAppend("No password change requested");
                } else if (rbChangePasswordDefault.isChecked()) {
                    // change password & pack to default
                    writeToUiAppend("== Password & Pack change ==");
                    int passwordPackChangeSuccess = writePasswordPackUltralightEv1(nfcA, defaultPassword, defaultPack);
                    if (passwordPackChangeSuccess == 1) {
                        writeToUiAppend("writePasswordPackUltralightEv1 with Default Password and Pack: SUCCESS");
                    } else {
                        writeToUiAppend("writePasswordPackUltralightEv1 with Default Password and Pack: FAILURE " + passwordPackChangeSuccess);
                    }
                } else {
                    // change password & pack to custom
                    writeToUiAppend("== Password & Pack change ==");
                    int passwordPackChangeSuccess = writePasswordPackUltralightEv1(nfcA, customPassword, customPack);
                    if (passwordPackChangeSuccess == 1) {
                        writeToUiAppend("writePasswordPackUltralightEv1 with Custom Password and Pack: SUCCESS");
                    } else {
                        writeToUiAppend("writePasswordPackUltralightEv1 with Custom Password and Pack: FAILURE " + passwordPackChangeSuccess);
                    }
                }
            }
        } catch (Exception e) {
            writeToUiAppend("Exception on connection: " + e.getMessage());
            e.printStackTrace();
        }
        writeToUiFinal(readResult);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        reconnect(nfcA);
    }

    private void returnOnNotSuccess() {
        writeToUiAppend("=== Return on Not Success ===");
        writeToUiFinal(readResult);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        mNfcAdapter.disableReaderMode(this.getActivity());
    }

    private void reconnect(NfcA nfcA) {
        // this is just an advice - if an error occurs - close the connection and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            nfcA.close();
            Log.d(TAG, "Close NfcA");
        } catch (Exception e) {
            Log.e(TAG, "Exception on Close NfcA: " + e.getMessage());
        }
        try {
            Log.d(TAG, "Reconnect NfcA");
            nfcA.connect();
        } catch (Exception e) {
            Log.e(TAG, "Exception on Reconnect NfcA: " + e.getMessage());
        }
    }

    /**
     * Sound files downloaded from Material Design Sounds
     * https://m2.material.io/design/sound/sound-resources.html
     */
    private void playSinglePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_02);
        mp.start();
    }

    private void playDoublePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_01);
        mp.start();
    }

    private void writeToUiAppend(String message) {
        //System.out.println(message);
        outputString = outputString + message + "\n";
    }

    private void writeToUiFinal(final TextView textView) {
        if (textView == (TextView) readResult) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(outputString);
                    System.out.println(outputString); // print the data to console
                }
            });
        }
    }

    /**
     * shows a progress bar as long as the reading lasts
     *
     * @param isVisible
     */

    private void setLoadingLayoutVisibility(boolean isVisible) {
        getActivity().runOnUiThread(() -> {
            if (isVisible) {
                loadingLayout.setVisibility(View.VISIBLE);
            } else {
                loadingLayout.setVisibility(View.GONE);
            }
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(this.getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for NfcA types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag after reading
            mNfcAdapter.enableReaderMode(this.getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this.getActivity());
    }
}