package com.yeonfish.phone2webcam.common.socket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yeonfish.phone2webcam.R;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SocketAdapter extends RecyclerView.Adapter<SocketAdapter.SocketViewHolder> {
    private List<CustomSocket> clients;
    private CustomSocket mainClient = null;

    public SocketAdapter() {
        clients = new ArrayList<CustomSocket>();
    }

    public void appendClient(CustomSocket socket) throws SocketException {
        if (clients.size() == 0)
            setMain(socket.id);
        clients.add(socket);
    }

    public CustomSocket setMain(String id) throws SocketException {
        AtomicReference<CustomSocket> result = new AtomicReference<CustomSocket>(null);
        clients.forEach((CustomSocket customSocket) -> {
            if (customSocket.id.equals(id))
                result.set(customSocket);
        });

        CustomSocket rValue = result.get();
        if (!rValue.socket.getKeepAlive())
            throw new RuntimeException("The Socket is unusable");

        mainClient = result.get();
        return result.get();
    }

    public List<CustomSocket> getClients() {
        return clients;
    }

    public CustomSocket getClient() {
        return mainClient;
    }

    @NonNull
    @Override
    public SocketAdapter.SocketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_socket, parent, false);
        return new SocketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SocketAdapter.SocketViewHolder holder, int position) {
        CustomSocket socketItem = clients.get(position);
        holder.socketNameTextView.setText(socketItem.id);
        holder.socketConnectBtnView.setTag(socketItem.id);
    }

    @Override
    public int getItemCount() {
        return clients.size();
    }

    public static class SocketViewHolder extends RecyclerView.ViewHolder {
        TextView socketNameTextView;
        Button socketConnectBtnView;

        public SocketViewHolder(@NonNull View itemView) {
            super(itemView);
            socketNameTextView = itemView.findViewById(R.id.socketNameTextView);
            socketConnectBtnView = itemView.findViewById(R.id.socketConnectBtnView);
        }
    }
}
