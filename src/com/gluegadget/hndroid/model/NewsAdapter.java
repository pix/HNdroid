package com.gluegadget.hndroid.model;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gluegadget.hndroid.R;
import com.gluegadget.hndroid.activities.CommentsActivity;

public class NewsAdapter extends ArrayAdapter<News> {

  private final LayoutInflater mInflater;
  private final Context context;
  private final int resource;

  public NewsAdapter(final Context _context, final int _resource, final List<News> _items) {
    super(_context, _resource, _items);
    mInflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    resource = _resource;
    context = _context;
  }

  static class ViewHolder {
    TextView title;
    TextView score;
    TextView comment;
    TextView author;
  }

  @Override
  public View getView(final int position, View convertView, final ViewGroup parent) {
    ViewHolder holder;

    final News item = getItem(position);

    if (convertView == null) {
      convertView = mInflater.inflate(resource, parent, false);
      holder = new ViewHolder();
      holder.title = (TextView) convertView.findViewById(R.id.title);
      holder.score = (TextView) convertView.findViewById(R.id.score);
      holder.comment = (TextView) convertView.findViewById(R.id.comments);
      holder.author = (TextView) convertView.findViewById(R.id.author);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    holder.title.setText(item.getTitle());
    holder.score.setText(item.getScore());
    holder.comment.setText(item.getComment());
    final String[] commentButtonTag = { item.getTitle(), item.getCommentsUrl() };
    holder.comment.setTag(commentButtonTag);
    holder.comment.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(final View v) {
        final String[] tag = (String[]) v.getTag();
        final Intent intent = new Intent(context, CommentsActivity.class);
        intent.putExtra("title", tag[0]);
        intent.putExtra("url", tag[1]);
        context.startActivity(intent);
      }
    });

    if (item.getAuthor() == "")
      holder.author.setText(item.getAuthor());
    else if (item.getDomain() == "")
      holder.author.setText("by " + item.getAuthor());
    else
      holder.author.setText("by " + item.getAuthor() + " from " + item.getDomain());

    return convertView;
  }
}