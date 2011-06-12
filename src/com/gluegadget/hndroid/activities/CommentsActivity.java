package com.gluegadget.hndroid.activities;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import com.gluegadget.hndroid.CommentDialog;
import com.gluegadget.hndroid.HackerNewsClient;
import com.gluegadget.hndroid.R;
import com.gluegadget.hndroid.model.Comment;
import com.gluegadget.hndroid.model.CommentsAdapter;

public class CommentsActivity extends HNDroidActivity {

  static final private int MENU_UPDATE = Menu.FIRST;
  static final private int MENU_COMMENT = Menu.FIRST + 1;

  static final private int NOTIFY_DATASET_CHANGED = 1;
  static final private int NOTIFY_COMMENT_ADDED = 2;

  static final private int CONTEXT_REPLY = 1;
  static final private int CONTEXT_UPVOTE = 2;
  static final private int CONTEXT_USER_SUBMISSIONS = 3;

  private ListView newsListView;
  private CommentsAdapter commentsAdapter;
  private final ArrayList<Comment> commentsList = new ArrayList<Comment>();
  private String extrasCommentsUrl;
  private String fnId = "";
  private Boolean loggedIn = false;
  private TextView hnUserKarma;
  private TextView hnTopDesc;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    newsListView = (ListView) findViewById(R.id.hnListView);
    registerForContextMenu(newsListView);

    final View header = View.inflate(this, R.layout.hntop, null);
    newsListView.addHeaderView(header);

    commentsAdapter = new CommentsAdapter(this, R.layout.comments_list_item, commentsList);
    newsListView.setAdapter(commentsAdapter);

    final Bundle extras = getIntent().getExtras();
    extrasCommentsUrl = extras.getString("url");
    hnTopDesc = (TextView) header.findViewById(R.id.hnTopDesc);
    hnTopDesc.setText(extras.getString("title"));

    hnUserKarma = (TextView) header.findViewById(R.id.hnUserKarma);

