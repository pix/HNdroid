package com.gluegadget.hndroid.model;

import com.gluegadget.hndroid.Utils;

public class News {

  private final String title;
  private final String author;
  private final String score;
  private final String comment;
  private final String url;
  private String domain;
  private String commentsUrl;
  private String upVoteUrl;

  public News(final String _title, final String _score, final String _comment, final String _author,
      final String _domain, final String _url, final String _commentsUrl, final String _upVoteUrl) {
    title = _title;
    score = _score;
    comment = _comment;
    author = _author;
    url = _url;
    if (_commentsUrl.length() > 7)
      commentsUrl = "http://news.ycombinator.com/item?id=" + _commentsUrl.substring(6);
    else
      commentsUrl = _commentsUrl;

    if (_domain.length() > 2)
      domain = _domain.substring(1, _domain.length() - 1);
    else
      domain = _domain;

    if (_upVoteUrl.length() > 1)
      upVoteUrl = "http://news.ycombinator.com/" + _upVoteUrl.replace("&amp", "&");
    else
      upVoteUrl = _upVoteUrl;
  }

  public News(final String _title) {
    this(_title, "", "", "", "", "", "", "");
  }

  public String getCommentsUrl() {
    return commentsUrl;
  }

  public String getTitle() {
    return title;
  }

  public String getScore() {
    return score;
  }

  public String getComment() {
    String returnValue = "";
    if (comment.contains("discuss")) {
      returnValue = "0";
    } else if (comment.equals("comments")) {
      returnValue = "0";
    } else {
      final String tmp = comment.replaceAll("comments?", "");
      if (tmp.length() == 0)
        returnValue = "?";
      else
        returnValue = tmp;
    }

    return returnValue;
  }

  public String getAuthor() {
    return author;
  }

  public String getUrl() {
    if (url == null)
      return null;

    if (!url.startsWith("http"))
      if (url.startsWith("/"))
        return "http://news.ycombinator.com" + url;
      else
        return "http://news.ycombinator.com/" + url;
    else
      return url;
  }

  public String getUpVoteUrl() {
    return upVoteUrl;
  }

  public String getDomain() {
    return domain;
  }

  @Override
  public String toString() {
    if (author == "")
      return title;
    else
      return title + " by " + author;
  }

  public boolean isDead() {
    return Utils.isEmpty(url);
  }

  public boolean isValidNews() {
    return !(Utils.isEmpty(domain) && Utils.isEmpty(author) && Utils.isEmpty(score));
  }

}
