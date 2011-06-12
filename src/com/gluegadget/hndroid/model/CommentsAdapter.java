package com.gluegadget.hndroid.model;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gluegadget.hndroid.R;

public class CommentsAdapter extends ArrayAdapter<Comment> {
  private final LayoutInflater mInflater;
  int resource;

  public CommentsAdapter(final Context _context, final int _resource, final List<Comment> _items) {
    super(_context, _resource, _items);
    mInflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    resource = _resource;
  }

  static class ViewHolder {
    TextView title;
    TextView author;
    TextView score;
  }

  @Override
  public View getView(final int position, View convertView, final ViewGroup parent) {
    ViewHolder holder;

    final Comment item = getItem(position);

    if (convertView == null) {
      convertView = mInflater.inflate(resource, parent, false);
      holder = new ViewHolder();
      holder.title = (TextView) convertView.findViewById(R.id.title);
      holder.author = (TextView) convertView.findViewById(R.id.author);
      holder.score = (TextView) convertView.findViewById(R.id.score);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    holder.title.setPadding(item.getPadding() + 1, 10, 10, 10);
    holder.title.setText(item.getTitle());

    if (item.getAuthor().toString() == "")
      holder.author.setText("");
    else
      holder.author.setText("by " + item.getAuthor().toString());

    if (item.getScore() != null)
      holder.score.setText(String.format("(%s)", item.getScore()));
    else
      holder.score.setText("");

    return convertView;
  }
}
