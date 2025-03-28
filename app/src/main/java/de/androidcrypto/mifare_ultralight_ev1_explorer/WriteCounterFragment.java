package de.androidcrypto.mifare_ultralight_ev1_explorer;

import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.authenticateUltralightEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.customPack;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.customPassword;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.defaultPack;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.defaultPassword;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.identifyUltralightEv1Tag;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.identifyUltralightFamily;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.increaseCounterByValueEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.setCounterValueEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.pagesToRead;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.readCounterEv1;
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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteCounterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteCounterFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "WriteCounterFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private com.google.android.material.textfield.TextInputLayout counter0Layout;
    private com.google.android.material.textfield.TextInputEditText incrementCounter, counter0, resultNfcWriting;
    private RadioButton rbNoAuth, rbDefaultAuth, rbCustomAuth;
    private RadioButton incrementNoCounter, incrementCounter0, incrementCounter1, incrementCounter2, setCounter0, setCounter1, setCounter2;
    private EditText etIncreaseValue0, etIncreaseValue1, etIncreaseValue2, etSetValue0, etSetValue1, etSetValue2;
    private View loadingLayout;
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;
    private String outputString = ""; // used for the UI output
    private boolean isTagUltralight = false;
    private int storageSize = 0;

    public WriteCounterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WriteCounterFragment newInstance(String param1, String param2) {
        WriteCounterFragment fragment = new WriteCounterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_write_counter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        counter0 = getView().findViewById(R.id.etCounter0);
        rbNoAuth = getView().findViewById(R.id.rbNoAuth);
        rbDefaultAuth = getView().findViewById(R.id.rbDefaultAuth);
        rbCustomAuth = getView().findViewById(R.id.rbCustomAuth);
        incrementNoCounter = getView().findViewById(R.id.rbCounterNoIncrease);
        incrementCounter0 = getView().findViewById(R.id.rbIncreaseCounter0);
        incrementCounter1 = getView().findViewById(R.id.rbIncreaseCounter1);
        incrementCounter2 = getView().findViewById(R.id.rbIncreaseCounter2);
        etIncreaseValue0 = getView().findViewById(R.id.etIncreaseValue0);
        etIncreaseValue1 = getView().findViewById(R.id.etIncreaseValue1);
        etIncreaseValue2 = getView().findViewById(R.id.etIncreaseValue2);
        setCounter0 = getView().findViewById(R.id.rbSetCounter0);
        setCounter1 = getView().findViewById(R.id.rbSetCounter1);
        setCounter2 = getView().findViewById(R.id.rbSetCounter2);
        etSetValue0 = getView().findViewById(R.id.etSetValue0);
        etSetValue1 = getView().findViewById(R.id.etSetValue1);
        etSetValue2 = getView().findViewById(R.id.etSetValue2);

        resultNfcWriting = getView().findViewById(R.id.etReadResult);
        loadingLayout = getView().findViewById(R.id.loading_layout);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        // ## Manually enforce mutual exclusivity of radio buttons since we added EditText in the RadioGroup ##
        // Group the radio buttons logically
        final RadioButton[] counterRadioButtons = {incrementNoCounter, incrementCounter0, incrementCounter1, incrementCounter2, setCounter0, setCounter1, setCounter2};
        final EditText[] counterEditTexts = {etIncreaseValue0, etIncreaseValue1, etIncreaseValue2, etSetValue0, etSetValue1, etSetValue2};

        // Listener for each radio button
        View.OnClickListener radioListener = v -> {
            RadioButton checkedRadioButton = (RadioButton) v;
            // Uncheck all others in the logical group
            for (RadioButton rb : counterRadioButtons) {
                if (rb.getId() != checkedRadioButton.getId()) {
                    rb.setChecked(false);
                }
            }
        };

        // Assign the listener to each radio button
        for (RadioButton rb : counterRadioButtons) {
            rb.setOnClickListener(radioListener);
        }
    }

    /**
     * section for NFC
     */

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
            resultNfcWriting.setText("");
            resultNfcWriting.setBackgroundColor(getResources().getColor(R.color.white));
        });

        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception
        nfcA = NfcA.get(tag);

        if (nfcA == null) {
            writeToUiAppend("The tag is not readable with NfcA classes, sorry");
            writeToUiFinal(resultNfcWriting);
            setLoadingLayoutVisibility(false);
            returnOnNotSuccess();
            return;
        }

        //get card details
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

                if (incrementNoCounter.isChecked()) {
                    Log.d(TAG, "No counter should get increased");
                    writeToUiAppend("No counter should get increased");
                } else if (incrementCounter0.isChecked()) {
                    if (!authSuccess) {
                        writeToUiAppend("Previous Auth was not successful or not done, skipped");
                    } else {
                        int incrementValue = 1;
                        try {
                            incrementValue = Integer.parseInt(etIncreaseValue0.getText().toString());
                        } catch (NumberFormatException e) {
                            writeToUiAppend("Error parsing increment value, using 1 as default");
                        }
                        success = increaseCounterByValueEv1(nfcA, 0, incrementValue);
                        writeToUiAppend("Status of increasing counter 0 by " + incrementValue + ": " + success);
                    }
                } else if (incrementCounter1.isChecked()) {
                    if (!authSuccess) {
                        writeToUiAppend("Previous Auth was not successful or not done, skipped");
                    } else {
                        int incrementValue = 1;
                        try {
                            incrementValue = Integer.parseInt(etIncreaseValue1.getText().toString());
                        } catch (NumberFormatException e) {
                            writeToUiAppend("Error parsing increment value, using 1 as default");
                        }
                        success = increaseCounterByValueEv1(nfcA, 1, incrementValue);
                        writeToUiAppend("Status of increasing counter 1 by " + incrementValue + ": " + success);
                    }
                } else if (incrementCounter2.isChecked()) {
                    if (!authSuccess) {
                        writeToUiAppend("Previous Auth was not successful or not done, skipped");
                    } else {
                        int incrementValue = 1;
                        try {
                            incrementValue = Integer.parseInt(etIncreaseValue2.getText().toString());
                        } catch (NumberFormatException e) {
                            writeToUiAppend("Error parsing increment value, using 1 as default");
                        }
                        success = increaseCounterByValueEv1(nfcA, 2, incrementValue);
                        writeToUiAppend("Status of increasing counter 2 by " + incrementValue + ": " + success);
                    }
                } else if (setCounter0.isChecked()) {
                    if (!authSuccess) {
                        writeToUiAppend("Previous Auth was not successful or not done, skipped");
                    } else {
                        int setValue = 0;
                        try {
                            setValue = Integer.parseInt(etSetValue0.getText().toString());
                        } catch (NumberFormatException e) {
                            writeToUiAppend("Error parsing set value, using 0 as default");
                        }
                        success = setCounterValueEv1(nfcA, 0, setValue);
                        writeToUiAppend("Status of setting counter 0 to " + setValue + ": " + success);
                    }
                } else if (setCounter1.isChecked()) {
                    if (!authSuccess) {
                        writeToUiAppend("Previous Auth was not successful or not done, skipped");
                    } else {
                        int setValue = 0;
                        try {
                            setValue = Integer.parseInt(etSetValue1.getText().toString());
                        } catch (NumberFormatException e) {
                            writeToUiAppend("Error parsing set value, using 0 as default");
                        }
                        success = setCounterValueEv1(nfcA, 1, setValue);
                        writeToUiAppend("Status of setting counter 1 to " + setValue + ": " + success);
                    }
                } else if (setCounter2.isChecked()) {
                    if (!authSuccess) {
                        writeToUiAppend("Previous Auth was not successful or not done, skipped");
                    } else {
                        int setValue = 0;
                        try {
                            setValue = Integer.parseInt(etSetValue2.getText().toString());
                        } catch (NumberFormatException e) {
                            writeToUiAppend("Error parsing set value, using 0 as default");
                        }
                        success = setCounterValueEv1(nfcA, 2, setValue);
                        writeToUiAppend("Status of setting counter 2 to " + setValue + ": " + success);
                    }
                }
                int counter0I = readCounterEv1(nfcA, 0);
                int counter1I = readCounterEv1(nfcA, 1);
                int counter2I = readCounterEv1(nfcA, 2);
                writeToUiAppend("Counter 0: " + counter0I);
                writeToUiAppend("Counter 1: " + counter1I);
                writeToUiAppend("Counter 2: " + counter2I);
                writeCounterToUi(counter0I, counter1I, counter2I);
            }

        } catch (
                Exception e) {
            writeToUiAppend("Exception on connection: " + e.getMessage());
            e.printStackTrace();
        }

        writeToUiFinal(resultNfcWriting);

        playDoublePing();

        setLoadingLayoutVisibility(false);

        doVibrate(getActivity());

        reconnect(nfcA);

    }

    private void returnOnNotSuccess() {
        writeToUiAppend("=== Return on Not Success ===");
        writeToUiFinal(resultNfcWriting);
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

    private void writeCounterToUi(final int counter0I, final int counter1I, final int counter2I) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                counter0.setText(String.valueOf(counter0I) + " / " + counter1I + " / " + counter2I);
            }
        });
    }

    private void writeToUiAppend(String message) {
        //System.out.println(message);
        outputString = outputString + message + "\n";
    }

    private void writeToUiFinal(final TextView textView) {
        if (textView == (TextView) resultNfcWriting) {
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

    private void showMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
            resultNfcWriting.setText(message);
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(getView().getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
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
            mNfcAdapter.enableReaderMode(getActivity(),
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
            mNfcAdapter.disableReaderMode(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}