package com.example.drim.adapters;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.drim.R;
import com.example.drim.models.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class TextAdapter extends RecyclerView.Adapter<TextAdapter.ViewHolder> {
    private List<Text> mTexts;
    private Context mContext;
    private int TYPE_OUTGOING = 1;
    private int TYPE_INCOMING = 2;

    public TextAdapter(Context context, List<Text> texts) {
        mTexts = texts;
        mContext = context;
    }

    @Override
    public int getItemViewType(int position) {
        if (mTexts.get(position).from == null) {
            return TYPE_OUTGOING;
        } else {
            return TYPE_INCOMING;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;

        if (viewType == TYPE_INCOMING) {
            view = inflater.inflate(R.layout.item_text_incoming, parent, false);
            return new ViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.item_text_outgoing, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Text text = mTexts.get(position);
        holder.bind(text);
    }

    @Override
    public int getItemCount() {
        return mTexts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView body;
        TextView date;

        public ViewHolder(View itemView) {
            super(itemView);
            body = (TextView) itemView.findViewById(R.id.tvText);
            date = (TextView) itemView.findViewById(R.id.tvDate);
        }

        public void bind(Text text) {
            body.setMovementMethod(LinkMovementMethod.getInstance());
            body.setText(text.stringContent);
            Linkify.addLinks(body, Linkify.ALL);
            String year = new SimpleDateFormat("yyyy").format(Long.parseLong(text.timestamp));
            String formattedDate = new SimpleDateFormat("MM/dd/yyyy").format(Long.parseLong(text.timestamp));
            if (Long.parseLong(year) == Calendar.getInstance().get(Calendar.YEAR)) {
                String today = new SimpleDateFormat("MM/dd/yyyy").format(Calendar.getInstance().getTime());
                if (formattedDate.equals(today)) {
                    formattedDate = new SimpleDateFormat("hh:mm aa").format(Long.parseLong(text.timestamp));
                } else {
                    formattedDate = new SimpleDateFormat("MMM d").format(Long.parseLong(text.timestamp));
                }
            }
            date.setText(formattedDate);
        }
    }
}
