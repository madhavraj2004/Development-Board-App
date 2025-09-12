package com.example.whatapp.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whatapp.ChatFragment;
import com.example.whatapp.ChatsModel;
import com.example.whatapp.R;
import com.example.whatapp.RecyclerViewAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements RecyclerViewAdapter.ChatsFragmentListener {

    private RecyclerView mRecyclerView;
    private FloatingActionButton fab;
    private RecyclerViewAdapter adapter;

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mRecyclerView = view.findViewById(R.id.usersRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fab = view.findViewById(R.id.fab);

        List<ChatsModel> chatList = loadChatsFromPrefs();
        adapter = new RecyclerViewAdapter(getContext(), chatList, this);
        mRecyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> showFabOptionsDialog());

        return view;
    }

    private void showFabOptionsDialog() {
        String[] options = { "Add New Contact", "Create New Group" };

        new AlertDialog.Builder(requireContext())
                .setTitle("Choose an action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddUserDialog();
                    } else {
                        showCreateGroupDialog();
                    }
                }).show();
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Contact");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        EditText nameInput = dialogView.findViewById(R.id.editName);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveButton.setVisibility(s.toString().trim().isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }
        });

        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) return;

            for (ChatsModel c : adapter.getAllChats()) {
                if (c.getName().equalsIgnoreCase(name)) return;
            }

            ChatsModel newUser = new ChatsModel(name);
            adapter.addChat(newUser);
            saveChatsToPrefs(adapter.getAllChats());
            dialog.dismiss();
        });
    }

    private void showCreateGroupDialog() {
        List<ChatsModel> allUsers = loadChatsFromPrefs();
        String[] userNames = new String[allUsers.size()];
        boolean[] checkedItems = new boolean[allUsers.size()];

        for (int i = 0; i < allUsers.size(); i++) {
            userNames[i] = allUsers.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Contacts for Group");

        builder.setMultiChoiceItems(userNames, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked);

        builder.setPositiveButton("Create Group", (dialog, which) -> {
            List<String> selectedNames = new ArrayList<>();
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    selectedNames.add(userNames[i]);
                }
            }

            if (selectedNames.isEmpty()) return;

            // Ask for group name
            AlertDialog.Builder groupNameDialog = new AlertDialog.Builder(requireContext());
            groupNameDialog.setTitle("Enter Group Name");

            EditText input = new EditText(requireContext());
            groupNameDialog.setView(input);

            groupNameDialog.setPositiveButton("Save", (nameDialog, nameWhich) -> {
                String groupName = input.getText().toString().trim();
                if (groupName.isEmpty()) return;

                // Save group as ChatsModel with concatenated names
                String groupDesc = groupName + " (" + String.join(", ", selectedNames) + ")";
                ChatsModel group = new ChatsModel(groupDesc);

                adapter.addChat(group);
                saveChatsToPrefs(adapter.getAllChats());
            });

            groupNameDialog.setNegativeButton("Cancel", null);
            groupNameDialog.show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveChatsToPrefs(List<ChatsModel> chats) {
        SharedPreferences prefs = requireContext().getSharedPreferences("chats", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        JSONArray jsonArray = new JSONArray();
        try {
            for (ChatsModel chat : chats) {
                JSONObject obj = new JSONObject();
                obj.put("name", chat.getName());
                jsonArray.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        editor.putString("chat_list", jsonArray.toString());
        editor.apply();
    }

    private List<ChatsModel> loadChatsFromPrefs() {
        List<ChatsModel> chats = new ArrayList<>();
        SharedPreferences prefs = requireContext().getSharedPreferences("chats", getContext().MODE_PRIVATE);
        String jsonString = prefs.getString("chat_list", "");

        if (!jsonString.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    chats.add(new ChatsModel(obj.getString("name")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return chats;
    }

    @Override
    public void openChatFragment(String name) {
        Log.d("HomeFragment", "Opening chat for: " + name);

        ChatFragment chatFragment = new ChatFragment();
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        chatFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, chatFragment)
                .addToBackStack(null)
                .commit();
    }
}
