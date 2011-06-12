package com.gluegadget.hndroid.model;

import android.text.Html;
import android.text.Spanned;

public class Comment {

  private final String title;
  private final String score;
  private final String author;
  private String replyToUrl;
  private String upVoteUrl;
  private final Integer padding;

  public Comment(final String _title, final String _author, final String _score, final Integer _padding,
      final String _replyToUrl, final String _upVoteUrl) {
    title = _title;
    score = _score;
    author = _author;
    padding = _padding;

    if (_replyToUrl.length() > 1)
      replyToUrl = "http://news.ycombinator.com/" + _replyToUrl.replace("&amp", "&");
    else
      replyToUrl = _replyToUrl;

    if (_upVoteUrl.length() > 1)
      upVoteUrl = "http://news.ycombinator.com/" + _upVoteUrl.replace("&amp", "&");
    else
      upVoteUrl = _upVoteUrl;
  }

  public Comment(final String _title) {
    this(_title, "", "", 0, "", "");
  }

  public Integer getPadding() {
    return padding;
  }

  public Spanned getTitle() {
    return Html.fromHtml(title);
  }

  public String getAuthor() {
    return author;
  }

  public String getScore() {
    return score;
  }

  public String getReplyToUrl() {
    return replyToUrl;
  }

  public String getUpVoteUrl() {
    return upVoteUrl;
  }

  @Override
  public String toString() {
    return author + ": " + title;
  }
}
