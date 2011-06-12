package com.gluegadget.hndroid;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginDialog extends Dialog {

  public interface ReadyListener {
    public void ready(String username, String password);
  }

  private class LoginListener implements android.view.View.OnClickListener {
    @Override
    public void onClick(final View v) {
      readyListener.ready(String.valueOf(username.getText()), String.valueOf(password.getText()));
      dismiss();
    }
  }

  private final ReadyListener readyListener;
  private Button loginButton;
  private EditText password;
  private EditText username;

  public LoginDialog(final Context context, final String name, final ReadyListener readyListener) {
    super(context);
    this.readyListener = readyListener;

  }

  public void onClick(final View v) {
    if (v == loginButton) {
      dismiss();
    }
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.login);
    setTitle("Sign In");
    loginButton = (Button) findViewById(R.id.login_button);
    loginButton.setOnClickListener(new LoginListener());
    username = (EditText) findViewById(R.id.username);
    password = (EditText) findViewById(R.id.password);
  }

}
