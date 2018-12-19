package it.unipi.iet.bikedacity;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class NoStationAdapter extends RecyclerView.Adapter<NoStationAdapter.NoStationViewHolder>{
    private Context context;
    private String description;

    public NoStationAdapter (Context context, String description){
        this.context = context;

        setDescription(description);
    }

    public void setDescription (String description){
        Resources resources = context.getResources();
        this.description = (description == null) ? resources.getString(R.string.default_address_text) :
                                                   description;
    }

    @NonNull
    @Override
    public NoStationViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType){
        Context ctx = parent.getContext();
        View stationView = LayoutInflater.from(ctx).inflate(R.layout.no_station_layout, parent, false);
        return new NoStationViewHolder(stationView);
    }

    @Override
    public void onBindViewHolder (@NonNull NoStationViewHolder holder, int position){
        Resources resources = context.getResources();
        String address = resources.getString(R.string.address_text, this.description);
        holder.addressInfo.setText(address);
    }

    @Override
    public int getItemCount (){
        return 1;
    }

    public static class NoStationViewHolder extends RecyclerView.ViewHolder {
        private TextView addressInfo;
        private TextView noStationFound;

        public NoStationViewHolder (View itemView) {
            super(itemView);
            addressInfo = itemView.findViewById(R.id.address_info);
            noStationFound = itemView.findViewById(R.id.no_station_found);
            noStationFound.setText(R.string.no_station_found_text);
        }
    }
}
