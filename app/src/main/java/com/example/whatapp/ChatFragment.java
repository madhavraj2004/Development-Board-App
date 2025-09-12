package com.example.whatapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatBluetooth";
    private static final UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final int CHUNK_SIZE = 125; // MTU–3

    private RxBleClient rxBleClient;
    private RxBleConnection connection;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private TextView statusTextView;
    private EditText messageEditText;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final ArrayList<ChatMessage> chatMessages = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private final Map<String, RxBleDevice> discovered = new HashMap<>();
    private final ArrayList<String> deviceNamesList = new ArrayList<>();
    private RxBleDevice selectedDevice;

    // Buffer for incoming RX fragments
    private final ByteArrayOutputStream incomingBuffer = new ByteArrayOutputStream();

    // permissions
    private final ActivityResultLauncher<String[]> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean all = true;
                for (Boolean g : result.values())
                    if (!g) {
                        all = false;
                        break;
                    }
                if (all) startScan();
                else statusTextView.setText("Permissions denied");
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        ImageButton menuButton = v.findViewById(R.id.menuButton);

        menuButton.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(requireContext(), menuButton);
            popup.getMenuInflater().inflate(R.menu.chatfragment_options_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_scan) {
                    startScan();
                    return true;
                } else if (id == R.id.action_connect) {
                    showDeviceDialog();
                    return true;
//                } else if (id == R.id.action_disconnect) {
//                    disconnect();
//                    return true;
                } else {
                    return false;
                }
            });

            popup.show();
        });

        statusTextView = v.findViewById(R.id.statusTextView);
        messageEditText = v.findViewById(R.id.messageEditText);
        chatRecyclerView = v.findViewById(R.id.chatRecyclerView);
        ImageButton sendBtn = v.findViewById(R.id.sendButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);
        rxBleClient = RxBleClient.create(requireContext());

        sendBtn.setOnClickListener(x -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                sendMessageWithLocation();
            } else {
                permLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        return v;
    }

    private void startScan() {
        statusTextView.setText("Scanning…");
        discovered.clear();
        deviceNamesList.clear();

        Disposable d = rxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(scanResult -> {
                    RxBleDevice dev = scanResult.getBleDevice();
                    String mac = dev.getMacAddress();
                    String name = dev.getName() != null ? dev.getName() : mac;
                    if (!discovered.containsKey(mac)) {
                        discovered.put(mac, dev);
                        deviceNamesList.add(name + " (" + mac + ")");
                        statusTextView.setText("Found " + discovered.size());
                    }
                }, t -> {
                    statusTextView.setText("Scan failed");
                    Log.e(TAG, "scan", t);
                });
        disposables.add(d);
    }

    private void showDeviceDialog() {
        if (discovered.isEmpty()) {
            statusTextView.setText("No devices");
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select device")
                .setItems(deviceNamesList.toArray(new String[0]), (dlg, which) -> {
                    String pick = deviceNamesList.get(which);
                    String mac = pick.substring(pick.indexOf('(') + 1, pick.indexOf(')'));
                    selectedDevice = discovered.get(mac);
                    statusTextView.setText("Selected " + pick);
                    connect();
                }).show();
    }

    private void connect() {
        if (selectedDevice == null) return;
        Disposable d = selectedDevice.establishConnection(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(conn -> {
                    connection = conn;
                    statusTextView.setText("Connected");
                    subscribeNotifications();
                }, t -> {
                    statusTextView.setText("Connect failed");
                    Log.e(TAG, "conn", t);
                });
        disposables.add(d);
    }

    private void subscribeNotifications() {
        Disposable d = connection
                .setupNotification(TX_CHAR_UUID)
                .flatMap(obs -> obs)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bytes -> {
                    incomingBuffer.write(bytes, 0, bytes.length);
                    String currentData = new String(incomingBuffer.toByteArray(), StandardCharsets.UTF_8);

                    if (isCompleteJson(currentData)) {
                        addChatMessage("RX: " + currentData, false, "Device", "DEVICE");
                        incomingBuffer.reset();
                    }
                }, t -> Log.e(TAG, "notif", t));
        disposables.add(d);
    }

    private boolean isCompleteJson(String data) {
        int braceCount = 0;
        for (char c : data.toCharArray()) {
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
        }
        return braceCount == 0 && data.trim().endsWith("}");
    }

    private void disconnectDevice() {
        disposables.clear();
        connection = null;
        statusTextView.setText("Disconnected");
    }

    private void sendMessageWithLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    String text = messageEditText.getText().toString().trim();
                    String msg = text;
                    if (location != null) {
                        msg += "\nLocation: https://www.openstreetmap.org/?mlat=" +
                                location.getLatitude() + "&mlon=" + location.getLongitude();
                    }
                    splitAndSend(msg);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Could not fetch location", Toast.LENGTH_SHORT).show());
    }

    private void splitAndSend(String msg) {
        if (connection == null) {
            Toast.makeText(requireContext(), "Not connected to any device", Toast.LENGTH_SHORT).show();
            return;
        }
        addChatMessage("TX: " + msg, true, "Me", "SELF");
        messageEditText.setText("");
        // sending as chunks
        Disposable d = connection
                .requestMtu(128)
                .flatMapPublisher(mtu -> {
                    byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                    List<byte[]> chunks = new ArrayList<>();
                    for (int i = 0; i < data.length; i += CHUNK_SIZE) {
                        int end = Math.min(data.length, i + CHUNK_SIZE);
                        chunks.add(Arrays.copyOfRange(data, i, end));
                    }
                    return Flowable.fromIterable(chunks);
                })
                .concatMap(chunk -> connection.writeCharacteristic(RX_CHAR_UUID, chunk).toFlowable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bytes -> Log.d(TAG, "chunk sent, size=" + bytes.length),
                        t -> Log.e(TAG, "send failed", t));
        disposables.add(d);
    }

    private void addChatMessage(String text, boolean isSent, String senderName, String senderMac) {
        chatMessages.add(new ChatMessage(text, isSent, senderName, senderMac, false, ""));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}
