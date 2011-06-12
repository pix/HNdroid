package com.gluegadget.hndroid;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.content.Context;
import android.content.SharedPreferences;

import com.gluegadget.hndroid.model.Comment;
import com.gluegadget.hndroid.model.News;

/**
 * This is a rather ad hoc class containing all the network client code that
 * used to live in other classes throughout the application. Our APIs are a
 * little odd, because code was yanked out of other classes and moved here
 * without any real re-design.
 */
public class HackerNewsClient {
  public static class CommentPageInfo {
    public String fnId = "";
    public boolean loggedIn = false;
  }

  public static class UserInfo {

    String karma;
    String username;

    UserInfo(final String username, final String karma) {
      this.username = username;
      this.karma = karma;
    }

    public String getKarma() {
      return karma;
    }

    public String getUsername() {
      return username;
    }

  }

  public static final int NEWS_PAGE = 0;
  public static final int SUBMISSIONS_PAGE = 1;

  private static final String PREFS_NAME = "user";

  private final Context context;
  private UserInfo cachedUserInfo;

  public HackerNewsClient(final Context context) {
    this.context = context;
  }

  public CommentPageInfo downloadAndParseComments(final String uri, final ArrayList<Comment> commentsList)
      throws IllegalStateException {
    final CommentPageInfo info = new CommentPageInfo();
    try {
      commentsList.clear();
      final SharedPreferences settings = getSharedPreferences();
      final String cookie = settings.getString("cookie", "");
      final DefaultHttpClient httpclient = new DefaultHttpClient();
      final HttpGet httpget = new HttpGet(uri);
      if (cookie != "")
        httpget.addHeader("Cookie", "user=" + cookie);
      final ResponseHandler<String> responseHandler = new BasicResponseHandler();
      final String responseBody = httpclient.execute(httpget, responseHandler);
      final HtmlCleaner cleaner = new HtmlCleaner();
      final TagNode node = cleaner.clean(responseBody);

      cachedUserInfo = parseUserInfos(node);
      final Object[] loginFnid = node.evaluateXPath("//span[@class='pagetop']/a");
      final TagNode loginNode = (TagNode) loginFnid[5];
      if (loginNode.getAttributeByName("href").toString().trim().equalsIgnoreCase("submit"))
        info.loggedIn = true;
      final Object[] forms = node.evaluateXPath("//form[@method='post']/input[@name='fnid']");
      if (forms.length == 1) {
        final TagNode formNode = (TagNode) forms[0];
        info.fnId = formNode.getAttributeByName("value").toString().trim();
      }
      final Object[] comments = node
          .evaluateXPath("//table[@border='0']/tbody/tr/td/img[@src='http://ycombinator.com/images/s.gif']");

      if (comments.length > 1) {
        for (final Object comment2 : comments) {
          final TagNode commentNode = (TagNode) comment2;
          final String depth = commentNode.getAttributeByName("width").toString().trim();
          final Integer depthValue = Integer.parseInt(depth) / 2;
          final TagNode nodeParent = commentNode.getParent().getParent();
          final Object[] comment = nodeParent.evaluateXPath("//span[@class='comment']");
          Comment commentEntry;
          if (comment.length > 0) {
            final TagNode commentSpan = (TagNode) comment[0];
            final StringBuffer commentText = commentSpan.getText();
            if (!commentText.toString().equalsIgnoreCase("[deleted]")) {
              final Object[] score = nodeParent.evaluateXPath("//span[@class='comhead']/span[@id]");
              final Object[] author = nodeParent.evaluateXPath("//span[@class='comhead']/a[1]");
              final Object[] replyTo = nodeParent.evaluateXPath("//p/font[@size='1']/u/a");
              final Object[] upVotes = nodeParent.getParent().evaluateXPath("//td[@valign='top']/center/a[1]");

              final TagNode authorNode = (TagNode) author[0];

              String scoreValue = null;
              String upVoteUrl = "";
              String replyToValue = "";

              if (score.length > 0) {
                final TagNode scoreNode = (TagNode) score[0];
                scoreValue = scoreNode.getText().toString();
                System.out.println(scoreValue);
              }

              final String authorValue = authorNode.getChildren().iterator().next().toString().trim();
              if (upVotes.length > 0) {
                final TagNode upVote = (TagNode) upVotes[0];
                upVoteUrl = upVote.getAttributeByName("href").toString().trim();
              }
              if (replyTo.length > 0) {
                final TagNode replyToNode = (TagNode) replyTo[0];
                replyToValue = replyToNode.getAttributeByName("href").toString().trim();
              }

              final String commentBody = cleaner.getInnerHtml(commentSpan);
              commentEntry = new Comment(commentBody, authorValue, scoreValue, depthValue, replyToValue, upVoteUrl);
            } else {
              commentEntry = new Comment("[deleted]");
            }
            commentsList.add(commentEntry);
          }
        }
      } else {
        final Comment commentEntry = new Comment("No comments.");
        commentsList.add(commentEntry);
      }
    } catch (final MalformedURLException e) {
      e.printStackTrace();
    } catch (final IOException e) {
      e.printStackTrace();
    } catch (final XPatherException e) {
      e.printStackTrace();
    }
    return info;
  }

