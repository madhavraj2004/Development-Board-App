package com.example.whatapp;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> chatList;
    private static final Pattern OSM_PATTERN = Pattern.compile(
            "(https://www\\.openstreetmap\\.org/\\?mlat=([-+]?\\d*\\.\\d+)&mlon=([-+]?\\d*\\.\\d+))"
    );
    private static final Pattern COORD_PATTERN = Pattern.compile(
            "\"latitude\":([-+]?\\d*\\.\\d+),\"longitude\":([-+]?\\d*\\.\\d+)"
    );

    public ChatAdapter(List<ChatMessage> chatList) {
        this.chatList = chatList;
    }

    @NonNull @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == 0 ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage chat = chatList.get(position);
        String originalText = chat.getMessage();
        String formattedText = formatMessage(originalText, chat);

        SpannableString spannable = new SpannableString(formattedText);

        // Make coordinates clickable
        makeCoordinatesClickable(spannable, formattedText);

        holder.messageText.setText(spannable);
        holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String formatMessage(String originalText, ChatMessage chat) {
        StringBuilder formatted = new StringBuilder();

        // Extract information from JSON if it's a JSON message
        if (originalText.startsWith("{") && originalText.endsWith("}")) {
            try {
                // Parse JSON to extract components
                String senderMac = extractValue(originalText, "sender_mac");
                String message = extractValue(originalText, "message");
                String latitude = extractValue(originalText, "latitude");
                String longitude = extractValue(originalText, "longitude");
                String groupId = extractValue(originalText, "group_id");
                String type = extractValue(originalText, "type");

                // Build formatted message
                if (senderMac != null && !senderMac.equals("SELF")) {
                    formatted.append("From: ").append(chat.getSenderName()).append("\n");
                }

                if (groupId != null && !groupId.isEmpty()) {
                    formatted.append("To: Group ").append(groupId).append("\n");
                } else {
                    formatted.append("To: All\n");
                }

                if (latitude != null && longitude != null &&
                        !latitude.equals("null") && !longitude.equals("null")) {
                    formatted.append("Location: ").append(latitude).append(", ").append(longitude).append("\n");
                }

                if (message != null && !message.isEmpty()) {
                    formatted.append("Message: ").append(message);
                } else if (type != null) {
                    // Handle system messages
                    switch (type) {
                        case "group_create":
                            formatted.append("System: Group created");
                            break;
                        case "group_join":
                            formatted.append("System: Device joined");
                            break;
                        case "group_invite":
                            formatted.append("System: Invitation sent");
                            break;
                        default:
                            formatted.append("System: ").append(type);
                    }
                }

            } catch (Exception e) {
                // If JSON parsing fails, fall back to original text
                formatted.append(originalText);
            }
        } else {
            // For non-JSON messages, use the original format but add sender info for group messages
            if (chat.isGroupMessage() && !chat.isSent()) {
                formatted.append("From: ").append(chat.getSenderName()).append("\n");
                formatted.append("To: Group ").append(chat.getGroupId()).append("\n");
                formatted.append(originalText);
            } else {
                formatted.append(originalText);
            }
        }

        return formatted.toString();
    }

    private String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\"?([^\",}]+)\"?,?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\"", "");
        }
        return null;
    }

    private void makeCoordinatesClickable(SpannableString spannable, String text) {
        // Look for coordinate patterns like "12.3456, 78.9012"
        Pattern coordPattern = Pattern.compile("(\\d+\\.\\d+),\\s*(\\d+\\.\\d+)");
        Matcher matcher = coordPattern.matcher(text);

        while (matcher.find()) {
            final double lat = Double.parseDouble(matcher.group(1));
            final double lon = Double.parseDouble(matcher.group(2));

            spannable.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull @NotNull View widget) {
                    Bundle args = new Bundle();
                    args.putDouble("lat", lat);
                    args.putDouble("lon", lon);
                    Navigation.findNavController(widget)
                            .navigate(R.id.action_chatFragment_to_mapFragment, args);
                }
            }, matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int pos) {
        ChatMessage message = chatList.get(pos);
        return message.isSent() ? 0 : 1;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        ChatViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
        }
    }
}