    refreshComments(extrasCommentsUrl);
  }

  Handler handler = new Handler() {
    @Override
    public void handleMessage(final Message msg) {
      switch (msg.what) {
        case NOTIFY_DATASET_CHANGED:
          commentsAdapter.notifyDataSetChanged();
          break;
        case NOTIFY_COMMENT_ADDED:
          refreshComments(extrasCommentsUrl);
          break;
        default:
          break;
      }
    }
  };

  private class OnCommentListener implements CommentDialog.ReadyListener {
    @Override
    public void ready(final String text) {
      try {
        showProgressDialog("Trying to comment. Please wait...");
        new Thread(new Runnable() {
          @Override
          public void run() {
            final boolean success = getClient().postComment(text, fnId);
            hideProgressDialog();
            if (success)
              handler.sendEmptyMessage(NOTIFY_COMMENT_ADDED);
          }
        }).start();
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    @Override
    public void ready(final String text, final String url) {
    }
  }

  private class OnReplyListener implements CommentDialog.ReadyListener {
    @Override
    public void ready(final String text, final String replyUrl) {
      try {
        showProgressDialog("Trying to reply. Please wait...");
        new Thread(new Runnable() {
          @Override
          public void run() {
            final boolean success = getClient().replyToComment(text, replyUrl);
            hideProgressDialog();
            if (success)
              handler.sendEmptyMessage(NOTIFY_COMMENT_ADDED);
          }
        }).start();
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    @Override
    public void ready(final String text) {
    }
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    super.onCreateOptionsMenu(menu);

    addRefreshToOptionMenu(menu);
    addCommentToOptionMenu(menu);

    return true;
  }

  private void addCommentToOptionMenu(final Menu menu) {
    final MenuItem itemComment = menu.add(0, MENU_COMMENT, Menu.NONE, R.string.menu_comment);
    itemComment.setIcon(R.drawable.ic_menu_compose);
    itemComment.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        final CommentDialog commentDialog = new CommentDialog(CommentsActivity.this, "Comment on submission",
            new OnCommentListener());
        commentDialog.show();

        return true;
      }
    });
  }

  private void addRefreshToOptionMenu(final Menu menu) {
    final MenuItem itemRefresh = menu.add(0, MENU_UPDATE, Menu.NONE, R.string.menu_refresh);
    itemRefresh.setIcon(R.drawable.ic_menu_refresh);
    itemRefresh.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        try {
          refreshComments(extrasCommentsUrl);
        } catch (final Exception e) {
          e.printStackTrace();
        }
        return true;
      }
    });
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu) {
    if (!loggedIn || fnId == "") {
      menu.findItem(MENU_COMMENT).setVisible(false);
      menu.findItem(MENU_COMMENT).setEnabled(false);
    }

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);

    final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    final Comment newsContexted = (Comment) newsListView.getAdapter().getItem(info.position);

    menu.setHeaderTitle(newsContexted.getTitle().toString());

    addReplyToContextMenu(menu, newsContexted);
    addUserSubmissionToContextMenu(menu, newsContexted);
    addUpvoteToContextMenu(menu, newsContexted);
  }

  private void addReplyToContextMenu(final ContextMenu menu, final Comment newsContexted) {
    if (fnId != "" && newsContexted.getReplyToUrl() != "" && loggedIn) {
      final MenuItem originalLink = menu.add(0, CONTEXT_REPLY, 0, R.string.context_reply);
      originalLink.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(final MenuItem item) {
          final CommentDialog commentDialog = new CommentDialog(CommentsActivity.this, "Reply to "
              + newsContexted.getAuthor(), newsContexted.getReplyToUrl(), new OnReplyListener());
          commentDialog.show();

          return true;
        }
      });
    }
  }

  private void addUserSubmissionToContextMenu(final ContextMenu menu, final Comment newsContexted) {
    final MenuItem userSubmissions = menu.add(0, CONTEXT_USER_SUBMISSIONS, 0, newsContexted.getAuthor()
        + " submissions");
    userSubmissions.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(final MenuItem item) {
        final Intent intent = new Intent(CommentsActivity.this, MainActivity.class);
        intent.putExtra("user", newsContexted.getAuthor().toString());
        intent.putExtra("title", newsContexted.getAuthor() + " submissions");
        startActivity(intent);
        return true;
      }
    });
  }

  private void addUpvoteToContextMenu(final ContextMenu menu, final Comment newsContexted) {
    if (newsContexted.getUpVoteUrl() != "" && loggedIn) {
      final MenuItem upVote = menu.add(0, CONTEXT_UPVOTE, 0, R.string.context_upvote);
      upVote.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(final MenuItem item) {
          showProgressDialog("Voting. Please wait...");
          new Thread(new Runnable() {
            @Override
            public void run() {
              getClient().upVoteComment(newsContexted);
              hideProgressDialog();
            }
          }).start();
          return true;
        }
      });
    }
  }

  private void refreshComments(final String commentsUrl) {
    showProgressDialog("Loading. Please wait...");

    new Thread(new Runnable() {
      @Override
      public void run() {
        downloadAndParseComments(commentsUrl);
        final HackerNewsClient.UserInfo userInfo = getClient().getCachedUserInfo();
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            if (userInfo != null) {
              hnUserKarma.setText(String.format("%s (%s)", userInfo.getUsername(), userInfo.getKarma()));
            }
            hideProgressDialog();
            commentsAdapter.notifyDataSetChanged();
          }
        });
      }
    }).start();
  }

  private void downloadAndParseComments(final String uri) {
    try {
      final HackerNewsClient.CommentPageInfo info = getClient().downloadAndParseComments(uri, commentsList);
      fnId = info.fnId;
      loggedIn = info.loggedIn;
    } catch (final IllegalStateException e) {
      // TODO: Can we do something better than this?
      // TODO: What is this code doing, anyway?
      finish();
    }
  }
}