  public String downloadAndParseNews(final String newsUrl, final ArrayList<News> news) {
    String loginUrl = "";
    try {
      news.clear();
      final SharedPreferences settings = getSharedPreferences();
      final String cookie = settings.getString("cookie", "");
      final DefaultHttpClient httpclient = new DefaultHttpClient();
      final HttpGet httpget = new HttpGet(newsUrl);
      if (cookie != "")
        httpget.addHeader("Cookie", "user=" + cookie);
      final ResponseHandler<String> responseHandler = new BasicResponseHandler();
      final String responseBody = httpclient.execute(httpget, responseHandler);
      final HtmlCleaner cleaner = new HtmlCleaner();
      final TagNode node = cleaner.clean(responseBody);

      cachedUserInfo = parseUserInfos(node);
      final Object[] newsTitles = findNewsTitles(node);
      final Object[] subtexts = node.evaluateXPath("//td[@class='subtext']");
      final Object[] domains = node.evaluateXPath("//span[@class='comhead']");
      final Object[] loginFnid = node.evaluateXPath("//span[@class='pagetop']/a");
      final TagNode loginNode = (TagNode) loginFnid[5];
      loginUrl = loginNode.getAttributeByName("href").toString().trim();

      if (newsTitles.length > 0) {
        int j = 0;
        final int iterateFor = newsTitles.length;
        for (int i = 0; i < iterateFor; i++) {
          String scoreValue = "";
          String authorValue = "";
          String commentValue = "";
          String domainValue = "";
          String commentsUrl = "";
          String upVoteUrl = "";
          final TagNode newsTitle = (TagNode) newsTitles[i];

          final String title = newsTitle.getChildren().iterator().next().toString().trim();

          String href = newsTitle.getAttributeByName("href");
          if (href != null)
            href = href.trim();

          if (i < subtexts.length) {
            final TagNode subtext = (TagNode) subtexts[i];
            final Object[] scoreSpanNode = subtext.evaluateXPath("/span");

            if (scoreSpanNode.length == 0)
              // If there's no span in the subtext it's a job advert. Skip it.
              continue;

            final TagNode score = (TagNode) scoreSpanNode[0];

            final Object[] scoreAnchorNodes = subtext.evaluateXPath("/a");
            final TagNode author = (TagNode) scoreAnchorNodes[0];
            authorValue = findAuthorValue(author);

            if (scoreAnchorNodes.length == 2 || scoreAnchorNodes.length == 3) {
              final TagNode comment = (TagNode) scoreAnchorNodes[scoreAnchorNodes.length - 1];
              commentValue = comment.getChildren().iterator().next().toString().trim();
              commentsUrl = score.getAttributeByName("id").toString().trim();
            }

            final TagNode userNode = newsTitle.getParent().getParent();
            final Object[] upVotes = userNode.evaluateXPath("//td/center/a[1]");
            if (upVotes.length > 0) {
              final TagNode upVote = (TagNode) upVotes[0];
              upVoteUrl = upVote.getAttributeByName("href").toString().trim();
            }

            scoreValue = score.getChildren().iterator().next().toString().trim();

            if (href != null && href.startsWith("http")) {
              final TagNode domain = (TagNode) domains[j];
              domainValue = domain.getChildren().iterator().next().toString().trim();
              j++;
            }
          }

          final News newsEntry = new News(title, scoreValue, commentValue, authorValue, domainValue, href, commentsUrl,
              upVoteUrl);
          news.add(newsEntry);
        }
      }
    } catch (final MalformedURLException e) {
      e.printStackTrace();
    } catch (final IOException e) {
      e.printStackTrace();
    } catch (final XPatherException e) {
      e.printStackTrace();
    }
    return loginUrl;
  }

