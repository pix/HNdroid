package com.gluegadget.hndroid.activities;

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gluegadget.hndroid.HackerNewsClient;
import com.gluegadget.hndroid.LoginDialog;
import com.gluegadget.hndroid.R;
import com.gluegadget.hndroid.Utils;
import com.gluegadget.hndroid.model.News;
import com.gluegadget.hndroid.model.NewsAdapter;

public class MainActivity extends HNDroidActivity {
  static final String PREFS_NAME = "user";

  static final private int MENU_LOGIN = 2;
  static final private int MENU_LOGOUT = 3;
  static final private int MENU_PREFERENCES = 4;

  private static final int LIST_MENU_GROUP = 10;
  private static final int LIST_NEWS_ID = 11;
  private static final int LIST_BEST_ID = 12;
  private static final int LIST_ACTIVE_ID = 13;
  private static final int LIST_NOOB_ID = 14;

  static final private int CONTEXT_USER_SUBMISSIONS = 2;
  static final private int CONTEXT_COMMENTS = 3;
  static final private int CONTEXT_USER_LINK = 4;
  static final private int CONTEXT_USER_UPVOTE = 5;
  static final private int CONTEXT_GOOGLE_MOBILE = 6;
  static final private int CONTEXT_SHARE = 7;

  static final private int NOTIFY_DATASET_CHANGED = 1;
  static final private int LOGIN_FAILED = 2;
  static final private int LOGIN_SUCCESSFULL = 3;

  static int DEFAULT_ACTION_PREFERENCES = 0;

  private String loginUrl = "";
  private ListView newsListView;
  private NewsAdapter aa;
  private final ArrayList<News> news = new ArrayList<News>();
  private TextView hnTopDesc;
  private TextView hnUserKarma;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Bundle extras = getIntent().getExtras();

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    setContentView(R.layout.main);

    newsListView = (ListView) findViewById(R.id.hnListView);
    registerForContextMenu(newsListView);

    final View header = View.inflate(this, R.layout.hntop, null);
    newsListView.addHeaderView(header);
    hnTopDesc = (TextView) header.findViewById(R.id.hnTopDesc);
    hnUserKarma = (TextView) header.findViewById(R.id.hnUserKarma);

    aa = new NewsAdapter(this, R.layout.news_list_item, news);

    newsListView.setAdapter(aa);
    newsListView.setOnItemClickListener(clickListener);

