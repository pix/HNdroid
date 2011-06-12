package com.gluegadget.hndroid;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CommentDialog extends Dialog {

  public interface ReadyListener {
    public void ready(String text);

    public void ready(String text, String replyUrl);
  }

  private class loginListener implements android.view.View.OnClickListener {
    @Override
    public void onClick(final View v) {
      if (replyUrl == null)
        readyListener.ready(String.valueOf(text.getText()));
      else
        readyListener.ready(String.valueOf(text.getText()), replyUrl);

      dismiss();
    }
  }

  private final ReadyListener readyListener;
  private final String name;
  private String replyUrl;
  private Button submitButton;
  private EditText text;

  public CommentDialog(final Context context, final String name, final ReadyListener readyListener) {
    super(context);
    this.readyListener = readyListener;
    this.name = name;
  }

  public CommentDialog(final Context context, final String name, final String replyUrl,
      final ReadyListener readyListener) {
    super(context);
    this.readyListener = readyListener;
    this.name = name;
    this.replyUrl = replyUrl;
  }

  public void onClick(final View v) {
    if (v == submitButton) {
      dismiss();
    }
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.comment);
    setTitle(name);
    submitButton = (Button) findViewById(R.id.submit_button);
    submitButton.setOnClickListener(new loginListener());
    text = (EditText) findViewById(R.id.text);
  }

}