  public UserInfo getCachedUserInfo() {
    return cachedUserInfo;
  }

  /**
   * Return the username and karma of the currently logged-in user, or null if
   * we're not logged in.
   */
  public UserInfo getUserInfo() {
    try {
      final SharedPreferences settings = getSharedPreferences();
      final String username = settings.getString("username", "");
      if (username == "")
        return null;

      final URL url = new URL("http://news.ycombinator.com/user?id=" + username);
      URLConnection connection;
      connection = url.openConnection();

      final InputStream in = connection.getInputStream();
      final HtmlCleaner cleaner = new HtmlCleaner();
      final TagNode node = cleaner.clean(in);
      final Object[] userInfo = node.evaluateXPath("//form[@method='post']/table/tbody/tr/td[2]");
      if (userInfo.length > 3) {
        final TagNode karmaNode = (TagNode) userInfo[2];
        final String karma = karmaNode.getChildren().iterator().next().toString().trim();
        return new UserInfo(username, karma);
      }
    } catch (final Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  public boolean logIn(final String loginUrl, final String username, final String password) {
    boolean success = false;
    try {
      final DefaultHttpClient httpclient = new DefaultHttpClient();
      final HttpGet httpget = new HttpGet("http://news.ycombinator.com" + loginUrl);
      HttpResponse response;
      final HtmlCleaner cleaner = new HtmlCleaner();
      response = httpclient.execute(httpget);
      HttpEntity entity = response.getEntity();
      final TagNode node = cleaner.clean(entity.getContent());
      final Object[] loginForm = node.evaluateXPath("//form[@method='post']/input");
      final TagNode loginNode = (TagNode) loginForm[0];
      final String fnId = loginNode.getAttributeByName("value").toString().trim();

      final HttpPost httpost = new HttpPost("http://news.ycombinator.com/y");
      final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
      nvps.add(new BasicNameValuePair("u", username));
      nvps.add(new BasicNameValuePair("p", password));
      nvps.add(new BasicNameValuePair("fnid", fnId));
      httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
      response = httpclient.execute(httpost);
      entity = response.getEntity();
      if (entity != null) {
        entity.consumeContent();
      }
      final List<Cookie> cookies = httpclient.getCookieStore().getCookies();
      if (!cookies.isEmpty()) {
        final SharedPreferences settings = getSharedPreferences();
        final SharedPreferences.Editor editor = settings.edit();
        editor.putString("username", username);
        editor.putString("cookie", cookies.get(0).getValue());
        editor.commit();
        success = true;
      }
      httpclient.getConnectionManager().shutdown();
    } catch (final Exception e) {
      // TODO: Do something intelligent with errors.
      e.printStackTrace();
    }
    return success;
  }

  public void logOut() {
    final SharedPreferences settings = getSharedPreferences();
    final SharedPreferences.Editor editor = settings.edit();
    editor.remove("username");
    editor.remove("cookie");
    editor.commit();
    cachedUserInfo = null;
  }

  public boolean postComment(final String text, final String fnId) {
    try {
      final DefaultHttpClient httpclient = new DefaultHttpClient();
      final HttpPost httpost = new HttpPost("http://news.ycombinator.com/r");
      final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
      nvps.add(new BasicNameValuePair("text", text));
      nvps.add(new BasicNameValuePair("fnid", fnId));
      httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
      final SharedPreferences settings = getSharedPreferences();
      final String cookie = settings.getString("cookie", "");
      httpost.addHeader("Cookie", "user=" + cookie);
      httpclient.execute(httpost);
      httpclient.getConnectionManager().shutdown();
      return true;
    } catch (final Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean replyToComment(final String text, final String replyUrl) {
    try {
      final DefaultHttpClient httpclient = new DefaultHttpClient();
      final SharedPreferences settings = getSharedPreferences();
      final String cookie = settings.getString("cookie", "");
      final HttpGet httpget = new HttpGet(replyUrl);
      httpget.addHeader("Cookie", "user=" + cookie);
      final ResponseHandler<String> responseHandler = new BasicResponseHandler();
      final String responseBody = httpclient.execute(httpget, responseHandler);
      final HtmlCleaner cleaner = new HtmlCleaner();
      final TagNode node = cleaner.clean(responseBody);
      final Object[] forms = node.evaluateXPath("//form[@method='post']/input[@name='fnid']");
      if (forms.length == 1) {
        final TagNode formNode = (TagNode) forms[0];
        final String replyToFnId = formNode.getAttributeByName("value").toString().trim();
        final HttpPost httpost = new HttpPost("http://news.ycombinator.com/r");
        final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("text", text));
        nvps.add(new BasicNameValuePair("fnid", replyToFnId));
        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        httpost.addHeader("Cookie", "user=" + cookie);
        httpclient.execute(httpost);
        httpclient.getConnectionManager().shutdown();
      }
      return true;
    } catch (final Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public void upVote(final News news) {
    final SharedPreferences settings = getSharedPreferences();
    final String cookie = settings.getString("cookie", "");
    final DefaultHttpClient httpclient = new DefaultHttpClient();
    final HttpGet httpget = new HttpGet(news.getUpVoteUrl());
    httpget.addHeader("Cookie", "user=" + cookie);
    final ResponseHandler<String> responseHandler = new BasicResponseHandler();
    try {
      httpclient.execute(httpget, responseHandler);
    } catch (final ClientProtocolException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void upVoteComment(final Comment comment) {
    final SharedPreferences settings = getSharedPreferences();
    final String cookie = settings.getString("cookie", "");
    final DefaultHttpClient httpclient = new DefaultHttpClient();
    final HttpGet httpget = new HttpGet(comment.getUpVoteUrl());
    httpget.addHeader("Cookie", "user=" + cookie);
    final ResponseHandler<String> responseHandler = new BasicResponseHandler();
    try {
      httpclient.execute(httpget, responseHandler);
    } catch (final ClientProtocolException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private String findAuthorValue(final TagNode author) {
    return author.getChildren().iterator().next().toString().trim();
  }

  private Object[] findNewsTitles(final TagNode node) throws XPatherException {
    return node.evaluateXPath("//td[@class='title']/a[1]");
  }

  private SharedPreferences getSharedPreferences() {
    return context.getSharedPreferences(PREFS_NAME, 0);
  }

  private UserInfo parseUserInfos(final TagNode node) {
    try {
      String username = null;
      String karma = null;

      final Object[] userInfoNode = node.evaluateXPath("//td[3]/span[@class='pagetop']");
      if (userInfoNode != null && userInfoNode.length > 0) {
        final TagNode userNode = ((TagNode) userInfoNode[0]).findElementByName("a", true);
        final TagNode karmaNode = (TagNode) userInfoNode[0];
        if (userNode.getAttributeByName("href").startsWith("user?")) {
          username = userNode.getText().toString();
          karma = karmaNode.getText().toString().replaceAll("[^0-9]", "");
          return new UserInfo(username, karma);
        }
      }
      return null;
    } catch (final XPatherException e) {
      return null;
    }
  }
}