    if (extras != null) {
      String user = null, title = null;
      user = extras.getString("user");
      title = extras.getString("title");

      if (!Utils.isEmpty(title))
        hnTopDesc.setText(extras.getString("title"));

      if (!Utils.isEmpty(user))
        refreshNews("http://news.ycombinator.com/submitted?id=" + user);
      else
        refreshNews();
    } else {
      refreshNews();
    }

  }

  Handler handler = new Handler() {
    @Override
    public void handleMessage(final Message msg) {
      switch (msg.what) {
        case NOTIFY_DATASET_CHANGED:
          aa.notifyDataSetChanged();
          newsListView.setSelection(0);
          break;
        case LOGIN_FAILED:
          onLoginFailed();
          break;
        case LOGIN_SUCCESSFULL:
          onLoginSucceeded();
          break;
        default:
          break;
      }
    }
  };

  OnItemClickListener clickListener = new OnItemClickListener() {
    @Override
    public void onItemClick(final AdapterView<?> newsAV, final View view, final int pos, final long id) {
      final News item = (News) newsAV.getAdapter().getItem(pos);
      if (pos < newsAV.getAdapter().getCount() - 1) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        final String ListPreference = prefs.getString("PREF_DEFAULT_ACTION", "view-comments");
        if (ListPreference.equalsIgnoreCase("open-in-browser")) {
          final Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(item.getUrl()));
          startActivity(viewIntent);
        } else if (ListPreference.equalsIgnoreCase("view-comments")) {
          final Intent intent = new Intent(MainActivity.this, CommentsActivity.class);
          intent.putExtra("url", item.getCommentsUrl());
          intent.putExtra("title", item.getTitle());
          startActivity(intent);
        } else if (ListPreference.equalsIgnoreCase("mobile-adapted-view")) {
          final Intent viewIntent = new Intent("android.intent.action.VIEW", getMobileUri(item));
          startActivity(viewIntent);
        }
      } else {
        refreshNews(item.getUrl());
      }
    }
  };

  void onLoginFailed() {
    Toast.makeText(MainActivity.this, "Login failed :(", Toast.LENGTH_LONG).show();
  }

  void onLoginSucceeded() {
    Toast.makeText(MainActivity.this, "Successful login :)", Toast.LENGTH_LONG).show();
    refreshNews();
  }

  private class OnLoginListener implements LoginDialog.ReadyListener {
    @Override
    public void ready(final String username, final String password) {
      try {
        showProgressDialog("Trying to login. Please wait...");
        new Thread(new Runnable() {
          @Override
          public void run() {
            final boolean success = getClient().logIn(loginUrl, username, password);
            hideProgressDialog();
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                if (success)
                  onLoginSucceeded();
                else
                  onLoginFailed();
              }
            });
          }
        }).start();
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    super.onCreateOptionsMenu(menu);

    final MenuItem menuItemRefresh = menu.add(R.string.menu_refresh);
    menuItemRefresh.setIcon(R.drawable.ic_menu_refresh);
    menuItemRefresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        try {
          refreshNews();
        } catch (final Exception e) {
          e.printStackTrace();
        }
        return true;
      }
    });

    final SubMenu subMenu = menu.addSubMenu(R.string.menu_lists);
    subMenu.add(LIST_MENU_GROUP, LIST_NEWS_ID, 0, "news");
    subMenu.add(LIST_MENU_GROUP, LIST_BEST_ID, 1, "best");
    subMenu.add(LIST_MENU_GROUP, LIST_ACTIVE_ID, 2, "active");
    subMenu.add(LIST_MENU_GROUP, LIST_NOOB_ID, 3, "noobstories");
    subMenu.setIcon(R.drawable.ic_menu_friendslist);
    subMenu.setGroupCheckable(LIST_MENU_GROUP, true, true);

    final MenuItem itemLogout = menu.add(0, MENU_LOGOUT, Menu.NONE, R.string.menu_logout);
    itemLogout.setIcon(R.drawable.ic_menu_logout);
    itemLogout.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        try {
          getClient().logOut();
          refreshNews();
        } catch (final Exception e) {
          e.printStackTrace();
        }
        return true;
      }
    });

    final MenuItem itemLogin = menu.add(0, MENU_LOGIN, Menu.NONE, R.string.menu_login);
    itemLogin.setIcon(R.drawable.ic_menu_login);
    itemLogin.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        final LoginDialog loginDialog = new LoginDialog(MainActivity.this, "", new OnLoginListener());
        loginDialog.show();
        return true;
      }
    });

    final MenuItem itemPreferences = menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
    itemPreferences.setIcon(R.drawable.ic_menu_preferences);
    itemPreferences.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        final Intent intent = new Intent(MainActivity.this, PreferenceActivity.class);
        startActivity(intent);
        return true;
      }
    });

    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu) {

    if (loginUrl.contains("submit")) {
      menu.findItem(MENU_LOGIN).setVisible(false);
      menu.findItem(MENU_LOGIN).setEnabled(false);
      menu.findItem(MENU_LOGOUT).setVisible(true);
      menu.findItem(MENU_LOGOUT).setEnabled(true);
    } else {
      menu.findItem(MENU_LOGIN).setVisible(true);
      menu.findItem(MENU_LOGIN).setEnabled(true);
      menu.findItem(MENU_LOGOUT).setVisible(false);
      menu.findItem(MENU_LOGOUT).setEnabled(false);
    }

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
      case LIST_ACTIVE_ID:
      case LIST_BEST_ID:
      case LIST_NOOB_ID:
      case LIST_NEWS_ID:
        final String hnFeed = getString(R.string.hnfeed);
        refreshNews(hnFeed + item.toString());
        handler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
    }
    return true;
  }

  @Override
  public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);

    final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    if (info.position < 30) {
      final News newsContexted = (News) newsListView.getAdapter().getItem(info.position);

      menu.setHeaderTitle(newsContexted.getTitle());

      addShareToMenu(menu, newsContexted);
      addOriginalLinkToMenu(menu, newsContexted);
      addGoogleMobileLinkToMenu(menu, newsContexted);
      addCommentsToMenu(menu, newsContexted);
      maybeAddUserSubmissionsToMenu(menu, newsContexted);
      maybeAddUpvoteToMenu(menu, newsContexted);

    }
  }

  private void addOriginalLinkToMenu(final ContextMenu menu, final News newsContexted) {
    final MenuItem originalLink = menu.add(0, CONTEXT_USER_LINK, 0, newsContexted.getUrl());
    originalLink.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        final Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse((String) item.getTitle()));
        startActivity(viewIntent);
        return true;
      }
    });
  }

  private void addShareToMenu(final ContextMenu menu, final News newsContexted) {
    final MenuItem shareLink = menu.add(0, CONTEXT_SHARE, 0, R.string.context_share);
    shareLink.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, newsContexted.getTitle());
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
            String.format("%s: %s", newsContexted.getTitle(), newsContexted.getUrl()));
        startActivity(Intent.createChooser(shareIntent, "Share this link"));
        return true;
      }
    });

  }

  private void addGoogleMobileLinkToMenu(final ContextMenu menu, final News newsContexted) {
    final MenuItem googleMobileLink = menu.add(0, CONTEXT_GOOGLE_MOBILE, 0, R.string.context_google_mobile);
    googleMobileLink.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        final Uri mobileUri = getMobileUri(newsContexted);
        final Intent viewIntent = new Intent("android.intent.action.VIEW", mobileUri);
        startActivity(viewIntent);
        return true;
      }
    });
  }

  private void addCommentsToMenu(final ContextMenu menu, final News newsContexted) {
    if (newsContexted.getCommentsUrl() != "") {
      final MenuItem comments = menu.add(0, CONTEXT_COMMENTS, 0, R.string.menu_comments);
      comments.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(final MenuItem item) {
          final Intent intent = new Intent(MainActivity.this, CommentsActivity.class);
          intent.putExtra("url", newsContexted.getCommentsUrl());
          intent.putExtra("title", newsContexted.getTitle());
          startActivity(intent);
          return true;
        }
      });
    }
  }

  private Uri getMobileUri(final News newsContexted) {
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    final String mobileConverter = prefs.getString("PREF_MOBILE_VERSION",
        getResources().getStringArray(R.array.mobile_version_values)[0]);
    final Uri mobileUri = Uri.parse(String.format(mobileConverter, newsContexted.getUrl()));
    return mobileUri;
  }

  private void maybeAddUpvoteToMenu(final ContextMenu menu, final News newsContexted) {
    if (loginUrl.contains("submit") && newsContexted.getUpVoteUrl() != "") {
      final MenuItem upVote = menu.add(0, CONTEXT_USER_UPVOTE, 0, R.string.context_upvote);
      upVote.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(final MenuItem item) {
          showProgressDialog("Voting. Please wait...");
          new Thread(new Runnable() {
            @Override
            public void run() {
              getClient().upVote(newsContexted);
              hideProgressDialog();
              handler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
            }
          }).start();
          return true;
        }
      });
    }
  }

  private void maybeAddUserSubmissionsToMenu(final ContextMenu menu, final News newsContexted) {
    final MenuItem userSubmissions = menu.add(0, CONTEXT_USER_SUBMISSIONS, 0, newsContexted.getAuthor()
        + " submissions");
    userSubmissions.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        final Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.putExtra("user", newsContexted.getAuthor());
        intent.putExtra("title", newsContexted.getAuthor() + " submissions");
        startActivity(intent);
        return true;
      }
    });
  }

  private void refreshNews() {
    final String hnFeed = getString(R.string.hnfeed);
    refreshNews(hnFeed);
  }

  protected void refreshNews(final String newsUrl) {
    showProgressDialog("Loading news. Please wait...");
    new Thread(new Runnable() {
      @Override
      public void run() {
        loginUrl = getClient().downloadAndParseNews(newsUrl, news);
        final HackerNewsClient.UserInfo userInfo = getClient().getCachedUserInfo();
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            if (userInfo != null) {
              hnUserKarma.setText(String.format("%s (%s)", userInfo.getUsername(), userInfo.getKarma()));
            }
            aa.notifyDataSetChanged();
            newsListView.setSelection(0);
            hideProgressDialog();
          }
        });
      }
    }).start();
  }
}
